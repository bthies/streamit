package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.BrowserLauncher;

/**
 * Shows the website where the user can report a bug they have found.
 * @author jcarlos
 */
public class HelpSubmitABug extends AbstractActionDefault {

	/** 
	 * The about dialog for the GraphEditor
	 */
	protected JDialog aboutDlg;

	/**
	 * Constructor 
	 * @param graphpad
	 */
	public HelpSubmitABug(GPGraphpad graphpad) 
	{
		super(graphpad);
	}

	/** 
	 * Opens the url where the user can report a bug that they found in the program.
	 *
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{

		try {		
			BrowserLauncher.openURL("http://cag.lcs.mit.edu/streamit/html/contact.html");		
		} 
		catch (Exception ex){
			JOptionPane.showMessageDialog(graphpad, ex.toString(), Translator.getString("Error"), JOptionPane.ERROR_MESSAGE );
		}
		
	}
	/** Empty implementation. 
	 *  This Action should be available each time.
	 */
	public void update(){};

}
