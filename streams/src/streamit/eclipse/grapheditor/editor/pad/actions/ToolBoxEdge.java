
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * Action that will add an edge connection between nodes.
 * @author jcarlos
 */
public class ToolBoxEdge extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxEdge.
	 * @param graphpad GPGraphpad
	 */
	public ToolBoxEdge(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Add connection edge between nodes. 
	 */
	public void actionPerformed(ActionEvent e) {
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JToggleButton button = graphpad.getMarqueeHandler().getButtonEdge();
		GPBarFactory.fillToolbarButton(
					button,
					getName(),
					actionCommand);
		return button;
	}
	
	/**
	 * Set the button as enabled or not enabled depending on its status.
	 */
	public void update() {
		super.update();
		graphpad.getMarqueeHandler().getButtonEdge().setEnabled(isEnabled());
	}

}
