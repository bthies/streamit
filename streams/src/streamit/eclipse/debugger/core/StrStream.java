package streamit.eclipse.debugger.core;


/**
 * @author kkuo
 */
public abstract class StrStream {

	protected String fName;
	protected String fInputType;
	protected String fOutputType;
	
	protected StrStream(String name) {
		fName = name;
	}
	
	protected String getName() {
		return fName;
	}
	
	/**
	 * @param string
	 */
	protected void setInputType(String s) {
		fInputType = s;
	}
	/**
	 * @param string
	 */
	protected void setOutputType(String s) {
		fOutputType = s;
	}
}
