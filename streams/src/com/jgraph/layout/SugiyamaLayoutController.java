// This file is part of the Echidna project
// (C) 2002 Forschungszentrum Informatik (FZI) Karlsruhe
// Please visit our website at http://echidna.sf.net
package com.jgraph.layout;

import java.util.Properties;
import javax.swing.JFrame;

/**
 * Responsible for administrating the SugiyamaLayoutAlgorithm.<br>
 *<br>
 *<br>
 * @author Sven Luzar
 * @version 1.0 init
 */
public class SugiyamaLayoutController implements LayoutController {

    public static final String KEY_HORIZONTAL_SPACING = "HorizontalSpacing";

    public static final String KEY_VERTICAL_SPACING   = "VerticalSpacing";

    private Properties properties;

    /**
     *
     */
    public SugiyamaLayoutController() {
       properties = new Properties();
       properties.put(KEY_HORIZONTAL_SPACING, "250");
       properties.put(KEY_VERTICAL_SPACING,   "150");
    }

    /**
     * Implementation.
     */
    public String toString() {
        return "Sugiyama";
    }

    /**
     * Implementation.
     */
    public boolean isConfigurable() {
        return true;
    }

    /**
     * Implementation.
     */
    public void configure() {
		/*
        SugiyamaLayoutConfigurationDialog dialog = new SugiyamaLayoutConfigurationDialog(new JFrame());
        dialog.setIndention(properties.getProperty(         KEY_HORIZONTAL_SPACING));
        dialog.setVerticalSpacing(properties.getProperty(   KEY_VERTICAL_SPACING));

        dialog.setVisible(true);
        if (dialog.canceled()) return;

        properties.put(KEY_HORIZONTAL_SPACING,  dialog.getIndention());
        properties.put(KEY_VERTICAL_SPACING,    dialog.getVerticalSpacing());
        */
        
    }

    /**
     * Implementation.
     */
    public Properties getConfiguration() {
        return properties;
    }

    /**
     * Implementation.
     */
    public LayoutAlgorithm getLayoutAlgorithm() {
        return new SugiyamaLayoutAlgorithm();
    }
}