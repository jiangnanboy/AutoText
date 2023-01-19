package sy.ocr.text.recognition;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.util.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author sy
 * @date 2022/1/3 22:01
 */
public class OcrRecognitionTranslator implements Translator<Image, String> {
    private List<String> ocrKeywords;
    private final boolean useSpaceChar;

    public OcrRecognitionTranslator(Map<String, ?> arguments) {
        useSpaceChar = arguments.containsKey("use_space_char")
                        ? Boolean.parseBoolean(arguments.get("use_space_char").toString())
                        : false;
    }

    /**
     * prepare
     * @param ctx
     * @throws IOException
     */
    @Override
    public void prepare(TranslatorContext ctx) throws IOException {
        var model = ctx.getModel();
        try (var is = model.getArtifact("ppocr_keys_v1.txt").openStream()) {
            ocrKeywords = Utils.readLines(is, true);
            ocrKeywords.add(0, "blank");
            if(useSpaceChar) {
                ocrKeywords.add(" ");
            } else {
                ocrKeywords.add("");
            }
        }
    }

    /**
     * process output
     * @param ctx
     * @param list
     * @return
     * @throws IOException
     */
    @Override
    public String processOutput(TranslatorContext ctx, NDList list) {
        var sb = new StringBuilder();
        var tokens = list.singletonOrThrow();
        long[] indices = tokens.get(0).argMax(1).toLongArray();
        boolean[] selection = new boolean[indices.length];
        Arrays.fill(selection, true);
        for (int i = 1; i < indices.length; i++) {
            if (indices[i] == indices[i - 1]) {
                selection[i] = false;
            }
        }

        // text recognition confidence
        /*
        float[] probs = new float[indices.length];
        for (int row = 0; row < indices.length; row++) {
            NDArray value = tokens.get(0).get(new NDIndex(""+ row +":" + (row + 1) +"," + indices[row] +":" + ( indices[row] + 1)));
            probs[row] = value.toFloatArray()[0];
        }
        */
        var lastIdx = 0;
        for (int i = 0; i < indices.length; i++) {
            if(selection[i]) {
                if(indices[i] > 0) {
                    if(!((i > 0) && (indices[i] == lastIdx))) {
                        sb.append(ocrKeywords.get((int) indices[i]));
                    }
                }
            }

//            if ((selection[i] == true) && (indices[i] > 0) && !(i > 0 && indices[i] == lastIdx)) {
//                sb.append(ocrKeywords.get((int) indices[i]));
//            }

        }
        return sb.toString();
    }

    /**
     * process input
     * @param ctx
     * @param input
     * @return
     */
    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        var img = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        var imgC = 3;
        var imgH = 48;
        //192 320
        var imgW = 320;

        var h = input.getHeight();
        var w = input.getWidth();
        var ratio = (float) w / (float) h;
        imgW = (int)(imgH * ratio);

        int resizedW;
        if (Math.ceil(imgH * ratio) > imgW) {
            resizedW = imgW;
        } else {
            resizedW = (int) (Math.ceil(imgH * ratio));
        }
        img = NDImageUtils.resize(img, resizedW, imgH);
        img = img.transpose(2, 0, 1).div(255).sub(0.5f).div(0.5f);
        var paddingIm = ctx.getNDManager().zeros(new Shape(imgC, imgH, imgW), DataType.FLOAT32);
        paddingIm.set(new NDIndex(":,:,0:" + resizedW), img);
        paddingIm = paddingIm.expandDims(0);
        return new NDList(paddingIm);
    }

    @Override
    public Batchifier getBatchifier() {
        return null;
    }

}

