/*
 * @(#)SpringEmbeddedLayoutController.java	1.0 01/20/03
 *
 * Copyright (C) 2003 Sven Luzar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */
package org.jgraph.layout;

import java.util.Properties;

/**
 * Responsible for administrating the Sugiyama Layout Algorithm.<br>
 *
 * @author <a href="mailto:Sven.Luzar@web.de">Sven Luzar</a>
 * @since 1.2.2
 * @version 1.0 init
 */
public class SpringEmbeddedLayoutController implements LayoutController {

    /** The properties for the algorithm
     *
     */
    private Properties properties;

    /** Creates an instance of this controller
     *
     */
    public SpringEmbeddedLayoutController() {
       properties = new Properties();
    }

    /**
     * Returns the name of this algorithm.
     */
    public String toString() {
        return "Spring Embedded";
    }

    /**
     * Returns false
     */
    public boolean isConfigurable() {
        return false;
    }

    /**
     * returns directly
     */
    public void configure() {
		return;
    }

    /**
     * returns null.
     */
    public Properties getConfiguration() {
        return properties;
    }

    /**
     * returns the layout Algorithm
     */
    public LayoutAlgorithm getLayoutAlgorithm() {
        return new SpringEmbeddedLayoutAlgorithm();
    }
}