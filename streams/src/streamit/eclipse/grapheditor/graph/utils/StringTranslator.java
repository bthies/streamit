/*
 * Created on Dec 3, 2003
 *
 */
package streamit.eclipse.grapheditor.graph.utils;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Provide basic functionality to translate strings to other representations 
 * as it is needed.
 * @author jcarlos
 *
 */
public class StringTranslator {

	/**
	 * Convert the weights w from a String with integers separated by
	 * spaces to an array of Integer objects.
	 * @param w String representation of the weights (separated by spaces)
	 * @return Integer[] array with the Integer values passed in w
	 */
	public static int[] weightsToInt(String w)
	{
		StringTokenizer str =  new StringTokenizer(w, "(,) ");
		int weights[] =  new int[str.countTokens()];
		int i = 0;
		while(str.hasMoreTokens())
		{
			weights[i] = Integer.parseInt(str.nextToken());
			i++;
		}
	
		return weights;
	}

	/**
	 * Check if the string entered as an argument 
	 * @param w String representation of the weights (separated by spaces)
	 * @return True if it is a valid representation of the weights, false otherwise.
	 */
	public static boolean checkWeightsValidity(String w)
	{
		StringTokenizer str =  new StringTokenizer(w);
		ArrayList weights = new ArrayList();
		while(str.hasMoreTokens())
		{
			try
			{
				Integer.valueOf(str.nextToken());	
			}
			catch (NumberFormatException nfe)
			{
				System.out.println("The weight values that were entered are not valid");
				return false;
			}
		}
		return true;
	}


	/**
	 * Return the string up to the last place where an underscore appears
	 * @param str String 
	 * @return String up to the last ocurrence of a string.
	 */
	public static String removeUnderscore(String str)
	{
		int indexUnderscore = str.lastIndexOf("_");
		if (indexUnderscore != -1)
		{
			return str.substring(0,indexUnderscore); 
		}
		else
		{
			return str;
		
		}	
	}


}
