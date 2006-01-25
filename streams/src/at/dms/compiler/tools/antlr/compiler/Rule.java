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
 * $Id: Rule.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Enumeration;

class Rule {
    protected String name;
    protected String block;
    protected String args;
    protected String returnValue;
    protected String throwsSpec;
    protected String initAction;
    protected IndexedVector options;
    protected String visibility;
    protected GrammarDefinition enclosingGrammar;

    public Rule(String n, String b, IndexedVector options, GrammarDefinition gr) {
        name = n;
        block = b;
        this.options = options;
        setEnclosingGrammar(gr);
    }
    public String getArgs() { return args; }
    public String getName() { return name; }
    public String getReturnValue() { return returnValue; }
    public String getVisibility() { return visibility; }
    /**
     * If 'rule' narrows the visible of 'this', return true;
     *  For example, 'this' is public and 'rule' is private,
     *  true is returned.  You cannot narrow the vis. of
     *  a rule.
     */
    public boolean narrowerVisibility(Rule rule) {
        if ( visibility.equals("public") ) {
            if ( !rule.equals("public") ) {
                return true;    // everything narrower than public
            }
            return false;
        } else if ( visibility.equals("protected") ) {
            if ( rule.equals("private") ) {
                return true;    // private narrower than protected
            }
            return false;
        } else if ( visibility.equals("private") ) {
            return false;   // nothing is narrower than private
        }
        return false;
    }
    /**
     * Two rules have the same signature if they have:
     *      same name
     *      same return value
     *      same args
     *  I do a simple string compare now, but later
     *  the type could be pulled out so it is insensitive
     *  to names of args etc...
     */
    public boolean sameSignature(Rule rule) {
        boolean nSame=true;
        boolean aSame=true;
        boolean rSame=true;

        nSame = name.equals(rule.getName());
        if ( args!=null ) {
            aSame = args.equals(rule.getArgs());
        }
        if ( returnValue!=null ) {
            rSame = returnValue.equals(rule.getReturnValue());
        }
        return nSame && aSame && rSame;
    }
    public void setArgs(String a) { args=a; }
    public void setEnclosingGrammar(GrammarDefinition g) { enclosingGrammar=g; }
    public void setInitAction(String a) {initAction = a;}
    public void setOptions(IndexedVector options) {
        this.options = options;
    }
    public void setReturnValue(String ret) { returnValue=ret; }
    public void setThrowsSpec(String t) { throwsSpec=t; }
    public void setVisibility(String v) { visibility=v; }
    public String toString() {
        String s="";
        String retString = returnValue==null ? "" : "returns "+returnValue;
        String argString = args==null ? "" : args;

        s += visibility==null ? "" : visibility+" ";
        s += name+argString+" "+retString+throwsSpec;
        if ( options!=null ) {
            s += System.getProperty("line.separator")+
                "options {"+
                System.getProperty("line.separator");
            for (Enumeration e = options.elements() ; e.hasMoreElements() ;) {
                s += (Option)e.nextElement()+System.getProperty("line.separator");
            }
            s += "}"+System.getProperty("line.separator");
        }
        if ( initAction!=null ) {
            s+=initAction+System.getProperty("line.separator");
        }
        s += block;
        return s;
    }
}
