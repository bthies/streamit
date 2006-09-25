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
 * $Id: RegisterAllocation.java,v 1.4 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import java.util.Stack;

/**
 * This class allocate the the temporary to a minimal number of registers
 */
public class RegisterAllocation extends TreeWalker {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * (Probably buggy description from cut and paste -- deleted references to wrong parameters)
     */
    public RegisterAllocation(MethodEnv env, BasicBlock[] bblocks, BasicBlock[] eblocks, LivenessAnalysis live) {
        super(bblocks, eblocks);
        this.live = live;
        this.env = env;
        temps = live.getTemporaries();
        infer = new InferenceNode[temps.length];

        for (int i = 0; i < infer.length; i++) {
            infer[i] = new InferenceNode(temps[i]);
            temps[i].setPosition(i);
        }

        traverse();
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Runs the register allocation algorithm
     */
    public void run() {
        int     max = -1;
        Stack<InferenceNode>   stack = new Stack<InferenceNode>();
        boolean stop = false;

        // MARK PARAMETERS
        QNode       quads = getBasicBlock(0).getQuadruples()[0];
        QTemporary[]    temps = quads.getLivein();
        for (int i = 0; i < temps.length; i++) {
            temps[i].enforceColor();
            //System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>" + temps[i]);
        }

        all:
        do {
            stop = true;
            max++;
            sort(infer);
            // System.err.println(">>>>> max " + max);

            boolean change = true;
            while (change) {
                change = false;
                for (int i = 0; i < infer.length; i++) {
                    //System.err.println(">>>>> precolored " + infer[i].isPrecolored());
                    if (!infer[i].isRemoved() && !infer[i].isPrecolored()) {
                        stop = false;
                        if (infer[i].countInference() < max) {
                            change = true;
                            infer[i].remove();
                            stack.push(infer[i]);
                            //  System.err.println(">>>>> push " + infer[i].getTemporaries()[0]);
                        }
                    }
                }
            }
        } while (!stop);

        // COLORISE BACKWARD
        InferenceNode   node;

        for (int i = 0; i < infer.length; i++) {
            if (!infer[i].isRemoved()) {
                // precolored node
                infer[i].colorize();
            }
        }
        while (!stack.isEmpty()) {
            node = stack.pop();
            node.colorize();
        }

        // ASSIGN TO TEMPORARIES
        for (int i = 0; i < infer.length; i++) {
            infer[i].setTempsColor();
        }
    }

    /**
     * Returns the inference Graph
     */
    public InferenceNode[] getInferenceGraph() {
        return infer;
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    protected void processNode(QNode node) {
        QTemporary[]    liveins = node.getLivein();

        for (int i = 0; i < liveins.length; i++) {
            for (int j = 0; j < liveins.length; j++) {
                if (i != j) {
                    infer[liveins[i].getPosition()].linkTo(infer[liveins[j].getPosition()]);
                }
            }
        }
    }

    private void sort(QTemporary[] temps) {
        for (int i = temps.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (temps[j].getUseCount() < temps[j+1].getUseCount()) {
                    QTemporary      tmp = temps[j];

                    temps[j] = temps[j+1];
                    temps[j+1] = tmp;
                }
            }
        }
    }

    private void sort(InferenceNode[] temps) {
        for (int i = temps.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (temps[j].countInference() > temps[j+1].countInference()) {
                    InferenceNode       tmp = temps[j];

                    temps[j] = temps[j+1];
                    temps[j+1] = tmp;
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private int         locals;
    private MethodEnv       env;
    private LivenessAnalysis    live;
    private InferenceNode[] infer;
    private QTemporary[]        temps;
}
