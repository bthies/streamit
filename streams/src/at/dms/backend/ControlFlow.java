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
 * $Id: ControlFlow.java,v 1.3 2006-03-24 18:19:48 dimock Exp $
 */

package at.dms.backend;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.classfile.SwitchInstruction;
import at.dms.classfile.HandlerInfo;
import at.dms.backend.InstructionHandle;

/**
 * This is the entry point of the backend, this class constructs the
 * control flow graf and applies optimizations
 */
public class ControlFlow {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     * (Probably buggy description from cut and paste -- deleted references to wrong parameters)
     */
    public ControlFlow(MethodEnv env, InstructionHandle start, HandlerInfo[] handlers) {
        this.env = env;

        buildBasicBlocks(start, handlers);

        setMarked(false);
        bblocks[0].buildQuadruples(env);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Prints a trace of the control flow structure
     */
    public void trace() {
        new TraceControlFlow(bblocks, eblocks).run();
    }

    public void optimize() {
        // HIGH LEVEL OPTIMIZATION

        // LIVENESS ANALYSIS
        LivenessAnalysis    live = new LivenessAnalysis(bblocks, eblocks);

        live.run();
        trace();

        // CSE, COPY PROPAGATION, ....
        new DeadcodeElimination(bblocks, eblocks).run();
        new StackSchleduler(bblocks, eblocks).run();

        // REGISTER ALLOCATION
        RegisterAllocation  alloc = new RegisterAllocation(env, bblocks, eblocks, live);

        alloc.run();
        new TraceInferenceGraph(alloc.getInferenceGraph()).run();
    }

    public InstructionHandle getInstructions() {
        setMarked(false);

        CodeSequence    seq = new CodeSequence();

        // Goes all over the tree
        seq.plantBasicBlock(bblocks[0]);
        seq.close();

        // Generates exceptions
        for (int i = 0; i < eblocks.length; i++) {
            seq.plantBasicBlock(eblocks[i]);
        }

        resolveAccessors(seq.getCodeStart());
        for (int i = 0; i < eblocks.length; i++) {
            resolveAccessors(eblocks[i].getFirstInstruction());
        }

        return seq.getCodeStart();
    }

    // --------------------------------------------------------------------
    // STATIC UTILITIES
    // --------------------------------------------------------------------

    public static BasicBlock findBasicBlock(InstructionHandle handle) {
        for (int pos = 0;;pos++) {
            if (handle.getAccessor(pos) instanceof BasicBlock) {
                return (BasicBlock)handle.getAccessor(pos);
            }
        }
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    private void buildBasicBlocks(InstructionHandle start, HandlerInfo[] handlers) {
        Vector  bblocks = new Vector();
        Vector  body = new Vector();
        BasicBlock  current = null;
        boolean startBB = true;
        int     countBB = 0;

        for (InstructionHandle handle = start; handle != null; handle = handle.getNext()) {
            if (handle.isTarget() || startBB) {
                // new basic block
                if (current != null && !current.isMarked()) {
                    closeBasicBlock(current, body, bblocks);
                }
                current = new BasicBlock(countBB++);

                handle.addAccessor(current);
                body.addElement(handle);
                startBB = isEndOfBasicBlock(handle);
            } else if (isEndOfBasicBlock(handle)) {
                // end of basic block
                body.addElement(handle);
                closeBasicBlock(current, body, bblocks);

                startBB = true;
            } else {
                // add to basic block
                body.addElement(handle);
            }
        }
        if (body.size() != 0) {
            closeBasicBlock(current, body, bblocks);
        }

        clean(handlers, bblocks);
    }

    private void closeBasicBlock(BasicBlock current, Vector body, Vector bblocks) {
        current.setBody((InstructionHandle[])Utils.toArray(body, InstructionHandle.class));
        body.setSize(0);
        bblocks.addElement(current);
        current.setMarked(true);
    }

    private void clean(HandlerInfo[] handlers, Vector vbblocks) {
        bblocks = (BasicBlock[])Utils.toArray(vbblocks, BasicBlock.class);

        for (int i = 0; i < bblocks.length; i++ ) {
            bblocks[i].resolveJump();
        }

        // Handlers
        eblocks = new BasicBlock[handlers.length];
        for (int i  = 0; i < handlers.length; i++) {
            eblocks[i] = findBasicBlock((InstructionHandle)handlers[i].getHandler());
        }

        // Clean all
        for (InstructionHandle handle = bblocks[0].getFirstInstruction(); handle != null;) {
            InstructionHandle last = handle;

            handle.removeAccessors();
            handle = handle.getNext();
            last.setNext(null);
        }
    }

    private static boolean isEndOfBasicBlock(InstructionHandle handle) {
        return handle.isJump() || !handle.getInstruction().canComplete();
    }

    private void setMarked(boolean marked) {
        setMarked(bblocks, marked);
    }

    protected final static void setMarked(BasicBlock[] bblocks, boolean marked) {
        for (int i = 0; i < bblocks.length; i++) {
            bblocks[i].setMarked(marked);
        }
    }

    private void resolveAccessors(InstructionHandle handlex) {
        for (; handlex != null; handlex = handlex.getNext()) {
            if (handlex.isJump()) {
                handlex.setTarget(((BasicBlock)handlex.getJump().getTarget()).getFirstInstruction());
            } else if (handlex.getInstruction() instanceof SwitchInstruction) {
                SwitchInstruction   insn = (SwitchInstruction)handlex.getInstruction();

                for (int i = -1; i < insn.getSwitchCount(); i++) {
                    insn.setTarget(((BasicBlock)insn.getTarget(i)).getFirstInstruction(), i);
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private MethodEnv           env;
    private BasicBlock[]            bblocks; // List of basic blocks
    private BasicBlock[]            eblocks; // List of exception handler
}
