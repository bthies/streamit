/*
 * Created on Dec 4, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * @author jcarlos
 *
 */


public class ToolBoxFeedbackLoop extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxNode.
	 * @param graphpad
	 */
	public ToolBoxFeedbackLoop(GPGraphpad graphpad) {
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
		JToggleButton button = graphpad.getMarqueeHandler().getButtonFeedbackLoop();
		GPBarFactory.fillToolbarButton(
					button,
					getName(),
					actionCommand);
		return button;
	}

	/**
	 * Update method
	 */
	public void update() {
		super.update();
		graphpad.getMarqueeHandler().getButtonFeedbackLoop().setEnabled(isEnabled());
	}

}
