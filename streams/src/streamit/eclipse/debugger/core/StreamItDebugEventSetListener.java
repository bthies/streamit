/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import streamit.eclipse.debugger.graph.StreamView;
import streamit.eclipse.debugger.graph.StreamViewer;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.ui.FilterView;
import streamit.eclipse.debugger.ui.LaunchViewerFilter;
import streamit.eclipse.debugger.ui.StreamItModelPresentation;
import streamit.eclipse.debugger.ui.VariableViewerFilter;

public class StreamItDebugEventSetListener implements IDebugEventSetListener, IPartListener2 {
	
	private static StreamItDebugEventSetListener fInstance = new StreamItDebugEventSetListener();

	// key = String absoluteJavaFileName, entry = LaunchData
	private static HashMap fLaunchData = new HashMap();

	private static LaunchViewerFilter fLaunchViewerFilter = new LaunchViewerFilter();
	private static StreamItModelPresentation fStreamItModelPresentation = new StreamItModelPresentation();
	private static VariableViewerFilter fVariableViewerFilter = new VariableViewerFilter();

	// constants
	private static final int UPDATE_ALL = 0;
	private static final int UPDATE_WORK = 1;
	private static final int UPDATE_STAGE = 2;
	
	/**
	 * Creates a new StreamItDebugEventSetListener.
	 */
	private StreamItDebugEventSetListener() {
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
	/**
	 * Returns the singleton StreamItDebugEventSetListener.
	 */
	public static StreamItDebugEventSetListener getInstance() {
		return fInstance;
	}
	
	public void handleDebugEvents(DebugEvent[] events) {
		try {
			for (int i = 0; i < events.length; i++) {
				int kind = events[i].getKind(); 
				if (kind == DebugEvent.SUSPEND) {
					int detail = events[i].getDetail();
					if (detail == DebugEvent.BREAKPOINT || detail == DebugEvent.STEP_END) handleSuspend(events[i]); 
				} else if (kind == DebugEvent.TERMINATE && events[i].getSource() instanceof IDebugTarget) {
					terminate(events[i]);
				}
			}
		} catch (Exception e) {
		}
	}
	
	// redirect from .java to .str
	private void handleSuspend(DebugEvent event) {
	
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return;

 		// only handle if StreamIt Launch
		IThread thread = (IThread) o;
		ILaunch launch = thread.getLaunch();
		if (launch.getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return;

		// get cause of launch
		IStackFrame top = null;
		try {
			top = thread.getTopStackFrame();
		if (top == null) return;
		ISourceLocator locator = launch.getSourceLocator();
		o = locator.getSourceElement(top);
		
		// only handle if from .java
		if (!(o instanceof ICompilationUnit)) return;
		ICompilationUnit unit = (ICompilationUnit) o;
		IFile javaFile = (IFile) unit.getResource();
		
		// already mapped, get line number
		LaunchData launchData = getLaunchData(javaFile.getLocation().toOSString());
		int lineNumber = launchData.getLineNumber(javaFile, new Integer(top.getLineNumber()));
		
		// make sure that a corresponding line exists
		if (lineNumber < 1) {
			// if init or work fn, handle exit debugging
			IBreakpoint[] b = thread.getBreakpoints();
			for (int i = 0; i < b.length; i++) {				
				if (launchData.containsInitBreakpoint(b[i])) handleInitExit(unit, top, thread.getStackFrames(), launchData);
				else if (launchData.containsWorkBreakpoint(b[i])) handleWorkExit(unit, top);
				else if (launchData.containsOtherBreakpoint(b[i])) handleOtherExit(unit, top);
			}
			top.resume();
			return;
		}
		
		// highlight in .str
		DebugUIPlugin.getStandardDisplay().syncExec(getSelecter(javaFile.getProject().getFile(getStrFileName(javaFile)), lineNumber - 1, top));

		// if prework or work fn, handle entry debugging 
		IBreakpoint[] b = thread.getBreakpoints();
		boolean resume = false;
		for (int i = 0; i < b.length; i++) {
			if (!(b[i] instanceof IJavaMethodBreakpoint)) {
				resume = false;
				break;
			}

			if (launchData.containsInitBreakpoint(b[i])) handleInitEntry(top);
			else if (launchData.containsWorkBreakpoint(b[i])) handleEntry(unit, top, IStreamItDebuggerConstants.WORK_BREAKPOINTS, IStreamItDebuggerConstants.WORK_METHOD);
			else if (launchData.containsPreworkBreakpoint(b[i])) handleEntry(unit, top, IStreamItDebuggerConstants.PREWORK_BREAKPOINTS, IStreamItDebuggerConstants.PREWORK_METHOD);
			else if (launchData.containsOtherBreakpoint(b[i])) handleEntry(unit, top, IStreamItDebuggerConstants.OTHER_BREAKPOINTS, '(' + ((IJavaMethodBreakpoint) b[i]).getMethodName() + ')');
			else continue;
			resume = b[i].getMarker().getAttribute(IStreamItDebuggerConstants.RESUME, resume);			
		}
		if (resume) top.resume();
		return;
		} catch (Exception e) {
			try {
				top.resume();
			} catch (Exception ef) {
			}
		}

	}
	
	public static String getStrFileName(IFile javaFile) {
		String javaFileName = javaFile.getName();
		return javaFileName.substring(0, javaFileName.lastIndexOf('.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION)) + '.' + IStreamItLaunchingConstants.STR_FILE_EXTENSION;
	}

	public static LaunchData getLaunchData(String absoluteJavaFileName) {
		return (LaunchData) fLaunchData.get(absoluteJavaFileName);
	}
	
	public static void removeLaunchData(String absoluteJavaFileName) {
		fLaunchData.remove(absoluteJavaFileName);
	}

	private Runnable getSelecter(final IFile strFile, final int lineNumber, final IStackFrame top) {
		return new Runnable() {									
			public void run() {
				try {
					// highlight in graph
					IVariable[] vars = top.getVariables();
					//selectGraphs(vars[0].getReferenceTypeName());
										
					// open .str if not already open					
					ITextEditor fTextEditor = (ITextEditor) getActivePage().openEditor(strFile).getAdapter(ITextEditor.class);											
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

	public static int getLineNumber(IFile javaFile, int javaLineNumber) {
		// get mapping if already mapped
		LaunchData launchData = getLaunchData(javaFile.getLocation().toOSString());
		if (launchData == null) return -1;
		return launchData.getLineNumber(javaFile, new Integer(javaLineNumber));
	}
	
	public static String[] getFilterVariables(String absoluteJavaFileName, String filterName) {
		return getLaunchData(absoluteJavaFileName).getFilterVariables(filterName);
	}

	public static void addBreakpoint(String absoluteJavaFileName, int methodType, boolean resume, IJavaMethodBreakpoint breakpoint) throws Exception {
		getLaunchData(absoluteJavaFileName).addBreakpoint(methodType, resume, breakpoint);
	}

	public LaunchData beforeLaunch(String absoluteJavaFileName, String javaFileName, IFile javaFile) throws Exception {
		LaunchData launchData = new LaunchData();
		fLaunchData.put(absoluteJavaFileName, launchData);

		// create all .java to .str file mappings
		launchData.mapJavaToStr(javaFile);
		
		// create view tools
		addViewFilters();

		Table t = getTable();
		if (t != null) t.removeAll();
		
		StreamItDebugEventFilter.getInstance().beforeLaunch();
		
		// wipe graph
		StreamViewer v = getStreamViewer(getActivePage());
		if (v != null) v.setInput(null); 
		
		return launchData;
	}
	
	protected static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	
	private Table getTable() {
		IViewPart viewPart = getActivePage().findView(IStreamItDebuggerConstants.ID_FILTERVIEW);
		if (viewPart == null) return null;
		return ((FilterView) viewPart).getTable();
	}

	// set input and output types; Number of pops, pushes, peeks; channel	
	private void handleInitExit(ICompilationUnit unit, IStackFrame top, IStackFrame[] frames, LaunchData launchData) throws Exception {
		// TODO
		if (true) return;
		
		// create filter, add to global Map of filters
		IVariable[] vars = top.getVariables();
		String streamName = vars[0].getReferenceTypeName();
		String streamNameWithId = streamName + vars[0].getValue().getValueString();

		if (!(top instanceof IJavaStackFrame)) return;
		IJavaStackFrame javaTop = (IJavaStackFrame) top;
		
		// try to create a pipeline
		StrPipeline pipeline = launchData.createPipeline(vars[0], streamName, streamNameWithId);
		if (pipeline != null) {
			getStreamInfo(pipeline, javaTop, launchData);
			return;
		}
		
		// try to create a splitjoin
		StrSplitJoin splitjoin = launchData.createSplitjoin(vars[0], streamName, streamNameWithId);
		if (pipeline != null) {
			getStreamInfo(splitjoin, javaTop, launchData);
			return;
		}

		// create a filter
		StrFilter filter = launchData.createFilter(vars[0], streamName, streamNameWithId);
		if (filter == null) return;

		// find input/output variables
		setFilterInfo(filter, true, javaTop.findVariable(IStreamItDebuggerConstants.INPUT_FIELD), vars[0]);
		setFilterInfo(filter, false, javaTop.findVariable(IStreamItDebuggerConstants.OUTPUT_FIELD), vars[0]);
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_ALL));
	}
	
	private void getStreamInfo(StrStream stream, IJavaStackFrame javaTop, LaunchData launchData) throws DebugException {
		stream.setInputType(getType(javaTop.findVariable(IStreamItDebuggerConstants.INPUT_FIELD)));
		stream.setOutputType(getType(javaTop.findVariable(IStreamItDebuggerConstants.OUTPUT_FIELD)));
	}

	private String getType(IVariable var) throws DebugException {
		IVariable[] vars = var.getValue().getVariables();
		StringTokenizer st;
		String type = IStreamItDebuggerConstants.VOID_VALUE;
		String pc;
		boolean setPeek;

		// iterate through Channel variables
		String varName, varType;
		for (int i = 0; i < vars.length; i++) {
			varName = vars[i].getName();
			varType = vars[i].getReferenceTypeName();
			if (varName.equals(IStreamItDebuggerConstants.TYPE_FIELD) && varType.equals(IStreamItDebuggerConstants.CLASS_CLASS)) {
				st = new StringTokenizer(vars[i].getValue().getValueString(), "()");
				if (st.countTokens() < 1) continue;
				return st.nextToken();
			}
		}
		return type;		
	}
	
	private void setFilterInfo(StrFilter filter, boolean setInput, IVariable var, IVariable filterVar) throws Exception {
		IVariable[] vars = var.getValue().getVariables();
		StringTokenizer st;
		String type = IStreamItDebuggerConstants.VOID_VALUE;
		String pc;
		boolean setPeek;

		// iterate through Channel variables
		String varName, varType;
		for (int i = 0; i < vars.length; i++) {
			varName = vars[i].getName();
			varType = vars[i].getReferenceTypeName();
			if (varName.equals(IStreamItDebuggerConstants.TYPE_FIELD) && varType.equals(IStreamItDebuggerConstants.CLASS_CLASS)) {
				st = new StringTokenizer(vars[i].getValue().getValueString(), "()");
				if (st.countTokens() < 1) continue;
				type = st.nextToken();
			} else if (varType.equals(IStreamItDebuggerConstants.INTEGER_CLASS)) {
				if (varName.equals(IStreamItDebuggerConstants.POP_PUSH_COUNT_FIELD)) setPeek = false;
				else if (varName.equals(IStreamItDebuggerConstants.PEEK_COUNT_FIELD)) setPeek = true;
				else continue;
				
				IVariable[] intVars = vars[i].getValue().getVariables();
				for (int j = 0; j < intVars.length; j++) {
					if (intVars[j].getName().equals(IStreamItDebuggerConstants.VALUE_FIELD) && intVars[j].getReferenceTypeName().equals(IStreamItDebuggerConstants.INT_VALUE)) {
						pc = intVars[j].getValue().getValueString();
						if (setPeek) filter.setPeekCount(pc);
						else if (setInput) filter.setPopCount(pc);
						else filter.setPushCount(pc);
					}
				}
			}
		}

		filter.setInputOutputType(setInput, type, var);
		LaunchData.addChannel(var.getValue(), setInput, filter.getName());
	}
	
	private Runnable updateTable(final StrFilter f, final int updateCode) {
		return new Runnable() {	
			public void run() {
				// update/add filter to table
				// i == 0 => called from init
				// i == 1 => called from work
				// i == 2 => called from entry
				try {
				
				switch (updateCode) {
					case UPDATE_ALL:
						Table t = getTable();
						if (t == null) return;
						f.setRow(t.getItemCount());
						TableItem ti = new TableItem(t, SWT.LEFT);
						f.setTableItem(ti);
						ti.setText(f.toRow());
						break;
					case UPDATE_WORK:
						//GraphViewer v = getGraphViewer(getActivePage());
						//String name = f.getName();
						//if (v != null) v.updateWork(name.substring(0, name.indexOf(" ")), f.getNumWork());
						ti = f.getTableItem();
						int row = f.getRow();
						if (ti == null || row < 0) return;
						ti.setText(IStreamItDebuggerConstants.FILTER_VIEW_HEADERS.length - 1, f.getNumWork());
						break;
					case UPDATE_STAGE:
						//v = getGraphViewer(getActivePage());
						//name = f.getName();
						//if (v != null) v.updateStage(name.substring(0, name.indexOf(" ")), f.getStage());
						t = getTable();
						ti = f.getTableItem();
						if (t == null || ti == null) return;
						ti.setText(IStreamItDebuggerConstants.FILTER_VIEW_HEADERS.length - 2, f.getStage());
						t.setSelection(f.getRow());
						break;
				}
				} catch(Exception e) {
				}
			}
		};
	}
	
	private void handleWorkExit(ICompilationUnit unit, IStackFrame top) throws Exception {
		// TODO
		if (true) return;
		
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = LaunchData.getFilter(vars);
		if (filter == null) return;

		// for tagging:  tag all items that have been pushed
		// if items were peeked, then also do modifications to last tagged item
		String pushCount = filter.getPushCount();
		if (!pushCount.equals(IStreamItDebuggerConstants.NOT_APPLICABLE)) tagPushedItems(top, filter, pushCount);
		
		// increment work
		filter.worked();
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_WORK));
	}

	private void tagPushedItems(IStackFrame top, StrFilter filter, String pushCount) throws Exception {
		// get pushed items (output channel variable => sink => output => queue)

		// get sink
		IVariable var = findVariable(filter.getOutputVar(), IStreamItDebuggerConstants.SINK_FIELD, IStreamItDebuggerConstants.OPERATOR_CLASS);

		// get output
		var = findVariable(var, IStreamItDebuggerConstants.OUTPUT_FIELD, IStreamItDebuggerConstants.CHANNEL_CLASS);
		
		// get queue
		var = findVariable(var, IStreamItDebuggerConstants.QUEUE_FIELD, IStreamItDebuggerConstants.LINKEDLIST_CLASS);

		// get pushed items
		IValue header = findVariable(var, IStreamItDebuggerConstants.HEADER_FIELD, IStreamItDebuggerConstants.LINKEDLISTENTRY_CLASS).getValue();
		Vector pushedItems = new Vector();
		getQueueItems(pushedItems, Integer.parseInt(pushCount), header, header.getVariables());

		// compile attributes of peeked items 
		Vector peekedItems = filter.getPeekItems();
		Iterator i = peekedItems.iterator();
		HashMap peekAttributes = new HashMap();
		HashMap attributes;
		while (i.hasNext()) {
			attributes = LaunchData.getAttributes((IVariable) i.next());
			if (attributes != null) peekAttributes.putAll(attributes);
		}

		// set attributes for pushed items corresponding to attributes of peek items
		i = pushedItems.iterator();
		while (i.hasNext()) {
			IVariable item = (IVariable) i.next();
			attributes = LaunchData.getAttributes(item);
			if (attributes == null) attributes = new HashMap();
			attributes.putAll(peekAttributes);
			LaunchData.addAttributes(item, attributes);
		}
	}
	
	private IVariable findVariable(IVariable var, String fieldName, String className) throws Exception {
		IVariable[] vars = var.getValue().getVariables();
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(fieldName) && vars[i].getReferenceTypeName().equals(className)) {
				return vars[i];
			}
		}
		return null;
	}
	
	private void handleOtherExit(ICompilationUnit unit, IStackFrame top) throws Exception {
		// TODO
		if (true) return;
		
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = LaunchData.getFilter(vars);
		if (filter == null) return;

		// update stage (remove phase)
		String stage = filter.getStage();
		filter.setStage(stage.substring(0, stage.indexOf('(')));
		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_STAGE));
	}
	
	private void handleInitEntry(IStackFrame top) throws DebugException {
		// get top-level pipeline
		IVariable[] vars = top.getVariables();
		if (vars.length < 1) return;
	
		DebugUIPlugin.getStandardDisplay().syncExec(initializeStream(vars[0]));
	}
	
	private Runnable initializeStream(final IVariable var) {
		return new Runnable() {	
			public void run() {
				StreamViewer v = getStreamViewer(getActivePage());
				if (v != null) v.setInput(var);
			}
		};
	}
	
	protected StreamViewer getStreamViewer(IWorkbenchPage page) {
		IViewPart viewPart = page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		if (viewPart == null) return null;
		return (StreamViewer) ((StreamView) viewPart).getViewer();
	}
	
	private Runnable updateStream(final String streamNameWithId) {
		return new Runnable() {	
			public void run() {
				StreamViewer v = getStreamViewer(getActivePage());
				if (v != null) v.setSelection(streamNameWithId);
			}
		};
	}
	
	private String getStreamName(IStackFrame top) throws DebugException {
		IVariable[] vars = top.getVariables();
		if (vars.length < 1) return "";
		else {
			IValue pipelineVal = vars[0].getValue();	
			return vars[0].getReferenceTypeName() + pipelineVal.getValueString();
		}
	}
	
	private void handleEntry(ICompilationUnit unit, IStackFrame top, int methodType, String methodName) throws Exception {
		// TODO test only 
		DebugUIPlugin.getStandardDisplay().syncExec(updateStream(getStreamName(top)));
		if (true) return;
		
		// get filter
		IVariable[] vars = top.getVariables();
		StrFilter filter = LaunchData.getFilter(vars);
		if (filter == null) return;
		
		// update stage by adding phase
		switch (methodType) {
			case IStreamItDebuggerConstants.WORK_BREAKPOINTS:
				filter.setStage(methodName);
				
				// for tagging:  save the vars in the peek range
				String peekCount = filter.getPeekCount();
				filter.workEntry();
				if (peekCount.equals(IStreamItDebuggerConstants.NOT_APPLICABLE)) break;

				// iterate through input channel variables to get queue, then header field
				IVariable queue = findVariable(filter.getInputVar(), IStreamItDebuggerConstants.QUEUE_FIELD, IStreamItDebuggerConstants.LINKEDLIST_CLASS);
				IValue header = findVariable(queue, IStreamItDebuggerConstants.HEADER_FIELD, IStreamItDebuggerConstants.LINKEDLISTENTRY_CLASS).getValue();
				getQueueItems(filter.getPeekItems(), Integer.parseInt(peekCount), header, header.getVariables());
				
				break;
			case IStreamItDebuggerConstants.PREWORK_BREAKPOINTS:
				filter.setStage(methodName);
				break;
			case IStreamItDebuggerConstants.OTHER_BREAKPOINTS:
				filter.setStage(filter.getStage() + methodName);
				break;
		}

		DebugUIPlugin.getStandardDisplay().syncExec(updateTable(filter, UPDATE_STAGE));
	}
	
	private void getQueueItems(Vector peekItems, int peekCount, IValue header, IVariable[] nextVars) throws Exception {
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
		if (!element.getValue().getValueString().equals(IStreamItDebuggerConstants.NULL_VALUE)) {
			elementVars = element.getValue().getVariables();
			for (int j = 0; j < elementVars.length; j++) {
				if (elementVars[j].getName().equals(IStreamItDebuggerConstants.VALUE_FIELD)) {
					peekItems.add(elementVars[j]);
					break;
				}
			}
		}
	
		// if next variable == header, you've come full circle
		if (next.getValue().equals(header)) return;
	
		// continue to the following next
		if (peekItems.size() == peekCount) return;
		getQueueItems(peekItems, peekCount, header, next.getValue().getVariables());				
	}
	
	private void terminate(DebugEvent event) throws CoreException {
		// TODO should I remove all breakpoints, watchpoints?
		
		// only handle if IDebugTarget
		Object o = event.getSource();
		if (!(o instanceof IDebugTarget)) return;

		// only handle if StreamIt Launch
		IDebugTarget target = (IDebugTarget) o;
		ILaunch launch = target.getLaunch();
		ILaunchConfiguration config = launch.getLaunchConfiguration(); 
		if (config.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return;

		// only handle if from .java
		String projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		String mainClassName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
		String javaFileName = mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION;
		String absoluteJavaFileName = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).getFile(javaFileName).getLocation().toOSString();
			
		// end launch, restore old breakpoints
		LaunchData launchData = getLaunchData(absoluteJavaFileName);
		if (launchData == null) return;
		launchData.clearBreakpoints();
		removeLaunchData(absoluteJavaFileName);
		
		// clean graph
		DebugUIPlugin.getStandardDisplay().syncExec(initializeStream(null));
	}
	
	private void addViewFilters() throws WorkbenchException {

		// open debug perspective and add self as listener
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		workbench.showPerspective(IDebugUIConstants.ID_DEBUG_PERSPECTIVE, activeWindow);
		IWorkbenchPage page = activeWindow.getActivePage();
		page.addPartListener(this);
		
		// fix Launch Manager View
		filterLaunchViewer(page.findView(IDebugUIConstants.ID_DEBUG_VIEW));

		// fix Variable View
		filterVariableViewer(page.findView(IDebugUIConstants.ID_VARIABLE_VIEW));
						
		// fix Graph View
		addToSelectionService(activeWindow, (StreamView) page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW));
	}

	private void filterLaunchViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		sViewer.addFilter(fLaunchViewerFilter);
		sViewer.setLabelProvider(fStreamItModelPresentation);
	}
	
	private void filterVariableViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		
		ViewerFilter[] vf = sViewer.getFilters();
		boolean add = true;
		for (int i = 0; i < vf.length; i++) if (vf[i] instanceof VariableViewerFilter) add = false;
		if (add) sViewer.addFilter(fVariableViewerFilter);
	}
	
	private static void addToSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.addSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partActivated(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partBroughtToTop(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partClosed(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partDeactivated(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partHidden(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partInputChanged(IWorkbenchPartReference ref) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partOpened(IWorkbenchPartReference ref) {
		String id = ref.getId();
		
		if (id.equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
			filterLaunchViewer((IViewPart) ref.getPart(false));
		} else if (id.equals(IDebugUIConstants.ID_VARIABLE_VIEW)) {
			filterVariableViewer((IViewPart) ref.getPart(false));
			/*
		} else if (id.equals(IStreamItDebuggerConstants.ID_GRAPHVIEW)) {
			
			GraphView view = (GraphView) ref.getPart(false);
			GraphViewer viewer = (GraphViewer) view.getViewer();
			
			IProcess p = DebugUITools.getCurrentProcess();
			if (p == null || p.isTerminated()) return;

			ILaunch l = p.getLaunch(); 
			if (l.getLaunchMode().equals(ILaunchManager.RUN_MODE)) return;

			ILaunchConfiguration lc = l.getLaunchConfiguration();
			if (lc.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return;

			try {
				// initialize input
				String mainClassName = lc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
				viewer.setInput(mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION);

				// register
				addToSelectionService(view.getSite().getWorkbenchWindow(), view);

				// get selection from debug view
				IWorkbenchPage page = view.getSite().getPage();
				IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
				StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer(); 
				ISelection selection = sViewer.getSelection();
				if (!selection.isEmpty()) viewer.setSelection(selection, false);			
				
			} catch (Exception e) {
			}
			
		} else if (id.equals(IStreamItDebuggerConstants.ID_GRAPHOVERVIEW)) {
			
			GraphOverview view = (GraphOverview) ref.getPart(false);
			GraphOverviewer viewer = view.getViewer();
			
			IProcess p = DebugUITools.getCurrentProcess();
			if (p == null || p.isTerminated()) return;

			ILaunch l = p.getLaunch(); 
			if (l.getLaunchMode().equals(ILaunchManager.RUN_MODE)) return;

			ILaunchConfiguration lc = l.getLaunchConfiguration();
			if (lc.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return;

			try {
				// initialize input
				String mainClassName = lc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
				viewer.setInput(mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION);

				// register
				addToSelectionService(view.getSite().getWorkbenchWindow(), view);

				// get selection from debug view
				IWorkbenchPage page = view.getSite().getPage();
				IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
				StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer(); 
				ISelection selection = sViewer.getSelection();
				if (!selection.isEmpty()) viewer.setSelection(selection, false);			
						
			} catch (Exception e) {
			}
			*/
		}		 
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partVisible(IWorkbenchPartReference ref) {
	}
}