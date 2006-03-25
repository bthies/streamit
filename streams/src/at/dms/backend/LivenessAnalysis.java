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
 * $Id: LivenessAnalysis.java,v 1.3 2006-03-25 00:07:58 dimock Exp $
 */

package at.dms.backend;

import java.util.Vector;
import java.util.Hashtable;

/**
 * This class compute the liveness of each temporaries
 */
public class LivenessAnalysis {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * BUGGY CUT AND PASTE COMMENT
     */
    public LivenessAnalysis(BasicBlock[] bblocks, BasicBlock[] eblocks) {
        this.bblocks = bblocks;
        this.eblocks = eblocks;
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    public void run() {
        ControlFlow.setMarked(bblocks, false);
        analyse(bblocks[0]);

        for (int i = 0; i < eblocks.length; i++) {
            analyse(eblocks[i]);
        }

        boolean changed = true;

        while (changed) {
            ControlFlow.setMarked(bblocks, false);
            changed = calculateLiveness(bblocks[0]);

            for (int i = 0; i < eblocks.length; i++) {
                changed |= calculateLiveness(eblocks[i]);
            }
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the temporaries defined in this method
     */
    public QTemporary[] getTemporaries() {
        return temps;
    }

    // --------------------------------------------------------------------
    // FIRST PASS
    // --------------------------------------------------------------------

    /**
     * Analyses a tree
     */
    private void analyse(BasicBlock block) {
        if (block.isMarked()) {
            return;
        }
        block.setMarked(true);

        QNode[] quads = block.getQuadruples();
        for (int i = 0; i < quads.length; i++) {
            analyseNode(block, i, quads[i]);
        }

        BasicBlock[]    successors = block.getSuccessors();
        QNode[][]       successorAccess = block.getSuccessorAccess();
        int         count = quads.length;

        for (int i = 0; i < successors.length; i++) {
            for (int j = 0; successorAccess[i] != null && j < successorAccess[i].length; j++) {
                analyseNode(block, count++, successorAccess[i][j]);
            }
            analyse(successors[i]);
        }

        temps = (QTemporary[])at.dms.util.Utils.toArray(vect, QTemporary.class);
    }

    /**
     * Processes a node
     */
    private void analyseNode(BasicBlock block, int pos, QNode node) {
        QTemporary      def = node.getDef();
        QTemporary[]    uses = node.getUses();

        if (def != null) {
            //System.err.println("DEF: " + def + " AT " + block + " / " + pos);
            def.def(block, pos);
            if (!hash.containsKey(def)) {
                hash.put(def, def);
                vect.addElement(def);
            }
        }

        if (uses != null) {
            for(int i = 0 ; i < uses.length; i++) {
                //System.err.println("USE: " + uses[i] + " AT " + block + " / " + pos);
                uses[i].use(block, pos);
                if (!hash.containsKey(uses[i])) {
                    hash.put(uses[i], uses[i]);
                    vect.addElement(uses[i]);
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // LIVENESS COMPUTIATION
    // --------------------------------------------------------------------

    /**
     * Analyses a tree
     */
    private boolean calculateLiveness(BasicBlock block) {
        if (block.isMarked()) {
            return false;
        }
        block.setMarked(true);

        boolean changed = false;
        QNode[] quads = block.getQuadruples();
        //for (int i = 0 ; i < quads.length; i++) {
        for (int i = quads.length - 1; i >= 0; i--) {
            changed |= calculateLiveness(block, i, -1, quads[i]);
        }

        BasicBlock[]    successors = block.getSuccessors();
        QNode[][]       successorAccess = block.getSuccessorAccess();
        int         count = quads.length;

        for (int i = 0; i < successors.length; i++) {
            for (int j = 0; successorAccess[i] != null && j < successorAccess[i].length; j++) {
                changed |= calculateLiveness(block, i, j, successorAccess[i][j]);
            }
            changed |= calculateLiveness(successors[i]);
        }

        return changed;
    }

    /**
     * Processes a node
     */
    private boolean calculateLiveness(BasicBlock block, int pos, int branch, QNode node) {
        QTemporary      def = node.getDef();
        QTemporary[]    uses = node.getUses();
        QTemporary[]    livein = node.getLivein();
        QTemporary[]    liveout = node.getLiveout();

        node.setLivein(livein(uses, liveout, def));
        boolean     changed = !equals(node.getLivein(), livein);
        node.setLiveout(liveout(block, pos, branch));
        changed |= !equals(liveout, node.getLiveout());

        liveout = node.getLiveout(); // remove
        livein = node.getLivein();

        /*
          System.err.print("node " + block + "[" + pos + "]" + " in = ");
          for (int i = 0; i < livein.length; i++) {
          System.err.print(livein[i] + " ");
          }
          System.err.print(", out = ");
          for (int i = 0; i < liveout.length; i++) {
          System.err.print(liveout[i] + " ");
          }
          System.err.println();
        */

        return changed;
    }

    private static final boolean equals(Object[] o1, Object[] o2) {
        if (o1.length != o2.length) {
            return false;
        }

        for (int i = 0; i < o1.length; i++) {
            if (o1[i] != o2[i]) {
                return false;
            }
        }

        return true;
    }

    private QTemporary[] liveout(BasicBlock block, int pos, int branch) {
        // UNION OF SUCCESSORS
        QNode[] nodes = block.getSuccessorNodes(pos, branch);

        if (nodes.length == 1) {
            return nodes[0].getLivein();
        } else {
            Vector  vect = new Vector();

            for (int i = 0; i < nodes.length; i++) {
                QTemporary[]    tt = nodes[i].getLivein();
                for (int j = 0; j < tt.length; j++) {
                    if (!vect.contains(tt[j])) {
                        vect.addElement(tt[j]);
                    }
                }
            }

            return (QTemporary[])at.dms.util.Utils.toArray(vect, QTemporary.class);
        }
    }

    private QTemporary[] livein(QTemporary[] uses, QTemporary[] outs, QTemporary def) {
        // USES U (OUTS - DEF)
        int     count = uses.length;

        for (int i = 0; i < outs.length; i++) {
            stop:
            if (outs[i] != def) {
                for (int j = 0; j < uses.length; j++) {
                    if (outs[i] == uses[j]) {
                        break stop;
                    }
                }
                count++;
            }
        }

        if (count == uses.length) {
            return uses;
        } else {
            QTemporary[]    temp = new QTemporary[count];

            count = uses.length;
            for (int j = 0; j < uses.length; j++) {
                temp[j] = uses[j];
            }
            for (int i = 0; i < outs.length; i++) {
                stop:
                if (outs[i] != def) {
                    for (int j = 0; j < uses.length; j++) {
                        if (outs[i] == uses[j]) {
                            break stop;
                        }
                    }
                    temp[count++] = outs[i];
                }
            }

            return temp;
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private Hashtable           hash = new Hashtable(); // collects temporaries
    private Vector          vect = new Vector();
    private QTemporary[]            temps;   // List of temporaries
    private BasicBlock[]            bblocks; // List of basic blocks
    private BasicBlock[]            eblocks; // List of exception handler
}
