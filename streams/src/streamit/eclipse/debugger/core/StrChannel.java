/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.core;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.swt.widgets.TableItem;

public class StrChannel {
	
	private TableItem fTI;
	private int fRow;
	
	private String fSource;
	private String fSink;
	
	private String fTotalItemsPushed;
	private String fTotalItemsPopped;
	
	// vector of IVariables
	private Vector fQueue;
	
	public StrChannel(boolean setInput, String filterName) throws Exception {
		fTI = null;
		fRow = -1;

		if (setInput) {
			fSink = filterName;
			fSource = "";
		} else {
			fSource = filterName;
			fSink = "";
		}
		
		fTotalItemsPushed = "0";
		fTotalItemsPopped = "0";
		
		fQueue = new Vector();
	}
	
	public TableItem getTableItem() {
		return fTI;
	}
	
	public int getRow() {
		return fRow;
	}
	
	public String getSource() {
		return fSource;
	}
	
	public String getSink() {
		return fSink;
	}
	
	public String getTotalItemsPushed() {
		return fTotalItemsPushed;
	}

	public String getTotalItemsPopped() {
		return fTotalItemsPopped;
	}
	
	public String getQueueAsString() {
		StringBuffer sb = new StringBuffer();
		Iterator i = fQueue.iterator();
		
		try {
			while (i.hasNext()) {
				sb.insert(0, ((IVariable) i.next()).getValue().getValueString());
				if (i.hasNext()) sb.insert(0, "; ");
			
			}
		} catch (DebugException e) {
		}
		
		return sb.toString();
	}
	
	public IVariable getFirstOnQueue() {
		if (fQueue.isEmpty()) return null;
		return (IVariable) fQueue.firstElement();
	}
	
	public IVariable getLastOnQueue() {
		if (fQueue.isEmpty()) return null;
		return (IVariable) fQueue.lastElement();
	}
	
	public void setTableItem(TableItem t) {
		fTI = t;
	}
	
	public void setRow(int r) {
		fRow = r;
	}
	
	public void setTotalItemsPushed(String s) {
		fTotalItemsPushed = s;
	}
	
	public void setTotalItemsPopped(String s) {
		fTotalItemsPopped = s;
	}
	
	public void startQueue() {
		fQueue.clear();	
	}
	
	public void addQueue(IVariable v) {
		fQueue.add(v);
	}
	
	public String[] toRow() {
		String[] row = new String[IStreamItDebuggerConstants.CHANNEL_VIEW_HEADERS.length];
		row[0] = getSource();
		row[1] = getTotalItemsPushed();
		row[2] = getQueueAsString();
		row[3] = getSink();
		row[4] = getTotalItemsPopped();
		return row;
	}
}