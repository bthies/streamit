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
 * The GEJoinerController class allows the user to administrate
 * the property values for a GEPipeline. The values that can be set 
 * are the name, input tape, output tape, weights, and parent.
 * 
 * @author jcarlos
 */
public class GEJoinerController extends GEStreamNodeController
{
	
	GEJoinerConfigurationDialog dialog  = null;

	/**
	  * Constructor. Set the default properties for the GEJoinerController. 
	  */
	 public GEJoinerController() 
	 {    	
		 super();
		 setDefaultProperties();
	 }

	 /**
	  * Return the type of the controller.
	  */
	 public String toString() 
	 {
		 return GEType.GETypeToString(GEType.JOINER);
	 }

	/**
	 * Display the dialog that allows the user to configure the values for the joiner. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */
	 public boolean configure(GPDocument document) 
	 {
		setDefaultProperties();
   
		dialog = new GEJoinerConfigurationDialog(new JFrame(), document);
		setPropertiesInDialog(this.properties);   
		dialog.setVisible(true);
		
		if (dialog.canceled()) return false;
		getPropertiesInDialog();
        
        return true;
	 }
	 
	/**
	 * Display the dialog that allows the user to configure the values for the joiner. 
	 * @return True if the properties clicked were accepted and valid, false otherwise.
	 */
	 public boolean configure(GPDocument document, Properties propert) 
	 {
		setDefaultProperties();
   
		dialog = new GEJoinerConfigurationDialog(new JFrame(), document);
		setPropertiesInDialog(propert);
		dialog.setVisible(true);
		dialog.saveInitialLevel(propert.getProperty(GEProperties.KEY_LEVEL));
		dialog.saveConnected(propert.getProperty(GEProperties.KEY_IS_CONNECTED));
		
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
		dialog.setImmediateParent(propert.getProperty(GEProperties.KEY_PARENT));
		dialog.setJoinerWeights(propert.getProperty(GEProperties.KEY_JOINER_WEIGHTS));
		dialog.setIndexInSJ(Integer.parseInt(propert.getProperty(GEProperties.KEY_INDEX_IN_SJ)));       
	}
	
	/**
	 * Get the properties in the dialog and place them in the controller's
	 * Properties field.
	 */
	public void getPropertiesInDialog()
	{
		properties.put(GEProperties.KEY_NAME, dialog.getName());
		properties.put(GEProperties.KEY_PARENT, dialog.getImmediateParent());		
		properties.put(GEProperties.KEY_JOINER_WEIGHTS, dialog.getJoinerWeights());
		properties.put(GEProperties.KEY_INDEX_IN_SJ, dialog.getIndexInSJ());
	}


	 /**
	  * Set the default properties of the GEJoinerController. If the default properties
	  * are not set again, then the values that are changed in the GEJoinerController, will
	  * remain stored. 
	  *
	  */
	 public void setDefaultProperties()
	 {
		properties.put(GEProperties.KEY_NAME, "StrJoiner"+ GEProperties.id_count++);
		properties.put(GEProperties.KEY_PARENT, "Toplevel");
		properties.put(GEProperties.KEY_TYPE, GEType.JOINER);
		properties.put(GEProperties.KEY_JOINER_WEIGHTS, "1");
		properties.put(GEProperties.KEY_INDEX_IN_SJ, "0");
	 }
	
	
	/**
	 * Get the default properties for a GEJoinerController.
	 * @return Properties the default properties of a GEJoinerController.
	 */
	 public Properties getDefaultConfiguration()
	 {
		 setDefaultProperties();
		 return properties;
	 }
}