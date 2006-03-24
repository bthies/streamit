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
 * $Id: PeepholeOptimizer.java,v 1.3 2006-03-24 18:26:08 dimock Exp $
 */

package at.dms.backend;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Stack;
import at.dms.util.Utils;
import at.dms.classfile.SwitchInstruction;
import at.dms.classfile.HandlerInfo;
import at.dms.classfile.AccessorContainer;
import at.dms.classfile.AbstractInstructionAccessor;
import at.dms.classfile.NoArgInstruction;
import at.dms.backend.InstructionHandle;

/**
 * This class performs peephole optimizations
 */
public class PeepholeOptimizer extends TreeWalker {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * (Probably buggy description from cut and paste -- deleted references to wrong parameters)
     */
    public PeepholeOptimizer(BasicBlock[] bblocks, BasicBlock[] eblocks) {
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
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    protected void processNode(QNode node) {
    }
}
