package streamit.eclipse.debugger.actions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.ExceptionHandler;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifier;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.launching.StreamItApplicationLaunchShortcut;
import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;
import streamit.eclipse.debugger.texteditor.StreamItEditor;

/**
 * @author kkuo
 */
public class ManageBreakpointActionDelegate implements IWorkbenchWindowActionDelegate, IPartListener {

	protected boolean fInitialized= false;
	private IAction fAction= null;
	private int fLineNumber;
	private int fJavaLineNumber;
	private IType fType= null;
	private ITextEditor fTextEditor= null;
	private IWorkbenchWindow fWorkbenchWindow= null;

	protected String fStrFileName;
	protected String fJavaFileName;
	protected String fStrName;
	protected IJavaElement fJavaFile;
	protected IEditorPart fJavaEditorPart;
	protected HashMap fStrToJava;
	protected HashMap fJavaToStr;	
	protected static StreamItApplicationLaunchShortcut launchShortcut = new StreamItApplicationLaunchShortcut();
	protected static StreamItLocalApplicationLaunchConfigurationDelegate launchDelegate = new StreamItLocalApplicationLaunchConfigurationDelegate();

	public ManageBreakpointActionDelegate() {
	}
	
	/**
	 * Manages a breakpoint.
	 */
	protected void manageBreakpoint(IEditorInput editorInput) {
		ISelectionProvider sp= getTextEditor().getSelectionProvider();
		if (sp == null) {
			report(ActionMessages.getString("ManageBreakpointActionDelegate.No_Breakpoint")); //$NON-NLS-1$
			return;
		}
		report(null);
		ISelection selection= sp.getSelection();
		if (selection instanceof ITextSelection) {
			IDocument document= getJavaDocument();
			int rulerLine= ((ITextSelection)selection).getStartLine() + 1;
			int javaRulerLine;

			Object isThere = fStrToJava.get(new Integer(rulerLine));
			if (isThere == null) return;
			else javaRulerLine = ((Integer) ((Set) isThere).iterator().next()).intValue() - 1;

			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int lineNumber = bv.getValidBreakpointLocation(document, javaRulerLine);

			// trace back to .str
			rulerLine = ((Integer) fJavaToStr.get(new Integer(lineNumber))).intValue();
			if (lineNumber > 0) {
				try {
					IRegion line= document.getLineInformation(lineNumber - 1);

					IType type = setType(getJavaEditorPart().getEditorInput(), line);
					
					if (type != null) {
						IJavaProject project= type.getJavaProject();
						if (type.exists() && project != null && project.isOnClasspath(type)) {
							IJavaLineBreakpoint breakpoint= JDIDebugModel.lineBreakpointExists(type.getFullyQualifiedName(), lineNumber + 1);
							if (breakpoint != null) {
								DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(breakpoint, true);
								removeMarkers(getMarkers(rulerLine));
								
							} else {
								Map attributes = new HashMap(10);
								int start= line.getOffset();
								int end= start + line.getLength() - 1;
								BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
								JDIDebugModel.createLineBreakpoint(BreakpointUtils.getBreakpointResource(type), type.getFullyQualifiedName(), lineNumber, -1, -1, 0, true, attributes);
								IMarker m = JDIDebugModel.createLineBreakpoint(getJavaResource(), type.getFullyQualifiedName(), lineNumber, -1, -1, 0, true, attributes).getMarker();

								// in .java, leave highlighted area on breakpoint just added
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(m, false);
							
								// Add breakpoint in .str
								IRegion strLine= getDocument().getLineInformation(rulerLine - 1);
								start = strLine.getOffset();
								end = start + strLine.getLength() - 1;
								BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
								JDIDebugModel.createLineBreakpoint(getResource(), type.getFullyQualifiedName(), rulerLine, -1, -1, 0, true, attributes);
							}
							JavaPlugin.getActivePage().bringToTop(getTextEditor());
						}
					}
				} catch (CoreException ce) {
					ExceptionHandler.handle(ce, ActionMessages.getString("ManageBreakpointActionDelegate.error.title1"), ActionMessages.getString("ManageBreakpointActionDelegate.error.message1")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
				}				
			}
		}
	}
	
	protected void removeMarkers(List markers) {
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		// remove breakpoints from .str
		try {
			Iterator e= markers.iterator();
			while (e.hasNext()) {
				IBreakpoint breakpoint= breakpointManager.getBreakpoint((IMarker) e.next());
				breakpointManager.removeBreakpoint(breakpoint, true);
			}
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.removing.message1"), e); //$NON-NLS-1$
		}
	}

	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (getTextEditor() != null) {
			manageBreakpoint(getTextEditor().getEditorInput());
		}
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (!fInitialized) {
			initialize(action);
		} 
	}

	protected void initialize(IAction action) {
		setAction(action);
		if (getWorkbenchWindow() != null) {
			IWorkbenchPage page= getWorkbenchWindow().getActivePage();
			if (page != null) {
				IEditorPart part= page.getActiveEditor();
				if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
			}
		}
		fInitialized= true;
	}
	
	protected IAction getAction() {
		return fAction;
	}

	protected void setAction(IAction action) {
		fAction = action;
	}
	
	/**
	 * @see IPartListener#partActivated(IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
	}

	/**
	 * @see IPartListener#partBroughtToTop(IWorkbenchPart)
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partClosed(IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		if (part == getTextEditor()) {
			setTextEditor(null);
			if (getAction() != null) {
				getAction().setEnabled(false);
			}
		}
	}

	/**
	 * @see IPartListener#partDeactivated(IWorkbenchPart)
	 */
	public void partDeactivated(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partOpened(IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
	}
	
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}

	protected void setTextEditor(ITextEditor editor) {
		fTextEditor = editor;
		setEnabledState(editor);
			
		if (fTextEditor == null) return;		
		fStrFileName = getResource().getName();
		fStrName = fStrFileName.substring(0, fStrFileName.lastIndexOf('.' + IStreamItLaunchingConstants.STR_FILE_EXTENSION));
		fStrFileName = getResource().getLocation().toOSString();
		fJavaFileName = fStrName + ".java";
		fJavaFile = null;
		fJavaEditorPart = null;
		fStrToJava = null;
		fJavaToStr = null;
	}

	protected void setEnabledState(ITextEditor editor) {
		if (getAction() != null) {
			getAction().setEnabled(editor != null && editor.getSite().getId().equals(IStreamItActionConstants.ID_STREAMIT_EDITOR));
		} 
	}
	
	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		setWorkbenchWindow(window);
		window.getPartService().addPartListener(this);
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		getWorkbenchWindow().getPartService().removePartListener(this);
	}

	protected IWorkbenchWindow getWorkbenchWindow() {
		return fWorkbenchWindow;
	}

	protected void setWorkbenchWindow(IWorkbenchWindow workbenchWindow) {
		fWorkbenchWindow = workbenchWindow;
	}

	protected void report(String message) {
		if (getTextEditor() != null) {
			IEditorStatusLine statusLine= (IEditorStatusLine) getTextEditor().getAdapter(IEditorStatusLine.class);
			if (statusLine != null) {
				if (message != null) {
					statusLine.setMessage(true, message, null);
				} else {
					statusLine.setMessage(true, null, null);
				}
			}
		}		
		if (message != null && JDIDebugUIPlugin.getActiveWorkbenchShell() != null) {
			JDIDebugUIPlugin.getActiveWorkbenchShell().getDisplay().beep();
		}
	}
	
	protected IEditorPart getJavaEditorPart() {
		if (fJavaEditorPart == null) openJavaFile();
		return fJavaEditorPart;
	}
	
	protected IJavaElement getJavaFile() {
		if (fJavaFile == null) openJavaFile();
		return fJavaFile;
	}

	protected void openJavaFile() {
		IProject project = getResource().getProject();
		IFile javaFile = project.getFile(fJavaFileName);
		try {
			if (!javaFile.exists() && project.hasNature(JavaCore.NATURE_ID)) {
				ILaunchConfiguration configuration = launchShortcut.findLaunchConfiguration(project.getName(), fStrName, ILaunchManager.RUN_MODE);
				launchDelegate.launchJava(configuration);

				// handle a secondary file
				String mainClassName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
				if (!mainClassName.equals(fStrName)) fJavaFileName = mainClassName + ".java";
				javaFile = project.getFile(fJavaFileName);
			}
			fJavaFile = JavaCore.create(javaFile);
			fJavaEditorPart = JavaUI.openInEditor(fJavaFile);
			mapStrToJava(javaFile);
			
			// open graph editor if not already open
			
		} catch (Exception e) {
		}
	}
	
	protected void mapStrToJava(IFile javaFile) throws Exception {
		fStrToJava = new HashMap();
		fJavaToStr = new HashMap();
		
		// look for commented mappings
		InputStream is = javaFile.getContents();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String strLine = null;
		int javaLineNumber = 0;
		String token = "// " + fStrFileName + ':';
		
		while (true) {
			javaLineNumber++;
			strLine = br.readLine();
			if (strLine == null) break;
			if (strLine.indexOf(token) != -1) {
				Integer strLineNumber = Integer.valueOf(strLine.substring(strLine.indexOf(token) + token.length()));
				Object isThere = fStrToJava.get(strLineNumber);			
				if (isThere == null) {
					Set javaLNs = new TreeSet();
					javaLNs.add(new Integer(javaLineNumber));
					fStrToJava.put(strLineNumber, javaLNs);
				} else {
					((Set) isThere).add(new Integer(javaLineNumber));				
				}
				fJavaToStr.put(new Integer(javaLineNumber), strLineNumber);
			}		
		}
		is.close();
	}
	
	/** 
	 * Returns the resource for which to create the marker, 
	 * or <code>null</code> if there is no applicable resource.
	 *
	 * @return the resource for which to create the marker or <code>null</code>
	 */
	protected IResource getResource() {
		IEditorInput input= getTextEditor().getEditorInput();
		
		IResource resource= (IResource) input.getAdapter(IFile.class);
		
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}
			
		return resource;
	}
	
	protected IResource getJavaResource() {
		return getJavaFile().getResource();
	}
	
	protected IDocument getJavaDocument() {
		return JavaUI.getDocumentProvider().getDocument(getJavaEditorPart().getEditorInput());
	}
	
	protected IType setType(IEditorInput editorInput, IRegion line) throws JavaModelException {
		if (editorInput instanceof IFileEditorInput) {
			IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
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
	
	/**
	 * Returns the <code>IDocument</code> of the editor's input.
	 *
	 * @return the document of the editor's input
	 */
	protected IDocument getDocument() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		return provider.getDocument(fTextEditor.getEditorInput());
	}
	
	protected List getMarkers(int lineNumber) {

		List breakpoints= new ArrayList();
			
		IResource resource = getResource();
		IDocument document = getDocument();
		AbstractMarkerAnnotationModel model = getAnnotationModel();
			
		if (model != null) {
			try {
					
				IMarker[] markers= getAllMarkers(resource);
					
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						IBreakpoint breakpoint= breakpointManager.getBreakpoint(markers[i]);
						if (breakpoint != null && breakpointManager.isRegistered(breakpoint)){
							try {
								int markerLine= document.getLineOfOffset(model.getMarkerPosition(markers[i]).getOffset());
								if (markerLine == lineNumber) {
									breakpoints.add(markers[i]);
								}
							} catch (Exception e) {
							}
						}
					}
				}
			} catch (CoreException x) {
				JDIDebugUIPlugin.log(x.getStatus());
			}
		}		
		return breakpoints;
	}
	
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(fTextEditor.getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}
	
	protected IMarker[] getAllMarkers(IResource resource) throws CoreException {
		if (resource instanceof IFile) {
			return resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);					
		} else {
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			return root.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
		}	
	}
}