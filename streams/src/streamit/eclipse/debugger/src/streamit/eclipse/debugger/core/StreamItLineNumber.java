package streamit.eclipse.debugger.core;

import org.eclipse.core.resources.IFile;

/**
 * @author kkuo
 */
public class StreamItLineNumber {
	private int fLineNumber;
	private IFile fStrFile;
		
	public StreamItLineNumber(int i, IFile strFile) {
		fLineNumber = i;
		fStrFile = strFile;
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
	public IFile getStrFile() {
		return fStrFile;
	}
}
