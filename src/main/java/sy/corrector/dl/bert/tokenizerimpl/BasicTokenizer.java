package sy.corrector.dl.bert.tokenizerimpl;

import sy.corrector.dl.bert.tokenizer.Tokenizer;
import sy.corrector.dl.bert.utils.TokenizerUtils;
import utils.common.CollectionUtil;

import java.util.List;

/**
 * @author sy
 * @date 2022/5/31 19:03
 */
public class BasicTokenizer implements Tokenizer {
	private boolean doLowerCase = true;
	private List<String> neverSplit;
	private boolean tokenizeChineseChars = true;
	private List<String> specialTokens;
	public BasicTokenizer(boolean doLowerCase, List<String> neverSplit, boolean tokenizeChineseChars) {
		this.doLowerCase = doLowerCase;
		if (neverSplit == null) {
			this.neverSplit = CollectionUtil.newArrayList();
		} else {
			this.neverSplit = neverSplit;
		}
		this.tokenizeChineseChars = tokenizeChineseChars;
	}

	public BasicTokenizer() {
	}

	@Override
	public List<String> tokenize(String text) {
		text = TokenizerUtils.clean_text(text);
		if (tokenizeChineseChars) {
			text = TokenizerUtils.tokenize_chinese_chars(text);
		}
		List<String> orig_tokens = TokenizerUtils.whitespace_tokenize(text);
		List<String> split_tokens = CollectionUtil.newArrayList();
		for (String token : orig_tokens) {
			if(doLowerCase) {
				token = token.toLowerCase();
			}
			if (!neverSplit.contains(token)) {
				token = TokenizerUtils.run_strip_accents(token);
				split_tokens.addAll(TokenizerUtils.run_split_on_punc(token, neverSplit));
			} else {
				split_tokens.add(token);
			}
		}
		return TokenizerUtils.whitespace_tokenize(String.join(" ", split_tokens));
	}

}
