package streamit.eclipse.debugger.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.core.LaunchData;
import streamit.eclipse.debugger.core.LogFileManager;
import streamit.eclipse.debugger.core.StrJavaData;
import streamit.eclipse.debugger.core.StrJavaMapper;
import streamit.eclipse.debugger.core.StreamItDebugEventFilter;
import streamit.eclipse.debugger.core.StreamItLineNumber;
import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.graph.StreamOverview;
import streamit.eclipse.debugger.graph.StreamOverviewer;
import streamit.eclipse.debugger.graph.StreamView;
import streamit.eclipse.debugger.graph.StreamViewer;
import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.launching.StreamItApplicationLaunchShortcut;
import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;
import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;

/**
 * @author kkuo
 */
public class StreamItViewsManager implements IPartListener2, ISelectionListener {

	private static StreamItViewsManager fInstance = new StreamItViewsManager();
	private static DebugViewerFilter fDebugViewerFilter = new DebugViewerFilter();
	private static StreamItModelPresentation fStreamItModelPresentation = new StreamItModelPresentation();
	private static VariablesViewerFilter fVariablesViewerFilter = new VariablesViewerFilter();
	private static StreamItApplicationLaunchShortcut fLaunchShortcut = new StreamItApplicationLaunchShortcut();
	
	private StreamItViewsManager() {
		super();
	}
	
	public static StreamItViewsManager getInstance() {
		return fInstance;
	}
	
	public static void addViewFilters() throws WorkbenchException {

		// open debug perspective and add self as listener
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		workbench.showPerspective(IDebugUIConstants.ID_DEBUG_PERSPECTIVE, activeWindow);
		IWorkbenchPage page = activeWindow.getActivePage();
		page.addPartListener(StreamItViewsManager.getInstance());
		
		// fix Debug View
		filterDebugViewer(page.findView(IDebugUIConstants.ID_DEBUG_VIEW));

		// fix Variables View
		filterVariablesViewer(page.findView(IDebugUIConstants.ID_VARIABLE_VIEW));
						
		// fix Graph View
		addToSelectionService(activeWindow, (StreamView) page.findView(IStreamItGraphConstants.ID_STREAMVIEW));

		// for line highlighting
		activeWindow.getSelectionService().addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, getInstance());
	}

	private static void filterDebugViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		sViewer.addFilter(fDebugViewerFilter);
		sViewer.setLabelProvider(fStreamItModelPresentation);
	}
	
	private static void filterVariablesViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		
		ViewerFilter[] vf = sViewer.getFilters();
		boolean add = true;
		for (int i = 0; i < vf.length; i++) if (vf[i] instanceof VariablesViewerFilter) add = false;
		if (add) sViewer.addFilter(fVariablesViewerFilter);
	}
	
	public static void addToSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.addSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partOpened(IWorkbenchPartReference ref) {
		String id = ref.getId();
		
		if (id.equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
			filterDebugViewer((IViewPart) ref.getPart(false));
		} else if (id.equals(IDebugUIConstants.ID_VARIABLE_VIEW)) {
			filterVariablesViewer((IViewPart) ref.getPart(false));
		} else if (id.equals(IStreamItGraphConstants.ID_STREAMVIEW)) {
			
			StreamView view = (StreamView) ref.getPart(false);
			StreamViewer viewer = (StreamViewer) view.getViewer();
			IWorkbenchPage page = view.getSite().getPage();
			IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
			StructuredViewer debugViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer(); 
			ISelection selection = debugViewer.getSelection();
			if (selection.isEmpty()) return;
			if (!(selection instanceof StructuredSelection)) return;
			Object element = ((StructuredSelection) selection).getFirstElement();			

			try {
				// show corresponding stream graph
				ILaunch launch;
				if (element instanceof ILaunch) launch = (ILaunch) element;
				else if (element instanceof IProcess) launch = ((IProcess) element).getLaunch();
				else if (element instanceof IDebugTarget) launch = ((IDebugTarget) element).getLaunch();
				else if (element instanceof IStackFrame) launch = ((IStackFrame) element).getLaunch();
				else if (element instanceof IThread) launch = ((IThread) element).getLaunch();
				else return;
				if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(launch)) return;
				
				if (launch.getAttribute(IStreamItLaunchingConstants.ATTR_KILL_LAUNCH) != null) {
					launch.terminate();
					return;
				}
				
				IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(launch.getLaunchConfiguration());
				if (javaFile == null) return;
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				LaunchData launchData = StreamItDebugEventFilter.getInstance().getLaunchData(javaFile);
				if (launchData != null && v != null) v.setInput(launchData);
			} catch (DebugException e) {
			} catch (CoreException e) {
			}
					
			// register
			addToSelectionService(view.getSite().getWorkbenchWindow(), view);

			// get selection from debug view
			if (!selection.isEmpty()) viewer.setSelection(selection, false);

			return;
		} else if (id.equals(IStreamItEditorConstants.ID_STREAMIT_EDITOR)) {
			IFile strFile = ((IFileEditorInput) ((ITextEditor) ref.getPart(false)).getEditorInput()).getFile();
			LogFileManager.getInstance().setLogFile(strFile);
			TestSwingEditorPlugin.getInstance().launchGraph(strFile);
		}
	}

	private void close(final IFile strFile, final ITextEditor editor) {	
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = StreamItViewsManager.getActivePage();
				page.closeEditor(editor, false);
				try {
					page.openEditor(strFile, "org.eclipse.ui.DefaultTextEditor");
				} catch (PartInitException e) {
				}
			}
		});
	}

	/*
	public static Object[] filter(Object root, Object[] elements, ViewerFilter[] filters, StructuredViewer viewer) {
		if (filters != null) {
			ArrayList filtered = new ArrayList(elements.length);
			for (int i = 0; i < elements.length; i++) {
				boolean add = true;
				for (int j = 0; j < filters.length; j++) {
					add = ((ViewerFilter) filters[j]).select(viewer, root, elements[i]);
					if (!add)
						break;
				}
				if (add)
					filtered.add(elements[i]);
			}
			return filtered.toArray();
		}
		return elements;
	}

	public static void updateDebugVariablesViews(String streamName, String id, IWorkbenchPage page) { 
		// update debug view => will automatically update variables view
		// if no debug view, then no input in variables view
		IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
		if (viewPart == null) return;
		StructuredViewer debugViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		ISelection selection;
		
		Object root = debugViewer.getInput();
		LaunchManager lm = (LaunchManager) root;
		ViewerFilter[] filters = debugViewer.getFilters();
		if (lm == null) return;
		ILaunch[] launches = lm.getLaunches();
		for (int i = 0; i < launches.length; i++) {
			if (launches[i].isTerminated() ||
				!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(launches[i])) continue;
			try {
				IThread[] threads = launches[i].getDebugTarget().getThreads();
				for (int j = 0; j < threads.length; j++) {
					Object[] frames = StreamItViewsManager.filter(root, threads[j].getStackFrames(), filters, debugViewer);
					for (int k = 0; k < frames.length; k++) {
						IVariable[] vars = ((IStackFrame) frames[k]).getVariables();
						IValue val;
						for (int x = 0; x < vars.length; x++) {
							val = vars[x].getValue();
							if (!streamName.equals(val.getReferenceTypeName()) || !id.equals(val.getValueString())) continue;
							debugViewer.setSelection(new StructuredSelection(frames[k]));
							return; 
						}
					}
				}
			} catch (CoreException e) {
			}
		}
	}
*/	
	public static IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow(){
		return getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}
	
	public static StreamViewer getStreamViewer() {
		return getStreamViewer(getActivePage());
	}

	public static StreamView getStreamView() {
		return (StreamView) getActivePage().findView(IStreamItGraphConstants.ID_STREAMVIEW);
	}
	
	public static StreamView getStreamView(IWorkbenchPage page) {
		return (StreamView) page.findView(IStreamItGraphConstants.ID_STREAMVIEW);
	}

	public static StreamViewer getStreamViewer(IWorkbenchPage page) {
		IViewPart viewPart = getStreamView(page);
		if (viewPart == null) return null;
		return (StreamViewer) ((StreamView) viewPart).getViewer();
	}
	
	public static StreamOverviewer getStreamOverviewer() {
		return getStreamOverviewer(getActivePage());
	}

	public static StreamOverviewer getStreamOverviewer(IWorkbenchPage page) {
		IViewPart viewPart = page.findView(IStreamItGraphConstants.ID_STREAMOVERVIEW);
		if (viewPart == null) return null;
		return (StreamOverviewer) ((StreamOverview) viewPart).getViewer();
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
	 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partVisible(IWorkbenchPartReference ref) {		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
	 */
	public void partInputChanged(IWorkbenchPartReference ref) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part.getSite().getId().equals(IDebugUIConstants.ID_DEBUG_VIEW)) {
			if (selection.isEmpty()) return;
			if (!(selection instanceof StructuredSelection)) return;
			Object element = ((StructuredSelection) selection).getFirstElement();			
			
			try {
				// show corresponding stream graph
				ILaunch launch;
				if (element instanceof ILaunch) launch = (ILaunch) element;
				else if (element instanceof IProcess) launch = ((IProcess) element).getLaunch();
				else if (element instanceof IDebugTarget) launch = ((IDebugTarget) element).getLaunch();
				else if (element instanceof IStackFrame) launch = ((IStackFrame) element).getLaunch();
				else if (element instanceof IThread) launch = ((IThread) element).getLaunch();
				else return;
				if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(launch)) return;

				if (launch.getAttribute(IStreamItLaunchingConstants.ATTR_KILL_LAUNCH) != null) {
					launch.terminate();
					return;
				}

				IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(launch.getLaunchConfiguration());
				if (javaFile == null) return;
				StreamViewer v = StreamItViewsManager.getStreamViewer();
				LaunchData launchData = StreamItDebugEventFilter.getInstance().getLaunchData(javaFile);
				if (launchData != null && v != null) {
					Object o = v.getInput();
					if (o != null && !((LaunchData) o).equals(launchData))
						v.setInput(launchData);
				}

				// show corresponding text in .str
				if (!(element instanceof IStackFrame)) return;
				IStackFrame frame = (IStackFrame) element;
			
				int lineNumber = frame.getLineNumber();
				if (lineNumber < 0) return;

				// map java line number to str line number
				StrJavaData data = StrJavaMapper.getInstance().getStrJavaData(javaFile);
				StreamItLineNumber line = data.getStreamItLineNumber(lineNumber);
				if (line != null) {
					lineNumber = line.getLineNumber();
					IFile strFile = line.getStrFile();
					ITextEditor strEditor = (ITextEditor) part.getSite().getPage().openEditor(strFile).getAdapter(ITextEditor.class);
					
					IDocument doc = strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber - 1);
					strEditor.selectAndReveal(strLine.getOffset(), strLine.getLength());					
				}
			} catch (CoreException ce) {
			} catch (BadLocationException pie) {
			}
		}
	}
	
	// TODO terrible hack to fix menu disabling
	public void addMenuListener(ITextEditor texteditor) {
		BasicTextEditorActionContributor c = (BasicTextEditorActionContributor) texteditor.getEditorSite().getActionBarContributor();
		c.init(c.getActionBars());
	}
}