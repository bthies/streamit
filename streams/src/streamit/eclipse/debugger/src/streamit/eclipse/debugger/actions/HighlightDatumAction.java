package streamit.eclipse.debugger.actions;

import org.eclipse.debug.internal.ui.actions.ChangeVariableValueInputDialog;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageRegistry;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.graph.ChannelWidget;
import streamit.eclipse.debugger.graph.DatumWidget;
import streamit.eclipse.debugger.graph.OptionData;

/**
 * @author kkuo
 */
public class HighlightDatumAction extends Action {

	private ChangeVariableValueInputDialog fInputDialog;
	private DatumWidget fDatum;
	private OptionData fAllExpanded;
		
	public HighlightDatumAction() {
		super(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_DATUM)); //$NON-NLS-1$
		setDescription(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_TOOL_TIP_TEXT)); //$NON-NLS-1$
		
		ImageRegistry ir = StreamItDebuggerPlugin.getDefault().getImageRegistry();
		setImageDescriptor(ir.getDescriptor(IStreamItDebuggerPluginConstants.HIGHLIGHT_IMAGE));
	}
	
	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	public void update(ChannelWidget c, Point p, OptionData optionData) {
		fDatum = c.onDatum(p);
		if (fDatum == null) {
			setEnabled(false);
			setText(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_DATUM));
			return;
		}
		
		setEnabled(true);
		fAllExpanded = optionData;
		if (fDatum.isHighlighted()) {
			setText(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_UNHIGHLIGHT_DATUM));
		} else {
			setText(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_DATUM));
		}
	}

	public void disable() {
		fDatum = null;
		setEnabled(false);
		setText(ActionMessages.getString(IStreamItActionConstants.HIGHLIGHT_DATUM_HIGHLIGHT_DATUM));
	}
	
	/**
	 * @see IAction#run()
	 */
	public void run() {
		if (fDatum == null) return;
		if (fDatum.isHighlighted()) {
			fDatum.unhighlight(fAllExpanded);
		} else {
			fDatum.highlight(fAllExpanded);
		}
	}	
}
