/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: InferenceNode.java,v 1.3 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import java.util.Vector;

import at.dms.util.InconsistencyException;

/**
 * This class represents a temporary
 */
class InferenceNode {

    InferenceNode(QTemporary temp) {
        temps.addElement(temp);
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Gets the position of this node
     */
    public int getPosition() {
        return ((QTemporary)temps.elementAt(0)).getPosition();
    }

    /**
     * Returns the number of inference.
     */
    public int countInference() {
        int     count = 0;

        for (int i = 0; i < links.size(); i++) {
            count += links.elementAt(i).isRemoved() ? 0 : 1;
        }

        return count;
    }

    /**
     * Adds a link to an other node
     */
    public void linkTo(InferenceNode other) {
        if (!links.contains(other)) {
            links.addElement(other);
        }
    }

    /**
     * Returns the weight.
     */
    public int getWeight() {
        return 0;
    }

    /**
     * Returns the linked nodes.
     */
    public Vector<InferenceNode> getInferences() {
        return links;
    }

    /**
     * Returns the temporaries represented by this block.
     */
    public QTemporary[] getTemporaries() {
        return (QTemporary[])at.dms.util.Utils.toArray(temps, QTemporary.class);
    }

    // ----------------------------------------------------------------------
    // COALESCING
    // ----------------------------------------------------------------------

    /**
     * Coalesce two nodes together.
     */
    public void coalesceTo(InferenceNode other) {
        throw new InconsistencyException();
    }

    // ----------------------------------------------------------------------
    // SIMPLIFY
    // ----------------------------------------------------------------------

    /**
     * Removes this node.
     */
    public void remove() {
        this.removed = true;
    }

    /**
     * Returns true if this node has been removed from the graph
     */
    public boolean isRemoved() {
        return removed;
    }

    // ----------------------------------------------------------------------
    // COLORIFY
    // ----------------------------------------------------------------------

    /**
     * Returns the precolor
     */
    public int getSize() {
        return ((QTemporary)temps.elementAt(0)).getSize();
    }

    /**
     * Returns the precolor
     */
    public int getPrecolor() {
        return ((QTemporary)temps.elementAt(0)).getPrecolor();
    }

    /**
     * Returns if this node is precolored
     */
    public boolean isPrecolored() {
        return ((QTemporary)temps.elementAt(0)).getPrecolor() != QTemporary.UNINITIALIZED;
    }

    /**
     * Assigns the color to temporaries
     */
    public void setTempsColor() {
        for (int j = 0; j < temps.size(); j++) {
            QTemporary  temp = (QTemporary)temps.elementAt(j);

            temp.setRegister(getColor());
        }
    }

    /**
     * Sets the color of the node.
     */
    public void colorize() {
        if (isPrecolored()) {
            color = getPrecolor();
        } else {
            int i = -1;

            if (links.size() == 0) {
                removed = false;
                color = 0;
                return;
            }
            loop:
            while (true) {
                i += 1;

                for (int j = 0; j < links.size(); j++) {
                    InferenceNode   inf = links.elementAt(j);

                    if (!inf.isRemoved()) {
                        if (inf.getColor() == i || inf.getColor() + inf.getSize() -1 == i) {
                            continue loop;
                        }
                        if (getSize() == 2 && i + 1 == inf.getColor()) {
                            continue loop;
                        }
                    }
                }
                color = i;
                removed = false;
                return;
            }
        }
    }

    /**
     * Returns the color of the node
     */
    public int getColor() {
        return color;
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private Vector  temps = new Vector();
    private Vector<InferenceNode>  links = new Vector<InferenceNode>();
    private boolean removed;
    private int     color;
}
