/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 * @author kkuo
 *******************************************************************************/

package streamit.eclipse.debugger.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.texteditor.StreamItEditorMessages;

public class ManageWatchpointRulerAction extends ManageBreakpointRulerAction {
	
	public ManageWatchpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		super(editor, ruler);
		fAddLabel= StreamItEditorMessages.getString("ManageWatchpointRulerAction.add.label"); //$NON-NLS-1$
		fRemoveLabel= StreamItEditorMessages.getString("ManageWatchpointRulerAction.remove.label"); //$NON-NLS-1$
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
		if (fMarkers.isEmpty()) {
			setText(fAddLabel);
			
			// find mapping between str and Java
			int validJavaLineNumber = fData.getJavaWatchpoinLineNumber(getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1);
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
			
			if (field == null) return false;
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
		int validJavaLineNumber = fData.getJavaWatchpoinLineNumber(strRulerLineNumber);

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
						JDIDebugModel.createWatchpoint(getJavaResource(),type.getFullyQualifiedName(), field.getElementName(), validJavaLineNumber, fieldStart, fieldEnd, 0, true, attributes);

						// Add watchpoint in .str
						IRegion strLine= getDocument().getLineInformation(strRulerLineNumber - 1);
						fieldStart = strLine.getOffset();
						fieldEnd = fieldStart + strLine.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributes(attributes, field);
						JDIDebugModel.createWatchpoint(getFile(),type.getFullyQualifiedName(), field.getElementName(), strRulerLineNumber, fieldStart, fieldEnd, 0, true, attributes);

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
	
	protected boolean includesJavaRulerLine(Position position, IDocument document) {
		if (position != null) {
			try {
				int markerLine = document.getLineOfOffset(position.getOffset());
				int line = fRuler.getLineOfLastMouseButtonActivity() + 1;
				
				// find mapping to .java
				line = fData.getJavaWatchpoinLineNumber(line);
				if (line < 0) return false;
				if (line - 1 == markerLine) return true;
			} catch (BadLocationException x) {
			}
		}		
		return false;
	}

}