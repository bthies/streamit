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
 * $Id: BytecodeOptimizer.java,v 1.2 2006-01-25 17:01:22 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import java.util.Enumeration;

import at.dms.classfile.CodeInfo;

/**
 * This class encapsulates the bytecode optimization calls.
 * Depending on the requested optimization level, it will choose
 * the optimizer.
 */
public class BytecodeOptimizer {

    /**
     * Creates an optimizer.
     *
     * @param   level       the requested optimization level
     */
    public BytecodeOptimizer(int level) {
        this.level = level;
    }

    /**
     * Runs the optimizer on the given code info.
     *
     * @param   info        the code to optimize.
     */
    public CodeInfo run(CodeInfo info) {
        if (level >= 10) {
            return at.dms.backend.Optimizer.optimize(info);
        } else if (level >= 1) {
            return at.dms.optimize.Optimizer.optimize(info, level);
        } else {
            return info;
        }
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    /**
     * The requested optimization level.
     */
    private int         level;
}
