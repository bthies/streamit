package streamit.eclipse.debugger.core;

/**
 * @author kkuo
 */
public class StreamItLineNumber {
	private int fLineNumber;
	private String fStrFileName;
		
	public StreamItLineNumber(int i, String s) {
		fLineNumber = i;
		fStrFileName = s;
	}
		
	/**
	 * @return
	 */
	public int getLineNumber() {
		return fLineNumber;
	}

	/**
	 * @return
	 */
	public String getStrFileName() {
		return fStrFileName;
	}
}
