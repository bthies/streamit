/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.core;

import java.util.Vector;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.swt.widgets.TableItem;

public class StrFilter extends StrStream {
	
	private TableItem fTI;
	private int fRow;
	
	private String[] fFields;

	private IVariable fInputVar;
	private IVariable fOutputVar;

	private String fPopCount;
	private String fPushCount;
	private String fPeekCount;

	private String fStage;
	private int fNumWork;
	
	private Vector fPeekItems;
	
	protected StrFilter(String n, String[] f) {
		super(n);
		fTI = null;
		fRow = -1;
		if (f == null) fFields = new String[0];
		else fFields = f; 
		fInputType = IStreamItDebuggerConstants.NULL_VALUE;
		fOutputType = IStreamItDebuggerConstants.NULL_VALUE;		
		fPopCount = IStreamItDebuggerConstants.NOT_APPLICABLE;
		fPushCount = IStreamItDebuggerConstants.NOT_APPLICABLE;
		fPeekCount = IStreamItDebuggerConstants.NOT_APPLICABLE;
		fStage = IStreamItDebuggerConstants.INIT_METHOD;
		fNumWork = 0;
		fPeekItems = new Vector();
	}
	
	protected String[] toRow() {
		String[] row = new String[IStreamItDebuggerConstants.FILTER_VIEW_HEADERS.length];
		row[0] = getName();
		row[1] = getInputType();
		row[2] = getOutputType();
		row[3] = getFields();
		row[4] = getPopCount();
		row[5] = getPushCount();
		row[6] = getPeekCount();
		row[7] = getStage();
		row[8] = getNumWork();
		return row;
	}
	
	protected TableItem getTableItem() {
		return fTI;
	}
	
	protected int getRow() {
		return fRow;
	}
	
	protected String getFields() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < fFields.length; i++) {
			sb.append(fFields[i]);
			if (i != fFields.length - 1) sb.append("; ");
		}
		return sb.toString();
	}

	protected String getInputType() {
		return fInputType;
	}
	
	protected String getOutputType() {
		return fOutputType;
	}

	protected IVariable getInputVar() {
		return fInputVar;
	}
	
	protected IVariable getOutputVar() {
		return fOutputVar;
	}

	protected String getPopCount() {
		return fPopCount;
	}

	protected String getPushCount() {
		return fPushCount;
	}
	
	protected String getPeekCount() {
		return fPeekCount;
	}
	
	protected String getStage() {
		return fStage;
	}
	
	protected String getNumWork() {
		return String.valueOf(fNumWork);
	}
	
	protected Vector getPeekItems() {
		return fPeekItems;
	}

	protected void setTableItem(TableItem t) {
		fTI = t;
	}
	
	protected void setRow(int r) {
		fRow = r;
	}
	
	protected void setInputOutputType(boolean setInput, String type, IVariable var) {
		if (setInput) {
			setInputType(type);
			fInputVar = var;
		} else {
			setOutputType(type);
			fOutputVar = var;
		}
	}
	
	protected void setPushCount(String pc) {
		fPushCount = pc;
	}
		
	protected void setPopCount(String pc) {
		fPopCount = pc;
	}
		
	protected void setPeekCount(String pc) {
		fPeekCount = pc;
	}
	
	protected void setStage(String s) {
		fStage = s;
	}
	
	protected void worked() {
		fNumWork++;
		// TODO this sound be unnecessary!
		if (fNumWork > 0) setStage(IStreamItDebuggerConstants.WORK_METHOD);
	}
	
	protected void workEntry() {
		fPeekItems = new Vector();
	}
	
	protected void setPeekItems(Vector items) {
		fPeekItems = items;
	}
}