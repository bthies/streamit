/*
 * Created on Jan 27, 2004
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.Properties;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface Controller {

	/**
	 * Display the dialog that allows the user to configure the values for the pipeline.
	 */
	public boolean configure(GPDocument document, Properties prop);
	public boolean configure(GPDocument document);
	
	/**
	 * Set the default properties of the GEPipelineController. If the default properties
	 * are not set again, then the values that are changed in the GEPipelineController, will
	 * remain stored. 
	 *
	 */
	public void setDefaultProperties();
	
	/**
	 * Return the type of the controller.
	 */
	public String toString();
		
	public Properties getDefaultConfiguration();
	
	/**
	 * Get the configuration values for the pipeline. 
	 */
	public Properties getConfiguration();
}
