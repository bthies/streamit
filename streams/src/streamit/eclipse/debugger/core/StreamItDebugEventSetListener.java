package streamit.eclipse.debugger.core;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.graph.StreamViewer;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;

public class StreamItDebugEventSetListener implements IDebugEventSetListener {
	
	private static StreamItDebugEventSetListener fInstance = new StreamItDebugEventSetListener();

	// key = String absoluteJavaFileName, entry = LaunchData
	private static HashMap fLaunchData = new HashMap();
	
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
				top.resume();
				return;
			}
		
			// highlight in .str
			DebugUIPlugin.getStandardDisplay().syncExec(getSelecter(javaFile.getProject().getFile(getStrFileName(javaFile)), lineNumber - 1, top));
	 		boolean resume = false;
			IBreakpoint[] b = thread.getBreakpoints();
			for (int i = 0; i < b.length; i++) {
				if (launchData.isInitBreakpoint(b[i])) {
					handleInitEntry(top);
					resume = true;
				}			
			}
			if (b.length == 0) resume = true;
			if (resume) top.resume();
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
										
					// open .str if not already open					
					ITextEditor strEditor = (ITextEditor) StreamItViewsManager.getActivePage().openEditor(strFile).getAdapter(ITextEditor.class);											
					if (strEditor == null) return;
					
					// highlight text just in case search for marker fails
					IDocument doc = strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber);
					strEditor.selectAndReveal(strLine.getOffset(), strLine.getLength());
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

	public static void setInitBreakpoint(String absoluteJavaFileName, IJavaMethodBreakpoint breakpoint) throws Exception {
		getLaunchData(absoluteJavaFileName).setInitBreakpoint(breakpoint);
	}

	public LaunchData beforeLaunch(String absoluteJavaFileName, String javaFileName, IFile javaFile) throws Exception {
		LaunchData launchData = new LaunchData();
		fLaunchData.put(absoluteJavaFileName, launchData);

		// create all .java to .str file mappings
		launchData.mapJavaToStr(javaFile);
		
		// create view tools
		StreamItViewsManager.addViewFilters();
		
		// wipe graph
		StreamViewer v = StreamItViewsManager.getStreamViewer();
		if (v != null) v.setInput(null); 
		
		return launchData;
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

	private IVariable findVariable(IVariable var, String fieldName, String className) throws Exception {
		IVariable[] vars = var.getValue().getVariables();
		for (int i = 0; i < vars.length; i++) {
			if (vars[i].getName().equals(fieldName) && vars[i].getReferenceTypeName().equals(className)) {
				return vars[i];
			}
		}
		return null;
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
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) v.setInput(var);
			}
		};
	}
	
	private Runnable updateStream(final String streamNameWithId) {
		return new Runnable() {	
			public void run() {
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) v.setSelection(streamNameWithId, true);
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
		removeLaunchData(absoluteJavaFileName);
		
		// clean graph
		DebugUIPlugin.getStandardDisplay().syncExec(initializeStream(null));
	}
}