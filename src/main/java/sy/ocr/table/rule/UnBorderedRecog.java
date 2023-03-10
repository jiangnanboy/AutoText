package sy.ocr.table.rule;

import org.apache.commons.lang3.tuple.Pair;
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
 * @date 2023/1/17 23:51
 */
public class UnBorderedRecog {
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
//        threshold(bw, bw, 40, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
//        imshow("Binary Image", bw);
        int imgHeight = mat.size().height();
        int imgWidth = mat.size().width();
        // thresholding the image to a binary image
        Mat binaryMat = new Mat();
        opencv_imgproc.adaptiveThreshold(mat, binaryMat, 255, opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, opencv_imgproc.THRESH_BINARY, 5, 5);

        Mat imgMedian = new Mat();
        opencv_imgproc.medianBlur(binaryMat, imgMedian, 3);

        Mat verKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(9, imgHeight*2));
        Mat erodeVerticalMat = new Mat();
        opencv_imgproc.erode(imgMedian, erodeVerticalMat, verKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);

        Mat horKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(imgWidth*2, 9));
        Mat erodeHorizontalMat = new Mat();
        opencv_imgproc.erode(imgMedian, erodeHorizontalMat, horKernel, new Point(-1, -1), 1, opencv_core.BORDER_CONSTANT, null);

        // a kernel of 2*2
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(2, 2));

        // combine horizontal and vertical lines in a new third image, with both having same weight
        Mat vhMat = new Mat();
        opencv_core.addWeighted(erodeVerticalMat, 0.5, erodeHorizontalMat, 0.5, 0.0, vhMat);

        // eroding and thresholding the image
        Mat bitWiseNotVhMat = new Mat();
        opencv_core.bitwise_not(vhMat, bitWiseNotVhMat);
        opencv_imgproc.erode(bitWiseNotVhMat, vhMat, kernel, new Point(-1, -1), 2, opencv_core.BORDER_CONSTANT, null);
        opencv_imgproc.threshold(vhMat, vhMat, 128, 255, opencv_imgproc.THRESH_BINARY);

        // detect contours for following box detection
        MatVector contours = new MatVector();
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
