/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class StreamItDebugEventSetListener implements IDebugEventSetListener {
	
	protected static Map filters;
	// 0 = init, 1 = work, 2 = prework, 3 = others
	protected static List[] allBreakpoints = new List[4];
	//	map java resources to map str line numbers to java line numbers
	protected static Map fJavaFiles;

	protected static final int INIT_BREAKPOINTS = 0;
	protected static final int WORK_BREAKPOINTS = 1;
	protected static final int PREWORK_BREAKPOINTS = 2;
	protected static final int OTHER_BREAKPOINTS = 3;
	protected static final int UPDATE_ALL = 0;
	protected static final int UPDATE_WORK = 1;
	protected static final int UPDATE_STAGE = 2;

	public void handleDebugEvents(DebugEvent[] events) {
		try {
			for (int i = 0; i < events.length; i++) {
				int kind = events[i].getKind(); 
				if (kind == DebugEvent.SUSPEND) {
					int detail = events[i].getDetail();
					if (detail == DebugEvent.BREAKPOINT || detail == DebugEvent.STEP_END) handleSuspend(events[i]); 
				} else if (kind == DebugEvent.TERMINATE && events[i].getSource() instanceof IDebugTarget) {
					// do unset entries for entries that were changed from true
					Iterator iter;
					IJavaMethodBreakpoint b;
					for (int j = 0; j < 4; j++) {
						iter = allBreakpoints[j].iterator();
						while (iter.hasNext()) {
							b = (IJavaMethodBreakpoint) iter.next();
							if (b.getMarker().getAttribute(StreamItLauncherPlugin.RESUME, true)) b.setEntry(false);
						}
					}
				}
			}
		} catch (Exception e) {
		}
	}
	
	// redirect from .java to .str
	protected void handleSuspend(DebugEvent event) throws Exception {
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return;

 		// only handle if StreamIt Launch
		IThread thread = (IThread) o;
		ILaunch launch = thread.getLaunch();
		if (!launch.getLaunchConfiguration().getName().equals(StreamItLauncherPlugin.ID_STR_APPLICATION)) return;

		// get cause of launch
		IStackFrame top = thread.getTopStackFrame();
		if (top == null) return;
		ISourceLocator locator = launch.getSourceLocator();
		o = locator.getSourceElement(top);
			
		// only handle if from .java
		if (!(o instanceof ICompilationUnit)) return;
		ICompilationUnit unit = (ICompilationUnit) o;
		IFile javaFile = (IFile) unit.getResource();
			
		// get mapping if already mapped
		Map javaToStr = null;
		if (fJavaFiles == null) fJavaFiles = new HashMap();
		else if (fJavaFiles.containsKey(javaFile)) javaToStr = (Map) fJavaFiles.get(javaFile); 
			
		// map if not already mapped
		String javaFileName = javaFile.getName();
		String strFileName = javaFileName.substring(0, javaFileName.lastIndexOf('.' + StreamItLauncherPlugin.JAVA_FILE_EXTENSION)) + '.' + StreamItLauncherPlugin.STR_FILE_EXTENSION;
		if (javaToStr == null) {
			javaToStr = mapJavaToStr(javaFile, strFileName);
			fJavaFiles.put(javaFile, javaToStr);
		}
		
		// make sure that a corresponding line exists
		o = javaToStr.get(new Integer(top.getLineNumber()));
		if (o == null) {
			// if init or work fn, handle exit debugging
			IBreakpoint[] b = thread.getBreakpoints();
			for (int i = 0; i < b.length; i++) {
				if (allBreakpoints[INIT_BREAKPOINTS].contains(b[i])) handleInitExit(unit, top);
				else if (allBreakpoints[WORK_BREAKPOINTS].contains(b[i])) handleWorkExit(unit, top);
				else if (allBreakpoints[OTHER_BREAKPOINTS].contains(b[i])) handleOtherExit(unit, top);
			}
			top.resume();
			return;
		}
			
		// highlight in .str
		DebugUIPlugin.getStandardDisplay().syncExec(getHighlighter(javaFile.getProject().getFile(strFileName), 
													((Integer) o).intValue() - 1));
			
		// if prework or work fn, handle entry debugging 
		IBreakpoint[] b = thread.getBreakpoints();
		boolean resume = false;
		for (int i = 0; i < b.length; i++) {
			if (allBreakpoints[WORK_BREAKPOINTS].contains(b[i])) handleEntry(unit, top, false, StreamItLauncherPlugin.WORK_METHOD);
			else if (allBreakpoints[PREWORK_BREAKPOINTS].contains(b[i])) handleEntry(unit, top, false, StreamItLauncherPlugin.PREWORK_METHOD);
			else if (allBreakpoints[OTHER_BREAKPOINTS].contains(b[i])) handleEntry(unit, top, true, '(' + ((IJavaMethodBreakpoint) b[i]).getMethodName() + ')');
			else continue;
			resume = b[i].getMarker().getAttribute(StreamItLauncherPlugin.RESUME, resume);					
		}
		if (resume) top.resume();
		return;
	}

	protected static Map mapJavaToStr(IFile javaFile, String fStrFileName) throws Exception {
		Map javaToStr = new HashMap();

		// look for commented mappings
		InputStreamReader isr = new InputStreamReader(javaFile.getContents());
		BufferedReader br = new BufferedReader(isr);
		String strLine = null;
		int javaLineNumber = 0;
		String token = "// " + fStrFileName + ':';
			
		while (true) {
			javaLineNumber++;
			strLine = br.readLine();
			if (strLine == null) return javaToStr;
			if (strLine.indexOf(token) != -1) javaToStr.put(new Integer(javaLineNumber), Integer.valueOf(strLine.substring(strLine.indexOf(token) + token.length()))); 	
		}
	}
	
	protected Runnable getHighlighter(final IFile strFile, final int lineNumber) {
		return new Runnable() {									
			public void run() {
				try {
					// open .str if not already open					
					ITextEditor fTextEditor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(strFile).getAdapter(ITextEditor.class);											
					if (fTextEditor == null) return;

					// goto marker			
					Iterator i = fTextEditor.getDocumentProvider().getAnnotationModel(fTextEditor.getEditorInput()).getAnnotationIterator();
					while (i.hasNext()) {
						Object o = i.next();
						if (o instanceof MarkerAnnotation) {
							IMarker m = ((MarkerAnnotation) o).getMarker();
							o = m.getAttribute(IMarker.LINE_NUMBER);														
							if (o != null && ((Integer) o).intValue() == lineNumber + 1) {
								fTextEditor.gotoMarker(m);
								return;
							}															
						}													
					}
					
					// highlight text just in case search for marker fails
					IDocument doc = fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber);
					fTextEditor.getEditorSite().getSelectionProvider().setSelection(new TextSelection(doc, strLine.getOffset(), strLine.getLength()));					
				} catch (Exception e) {
				}
			}
		};
	}

	public static void addJavaToStrMap(IFile javaFile, Map javaToStr) {
		if (fJavaFiles == null) fJavaFiles = new HashMap();
		fJavaFiles.put(javaFile, javaToStr);
	}
	
	public static Map getJavaToStrMap(IFile javaFile) {
		if (fJavaFiles == null) return null;
		return (Map) fJavaFiles.get(javaFile);
	}

	public static void beforeLaunch() {
		for (int i = 0; i < allBreakpoints.length; i++) allBreakpoints[i] = new Vector();
		filters = new HashMap();

		Table t = getTable();
		if (t != null) t.removeAll();
	}

	protected static Table getTable() {
		// clear table
		IViewReference[] views = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (int j = 0; j < views.length; j++) {
			if (views[j].getId().equals(StreamItLauncherPlugin.ID_FILTERVIEW)) return ((FilterView) views[j].getView(true)).getTable();
		}
		return null;
	}
	
	public static void addFilter(String name, String[] fields) {
		filters.put(name, new StrFilter(name, fields));
	}
	
	public static void addBreakpoint(int methodType, boolean resume, IJavaMethodBreakpoint breakpoint) throws Exception {
		allBreakpoints[methodType].add(breakpoint);
		breakpoint.getMarker().setAttribute(StreamItLauncherPlugin.RESUME, resume);
	}

	// set input and output types; Number of pops, pushes, peeks	
	protected void handleInitExit(ICompilationUnit unit, IStackFrame top) throws Exception {
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = getFilter(vars);
		if (filter == null) return;
		
		// look at subclassed Filter var
		vars = vars[0].getValue().getVariables();
		String varName;
		IValue varValue;
		boolean setInput;
				
		// iterate through Filter vars
		for (int i = 0; i < vars.length; i++) {
			if (!vars[i].getReferenceTypeName().equals(StreamItLauncherPlugin.CHANNEL_CLASS)) continue;
			varName = vars[i].getName(); 					
			if (varName.equals(StreamItLauncherPlugin.INPUT_FIELD)) setInput = true;
			else if (varName.equals(StreamItLauncherPlugin.OUTPUT_FIELD)) setInput = false; 
			else continue;
				varValue = vars[i].getValue();
			setFilterInfo(filter, setInput, varValue.getVariables());
		}
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_ALL));
	}
	
	protected StrFilter getFilter(IVariable[] vars) throws Exception {
		if (vars.length < 1) return null;
		StrFilter filter = null;
		return (StrFilter) filters.get(vars[0].getReferenceTypeName());
	}
	
	protected void setFilterInfo(StrFilter filter, boolean setInput, IVariable[] vars) throws Exception {
		StringTokenizer st;
		String type = StreamItLauncherPlugin.VOID_VALUE;
		String pc;
		boolean setPeek;

		// iterate through Channel variables
		String varName, varType;
		for (int i = 0; i < vars.length; i++) {
			varName = vars[i].getName();
			varType = vars[i].getReferenceTypeName();
			if (varName.equals(StreamItLauncherPlugin.TYPE_FIELD) && varType.equals(StreamItLauncherPlugin.CLASS_CLASS)) {
				st = new StringTokenizer(vars[i].getValue().getValueString(), "()");
				if (st.countTokens() < 1) continue;
				type = st.nextToken();
			} else if (varType.equals(StreamItLauncherPlugin.INTEGER_CLASS)) {
				if (varName.equals(StreamItLauncherPlugin.POP_PUSH_COUNT_FIELD)) setPeek = false;
				else if (varName.equals(StreamItLauncherPlugin.PEEK_COUNT_FIELD)) setPeek = true;
				else continue;
				
				IVariable[] intVars = vars[i].getValue().getVariables();
				for (int j = 0; j < intVars.length; j++) {
					if (intVars[j].getName().equals(StreamItLauncherPlugin.VALUE_FIELD) && intVars[j].getReferenceTypeName().equals(StreamItLauncherPlugin.INT_VALUE)) {
						pc = intVars[j].getValue().getValueString();
						if (setPeek) filter.setPeekCount(pc);
						else {
							if (setInput) filter.setPopCount(pc);
							else filter.setPushCount(pc);
						}
					}
				}
			}
		}
		filter.setInputOutputType(setInput, type);
	}
	
	protected Runnable updateTable(final StrFilter f, final int i) {
		return new Runnable() {	
			public void run() {
				// update/add filter to table
				// i == 0 => called from init
				// i == 1 => called from work
				// i == 2 => called from entry
				switch (i) {
					case 0:
						Table t = getTable();
						if (t == null) return;
						f.setRow(t.getItemCount());
						TableItem ti = new TableItem(t, SWT.LEFT);
						f.setTableItem(ti);
						ti.setText(f.toRow());
						break;
					case 1:
						ti = f.getTableItem();
						int row = f.getRow();
						if (ti == null || row < 0) return;
						ti.setText(StreamItLauncherPlugin.FILTER_VIEW_HEADERS.length - 1, f.getNumWork());
						break;
					case 2:
						t = getTable();
						ti = f.getTableItem();
						if (t == null || ti == null) return;
						ti.setText(StreamItLauncherPlugin.FILTER_VIEW_HEADERS.length - 2, f.getStage());
						t.setSelection(f.getRow());
						break;
				}
			}
		};
	}

	protected void handleWorkExit(ICompilationUnit unit, IStackFrame top) throws Exception {
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = getFilter(vars);
		if (filter == null) return;

		// increment work
		filter.worked();
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_WORK));
	}
	
	protected void handleOtherExit(ICompilationUnit unit, IStackFrame top) throws Exception {
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = getFilter(vars);
		if (filter == null) return;

		// update stage (remove phase)
		String stage = filter.getStage();
		filter.setStage(stage.substring(0, stage.indexOf('(')));
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_STAGE));
	}
	
	
	protected void handleEntry(ICompilationUnit unit, IStackFrame top, boolean otherBreakpoints, String methodName) throws Exception {
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = getFilter(vars);
		if (filter == null) return;
		
		// update stage (add phase)
		if (otherBreakpoints) filter.setStage(filter.getStage() + methodName);
		else filter.setStage(methodName);
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_STAGE));
	}

	/*	
		1.8 Tabular display for a tape
		  1.8.1 Number of items
		  1.8.2 Values

		? show values of state variables
		see package org.eclipse.debug.internal.ui.views.variables;

		1.9 Modification of values on a tape
		1.10 Static scheduling array - use large switch statement to execute filter by getting input from user
		1.11 Bill - "tag" some data items and follow their effects through the stream graph... for instance, 
		color some items blue at the top, and then all items computed based on those (i.e., produced in a filter 
		firing that consumed some of those) are also blue later down.		
	*/
}