/*
 * Created on Nov 13, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

import javax.swing.JFrame;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * The GEPipelineController class allows the user to administrate
 * the property values for a GEPipeline. The values that can be set 
 * are the name, input tape, output tape, arguments, and parent.
 * 
 * @author jcarlos
 */
public class GEPipelineController extends GEStreamNodeController {

	GEPipelineConfigurationDialog dialog = null;

    /**
     * Constructor. Set the default properties for the GEPipeline. 
     */
    public GEPipelineController() 
    {    	
    	super();
       	setDefaultProperties();
    }

    /**
     * Return the type of the controller.
     */
    public String toString() 
    {
        return GEType.GETypeToString(GEType.PIPELINE);
    }


	/**
	 * Display the dialog that allows the user to configure the values for the pipeline. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */

    public boolean configure(GPDocument document) 
    {
    	setDefaultProperties();
    	
		dialog = new GEPipelineConfigurationDialog(new JFrame(), document);
        setPropertiesInDialog(this.properties);
        		
        dialog.setVisible(true);
        if (dialog.canceled()) return false;

		getPropertiesInDialog();
		        
        return true;
    }

	public boolean configure(GPDocument document, Properties propert)
	{
		dialog = new GEPipelineConfigurationDialog(new JFrame(), document);
		setPropertiesInDialog(propert);
		dialog.saveInitialLevel(propert.getProperty(GEProperties.KEY_LEVEL));
        		
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
	 * Set the default properties of the GEPipelineController. If the default properties
	 * are not set again, then the values that are changed in the GEPipelineController, will
	 * remain stored. 
	 *
	 */
	public void setDefaultProperties()
	{
		properties.put(GEProperties.KEY_NAME, "Pipeline_"+ GEProperties.id_count++);
		properties.put(GEProperties.KEY_INPUT_TAPE, "void");
		properties.put(GEProperties.KEY_OUTPUT_TAPE, "void");
		properties.put(GEProperties.KEY_PARENT, "Toplevel");
		properties.put(GEProperties.KEY_TYPE, GEType.PIPELINE);
	}
	
	/**
	 * Get the default properties for a GEPipelineController.
	 * @return Properties the default properties of a GEPipelineController.
	 */
    public Properties getDefaultConfiguration()
    {
    	setDefaultProperties();
    	return properties;
    }
    
    
}