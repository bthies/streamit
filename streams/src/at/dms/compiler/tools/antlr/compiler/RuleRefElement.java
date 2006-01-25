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
 * $Id: RuleRefElement.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

class RuleRefElement extends AlternativeElement {
    protected String targetRule; // which rule is being called?
    protected String args=null;      // were any args passed to rule?
    protected String idAssign=null;  // is the return type assigned to a variable?
    protected String label;


    public RuleRefElement(Grammar g, Token t) {
        super(g);
        targetRule = t.getText();
        //      if ( Character.isUpperCase(targetRule.charAt(0)) ) { // lexer rule?
        if ( t.getType() == ANTLRTokenTypes.TOKEN_REF ) { // lexer rule?
            targetRule = JavaCodeGenerator.lexerRuleName(targetRule);
        }
        line = t.getLine();
    }

    public RuleRefElement(Grammar g, String t, int line) {
        super(g);
        targetRule = t;
        if ( Character.isUpperCase(targetRule.charAt(0)) ) { // lexer rule?
            targetRule = JavaCodeGenerator.lexerRuleName(targetRule);
        }
        this.line = line;
    }

    public void generate(JavaCodeGenerator generator) {
        generator.gen(this);
    }
    public String getArgs() {
        return args;
    }
    public String getIdAssign() {
        return idAssign;
    }
    public String getLabel() {
        return label;
    }
    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
    public void setArgs(String a) {
        args = a;
    }
    public void setIdAssign(String id) {
        idAssign = id;
    }
    public void setLabel(String label_) {
        label = label_;
    }
    public String toString() {
        if (args!=null) {
            return " "+targetRule+args;
        } else {
            return " "+targetRule;
        }
    }
}
