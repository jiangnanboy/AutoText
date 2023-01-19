package sy.ocr.text;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import sy.ocr.text.detection.OcrDetection;
import sy.ocr.text.recognition.OcrRecognition;
import utils.engine.EngineConstant;
import utils.image.ImageUtils;
import utils.image.TextListBox;
import utils.cv.OpenCVUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * @author sy
 * @date 2022/1/14 19:01
 */
public class OcrApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrApp.class);

    private ZooModel detectionModel;
    private ZooModel recognitionModel;
    private Predictor<Image, NDList> detector;
    private Predictor<Image, String> recognizer;
    private Path detectionModelPath;
    private Path recognitionModelPath;
    private OcrDetection detection;
    private OcrRecognition recognition;
    private String engineType;

    public OcrApp(Path detectionModelPath, Path recognitionModelPath) {
        this(detectionModelPath, recognitionModelPath, EngineConstant.ENGINE_ONNX);
    }

    public OcrApp(Path detectionModelPath, Path recognitionModelPath, String engienType) {
        this.detectionModelPath = detectionModelPath;
        this.recognitionModelPath = recognitionModelPath;
        this.engineType = engienType;
    }

    /**
     * init ocr model
     */
    public void init() {
        this.detection = new OcrDetection();
        this.recognition = new OcrRecognition();
        try {
            this.detectionModel = ModelZoo.loadModel(this.detection.detectCriteria(this.detectionModelPath, this.engineType));
            this.detector =  this.detectionModel.newPredictor();
            this.recognitionModel = ModelZoo.loadModel(this.recognition.recognizeCriteria(this.recognitionModelPath, this.engineType));
            this.recognizer =  this.recognitionModel.newPredictor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ModelNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedModelException e) {
            e.printStackTrace();
        }
        LOGGER.info("Init ocr model done!");
    }

    /**
     * close all model
     */
    public void closeAllModel() {
        if(Optional.ofNullable(this.recognizer).isPresent()) {
            this.recognizer.close();
        }
        if(Optional.ofNullable(this.recognitionModel).isPresent()) {
            this.recognitionModel.close();
        }
        if(Optional.ofNullable(this.detector).isPresent()) {
            this.detector.close();
        }
        if(Optional.ofNullable(this.detectionModel).isPresent()) {
            this.detectionModel.close();
        }
        LOGGER.info("Close ocr model done!");
    }

    /**
     * image ocr
     * @param imagePath
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> ocr(String imagePath) throws IOException, TranslateException {
        return ocr(imagePath, -1);
    }

    /**
     * image ocr
     * @param imagePath
     * @param maxSideLen
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> ocr(String imagePath, int maxSideLen) throws IOException, TranslateException {
        var imageFile = Paths.get(imagePath);
        var image = ImageFactory.getInstance().fromFile(imageFile);
        LOGGER.info("Load image from -> " + imagePath + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
            LOGGER.info("Resize image from -> " + imagePath + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        }
        List<TextListBox> detectionResult = this.recognition.predict(image, this.detector, this.recognizer);
        return detectionResult;
    }

    /**
     * image ocr
     * @param image
     * @return
     * @throws TranslateException
     */
    public List<TextListBox> ocr(Image image) throws TranslateException {
        return ocr(image, -1);
    }

    /**
     * image ocr
     * @param image
     * @param maxSideLen
     * @return
     * @throws TranslateException
     */
    public List<TextListBox> ocr(Image image, int maxSideLen) throws TranslateException {
        LOGGER.info("Load image -> " + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
            LOGGER.info("Resize image -> " + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        }
        List<TextListBox> detectionResult = this.recognition.predict(image, this.detector, this.recognizer);
        return detectionResult;
    }

    /**
     * image ocr
     * @param imagePath
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public Pair<List<TextListBox>, Image> ocrImage(String imagePath) throws IOException, TranslateException {
        return ocrImage(imagePath, -1);
    }

    /**
     * image ocr
     * @param imagePath
     * @param maxSideLen
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public Pair<List<TextListBox>, Image> ocrImage(String imagePath, int maxSideLen) throws IOException, TranslateException {
        var imageFile = Paths.get(imagePath);
        var image = ImageFactory.getInstance().fromFile(imageFile);
        LOGGER.info("Load image from -> " + imagePath + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
            LOGGER.info("Resize image from -> " + imagePath + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        }
        List<TextListBox> detectionResult = this.recognition.predict(image, this.detector, this.recognizer);
        return Pair.of(detectionResult, image);
    }

    /**
     * image ocr
     * @param image
     * @return
     * @throws TranslateException
     */
    public Pair<List<TextListBox>, Image> ocrImage(Image image) throws TranslateException {
        return ocrImage(image, -1);
    }

    /**
     * image ocr
     * @param image
     * @param maxSideLen
     * @return
     * @throws TranslateException
     */
    public Pair<List<TextListBox>, Image> ocrImage(Image image, int maxSideLen) throws TranslateException {
        LOGGER.info("Load image -> " + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
            LOGGER.info("Resize image -> " + "；height: " + image.getHeight() + "；width: " + image.getWidth());
        }
        List<TextListBox> detectionResult = this.recognition.predict(image, this.detector, this.recognizer);
        return Pair.of(detectionResult, image);
    }

    /**
     * save ocr results as image
     * @param textBoxList
     * @param image
     * @param saveImageName
     * @param saveImagePath
     */
    public void saveImageOcrResult(List<TextListBox> textBoxList, Image image, String saveImageName, String saveImagePath) {
        var bufferedImage = OpenCVUtils.mat2Image((org.opencv.core.Mat) image.getWrappedImage());
        bufferedImage = ImageUtils.drawTextListResults(bufferedImage, textBoxList);
        image = ImageFactory.getInstance().fromImage(OpenCVUtils.image2Mat(bufferedImage));
        ImageUtils.saveImage(image, saveImageName, saveImagePath);
        LOGGER.info("Save ocr results as image path :" + saveImagePath + "\\" + saveImageName);
    }

    /**
     * save ocr results as image
     * @param ocrImagePair
     * @param saveImageName
     * @param saveImagePath
     */
    public void saveImageOcrResult(Pair<List<TextListBox>, Image> ocrImagePair, String saveImageName, String saveImagePath) {
        List<TextListBox> textBoxList = ocrImagePair.getLeft();
        Image image = ocrImagePair.getRight();
        saveImageOcrResult(textBoxList, image, saveImageName, saveImagePath);
    }

}

