/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
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

import streamit.eclipse.debugger.texteditor.StreamItEditorMessages;

public class ManageMethodBreakpointRulerAction extends ManageBreakpointRulerAction {	
	
	public ManageMethodBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		super(editor, ruler);
		fAddLabel= StreamItEditorMessages.getString("ManageMethodBreakpointRulerAction.add.label"); //$NON-NLS-1$
		fRemoveLabel= StreamItEditorMessages.getString("ManageMethodBreakpointRulerAction.remove.label"); //$NON-NLS-1$
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
		if (fMarkers.isEmpty()) {
			setText(fAddLabel);
			
			// find mapping between str and Java
			int validJavaLineNumber = fData.getJavaBreakpointLineNumber(getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1);
			IEditorInput editorInput = fData.getJavaEditorPart().getEditorInput();
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
			IType type = getType(editorInput, line);

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
		}
		return true;
	}
		
	protected void addMarker() {
		IEditorInput editorInput = fData.getJavaEditorPart().getEditorInput();
		IDocument document = getJavaDocument();

		// find mapping between str and Java
		int strRulerLineNumber = getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1;
		int validJavaLineNumber = fData.getJavaBreakpointLineNumber(strRulerLineNumber);

		// get & verify line number
		try {
			if (validJavaLineNumber > 0) {
				IRegion line= document.getLineInformation(validJavaLineNumber - 1);
				IType type = getType(editorInput, line);

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
						
						if (method == null) throw new Exception();
						
						// Add method breakpoint to .java
						Map attributes = new HashMap(10);
						BreakpointUtils.addJavaBreakpointAttributes(attributes, method);
						String methodName = method.getElementName();
						if (((IMethod)method).isConstructor()) methodName = "<init>"; //$NON-NLS-1$
						String methodSignature= method.getSignature();
						if (!type.isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(type, methodSignature);
						JDIDebugModel.createMethodBreakpoint(getJavaResource(), type.getFullyQualifiedName(), methodName, methodSignature, true, false, false, -1, methodStart, methodEnd, 0, true, attributes);

						// Add method breakpoint to .str
						IRegion strLine= getDocument().getLineInformation(strRulerLineNumber - 1);
						methodStart = strLine.getOffset();
						methodEnd = methodStart + strLine.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributes(attributes, method);
						JDIDebugModel.createMethodBreakpoint(getResource(), type.getFullyQualifiedName(), methodName, methodSignature, true, false, false, -1, methodStart, methodEnd, 0, true, attributes);
					}
				}	
			}
		} catch (Exception e) {
			if (getTextEditor() != null) {
				IEditorStatusLine statusLine= (IEditorStatusLine) getTextEditor().getAdapter(IEditorStatusLine.class);
				if (statusLine != null) {
					statusLine.setMessage(true, ActionMessages.getString("ManageMethodBreakpointActionDelegate.CantAdd"), null);
				}
			}		
			if (JDIDebugUIPlugin.getActiveWorkbenchShell() != null) {
				JDIDebugUIPlugin.getActiveWorkbenchShell().getDisplay().beep();
			}
		}		
		fTextEditor.setFocus();
	}
}