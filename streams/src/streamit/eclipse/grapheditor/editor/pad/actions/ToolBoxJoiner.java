
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * Action to add a GEJoiner to the graph editor. 
 * @author jcarlos
 */
public class ToolBoxJoiner extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 */
	public ToolBoxJoiner(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 * @param name
	 */
	public ToolBoxJoiner(GPGraphpad graphpad, String name) {
		super(graphpad, name);
	}

	/**
	 * Constructor for ToolBoxJoiner.
	 * @param graphpad
	 * @param name
	 * @param icon
	 */
	public ToolBoxJoiner(GPGraphpad graphpad, String name, Icon icon) {
		super(graphpad, name, icon);
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
		JToggleButton button = graphpad.getMarqueeHandler().getButtonJoiner() ;
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
		graphpad.getMarqueeHandler().getButtonJoiner().setEnabled(isEnabled());
	}

}
