
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * Action to add a GESplitter to the graph editor. 
 * @author jcarlos
 */
public class ToolBoxSplitter extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxSplitter.
	 * @param graphpad
	 */
	public ToolBoxSplitter(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
	}
	
	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JToggleButton button = graphpad.getMarqueeHandler().getButtonSplitter();
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
		graphpad.getMarqueeHandler().getButtonSplitter().setEnabled(isEnabled());
	}

}
