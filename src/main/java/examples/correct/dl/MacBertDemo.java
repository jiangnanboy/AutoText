package examples.correct.dl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import org.apache.commons.lang3.tuple.Pair;
import sy.corrector.dl.MacBert;
import sy.corrector.dl.bert.LoadModel;
import sy.corrector.dl.bert.tokenizerimpl.BertTokenizer;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2022/7/4 20:45
 */
public class MacBertDemo {

    public static void main(String[] args) throws OrtException, FileNotFoundException {
        LoadModel.loadOnnxModel();
        String text = "今天新情很好。";
        text = "你找到你最喜欢的工作，我也很高心。";
        text = "是的，当线程不再使用时，该缓冲区将被清理（我昨天实际上对此进行了测试，我可以每5ms发送一个新线程，而不会产生净内存累积，并确认它的rng内存已在gc上清理）。编号7788";
        text = text.toLowerCase();
        BertTokenizer tokenizer = new BertTokenizer();
        MacBert macBert = new MacBert(tokenizer);
        Map<String, OnnxTensor> inputTensor = null;
        try {
            inputTensor = macBert.parseInputText(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> predTokenList = macBert.predCSC(inputTensor);
        predTokenList = predTokenList.stream().map(token -> token.replace("##", "")).collect(Collectors.toList());
        String predString = String.join("", predTokenList);
        System.out.println(predString);
        List<Pair<String, String>> resultList = macBert.getErrors(predString, text);
        for(Pair<String, String> result : resultList) {
            System.out.println(text + " => " + result.getLeft() + " " + result.getRight());
        }
    }

}
