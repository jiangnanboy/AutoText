package sy.corrector.dl.bert.tokenizerimpl;

import sy.corrector.dl.bert.tokenizer.Tokenizer;
import sy.corrector.dl.bert.utils.TokenizerUtils;
import utils.common.CollectionUtil;

import java.util.List;
import java.util.Map;

/**
 * @author sy
 * @date 2022/5/31 19:03
 */
public class WordpieceTokenizer implements Tokenizer {
	private Map<String, Integer> vocab;
	private String unkToken;
	private int maxInputCharsPerWord;
	private List<String> specialTokensList;

	public WordpieceTokenizer(Map<String, Integer> vocab, String unkToken, int maxInputCharsPerWord) {
		this.vocab = vocab;
		this.unkToken = unkToken;
		this.maxInputCharsPerWord = maxInputCharsPerWord;
	}

	public WordpieceTokenizer(Map<String, Integer> vocab, String unkToken, List<String> specialTokensList) {
		this.vocab = vocab;
		this.unkToken = unkToken;
		this.specialTokensList = specialTokensList;
		this.maxInputCharsPerWord = 100;
	}

	@Override
	public List<String> tokenize(String text) {
		List<String> outputTokens = CollectionUtil.newArrayList();
		if(this.specialTokensList.contains(text)) {
			outputTokens.add(text);
			return outputTokens;
		}
		for (String token : TokenizerUtils.whitespace_tokenize(text)) {
			if (token.length() > maxInputCharsPerWord) {
				outputTokens.add(unkToken);
				continue;
			}
			boolean isBad = false;
			int start = 0;

			List<String> subTokens = CollectionUtil.newArrayList();
			while (start < token.length()) {
				int end = token.length();
				String cur_substr = "";
				while (start < end) {
					String substr = token.substring(start, end);
					if (start > 0) {
						substr = "##" + substr;
					}
					if (vocab.containsKey(substr)) {
						cur_substr = substr;
						break;
					}
					end -= 1;
				}
				if (cur_substr == "") {
					isBad = true;
					break;
				}
				subTokens.add(cur_substr);
				start = end;
			}
			if (isBad) {
				outputTokens.add(unkToken);
			} else {
				outputTokens.addAll(subTokens);
			}
		}
		return outputTokens;
	}
}
