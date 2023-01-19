package sy.corrector.dl.bert;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.common.PropertiesReader;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author sy
 * @date 2022/5/31 19:03
 */
public class LoadModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadModel.class);
    public static OrtSession session;
    public static OrtEnvironment env;
    /**
     * load onnx model
     * @throws OrtException
     */
    public static void loadOnnxModel() throws OrtException, FileNotFoundException {
        String onnxPath = LoadModel.class.getClassLoader().getResource(PropertiesReader.get("onnx_model_path")).getPath().replaceFirst("/", "");
        if(Files.exists(Paths.get(onnxPath))) {
            LOGGER.info("load onnx model -> " + onnxPath);
            loadOnnxModel(onnxPath);
        } else {
            throw new FileNotFoundException("vocab file not exists -> " + onnxPath + ", place check!");
        }
    }

    public static void loadOnnxModel(String onnxPath) throws OrtException {
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(onnxPath, new OrtSession.SessionOptions());
    }

    /**
     * close onnx model
     */
    public static void closeOnnxModel() {
        LOGGER.info("close onnx model...");
        if (Optional.of(session).isPresent()) {
            try {
                session.close();
            } catch (OrtException e) {
                e.printStackTrace();
            }
        }
        if(Optional.of(env).isPresent()) {
            env.close();
        }
    }

}
