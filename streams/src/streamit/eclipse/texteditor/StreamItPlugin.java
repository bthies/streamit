/*******************************************************************************
 * StreamIt Plugin adapted from Example Readme Tool
 * modifier - Kimberly Kuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package texteditor;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.lang.StringBuffer;

/**
 * This is the top-level class of the StreamIt plugin tool.
 *
 * @see AbstractUIPlugin for additional information on UI plugins
 */
public class StreamItPlugin extends AbstractUIPlugin {

    // Default instance of the receiver
    private static StreamItPlugin inst;

    /**
     * Creates the StreamIt plugin and caches its default instance
     *
     * @param descriptor  the plugin descriptor which the receiver is made from
     */
    public StreamItPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		if (inst == null) inst = this;
    }

    /**
     * Gets the plugin singleton.
     *
     * @return the default StreamItPlugin instance
     */
    static public StreamItPlugin getDefault() {
		return inst;
    }
    
    /** 
     * Sets default preference values. These values will be used
     * until some preferences are actually set using Preference dialog.
     */
    protected void initializeDefaultPreferences(IPreferenceStore store) {
		// These settings will show up when Preference dialog
		// opens up for the first time.
		StringBuffer temp;
		String toUse;
		String[] toIter;

		for (int i = 0; i < 5; i++) {
			temp = new StringBuffer();
			switch (i) {
				case 0: 
					toUse = IStreamItConstants.PRE_KEYWORD;
					toIter = StreamItCodeScanner.getFGKeywords(); break;
				case 1:
					toUse = IStreamItConstants.PRE_STR_KEYWORD;
					toIter = IStreamItConstants.FG_STR_KEYWORD_DEFAULTS; break;
				case 2:
					toUse = IStreamItConstants.PRE_TYPE;
					toIter = StreamItCodeScanner.getFGTypes(); break;
				case 3: 
					toUse = IStreamItConstants.PRE_CONSTANT;
					toIter = StreamItCodeScanner.getFGConstants(); break;
				default: 
					toUse = IStreamItConstants.PRE_STR_COMMON;
					toIter = IStreamItConstants.FG_STR_COMMON_DEFAULTS; break;
			}
			
			for (int j = 0; j < toIter.length; j++) {
				temp.append(toIter[j]);
				if (j != toIter.length - 1)
					temp.append("\n");
    		}
			if (toUse.equals(IStreamItConstants.PRE_TYPE)) {
				toIter = IStreamItConstants.FG_STR_TYPE_DEFAULTS;
				temp.append("\n");
				for (int j = 0; j < toIter.length; j++) {
					temp.append(toIter[j]);
					if (j != toIter.length - 1)
						temp.append("\n");
				}	
			}    		
    		
			store.setDefault(toUse, temp.toString());
		}	
    }
}