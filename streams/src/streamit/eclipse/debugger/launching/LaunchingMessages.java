package streamit.eclipse.debugger.launching;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author kkuo
 */
public class LaunchingMessages {

	private static final String RESOURCE_BUNDLE= "streamit.eclipse.debugger.launching.LaunchingMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

		public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}