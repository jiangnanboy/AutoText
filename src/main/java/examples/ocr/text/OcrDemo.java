package examples.ocr.text;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.translate.TranslateException;
import sy.ocr.text.OcrApp;
import utils.common.PropertiesReader;
import utils.image.TextListBox;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author sy
 * @date 2022/12/30 22:25
 */
public class OcrDemo {
    public static void main(String[] args) throws IOException, TranslateException {
        // read image file
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\text_example.png";
        var imageFile = Paths.get(imagePath);
        var image = ImageFactory.getInstance().fromFile(imageFile);

        // init model
        String detectionModelFile = OcrDemo.class.getClassLoader().getResource(PropertiesReader.get("text_recog_det_model_path")).getPath().replaceFirst("/", "");
        String recognitionModelFile = OcrDemo.class.getClassLoader().getResource(PropertiesReader.get("text_recog_rec_model_path")).getPath().replaceFirst("/", "");
        Path detectionModelPath = Paths.get(detectionModelFile);
        Path recognitionModelPath = Paths.get(recognitionModelFile);
        OcrApp ocrApp = new OcrApp(detectionModelPath, recognitionModelPath);
        ocrApp.init();

        // predict result and consume time
        var timeInferStart = System.currentTimeMillis();
        Pair<List<TextListBox>, Image> imagePair = ocrApp.ocrImage(image, 960);
        System.out.println("consume time: " + (System.currentTimeMillis() - timeInferStart)/1000.0 + "s");
        for (var result : imagePair.getLeft()) {
                System.out.println(result);
        }
        // save ocr result image
        ocrApp.saveImageOcrResult(imagePair, "ocr_result.png", "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\output");
        ocrApp.closeAllModel();
    }
}

