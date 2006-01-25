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
 * $Id: Patterns.java,v 1.2 2006-01-25 17:02:45 thies Exp $
 */

package at.dms.optimize;

import at.dms.classfile.JumpInstruction;
import at.dms.classfile.Instruction;
import at.dms.classfile.NoArgInstruction;
import at.dms.classfile.LocalVarInstruction;
import at.dms.classfile.PushLiteralInstruction;

import at.dms.util.InconsistencyException;

/**
 * This class is the entry point for the peephole byte code optimizer
 */
public class Patterns implements at.dms.classfile.Constants {

    /**
     * Optimizes the byte code for a single method.
     */
    public static boolean optimize(InstructionHandle handle) {
        int opcode = handle.getOpcode();

        switch (opcode) {
        case opc_ifeq:
        case opc_ifne:
        case opc_iflt:
        case opc_ifge:
        case opc_ifgt:
        case opc_ifle:
        case opc_ifnull:
        case opc_ifnonnull:
        case opc_if_icmpeq:
        case opc_if_icmpne:
        case opc_if_icmplt:
        case opc_if_icmpge:
        case opc_if_icmpgt:
        case opc_if_icmple:
        case opc_if_acmpeq:
        case opc_if_acmpne:
        case opc_goto_w:
        case opc_goto:
        case opc_jsr:
        case opc_jsr_w:
            // Jump instruction
            return optimizeJump(handle, opcode);
        case opc_dastore:
        case opc_lastore:
        case opc_aastore:
        case opc_bastore:
        case opc_castore:
        case opc_dcmpg:
        case opc_dcmpl:
        case opc_fastore:
        case opc_iastore:
        case opc_lcmp:
        case opc_sastore:
        case opc_dadd:
        case opc_ddiv:
        case opc_dmul:
        case opc_drem:
        case opc_dreturn:
        case opc_dsub:
        case opc_ladd:
        case opc_land:
        case opc_ldiv:
        case opc_lmul:
        case opc_lor:
        case opc_lrem:
        case opc_lreturn:
        case opc_lsub:
        case opc_lxor:
        case opc_pop2:
        case opc_aaload:
        case opc_areturn:
        case opc_athrow:
        case opc_baload:
        case opc_caload:
        case opc_d2f:
        case opc_d2i:
        case opc_fadd:
        case opc_faload:
        case opc_fcmpg:
        case opc_fcmpl:
        case opc_fdiv:
        case opc_fmul:
        case opc_frem:
        case opc_freturn:
        case opc_fsub:
        case opc_iadd:
        case opc_iaload:
        case opc_iand:
        case opc_idiv:
        case opc_imul:
        case opc_isub:
        case opc_irem:
        case opc_ishl:
        case opc_ishr:
        case opc_iushr:
        case opc_ior:
        case opc_ixor:
        case opc_l2f:
        case opc_l2i:
        case opc_lshl:
        case opc_lshr:
        case opc_lushr:
        case opc_monitorenter:
        case opc_monitorexit:
        case opc_pop:
        case opc_saload:
        case opc_ireturn:
        case opc_nop:
        case opc_arraylength:
        case opc_d2l:
        case opc_daload:
        case opc_dneg:
        case opc_f2i:
        case opc_fneg:
        case opc_i2b:
        case opc_i2c:
        case opc_i2f:
        case opc_i2s:
        case opc_ineg:
        case opc_l2d:
        case opc_laload:
        case opc_lneg:
        case opc_return:
        case opc_swap:
        case opc_aconst_null:
        case opc_dup:
        case opc_dup_x1:
        case opc_dup_x2:
        case opc_f2d:
        case opc_f2l:
        case opc_i2d:
        case opc_i2l:
        case opc_dup2:
        case opc_dup2_x1:
        case opc_dup2_x2:
            // NoArgInstruction
            return optimizeNoArgInstruction(handle, opcode);
        case opc_invokevirtual:
        case opc_invokespecial:
        case opc_invokestatic:
            // MethodRefInstruction
            // return optimizeMethodRefInstruction(handle);
            break;
        case opc_getstatic:
        case opc_putstatic:
        case opc_getfield:
        case opc_putfield:
            // FieldRefInstruction
            return optimizeFieldRefInstruction(handle, opcode);
        case opc_aload:
        case opc_aload_0:
        case opc_aload_1:
        case opc_aload_2:
        case opc_aload_3:
        case opc_astore:
        case opc_astore_0:
        case opc_astore_1:
        case opc_astore_2:
        case opc_astore_3:
        case opc_dload:
        case opc_dload_0:
        case opc_dload_1:
        case opc_dload_2:
        case opc_dload_3:
        case opc_dstore:
        case opc_dstore_0:
        case opc_dstore_1:
        case opc_dstore_2:
        case opc_dstore_3:
        case opc_fload:
        case opc_fload_0:
        case opc_fload_1:
        case opc_fload_2:
        case opc_fload_3:
        case opc_fstore:
        case opc_fstore_0:
        case opc_fstore_1:
        case opc_fstore_2:
        case opc_fstore_3:
        case opc_iload:
        case opc_iload_0:
        case opc_iload_1:
        case opc_iload_2:
        case opc_iload_3:
        case opc_istore:
        case opc_istore_0:
        case opc_istore_1:
        case opc_istore_2:
        case opc_istore_3:
        case opc_lload:
        case opc_lload_0:
        case opc_lload_1:
        case opc_lload_2:
        case opc_lload_3:
        case opc_lstore:
        case opc_lstore_0:
        case opc_lstore_1:
        case opc_lstore_2:
        case opc_lstore_3:
        case opc_ret:
            // LocalVarInstruction
            return optimizeLocalVarInstruction(handle, opcode);
        case opc_iconst_m1:
        case opc_iconst_0:
        case opc_iconst_1:
        case opc_iconst_2:
        case opc_iconst_3:
        case opc_iconst_4:
        case opc_iconst_5:
        case opc_fconst_0:
        case opc_fconst_1:
        case opc_fconst_2:
        case opc_sipush:
        case opc_bipush:
        case opc_ldc:
        case opc_ldc_w:
        case opc_lconst_0:
        case opc_lconst_1:
        case opc_dconst_0:
        case opc_dconst_1:
        case opc_ldc2_w:
            // PushLiteralInstruction
            return optimizePushLiteralInstruction(handle, opcode);
        }

        handle.set();

        return false;
    }

    // --------------------------------------------------------------------
    // JUMP INSTRUCTION
    // --------------------------------------------------------------------

    /**
     * Optimizes the byte code for a single method.
     */
    public static boolean optimizeJump(InstructionHandle handle, int opcode) {
        JumpInstruction current = (JumpInstruction)handle.getInstruction();

        /*
          if (optimize5(handle, opcode)) {
          return true;
          }
        */

        // Jump to next instruction
        if (current.getTarget() == handle.getNext()) {
            switch (opcode) {
            case opc_if_icmpeq:
            case opc_if_icmpne:
            case opc_if_icmplt:
            case opc_if_icmpgt:
            case opc_if_icmpge:
            case opc_if_icmple:
            case opc_if_acmpeq:
            case opc_if_acmpne:
                handle.replaceBy(new NoArgInstruction(opc_pop2));
                return true;
            case opc_ifeq:
            case opc_ifne:
            case opc_iflt:
            case opc_ifge:
            case opc_ifgt:
            case opc_ifle:
            case opc_ifnull:
            case opc_ifnonnull:
                handle.replaceBy(new NoArgInstruction(opc_pop));
                return true;

            case opc_goto:
                handle.destroy();
                return true;
            }
        }

        // Jump to goto instruction
        if (handle.getTarget().getOpcode() == opc_goto) {
            // L:   goto L
            // cannot be optimized
            if (handle.getTarget() == handle.getTarget().getTarget()) {
                handle.set();
                return false;
            } else {
                //  ifeq    L1
                //  ...
                // L1:  goto    L2
                //  ...
                // L2:
                // ===>
                //  ifeq    L2
                //  ...
                //  goto    L2
                //  ...
                // L2:
                handle.getTarget().removeAccessor(current);
                handle.setTarget(handle.getTarget().getTarget());
                handle.getTarget().addAccessor(current);

                handle.set();

                return true;
            }
        }

        // Goto ifXXX instruction
        if (handle.getOpcode() == opc_goto) {
            //  goto    L1
            //  ...
            // L1:  ifeq    L2
            //  ...
            // L2:
            // ===>
            //  ifeq    L2
            //  goto    L3
            //  ...
            // L1:  ifeq    L2
            // L3:  ...
            //  ...
            // L2:
            switch (handle.getTarget().getOpcode()) {
            case opc_ifeq:
            case opc_ifne:
            case opc_iflt:
            case opc_ifge:
            case opc_ifgt:
            case opc_ifle:
            case opc_ifnull:
            case opc_ifnonnull:
            case opc_if_icmpeq:
            case opc_if_icmpne:
            case opc_if_icmplt:
            case opc_if_icmpge:
            case opc_if_icmpgt:
            case opc_if_icmple:
            case opc_if_acmpeq:
            case opc_if_acmpne:
                handle.replaceBy(new Instruction[] {
                    new JumpInstruction(handle.getTarget().getOpcode(), handle.getTarget().getJump().getTarget()),
                    new JumpInstruction(opc_goto, handle.getTarget().getNext())
                });

                handle.set();

                return true;
            default:
            }
        }

        // Jump over a goto
        if (handle.getNext() != null && handle.getNext().isJump() && handle.getTarget() == handle.getNext().getNext()) {
            InstructionHandle   next = handle.getNext();
            //  ifeq X             ifne Z
            //  goto Z             X:
            //X:...           ===>
            if (next.getJump().getOpcode() == opc_goto && !next.isTarget()) {
                switch (opcode) {
                case opc_if_icmpeq:
                case opc_if_icmpne:
                case opc_if_icmplt:
                case opc_if_icmple:
                case opc_if_icmpgt:
                case opc_if_icmpge:
                case opc_ifeq:
                case opc_ifne:
                case opc_iflt:
                case opc_ifgt:
                case opc_ifle:
                case opc_ifge:
                case opc_ifnull:
                case opc_ifnonnull:
                    handle.replaceBy(new JumpInstruction(getReverseOpcode(opcode), next.getJump().getTarget()));
                    next.destroy();

                    return true;
                }
            }
        }

        // Goto to return instruction
        if (opcode == opc_goto && handle.getTarget().getInstruction() instanceof NoArgInstruction) {
            NoArgInstruction    target = (NoArgInstruction)handle.getTarget().getInstruction();

            if (!target.canComplete()) {
                handle.replaceBy(new NoArgInstruction(target.getOpcode()));

                return true;
            }
        }


        handle.set();

        return false;
    }

    // --------------------------------------------------------------------
    // NO ARG INSTRUCTION
    // --------------------------------------------------------------------

    /**
     * Optimizes the byte code for a single method.
     */
    public static boolean optimizeNoArgInstruction(InstructionHandle handle, int opcode) {
        if (opcode == opc_nop) {
            if (handle.remove()) {
                return true;
            } else {
                handle.set();

                return false;
            }
        }

        handle.set();

        return false;
    }

    // --------------------------------------------------------------------
    // NO ARG INSTRUCTION
    // --------------------------------------------------------------------

    /**
     * Optimizes the byte code for a single method.
     */
    public static boolean optimizePushLiteralInstruction(InstructionHandle handle, int opcode) {
        PushLiteralInstruction  current = (PushLiteralInstruction)handle.getInstruction();

        //    comparison to 0 or 1
        if (handle.getNext().isJump()) {
            InstructionHandle next = handle.getNext();

            if (opcode == opc_iconst_0) {
                switch (next.getOpcode()) {
                case opc_if_icmpeq:
                    // Replace:           by:
                    //     const_0            ifzz X
                    //                        goto Lnew
                    //     if_icmpzz X        if_icmpzz X
                    //                  Lnew:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_ifeq, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_if_icmpne:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_ifne, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_if_icmplt:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_iflt, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_if_icmpgt:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_ifgt, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_if_icmpge:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_ifge, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_if_icmple:
                    handle.replaceBy(new Instruction[] {
                        new JumpInstruction(opc_ifle, next.getTarget()),
                        new JumpInstruction(opc_goto, next.getNext())
                    });
                    return true;
                case opc_ifeq:
                    // Replace:           by:
                    //     const_0            goto X
                    //     ifeq X             ifeq X
                    handle.replaceBy(new JumpInstruction(opc_goto, next.getTarget()));
                    return true;
                case opc_ifne:
                    // Replace:           by:
                    //     const_0            goto Lnew
                    //     ifne X             ifne X
                    //                  Lnew:
                    handle.replaceBy(new JumpInstruction(opc_goto, next.getNext()));
                    return true;
                }
            } else if (((PushLiteralInstruction)handle.getInstruction()).getLiteral() instanceof Integer) {
                if (next.getOpcode() == opc_ifne) {
                    // Replace:           by:
                    //     const n            goto X
                    //     ifne X             ifne X
                    handle.replaceBy(new JumpInstruction(opc_goto, next.getTarget()));
                    return true;
                } else if (next.getOpcode() == opc_ifeq) {
                    // Replace:           by:
                    //     const n            goto Lnew
                    //     ifeq X             ifeq X
                    //                  Lnew:
                    handle.replaceBy(new JumpInstruction(opc_goto, next.getNext()));
                    return true;
                }
            } else if (opcode == opc_aconst_null && !handle.getNext().isTarget()) {
                if (next.getOpcode() == opc_if_acmpeq) {
                    handle.replaceBy(new JumpInstruction(opc_ifnull, next.getTarget()));
                    next.destroy();

                    return true;
                } else if (next.getOpcode() == opc_if_acmpne) {
                    handle.replaceBy(new JumpInstruction(opc_ifnonnull, next.getTarget()));
                    next.destroy();

                    return true;
                } else if (next.getOpcode() == opc_ifnull) {
                    handle.replaceBy(new JumpInstruction(opc_goto, next.getTarget()));
                    next.destroy();

                    return true;
                } else if (next.getOpcode() == opc_ifnonnull) {
                    handle.destroy();
                    next.destroy();

                    return true;
                }
            } else if (opcode == opc_ldc) {
                // USE A STACK TYPE AND RANGE
            }
        } else if (!handle.getNext().isTarget() && handle.getNext().getOpcode() == opc_pop) {
            InstructionHandle next = handle.getNext();

            handle.destroy();
            next.destroy();

            return true;
        } else if (!handle.getNext().isTarget() && handle.getNext().getOpcode() == opc_pop2) {
            InstructionHandle next = handle.getNext();

            switch (opcode) {
            case opc_iconst_m1:
            case opc_iconst_0:
            case opc_iconst_1:
            case opc_iconst_2:
            case opc_iconst_3:
            case opc_iconst_4:
            case opc_iconst_5:
            case opc_fconst_0:
            case opc_fconst_1:
            case opc_fconst_2:
            case opc_sipush:
            case opc_bipush:
            case opc_ldc:
            case opc_ldc_w:
                // stack 1;
                handle.replaceBy(new NoArgInstruction(opc_pop));
                next.destroy();

                return true;
            case opc_lconst_0:
            case opc_lconst_1:
            case opc_dconst_0:
            case opc_dconst_1:
            case opc_ldc2_w:
                // stack 2;

                handle.destroy();
                next.destroy();

                return true;

            default:
                throw new InconsistencyException("invalid opcode: " + opcode);
            }
        } else if (opcode == opc_iconst_m1
                   && (!handle.getNext().isTarget() && handle.getNext().getOpcode() == opc_ixor)
                   && !handle.getNext().getNext().isTarget()) {
            if (handle.getNext().getNext().getOpcode() == opc_ifeq) {
                handle.replaceBy(new JumpInstruction(opc_ifne, handle.getNext().getNext().getTarget()));
                handle.getNext().getNext().destroy();
                handle.getNext().destroy();

                return true;
            } else if (handle.getNext().getNext().getOpcode() == opc_ifne) {
                handle.replaceBy(new JumpInstruction(opc_ifeq, handle.getNext().getNext().getTarget()));
                handle.getNext().getNext().destroy();
                handle.getNext().destroy();

                return true;
            }
        }/* else if (opcode == handle.getNext().getOpcode()) {
         // MAKE CODE SLOWER !!!
         // dup
         if (((PushLiteralInstruction)handle.getInstruction()).getLiteral().equals(((PushLiteralInstruction)handle.getNext().getInstruction()).getLiteral()) && !handle.getNext().isTarget()) {
         handle.getNext().replaceBy(handle.getInstruction().getStack() == 2 ?
         new NoArgInstruction(opc_dup2) :
         new NoArgInstruction(opc_dup));
         handle.set();

         return true;
         }
         }*/

        handle.set();

        return false;
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    private static int getReverseOpcode(int opcode) {
        switch (opcode) {
        case opc_if_acmpeq:
            return opc_if_acmpne;
        case opc_if_acmpne:
            return opc_if_acmpeq;
        case opc_if_icmpeq:
            return opc_if_icmpne;
        case opc_if_icmpne:
            return opc_if_icmpeq;
        case opc_if_icmplt:
            return opc_if_icmpge;
        case opc_if_icmple:
            return opc_if_icmpgt;
        case opc_if_icmpgt:
            return opc_if_icmple;
        case opc_if_icmpge:
            return opc_if_icmplt;
        case opc_ifeq:
            return opc_ifne;
        case opc_ifne:
            return opc_ifeq;
        case opc_iflt:
            return opc_ifge;
        case opc_ifgt:
            return opc_ifle;
        case opc_ifle:
            return opc_ifgt;
        case opc_ifge:
            return opc_iflt;
        case opc_ifnull:
            return opc_ifnonnull;
        case opc_ifnonnull:
            return opc_ifnull;
        }
        throw new InconsistencyException("OPCODE NOT ALLOWED:" + opcode);
    }

    // --------------------------------------------------------------------
    // OPTIMIZING A 5 INSTRUCTION DEPTH BLOCK
    // --------------------------------------------------------------------

    /**
     * Optimize a block of 5 instructions
     *
     * Return true if an optimization is made
     * This method should not modify other instructions
     */
    private static boolean optimize5(InstructionHandle handle, int opcode) {
        // replace boolean branch
        //    Java code that looks like this:     with this:
        //         if_icmplt A                      B: if_icmpge X
        //         iconst_0                   //goto Z {if someone branched on goto B}
        //         goto B                 Z
        //      A: iconst_1
        //      B: ifeq X
        //      Z
        // 5 BasicBlock
        if (handle.getNext() == null ||
            handle.getNext().getNext() == null ||
            handle.getNext().getNext().getNext() == null ||
            handle.getNext().getNext().getNext().getNext() == null) {
            return false;
        }

        switch (opcode) {
        case opc_if_acmpeq:
        case opc_if_acmpne:
        case opc_if_icmpeq:
        case opc_if_icmpne:
        case opc_if_icmplt:
        case opc_if_icmple:
        case opc_if_icmpgt:
        case opc_if_icmpge:
        case opc_ifeq:
        case opc_ifne:
        case opc_iflt:
        case opc_ifgt:
        case opc_ifle:
        case opc_ifge:
        case opc_ifnull:
        case opc_ifnonnull:
            boolean         startByZero;
            boolean         reverse;
            InstructionHandle   pointer = handle.getNext();
            InstructionHandle   gotoIns;

            if (pointer.getOpcode() == opc_iconst_0) {
                startByZero = true;
            } else if (pointer.getOpcode() == opc_iconst_1) {
                startByZero = false;
            } else {
                break;
            }

            gotoIns = pointer = pointer.getNext();
            if (pointer.getOpcode() != opc_goto) {
                // short one
                break;
            }

            pointer = pointer.getNext();
            if (pointer.getOpcode() != opc_iconst_0 && (pointer.getOpcode() != opc_iconst_1)) {
                break;
            } else if (pointer.getOpcode() == opc_iconst_0 && startByZero) {
                break;
            } else if (pointer.getOpcode() == opc_iconst_1 && !startByZero) {
                break;
            }

            pointer = pointer.getNext();
            if (gotoIns.getTarget() != pointer) {
                break;
            }
            if (pointer.getOpcode() == opc_ifeq) {
                reverse = true;
            } else if (pointer.getOpcode() == opc_ifne) {
                reverse = false;
            } else {
                break;
            }

            reverse = startByZero ? reverse : !reverse;

            if (gotoIns.isTarget()) {
                System.err.println("BEEP");
            } else {
                if (reverse) {
                    handle.replaceBy(new JumpInstruction(getReverseOpcode(opcode), pointer.getTarget()));
                } else {
                    handle.replaceBy(new JumpInstruction(opcode, pointer.getTarget()));
                }

                handle.getNext().getNext().getNext().getNext().destroy();
                handle.getNext().getNext().getNext().destroy();
                handle.getNext().getNext().destroy();
                handle.getNext().destroy();

                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------------
    // LOCAL VAR INSTRUCTION
    // --------------------------------------------------------------------

    /**
     * Optimizes the byte code for a single method.
     */
    private static boolean optimizeLocalVarInstruction(InstructionHandle handle, int opcode) {
        if (handle.getNext() == null) {
            handle.set();

            return false;
        }
        /*if (opcode == handle.getNext().getOpcode() && handle.getLocal().isLoad()) {
        // MAKE CODE SLOWER
        // dup
        if (((LocalVarInstruction)handle.getInstruction()).getIndex() == ((LocalVarInstruction)handle.getNext().getInstruction()).getIndex() && !handle.getNext().isTarget()) {
        handle.getNext().replaceBy(handle.getInstruction().getStack() == 2 ?
        new NoArgInstruction(opc_dup2) :
        new NoArgInstruction(opc_dup));
        handle.set();

        return true;
        }
        } else*/ if (handle.getNext().getOpcode() == opc_pop && !handle.getNext().isTarget()) {
            InstructionHandle next = handle.getNext();

            handle.destroy();
            next.destroy();

            return true;
        } else if (handle.getNext().getOpcode() == opc_pop2 && !handle.getNext().isTarget()) {
            if (handle.getInstruction().getStack() == 2) {
                handle.getNext().destroy();
                handle.destroy();

                return true;
            } else {
                handle.getNext().destroy();
                handle.replaceBy(new NoArgInstruction(opc_pop));

                return true;
            }
        } else if (handle.getNext().getInstruction() instanceof LocalVarInstruction && !handle.getNext().isTarget()) {
            if (handle.getLocal().isLoad() && handle.getNext().getLocal().isStore()) {
                // load store => nop
                if (((LocalVarInstruction)handle.getInstruction()).getIndex() == ((LocalVarInstruction)handle.getNext().getInstruction()).getIndex() && !handle.getNext().isTarget()) {
                    handle.destroy();
                    handle.getNext().destroy();

                    return true;
                }
            } /*else if (handle.getLocal().isStore() && handle.getNext().getLocal().isLoad()&& !handle.getNext().isTarget()) {
              // MAKE CODE CLOWER
              // store load => dup store
              if (handle.getLocal().getIndex() == handle.getNext().getLocal().getIndex()) {
              handle.getNext().destroy();
              handle.replaceBy(new Instruction[] {
              new NoArgInstruction(handle.getInstruction().getStack() == -2 ? opc_dup2 : opc_dup),
              handle.getInstruction()
              });

              return true;
              }
              }*/
        }

        handle.set();

        return false;
    }

    // --------------------------------------------------------------------
    // FIELD REF
    // --------------------------------------------------------------------

    /**
     * Optimizes the byte code for a single method.
     */
    private static boolean optimizeFieldRefInstruction(InstructionHandle handle, int opcode) {
        if (handle.getInstruction().getStack() == 0) {
            if (handle.getNext().getOpcode() == opc_pop
                || handle.getNext().getOpcode() == opc_pop2) {
                handle.destroy();
                return true;
            }
        } else if (handle.getInstruction().getStack() == 1) {
            if (handle.getNext().getOpcode() == opc_pop) {
                handle.replaceBy(new JumpInstruction(opc_goto, handle.getNext().getNext()));
                return true;
            } else if (handle.getNext().getOpcode() == opc_pop2) {
                handle.replaceBy(new Instruction[] {
                    new NoArgInstruction(opc_pop),
                    new JumpInstruction(opc_goto, handle.getNext().getNext())
                });
                return true;
            }
        } else if (handle.getInstruction().getStack() == 2) {
            if (handle.getNext().getOpcode() == opc_pop2) {
                handle.replaceBy(new JumpInstruction(opc_goto, handle.getNext().getNext()));
                return true;
            }
        }

        handle.set();

        return false;
    }
}
