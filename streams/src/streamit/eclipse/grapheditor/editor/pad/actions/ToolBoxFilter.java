package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JToggleButton;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPBarFactory;

/**
 * Action to add a GEPhasedFilter to the graph editor. 
 * @author jcarlos
 */
public class ToolBoxFilter extends AbstractActionDefault {

	/**
	 * Constructor for ToolBoxFilter.
	 * @param graphpad GPGraphpad
	 */
	public ToolBoxFilter(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Add a GEPhasedFilter in the graph editor.
	 */
	public void actionPerformed(ActionEvent e) {
	}
	/**
	 * @see org.jgraph.pad.actions.AbstractActionDefault#getToolComponent(String)
	 */
	protected Component getToolComponent(String actionCommand) {
		JToggleButton button = graphpad.getMarqueeHandler().getButtonFilter();
		GPBarFactory.fillToolbarButton(
					button,
					getName(),
					actionCommand);
		return button;
	}
	/** 
	 * 
	 */
	public void update() {
		super.update();
		graphpad.getMarqueeHandler().getButtonFilter().setEnabled(isEnabled());
	}

}
