package streamit.eclipse.debugger.wizards;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author kkuo
 */
public class NewWizardMessages {

	private static ResourceBundle fgResourceBundle = ResourceBundle.getBundle(NewWizardMessages.class.getName());

	private NewWizardMessages() {
	}
		
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return IStreamItWizardsConstants.EXCLAMATION_CHAR + key + IStreamItWizardsConstants.EXCLAMATION_CHAR;
		}
	}
	
	/**
	 * Gets a string from the resource bundle and formats it with the argument
	 * 
	 * @param key	the string used to get the bundle value, must not be null
	 */
	public static String getFormattedString(String key, Object arg) {
		return MessageFormat.format(getString(key), new Object[] { arg });
	}

	/**
	 * Gets a string from the resource bundle and formats it with arguments
	 */	
	public static String getFormattedString(String key, Object[] args) {
		return MessageFormat.format(getString(key), args);
	}

}
