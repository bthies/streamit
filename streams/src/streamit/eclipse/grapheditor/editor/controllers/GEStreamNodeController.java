/*
 * Created on Nov 13, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;

/**
 * @author jcarlos
 *
 */

abstract class GEStreamNodeController {
    

	/**
	 * Properties of the pipeline. 
	 */
	protected Properties properties;
    
	
	/**
	 * Constructor. Set the default properties for the GEPipeline. 
	 */
	public GEStreamNodeController() 
	{    	
		properties = new Properties();
	}

	/**
	 * Return the type of the controller.
	 */
	public abstract String toString();

	/**
	 * The properties of the pipeline are configurable.
	 */
	public boolean isConfigurable() 
	{
		return true;
	}

	/**
	 * Display the dialog that allows the user to configure the values for the pipeline.
	 */
	public abstract boolean configure(GPDocument document) ;
	/**
	 * Set the default properties of the GEPipelineController. If the default properties
	 * are not set again, then the values that are changed in the GEPipelineController, will
	 * remain stored. 
	 *
	 */
	
	public abstract void setDefaultProperties();	
	/**
	 * Get the configuration values for the pipeline. 
	 */
	
	public Properties getConfiguration() 
	{
		return properties;
	}
    
	public abstract Properties getDefaultConfiguration();

}






	
	/**
	 * Implement this method to specify the name of the GEStreamNode.
	 */
//	public String toString();

	/**
	 * Should return true only if the configure method will do something usefull.
	 */
//	public abstract boolean isConfigurable();

	/**
	 * Will be called when the user wants to configure the GEStreamNode.
	 * Its up to you to do the appropriate things.
	 */
//	public abstract void configure(GPDocument document);

	/**
	 * Returns the Configuration of the GEStreamNode as a Properties object.
	 */
//	public abstract Properties getConfiguration();

	/**
	 * Returns the Configuration of the GEStreamNode as a Properties object.
	 */
//	public abstract Properties getDefaultConfiguration();
 
	/**
	 * Must return an instance of the administrated LayoutAlgorithm.
	 */
	//public abstract LayoutAlgorithm getLayoutAlgorithm();


