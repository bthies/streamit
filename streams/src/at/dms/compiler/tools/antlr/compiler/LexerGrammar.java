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
 * $Id: LexerGrammar.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.IOException;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * Lexer-specific grammar subclass
 */
class LexerGrammar extends Grammar {
    // character set used by lexer
    protected BitSet charVocabulary;
    // true if the lexer generates literal testing code for nextToken
    protected boolean testLiterals = true;
    // true if the lexer generates case-sensitive LA(k) testing
    protected boolean caseSensitiveLiterals = true;
    /**
     * true if the lexer generates case-sensitive literals testing
     */
    protected boolean caseSensitive = true;
    /**
     * true if lexer is to ignore all unrecognized tokens
     */
    protected boolean filterMode = false;

    /**
     * if filterMode is true, then filterRule can indicate an optional
     *  rule to use as the scarf language.  If null, programmer used
     *  plain "filter=true" not "filter=rule".
     */
    protected String filterRule = null;

    LexerGrammar(String className_, Main tool_) {
        super(className_, tool_, null);
        charVocabulary = new BitSet();

        // Lexer usually has no default error handling
        defaultErrorHandler = false;
    }
    /**
     * Top-level call to generate the code
     */
    public void generate(JavaCodeGenerator generator) throws IOException {
        generator.gen(this);
    }
    public String getSuperClass() {
        return "CharScanner";
    }
    // Get the testLiterals option value
    public boolean getTestLiterals() {
        return testLiterals;
    }
    /**
     * Set the character vocabulary used by the lexer
     */
    public void setCharVocabulary(BitSet b) {
        charVocabulary = b;
    }
    /**
     * Set lexer options
     */
    public boolean setOption(String key, Token value) {
        String s = value.getText();

        if (key.equals("testLiterals")) {
            if (s.equals("true")) {
                testLiterals = true;
            } else if (s.equals("false")) {
                testLiterals = false;
            } else {
                Utils.warning("testLiterals option must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("interactive")) {
            if (s.equals("true")) {
                interactive = true;
            } else if (s.equals("false")) {
                interactive = false;
            } else {
                tool.error("interactive option must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("caseSensitive")) {
            if (s.equals("true")) {
                caseSensitive = true;
            } else if (s.equals("false")) {
                caseSensitive = false;
            } else {
                Utils.warning("caseSensitive option must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("caseSensitiveLiterals")) {
            if (s.equals("true")) {
                caseSensitiveLiterals= true;
            } else if (s.equals("false")) {
                caseSensitiveLiterals= false;
            } else {
                Utils.warning("caseSensitiveLiterals option must be true or false", getFilename(), value.getLine());
            }
            return true;
        }
        if (key.equals("filter")) {
            if (s.equals("true")) {
                filterMode = true;
            } else if (s.equals("false")) {
                filterMode = false;
            } else if ( value.getType()==ANTLRTokenTypes.TOKEN_REF) {
                filterMode = true;
                filterRule = s;
            } else {
                Utils.warning("filter option must be true, false, or a lexer rule name", getFilename(), value.getLine());
            }
            return true;
        }
        if (super.setOption(key, value)) {
            return true;
        }
        tool.error("Invalid option: " + key, getFilename(), value.getLine());
        return false;
    }
}
