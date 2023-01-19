package utils.image;

import java.util.List;

/**
 * @author sy
 * @date 2022/1/3 23:04
 */
public class TextListBox {
    private List<Float> box;
    private String text;

    public List<Float> getBox() {
        return box;
    }

    public void setBox(List<Float> box) {
        this.box = box;
    }

    public TextListBox(List<Float> box, String text) {
        this.box = box;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "position: " + this.getBox() + ", text: " + this.getText();
    }
}
