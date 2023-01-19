package utils.image;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author sy
 * @date 2022/1/3 22:01
 */
public class ImageUtils {

    private static final Map<Integer, Color> COLOR_MAP = Map.of(
            0, new Color(200, 0, 0),
            1, new Color(0, 200, 0),
            2, new Color(0, 0, 200),
            3, new Color(200, 200, 0),
            4, new Color(200, 0, 200),
            5, new Color(0, 200, 200)
    );

    /**
     * convert BufferedImage to DJL Image
     * @param img
     * @return
     */
    public static Image convert(BufferedImage img) {
        return ImageFactory.getInstance().fromImage(img);
    }

    /**
     * save BufferedImage
     * @param img
     * @param name
     * @param path
     */
    public static void saveImage(BufferedImage img, String name, String path) {
        var djlImg = ImageFactory.getInstance().fromImage(img);
        var outputDir = Paths.get(path);
        var imagePath = outputDir.resolve(name);
        try {
            djlImg.save(Files.newOutputStream(imagePath), "png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save DJL Image
     * @param img
     * @param name
     * @param path
     */
    public static void saveImage(Image img, String name, String path) {
        var outputDir = Paths.get(path);
        var imagePath = outputDir.resolve(name);
        try {
            img.save(Files.newOutputStream(imagePath), "png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save image, including the detection box
     * @param img
     * @param detection
     * @param name
     * @param path
     * @throws IOException
     */
    public static void saveBoundingBoxImage(Image img, DetectedObjects detection, String name, String path) throws IOException {
        // Make image copy with alpha channel because original image was jpg
        img.drawBoundingBoxes(detection);
        var outputDir = Paths.get(path);
        Files.createDirectories(outputDir);
        var imagePath = outputDir.resolve(name);
        // OpenJDK can't save jpg with alpha channel
        img.save(Files.newOutputStream(imagePath), "png");
    }

    /**
     * draw detection box (with inclination Angle)
     * @param image
     * @param box
     */
    public static void drawImageRect(BufferedImage image, NDArray box) {
        float[] points = box.toFloatArray();
        int[] xPoints = new int[5];
        int[] yPoints = new int[5];

        for (int i = 0; i < 4; i++) {
            xPoints[i] = (int) points[2 * i];
            yPoints[i] = (int) points[2 * i + 1];
        }
        xPoints[4] = xPoints[0];
        yPoints[4] = yPoints[0];

        // convert image to Graphics2D
        var g = (Graphics2D) image.getGraphics();
        try {
            g.setColor(new Color(0, 255, 0));
            //declare brush properties
            BasicStroke bStroke = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            g.setStroke(bStroke);
            // xPoints, yPoints, nPoints
            g.drawPolyline(xPoints, yPoints, 5);
        } finally {
            g.dispose();
        }
    }

    /**
     * draw detection box (with inclination Angle) and text
     * @param image
     * @param box
     * @param text
     */
    public static void drawImageRectWithText(BufferedImage image, NDArray box, String text) {
        float[] points = box.toFloatArray();
        int[] xPoints = new int[5];
        int[] yPoints = new int[5];

        for (int i = 0; i < 4; i++) {
            xPoints[i] = (int) points[2 * i];
            yPoints[i] = (int) points[2 * i + 1];
        }
        xPoints[4] = xPoints[0];
        yPoints[4] = yPoints[0];

        // convert image to Graphics2D
        var g = (Graphics2D) image.getGraphics();
        try {
            var fontSize = 32;
            var font = new Font("楷体", Font.PLAIN, fontSize);
            g.setFont(font);
            g.setColor(new Color(0, 0, 255));
            // declare brush properties
            BasicStroke bStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            g.setStroke(bStroke);
            g.drawPolyline(xPoints, yPoints, 5); // xPoints, yPoints, nPoints
            g.drawString(text, xPoints[0], yPoints[0]);
        } finally {
            g.dispose();
        }
    }

    /**
     * draw ocr results
     * @param image
     * @param resultBoxList
     * @return
     */
    public static BufferedImage drawTextListResults(BufferedImage image, List<TextListBox> resultBoxList) {
        var g = (Graphics2D) image.getGraphics();
        var stroke = 2;
        g.setStroke(new BasicStroke(stroke));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for(TextListBox resultBox:resultBoxList) {
            var text = resultBox.getText();
            var ndArray = resultBox.getBox();
            var color = COLOR_MAP.get(Math.abs(text.hashCode() % 6));
            g.setPaint(color);
            float x = ndArray.get(0);
            var xInt = (int)x;
            float y = ndArray.get(1);
            var yInt = (int)y;
            var width = (int) (ndArray.get(2) - x);
            var height = (int) (ndArray.get(7) - y);
            g.drawRect(xInt, yInt, width, height);
            drawText(g, text, xInt, yInt, width);
        }
        g.dispose();
        return image;
    }

    /**
     * draw text
     * @param g
     * @param className
     * @param x
     * @param y
     * @param width
     */
    private static void drawText(Graphics2D g, String className, int x, int y, int width) {
        //set the coordinates of the watermark
        var showText = String.format("%s", className);
//        g.fillRect(x, y - 30, width, 30);
        g.setColor(Color.red);
        //set font
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString(showText, x, y - 10);
    }

    /**
     * draw detection box
     * @param image
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public static void drawImageRect(BufferedImage image, int x, int y, int width, int height) {
        // convert image to Graphics2D
        var g = (Graphics2D) image.getGraphics();
        try {
            g.setColor(new Color(0, 255, 0));
            // declare brush properties
            BasicStroke bStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            g.setStroke(bStroke);
            g.drawRect(x, y, width, height);
        } finally {
            g.dispose();
        }
    }

    /**
     * draw detection box
     * @param image
     * @param x
     * @param y
     * @param width
     * @param height
     * @param c
     */
    public static void drawImageRect(BufferedImage image, int x, int y, int width, int height, Color c) {
        // convert image to Graphics2D
        var g = (Graphics2D) image.getGraphics();
        try {
            g.setColor(c);
            // declare brush properties
            BasicStroke bStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            g.setStroke(bStroke);
            g.drawRect(x, y, width, height);

        } finally {
            g.dispose();
        }
    }

    /**
     * resize image
     * @param image
     * @param width
     * @param height
     * @param ratio
     * @return
     */
    public static Image imageResize(Image image, int width, int height, float ratio) {
        if((image.getHeight() > height) && (image.getWidth() > width)) {
            try (var manager = NDManager.newBaseManager()) {
                var ndArray = image.toNDArray(manager);
                ndArray = NDImageUtils.resize(ndArray, (int)(ratio*width), (int)(ratio*height), Image.Interpolation.BICUBIC);
                image = ImageFactory.getInstance().fromNDArray(ndArray.toType(DataType.UINT8, false));
            }
        }
        return image;
    }

    /**
     * resize image
     * @param input
     * @param maxSideLen
     * @return
     */
    public static Image imageResize(Image input, int maxSideLen) {
        try(var manager = NDManager.newBaseManager()) {
            var img = input.toNDArray(manager);
            var h = input.getHeight();
            var w = input.getWidth();
            var resizeW = w;
            var resizeH = h;
            // limit the max side
            var ratio = 1.0f;
            if(Math.max(resizeH, resizeW) > maxSideLen) {
                if(resizeH > resizeW) {
                    ratio = (float)maxSideLen / resizeH;
                } else {
                    ratio = (float)maxSideLen / resizeW;
                }
            }
            resizeH = (int) (resizeH * ratio);
            resizeW = (int) (resizeW * ratio);
            if(resizeH % 32 == 0) {
//                resizeH = resizeH;
            } else if(Math.floor((float) resizeH / 32) <= 1) {
                resizeH = 32;
            } else {
                resizeH = (int) Math.floor((float) resizeH / 32) * 32;
            }
            if(resizeW % 32 == 0) {
//                resizeW = resizeW;
            } else if(Math.floor((float) resizeW / 32) <= 1) {
                resizeW = 32;
            } else {
                resizeW = (int) Math.floor((float) resizeW / 32) * 32;
            }
            var resizeIDArray = NDImageUtils.resize(img, resizeW, resizeH, Image.Interpolation.BICUBIC);
            input = ImageFactory.getInstance().fromNDArray(resizeIDArray.toType(DataType.UINT8, false));
        }
        return input;
    }

    /**
     * get X
     * @param img
     * @param box
     * @param x
     * @return
     */
    private static int getX(Image img, BoundingBox box, float x) {
        var rect = box.getBounds();
        // pointLeftTop
        var x1 = (int) (rect.getX() * img.getWidth());
        // width
        var w = (int) (rect.getWidth() * img.getWidth());
        return (int) (x * w + x1);
    }

    /**
     * get Y
     * @param img
     * @param box
     * @param y
     * @return
     */
    private static int getY(Image img, BoundingBox box, float y) {
        var rect = box.getBounds();
        // pointLeftTop
        var y1 = (int) (rect.getY() * img.getHeight());
        // height
        var h = (int) (rect.getHeight() * img.getHeight());
        return (int) (y * h + y1);
    }

    /**
     * rotate img
     * @param image
     * @return
     */
    public static Image rotateImg(Image image) {
        try (var manager = NDManager.newBaseManager()) {
            var rotated = NDImageUtils.rotate90(image.toNDArray(manager), 1);
            return ImageFactory.getInstance().fromNDArray(rotated);
        }
    }

    /**
     * show image
     * @param txt
     * @param img
     */
    public static void imshow(String txt, Mat img) {
        CanvasFrame canvasFrame = new CanvasFrame(txt);
        canvasFrame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        canvasFrame.setCanvasSize(img.cols(), img.rows());
        canvasFrame.showImage(new OpenCVFrameConverter.ToMat().convert(img));
    }

}

