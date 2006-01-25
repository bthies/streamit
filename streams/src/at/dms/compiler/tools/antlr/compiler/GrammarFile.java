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
 * $Id: GrammarFile.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.*;
import java.util.Enumeration;

/**
 * Stores header action, grammar preamble, file options, and
 *  list of grammars in the file
 */
public class GrammarFile {
    protected String fileName;
    protected String headerAction="";
    protected IndexedVector options;
    protected IndexedVector grammars;
    protected boolean expanded = false; // any grammars expanded within?

    public GrammarFile(String f) {
        fileName = f;
        grammars = new IndexedVector();
    }
    public void addGrammar(GrammarDefinition g) {
        grammars.appendElement(g.getName(), g);
    }
    public void generateExpandedFile(Main tool) throws IOException {
        if ( !expanded ) {
            return; // don't generate if nothing got expanded
        }
        String expandedFileName = nameForExpandedGrammarFile(this.getName());

        // create the new grammar file with expanded grammars
        PrintWriter expF = tool.openOutputFile(expandedFileName);
        expF.println(toString());
        expF.close();
    }
    public IndexedVector getGrammars() {
        return grammars;
    }
    public String getName() {return fileName;}
    public String nameForExpandedGrammarFile(String f) {
        if ( expanded ) {
            // strip path to original input, make expanded file in current dir
            return "expanded"+Utils.fileMinusPath(f);
        } else {
            return f;
        }
    }
    public void setExpanded(boolean exp) {
        expanded = exp;
    }
    public void addHeaderAction(String a) {headerAction+=a+System.getProperty("line.separator");}
    public void setOptions(IndexedVector o) {options=o;}
    public String toString() {
        String h = headerAction==null ? "" : headerAction;
        String o = options==null ? "" : Hierarchy.optionsToString(options);

        String s=h+o;
        for (Enumeration e=grammars.elements(); e.hasMoreElements(); ) {
            GrammarDefinition g = (GrammarDefinition)e.nextElement();
            s += g;
        }
        return s;
    }
}
