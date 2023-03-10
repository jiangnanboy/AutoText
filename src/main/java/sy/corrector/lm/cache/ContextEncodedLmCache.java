package sy.corrector.lm.cache;

import java.io.Serializable;

import sy.corrector.lm.ContextEncodedNgramLanguageModel.LmContextInfo;
import sy.corrector.lm.util.Annotations.OutputParameter;

public interface ContextEncodedLmCache extends Serializable
{
	/**
	 * Should return Float.NaN if requested n-gram is not in the cache.
	 * 
	 * @param contextOffset
	 * @param contextOrder
	 * @param word
	 * @param hash
	 * @param outputPrefix
	 * @return
	 */
	public float getCached(long contextOffset, int contextOrder, int word, int hash, @OutputParameter LmContextInfo outputPrefix);

	public void putCached(long contextOffset, int contextOrder, int word, float prob, int hash, @OutputParameter LmContextInfo outputPrefix);

	public int capacity();

}
