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
 * $Id: StackSchleduler.java,v 1.4 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import java.util.Hashtable;
import java.util.Stack;

/**
 * This class tries to find the best schleduling to minimize local vars
 */
public class StackSchleduler extends TreeWalker {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * (Probably buggy description from cut and paste -- deleted references to wrong parameters)
     */
    public StackSchleduler(BasicBlock[] bblocks, BasicBlock[] eblocks) {
        super(bblocks, eblocks);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Runs the deadcode algorithm
     */
    public void run() {
        traverse();
    }

    // --------------------------------------------------------------------
    // PROTECTED METHODS
    // --------------------------------------------------------------------

    /**
     * Processes the node
     *
     * @param   node        the node to be processed
     */
    protected void processNode(QNode node) {
        QOrigin[]   origins = node.getOrigins();
        int     pos = 0;

        for (int i = origins.length - 1; i >= 0; i--) {
            if (origins[i] == getTemp(pos) && !isInList(getTemp(pos), node.getLiveout())) {

                getNode(pos).useStack();
                node.setOrigin(new QStack(getTemp(pos).getType()), i);// !!! share
                pos += 1;
            } else if (!(origins[i] instanceof QLiteral)) {
                break;
            }
        }

        if (node.hasSideEffect()) {
            stack.setSize(0);
        }

        if (node.isStore()) {
            stack.push(node);
        }
    }

//     /**
//      * Processes the node
//      *
//      * @param   node        the node to be processed
//      *
//      protected void processNode(QNode node) {
//      QTemporary[]   uses = node.getUses();

//      for (int i = 0; i < uses.length; i++) {
//      QTemporary temp = uses[i];

//      if (!isInList(temp, node.getLiveout()) && defs.containsKey(temp)) {
//      QQuadruple defNode = (QQuadruple)defs.get(temp);

//      removeNode(defNode);
//      //node.setParameter(temp, defNode);
//      System.out.println("YOU'VE GOT IT " + defNode);
//      }
//      }

//      if (node.isStore()) {
//      QTemporary def = node.getDef();

//      defs.put(def, node);
//      }
//      }

     /**
     * Called when a branch is reached
     */
    protected void kill() {
        stack.setSize(0);
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    private QTemporary getTemp(int pos) {
        QQuadruple  quad = getNode(pos);

        return quad == null ? null : quad.getDef();
    }

    private QQuadruple getNode(int pos) {
        return stack.size() - 1 - pos >= 0 ?
            (QQuadruple)stack.elementAt(stack.size() - 1 - pos) :
            null;
    }

    private static boolean isInList(QTemporary temp, QTemporary[] list) {
        for (int i = 0; i < list.length; i++) {
            if (temp == list[i]) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------------
    // PRIVATE MEMBERS
    // --------------------------------------------------------------------

    private Stack<QNode>       stack = new Stack<QNode>();
    private Hashtable   defs = new Hashtable();
}
