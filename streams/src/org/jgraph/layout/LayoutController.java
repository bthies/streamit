/*
 * @(#)LayoutController.java	1.0 01/20/03
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
 * This class administrates a LayoutAlgorithm.<br>
 * Means it gives it a name, is responsible for the configuration<br>
 * and creates a LayoutAlgorithm object on demand.<br>
 *<br>
 *<br>
 * @author <a href="mailto:Sven.Luzar@web.de">Sven Luzar</a>
 * @since 1.2.2
 * @version 1.0 init
 */
public interface LayoutController {

    /**
     * Implement this method to specify the name of your LayoutAlgorithm.
     */
    public abstract String toString();

    /**
     * Should return true only if the configure method will do something usefull.
     */
    public abstract boolean isConfigurable();

    /**
     * Will be called when the user wants to configure the LayoutAlgorithm.
     * Its up to you to do the appropriate things.
     */
    public abstract void configure();

    /**
     * Returns the Configuration of the LayoutAlgorithm as a Properties object.
     */
    public abstract Properties getConfiguration();

    /**
     * Must return an instance of the administrated LayoutAlgorithm.
     */
    public abstract LayoutAlgorithm getLayoutAlgorithm();

}
