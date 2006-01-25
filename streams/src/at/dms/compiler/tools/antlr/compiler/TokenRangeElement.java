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
 * $Id: TokenRangeElement.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

class TokenRangeElement extends AlternativeElement {
    String label;
    protected int begin=Token.INVALID_TYPE;
    protected int end  =Token.INVALID_TYPE;
    protected String beginText;
    protected String endText;


    public TokenRangeElement(Grammar g, Token t1, Token t2) {
        super(g);
        begin = grammar.tokenManager.getTokenSymbol(t1.getText()).getTokenType();
        beginText = t1.getText();
        end = grammar.tokenManager.getTokenSymbol(t2.getText()).getTokenType();
        endText = t2.getText();
        line = t1.getLine();
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
        if ( label!=null ) {
            return " "+label+":"+beginText+".."+endText;
        } else {
            return " "+beginText+".."+endText;
        }
    }
}
