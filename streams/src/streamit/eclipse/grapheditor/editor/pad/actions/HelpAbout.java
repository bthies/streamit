/*
 * Created on December 1, 2003
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.MissingResourceException;

import javax.swing.ImageIcon;
import javax.swing.JDialog;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPAboutDialog;
import streamit.eclipse.grapheditor.editor.pad.resources.ImageLoader;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Display the "about dialog" for the GraphEditor
 * @author jcarlos
 */
public class HelpAbout extends AbstractActionDefault {

	/** 
	 * The about dialog for the GraphEditor
	 */
	protected JDialog aboutDlg;

	/**
	 * Constructor for HelpAbout.
	 * @param graphpad
	 */
	public HelpAbout(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (aboutDlg == null) 
		{	
			String iconName = Translator.getString("Logo");
			ImageIcon logoIcon = ImageLoader.getImageIcon(iconName);
			
			try 
			{
				String title = Translator.getString("AboutFrameTitle");	
				aboutDlg = new GPAboutDialog(graphpad.getFrame(), title, logoIcon);
			} catch (MissingResourceException mre) {
				aboutDlg =
					new GPAboutDialog(
						graphpad.getFrame(),
						"About StreamIt GraphEditor",
						logoIcon);
			}
		}
		aboutDlg.show();
	}
	/** Empty implementation. 
	 *  This Action should be available
	 *  each time.
	 */
	public void update(){};

}
