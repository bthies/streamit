/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.core;

import java.util.Vector;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventFilter;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewPart;

import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.ui.ChannelView;

public class StreamItDebugEventFilter implements IDebugEventFilter {
	
	private static StreamItDebugEventFilter fInstance = new StreamItDebugEventFilter();

	/**
	 * Creates a new StreamItDebugEventSetListener.
	 */
	private StreamItDebugEventFilter() {
		DebugPlugin.getDefault().addDebugEventFilter(this);
	}
	/**
	 * Returns the singleton StreamItDebugEventSetListener.
	 */
	public static StreamItDebugEventFilter getInstance() {
		return fInstance;
	}
	
	public DebugEvent[] filterDebugEvents(DebugEvent[] events) {
		try {
			Vector filteredEvents = new Vector();
			for (int i = 0; i < events.length; i++) {
				int kind = events[i].getKind(); 
				if (kind != DebugEvent.SUSPEND) {
					filteredEvents.add(events[i]);
					continue;	
				} 
				
				int detail = events[i].getDetail();
				if (detail == DebugEvent.BREAKPOINT || detail == DebugEvent.STEP_END) {
					if (!handleSuspend(events[i])) filteredEvents.add(events[i]);
				} else {
					filteredEvents.add(events[i]);
				} 
			}
			if (filteredEvents.size() == 0) return null;

			DebugEvent[] toReturn = new DebugEvent[filteredEvents.size()];
			filteredEvents.toArray(toReturn);
			return toReturn;
		} catch (Exception e) {
		}
		return events;
	}
	
	public void beforeLaunch() {
		Table t = getTable();
		if (t != null) t.removeAll();
	}

	protected boolean handleSuspend(DebugEvent event) throws Exception {
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return false;

		// only handle if StreamIt Launch
		IThread thread = (IThread) o;
		ILaunch launch = thread.getLaunch();
		if (launch.getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return false;

		// get cause of launch
		IStackFrame top = thread.getTopStackFrame();
		if (top == null) return false;
		ISourceLocator locator = launch.getSourceLocator();
		o = locator.getSourceElement(top);

		// only handle if from .class
		if (!(o instanceof IClassFile)) return false;
		IClassFile unit = (IClassFile) o;
		
		IBreakpoint[] b = thread.getBreakpoints();
		for (int i = 0; i < b.length; i++) {
			if (b[i] instanceof IJavaWatchpoint) {
				handleWatchpoint(unit, top);
				top.resume();
				return true;
			} 
		}
		return false;
	}
	
	protected void handleWatchpoint(IClassFile unit, IStackFrame top) throws Exception {
		// get channel variable
		if (!(top instanceof IJavaStackFrame)) return;
		IJavaStackFrame javaTop = (IJavaStackFrame) top;
		IJavaVariable var = javaTop.findVariable(IStreamItDebuggerConstants.THIS_FIELD);

		// TODO
		if (true) return;
	
		// get channel
		Object o = LaunchData.getChannel(var.getValue());
		if (o == null) return; // haven't gotten to init stage yet
		StrChannel channel = (StrChannel) o; 

		// find variables (totalItemsPushed, totalItemsPopped, queue)
		var = javaTop.findVariable(IStreamItDebuggerConstants.TOTALITEMSPUSHED_FIELD);
		channel.setTotalItemsPushed(var.getValue().getValueString());
		var = javaTop.findVariable(IStreamItDebuggerConstants.TOTALITEMSPOPPED_FIELD);
		channel.setTotalItemsPopped(var.getValue().getValueString());
		var = javaTop.findVariable(IStreamItDebuggerConstants.QUEUE_FIELD);
		IVariable[] queueVars = var.getValue().getVariables();
		for (int j = 0; j < queueVars.length; j++) {
			if (queueVars[j].getName().equals(IStreamItDebuggerConstants.HEADER_FIELD) && queueVars[j].getReferenceTypeName().equals(IStreamItDebuggerConstants.LINKEDLISTENTRY_CLASS)) {
				IValue val = queueVars[j].getValue();
				setQueue(channel, val, val.getVariables()); 
				break;
			}
		}
		
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(channel));
	}
	
	protected void setQueue(StrChannel channel, IValue header, IVariable[] nextVars) throws Exception {
		// find next & element variable
		String varName, varType;
		IVariable next = null;
		IVariable element = null;
		IVariable[] elementVars;
		for (int i = 0; i < nextVars.length; i++) {
			varName = nextVars[i].getName();
			varType = nextVars[i].getReferenceTypeName();
			if (varName.equals(IStreamItDebuggerConstants.NEXT_FIELD) && varType.equals(IStreamItDebuggerConstants.LINKEDLISTENTRY_CLASS)) {
				next = nextVars[i];
			} else if (varName.equals(IStreamItDebuggerConstants.ELEMENT_FIELD)) {
				element = nextVars[i];
			}
		}
	
		// save element unless it is null (element is null at the beginning of recursion)
		if (element.getValue().getValueString().equals(IStreamItDebuggerConstants.NULL_VALUE)) channel.startQueue();
		else {
			elementVars = element.getValue().getVariables();
			for (int j = 0; j < elementVars.length; j++) {
				if (elementVars[j].getName().equals(IStreamItDebuggerConstants.VALUE_FIELD)) {
					channel.addQueue(elementVars[j]);
					break;
				}
			}
		}
		
		// if next variable == header, you've come full circle
		if (next.getValue().equals(header)) return;
		
		// continue to the following next
		setQueue(channel, header, next.getValue().getVariables());				
	}

	protected Table getTable() {
		IViewPart viewPart = StreamItDebugEventSetListener.getActivePage().findView(IStreamItDebuggerConstants.ID_CHANNELVIEW);
		if (viewPart == null) return null;
		return ((ChannelView) viewPart).getTable();
	}

	protected Runnable updateTable(final StrChannel c) {
		return new Runnable() {
			public void run() {

				// update graph view
				/*
				GraphViewer v = StreamItDebugEventSetListener.getInstance().getGraphViewer(StreamItDebugEventSetListener.getActivePage());
				if (v != null) {
					String name = c.getSource();
					IVariable datum = c.getFirstOnQueue();
					boolean source = false;
					if (name == "") { 
						name = c.getSink();
						datum = c.getLastOnQueue();
						source = true;
					}
					v.updateChannel(name.substring(0, name.indexOf(" ")), source, datum, LaunchData.isHighlighted(datum));
				}
				*/
				
				// update/add channel to table
				Table t = getTable();
				if (t == null) return;

				TableItem ti = c.getTableItem();
				int row = c.getRow();
				if (ti == null || row < 0) {
					c.setRow(t.getItemCount());
					ti = new TableItem(t, SWT.LEFT);
					c.setTableItem(ti);
				}
				ti.setText(c.toRow());	
				t.setSelection(row);
			}
		};
	}
}