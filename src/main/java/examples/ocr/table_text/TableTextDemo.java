package examples.ocr.table_text;

import ai.djl.translate.TranslateException;
import org.bytedeco.opencv.opencv_core.Mat;
import sy.ocr.table.rule.BorderedRecog;
import sy.ocr.table.rule.PartiallyBorderedRecog;
import sy.ocr.table.rule.UnBorderedRecog;
import sy.ocr.table_text.TableText;
import utils.image.TextListBox;

import java.io.IOException;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;


/**
 * @author sy
 * @date 2023/1/18 22:35
 */
public class TableTextDemo {
    public static void main(String...args) throws IOException, TranslateException {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\bordered_example.png";
        TableText tableText = new TableText();
        /**
         * maxSideLen:image resize
         *
         * borderType:{0:all, 1:bordered(default), 2:unbordered, 3:partiallybordered}
         */
        int maxSideLen = -1; // default, no resize
        int borderType = 1; // default, bordered
        List<TextListBox> listBoxes = tableText.tableTextRecog(imagePath);
        for(TextListBox textListBox : listBoxes) {
            System.out.print(textListBox);
        }

        maxSideLen = 1024; // image resize
        borderType = 0; // all
        listBoxes = tableText.tableTextRecog(imagePath,maxSideLen, borderType);
        for(TextListBox textListBox : listBoxes) {
            System.out.print(textListBox);
        }
    }
}
