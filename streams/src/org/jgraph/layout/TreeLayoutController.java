// This file is part of the Echidna project
// (C) 2002 Forschungszentrum Informatik (FZI) Karlsruhe
// Please visit our website at http://echidna.sf.net
package org.jgraph.layout;

import java.util.Properties;

/**
 * Responsible for administrating the TreeLayoutAlgorithm.<br>
 *<br>
 *<br>
 * @author Sven Luzar
 * @version 1.0 init
 */
public class TreeLayoutController implements LayoutController {

    private Properties properties;

    /**
     *
     */
    public TreeLayoutController() {
       properties = new Properties();
    }

    /**
     * Implementation.
     */
    public String toString() {
        return "Tree";
    }

    /**
     * Implementation.
     */
    public boolean isConfigurable() {
        return false;
    }

    /**
     * Implementation.
     */
    public void configure() {
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
        return new TreeLayoutAlgorithm();
    }
}