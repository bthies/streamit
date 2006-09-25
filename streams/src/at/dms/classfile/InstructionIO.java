/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
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
 * $Id: InstructionIO.java,v 1.4 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;

import at.dms.util.InconsistencyException;

/**
 * An utility class that read the instructions from a stream
 */
public class InstructionIO implements Constants {

    /**
     * Constructs an array of instructions from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   IOException an io problem has occured
     * @exception   ClassFileFormatException attempt to read a bad classfile info
     */
    public static Instruction[] read(DataInput in, ConstantPool cp)
        throws IOException, ClassFileFormatException
    {
        InstructionIO   reader = new InstructionIO(in, cp);

        reader.readInstructions();
        reader.resolveReferences();
        reader.selectSwitchTypes();

        return reader.instructions;
    }


    /*
     *
     */
    private InstructionIO(DataInput in, ConstantPool cp) {
        this.in = in;
        this.cp = cp;

        this.forwards = new Vector<Instruction>();
        this.switches = new Vector<Instruction>();
    }

    /*
     *
     */
    private void readInstructions() throws IOException, ClassFileFormatException {
        int         length;

        length = in.readInt();
        instructions = new Instruction[length];

        boolean     wide = false;

        for (int start = 0; start < length; /*increment in loop body*/) {
            int     opcode = in.readUnsignedByte();
            int     extra = 0;

            switch (opcode) {
            case opc_nop:
            case opc_aconst_null:
            case opc_iaload:
            case opc_laload:
            case opc_faload:
            case opc_daload:
            case opc_aaload:
            case opc_baload:
            case opc_caload:
            case opc_saload:
            case opc_iastore:
            case opc_lastore:
            case opc_fastore:
            case opc_dastore:
            case opc_aastore:
            case opc_bastore:
            case opc_castore:
            case opc_sastore:
            case opc_pop:
            case opc_pop2:
            case opc_dup:
            case opc_dup_x1:
            case opc_dup_x2:
            case opc_dup2:
            case opc_dup2_x1:
            case opc_dup2_x2:
            case opc_swap:
            case opc_iadd:
            case opc_ladd:
            case opc_fadd:
            case opc_dadd:
            case opc_isub:
            case opc_lsub:
            case opc_fsub:
            case opc_dsub:
            case opc_imul:
            case opc_lmul:
            case opc_fmul:
            case opc_dmul:
            case opc_idiv:
            case opc_ldiv:
            case opc_fdiv:
            case opc_ddiv:
            case opc_irem:
            case opc_lrem:
            case opc_frem:
            case opc_drem:
            case opc_ineg:
            case opc_lneg:
            case opc_fneg:
            case opc_dneg:
            case opc_ishl:
            case opc_lshl:
            case opc_ishr:
            case opc_lshr:
            case opc_iushr:
            case opc_lushr:
            case opc_iand:
            case opc_land:
            case opc_ior:
            case opc_lor:
            case opc_ixor:
            case opc_lxor:
            case opc_i2l:
            case opc_i2f:
            case opc_i2d:
            case opc_l2i:
            case opc_l2f:
            case opc_l2d:
            case opc_f2i:
            case opc_f2l:
            case opc_f2d:
            case opc_d2i:
            case opc_d2l:
            case opc_d2f:
            case opc_i2b:
            case opc_i2c:
            case opc_i2s:
            case opc_lcmp:
            case opc_fcmpl:
            case opc_fcmpg:
            case opc_dcmpl:
            case opc_dcmpg:
            case opc_ireturn:
            case opc_lreturn:
            case opc_freturn:
            case opc_dreturn:
            case opc_areturn:
            case opc_return:
            case opc_arraylength:
            case opc_athrow:
            case opc_monitorenter:
            case opc_monitorexit:
                instructions[start] = new NoArgInstruction(opcode);
                break;
            case opc_iconst_m1:
                instructions[start] = new PushLiteralInstruction(-1);
                break;
            case opc_iconst_0:
                instructions[start] = new PushLiteralInstruction(0);
                break;
            case opc_iconst_1:
                instructions[start] = new PushLiteralInstruction(1);
                break;
            case opc_iconst_2:
                instructions[start] = new PushLiteralInstruction(2);
                break;
            case opc_iconst_3:
                instructions[start] = new PushLiteralInstruction(3);
                break;
            case opc_iconst_4:
                instructions[start] = new PushLiteralInstruction(4);
                break;
            case opc_iconst_5:
                instructions[start] = new PushLiteralInstruction(5);
                break;
            case opc_lconst_0:
                instructions[start] = new PushLiteralInstruction((long)0);
                break;
            case opc_lconst_1:
                instructions[start] = new PushLiteralInstruction((long)1);
                break;
            case opc_fconst_0:
                instructions[start] = new PushLiteralInstruction((float)0);
                break;
            case opc_fconst_1:
                instructions[start] = new PushLiteralInstruction((float)1);
                break;
            case opc_fconst_2:
                instructions[start] = new PushLiteralInstruction((float)2);
                break;
            case opc_dconst_0:
                instructions[start] = new PushLiteralInstruction((double)0);
                break;
            case opc_dconst_1:
                instructions[start] = new PushLiteralInstruction((double)1);
                break;
            case opc_bipush:
                instructions[start] = new PushLiteralInstruction(in.readByte());
                extra = 1;
                break;
            case opc_sipush:
                instructions[start] = new PushLiteralInstruction(in.readShort());
                extra = 2;
                break;
            case opc_ldc:
                instructions[start] = new PushLiteralInstruction(cp.getEntryAt(in.readUnsignedByte()), false);
                extra = 1;
                break;
            case opc_ldc_w:
                instructions[start] = new PushLiteralInstruction(cp.getEntryAt(in.readUnsignedShort()), false);
                extra = 2;
                break;
            case opc_ldc2_w:
                instructions[start] = new PushLiteralInstruction(cp.getEntryAt(in.readUnsignedShort()), true);
                extra = 2;
                break;
            case opc_iload:
            case opc_lload:
            case opc_fload:
            case opc_dload:
            case opc_aload:
            case opc_istore:
            case opc_lstore:
            case opc_fstore:
            case opc_dstore:
            case opc_astore:
            case opc_ret:
                if (wide) {
                    wide = false;
                    instructions[start] = new LocalVarInstruction(opcode, in.readUnsignedShort());
                    extra = 3;  // count wide prefix
                } else {
                    instructions[start] = new LocalVarInstruction(opcode, in.readUnsignedByte());
                    extra = 1;
                }
                break;
            case opc_iload_0:
                instructions[start] = new LocalVarInstruction(opc_iload, 0);
                break;
            case opc_iload_1:
                instructions[start] = new LocalVarInstruction(opc_iload, 1);
                break;
            case opc_iload_2:
                instructions[start] = new LocalVarInstruction(opc_iload, 2);
                break;
            case opc_iload_3:
                instructions[start] = new LocalVarInstruction(opc_iload, 3);
                break;
            case opc_lload_0:
                instructions[start] = new LocalVarInstruction(opc_lload, 0);
                break;
            case opc_lload_1:
                instructions[start] = new LocalVarInstruction(opc_lload, 1);
                break;
            case opc_lload_2:
                instructions[start] = new LocalVarInstruction(opc_lload, 2);
                break;
            case opc_lload_3:
                instructions[start] = new LocalVarInstruction(opc_lload, 3);
                break;
            case opc_fload_0:
                instructions[start] = new LocalVarInstruction(opc_fload, 0);
                break;
            case opc_fload_1:
                instructions[start] = new LocalVarInstruction(opc_fload, 1);
                break;
            case opc_fload_2:
                instructions[start] = new LocalVarInstruction(opc_fload, 2);
                break;
            case opc_fload_3:
                instructions[start] = new LocalVarInstruction(opc_fload, 3);
                break;
            case opc_dload_0:
                instructions[start] = new LocalVarInstruction(opc_dload, 0);
                break;
            case opc_dload_1:
                instructions[start] = new LocalVarInstruction(opc_dload, 1);
                break;
            case opc_dload_2:
                instructions[start] = new LocalVarInstruction(opc_dload, 2);
                break;
            case opc_dload_3:
                instructions[start] = new LocalVarInstruction(opc_dload, 3);
                break;
            case opc_aload_0:
                instructions[start] = new LocalVarInstruction(opc_aload, 0);
                break;
            case opc_aload_1:
                instructions[start] = new LocalVarInstruction(opc_aload, 1);
                break;
            case opc_aload_2:
                instructions[start] = new LocalVarInstruction(opc_aload, 2);
                break;
            case opc_aload_3:
                instructions[start] = new LocalVarInstruction(opc_aload, 3);
                break;
            case opc_istore_0:
                instructions[start] = new LocalVarInstruction(opc_istore, 0);
                break;
            case opc_istore_1:
                instructions[start] = new LocalVarInstruction(opc_istore, 1);
                break;
            case opc_istore_2:
                instructions[start] = new LocalVarInstruction(opc_istore, 2);
                break;
            case opc_istore_3:
                instructions[start] = new LocalVarInstruction(opc_istore, 3);
                break;
            case opc_lstore_0:
                instructions[start] = new LocalVarInstruction(opc_lstore, 0);
                break;
            case opc_lstore_1:
                instructions[start] = new LocalVarInstruction(opc_lstore, 1);
                break;
            case opc_lstore_2:
                instructions[start] = new LocalVarInstruction(opc_lstore, 2);
                break;
            case opc_lstore_3:
                instructions[start] = new LocalVarInstruction(opc_lstore, 3);
                break;
            case opc_fstore_0:
                instructions[start] = new LocalVarInstruction(opc_fstore, 0);
                break;
            case opc_fstore_1:
                instructions[start] = new LocalVarInstruction(opc_fstore, 1);
                break;
            case opc_fstore_2:
                instructions[start] = new LocalVarInstruction(opc_fstore, 2);
                break;
            case opc_fstore_3:
                instructions[start] = new LocalVarInstruction(opc_fstore, 3);
                break;
            case opc_dstore_0:
                instructions[start] = new LocalVarInstruction(opc_dstore, 0);
                break;
            case opc_dstore_1:
                instructions[start] = new LocalVarInstruction(opc_dstore, 1);
                break;
            case opc_dstore_2:
                instructions[start] = new LocalVarInstruction(opc_dstore, 2);
                break;
            case opc_dstore_3:
                instructions[start] = new LocalVarInstruction(opc_dstore, 3);
                break;
            case opc_astore_0:
                instructions[start] = new LocalVarInstruction(opc_astore, 0);
                break;
            case opc_astore_1:
                instructions[start] = new LocalVarInstruction(opc_astore, 1);
                break;
            case opc_astore_2:
                instructions[start] = new LocalVarInstruction(opc_astore, 2);
                break;
            case opc_astore_3:
                instructions[start] = new LocalVarInstruction(opc_astore, 3);
                break;
            case opc_iinc:
                if (wide) {
                    wide = false;
                    instructions[start] = new IincInstruction(in.readUnsignedShort(), in.readShort());
                    extra = 5;  // count wide prefix
                } else {
                    instructions[start] = new IincInstruction(in.readUnsignedByte(), in.readByte());
                    extra = 2;
                }
                break;

            case opc_ifeq:
            case opc_ifne:
            case opc_iflt:
            case opc_ifge:
            case opc_ifgt:
            case opc_ifle:
            case opc_if_icmpeq:
            case opc_if_icmpne:
            case opc_if_icmplt:
            case opc_if_icmpge:
            case opc_if_icmpgt:
            case opc_if_icmple:
            case opc_if_acmpeq:
            case opc_if_acmpne:
            case opc_goto:
            case opc_jsr:
            case opc_ifnull:
            case opc_ifnonnull:
                {
                    int     target = start + in.readShort();

                    if (target < 0 || target >= instructions.length) {
                        throw new InconsistencyException(); // !!! class format exception
                    }
                    if (target < start) {
                        instructions[start] = new JumpInstruction(opcode, instructions[target]);
                    } else {
                        instructions[start] = new JumpInstruction(opcode, new ForwardReference(target));
                        forwards.addElement(instructions[start]);
                    }
                    extra = 2;
                }
                break;
            case opc_goto_w:
            case opc_jsr_w:
                {
                    int     target = start + in.readInt();

                    if (target < 0 || target >= instructions.length) {
                        throw new InconsistencyException(); // !!! class format exception
                    }
                    if (target < start) {
                        instructions[start] = new JumpInstruction(opcode, instructions[target]);
                    } else {
                        instructions[start] = new JumpInstruction(opcode, new ForwardReference(target));
                        forwards.addElement(instructions[start]);
                    }
                    extra = 4;
                }
                break;
            case opc_tableswitch:
                extra = readTableSwitch(start);
                break;
            case opc_lookupswitch:
                extra = readLookupSwitch(start);
                break;
            case opc_getstatic:
            case opc_putstatic:
            case opc_getfield:
            case opc_putfield:
                instructions[start] = new FieldRefInstruction(opcode, (FieldRefConstant)cp.getEntryAt(in.readUnsignedShort()));
                extra = 2;
                break;
            case opc_invokevirtual:
            case opc_invokespecial:
            case opc_invokestatic:
                instructions[start] = new MethodRefInstruction(opcode, (MethodRefConstant)cp.getEntryAt(in.readUnsignedShort()));
                extra = 2;
                break;
            case opc_invokeinterface:
                {
                    InterfaceConstant   method;
                    int         nargs;

                    method = (InterfaceConstant)cp.getEntryAt(in.readUnsignedShort());
                    nargs = in.readUnsignedByte();
                    in.readUnsignedByte();

                    instructions[start] = new InvokeinterfaceInstruction(method, nargs);
                    extra = 2 + 1 + 1;
                }
                break;
            case opc_new:
            case opc_anewarray:
            case opc_checkcast:
            case opc_instanceof:
                instructions[start] = new ClassRefInstruction(opcode, (ClassConstant)cp.getEntryAt(in.readUnsignedShort()));
                extra = 2;
                break;
            case opc_newarray:
                instructions[start] = new NewarrayInstruction((byte)in.readUnsignedByte());
                extra = 1;
                break;
            case opc_multianewarray:
                instructions[start] = new MultiarrayInstruction((ClassConstant)cp.getEntryAt(in.readUnsignedShort()),
                                                                in.readUnsignedByte());
                extra = 3;
                break;
            case opc_wide:
                wide = true;
                continue;   // no instruction to create
            case opc_xxxunusedxxx:
            default:
                throw new ClassFileFormatException("read invalid opcode: "  + opcode);
            }

            start += 1 + extra;
        }
    }

    /*
     * Resolves forward references.
     */
    private void resolveReferences() throws ClassFileFormatException {
        AccessorTransformer transformer = new AccessorTransformer() {
                public InstructionAccessor transform(InstructionAccessor accessor,
                                                     AccessorContainer container)
                    throws BadAccessorException
                {
                    if (!(accessor instanceof ForwardReference)) {
                        return accessor;
                    }
                    // the only accessors to resolve are forward references
                    int     index = ((ForwardReference)accessor).getIndex();

                    if (instructions[index] == null) {
                        throw new BadAccessorException("no instruction at " + index);
                    }
                    return instructions[index];
                }
            };

        for (Enumeration<Instruction> eNum = forwards.elements(); eNum.hasMoreElements(); ) {
            AccessorContainer       insn = (AccessorContainer)eNum.nextElement();

            try {
                insn.transformAccessors(transformer);
            } catch (BadAccessorException e) {
                throw new ClassFileFormatException("forward reference not resolvable");
            }
        }
    }

    /*
     * Selects appropriate switch types.
     */
    private void selectSwitchTypes() {
        //for (Enumeration e = switches.elements(); e.hasMoreElements(); ) {
        //((SwitchInstruction)e.nextElement()).selectSwitchType();
        //    }
    }

    // --------------------------------------------------------------------
    // HANDLING OF COMPLEX INSTRUCTIONS
    // --------------------------------------------------------------------

    /*
     * Reads a tableswitch instruction.
     */
    private int readTableSwitch(int start) throws IOException {
        int     extra = 0;

        // skip padding
        for (int i = start + 1; (i % 4) != 0; i++) {
            extra += 1;
            in.readUnsignedByte();
        }

        InstructionAccessor     deflab;
        int             first;
        int             count;
        int             target;
        boolean         addForward = false;

        target = start + in.readInt();
        if (target < start) {
            deflab = instructions[target];
        } else {
            deflab = new ForwardReference(target);
            addForward = true;
        }

        first = in.readInt();
        count = in.readInt() - first + 1;

        int[]           matches = new int[count];
        InstructionAccessor[]   targets = new InstructionAccessor[count];

        for (int i = 0; i < count; i++) {
            matches[i] = first + i;

            target = start + in.readInt();
            if (target < start) {
                targets[i] = instructions[target];
            } else {
                targets[i] = new ForwardReference(target);
                addForward = true;
            }
        }

        instructions[start] = new SwitchInstruction(opc_tableswitch, deflab, matches, targets);
        extra += 4 + 4 + 4 + 4*count;

        if (addForward) {
            forwards.addElement(instructions[start]);
        }
        switches.addElement(instructions[start]);

        return extra;
    }


    /*
     * Reads a lookupswitch instruction.
     */
    private int readLookupSwitch(int start) throws IOException {
        int     extra = 0;

        // skip padding
        for (int i = start + 1; (i % 4) != 0; i++) {
            extra += 1;
            in.readUnsignedByte();
        }

        InstructionAccessor     deflab;
        int             count;
        int             target;
        boolean         addForward = false;

        target = start + in.readInt();
        if (target < start) {
            deflab = instructions[target];
        } else {
            deflab = new ForwardReference(target);
            addForward = true;
        }

        count = in.readInt();

        int[]           matches = new int[count];
        InstructionAccessor[]   targets = new InstructionAccessor[count];

        for (int i = 0; i < count; i++) {
            matches[i] = in.readInt();

            target = start + in.readInt();
            if (target < start) {
                targets[i] = instructions[target];
            } else {
                targets[i] = new ForwardReference(target);
                addForward = true;
            }
        }

        instructions[start] = new SwitchInstruction(opc_lookupswitch, deflab, matches, targets);
        extra += 4 + 4 + 8*count;

        if (addForward) {
            forwards.addElement(instructions[start]);
        }
        switches.addElement(instructions[start]);

        return extra;
    }

    // --------------------------------------------------------------------
    // FORWARD REFERENCES
    // --------------------------------------------------------------------

    private static class ForwardReference extends AbstractInstructionAccessor {

        /**
         * Constructs a new forward reference
         *
         * @param   index       the index of the referenced target
         *              in the instruction array
         */
        ForwardReference(int index) {
            this.index = index;
        }

        /**
         * Returns the index of the referenced instruction.
         */
        public int getIndex() {
            return index;
        }

        // ------------------------------------------------------------------
        // DATA MEMBERS
        // ------------------------------------------------------------------

        private final int       index;
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private DataInput       in;
    private ConstantPool        cp;

    private Vector<Instruction>      forwards;
    private Vector<Instruction>      switches;

    private Instruction[]       instructions;
}
