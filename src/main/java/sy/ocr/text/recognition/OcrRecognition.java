package sy.ocr.text.recognition;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.Point;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.opencv.OpenCVImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import utils.common.CollectionUtil;
import utils.image.TextListBox;
import utils.image.ImageUtils;
import utils.cv.NDArrayUtils;
import utils.cv.OpenCVUtils;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sy
 * @date 2022/4/3 22:01
 */
public class OcrRecognition {

    public OcrRecognition() {
    }

    /**
     * load recognize model
     * @param modelPath
     * @param engineType
     * @return
     */
    public Criteria<Image, String> recognizeCriteria(Path modelPath, String engineType) {
        Criteria<Image, String> criteria =
                Criteria.builder()
                        .optEngine(engineType)
                        .setTypes(Image.class, String.class)
                        .optModelPath(modelPath)
                        .optProgress(new ProgressBar())
//                        .optOptions(Collections.singletonMap("use_mkldnn", "true"))
//                        .optOption("interOpNumThreads", "6")
//                        .optOption("intraOpNumThreads", "6")
                        .optTranslator(new OcrRecognitionTranslator((new ConcurrentHashMap<String, String>())))
                        .build();
        return criteria;
    }

    /**
     * predict
     * @param image
     * @param detector
     * @param recognizer
     * @return
     * @throws TranslateException
     */
    public List<TextListBox> predict(
            Image image, Predictor<Image, NDList> detector, Predictor<Image, String> recognizer)
            throws TranslateException {
        var boxes = detector.predict(image);
        List<TextListBox> resultBoxes = CollectionUtil.newArrayList();

        try(OpenCVFrameConverter.ToMat cv = new OpenCVFrameConverter.ToMat();
            OpenCVFrameConverter.ToMat converter1 = new OpenCVFrameConverter.ToMat();
            OpenCVFrameConverter.ToOrgOpenCvCoreMat converter2 = new OpenCVFrameConverter.ToOrgOpenCvCoreMat()) {
            for (int i = 0; i < boxes.size(); i++) {
                var box = boxes.get(i);
                float[] pointsArr = box.toFloatArray();
                float[] lt = java.util.Arrays.copyOfRange(pointsArr, 0, 2);
                float[] rt = java.util.Arrays.copyOfRange(pointsArr, 2, 4);
                float[] rb = java.util.Arrays.copyOfRange(pointsArr, 4, 6);
                float[] lb = java.util.Arrays.copyOfRange(pointsArr, 6, 8);
                var imgCropWidth = (int) Math.max(distance(lt, rt), distance(rb, lb));
                var imgCropHeight = (int) Math.max(distance(lt, lb), distance(rt, rb));
                List<Point> srcPoints = CollectionUtil.newArrayList();
                srcPoints.add(new Point(lt[0], lt[1]));
                srcPoints.add(new Point(rt[0], rt[1]));
                srcPoints.add(new Point(rb[0], rb[1]));
                srcPoints.add(new Point(lb[0], lb[1]));
                List<Point> dstPoints = CollectionUtil.newArrayList();
                dstPoints.add(new Point(0, 0));
                dstPoints.add(new Point(imgCropWidth, 0));
                dstPoints.add(new Point(imgCropWidth, imgCropHeight));
                dstPoints.add(new Point(0, imgCropHeight));

                var srcPoint2f = NDArrayUtils.toOpenCVPoint2f(srcPoints, 4);
                var dstPoint2f = NDArrayUtils.toOpenCVPoint2f(dstPoints, 4);

                var bufferedImage = OpenCVUtils.matToBufferedImage((org.opencv.core.Mat) image.getWrappedImage());

                org.bytedeco.opencv.opencv_core.Mat mat = cv.convertToMat(new Java2DFrameConverter().convert(bufferedImage));
                org.bytedeco.opencv.opencv_core.Mat dstMat = OpenCVUtils.perspectiveTransform(mat, srcPoint2f, dstPoint2f);
                org.opencv.core.Mat cvMat = converter2.convert(converter1.convert(dstMat));
                var subImg = OpenCVImageFactory.getInstance().fromImage(cvMat);

                subImg = subImg.getSubImage(0,0, imgCropWidth, imgCropHeight);
                if (subImg.getHeight() * 1.0 / subImg.getWidth() > 1.5) {
                    subImg = ImageUtils.rotateImg(subImg);
                }

                var text = recognizer.predict(subImg);
                List<Float> positionBoxList = CollectionUtil.newArrayList();
                Shape boxShape = box.getShape();
                for(int dimI=0; dimI<boxShape.get(0); dimI++) {
                    for(int dimJ=0; dimJ<boxShape.get(1); dimJ++) {
                        var boxPos = box.getFloat(dimI, dimJ);
                        positionBoxList.add(boxPos);
                    }
                }

                var resultBox = new TextListBox(positionBoxList, text);
                resultBoxes.add(resultBox);

                mat.release();
                dstMat.release();
                cvMat.release();
                srcPoint2f.releaseReference();
                dstPoint2f.releaseReference();
            }
        }

        return resultBoxes;
    }

    /**
     * distance between point1 and point2
     * @param point1
     * @param point2
     * @return
     */
    private float distance(float[] point1, float[] point2) {
        var disX = point1[0] - point2[0];
        var disY = point1[1] - point2[1];
        var dis = (float) Math.sqrt(disX * disX + disY * disY);
        return dis;
    }

}

