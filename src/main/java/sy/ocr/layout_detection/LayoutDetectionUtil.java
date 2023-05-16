package sy.ocr.layout_detection;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import utils.common.CollectionUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2023/5/2 13:48
 */
public class LayoutDetectionUtil {

    public static float[] whc2cwh(float[] src) {
        float[] chw = new float[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ++ch) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }

    private static final Map<Integer, Scalar> COLOR_MAP = Map.of(
            0, new Scalar(220, 50, 0),
            1, new Scalar(0, 200, 0),
            2, new Scalar(0, 0, 200),
            3, new Scalar(200, 200, 0),
            4, new Scalar(200, 0, 200),
            5, new Scalar(0, 200, 200),
            6, new Scalar(200, 100, 60),
            7, new Scalar(60, 50, 249),
            8, new Scalar(10, 60, 249),
            9, new Scalar(60, 100, 10)
    );

    public static void drawPredictions(Mat img, List<Detection> detectionList) {
        // debugging image
        for (Detection detection : detectionList) {
            float[] bbox = detection.getBbox();
            Scalar color = COLOR_MAP.get(detection.getLabelIndex());
            Imgproc.rectangle(img,                    //Matrix obj of the image
                    new Point(bbox[0], bbox[1]),        //p1
                    new Point(bbox[2], bbox[3]),       //p2
                    color,     //Scalar object for color
                    2                        //Thickness of the line
            );
            Imgproc.putText(
                    img,
                    detection.getLabel(),
                    new Point(bbox[0] - 1, bbox[1] - 5),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    .5, color,
                    1);
        }
    }
    public static void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];
        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {
        // output boxes
        List<float[]> bestBboxes = CollectionUtil.newArrayList();
        // confidence
        bboxes.sort(Comparator.comparing(a -> a[4]));
        // standard nms
        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }
        return bestBboxes;
    }

    public static float computeIOU(float[] box1, float[] box2) {
        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);
    }

}
