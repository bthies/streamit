package streamit.eclipse.debugger.actions;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author kkuo
 */ 
public class ActionMessages {
	private static ResourceBundle fgResourceBundle = ResourceBundle.getBundle(ActionMessages.class.getName());

	private ActionMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return IStreamItActionConstants.MESSAGE_ERROR + key + IStreamItActionConstants.MESSAGE_ERROR;//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}