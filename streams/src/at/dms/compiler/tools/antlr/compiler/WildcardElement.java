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
 * $Id: WildcardElement.java,v 1.2 2006-01-25 17:00:50 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

class WildcardElement extends GrammarAtom {
    protected String label;

    public WildcardElement(Grammar g, Token t) {
        super(g, t);
        line = t.getLine();
    }
    public void generate(JavaCodeGenerator generator) {
        generator.gen(this);
    }
    public String getLabel() {
        return label;
    }
    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
    public void setLabel(String label_) {
        label = label_;
    }
    public String toString() {
        String s = " ";
        if (label!=null) {
            s += label+":";
        }
        return s + ".";
    }
}
