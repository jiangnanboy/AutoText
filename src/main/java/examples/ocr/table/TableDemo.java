package examples.ocr.table;

import org.apache.commons.lang3.tuple.Pair;
import org.bytedeco.opencv.opencv_core.Mat;
import java.util.List;

import utils.image.ImageUtils;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import sy.ocr.table.rule.BorderedRecog;
import sy.ocr.table.rule.UnBorderedRecog;
import sy.ocr.table.rule.PartiallyBorderedRecog;


/**
 * @author sy
 * @date 2023/1/18 22:35
 */
public class TableDemo {
    public static void main(String...args) {
        borderedRecog();
//        unBorderedRecog();
//        partiallyBorderedRecog();
    }

    public static void borderedRecog() {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\bordered_example.png";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        List<List<List<Integer>>> resultList = BorderedRecog.recognizeStructure(imageMat);
        System.out.println(resultList);
//        ImageUtils.imshow("Image", pair.getRight());
    }

    public static void unBorderedRecog() {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\unbordered_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        List<List<List<Integer>>> resultList = UnBorderedRecog.recognizeStructure(imageMat);
        System.out.println(resultList);
//        ImageUtils.imshow("Image", pair.getRight());
    }

    public static void partiallyBorderedRecog() {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\partially_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        List<List<List<Integer>>> resultList = PartiallyBorderedRecog.recognizeStructure(imageMat);
        System.out.println(resultList);
//        ImageUtils.imshow("Image", pair.getRight());
    }


}
