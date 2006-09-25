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
 * $Id: Grammar.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * A Grammar holds a set of rules (which are stored
 * in a symbol table).  Most of the time a grammar
 * needs a code generator and an LLkAnalyzer too.
 */
public abstract class Grammar {
    private Hashtable<String, RuleSymbol> symbols;

    protected Main tool;
    protected LLkGrammarAnalyzer theLLkAnalyzer;
    protected boolean analyzerDebug = false;
    protected boolean interactive = false;
    protected String superClass = null;

    /**
     * The token manager associated with the grammar, if any.
     * The token manager is responsible for maintaining the set of valid tokens, and
     * is conceptually shared between the lexer and parser.  This may be either a
     * LexerGrammar or a ImportVocabTokenManager.
     */
    protected TokenManager tokenManager;

    /**
     * The name of the export vocabulary...used to generate the output
     *  token types interchange file.
     */
    protected String exportVocab = null;

    /**
     * The name of the import vocabulary.  "Initial conditions"
     */
    protected String importVocab = null;

    // Mapping from String keys to Token option values
    protected Hashtable<String, Token> options;
    // Vector of RuleSymbol entries
    protected Vector rules;

    protected Token preambleAction = new CommonToken(Token.INVALID_TYPE, "");
    private String className = null;
    private String fileName = null;
    protected Token classMemberAction = new CommonToken(Token.INVALID_TYPE, "");
    protected boolean hasSyntacticPredicate = false;
    protected boolean hasUserErrorHandling = false;

    // max lookahead that can be attempted for this parser.
    protected int maxk=1;

    // options
    protected boolean defaultErrorHandler = true;

    protected String comment = null; // javadoc comment

    /**
     * default access modifier for rules
     */
    private String defaultAccess = "public";

    public Grammar(String className_, Main tool_, String superClass) {
        className = className_;
        tool = tool_;
        symbols = new Hashtable<String, RuleSymbol>();
        options = new Hashtable<String, Token>();
        rules = new Vector(100);
        this.superClass = superClass;
    }


    /**
     * Sets the default access modifier for rules.
     */
    public void setDefaultAccess(String defaultAccess) {
        this.defaultAccess = defaultAccess;
    }

    /**
     * Returns the default access modifier for rules.
     */
    public String getDefaultAccess() {
        return defaultAccess;
    }

    /**
     * Define a rule
     */
    public void define(RuleSymbol rs) {
        rules.appendElement(rs);
        // add the symbol to the rules hash table
        symbols.put(rs.getId(), rs);
    }

    /**
     * Top-level call to generate the code for this grammar
     */
    public abstract void generate(JavaCodeGenerator generator) throws IOException;

    protected String getClassName() { return className; }

    /* Does this grammar have a default error handler? */
    public boolean getDefaultErrorHandler() {
        return defaultErrorHandler;
    }

    public String getFilename() {
        return fileName;
    }

    /**
     * Get an integer option.  Given the name of the option find its
     * associated integer value.  If the associated value is not an integer or
     * is not in the table, then throw an exception of type NumberFormatException.
     * @param key The name of the option
     * @return The value associated with the key.
     */
    public int getIntegerOption(String key) throws NumberFormatException {
        Token t = options.get(key);
        if (t == null || t.getType() != ANTLRTokenTypes.INT) {
            throw new NumberFormatException();
        } else {
            return Integer.parseInt(t.getText());
        }
    }

    /**
     * Get an option.  Given the name of the option find its associated value.
     * @param key The name of the option
     * @return The value associated with the key, or null if the key has not been set.
     */
    public Token getOption(String key) {
        return options.get(key);
    }

    // Get name of class from which generated parser/lexer inherits
    protected abstract String getSuperClass();

    public GrammarSymbol getSymbol(String s) {
        return symbols.get(s);
    }

    public Enumeration<RuleSymbol> getSymbols() {
        return symbols.elements();
    }

    /**
     * Check the existence of an option in the table
     * @param key The name of the option
     * @return true if the option is in the table
     */
    public boolean hasOption(String key) {
        return options.containsKey(key);
    }

    /**
     * Is a rule symbol defined? (not used for tokens)
     */
    public boolean isDefined(String s) {
        return symbols.containsKey(s);
    }

    public void setFilename(String s) {
        fileName = s;
    }

    public void setGrammarAnalyzer(LLkGrammarAnalyzer a) {
        theLLkAnalyzer = a;
    }

    /**
     * Set a generic option.
     * This associates a generic option key with a Token value.
     * No validation is performed by this method, although users of the value
     * (code generation and/or analysis) may require certain formats.
     * The value is stored as a token so that the location of an error
     * can be reported.
     * @param key The name of the option.
     * @param value The value to associate with the key.
     * @return true if the option was a valid generic grammar option, false o/w
     */
    public boolean setOption(String key, Token value) {
        options.put(key, value);
        String s = value.getText();
        int i;
        if (key.equals("k")) {
            try {
                maxk = getIntegerOption("k");
            } catch (NumberFormatException e) {
                tool.error("option 'k' must be an integer (was "+value.getText()+")", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("codeGenMakeSwitchThreshold")) {
            try {
                i = getIntegerOption("codeGenMakeSwitchThreshold");
            } catch (NumberFormatException e) {
                tool.error("option 'codeGenMakeSwitchThreshold' must be an integer", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("codeGenBitsetTestThreshold")) {
            try {
                i = getIntegerOption("codeGenBitsetTestThreshold");
            } catch (NumberFormatException e) {
                tool.error("option 'codeGenBitsetTestThreshold' must be an integer", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("defaultErrorHandler")) {
            if (s.equals("true")) {
                defaultErrorHandler = true;
            } else if (s.equals("false")) {
                defaultErrorHandler = false;
            } else {
                tool.error("Value for defaultErrorHandler must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("analyzerDebug")) {
            if (s.equals("true")) {
                analyzerDebug = true;
            } else if (s.equals("false")) {
                analyzerDebug = false;
            } else {
                tool.error("option 'analyzerDebug' must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("classHeaderSuffix")) {
            return true;
        }
        return false;
    }

    public void setTokenManager(TokenManager tokenManager_) {
        tokenManager = tokenManager_;
    }

    /**
     * Print out the grammar without actions
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(20000);
        Enumeration ids = rules.elements();
        while ( ids.hasMoreElements() ) {
            RuleSymbol rs = (RuleSymbol)ids.nextElement();
            if (!rs.id.equals("mnextToken")) {
                buf.append(rs.getBlock().toString());
                buf.append("\n\n");
            }
        }
        return buf.toString();
    }
}
