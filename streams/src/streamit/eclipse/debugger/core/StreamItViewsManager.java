package streamit.eclipse.debugger.core;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.graph.StreamView;
import streamit.eclipse.debugger.graph.StreamViewer;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.ui.LaunchViewerFilter;
import streamit.eclipse.debugger.ui.StreamItModelPresentation;
import streamit.eclipse.debugger.ui.VariableViewerFilter;

/**
 * @author kkuo
 */
public class StreamItViewsManager implements IPartListener2, ISelectionListener {

	private static StreamItViewsManager fInstance = new StreamItViewsManager();
	private static LaunchViewerFilter fLaunchViewerFilter = new LaunchViewerFilter();
	private static StreamItModelPresentation fStreamItModelPresentation = new StreamItModelPresentation();
	private static VariableViewerFilter fVariableViewerFilter = new VariableViewerFilter();

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
		
		// fix Launch Manager View
		filterLaunchViewer(page.findView(IDebugUIConstants.ID_DEBUG_VIEW));

		// fix Variable View
		filterVariableViewer(page.findView(IDebugUIConstants.ID_VARIABLE_VIEW));
						
		// fix Graph View
		addToSelectionService(activeWindow, (StreamView) page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW));

		// for line highlighting		
		activeWindow.getSelectionService().addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, getInstance());
	}

	private static void filterLaunchViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		sViewer.addFilter(fLaunchViewerFilter);
		sViewer.setLabelProvider(fStreamItModelPresentation);
	}
	
	private static void filterVariableViewer(IViewPart viewPart) {
		if (viewPart == null) return;
		StructuredViewer sViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		
		ViewerFilter[] vf = sViewer.getFilters();
		boolean add = true;
		for (int i = 0; i < vf.length; i++) if (vf[i] instanceof VariableViewerFilter) add = false;
		if (add) sViewer.addFilter(fVariableViewerFilter);
	}
	
	public static void addToSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.addSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
	}

	public static void removeFromSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.removeSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.removeSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
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
		} else if (id.equals(IStreamItDebuggerConstants.ID_STREAMVIEW)) {
			
			StreamView view = (StreamView) ref.getPart(false);
			StreamViewer viewer = (StreamViewer) view.getViewer();
			
			IProcess p = DebugUITools.getCurrentProcess();
			if (p == null || p.isTerminated()) return;

			ILaunch l = p.getLaunch(); 
			if (l.getLaunchMode().equals(ILaunchManager.RUN_MODE)) return;

			ILaunchConfiguration lc = l.getLaunchConfiguration();
			if (lc.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) return;

			try {
				IWorkbenchPage page = view.getSite().getPage();
				IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
				StructuredViewer debugViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer(); 
				
				Object root = debugViewer.getInput();
				LaunchManager lm = (LaunchManager) root;
				ViewerFilter[] filters = debugViewer.getFilters();
				if (lm == null) return;
				ILaunch[] launches = lm.getLaunches();
				IStackFrame frame;
				for (int i = 0; i < launches.length; i++) {
					if (launches[i].isTerminated() ||
						launches[i].getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) continue;
						IThread[] threads = launches[i].getDebugTarget().getThreads();
					for (int j = 0; j < threads.length; j++) {
						Object[] frames = filter(root, threads[j].getStackFrames(), filters, debugViewer);
						for (int k = 0; k < frames.length; k++) {
							frame = (IStackFrame) frames[k];
							if (!frame.getName().equals("main")) continue;
							IVariable[] vars = frame.getVariables();
							for (int m = 0; m < vars.length; m++) {
								if (!vars[m].getName().equals("program")) continue;
								
								// initialize input
								viewer.setInput(vars[m]);

								// register
								addToSelectionService(view.getSite().getWorkbenchWindow(), view);

								// get selection from debug view
								ISelection selection = debugViewer.getSelection();
								if (!selection.isEmpty()) viewer.setSelection(selection, false);

								return;
							}
						}
					}
				}
			} catch (Exception e) {
			}
		}
	}
	
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

	public static void updateLaunchVariableViews(String streamName, String id, IWorkbenchPage page) { 
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
				launches[i].getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) == -1) continue;
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
			} catch (Exception e) {
			}
		}
	}
	
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

	public static StreamViewer getStreamViewer(IWorkbenchPage page) {
		IViewPart viewPart = page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		if (viewPart == null) return null;
		return (StreamViewer) ((StreamView) viewPart).getViewer();
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
		String id = ref.getId();
		if (id.equals(JavaUI.ID_CU_EDITOR)) {
			// also close str
			IWorkbenchPage page = ref.getPage();
			IEditorPart strEditor = findStreamItEditor(page, ref);
			if (strEditor != null) getActivePage().closeEditor(strEditor, true);
		} else if (id.equals(JavaUI.ID_CU_EDITOR)) {
			// also close java
			IWorkbenchPage page = ref.getPage();
			IEditorPart javaEditor = findJavaEditor(page, ref);
			if (javaEditor != null) getActivePage().closeEditor(javaEditor, true);
			
		}
	}
	
	private IEditorPart findJavaEditor(IWorkbenchPage page, IWorkbenchPartReference ref) {
		IEditorInput input = ((IEditorPart) ref.getPart(false)).getEditorInput();
		if (!(input instanceof IFileEditorInput)) return null;

		IFile strFile = ((IFileEditorInput) input).getFile();
		String strFileName = input.getName();
		String javaFileName = strFileName.substring(0, strFileName.indexOf(".str")) + ".java";
		IFile javaFile = strFile.getProject().getFile(javaFileName);
		return page.findEditor(new FileEditorInput(javaFile));
	}
	
	private IEditorPart findStreamItEditor(IWorkbenchPage page, IWorkbenchPartReference ref) {
		IEditorInput input = ((IEditorPart) ref.getPart(false)).getEditorInput();
		if (!(input instanceof IFileEditorInput)) return null;

		IFile javaFile = ((IFileEditorInput) input).getFile();
		String strFileName = StreamItDebugEventSetListener.getStrFileName(javaFile);
		IFile strFile = javaFile.getProject().getFile(strFileName);
		return page.findEditor(new FileEditorInput(strFile));
	}
	
	public static void setCollapseAll(IViewPart part, boolean enable) {
		IContributionItem[] items = ((IViewSite) part.getSite()).getActionBars().getToolBarManager().getItems();
		IContributionItem item;
		String id;
		for (int i = 0; i < items.length; i++) {
			item = items[i];
			id = item.getId();
			if (id.equals("streamit.eclipse.debugger.graph.collapseall")) {
				((ActionContributionItem) item).getAction().setEnabled(enable);
				return;
			}
		}
	}
	
	public static void setCollapseAll(boolean enable) {
		IViewPart part = getActivePage().findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		if (part == null) return;
		StreamItViewsManager.setCollapseAll(part, enable);
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
		if (!ref.getId().equals(JavaUI.ID_CU_EDITOR)) return;

		IWorkbenchPage page = ref.getPage();
		IEditorPart strEditor = findStreamItEditor(page, ref);
		if (strEditor != null) getActivePage().bringToTop(strEditor);
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
			if (!(element instanceof IStackFrame)) return;
			
			IStackFrame frame = (IStackFrame) element;
			try {
				int lineNumber = frame.getLineNumber();
				if (lineNumber < 0) return;

				// get cause of launch
				ISourceLocator locator = frame.getLaunch().getSourceLocator();
				Object o = locator.getSourceElement(frame);

				// only handle if from .java
				if (!(o instanceof ICompilationUnit)) return;
				ICompilationUnit unit = (ICompilationUnit) o;
				IFile javaFile = (IFile) unit.getResource();

				// map java line number to str line number
				lineNumber = StreamItDebugEventSetListener.getLineNumber(javaFile, lineNumber);
				if (lineNumber > 0) {
					String strFileName = StreamItDebugEventSetListener.getStrFileName(javaFile);
					IFile strFile = javaFile.getProject().getFile(strFileName);
					ITextEditor strEditor = (ITextEditor) part.getSite().getPage().openEditor(strFile).getAdapter(ITextEditor.class);
					
					IDocument doc = strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber - 1);
					strEditor.selectAndReveal(strLine.getOffset(), strLine.getLength());
				}
			} catch (DebugException de) {
			} catch (PartInitException pie) {
			} catch (BadLocationException pie) {
			}
		}
	}
}