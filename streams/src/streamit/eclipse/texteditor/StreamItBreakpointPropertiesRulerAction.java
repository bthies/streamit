/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.JavaBreakpointPropertiesRulerAction
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
package texteditor;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.JavaBreakpointPropertiesDialog;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Presents a custom properties dialog to configure
 * the attibutes of a Java Breakpoint from the ruler popup menu of a 
 * text editor.
 */
public class StreamItBreakpointPropertiesRulerAction extends ManageBreakpointRulerAction {

	protected IBreakpoint fBreakpoint;
	
	/**
	 * Creates the action to enable/disable breakpoints
	 */
	public StreamItBreakpointPropertiesRulerAction(ITextEditor editor, IVerticalRulerInfo info) {
		super(editor, info);
		setText(ActionMessages.getString("JavaBreakpointPropertiesRulerAction.Breakpoint_&Properties_1")); //$NON-NLS-1$
		fBreakpoint = null;
	}
	
	protected IBreakpoint getBreakpoint() {
		return fBreakpoint;
	}

	protected void setBreakpoint(IBreakpoint breakpoint) {
		fBreakpoint = breakpoint;
	}
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		if (getBreakpoint() != null) {
			Dialog d= 
				new JavaBreakpointPropertiesDialog(getTextEditor().getEditorSite().getShell(), (IJavaBreakpoint)getBreakpoint());
			d.open();	
		}
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		setBreakpoint(determineBreakpoint());
		if (getBreakpoint() == null || !(getBreakpoint() instanceof IJavaBreakpoint)) {
			setBreakpoint(null);
			setEnabled(false);
			return;
		}
		setEnabled(true);
	}
	
	protected IBreakpoint determineBreakpoint() {
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		
		Iterator i = getJavaMarkers().iterator();
		while (i.hasNext()) {
			IBreakpoint breakpoint = breakpointManager.getBreakpoint((IMarker) i.next());
			if (breakpoint instanceof IJavaLineBreakpoint) return (IJavaLineBreakpoint)breakpoint;
		}

		return null;
	}
	
}