/*
 * Created on Nov 13, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

/**
 * The GEStreamNodeController class allows the user to administrate
 * the property values for a GEPipeline. The values that can be set 
 * are the name, input tape, output tape, arguments, and parent.
 * 
 * @author jcarlos
 */
abstract class GEStreamNodeController implements Controller {
    
	/**
	 * Properties of the GEStreamNode. 
	 */
	protected Properties properties;
    
	/**
	 * Constructor. Set the default properties for the GEStreamNodeController. 
	 */
	public GEStreamNodeController() 
	{    	
		properties = new Properties();
	}

	/**
	 * The properties of the GEStreamNode are configurable.
	 * @param boolean
	 */	
	public boolean isConfigurable() 
	{
		return true;
	}

	/**
	 * Get the configuration values for the GEStreamNode.
	 * @param Properties
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


