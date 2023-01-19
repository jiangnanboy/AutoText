package utils.cv;

import ai.djl.ndarray.NDArray;
import org.bytedeco.javacpp.indexer.DoubleRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;

import java.util.List;

/**
 * @author sy
 * @date 2023/1/3 21:01
 */
public class NDArrayUtils {

    /**
     * convert NDArray to opencv_core.Mat
     * @param points
     * @param rows
     * @param cols
     * @return
     */
    public static Mat toOpenCVMat(NDArray points, int rows, int cols) {
        double[] doubleArray = points.toDoubleArray();
        // CV_32F = FloatRawIndexer
        // CV_64F = DoubleRawIndexer
        var mat = new Mat(rows, cols, opencv_core.CV_64F);

        DoubleRawIndexer ldIdx = mat.createIndexer();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                ldIdx.put(i, j, doubleArray[i * cols + j]);
            }
        }
        ldIdx.release();

        return mat;
    }

    /**
     * convert NDArray to opencv_core.Point2f
     * @param points
     * @param rows
     * @return
     */
    public static Point2f toOpenCVPoint2f(NDArray points, int rows) {
        double[] doubleArray = points.toDoubleArray();
        var points2f = new Point2f(rows);

        for (int i = 0; i < rows; i++) {
            points2f.position(i).x((float) doubleArray[i * 2]).y((float) doubleArray[i * 2 + 1]);
        }

        return points2f;
    }

    /**
     * convert Doubel array to opencv_core.Point2f
     * @param doubleArray
     * @param rows
     * @return
     */
    public static Point2f toOpenCVPoint2f(double[] doubleArray, int rows) {
        var points2f = new Point2f(rows);

        for (int i = 0; i < rows; i++) {
            points2f.position(i).x((float) doubleArray[i * 2]).y((float) doubleArray[i * 2 + 1]);
        }

        return points2f;
    }

    /**
     * convert list to opencv_core.Point2f
     * @param points
     * @param rows
     * @return
     */
    public static Point2f toOpenCVPoint2f(List<ai.djl.modality.cv.output.Point> points, int rows) {
        var points2f = new Point2f(points.size());

        for (int i = 0; i < rows; i++) {
            ai.djl.modality.cv.output.Point point = points.get(i);
            points2f.position(i).x((float) point.getX()).y((float) point.getY());
        }

        return points2f;
    }
}
