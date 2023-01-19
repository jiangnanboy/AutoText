package sy.corrector.lm.map;

interface HashMap
{

	
	public long put(final long key);

	public long getOffset(final long key);

	public double getLoadFactor();

	public long getCapacity();

	public long getKey(long contextOffset);

	public boolean isEmptyKey(long key);

	public long size();

	public Iterable<Long> keys();

	

	public boolean hasContexts(int word);

}