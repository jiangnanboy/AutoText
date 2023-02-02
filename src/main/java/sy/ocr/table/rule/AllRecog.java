package sy.ocr.table.rule;

import org.apache.commons.lang3.tuple.Pair;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Parallel;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import utils.common.CollectionUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static sy.ocr.table.rule.util.sortContours;

/**
 * @author sy
 * @date 2023/1/17 23:52
 */
public class AllRecog {

    public static List<List<List<Integer>>> recognizeStructure(String imagePath) {
        return recognizeStructure(imread(imagePath));
    }

    /**
     * recognize structure
     * @param mat
     * @return
     */
    public static List<List<List<Integer>>> recognizeStructure(Mat mat) {
        // Create binary image from source image
        opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_BGR2GRAY);
        int imgHeight = mat.size().height();
        int imgWidth = mat.size().width();
        // thresholding the image to a binary image
        Mat binaryMat = new Mat();
        opencv_imgproc.threshold(mat, binaryMat, 180, 255, opencv_imgproc.THRESH_BINARY);
        MatVector contours = new MatVector();
        opencv_imgproc.findContours(binaryMat, contours, opencv_imgproc.RETR_TREE, opencv_imgproc.CHAIN_APPROX_SIMPLE);

        boolean invert = false;
        UByteIndexer uByteIndexer = binaryMat.createIndexer();
        for(int index=0; index<contours.size(); index++) {
            Mat coutourMat = contours.get(index);
            Rect coutourRect = opencv_imgproc.boundingRect(coutourMat);
            int x = coutourRect.x();
            int y = coutourRect.y();
            int width = coutourRect.width();
            int height = coutourRect.height();
            if((width < 0.9 * imgWidth) && (height < 0.9 * imgHeight) && ((width > Math.max(10, imgWidth/30)) && (height > Math.max(10, imgHeight/30)))) {
                invert = true;
                Parallel.loop(y, y + height, new Parallel.Looper() {
                    @Override
                    public void loop(int from, int to, int looperID) {
                        for(int i=from; i<to; i++) {
                            for(int j=x; j<x+width; j++) {
                                float a = 255 - uByteIndexer.get(i, j);
                                uByteIndexer.put(i, j, (byte) a);
                            }
                        }
                    }
                });
            }
        }
        uByteIndexer.release();
        if(invert) {
            binaryMat = opencv_core.subtract(new Scalar(255), binaryMat).asMat();
        }
        Mat imgBinInv = opencv_core.subtract(new Scalar(255), binaryMat).asMat();
        // countcol(width) of kernel as 100th of total width
        // kernel_len = np.array(img).shape[1] // 100
        int kernelLenVer = Math.max(10, imgHeight / 50);
        int kernelLenHor = Math.max(10, imgWidth / 50);

        // Defining a vertical kernel to detect all vertical lines of image, [kernelLenVer, 1]
        Mat verKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(1, kernelLenVer));

        // Defining a horizontal kernel to detect all horizontal lines of image, [1, kernelLenHor]
        Mat horKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(kernelLenHor, 1));

        // a kernel of 2*2
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(2, 2));

        // 1. use vertical kernel to detect and save the vertical lines in a jpg
        Mat erodeVerticalMat = new Mat();
        opencv_imgproc.erode(imgBinInv, erodeVerticalMat, verKernel, new Point(-1, -1), 3, opencv_core.BORDER_CONSTANT, null);
        Mat verticalLines = new Mat();
        opencv_imgproc.dilate(erodeVerticalMat, verticalLines, verKernel, new Point(-1, -1), 4, opencv_core.BORDER_CONSTANT, null);
//        imshow("verticallines", verticalLines);

        // 2. use horizontal kernel to detect and save the horizontal lines in a jpg
        Mat erodeHorizontalMat = new Mat();
        opencv_imgproc.erode(imgBinInv, erodeHorizontalMat, horKernel, new Point(-1, -1), 3, opencv_core.BORDER_CONSTANT, null);
        Mat horizontalLines = new Mat();
        opencv_imgproc.dilate(erodeHorizontalMat, horizontalLines, horKernel, new Point(-1, -1), 5, opencv_core.BORDER_CONSTANT, null);
//        imshow("horizontallines", horizontalLines);

        // 3. combine horizontal and vertical lines in a new third image, with both having same weight
        Mat vhMat = new Mat();
        opencv_core.addWeighted(verticalLines, 0.5, horizontalLines, 0.5, 0.0, vhMat);
//        imshow("vh", vhMat);
        // dilate
        opencv_imgproc.dilate(vhMat, vhMat, kernel, new Point(-1, -1), 3, opencv_core.BORDER_CONSTANT, null);
        opencv_imgproc.threshold(vhMat, vhMat, 50, 255, opencv_imgproc.THRESH_BINARY);

        Mat bitWiseOrVhMat = new Mat();
        opencv_core.bitwise_or(binaryMat, vhMat, bitWiseOrVhMat);

        Mat imgMedian = new Mat();
        opencv_imgproc.medianBlur(bitWiseOrVhMat, imgMedian, 3);

        verKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(5, imgHeight*2));
        opencv_imgproc.erode(imgMedian, erodeVerticalMat, verKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);

        horKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(imgWidth*2, 3));
        opencv_imgproc.erode(imgMedian, erodeHorizontalMat, horKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);

        opencv_core.addWeighted(erodeVerticalMat, 0.5, erodeHorizontalMat, 0.5, 0.0, vhMat);

        // eroding and thresholding the image
        Mat bitWiseNotVhMat = new Mat();
        opencv_core.bitwise_not(vhMat, bitWiseNotVhMat);
        opencv_imgproc.erode(bitWiseNotVhMat, vhMat, kernel, new Point(-1, -1), 2, opencv_core.BORDER_CONSTANT, null);
        opencv_imgproc.threshold(vhMat, vhMat, 128, 255, opencv_imgproc.THRESH_BINARY);

//        imshow("vh_threshold", vhMat);

//        Mat bitXorVhMat = new Mat();
//        opencv_core.bitwise_xor(mat, vhMat, bitXorVhMat);
//        Mat bitNotVhMat = new Mat();
//        opencv_core.bitwise_not(bitXorVhMat, bitNotVhMat);
//        imshow("bitNotVhMat", bitNotVhMat);

        // 5.detect contours for following box detection
//        MatVector contours = new MatVector();
        opencv_imgproc.findContours(vhMat, contours, opencv_imgproc.RETR_TREE, opencv_imgproc.CHAIN_APPROX_SIMPLE);

        Pair<List<Rect>, Map<Rect, Integer>> sortedPair = sortContours(contours, "top-to-bottom");

        // creating a list of heights for all detected boxes
        List<Integer> heightList = sortedPair.getLeft().stream().map(rect -> rect.height()).collect(Collectors.toList());
        // get mean of heights
        double heightMean = heightList.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        Map<Rect, Integer> sortedContoursMap = sortedPair.getRight();
        // create list box to store all boxes in
        List<List<Integer>> box = CollectionUtil.newArrayList();
        // Get position (x,y), width and height for every contour and show the contour on image
        sortedContoursMap.forEach((k, v) -> {
            int x = k.x();
            int y = k.y();
            int width = k.width();
            int height = k.height();
            if((width < 0.9 * imgWidth) && (height < 0.9 * imgHeight)) {
                opencv_imgproc.rectangle(mat, k, new Scalar(0));
                List<Integer> subBox = CollectionUtil.newArrayList();
                subBox.add(x);
                subBox.add(y);
                subBox.add(width);
                subBox.add(height);
                box.add(subBox);
            }
        });

//        ImageUtils.imshow("mat final", mat);

        // create two lists to define row and column in which cell is located
        List<List<List<Integer>>> rowList = CollectionUtil.newArrayList();
        List<List<Integer>> columnList = CollectionUtil.newArrayList();
        // Sorting the boxes to their respective row and column
        List<Integer> previous = null;
        for(int i = 0; i < box.size(); i++) {
            if(0 == i) {
                columnList.add(box.get(i));
                previous = box.get(i);
            } else {
                if(box.get(i).get(1) <= (previous.get(1) + heightMean / 2)) {
                    columnList.add(box.get(i));
                    previous = box.get(i);
                    if(i == (box.size() - 1)) {
                        rowList.add(columnList);
                    }
                } else {
                    rowList.add(columnList);
                    columnList = CollectionUtil.newArrayList();
                    previous = box.get(i);
                    columnList.add(box.get(i));
                }
            }
        }

        // calculating maximum number of cells
        int countCol = 0;
        int index = 0;
        for(int rowIndex=0; rowIndex<rowList.size(); rowIndex++) {
            int currentLen = rowList.get(rowIndex).size();
            if(currentLen > countCol) {
                countCol = currentLen;
                index = rowIndex;
            }
        }

        // retrieving the center of each column
        List<Integer> centerList = CollectionUtil.newArrayList();
        for(int j=0; j<rowList.get(index).size(); j++) {
            int center = rowList.get(index).get(j).get(0) + rowList.get(index).get(j).get(2) / 2;
            centerList.add(center);
        }
        centerList = centerList.stream().sorted(Comparator.comparing(Integer::intValue)).collect(Collectors.toList());
        List<List<List<Integer>>> finalBoxes = CollectionUtil.newArrayList();
        for(int rowIndex=0; rowIndex<rowList.size(); rowIndex++) {
            List<List<Integer>> list = CollectionUtil.newArrayList();
            for(int count=0; count<countCol; count++) {
                List<Integer> subList = CollectionUtil.newArrayList();
                list.add(subList);
            }
            int finalIndex = rowIndex;
            for(int j=0; j<rowList.get(rowIndex).size(); j++) {
                int finalJ = j;
                List<Integer> diffList = centerList.stream().map(center -> Math.abs(center - (rowList.get(finalIndex).get(finalJ).get(0) + rowList.get(finalIndex).get(finalJ).get(2) / 4))).collect(Collectors.toList());
                int minimum = Collections.min(diffList);
                int minimumIndex = diffList.indexOf(minimum);
                list.add(minimumIndex, rowList.get(rowIndex).get(j));
            }
            list = list.stream().filter(lst -> lst.size() != 0).collect(Collectors.toList());
            finalBoxes.add(list);
        }
        return finalBoxes;
    }
}
