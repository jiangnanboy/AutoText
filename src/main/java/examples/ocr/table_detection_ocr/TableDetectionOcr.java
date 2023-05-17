package examples.ocr.table_detection_ocr;

import ai.onnxruntime.OrtException;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import sy.ocr.layout_detection.Detection;
import sy.ocr.layout_detection.LayoutDet;
import sy.ocr.layout_detection.LayoutDetectionUtil;
import sy.ocr.table.rule.BorderedRecog;
import utils.common.PropertiesReader;
import utils.cv.OpenCVUtils;

import java.awt.image.BufferedImage;
import java.util.List;

import static utils.cv.OpenCVUtils.bufferedImageToMat;

/**
 * @author sy
 * @date 2023/5/1 15:49
 */
public class TableDetectionOcr {
        public static void main(String...args) {
        String modelPath = TableDetectionOcr.class.getClassLoader().getResource(PropertiesReader.get("table_det_model_path")).getPath().replaceFirst("/", "");
        String labelPath = TableDetectionOcr.class.getClassLoader().getResource(PropertiesReader.get("table_det_labels_path")).getPath().replaceFirst("/", "");
        String imgPath = "examples\\ocr\\img_test\\raw.jpg";

        try {
            LayoutDet modelDet = new LayoutDet(modelPath, labelPath);
            Mat img = Imgcodecs.imread(imgPath);
            if (img.dataAddr() == 0) {
                System.out.println("Could not open image: " + imgPath);
                System.exit(1);
            }
            // run detection
            try {
                List<Detection> detectionList = modelDet.detectObjects(img);
                for(Detection detection : detectionList) {
                    if(StringUtils.equals(detection.getLabel(), "Table")) {
                        float[] bbox = detection.getBbox();
                        Rect rect = new Rect(new Point(bbox[0], bbox[1]),
                                             new Point(bbox[2], bbox[3]));
                        Mat tableImg = img.submat(rect);
                        BufferedImage bufferedImage = OpenCVUtils.matToBufferedImage(tableImg);

                        List<List<List<Integer>>> resultList = BorderedRecog.recognizeStructure(bufferedImageToMat(bufferedImage));
                        System.out.println(resultList);
                        System.out.println(resultList.size());
                        for(List<List<Integer>> listlist : resultList) {
                            System.out.println(listlist.size());
                        }
                    }
                }
                LayoutDetectionUtil.drawPredictions(img, detectionList);
                System.out.println(JSON.toJSONString(detectionList));
                Imgcodecs.imwrite("examples\\predictions.jpg", img);
            } catch (OrtException ortException) {
                ortException.printStackTrace();
            }

        } catch (OrtException e) {
            e.printStackTrace();
        }
    }



}
