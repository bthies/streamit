package streamit.eclipse.debugger.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.ManageMethodBreakpointActionDelegate;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.core.IStreamItCoreConstants;
import streamit.eclipse.debugger.core.StreamItDebugEventFilter;
import streamit.eclipse.debugger.texteditor.StreamItEditorMessages;

/**
 * @author kkuo
 */
public class ManageMethodBreakpointRulerAction extends ManageBreakpointRulerAction {	
	
	public ManageMethodBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		super(editor, ruler);
		fAddLabel= StreamItEditorMessages.getString(IStreamItActionConstants.MANAGE_METHOD_BREAKPOINT_RULER_ACTION_ADD_LABEL); //$NON-NLS-1$
		fRemoveLabel= StreamItEditorMessages.getString(IStreamItActionConstants.MANAGE_METHOD_BREAKPOINT_RULER_ACTION_REMOVE_LABEL); //$NON-NLS-1$
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
		if (fMarkers.isEmpty()) {
			setText(fAddLabel);
			
			// find mapping between str and Java
			int validJavaLineNumber = fData.getJavaBreakpointLineNumber(getFile(), getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1);
			IEditorInput editorInput = fData.getJavaFileEditorInput();
			IDocument document = getJavaDocument();
			setEnabled(enableAction(validJavaLineNumber, document, editorInput));
		} else {
			setText(fRemoveLabel);
			setEnabled(true);
		}
	}
	
	public static boolean enableAction(int validJavaLineNumber, IDocument document, IEditorInput editorInput) {
		if (!enableAction(validJavaLineNumber)) return false;
		try {
			IRegion line = document.getLineInformation(validJavaLineNumber - 1);
			IType type = StreamItDebugEventFilter.getInstance().getType(editorInput, line);

			if (type == null) return false;

			IJavaProject project = type.getJavaProject();
			if (!type.exists() || project == null || !project.isOnClasspath(type)) return false;

			int lineStart = line.getOffset();
			int lineEnd = lineStart + line.getLength();
						
			// find field					
			IMethod[] methods = type.getMethods();
			IMethod method = null;
			IRegion methodRegion = null;
			int methodStart = -1;
			int methodEnd = -1;
			int checkStart, checkEnd;
			for (int i = 0; i < methods.length && method == null; i++) {
				methodStart = methods[i].getSourceRange().getOffset();
				methodRegion = document.getLineInformationOfOffset(methodStart);
				checkStart = methodRegion.getOffset();
				checkEnd = checkStart + methodRegion.getLength();
				// check if field contained in line
				if (checkStart >= lineStart && checkEnd <= lineEnd) {
					method = methods[i];
					methodStart = method.getNameRange().getOffset();
					methodEnd = methodStart + method.getNameRange().getLength();
				}
			}
				
			if (method == null) return false;
		} catch (BadLocationException ble) {
			return false;
		} catch (JavaModelException jme) {
			return false;
		} catch (CoreException ce) {
			return false;
		}
		return true;
	}
		
	protected void addMarker() {
		IEditorInput editorInput = fData.getJavaFileEditorInput();
		IDocument document = getJavaDocument();

		// find mapping between str and Java
		int strRulerLineNumber = getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1;
		int validJavaLineNumber = fData.getJavaBreakpointLineNumber(getFile(), strRulerLineNumber);

		// get & verify line number
		try {
			if (validJavaLineNumber > 0) {
				IRegion line= document.getLineInformation(validJavaLineNumber - 1);
				IType type = StreamItDebugEventFilter.getInstance().getType(editorInput, line);

				if (type != null) {
					IJavaProject project= type.getJavaProject();
					if (type.exists() && project != null && project.isOnClasspath(type)) {
						int lineStart = line.getOffset();
						int lineEnd = lineStart + line.getLength();
						
						// find field					
						IMethod[] methods = type.getMethods();
						IMethod method = null;
						IRegion methodRegion = null;
						int methodStart = -1;
						int methodEnd = -1;
						int checkStart, checkEnd;
						for (int i = 0; i < methods.length && method == null; i++) {
							methodStart = methods[i].getSourceRange().getOffset();
							methodRegion = document.getLineInformationOfOffset(methodStart);
							checkStart = methodRegion.getOffset();
							checkEnd = checkStart + methodRegion.getLength();
							// check if field contained in line
							if (checkStart >= lineStart && checkEnd <= lineEnd) {
								method = methods[i];
								methodStart = method.getNameRange().getOffset();
								methodEnd = methodStart + method.getNameRange().getLength();
							}
						}						
						
						if (method == null) throw new BadLocationException();
						
						// Add method breakpoint to .java
						Map attributes = new HashMap(10);
						BreakpointUtils.addJavaBreakpointAttributes(attributes, method);
						String methodName = method.getElementName();
						if (((IMethod)method).isConstructor()) methodName = IStreamItActionConstants.MANAGE_METHOD_BREAKPOINT_RULER_ACTION_INIT; //$NON-NLS-1$
						String methodSignature= method.getSignature();
						if (!type.isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(type, methodSignature);
						String typeName = type.getFullyQualifiedName();
						IJavaMethodBreakpoint b = JDIDebugModel.createMethodBreakpoint(getJavaResource(), typeName, methodName, methodSignature, true, false, false, validJavaLineNumber, methodStart, methodEnd, 0, true, attributes);
						b.getMarker().setAttribute(IStreamItCoreConstants.ID_FILTER_INST_BREAKPOINT, false);

						// Add method breakpoint to .str
						IRegion strLine = getDocument().getLineInformation(strRulerLineNumber - 1);
						methodStart = strLine.getOffset();
						methodEnd = methodStart + strLine.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributes(attributes, method);
						attributes.put(IStreamItActionConstants.HANDLE_ID, null);
						JDIDebugModel.createMethodBreakpoint(getFile(), typeName, methodName, methodSignature, true, false, false, strRulerLineNumber, methodStart, methodEnd, 0, true, attributes);
					}
				}
			}
		} catch (BadLocationException ble) {
			addMarkerError();
		} catch (JavaModelException jme) {
			addMarkerError();
		} catch (CoreException ce) {
			addMarkerError();
		}		
		fTextEditor.setFocus();
	}

	private void addMarkerError() {
		if (getTextEditor() != null) {
			IEditorStatusLine statusLine= (IEditorStatusLine) getTextEditor().getAdapter(IEditorStatusLine.class);
			if (statusLine != null) {
				statusLine.setMessage(true, ActionMessages.getString(IStreamItActionConstants.MANAGE_METHOD_BREAKPOINT_RULER_ACTION_CANT_ADD), null);
			}
		}		
		if (JDIDebugUIPlugin.getActiveWorkbenchShell() != null) {
			JDIDebugUIPlugin.getActiveWorkbenchShell().getDisplay().beep();
		}
	}
}