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
package streamit.eclipse.texteditor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import java.util.Set;
import org.eclipse.jdt.core.IField;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.core.IJavaElement;

public class ManageWatchpointRulerAction extends ManageBreakpointRulerAction {	
	
	public ManageWatchpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		super(editor, ruler);
		super.fAddLabel= StreamItEditorMessages.getString("ManageWatchpointRulerAction.add.label"); //$NON-NLS-1$
		super.fRemoveLabel= StreamItEditorMessages.getString("ManageWatchpointRulerAction.remove.label"); //$NON-NLS-1$
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
						IField[] fields = type.getFields();
						IField field = null;
						IRegion fieldRegion = null;
						int fieldStart = -1;
						int fieldEnd = -1;
						int checkStart, checkEnd;
						for (int i = 0; i < fields.length && field == null; i++) {
							fieldStart = fields[i].getSourceRange().getOffset();
							fieldRegion = document.getLineInformationOfOffset(fieldStart);
							checkStart = fieldRegion.getOffset();
							checkEnd = checkStart + fieldRegion.getLength();
							// check if field contained in line
							if (checkStart >= lineStart && checkEnd <= lineEnd) {
								field = fields[i];
								fieldStart = field.getNameRange().getOffset();
								fieldEnd = fieldStart + field.getNameRange().getLength();
							}
						}
						
						if (field == null) throw new Exception();
												
						Map attributes = new HashMap(10);
						BreakpointUtils.addJavaBreakpointAttributes(attributes, field);
						JDIDebugModel.createWatchpoint(getJavaResource(),type.getFullyQualifiedName(), field.getElementName(), -1, fieldStart, fieldEnd, 0, true, attributes);

						// in .java, leave highlighted area on breakpoint just added
						JavaUI.revealInEditor(fJavaEditorPart, (IJavaElement) field);


						// Add watchpoint in .str
						IRegion strLine= getDocument().getLineInformation(rulerLine - 1);
						fieldStart = strLine.getOffset();
						fieldEnd = fieldStart + strLine.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributes(attributes, field);
						JDIDebugModel.createWatchpoint(getResource(),type.getFullyQualifiedName(), field.getElementName(), -1, fieldStart, fieldEnd, 0, true, attributes);

					}
				}	
			}
		} catch (Exception e) {
			if (getTextEditor() != null) {
				IEditorStatusLine statusLine= (IEditorStatusLine) getTextEditor().getAdapter(IEditorStatusLine.class);
				if (statusLine != null) {
					statusLine.setMessage(true, ActionMessages.getString("ManageWatchpointActionDelegate.CantAdd"), null);
				}
			}		
			if (JDIDebugUIPlugin.getActiveWorkbenchShell() != null) {
				JDIDebugUIPlugin.getActiveWorkbenchShell().getDisplay().beep();
			}

		}		
		fTextEditor.setFocus();
	}
}