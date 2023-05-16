package sy.ocr.layout_detection;

import ai.onnxruntime.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import utils.common.CollectionUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author sy
 * @date 2023/5/2 13:48
 */
public class LayoutDet {
    float confThreshold;
    float iouThreshold;
    long inputHeight;
    long inputWidth;
    long[] inputShape;
    int numInputElements;
    OnnxJavaType inputType;
    OrtEnvironment env;
    OrtSession session;
    String inputName;
    String outputName;

    long rawImgHeight;
    long rawImgWidth;

    public List<String> labelNames;

    public LayoutDet(String path, String labelPath) throws OrtException {
        this(path, labelPath, 0.3f, 0.5f);
    }

    public LayoutDet(String path, String labelPath, float confThres, float iouThres) throws OrtException {
        this.confThreshold = confThres;
        this.iouThreshold = iouThres;
        initializeModel(path);
        initializeLabel(labelPath);
    }

    private void initializeModel(String path) throws OrtException {
        nu.pattern.OpenCV.loadLocally();
        this.initializeModel(path, -1);
    }

    private void initializeModel(String path, int gpuDeviceId) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        var sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        if(gpuDeviceId >= 0) {
            sessionOptions.addCPU(false);
            sessionOptions.addCUDA(gpuDeviceId);
        } else {
            sessionOptions.addCPU(true);
        }
        this.session = this.env.createSession(path, sessionOptions);

        Map<String, NodeInfo> inputMetaMap = this.session.getInputInfo();
        this.inputName = this.session.getInputNames().iterator().next();
        NodeInfo inputMeta = inputMetaMap.get(this.inputName);
        this.inputType = ((TensorInfo)inputMeta.getInfo()).type;
        this.inputShape = ((TensorInfo) inputMeta.getInfo()).getShape();
        this.numInputElements = (int) (this.inputShape[1] * this.inputShape[2] * this.inputShape[3]);
        this.inputHeight = this.inputShape[2];
        this.inputWidth = this.inputShape[3];
        this.outputName = this.session.getOutputNames().iterator().next();
    }

    private void initializeLabel(String labelPath) {
        try(Stream<String> lines = Files.lines(Paths.get(labelPath))) {
            labelNames = lines.map(line -> line.strip()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Detection> detectObjects(Mat img) throws OrtException {
        Map<String, OnnxTensor> inputMap = this.prepareInput(img);
        float[][] predictions = inference(inputMap);
        List<Detection> detectionList = this.processOutput(predictions);
        return detectionList;
    }

    private float[][] inference(Map<String, OnnxTensor> inputMap) throws OrtException { ;
        OrtSession.Result result = this.session.run(inputMap);
        float[][] predicitons = ((float[][][])result.get(0).getValue())[0];
        return predicitons;
    }

    private Map<String, OnnxTensor> prepareInput(Mat img) throws OrtException {
        this.rawImgHeight = img.height();
        this.rawImgWidth = img.width();
        Mat inputImg = new Mat();
        Imgproc.cvtColor(img, inputImg, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(inputImg, inputImg, new Size(this.inputWidth, this.inputHeight));
        inputImg.convertTo(inputImg, CvType.CV_32FC3, 1./255);

        Map<String, OnnxTensor> inputMap = CollectionUtil.newHashMap();
        float[] whc = new float[this.numInputElements];
        inputImg.get(0, 0, whc);
        float[] chw = LayoutDetectionUtil.whc2cwh(whc);
        FloatBuffer inputBuffer= FloatBuffer.wrap(chw);
        OnnxTensor inputTensor = OnnxTensor.createTensor(this.env, inputBuffer, this.inputShape);
        inputMap.put(this.inputName, inputTensor);
        return inputMap;
    }

    private List<Detection> processOutput(float[][] predictions) {
        // prediction
        predictions = transposeMatrix(predictions);
        Map<Integer, List<float[]>> class2Bbox = CollectionUtil.newHashMap();
        for(float[] bbox : predictions) {
            float[] condProb = Arrays.copyOfRange(bbox, 4, bbox.length);
            int label = predMax(condProb);
            float conf = condProb[label];
            if(conf < this.confThreshold) {
                continue;
            }
            bbox[4] = conf;
            rescaleBoxes(bbox); // xmin, ymin, xmax, ymax -> (xmin_raw, ymin_raw, xmax_raw, ymax_raw)
            LayoutDetectionUtil.xywh2xyxy(bbox); // xywh -> (x1, y1, x2, y2)
            //skip invalid prediction
            if(bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) {
                continue;
            }
            class2Bbox.putIfAbsent(label, CollectionUtil.newArrayList());
            class2Bbox.get(label).add(bbox);
        }
        //apply non-max suppression for each class
        List<Detection> detectionList = CollectionUtil.newArrayList();
        for(Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
            int lable = entry.getKey();
            List<float[]> bboxList = entry.getValue();
            bboxList = LayoutDetectionUtil.nonMaxSuppression(bboxList, this.iouThreshold);
            for(float[] bbox : bboxList) {
                String labelString = labelNames.get(lable);
                detectionList.add(new Detection(labelString, lable, Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
            }
        }
        return detectionList;
    }

    public float[][] transposeMatrix(float[][] matrix) {
        float[][] transMatrix = new float[matrix[0].length][matrix.length];
        for(int i=0; i<matrix.length; i++) {
            for(int j=0; j<matrix[0].length; j++) {
                transMatrix[j][i] = matrix[i][j];
            }
        }
        return transMatrix;
    }

    /**
     * @param probabilities
     * @return
     */
    private int predMax(float[] probabilities) {
        float maxVal = Float.NEGATIVE_INFINITY;
        int idx = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxVal) {
                maxVal = probabilities[i];
                idx = i;
            }
        }
        return idx;
    }

    public void rescaleBoxes(float[] bbox) {
        bbox[0] /= this.inputWidth;
        bbox[0] *= this.rawImgWidth;
        bbox[1] /= this.inputHeight;
        bbox[1] *= this.rawImgHeight;
        bbox[2] /= this.inputWidth;
        bbox[2] *= this.rawImgWidth;
        bbox[3] /= this.inputHeight;
        bbox[3] *= this.rawImgHeight;
    }

}

