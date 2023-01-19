package sy.ocr.table.rule;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import utils.common.CollectionUtil;

import java.util.List;
import java.util.Map;

import static utils.common.CommomUtil.sortByValue;

/**
 * @author YanShi
 * @date 2023/1/19 23:12
 */
public class util {
    public static void sortContours(MatVector contours) {
        sortContours(contours, "left-to-right");
    }

    /**
     * contours sorted
     * @param contours
     * @param method
     * @return
     */
    public static Pair<List<Rect>, Map<Rect, Integer>> sortContours(MatVector contours, String method) {
        // initialize the reverse flag and sort index
        boolean reverse = false;
        int i = 0;
        // handle if we need to sort in reverse
        if(StringUtils.equals(method, "right-to-left") || StringUtils.equals(method, "bottom-to-top")) {
            reverse = true;
        }
        // handle if we are sorting against the y-coordinate rather than the x-coordinate of the bounding box
        if(StringUtils.equals(method, "top-to-bottom") || StringUtils.equals(method, "bottom-to-top")) {
            i = 1;
        }
        // construct the list of bounding boxes and sort them from top to bottom
        List<Rect> boundingBoxes = CollectionUtil.newArrayList();
        Map<Rect, Integer> rectIntegerMap = CollectionUtil.newHashMap();
        for(int index=0; index<contours.size(); index++) {
            Mat mat = contours.get(index);
            Rect rect = opencv_imgproc.boundingRect(mat);
            boundingBoxes.add(rect);
            if(0 == i) {
                rectIntegerMap.put(rect, rect.x());
            } else {
                rectIntegerMap.put(rect, rect.y());
            }
        }
        Map<Rect, Integer> resultRectMap = sortByValue(rectIntegerMap, reverse);
        return Pair.of(boundingBoxes, resultRectMap);
    }
}
