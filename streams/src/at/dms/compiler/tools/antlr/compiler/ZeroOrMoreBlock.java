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
 * $Id: ZeroOrMoreBlock.java,v 1.2 2006-01-25 17:00:50 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

class ZeroOrMoreBlock extends BlockWithImpliedExitPath {


    public ZeroOrMoreBlock(Grammar g) {
        super(g);
    }
    public ZeroOrMoreBlock(Grammar g, int line) {
        super(g, line);
    }
    public void generate(JavaCodeGenerator generator) {
        generator.gen(this);
    }
    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
    public String toString() {
        return super.toString() + "*";
    }
}
