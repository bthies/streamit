package streamit.eclipse.debugger.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IDebugEventFilter;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.graph.StreamViewer;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;
import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;
import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamItDebugEventFilter implements IDebugEventFilter, IBreakpointListener {
	
	private static StreamItDebugEventFilter fInstance = new StreamItDebugEventFilter();

	// key = IFile javaFile, entry = LaunchData
	private static HashMap fLaunchData = new HashMap();
	
	/**
	 * Creates a new StreamItDebugEventSetListener.
	 */
	private StreamItDebugEventFilter() {
		DebugPlugin.getDefault().addDebugEventFilter(this);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}
	
	/**
	 * Returns the singleton StreamItDebugEventSetListener.
	 */
	public static StreamItDebugEventFilter getInstance() {
		return fInstance;
	}
	
	public LaunchData beforeLaunch(String javaFileName, IFile javaFile) throws IOException, CoreException, WorkbenchException {
		
		LaunchData launchData = new LaunchData(javaFile);
		LaunchData oldData = getLaunchData(javaFile);
		if (oldData != null) {
			launchData.addStaticFilterInstanceBreakpoints(oldData.getStaticFilterInstanceBreakpoints());
		}
		fLaunchData.put(javaFile, launchData);

		// create view tools
		StreamItViewsManager.addViewFilters();

		// wipe graph
		StreamViewer v = StreamItViewsManager.getStreamViewer();
		if (v != null) {
			v.setInput(null);
		}
		
		return launchData;
	}
	
	public DebugEvent[] filterDebugEvents(DebugEvent[] events) {
		try {
			Vector filteredEvents = new Vector();
			int kind, detail;
			for (int i = 0; i < events.length; i++) {
				kind = events[i].getKind();
				switch(kind) {
					case DebugEvent.TERMINATE:
						if (events[i].getSource() instanceof IDebugTarget) {
							filteredEvents.add(events[i]);
							terminate(events[i]);
						}
						break;
					case DebugEvent.RESUME:
						detail = events[i].getDetail();
						if (detail == DebugEvent.STEP_INTO || detail == DebugEvent.STEP_OVER ||
							detail == DebugEvent.STEP_RETURN || detail == DebugEvent.CLIENT_REQUEST) {
								// all will be treated as step over
							if (!handleResume(events[i], detail == DebugEvent.CLIENT_REQUEST)) filteredEvents.add(events[i]);
						}
						break;
					case DebugEvent.SUSPEND:
						detail = events[i].getDetail();
						if (detail == DebugEvent.BREAKPOINT || detail == DebugEvent.STEP_END)
							if (!handleSuspend(events[i])) filteredEvents.add(events[i]);
						break;
					default:
						filteredEvents.add(events[i]);
						break;
				}
			}

			if (filteredEvents.size() == 0) return null;

			DebugEvent[] toReturn = new DebugEvent[filteredEvents.size()];
			filteredEvents.toArray(toReturn);
			return toReturn;
		} catch (CoreException e) {
		}
		return events;
	}
	
	
	private boolean handleResume(DebugEvent event, boolean resume) {
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return false;

		// only handle if StreamIt Launch
		IThread thread = (IThread) o;
		ILaunch launch = thread.getLaunch();
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(configuration)) return false;

		// get cause of launch
		try {
			// get javaFile
			IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(configuration);

			// already mapped, get line number
			LaunchData launchData = getLaunchData(javaFile);
			if (launchData == null || launchData.isTerminated()) return true;

			// client asked to resume => remove previous step breakpoint
			if (resume) {
				launchData.setClientStepRequest(false);
				return true;
			} 
			
			// client asked to step through code
			launchData.setClientStepRequest(true);
			
		} catch (CoreException e) {
		}
		return false;
	}
	
	// redirect from .java to .str
	private boolean handleSuspend(DebugEvent event) {
	
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return false;
		IThread thread = (IThread) o;
		try {
			ILaunchConfiguration config = thread.getLaunch().getLaunchConfiguration(); 
			if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(config)) return false;
			IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(config);
			
			// get cause of launch
			IStackFrame top = null;
			top = thread.getTopStackFrame();
			if (top == null) return false;
			
			// get corresponding launch info
			LaunchData launchData = getLaunchData(javaFile);
			if (launchData == null || launchData.isTerminated()) return true;

			IBreakpoint[] b = thread.getBreakpoints();
			for (int i = 0; i < b.length; i++) {
				// don't do .str breakpoints (unless .java breakpoint is also in here
				if (b[i].getMarker().getResource().getFileExtension().equals(IStreamItLaunchingConstants.STR_FILE_EXTENSION) &&
					b.length == 1) {
						thread.resume();
						return true;
				// handle init breakpoint
				} else if (launchData.isInitBreakpoint(b[i])) {
					handleInitEntryExit(top, launchData);
					thread.resume();
					return true;
				// if due to method breakpoint, only suspend if on correct filter instance or breakpoint created by user
				} else if (b[i] instanceof IJavaMethodBreakpoint) {
					IMarker m = ((IJavaMethodBreakpoint) b[i]).getMarker();
					IVariable[] vars = top.getVariables();
					if (vars.length == 1) {
						IValue val = vars[0].getValue();
						if (!b[i].getMarker().getAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, false)) {
							break;
						}

						String filterName = val.getReferenceTypeName();
						String filterRuntimeId = val.getValueString();
						if (launchData.hasFilterInstanceBreakpoint(filterName, filterRuntimeId, null)) {
							break;
						} else if (launchData.hasFilterInstanceBreakpoint(filterName, filterRuntimeId, launchData.getStaticId(filterName, filterRuntimeId))) {
							break;						
						} else {
							thread.resume();
							return true;
						}
					}
				}
			}

			// get current java, str line number
			int javaLineNumber = top.getLineNumber();
			StrJavaData strJavaData = StrJavaMapper.getInstance().getStrJavaData(javaFile);
			StreamItLineNumber streamItLineNumber = strJavaData.getStreamItLineNumber(javaLineNumber);

			// no valid mapping from java to str
			if (streamItLineNumber == null) {
				launchData.setPreviousLineNumber(-1);
				thread.resume();
				return true;
			}
			int strLineNumber = streamItLineNumber.getLineNumber();

			// check if this suspend is caused by a client request to step
			if (launchData.isClientStepRequest()) {
				// Because 1 .str lines maps to many .java lines,
				// don't suspend if the previous .str suspended line = current .str suspended line
				if (launchData.getPrevStrLineNumber() == strLineNumber) {
					// request another step
					thread.stepOver();
					return true;
				} else {
					// the step request has been answered
					launchData.setClientStepRequest(false);
				}
			}

			// highlight in .str
			IFile strFile = streamItLineNumber.getStrFile();
			DebugUIPlugin.getStandardDisplay().syncExec(getSelecter(strFile, strLineNumber - 1, top));
			launchData.setPreviousLineNumber(strLineNumber);

			// if this code is reached, then a valid suspend => update stream graph
			// get top-level pipeline
			DebugUIPlugin.getStandardDisplay().syncExec(updateStream(launchData));
			return false;
		} catch (CoreException e) {
		}
		return false;
	}
	
	private Runnable getSelecter(final IFile strFile, final int lineNumber, final IStackFrame top) {
		return new Runnable() {									
			public void run() {
				try {
					// open .str if not already open
					ITextEditor strEditor = (ITextEditor) StreamItViewsManager.getActivePage().openEditor(strFile, IStreamItEditorConstants.ID_STREAMIT_EDITOR).getAdapter(ITextEditor.class);											
					if (strEditor == null) return;
					
					// highlight text just in case search for marker fails
					IDocument doc = strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber);
					strEditor.selectAndReveal(strLine.getOffset(), strLine.getLength());
				} catch (CoreException e) {
				} catch (BadLocationException e) {
				}
			}
		};
	}

	private void handleInitEntryExit(IStackFrame top, LaunchData data) throws DebugException {
		// get top-level pipeline
		IVariable[] vars = top.getVariables();
		if (vars.length < 1) return;
	
		DebugUIPlugin.getStandardDisplay().syncExec(makeStream(vars[0], data));
	}
	
	private Runnable makeStream(final IVariable var, final LaunchData data) {
		return new Runnable() {
			public void run() {
				data.makeStreamItModel(var);
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) {
					v.setInput(data);
				}
			}
		};
	}
	
	private Runnable updateStream(final LaunchData data) {
		return new Runnable() {
			public void run() {
				data.updateStreamItModel();
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) {
					v.setInput(data);
				}
			}
		};
	}
	
	// termination
	private void terminate(DebugEvent event) throws CoreException {		
		// only handle if IDebugTarget
		Object o = event.getSource();
		if (!(o instanceof IDebugTarget)) return;

		// only handle if StreamIt Launch
		IDebugTarget target = (IDebugTarget) o;
		ILaunch launch = target.getLaunch();
		ILaunchConfiguration config = launch.getLaunchConfiguration(); 
		if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(config)) return;

		// end launch, restore old breakpoints
		IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(config);

		// remove any filter instance breakpoints
		//DebugUIPlugin.getStandardDisplay().syncExec(removeJavaMethodBreakpoints());

		LaunchData data = getLaunchData(javaFile);
		if (data == null || data.isTerminated()) return;

		// terminate launchData
		data.setTerminated(true);
		
		// make static graph
		DebugUIPlugin.getStandardDisplay().syncExec(makeStaticStreamGraph(data));
	}
	
	private Runnable makeStaticStreamGraph(final LaunchData data) {
		return new Runnable() {
			public void run() {
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) {
					v.setInput(data);
				}
			}
		};
	}
	
	public Runnable removeJavaMethodBreakpoints() {
		return new Runnable() {
			public void run() {
				IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
				IBreakpoint[] breakpoints = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
				for (int i = 0; i < breakpoints.length; i++) {
					if (!(breakpoints[i] instanceof IJavaMethodBreakpoint)) continue;
					IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) breakpoints[i];
					boolean remove = breakpoint.getMarker().getAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, false);
					if (!remove) continue;
					try {
						manager.removeBreakpoint(breakpoint, true);
					} catch (CoreException e) {
					}
				}
			}
		};
	}
	
	// launch removal
	public void removeLaunch(IFile javaFile) {
		LaunchData launchData = getLaunchData(javaFile);
		if (launchData == null) return;
		if (!launchData.isTerminated()) return;
		DebugUIPlugin.getStandardDisplay().syncExec(clearStreamGraph());
		DebugUIPlugin.getStandardDisplay().syncExec(removeJavaMethodBreakpoints());
		fLaunchData.remove(javaFile);
	}
	
	private Runnable clearStreamGraph() {
		return new Runnable() {
			public void run() {
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				if (v != null) {
					v.setInput(null);
				}
			}
		};
	}

	// Utility functions
	public IType getType(IEditorInput editorInput, IRegion line) throws JavaModelException, CoreException {
		if (editorInput instanceof IFileEditorInput) {
			IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
			manager.connect(editorInput);
			ICompilationUnit unit= manager.getWorkingCopy(editorInput);

			if (unit != null) {
				synchronized (unit) {
					unit.reconcile();
				}
				IJavaElement e = unit.getElementAt(line.getOffset());
				if (e instanceof IType) {
					return (IType)e;
				} else if (e instanceof IMember) {
					return ((IMember)e).getDeclaringType();
				}
			}
		}
		return null;
	}	

	// LaunchData access
	public LaunchData getLaunchData(IFile javaFile) {
		return (LaunchData) fLaunchData.get(javaFile);
	}
	
	// for AddRuntimeFilterBreakpointAction
	public void toggleFilterWorkMethodBreakpoint(LaunchData launchData, String filterName, String filterRuntimeId, String filterStaticId) throws CoreException {
		FilterWorkMethodInformation info = launchData.getFilterWorkMethodInformation(filterName);
		
		if (info.hasId(filterRuntimeId, filterStaticId, launchData.isTerminated())) {
			info.removeId(filterRuntimeId, filterStaticId, launchData.isTerminated());
			
			// remove breakpoint only if no ids left and if the method breakpoint was not user-added
			if (info.hasNoIds(launchData.isTerminated())) {
				IJavaMethodBreakpoint breakpoint = getMethodBreakpoint(info.getTypeName(), info.getMethodStart(), info.getMethodEnd());
				boolean remove = breakpoint.getMarker().getAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, false);
				if (!remove) return;
				IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
				manager.removeBreakpoint(breakpoint, true);
			}
		} else {
			// add breakpoint only if no ids and a method breakpoint doesn't already exist
			if (info.hasNoIds(launchData.isTerminated()) && getMethodBreakpoint(info.getTypeName(), info.getMethodStart(), info.getMethodEnd()) == null) {
				HashMap attributes = new HashMap(10);
				BreakpointUtils.addJavaBreakpointAttributes(attributes, info.getMethod());
				IJavaMethodBreakpoint b = JDIDebugModel.createMethodBreakpoint(launchData.getJavaFile(), info.getTypeName(), info.getMethodName(), info.getMethodSignature(), true, false, false, -1, info.getMethodStart(), info.getMethodEnd(), 0, true, attributes);
				b.getMarker().setAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, true);
			}
			
			info.addId(filterRuntimeId, filterStaticId, launchData.isTerminated());
		}
		
		StreamItViewsManager.getStreamViewer().setSelection(filterName + filterRuntimeId, false);
	}
	
	protected IJavaMethodBreakpoint getMethodBreakpoint(String typeName, int methodStart, int methodEnd) throws CoreException {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (!(breakpoints[i] instanceof IJavaMethodBreakpoint)) continue;
			IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) breakpoints[i];
			if (!(breakpoint.getMarker().getType().equals(IStreamItCoreConstants.ID_JAVA_METHOD_BREAKPOINT))) continue;
			if (breakpoint.getTypeName().equals(typeName) && breakpoint.getCharStart() == methodStart && breakpoint.getCharEnd() == methodEnd)
				return breakpoint;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (!(breakpoint instanceof IJavaMethodBreakpoint)) return;
		IMarker m = breakpoint.getMarker();
		if (m.getAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, false)) return;
		IResource r = m.getResource();
		if (!r.getFileExtension().equals(IStreamItCoreConstants.JAVA_FILE_EXTENSION)) return;
		String filterName = m.getAttribute(IStreamItCoreConstants.TYPE_NAME, IStreamItCoreConstants.EMPTY_STRING);
		if (filterName.equals(IStreamItCoreConstants.EMPTY_STRING)) return;

		// user is removing a breakpoint that is needed by streamit graph
		LaunchData launchData = getLaunchData((IFile) r);
		if (launchData == null || launchData.isTerminated()) return;
		FilterWorkMethodInformation info = launchData.getFilterWorkMethodInformation(filterName);
		if (info == null) return;
		HashMap attributes = new HashMap(10);
		BreakpointUtils.addJavaBreakpointAttributes(attributes, info.getMethod());
		try {
			IJavaMethodBreakpoint b = JDIDebugModel.createMethodBreakpoint(launchData.getJavaFile(), info.getTypeName(), info.getMethodName(), info.getMethodSignature(), true, false, false, -1, info.getMethodStart(), info.getMethodEnd(), 0, true, attributes);
			b.getMarker().setAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, true);
		} catch (CoreException e) {
		}
	}
}