/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 * @author kkuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package streamit.eclipse.debugger.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.ManageMethodBreakpointActionDelegate;
import org.eclipse.jdt.ui.JavaUI;
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
		super.fAddLabel= StreamItEditorMessages.getString("ManageMethodBreakpointRulerAction.add.label"); //$NON-NLS-1$
		super.fRemoveLabel= StreamItEditorMessages.getString("ManageMethodBreakpointRulerAction.remove.label"); //$NON-NLS-1$
	}
	
	protected void addMarker() {
		IEditorInput editorInput = getJavaEditorPart().getEditorInput();
		IDocument document = getJavaDocument();

		// find mapping between str and Java
		int rulerLine= getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1;
		int lineNumber;
		
		Object isThere = fStrToJava.get(new Integer(rulerLine));
		if (isThere == null) return;
		else lineNumber = ((Integer) ((Set) isThere).iterator().next()).intValue();

		// get & verify line number
		try {
			if (lineNumber > 0) {
				IRegion line= document.getLineInformation(lineNumber - 1);
				IType type = setType(editorInput, line);

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

						// in .java, leave highlighted area on breakpoint just added
						JavaUI.revealInEditor(fJavaEditorPart, (IJavaElement) method);

						// Add method breakpoint to .str
						IRegion strLine= getDocument().getLineInformation(rulerLine - 1);
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