/*
 * Created on Nov 26, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

import javax.swing.JFrame;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * The GEFilterController class allows the user to administrate
 * the property values for a GEPipeline. The values that can be set 
 * are the name, input tape, output tape, parent, push rate, pop rate, 
 * and peek rate.
 * 
 * @author jcarlos
 */
public class GEFilterController extends GEStreamNodeController{
	
	
	GEFilterConfigurationDialog dialog = null;
	
	/**
	  * Constructor. Set the default properties for the GEPhasedFilter. 
	  */
	 public GEFilterController() 
	 {    	
		 super();
		 setDefaultProperties();
	 }

	/**
	 * Return the type of the controller.
	 * @return String type of the controller
	 */
	 public String toString() 
	 {
		 return GEType.GETypeToString(GEType.PHASED_FILTER);
	 }

	/**
	 * Display the dialog that allows the user to configure the values for the filter. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */
	 public boolean configure(GPDocument document) 
	 {
		setDefaultProperties();
    	
		dialog = new GEFilterConfigurationDialog(new JFrame(), document);
        
		setPropertiesInDialog(this.properties);
		 
		
		dialog.setVisible(true);
		if (dialog.canceled()) return false;
		
		getPropertiesInDialog();
		return true;
        
	 }
	 
	/**
	 * Display the dialog that allows the user to configure the values for the filter. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */ 
	public boolean configure(GPDocument document, Properties props) 
	{
	       	
	   dialog = new GEFilterConfigurationDialog(new JFrame(), document);
        
	   setPropertiesInDialog(props);
	   dialog.saveInitialLevel(props.getProperty(GEProperties.KEY_LEVEL));
		 
		
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
		dialog.saveInitialName(propert.getProperty (GEProperties.KEY_NAME));
		dialog.setName(propert.getProperty (GEProperties.KEY_NAME));
		dialog.setInputTape(propert.getProperty(GEProperties.KEY_INPUT_TAPE));
		dialog.setOutputTape(propert.getProperty(GEProperties.KEY_OUTPUT_TAPE));		 
		dialog.setImmediateParent(propert.getProperty(GEProperties.KEY_PARENT));
		dialog.setPushRate(propert.getProperty(GEProperties.KEY_PUSH_RATE));
		dialog.setPopRate(propert.getProperty(GEProperties.KEY_POP_RATE));
		dialog.setPeekRate(propert.getProperty(GEProperties.KEY_PEEK_RATE));
	}
	
	/**
	 * Get the properties in the dialog and place them in the controller's
	 * Properties field.
	 */
	public void getPropertiesInDialog()
	{
		properties.put(GEProperties.KEY_NAME, dialog.getName());
		properties.put(GEProperties.KEY_INPUT_TAPE, dialog.getInputTape());
		properties.put(GEProperties.KEY_OUTPUT_TAPE, dialog.getOutputTape());
		properties.put(GEProperties.KEY_PARENT, dialog.getImmediateParent());
		properties.put(GEProperties.KEY_PUSH_RATE, dialog.getPushRate());
		properties.put(GEProperties.KEY_POP_RATE, dialog.getPopRate());
		properties.put(GEProperties.KEY_PEEK_RATE, dialog.getPeekRate());
	}
	
	 /**
	  * Set the default properties of the GEFilterController. If the default properties
	  * are not set again, then the values that are changed in the GEFilterController, will
	  * remain stored. 
	  *
	  */
	 public void setDefaultProperties()
	 {
		 properties.put(GEProperties.KEY_NAME, "Filter_"+ GEProperties.id_count++);
		 properties.put(GEProperties.KEY_INPUT_TAPE, "void");
		 properties.put(GEProperties.KEY_OUTPUT_TAPE, "void");
		 properties.put(GEProperties.KEY_PARENT, "Toplevel");
		 properties.put(GEProperties.KEY_TYPE, GEType.PHASED_FILTER);
		 properties.put(GEProperties.KEY_POP_RATE, "0");
		 properties.put(GEProperties.KEY_PUSH_RATE, "0");
		 properties.put(GEProperties.KEY_PEEK_RATE, "0");
	 }
	
	/**
	 * Get the default properties for a GEFilterController.
	 * @return Properties the default properties of a GEFilterController.
	 */
	 public Properties getDefaultConfiguration()
	 {
		 setDefaultProperties();
		 return properties;
	 }
}