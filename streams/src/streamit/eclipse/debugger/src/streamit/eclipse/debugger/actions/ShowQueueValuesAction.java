package streamit.eclipse.debugger.actions;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.graph.ChannelWidget;

/**
 * @author kkuo
 */
public class ShowQueueValuesAction extends Action {
	
	private ShowQueueValuesDialog fInputDialog;
	private ChannelWidget fChannel;
	
	public ShowQueueValuesAction() {
		super(ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_TITLE)); //$NON-NLS-1$
		setDescription(ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_TOOL_TIP_TEXT)); //$NON-NLS-1$
		
		ImageRegistry ir = StreamItDebuggerPlugin.getDefault().getImageRegistry();
		setImageDescriptor(ir.getDescriptor(IStreamItDebuggerPluginConstants.SHOW_CHANNEL_IMAGE));
		setDisabledImageDescriptor(ir.getDescriptor(IStreamItDebuggerPluginConstants.FADED_SHOW_CHANNEL_IMAGE));
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
			DebugUIPlugin.errorDialog(activeShell, ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_ERROR_DIALOG_TITLE),ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_ERROR_DIALOG_MESSAGE), exception);	//$NON-NLS-2$ //$NON-NLS-1$
			fInputDialog= null;
			return;
		}
		
		fInputDialog= new ShowQueueValuesDialog(activeShell, ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_SET_VARIABLE_VALUE), ActionMessages.getString(IStreamItActionConstants.SHOW_QUEUE_VALUES_ENTER_A_NEW_VALUE), value, new IInputValidator() { //$NON-NLS-1$ //$NON-NLS-2$
			/**
			 * Returns an error string if the input is invalid
			 */
			public String isValid(String input) {
				return null;
			}
		});
		
		fInputDialog.open();
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