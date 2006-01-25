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
 * $Id: GrammarDefinition.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Enumeration;
import java.io.IOException;

class GrammarDefinition {
    private String name;
    private String fileName;        // where does it come from?
    private String superGrammar;    // null if no super class
    private String type;                // lexer? parser? tree parser?
    private IndexedVector rules;    // text of rules as they were read in
    private IndexedVector options;// rule options
    private String tokenSection;    // the tokens{} stuff
    private String preambleAction;// action right before grammar
    private String memberAction;    // action inside grammar
    private Hierarchy hier;         // hierarchy of grammars
    private boolean predefined=false;   // one of the predefined grammars?
    private boolean alreadyExpanded = false;

    private String importVocab = null;
    private String exportVocab = null;

    public GrammarDefinition(String name, String superGrammar, IndexedVector rules) {
        this.name = name;
        this.superGrammar = superGrammar;
        this.rules = rules;
    }
    public void addOption(Option o) {
        if ( options==null ) {  // if not already there, create it
            options = new IndexedVector();
        }
        options.appendElement(o.getName(), o);
    }
    public void addRule(Rule r) {
        rules.appendElement(r.getName(), r);
    }

    /**
     * Copy all nonoverridden rules, vocabulary, and options into this grammar from
     *  supergrammar chain.  The change is made in place; e.g., this grammar's vector
     *  of rules gets bigger.  This has side-effects: all grammars on path to
     *  root of hierarchy are expanded also.
     */
    public void expandInPlace() {
        // if this grammar already expanded, just return
        if (alreadyExpanded) {
            return;
        }

        // Expand super grammar first (unless it's a predefined or subgrammar of predefined)
        GrammarDefinition superG = getSuperGrammar();
        if (superG == null) {
            return; // error (didn't provide superclass)
        }
        if (exportVocab == null) {
            // if no exportVocab for this grammar, make it same as grammar name
            exportVocab = getName();
        }
        if (superG.isPredefined()) {
            return; // can't expand Lexer, Parser, ...
        }
        superG.expandInPlace();

        // expand current grammar now.
        alreadyExpanded = true;
        // track whether a grammar file needed to have a grammar expanded
        GrammarFile gf = hier.getFile(getFileName());
        gf.setExpanded(true);

        // Copy rules from supergrammar into this grammar
        IndexedVector inhRules = superG.getRules();
        for (Enumeration e = inhRules.elements(); e.hasMoreElements();) {
            Rule r = (Rule) e.nextElement();
            inherit(r, superG);
        }

        // Copy options from supergrammar into this grammar
        // Modify tokdef options so that they point to dir of enclosing grammar
        IndexedVector inhOptions = superG.getOptions();
        if (inhOptions != null) {
            for (Enumeration e = inhOptions.elements(); e.hasMoreElements();) {
                Option o = (Option) e.nextElement();
                inherit(o, superG);
            }
        }

        // add an option to load the superGrammar's output vocab
        if ((options != null && options.getElement("importVocab") == null) || options == null) {
            // no importVocab found, add one that grabs superG's output vocab
            Option inputV = new Option("importVocab", superG.exportVocab+";", this);
            addOption(inputV);
            // copy output vocab file to current dir
            String originatingGrFileName = superG.getFileName();
            String path = Utils.pathToFile(originatingGrFileName);
            String superExportVocabFileName = path + superG.exportVocab +
                JavaCodeGenerator.TokenTypesFileSuffix +
                JavaCodeGenerator.TokenTypesFileExt;
            String newImportVocabFileName = Utils.fileMinusPath(superExportVocabFileName);
            if (path.equals("." + System.getProperty("file.separator"))) {
                // don't copy tokdef file onto itself (must be current directory)
                // System.out.println("importVocab file same dir; leaving as " + superExportVocabFileName);
            } else {
                try {
                    Utils.copyFile(superExportVocabFileName, newImportVocabFileName);
                } catch (IOException io) {
                    Utils.toolError("cannot find/copy importVocab file " + superExportVocabFileName);
                    return;
                }
            }
        }

        // copy member action from supergrammar into this grammar
        inherit(superG.memberAction, superG);
    }

    public String getFileName() { return fileName; }

    public String getName() {
        return name;
    }

    public IndexedVector getOptions() { return options; }

    public IndexedVector getRules() { return rules; }

    public GrammarDefinition getSuperGrammar() {
        if ( superGrammar==null ) {
            return null;
        }
        GrammarDefinition g = (GrammarDefinition)hier.getGrammar(superGrammar);
        return g;
    }

    public String getSuperGrammarName() {
        return superGrammar;
    }

    public String getType() {
        return type;
    }

    public void inherit(Option o, GrammarDefinition superG) {
        // do NOT inherit importVocab/exportVocab options under any circumstances
        if ( o.getName().equals("importVocab") ||
             o.getName().equals("exportVocab") ) {
            return;
        }

        Option overriddenOption = null;
        if ( options!=null ) {  // do we even have options?
            overriddenOption = (Option)options.getElement(o.getName());
        }
        // if overridden, do not add to this grammar
        if ( overriddenOption==null ) { // not overridden
            addOption(o);   // copy option into this grammar--not overridden
        }
    }

    public void inherit(Rule r, GrammarDefinition superG) {
        // if overridden, do not add to this grammar
        Rule overriddenRule = (Rule)rules.getElement(r.getName());
        if ( overriddenRule!=null ) {
            // rule is overridden in this grammar.
            if ( !overriddenRule.sameSignature(r) ) {
                // warn if different sig
                Utils.warning("rule "+getName()+"."+overriddenRule.getName()+
                              " has different signature than "+
                              superG.getName()+"."+overriddenRule.getName());
            }
        } else {  // not overridden, copy rule into this
            addRule(r);
        }
    }

    public void inherit(String memberAction, GrammarDefinition superG) {
        if (this.memberAction!=null) {
            return; // do nothing if already have member action
        }
        if ( memberAction != null ) { // don't have one here, use supergrammar's action
            this.memberAction = memberAction;
        }
    }

    public void setImportVocabulary(String text) {
        importVocab = text;
    }
    public void setExportVocabulary(String text) {
        exportVocab = text;
    }

    public boolean isPredefined() { return predefined; }
    public void setFileName(String f) { fileName=f; }
    public void setHierarchy(Hierarchy hier) { this.hier = hier; }
    public void setMemberAction(String a) {memberAction=a;}
    public void setOptions(IndexedVector options) {
        this.options = options;
    }
    public void setPreambleAction(String a) {preambleAction=a;}
    public void setPredefined(boolean b) { predefined=b; }
    public void setTokenSection(String tk) {
        tokenSection=tk;
    }
    public void setType(String t) {
        type = t;
    }
    public String toString() {
        String s="";
        if ( preambleAction!=null ) {
            s += preambleAction;
        }
        if ( superGrammar==null ) {
            return "class "+name+";";
        }
        String sup = "";
        s+="class "+name+" extends "+type+sup+";"+
            System.getProperty("line.separator")+
            System.getProperty("line.separator");
        if ( options!=null ) {
            s += Hierarchy.optionsToString(options);
        }
        if ( tokenSection!=null ) {
            s += tokenSection + System.getProperty("line.separator");
        }
        if ( memberAction!=null ) {
            s += memberAction+System.getProperty("line.separator");
        }
        for (int i=0; i<rules.size(); i++) {
            Rule r = (Rule)rules.elementAt(i);
            if ( !getName().equals(r.enclosingGrammar.getName()) ) {
                s += "// inherited from grammar "+r.enclosingGrammar.getName()+System.getProperty("line.separator");
            }
            s += r+
                System.getProperty("line.separator")+
                System.getProperty("line.separator");
        }
        return s;
    }
}
