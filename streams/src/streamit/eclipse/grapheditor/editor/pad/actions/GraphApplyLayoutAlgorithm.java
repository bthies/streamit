package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.layout.LayoutAlgorithm;
import streamit.eclipse.grapheditor.editor.layout.LayoutController;
import streamit.eclipse.grapheditor.editor.layout.LayoutDialog;

/**
 * Calls a frame to select the Layoutalgorithm.
 * After selecting the action applies the
 * algorithm to the current graph.
 * 
 * @author sven.luzar
 *
 */
public class GraphApplyLayoutAlgorithm extends AbstractActionDefault {

	/**
	 * Constructor for GraphApplyLayoutAlgorithm.
	 * 
	 * @param graphpad
	 * @param name
	 */
	public GraphApplyLayoutAlgorithm(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**Implementation
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Frame f = JOptionPane.getFrameForComponent(graphpad);
		final LayoutDialog dlg = new LayoutDialog(f);
		dlg.show();

		if (dlg.isCanceled())
			return;

		final LayoutController controller = dlg.getSelectedLayoutController();
		if (controller == null)
			return;
		Thread t = new Thread("Layout Algorithm " + controller.toString()) {
			public void run() {
				LayoutAlgorithm algorithm = controller.getLayoutAlgorithm();
				algorithm.perform(
					graphpad.getCurrentGraph(),
					dlg.isApplyLayoutToAll(),
					controller.getConfiguration());
			}
		};
		t.start();

	}
}