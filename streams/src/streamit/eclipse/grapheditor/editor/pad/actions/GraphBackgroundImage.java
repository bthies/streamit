package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Action opens a file dialog to select an image.
 * Afterthat the Action applies the selected image
 * to the graph background.
 *
 * @author sven.luzar
 *
 */
public class GraphBackgroundImage extends AbstractActionFile {

	/**
	 * Constructor for GraphBackgroundImage.
	 * @param graphpad
	 * @param name
	 */
	public GraphBackgroundImage(GPGraphpad graphpad) {
		super(graphpad);
	}

	public void actionPerformed(ActionEvent e) {
		String name =
			openDialog(Translator.getString("ImageDialog"), null, null);
		if (name != null) {
			try {
				// JDK 1.3
				//File f = new File(name);
				//Image value = new ImageIcon(ImageIO.read(f)).getImage();
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image value = toolkit.getImage(name);
				graphpad.getCurrentGraph() .setBackgroundImage(value);
				graphpad.getCurrentGraph() .repaint();
			} catch (Exception ex) {
				graphpad.error(ex.toString());
			}
		}
	}

}
