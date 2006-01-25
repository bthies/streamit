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
 * $Id: GrammarAtom.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * A GrammarAtom is either a token ref, a character ref, or string.
 * The analysis doesn't care.
 */
abstract class GrammarAtom extends AlternativeElement {
    protected String label;
    protected String atomText;
    protected int tokenType = Token.INVALID_TYPE;
    protected boolean not = false;  // ~T or ~'c' or ~"foo"
    /**
     * Set to type of AST node to create during parse.  Defaults to what is
     *  set in the TokenSymbol.
     */
    public GrammarAtom(Grammar g, Token t) {
        super(g);
        atomText = t.getText();
    }

    public String getLabel() {
        return label;
    }

    public String getText() {
        return atomText;
    }

    public int getType() {
        return tokenType;
    }

    public void setLabel(String label_) {
        label = label_;
    }

    public void setOption(Token option, Token value) {
        grammar.tool.error("Invalid element option:"+option.getText(),
                           grammar.getFilename(), option.getLine());
    }

    public String toString() {
        String s = " ";
        if ( label!=null ) {
            s += label+":";
        }
        if ( not ) {
            s += "~";
        }
        return s+atomText;
    }
}
