package sy.corrector.lm.io;

public interface LmReader<V, C extends LmReaderCallback<V>>
{
	public void parse(final C callback_);

}
