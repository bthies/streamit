package streamit.eclipse.debugger.actions;

import org.eclipse.debug.internal.ui.actions.ChangeVariableValueInputDialog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageRegistry;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.graph.Channel;
import streamit.eclipse.debugger.graph.OptionData;

/**
 * @author kkuo
 */
public class HighlightDatumAction extends Action {

	private ChangeVariableValueInputDialog fInputDialog;
	private Channel fChannel;
	private int fIndex;
	private OptionData fAllExpanded;
	
	public HighlightDatumAction() {
		super(ActionMessages.getString("HighlightDatum.highlightDatum")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("HighlightDatum.toolTipText")); //$NON-NLS-1$
		
		ImageRegistry ir = StreamItDebuggerPlugin.getDefault().getImageRegistry();
		setImageDescriptor(ir.getDescriptor(IStreamItDebuggerPluginConstants.HIGHLIGHT));
	}

	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	public void update(Channel c, int y, OptionData optionData) {
		fIndex = c.onDatum(y);
		if (fIndex < 0) {
			setEnabled(false);
			setText(ActionMessages.getString("HighlightDatum.highlightDatum"));
			return;
		}
		
		setEnabled(true);
		fChannel = c;
		fAllExpanded = optionData;
		if (c.isHighlighted(fIndex, fAllExpanded)) {
			setText(ActionMessages.getString("HighlightDatum.unhighlightDatum"));
		} else {
			setText(ActionMessages.getString("HighlightDatum.highlightDatum"));
		}
	}

	public void disable() {
		fChannel = null;
		setEnabled(false);
		setText(ActionMessages.getString("HighlightDatum.highlightDatum"));
	}
	
	/**
	 * @see IAction#run()
	 */
	public void run() {
		if (fChannel == null) return;
		if (fChannel.isHighlighted(fIndex, fAllExpanded)) {
			fChannel.unhighlight(fIndex, fAllExpanded);
		} else {
			fChannel.highlight(fIndex, fAllExpanded);
		}
	}	
}
