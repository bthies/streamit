package streamit.eclipse.debugger.actions;

import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Control;

/**
 * @author kkuo
 */
public class ManageBreakpointRulerInfo implements IVerticalRulerInfo {

	private int fLineNumber;

	public ManageBreakpointRulerInfo() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IVerticalRulerInfo#getControl()
	 */
	public Control getControl() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IVerticalRulerInfo#getLineOfLastMouseButtonActivity()
	 */
	public int getLineOfLastMouseButtonActivity() {
		return fLineNumber;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IVerticalRulerInfo#toDocumentLineNumber(int)
	 */
	public int toDocumentLineNumber(int y_coordinate) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IVerticalRulerInfo#getWidth()
	 */
	public int getWidth() {
		return 0;
	}
	
	protected void setLineNumber(int l) {
		fLineNumber = l;
	}
}
