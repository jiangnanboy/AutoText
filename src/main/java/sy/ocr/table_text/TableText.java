package sy.ocr.table_text;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.translate.TranslateException;
import org.bytedeco.javacpp.indexer.ByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sy.ocr.text.OcrApp;
import utils.common.CollectionUtil;
import utils.cv.NDArrayUtils;
import utils.engine.EngineConstant;
import utils.image.ImageUtils;
import utils.image.TextListBox;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import sy.ocr.table.rule.BorderedRecog;
import sy.ocr.table.rule.UnBorderedRecog;
import sy.ocr.table.rule.PartiallyBorderedRecog;

/**
 * @author sy
 * @date 2023/1/31 22:01
 */
public class TableText {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableText.class);

    OcrApp ocrApp;

    public TableText() {
        ocrApp = new OcrApp();
    }

    public TableText(String detectionModelFile, String recognitionModelFile) {
        this(detectionModelFile, recognitionModelFile, EngineConstant.ENGINE_ONNX);
    }

    public TableText(String detectionModelFile, String recognitionModelFile, String engineType) {
        ocrApp = new OcrApp(detectionModelFile, recognitionModelFile, engineType);
        ocrApp.init();
    }

    public TableText(Path detectionModelPath, Path recognitionModelPath) {
        this(detectionModelPath, recognitionModelPath, EngineConstant.ENGINE_ONNX);
    }

    public TableText(Path detectionModelPath, Path recognitionModelPath, String engineType) {
        ocrApp = new OcrApp(detectionModelPath, recognitionModelPath, engineType);
        ocrApp.init();
    }

    /**
     *  text ocr
     * @param imagePath
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> textOcr(String imagePath) throws IOException, TranslateException {
        // read image file
        var imageFile = Paths.get(imagePath);
        var image = ImageFactory.getInstance().fromFile(imageFile);
        // predict result
        List<TextListBox> resultList = ocrApp.ocr(image);
        return resultList;
    }

    /**
     *  text ocr
     * @param image
     * @return
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> textOcr(Image image) throws TranslateException {
        // predict result
        List<TextListBox> resultList = ocrApp.ocr(image);
        return resultList;
    }

    /**
     * table recognition
     * @param imagePath
     * @return
     */
    public List<List<List<Integer>>> tableRecg(String imagePath) {
        return tableRecg(imread(imagePath), 0);
    }

    /**
     * table recognition
     * @param imageMat
     * @return
     */
    public List<List<List<Integer>>> tableRecg(Mat imageMat) {
        return tableRecg(imageMat, 0);
    }

    /**
     * table recognition
     * @param imageMat
     * @param borderType {0:bordered (default), 1:unbordered, 2:partiallybordered}
     * @return
     */
    public List<List<List<Integer>>> tableRecg(Mat imageMat, int borderType) {
        // predict result
        List<List<List<Integer>>> resultList = null;
        switch (borderType) {
            case 0:
                resultList = BorderedRecog.recognizeStructure(imageMat);
                break;
            case 1:
                resultList = UnBorderedRecog.recognizeStructure(imageMat);
                break;
            case 2:
                resultList = PartiallyBorderedRecog.recognizeStructure(imageMat);
                break;
            default:
                LOGGER.error("Please check 'borderType' in '[0->bordered ,1->unbordered ,2->partiallybordered]' !");
        }
        return resultList;
    }

    public List<TextListBox> TableTextRecog(Image image) throws IOException, TranslateException {
        return this.TableTextRecog(image, -1);
    }

    /**
     * table and text recognition
     * @param image
     * @param maxSideLen
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> TableTextRecog(Image image, int maxSideLen) throws IOException, TranslateException {
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
        }
        List<TextListBox> textList = this.textOcr(image);
        var mat = NDArrayUtils.image2Mat2(image);
        List<List<List<Integer>>> tableList = this.tableRecg(mat);
        List<TextListBox> resultList = this.getTableTextResult(textList, tableList);
        return resultList;

    }

    public List<TextListBox> TableTextRecog(String imagePath) throws IOException, TranslateException {
        return this.TableTextRecog(imagePath, -1);
    }

    /**
     * table and text recognition
     * @param imagePath
     * @param maxSideLen
     * @throws IOException
     * @throws TranslateException
     */
    public List<TextListBox> TableTextRecog(String imagePath, int maxSideLen) throws IOException, TranslateException {
        var imageFile = Paths.get(imagePath);
        var image = ImageFactory.getInstance().fromFile(imageFile);
        if(-1 != maxSideLen) {
            image = ImageUtils.imageResize(image, maxSideLen);
        }
        List<TextListBox> textList = this.textOcr(image);
        var mat = NDArrayUtils.image2Mat2(image);
        List<List<List<Integer>>> tableList = this.tableRecg(mat);
        List<TextListBox> resultList = this.getTableTextResult(textList, tableList);
        return resultList;
    }

    /**
     * get table text ocr result
     * @param textList
     * @param tableList
     */
    public List<TextListBox> getTableTextResult(List<TextListBox> textList, List<List<List<Integer>>> tableList) {
        List<TextListBox> tableTextList = CollectionUtil.newArrayList();
        for(List<List<Integer>> listlistInt : tableList) {
            for(List<Integer> listInt : listlistInt) {
                if(listInt.size() == 4) {
                    int x = listInt.get(0);
                    int y = listInt.get(1);
                    int w = listInt.get(2);
                    int h = listInt.get(3);
                    int xMinCell = x;
                    int yMinCell = y;
                    int xMaxCell = x + w;
                    int yMaxCell = y + h;
                    StringBuilder stringBuilder = new StringBuilder();
                    for(TextListBox textListBox : textList) {
                        String text = textListBox.getText();
                        List<Float> textPosition = textListBox.getBox();
                        float xMinText = textPosition.get(0);
                        float yMinText = textPosition.get(1);
                        float xMaxText = textPosition.get(4);
                        float yMaxText = textPosition.get(5);

                        // find the center of the text box
                        float textMiddleX = (xMaxText + xMinText) / 2;
                        float textMiddleY = (yMaxText + yMinText) / 2;
                        if((textMiddleX > xMinCell) && (textMiddleY > yMinCell)) {
                            if((textMiddleX < xMaxCell) && (textMiddleY < yMaxCell)) {
                                stringBuilder.append(text).append("\n");
                            }
                        }


//                        float height = yMaxText - yMinText;
//                        float width = xMaxText - xMinText;
//                        if((xMinCell <= (0.1 * width + xMinText)) && (yMinCell <= (0.1 * height + yMinText))) {
//                            if((xMaxCell >= (xMaxText - 0.1 * width)) && (yMaxCell >= (yMaxText - 0.1 * height))) {
//                                stringBuilder.append(text).append("\n");
//                            }
//                        }



//                        if((xMinText >= xMinCell) && (yMinText >= yMinCell)) {
//                            if((xMaxText <= xMaxCell) && (yMaxText <= yMaxCell)) {
//                                stringBuilder.append(text).append("\n");
//                            }
//                        }


                    }
                    List<Float> tablePosition = CollectionUtil.newArrayList();
                    tablePosition.add((float) xMinCell);
                    tablePosition.add((float) yMinCell);
                    tablePosition.add((float) xMaxCell);
                    tablePosition.add((float) yMinCell);
                    tablePosition.add((float) xMaxCell);
                    tablePosition.add((float) yMaxCell);
                    tablePosition.add((float) xMinCell);
                    tablePosition.add((float) yMaxCell);
                    TextListBox textListBox = new TextListBox(tablePosition, stringBuilder.toString());
                    tableTextList.add(textListBox);
                }
            }
        }
        return tableTextList;
    }

    /**
     * bufferedimage to mat
     * @param img
     * @return
     */
    public Mat image2Mat(Image img) {
        var bufferedImage = (BufferedImage) img.getWrappedImage();
        var width = bufferedImage.getWidth();
        var height = bufferedImage.getHeight();
        byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        var mat = new Mat(height, width, opencv_core.CV_8UC3);
        ByteIndexer byteIndexer = mat.createIndexer();
        byteIndexer.put(0, 0, data);
        return mat;
    }

}

