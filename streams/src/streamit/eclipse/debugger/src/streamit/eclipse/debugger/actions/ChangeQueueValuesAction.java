package streamit.eclipse.debugger.actions;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.help.WorkbenchHelp;

import streamit.eclipse.debugger.graph.ChannelWidget;

/**
 * @author kkuo
 * Action for changing the value of primitives and <code>String</code> variables.
 */
public class ChangeQueueValuesAction extends Action {

	private ChangeVariableValueInputDialog fInputDialog;
	private ChannelWidget fChannel;
	
	public ChangeQueueValuesAction() {
		super(ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_TITLE)); //$NON-NLS-1$
		setDescription(ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_TOOL_TIP_TEXT)); //$NON-NLS-1$
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_CHANGE_VARIABLE_VALUE));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_CHANGE_VARIABLE_VALUE));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_CHANGE_VARIABLE_VALUE));
		WorkbenchHelp.setHelp(this,	IDebugHelpContextIds.CHANGE_VALUE_ACTION);
	}
	
	/**
	 * Edit the variable value with an inline text editor.  
	 */
	protected void doActionPerformed(final ChannelWidget c) {
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}
		Shell activeShell= window.getShell();
		
		// If a previous edit is still in progress, don't start another
		if (fInputDialog != null) {
			return;
		}
		fChannel = c;
		String value= IStreamItActionConstants.EMPTY_STRING; //$NON-NLS-1$
		try {
			value= c.getQueueAsString();
		} catch (DebugException exception) {
			DebugUIPlugin.errorDialog(activeShell, ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_ERROR_DIALOG_TITLE),ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_ERROR_DIALOG_MESSAGE), exception);	//$NON-NLS-2$ //$NON-NLS-1$
			fInputDialog= null;
			return;
		}

		fInputDialog= new ChangeVariableValueInputDialog(activeShell, ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_SET_VARIABLE_VALUE), ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_ENTER_A_NEW_VALUE), value, new IInputValidator() { //$NON-NLS-1$ //$NON-NLS-2$
			/**
			 * Returns an error string if the input is invalid
			 */
			public String isValid(String input) {
				try {
					if (fChannel.verifyValues(input)) {
						return null; // null means valid
					}
				} catch (DebugException exception) {
					return ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_AN_EXCEPTION_OCCURRED); //$NON-NLS-1$
				}
				return ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_INVALID_VALUE); //$NON-NLS-1$
			}
		});

		fInputDialog.open();
		String newValue= fInputDialog.getValue();
		if (newValue != null) {
			// null value means cancel was pressed
			try {
				fChannel.update(newValue);
			} catch (DebugException de) {
				DebugUIPlugin.errorDialog(activeShell, ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_ERROR_DIALOG_TITLE),ActionMessages.getString(IStreamItActionConstants.CHANGE_QUEUE_VALUES_ERROR_DIALOG_MESSAGE), de);	//$NON-NLS-2$ //$NON-NLS-1$
				fInputDialog= null;
				return;
			}
		}
		fInputDialog= null;
	}
		
	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	public void update(ChannelWidget c) {
		if (c.isChangeable()) {
			setEnabled(true);
			fChannel = c;
			return; 
		}
		
		setEnabled(false);
		fChannel = null;
	}
	
	public void disable() {
		fChannel = null;
		setEnabled(false);
	}

	/**
	 * @see IAction#run()
	 */
	public void run() {
		doActionPerformed(fChannel);
	}	
}