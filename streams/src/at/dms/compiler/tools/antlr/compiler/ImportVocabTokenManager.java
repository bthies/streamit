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
 * $Id: ImportVocabTokenManager.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.*;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * Static implementation of the TokenManager, used for importVocab option
 */
class ImportVocabTokenManager extends SimpleTokenManager implements Cloneable {
    private String filename;
    protected Grammar grammar;

    ImportVocabTokenManager(Grammar grammar, String filename_, String name_, Main tool_) {
        // initialize
        super(name_, tool_);
        this.grammar = grammar;

        // Figure out exactly where the file lives.  Check $PWD first,
        // and then search in -o <output_dir>.

        File grammarFile = new File(filename_);

        if ( ! grammarFile.exists() ) {
            grammarFile = new File( tool.getOutputDirectory(), filename_);

            if ( ! grammarFile.exists() ) {
                Utils.panic("Cannot find importVocab file '" + filename);
            }
        }

        filename = filename_;
        setReadOnly(true);

        // Read a file with lines of the form ID=number
        try {
            // SAS: changed the following for proper text io
            FileReader fileIn = new FileReader(grammarFile);
            ANTLRTokdefLexer tokdefLexer = new ANTLRTokdefLexer(fileIn);
            ANTLRTokdefParser tokdefParser = new ANTLRTokdefParser(tokdefLexer);
            tokdefParser.setFilename(filename);
            tokdefParser.file(this);
        } catch (FileNotFoundException fnf) {
            Utils.panic("Cannot find importVocab file '" + filename);
        } catch (RecognitionException ex) {
            Utils.panic("Error parsing importVocab file '" + filename + "': " + ex.toString());
        } catch (TokenStreamException ex) {
            Utils.panic("Error reading importVocab file '" + filename + "'");
        }
    }
    public Object clone() {
        ImportVocabTokenManager tm;
        tm = (ImportVocabTokenManager)super.clone();
        tm.filename = this.filename;
        tm.grammar = this.grammar;
        return tm;
    }
    /**
     * define a token.
     */
    public void define(TokenSymbol ts) {
        if (readOnly) {
            String  text = ts.getParaphrase();
            if (text == null) {
                text = ts.getId();
            }
            tool.error("Cannot redefine token " + text + ": vocabulary is read-only");
        }
        super.define(ts);
    }
    /**
     * define a token.  Intended for use only when reading the importVocab file.
     */
    public void define(String s, int ttype) {
        TokenSymbol ts=null;
        if ( s.startsWith("\"") ) {
            ts = new StringLiteralSymbol(s);
        } else {
            ts = new TokenSymbol(s);
        }
        ts.setTokenType(ttype);
        super.define(ts);
        maxToken = (ttype+1)>maxToken ? (ttype+1) : maxToken;   // record maximum token type
    }
    /**
     * importVocab token manager is read-only if output would be same as input
     */
    public boolean isReadOnly() {
        return readOnly;
    }
    /**
     * Get the next unused token type.
     */
    public int nextTokenType() {
        return super.nextTokenType();
    }
}
