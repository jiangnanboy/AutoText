package sy.ocr.text.detection;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sy
 * @date 2022/1/3 22:01
 */
public class OcrDetection {

    /**
     * load detection model
     * @param modelPath
     * @param engineType
     * @return
     */
    public Criteria<Image, NDList> detectCriteria(Path modelPath, String engineType) {
        Criteria<Image, NDList> criteria =
                Criteria.builder()
                        .optEngine(engineType)
                        .setTypes(Image.class, NDList.class)
                        .optModelPath(modelPath)
                        .optTranslator(new OcrDetectionTranslator(new ConcurrentHashMap<String, String>()))
//                        .optOptions(Collections.singletonMap("use_mkldnn", "true"))
//                        .optOption("interOpNumThreads", "6")
//                        .optOption("intraOpNumThreads", "6")
                        .optProgress(new ProgressBar())
                        .build();

        return criteria;
    }
}
