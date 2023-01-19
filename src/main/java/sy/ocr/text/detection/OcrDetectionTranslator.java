package sy.ocr.text.detection;

import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.opencv.core.CvType;

import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * @author sy
 * @date 2022/1/3 23:01
 */
public class OcrDetectionTranslator implements Translator<Image, NDList> {

    private Image image;
    private final int maxSideLen;
    private final int maxCandidates;
    private final int minSize;
    private final float boxThresh;
    private final float unclipRatio;
    private float ratioH;
    private float ratioW;
    private int imgHeight;
    private int imgWidth;

    /**
     * ocr detection translator
     * @param arguments
     */
    public OcrDetectionTranslator(Map<String, ?> arguments) {
        maxSideLen =
                arguments.containsKey("max_side_len")
                        ? Integer.parseInt(arguments.get("max_side_len").toString())
                        : 960;
        maxCandidates =
                arguments.containsKey("max_candidates")
                        ? Integer.parseInt(arguments.get("max_candidates").toString())
                        : 1000;
        minSize =
                arguments.containsKey("min_size")
                        ? Integer.parseInt(arguments.get("min_size").toString())
                        : 3;
        boxThresh =
                arguments.containsKey("box_thresh")
                        ? Float.parseFloat(arguments.get("box_thresh").toString())
                        : 0.5f;
        unclipRatio =
                arguments.containsKey("unclip_ratio")
                        ? Float.parseFloat(arguments.get("unclip_ratio").toString())
                        : 1.6f;
    }

    /**
     * process output
     * @param ctx
     * @param list
     * @return
     */
    @Override
    public NDList processOutput(TranslatorContext ctx, NDList list) {
        var manager = ctx.getNDManager();
        var pred = list.singletonOrThrow();
        pred = pred.squeeze();
        var segmentation = pred.toType(DataType.UINT8, true).gt(0.3);   // thresh=0.3 .mul(255f)
        segmentation = segmentation.toType(DataType.UINT8, true);
        //convert from NDArray to Mat
        byte[] byteArray = segmentation.toByteArray();
        var shape = segmentation.getShape();
        var rows = (int) shape.get(0);
        var cols = (int) shape.get(1);

        var srcMat = new Mat(rows, cols, CvType.CV_8U);

        UByteRawIndexer ldIdx = srcMat.createIndexer();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                ldIdx.put(row, col, byteArray[row * cols + col]);
            }
        }
        ldIdx.release();

        var mask = new Mat();
        // the smaller the size, the smaller the corrosion unit, the closer the image is to the original
        Mat structImage =
                opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(2, 2));
        /**
         * a part of the image is convolved with the specified kernel, and the maximum value of the kernel is found and assigned to the specified region.
         * inflation can be understood as the 'field expansion' of the 'highlighted area' in the image.
         */
        opencv_imgproc.dilate(srcMat, mask, structImage);

        ldIdx = mask.createIndexer();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                ldIdx.put(row, col, ldIdx.get(row, col) * 255);
            }
        }
        ldIdx.release();

        var boxes = boxesFromBitmap(manager, pred, mask, boxThresh);

        //boxes[:, :, 0] = boxes[:, :, 0] / ratio_w
        var boxes1 = boxes.get(":, :, 0").div(ratioW);
        boxes.set(new NDIndex(":, :, 0"), boxes1);
        //boxes[:, :, 1] = boxes[:, :, 1] / ratio_h
        var boxes2 = boxes.get(":, :, 1").div(ratioH);
        boxes.set(new NDIndex(":, :, 1"), boxes2);

        var dtBoxes = this.filterTagDetRes(boxes);

        dtBoxes.detach();

        // release Mat
        srcMat.release();
        mask.release();
        structImage.release();

        return dtBoxes;
    }

    /**
     * filter tag det res
     * @param dtBoxes
     * @return
     */
    private NDList filterTagDetRes(NDArray dtBoxes) {
        var boxesList = new NDList();

        var num = (int) dtBoxes.getShape().get(0);
        for (int i = 0; i < num; i++) {
            var box = dtBoxes.get(i);
            box = orderPointsClockwise(box);
            box = clipDetRes(box);
            float[] box0 = box.get(0).toFloatArray();
            float[] box1 = box.get(1).toFloatArray();
            float[] box3 = box.get(3).toFloatArray();
            var rectWidth = (int) Math.sqrt(Math.pow(box1[0] - box0[0], 2) + Math.pow(box1[1] - box0[1], 2));
            var rectHeight = (int) Math.sqrt(Math.pow(box3[0] - box0[0], 2) + Math.pow(box3[1] - box0[1], 2));
            if (rectWidth <= 3 || rectHeight <= 3) {
                continue;
            }
            boxesList.add(box);
        }

        return boxesList;
    }

    /**
     * clip det res
     * @param points
     * @return
     */
    private NDArray clipDetRes(NDArray points) {
        for (int i = 0; i < points.getShape().get(0); i++) {
            var value = Math.max((int) points.get(i, 0).toFloatArray()[0], 0);
            value = Math.min(value, imgWidth - 1);
            points.set(new NDIndex(i + ",0"), value);
            value = Math.max((int) points.get(i, 1).toFloatArray()[0], 0);
            value = Math.min(value, imgHeight - 1);
            points.set(new NDIndex(i + ",1"), value);
        }

        return points;
    }

    /**
     * sort the points based on their x-coordinates
     * @param pts
     * @return
     */

    private NDArray orderPointsClockwise(NDArray pts) {
        var list = new NDList();
        long[] indexes = pts.get(":, 0").argSort().toLongArray();

        // grab the left-most and right-most points from the sorted
        // x-roodinate points
        var s1 = pts.getShape();
        var leftMost1 = pts.get(indexes[0] + ",:");
        var leftMost2 = pts.get(indexes[1] + ",:");
        var leftMost = leftMost1.concat(leftMost2).reshape(2, 2);
        var rightMost1 = pts.get(indexes[2] + ",:");
        var rightMost2 = pts.get(indexes[3] + ",:");
        var rightMost = rightMost1.concat(rightMost2).reshape(2, 2);

        // now, sort the left-most coordinates according to their
        // y-coordinates so we can grab the top-left and bottom-left
        // points, respectively
        indexes = leftMost.get(":, 1").argSort().toLongArray();
        var lt = leftMost.get(indexes[0] + ",:");
        var lb = leftMost.get(indexes[1] + ",:");
        indexes = rightMost.get(":, 1").argSort().toLongArray();
        var rt = rightMost.get(indexes[0] + ",:");
        var rb = rightMost.get(indexes[1] + ",:");

        list.add(lt);
        list.add(rt);
        list.add(rb);
        list.add(lb);

        var rect = NDArrays.concat(list).reshape(4, 2);
        return rect;
    }

    /**
     * get boxes from the binarized image predicted by DB
     * @param manager
     * @param pred    the binarized image predicted by DB.
     * @param mask    new 'pred' after threshold filtering.
     * @param boxThresh
     */
    private NDArray boxesFromBitmap(NDManager manager, NDArray pred, Mat mask, float boxThresh) {
        var destHeight = (int) pred.getShape().get(0);
        var destWidth = (int) pred.getShape().get(1);
        var height = mask.rows();
        var width = mask.cols();

        var contours = new MatVector();
        var hierarchy = new Mat();
        // find contours
        findContours(
                mask,
                contours,
                hierarchy,
                opencv_imgproc.RETR_LIST,
                opencv_imgproc.CHAIN_APPROX_SIMPLE,
                new Point(0, 0));

        var numContours = Math.min((int) contours.size(), maxCandidates);
        var boxList = new NDList();
//        NDArray boxes = manager.zeros(new Shape(num_contours, 4, 2), DataType.FLOAT32);
        float[] scores = new float[numContours];

        var count = 0;
        for (int index = 0; index < numContours; index++) {
            var contour = contours.get(index);
            float[][] pointsArr = new float[4][2];
            var sside = getMiniBoxes(contour, pointsArr);
            if (sside < this.minSize) {
                continue;
            }
            var points = manager.create(pointsArr);
            var score = boxScoreFast(manager, pred, points);
            if (score < this.boxThresh) {
                continue;
            }

            var box = unClip(manager, points); // TODO get_mini_boxes(box)


            // box[:, 0] = np.clip(np.round(box[:, 0] / width * dest_width), 0, dest_width)
            var boxes1 = box.get(":,0").div(width).mul(destWidth).round().clip(0, destWidth);
            box.set(new NDIndex(":, 0"), boxes1);
            // box[:, 1] = np.clip(np.round(box[:, 1] / height * dest_height), 0, dest_height)
            var boxes2 = box.get(":,1").div(height).mul(destHeight).round().clip(0, destHeight);
            box.set(new NDIndex(":, 1"), boxes2);

            if (score > boxThresh) {
                boxList.add(box);
//                boxes.set(new NDIndex(count + ",:,:"), box);
                scores[index] = score;
                count++;
            }

            // release memory
            contour.release();
        }
//        if (count < num_contours) {
//            NDArray newBoxes = manager.zeros(new Shape(count, 4, 2), DataType.FLOAT32);
//            newBoxes.set(new NDIndex("0,0,0"), boxes.get(":" + count + ",:,:"));
//            boxes = newBoxes;
//        }
        var boxes = NDArrays.stack(boxList);

        // release
        hierarchy.release();
        contours.releaseReference();

        return boxes;
    }

    /**
     * shrink or expand the boxaccording to 'unclip_ratio'
     * @param points The predicted box.
     * @return uncliped box
     */
    private NDArray unClip(NDManager manager, NDArray points) {
        points = orderPointsClockwise(points);
        float[] pointsArr = points.toFloatArray();
        float[] lt = java.util.Arrays.copyOfRange(pointsArr, 0, 2);
        float[] lb = java.util.Arrays.copyOfRange(pointsArr, 6, 8);

        float[] rt = java.util.Arrays.copyOfRange(pointsArr, 2, 4);
        float[] rb = java.util.Arrays.copyOfRange(pointsArr, 4, 6);

        var width = distance(lt, rt);
        var height = distance(lt, lb);

        if (width > height) {
            var k = (lt[1] - rt[1]) / (lt[0] - rt[0]); // y = k * x + b

            var deltaDis = height;
            var deltaX = (float) Math.sqrt((deltaDis * deltaDis) / (k * k + 1));
            var deltaY = Math.abs(k * deltaX);

            if (k > 0) {
                pointsArr[0] = lt[0] - deltaX + deltaY;
                pointsArr[1] = lt[1] - deltaY - deltaX;
                pointsArr[2] = rt[0] + deltaX + deltaY;
                pointsArr[3] = rt[1] + deltaY - deltaX;

                pointsArr[4] = rb[0] + deltaX - deltaY;
                pointsArr[5] = rb[1] + deltaY + deltaX;
                pointsArr[6] = lb[0] - deltaX - deltaY;
                pointsArr[7] = lb[1] - deltaY + deltaX;
            } else {
                pointsArr[0] = lt[0] - deltaX - deltaY;
                pointsArr[1] = lt[1] + deltaY - deltaX;
                pointsArr[2] = rt[0] + deltaX - deltaY;
                pointsArr[3] = rt[1] - deltaY - deltaX;

                pointsArr[4] = rb[0] + deltaX + deltaY;
                pointsArr[5] = rb[1] - deltaY + deltaX;
                pointsArr[6] = lb[0] - deltaX + deltaY;
                pointsArr[7] = lb[1] + deltaY + deltaX;
            }
        } else {
            var k = (lt[1] - rt[1]) / (lt[0] - rt[0]); // y = k * x + b

            var deltaDis = width;
            var deltaY = (float) Math.sqrt((deltaDis * deltaDis) / (k * k + 1));
            var deltaX = Math.abs(k * deltaY);

            if (k > 0) {
                pointsArr[0] = lt[0] + deltaX - deltaY;
                pointsArr[1] = lt[1] - deltaY - deltaX;
                pointsArr[2] = rt[0] + deltaX + deltaY;
                pointsArr[3] = rt[1] - deltaY + deltaX;

                pointsArr[4] = rb[0] - deltaX + deltaY;
                pointsArr[5] = rb[1] + deltaY + deltaX;
                pointsArr[6] = lb[0] - deltaX - deltaY;
                pointsArr[7] = lb[1] + deltaY - deltaX;
            } else {
                pointsArr[0] = lt[0] - deltaX - deltaY;
                pointsArr[1] = lt[1] - deltaY + deltaX;
                pointsArr[2] = rt[0] - deltaX + deltaY;
                pointsArr[3] = rt[1] - deltaY - deltaX;

                pointsArr[4] = rb[0] + deltaX + deltaY;
                pointsArr[5] = rb[1] + deltaY - deltaX;
                pointsArr[6] = lb[0] + deltaX - deltaY;
                pointsArr[7] = lb[1] + deltaY + deltaX;
            }
        }
        points = manager.create(pointsArr).reshape(4, 2);

        return points;
    }

    /**
     * distance between point1 and point2
     * @param point1
     * @param point2
     * @return
     */
    private float distance(float[] point1, float[] point2) {
        var disX = point1[0] - point2[0];
        var disY = point1[1] - point2[1];
        var dis = (float) Math.sqrt(disX * disX + disY * disY);
        return dis;
    }

    /**
     * get boxes from the contour or box.
     * @param contour   The predicted contour.
     * @param pointsArr The predicted box.
     * @return smaller side of box
     */
    private int getMiniBoxes(Mat contour, float[][] pointsArr) {
        // https://blog.csdn.net/qq_37385726/article/details/82313558
        // bounding_box[1] - rect returns the length and width of the rectangle
        var rect = minAreaRect(contour);
        var points = new Mat();
        boxPoints(rect, points);

        FloatRawIndexer ldIdx = points.createIndexer();
        float[][] fourPoints = new float[4][2];
        for (int row = 0; row < fourPoints.length; row++) {
            fourPoints[row][0] = ldIdx.get(row, 0);
            fourPoints[row][1] = ldIdx.get(row, 1);
        }
        ldIdx.release();

        float[] tmpPoint = new float[2];
        for (int i = 0; i < fourPoints.length; i++) {
            for (int j = i + 1; j < fourPoints.length; j++) {
                if (fourPoints[j][0] < fourPoints[i][0]) {
                    tmpPoint[0] = fourPoints[i][0];
                    tmpPoint[1] = fourPoints[i][1];
                    fourPoints[i][0] = fourPoints[j][0];
                    fourPoints[i][1] = fourPoints[j][1];
                    fourPoints[j][0] = tmpPoint[0];
                    fourPoints[j][1] = tmpPoint[1];
                }
            }
        }

        var index1 = 0;
        var index2 = 1;
        var index3 = 2;
        var index4 = 3;

        if (fourPoints[1][1] > fourPoints[0][1]) {
            index1 = 0;
            index4 = 1;
        } else {
            index1 = 1;
            index4 = 0;
        }

        if (fourPoints[3][1] > fourPoints[2][1]) {
            index2 = 2;
            index3 = 3;
        } else {
            index2 = 3;
            index3 = 2;
        }

        pointsArr[0] = fourPoints[index1];
        pointsArr[1] = fourPoints[index2];
        pointsArr[2] = fourPoints[index3];
        pointsArr[3] = fourPoints[index4];

        var height = rect.boundingRect().height();
        var width = rect.boundingRect().width();
        var sside = Math.min(height, width);

        // release
        points.release();
        rect.releaseReference();

        return sside;
    }

    /**
     * calculate the score of box.
     * @param bitmap The binarized image predicted by DB.
     * @param points The predicted box
     * @return
     */
    private float boxScoreFast(NDManager manager, NDArray bitmap, NDArray points) {
        var box = points.get(":");
        var h = bitmap.getShape().get(0);
        var w = bitmap.getShape().get(1);
        // xmin = np.clip(np.floor(box[:, 0].min()).astype(np.int), 0, w - 1)
        var xmin = box.get(":, 0").min().floor().clip(0, w - 1).toType(DataType.INT32, true).toIntArray()[0];
        var xmax = box.get(":, 0").max().ceil().clip(0, w - 1).toType(DataType.INT32, true).toIntArray()[0];
        var ymin = box.get(":, 1").min().floor().clip(0, h - 1).toType(DataType.INT32, true).toIntArray()[0];
        var ymax = box.get(":, 1").max().ceil().clip(0, h - 1).toType(DataType.INT32, true).toIntArray()[0];

        var mask = manager.zeros(new Shape(ymax - ymin + 1, xmax - xmin + 1), DataType.UINT8);

        box.set(new NDIndex(":, 0"), box.get(":, 0").sub(xmin));
        box.set(new NDIndex(":, 1"), box.get(":, 1").sub(ymin));

        //mask - convert from NDArray to Mat
        byte[] maskArray = mask.toByteArray();
        var rows = (int) mask.getShape().get(0);
        var cols = (int) mask.getShape().get(1);
        var maskMat = new Mat(rows, cols, CvType.CV_8U);
        UByteRawIndexer ldIdx = maskMat.createIndexer();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                ldIdx.put(row, col, maskArray[row * cols + col]);
            }
        }
        ldIdx.release();

        //mask - convert from NDArray to Mat
        float[] boxArray = box.toFloatArray();
        var boxMat = new Mat(4, 2, CvType.CV_32S);
        IntRawIndexer intRawIndexer = boxMat.createIndexer();
        for (int row = 0; row < 4; row++) {
            intRawIndexer.put(row, 0, (int) boxArray[row * 2]);
            intRawIndexer.put(row, 1, (int) boxArray[row * 2 + 1]);
        }
        intRawIndexer.release();

//        boxMat.reshape(1, new int[]{1, 4, 2});
        var matVector = new MatVector();
        matVector.put(boxMat);
        fillPoly(maskMat, matVector, new Scalar(1));

        var subBitMap = bitmap.get(ymin + ":" + (ymax + 1) + "," + xmin + ":" + (xmax + 1));
        float[] subBitMapArr = subBitMap.toFloatArray();
        rows = (int) subBitMap.getShape().get(0);
        cols = (int) subBitMap.getShape().get(1);
        var bitMapMat = new Mat(rows, cols, CvType.CV_32F);
        FloatRawIndexer floatRawIndexer = bitMapMat.createIndexer();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                floatRawIndexer.put(row, col, subBitMapArr[row * cols + col]);
            }
        }
        floatRawIndexer.release();

        var score = org.bytedeco.opencv.global.opencv_core.mean(bitMapMat, maskMat);
        var scoreValue = (float) score.get();
        // release
        maskMat.release();
        boxMat.release();
        bitMapMat.release();
        matVector.releaseReference();
        score.releaseReference();

        return scoreValue;
    }

    /**
     * process input
     * @param ctx
     * @param input
     * @return
     */
    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        var img = input.toNDArray(ctx.getNDManager());
        image = BufferedImageFactory.getInstance().fromNDArray(img);
        var h = input.getHeight();
        var w = input.getWidth();
        imgHeight = h;
        imgWidth = w;
        var resizeW = w;
        var resizeH = h;

        // limit the max side
        var ratio = 1.0f;
        if (Math.max(resizeH, resizeW) > maxSideLen) {
            if (resizeH > resizeW) {
                ratio = (float) maxSideLen / (float) resizeH;
            } else {
                ratio = (float) maxSideLen / (float) resizeW;
            }
        }

        resizeH = (int) (resizeH * ratio);
        resizeW = (int) (resizeW * ratio);

        if (resizeH % 32 == 0) {
            resizeH = resizeH;
        } else if (Math.floor((float) resizeH / 32f) <= 1) {
            resizeH = 32;
        } else {
            resizeH = (int) Math.floor((float) resizeH / 32f) * 32;
        }

        if (resizeW % 32 == 0) {
            resizeW= resizeW;
        } else if (Math.floor((float) resizeW / 32f) <= 1) {
            resizeW = 32;
        } else {
            resizeW = (int) Math.floor((float) resizeW / 32f) * 32;
        }

        ratioH = resizeH / (float) h;
        ratioW = resizeW / (float) w;

        img = NDImageUtils.resize(img, resizeW, resizeH);
        img = NDImageUtils.toTensor(img);
        img = NDImageUtils.normalize(
                        img,
                        new float[]{0.485f, 0.456f, 0.406f},
                        new float[]{0.229f, 0.224f, 0.225f});
        img = img.expandDims(0);
        return new NDList(img);
    }

    @Override
    public Batchifier getBatchifier() {
        return null;
    }
}
