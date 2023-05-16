package examples.ocr.layout_detection;

import ai.onnxruntime.OrtException;
import com.alibaba.fastjson2.JSON;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import sy.ocr.layout_detection.Detection;
import sy.ocr.layout_detection.LayoutDetectionUtil;
import utils.common.PropertiesReader;

import sy.ocr.layout_detection.LayoutDet;

import java.util.List;

/**
 * @author sy
 * @date 2023/5/2 15:49
 */
public class LayoutDetection {
        public static void main(String...args) {
        String modelPath = LayoutDetection.class.getClassLoader().getResource(PropertiesReader.get("table_det_model_path")).getPath().replaceFirst("/", "");
        String labelPath = LayoutDetection.class.getClassLoader().getResource(PropertiesReader.get("table_det_labels_path")).getPath().replaceFirst("/", "");
        String imgPath = "examples\\ocr\\img_test\\layout_img.webp";

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

                LayoutDetectionUtil.drawPredictions(img, detectionList);
                System.out.println(JSON.toJSONString(detectionList));
                Imgcodecs.imwrite("examples\\ocr\\output\\layout_img_result.jpg", img);
            } catch (OrtException ortException) {
                ortException.printStackTrace();
            }

        } catch (OrtException e) {
            e.printStackTrace();
        }
    }

}
