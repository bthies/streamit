package streamit.eclipse.debugger.launching;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author kkuo
 */
public class LaunchingMessages {
	private static ResourceBundle fgResourceBundle = ResourceBundle.getBundle(LaunchingMessages.class.getName());

		public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return IStreamItLaunchingConstants.EXCLAMATION_CHAR + key + IStreamItLaunchingConstants.EXCLAMATION_CHAR;//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}