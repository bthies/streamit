package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

import javax.swing.JFrame;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEType;

/*
 * Created on Nov 26, 2003
 *
 */
 
public class GEFeedbackLoopController extends GEStreamNodeController{
	
	//TODO create a GEFeedbackLoopConfigurationDialog
	GESplitJoinConfigurationDialog dialog = null;
	
	/**
	  * Constructor. Set the default properties for the GEPhasedFilter. 
	  */
	 public GEFeedbackLoopController() 
	 {    	
		super();
		 setDefaultProperties();
	 }

	 /**
	  * Return the type of the controller.
	  */
	 public String toString() 
	 {
		 return GEType.GETypeToString(GEType.FEEDBACK_LOOP);
	 }


	/**
	 * Display the dialog that allows the user to configure the values for the splitjoin. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */

	 public boolean configure(GPDocument document) 
	 {
		setDefaultProperties();
		dialog = new GESplitJoinConfigurationDialog(new JFrame(), document);
		
		setPropertiesInDialog(this.properties);

		dialog.setVisible(true);	
		if (dialog.canceled()) return false;
		
		getPropertiesInDialog();
		return true;
	 }

	/**
	 * Display the dialog that allows the user to configure the values for the splitjoin. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */

	 public boolean configure(GPDocument document, Properties propert) 
	 {
		
		dialog = new GESplitJoinConfigurationDialog(new JFrame(), document);
		
		setPropertiesInDialog(propert);

		dialog.setVisible(true);	
		if (dialog.canceled()) return false;
		
		getPropertiesInDialog();
		return true;
	 }


	/**
	 * Set the properties in the dialog according to the values of propert
	 * @param propert Properties
	 */
	public void setPropertiesInDialog(Properties propert)
	{
		dialog.setName(propert.getProperty (GEProperties.KEY_NAME));
		dialog.setInputTape(propert.getProperty(GEProperties.KEY_INPUT_TAPE));
		dialog.setOutputTape(propert.getProperty(GEProperties.KEY_OUTPUT_TAPE));
		dialog.setImmediateParent(propert.getProperty(GEProperties.KEY_PARENT));
	}
	
	/**
	 * Get the properties in the dialog and put them in propert.
	 * @param propert Properties that are set according to values in dialog
	 */
	public void getPropertiesInDialog()
	{
		properties.put(GEProperties.KEY_NAME, dialog.getName());
		properties.put(GEProperties.KEY_INPUT_TAPE, dialog.getInputTape());
		properties.put(GEProperties.KEY_OUTPUT_TAPE, dialog.getOutputTape());
		properties.put(GEProperties.KEY_PARENT, dialog.getImmediateParent());
	}

	 /**
	  * Set the default properties of the GEFeedbackLoopController. If the default properties
	  * are not set again, then the values that are changed in the GEPipelineController, will
	  * remain stored. 
	  *
	  */
	 public void setDefaultProperties()
	 {
		properties.put(GEProperties.KEY_NAME, "FeedbackLoop_"+ GEProperties.id_count++);
		properties.put(GEProperties.KEY_INPUT_TAPE, "void");
		properties.put(GEProperties.KEY_OUTPUT_TAPE, "void");
		properties.put(GEProperties.KEY_PARENT, "Toplevel");
		properties.put(GEProperties.KEY_TYPE, GEType.FEEDBACK_LOOP);
		properties.put(GEProperties.KEY_JOINER_WEIGHTS, "1");
		properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, "1");
	 }
	

	 public Properties getDefaultConfiguration()
	 {
		 setDefaultProperties();
		 return properties;
	 }		
	
	
}