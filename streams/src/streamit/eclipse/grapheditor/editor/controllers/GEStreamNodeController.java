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

abstract class GEStreamNodeController implements Controller {
    

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
	 * The properties of the pipeline are configurable.
	 */
	
	public boolean isConfigurable() 
	{
		return true;
	}


	/**
	 * Get the configuration values for the pipeline. 
	 */
	
	public Properties getConfiguration() 
	{
		return properties;
	}
    


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


