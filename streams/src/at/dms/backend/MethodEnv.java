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
 * $Id: MethodEnv.java,v 1.3 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import java.util.Vector;

import at.dms.classfile.LocalVarInstruction;
import at.dms.classfile.MethodInfo;
import at.dms.util.InconsistencyException;

/**
 * This class represents a method environment
 */
class MethodEnv {

    MethodEnv(MethodInfo info) {
        this.info = info;
    }

    /**
     * getLocalVar
     */
    public QTemporary getLocalVar(int pos) {
        return locals.elementAt(pos);
    }

    /**
     * getLocalVar
     */
    public QTemporary getLocalVar(InstructionHandle insn) {
        int     slot = ((LocalVarInstruction)insn.getInstruction()).getIndex();

        if (slot >= locals.size()) {
            locals.setSize(slot + 1);
        }

        QTemporary  temp = locals.elementAt(slot);

        if (temp == null) {
            insn.getInstruction().dump();
            locals.setElementAt(temp = new QTemporary(insn.getInstruction().getReturnType(), slot), slot);
        }

        return temp;
    }

    public void setParameters(int[] sizes) {
        getParameters(info, info.getSignature(), sizes);
    }

    /**
     * Computes the number of parameters. (MOVE FROM HERE !!!)
     */
    private static void getParameters(MethodInfo info, String signature, int[] sizes) {
        int     paramCnt = 0;

        if (paramCnt == sizes.length) {
            return;
        }

        if ((info.getModifiers() & at.dms.classfile.Constants.ACC_STATIC) == 0) {
            // an instance method always passes "this" as first, hidden parameter
            sizes[paramCnt++] = 1;
        }

        if (signature.charAt(0) != '(') {
            throw new InconsistencyException("invalid signature " + signature);
        }

        int     pos = 1;

        _method_parameters_:
        for (;;) {
            if (paramCnt == sizes.length) {
                return;
            }
            switch (signature.charAt(pos++)) {
            case ')':
                break _method_parameters_;

            case '[':
                while (signature.charAt(pos) == '[') {
                    pos += 1;
                }
                if (signature.charAt(pos) == 'L') {
                    while (signature.charAt(pos) != ';') {
                        pos += 1;
                    }
                }
                pos += 1;

                sizes[paramCnt++] = 1;
                break;

            case 'L':
                while (signature.charAt(pos) != ';') {
                    pos += 1;
                }
                pos += 1;

                sizes[paramCnt++] = 1;
                break;

            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'F':
            case 'I':
                sizes[paramCnt++] = 1;
                break;

            case 'D':
            case 'J':
                sizes[paramCnt++] = 2;
                break;

            default:
                throw new InconsistencyException("invalid signature " + signature);
            }
        }
    }

    // ----------------------------------------------------------------------
    // PRIVATE DATA TYPE
    // ----------------------------------------------------------------------

    private Vector<QTemporary>  locals = new Vector<QTemporary>();
    private MethodInfo  info;
}
