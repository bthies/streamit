package streamit.eclipse.debugger.texteditor;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class which helps with managing messages.
 * 
 * @author kkuo
 */
public class StreamItEditorMessages {

    private static final String RESOURCE_BUNDLE =
	"streamit.eclipse.debugger.texteditor.StreamItEditorMessages"; //$NON-NLS-1$
    
    private static ResourceBundle fgResourceBundle = 
	ResourceBundle.getBundle(RESOURCE_BUNDLE);
    
    private StreamItEditorMessages() {
	// prevent instantiation of class
    }
    
    /**
     * Returns the formatted message for the given key in
     * the resource bundle. 
     *
     * @param key the resource name
     * @param args the message arguments
     * @return the string
     */	
    public static String format(String key, Object[] args) {
	return MessageFormat.format(getString(key),args);
    }
    
    /**
     * Returns the resource object with the given key in
     * the resource bundle. If there isn't any value under
     * the given key, the key is returned, surrounded by '!'s.
     *
     * @param key the resource name
     * @return the string
     */	
    public static String getString(String key) {
	try {
	    return fgResourceBundle.getString(key);
	} catch (MissingResourceException e) {
	    return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
	}
    }
    
    public static ResourceBundle getResourceBundle() {
	return fgResourceBundle;
    }
}
