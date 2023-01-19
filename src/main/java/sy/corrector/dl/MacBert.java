package sy.corrector.dl;

import ai.onnxruntime.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import sy.corrector.dl.bert.LoadModel;
import sy.corrector.dl.bert.tokenizerimpl.BertTokenizer;
import utils.common.CollectionUtil;

import java.util.List;
import java.util.Map;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * @author sy
 * @date 2022/5/31 19:03
 */
public class MacBert {
    BertTokenizer tokenizer;

    public MacBert(BertTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * tokenize text
     * @param text
     * @return
     * @throws Exception
     */
    public Map<String, OnnxTensor> parseInputText(String text) throws Exception{
        var env = LoadModel.env;
        List<String > tokens = tokenizer.tokenize(text);
        List<Integer> tokenIds = tokenizer.convertTokensToIds(tokens);
        long[] inputIds = new long[tokenIds.size()];
        long[] attentionMask = new long[tokenIds.size()];
        long[] tokenTypeIds = new long[tokenIds.size()];
        for(int index=0; index < tokenIds.size(); index ++) {
            inputIds[index] = tokenIds.get(index);
            attentionMask[index] = 1;
            tokenTypeIds[index] = 0;
        }
        long[] shape = new long[]{1, inputIds.length};
        Object ObjInputIds = OrtUtil.reshape(inputIds, shape);
        Object ObjAttentionMask = OrtUtil.reshape(attentionMask, shape);
        Object ObjTokenTypeIds = OrtUtil.reshape(tokenTypeIds, shape);
        OnnxTensor input_ids = OnnxTensor.createTensor(env, ObjInputIds);
        OnnxTensor attention_mask = OnnxTensor.createTensor(env, ObjAttentionMask);
        OnnxTensor token_type_ids = OnnxTensor.createTensor(env, ObjTokenTypeIds);
        var inputs = Map.of("input_ids", input_ids, "attention_mask", attention_mask, "token_type_ids", token_type_ids);
        return inputs;
    }

    /**
     * correct text
     * @param inputs
     * @return
     */
    public List<String> predCSC(Map<String, OnnxTensor> inputs) {
        List<String> predTokenList = CollectionUtil.newArrayList();
        try{
            var session = LoadModel.session;
            try(var results = session.run(inputs)) {
                OnnxValue onnxValue = results.get(0);
                float[][][] labels = (float[][][]) onnxValue.getValue();
                INDArray indArrayLabels = Nd4j.create(labels[0]);
                INDArray index = Nd4j.argMax(indArrayLabels, -1);
                int[] predIndex = index.toIntVector();
                for(int idx=1; idx<predIndex.length -1; idx++) {
                    predTokenList.add(tokenizer.convertIdsToTokens(predIndex[idx]));
                }
            }
        } catch (OrtException e) {
            e.printStackTrace();
        }
        return predTokenList;
    }

    public List<Pair<String, String>> getErrors(String correctedText, String originText) {
        List<String> specialList = CollectionUtil.newArrayList();
        specialList.add(" ");
        specialList.add("“");
        specialList.add("”");
        specialList.add("‘");
        specialList.add("’");
        specialList.add("琊");
        specialList.add("\n");
        specialList.add("…");
        specialList.add("—");
        specialList.add("擤");
        List<Pair<String, String>> subDetails = CollectionUtil.newArrayList();
        for(int i=0; i<originText.length();i++) {
            String oriChar = originText.substring(i, i+1);
            if(specialList.contains(oriChar)) {
                // add unk word
                correctedText = correctedText.substring(0, i) + oriChar + correctedText.substring(i+1);
                continue;
            }
            if( i >= correctedText.length()) {
                continue;
            }
            if(!StringUtils.equals(oriChar,correctedText.substring(i, i+1))) {
                if(oriChar.toLowerCase() == correctedText.substring(i, i+1)) {
                    // pass english upper char
                    correctedText = correctedText.substring(0, i) + oriChar + correctedText.substring(i+1);
                    continue;
                }
                StringBuffer sb = new StringBuffer();
                sb.append(oriChar).append(",").append(correctedText, i, i+1).append(",").append(i).append(",").append(i+1);
                subDetails.add(Pair.of(correctedText, sb.toString()));
            }
        }
        return subDetails;
    }

}

