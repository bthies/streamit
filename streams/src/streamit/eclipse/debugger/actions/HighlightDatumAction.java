package streamit.eclipse.debugger.actions;

import org.eclipse.debug.internal.ui.actions.ChangeVariableValueInputDialog;
import org.eclipse.jface.action.Action;

//import streamit.eclipse.debugger.core.LaunchData;
//import streamit.eclipse.debugger.graph.Channel;

/**
 * @author kkuo
 */
public class HighlightDatumAction extends Action {

	private ChangeVariableValueInputDialog fInputDialog;
	//private Channel fChannel;
	
	public HighlightDatumAction() {
		super(ActionMessages.getString("HighlightDatum.highlightDatum")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("HighlightDatum.toolTipText")); //$NON-NLS-1$
	}

	/**
	 * Updates the enabled state of this action based
	 * on the selection
	 */
	public void update() {//Channel c) {
		if (true) {//c.getVariable() == null) {
			setEnabled(false);
			return;
		}
		
		setEnabled(true);
		//fChannel = c;
		if (true) {//c.isHighlighted()) {
			setText(ActionMessages.getString("HighlightDatum.unhighlightDatum"));
		} else {
			setText(ActionMessages.getString("HighlightDatum.highlightDatum"));
		}
	}

	public void disable() {
		//fChannel = null;
		setEnabled(false);
		setText(ActionMessages.getString("HighlightDatum.highlightDatum"));
	}
	
	/**
	 * @see IAction#run()
	 */
	public void run() {
		/*
		if (fChannel == null) return;
		if (fChannel.isHighlighted()) {
			fChannel.unhighlight();
			LaunchData.changeHighlightAttribute(fChannel.getVariable(), false);
		} else {
			fChannel.highlight();
			LaunchData.changeHighlightAttribute(fChannel.getVariable(), true);
		}
		*/
	}	
}
