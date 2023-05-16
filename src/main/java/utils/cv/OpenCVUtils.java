package utils.cv;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * @author sy
 * @date 2022/1/3 21:01
 */
public class OpenCVUtils {

    /**
     * perspective transform
     * @param src
     * @param srcPoints
     * @param dstPoints
     * @return
     */
    public static Mat perspectiveTransform(Mat src, Point2f srcPoints, Point2f dstPoints) {
        var dst = src.clone();
        var warpMat = opencv_imgproc.getPerspectiveTransform(srcPoints.position(0), dstPoints.position(0));
        opencv_imgproc.warpPerspective(src, dst, warpMat, dst.size());
        warpMat.release();
        return dst;
    }

    /**
     * mat to bufferedimage
     * @param mat
     * @return
     */
    public static BufferedImage mat2Image(org.opencv.core.Mat mat) {
        var width = mat.width();
        var height = mat.height();
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        Imgproc.cvtColor(mat, mat, 4);
        mat.get(0, 0, data);
        var ret = new BufferedImage(width, height, 5);
        ret.getRaster().setDataElements(0, 0, width, height, data);
        return ret;
    }

    /**
     * mat to bufferedimage
     * @param frame
     * @return
     */
    public static BufferedImage matToBufferedImage(org.opencv.core.Mat frame) {
        var type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        var image = new BufferedImage(frame.width(), frame.height(), type);
        var raster = image.getRaster();
        var dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);
        return image;
    }

    /**
     * bufferedimage to mat
     * @param img
     * @return
     */
    public static org.opencv.core.Mat image2Mat(BufferedImage img) {
        var width = img.getWidth();
        var height = img.getHeight();
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        org.opencv.core.Mat mat = new org.opencv.core.Mat(height, width, CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    public static org.bytedeco.opencv.opencv_core.Mat bufferedImageToMat(BufferedImage bi) {
        OpenCVFrameConverter.ToMat cv = new OpenCVFrameConverter.ToMat();
        return cv.convertToMat(new Java2DFrameConverter().convert(bi));
    }

}
