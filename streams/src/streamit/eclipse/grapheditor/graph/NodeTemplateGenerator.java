/*
 * Created on Nov 26, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.util.Properties;

/**
 * Generate a StreamIt source code template for the node whose 
 * properties were passed as a parameter.
 * 
 * @author jcarlos
 *
 */
public class NodeTemplateGenerator 
{

	/**
	 * Generate a StreamIt source code template for the node whose 
 	 * properties were passed as a parameter.
	 * @param properties Properties that will be used to generate the code template.
	 * @return String representation of the template code to be generated.
	 */
	public static String createTemplateCode(Properties properties)
	{
		StringBuffer strBuff = new StringBuffer();
		System.out.println(GEProperties.KEY_TYPE);
		strBuff.append("\n"+ properties.getProperty(GEProperties.KEY_INPUT_TAPE))
				.append("->")
				.append(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE)+" ")
				.append(GEType.GETypeToString(properties.getProperty(GEProperties.KEY_TYPE))+" ")
				.append(properties.getProperty(GEProperties.KEY_NAME))
				.append("\n{\n\n}\n");

		return strBuff.toString();
	}
}
