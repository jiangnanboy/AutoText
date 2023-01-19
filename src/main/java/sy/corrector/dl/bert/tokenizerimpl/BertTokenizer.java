package sy.corrector.dl.bert.tokenizerimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sy.corrector.dl.bert.tokenizer.Tokenizer;
import sy.corrector.dl.bert.utils.TokenizerUtils;
import utils.common.CollectionUtil;
import utils.common.PropertiesReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2022/5/31 19:03
 */
public class BertTokenizer implements Tokenizer {
	private static final Logger LOGGER = LoggerFactory.getLogger(BertTokenizer.class);
	private String vocabFile = BertTokenizer.class.getClassLoader().getResource(PropertiesReader.get("bert_vocab")).getPath().replaceFirst("/", "");
	private Map<String, Integer> tokenIdMap;
	private Map<Integer, String> idTokenMap;
	private boolean doLowerCase = true;
	private boolean doBasicTokenize = true;
	private List<String> neverSplit;
	public String unkToken = "[UNK]";
	public String sepToken = "[SEP]";
	public String padToken = "[PAD]";
	public String clsToken = "[CLS]";
	public String maskToken = "[MASK]";
	private boolean tokenizeChineseChars = true;
	private BasicTokenizer basicTokenizer;
	private WordpieceTokenizer wordpieceTokenizer;

	private static final int MAX_LEN = 512;

	public BertTokenizer(String vocabFile, boolean doLowerCase, boolean doBasicTokenize, List<String> neverSplit,
			String unkToken, String sepToken, String padToken, String clsToken, String maskToken,
			boolean tokenizeChineseChars) {
		this.vocabFile = vocabFile;
		this.doLowerCase = doLowerCase;
		this.doBasicTokenize = doBasicTokenize;
		this.neverSplit = neverSplit;
		this.unkToken = unkToken;
		this.sepToken = sepToken;
		this.padToken = padToken;
		this.clsToken = clsToken;
		this.maskToken = maskToken;
		this.tokenizeChineseChars = tokenizeChineseChars;
		init();
	}

	public BertTokenizer() throws FileNotFoundException {
		if(Files.exists(Paths.get(vocabFile))) {
			init();
		} else {
			throw new FileNotFoundException("vocab file not exists -> " + vocabFile + ", place check!");
		}
	}

	public BertTokenizer(String vocabFile) throws FileNotFoundException {
		if(Files.exists(Paths.get(vocabFile))) {
			this.vocabFile = vocabFile;
			init();
		} else {
			throw new FileNotFoundException("vocab file not exists -> " + vocabFile + ", place check!");
		}
	}

	private void init() {
		LOGGER.info("init bertTokenizer vocab...");
		try {
			this.tokenIdMap = loadVocab(vocabFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.idTokenMap = CollectionUtil.newHashMap();
		for (String key : tokenIdMap.keySet()) {
			this.idTokenMap.put(tokenIdMap.get(key), key);
		}
		neverSplit = CollectionUtil.newArrayList();
		neverSplit.add(unkToken);
		neverSplit.add(sepToken);
		neverSplit.add(padToken);
		neverSplit.add(clsToken);
		neverSplit.add(maskToken);
		if (doBasicTokenize) {
			this.basicTokenizer = new BasicTokenizer(doLowerCase, neverSplit, tokenizeChineseChars);
		}
		this.wordpieceTokenizer = new WordpieceTokenizer(tokenIdMap, unkToken, neverSplit);
	}

	private Map<String, Integer> loadVocab(String vocabFile) throws IOException {
		LOGGER.info("load vocab ...");
		return TokenizerUtils.generateTokenIdMap(vocabFile);
	}

	@Override
	public List<String> tokenize(String text) {
		List<String> split_tokens = CollectionUtil.newArrayList();
		if (doBasicTokenize) {
			for (String token : basicTokenizer.tokenize(text)) {
				for (String sub_token : wordpieceTokenizer.tokenize(token)) {
					split_tokens.add(sub_token);
				}
			}
		} else {
			split_tokens = wordpieceTokenizer.tokenize(text);
		}
		split_tokens.add(0, "[CLS]");
		split_tokens.add("[SEP]");
		return split_tokens;
	}

	public List<String> basicTokenize(String text) {
		List<String> tokenizeList = basicTokenizer.tokenize(text);
		tokenizeList.add(0, "[CLS]");
		tokenizeList.add("[SEP]");
		return tokenizeList;
	}

	public String convertTokensToString(List<String> tokens) {
		// Converts a sequence of tokens (string) in a single string.
		return tokens.stream().map(s -> s.replace("##", "")).collect(Collectors.joining(" "));
	}

	public List<Integer> convertTokensToIds(List<String> tokens) {
		List<Integer> output = CollectionUtil.newArrayList();
		for (String s : tokens) {
			output.add(tokenIdMap.get(s.toLowerCase()));
		}
		return output;
	}

	public int convertTokensToIds(String token) {
		return tokenIdMap.get(token.toLowerCase());
	}

	public List<String> convertIdsToTokens(List<Integer> ids) {
		List<String> output = CollectionUtil.newArrayList();
		for(int id : ids) {
			output.add(idTokenMap.get(id));
		}
		return output;
	}

	public String convertIdsToTokens(int id) {
		return idTokenMap.get(id);
	}

	public int vocabSize() {
		return tokenIdMap.size();
	}
}
