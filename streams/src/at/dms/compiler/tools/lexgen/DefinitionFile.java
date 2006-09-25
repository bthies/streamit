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
 * $Id: DefinitionFile.java,v 1.4 2006-09-25 13:54:32 dimock Exp $
 */

package at.dms.compiler.tools.lexgen;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import at.dms.compiler.tools.common.CompilerMessages;
import at.dms.compiler.tools.common.PositionedError;
import at.dms.compiler.tools.common.TokenReference;
import at.dms.compiler.tools.antlr.runtime.ParserException;
import at.dms.compiler.tools.common.Utils;

class DefinitionFile {

    /**
     * Constructs a token definition file
     */
    public DefinitionFile(String sourceFile,
                          String packageName,
                          String vocabulary,
                          String prefix,
                          Vector definitions)
    {
        this.sourceFile = sourceFile;
        this.packageName    = packageName;
        this.vocabulary = vocabulary;
        this.prefix     = prefix == null ? DEFAULT_LITERAL_PREFIX : prefix;
        this.definitions    =
            (TokenDefinition[])Utils.toArray(definitions, TokenDefinition.class);
    }

    /**
     * Reads and parses an token definition file
     *
     * @param   sourceFile      the name of the source file
     * @return  a class info structure holding the information from the source
     *
     */
    public static DefinitionFile read(String sourceFile) throws LexgenError {
        try {
            InputStream input = new BufferedInputStream(new FileInputStream(sourceFile));
            LexgenLexer scanner = new LexgenLexer(input);
            LexgenParser    parser = new LexgenParser(scanner);
            DefinitionFile  defs = parser.aCompilationUnit(sourceFile);

            input.close();

            return defs;
        } catch (FileNotFoundException e) {
            throw new LexgenError(LexgenMessages.FILE_NOT_FOUND, sourceFile);
        } catch (IOException e) {
            throw new LexgenError(LexgenMessages.IO_EXCEPTION, sourceFile, e.getMessage());
        } catch (ParserException e) {
            throw new LexgenError(LexgenMessages.FORMATTED_ERROR,
                                  new PositionedError(new TokenReference(sourceFile, e.getLine()),
                                                      LexgenMessages.SYNTAX_ERROR,
                                                      e.getMessage()));
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Check for duplicate identifiers
     * @param   identifiers a table of all token identifiers
     * @param   prefix      the literal prefix
     * @param   id      the id of the first token
     * @return  the id of the last token + 1
     */
    public int checkIdentifiers(Hashtable<String, String> identifiers, String prefix, int id)
        throws LexgenError
    {
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].checkIdentifiers(identifiers, prefix, id++, sourceFile);
        }

        return id;
    }

    /**
     * Prints token definitions to definition file (txt)
     * @param   out     the output stream
     * @param   prefix      the literal prefix
     */
    public void printDefinition(PrintWriter out, String prefix) {
        out.println();
        out.println("// Definitions from " + sourceFile);
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].printDefinition(out, prefix);
        }
    }

    /**
     * Prints the token definition to interface file (java)
     * @param   out     the output stream
     * @param   parent      the super interface
     * @param   createTokens    create standard tokens
     */
    public void printInterface(PrintWriter out, String parent, boolean createTokens) {
        out.print("// Generated from " + sourceFile);
        out.println();
        out.println("package " + packageName + ";");
        out.println();
        out.print("public interface " + vocabulary + "TokenTypes");
        if (parent != null) {
            out.print(" extends " + parent);
        }
        out.println(" {");

        if (parent == null) {
            out.println("  int\tEOF = 1;");
            out.println("  int\tNULL_TREE_LOOKAHEAD = 3;");
        }

        for (int i = 0; i < definitions.length; i++) {
            definitions[i].printInterface(out, this.prefix);
        }

        if (createTokens) {
            for (int i = 0; i < definitions.length; i++) {
                definitions[i].printToken(out, this.prefix);
            }
        }

        out.println("}");
    }

    /**
     * Prints flex rules for literals, keywords and operators
     * @param   out     the output stream
     */
    public void printFlexRules(PrintWriter out) {
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].printFlexRule(out, this.prefix);
        }
    }

    /**
     * Adds keywords to vector
     */
    public void putKeywords(Vector keywords, Vector types, String prefix) {
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].putKeyword(keywords, types, prefix);
        }
    }

    /**
     * Returns the package name
     */
    public String getClassName() {
        return packageName + "." + vocabulary + "TokenTypes";
    }

    /**
     * Returns the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Returns the vocabulary name
     */
    public String getVocabulary() {
        return vocabulary;
    }

    /**
     * Returns the literal prefix
     */
    public String getPrefix() {
        return prefix;
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private static final String     DEFAULT_LITERAL_PREFIX = "LITERAL_";

    private final String            sourceFile;
    private final String            packageName;
    private final String            vocabulary;
    private final String            prefix;
    private final TokenDefinition[] definitions;
}
