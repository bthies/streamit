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
 * $Id: TokenDefinition.java,v 1.4 2006-09-25 13:54:32 dimock Exp $
 */

package at.dms.compiler.tools.lexgen;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

class TokenDefinition {

    /**
     * Token types
     */
    public static final int LITERAL = 1;
    public static final int KEYWORD = 2;
    public static final int OPERATOR = 3;
    public static final int OTHER = 4;

    /**
     * Constructs a token definition
     */
    public TokenDefinition(int type, String name, String value) {
        this.type       = type;
        this.name       = name;
        // strip leading and trailing quotes
        this.value      = value == null ? null : value.substring(1, value.length() - 1);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Check for duplicate identifiers
     * @param   identifiers a table of all token identifiers
     * @param   prefix      the literal prefix
     * @param   id      the id of the token
     * @param   sourceFile  the file where the token is defined
     */
    public void checkIdentifiers(Hashtable<String, String> identifiers, String prefix, int id, String sourceFile)
        throws LexgenError
    {
        String      ident  = type == LITERAL ? prefix + name : name;
        String      stored = identifiers.get(ident);

        if (stored != null) {
            throw new LexgenError(LexgenMessages.DUPLICATE_DEFINITION, new Object[] { ident, sourceFile, stored });
        }
        identifiers.put(ident, sourceFile);

        this.id = id;
    }

    /**
     * Prints the token definition to definition file (txt)
     * @param   out     the output stream
     * @param   prefix      the literal prefix
     */
    public void printDefinition(PrintWriter out, String prefix) {
        if (type == LITERAL) {
            out.print(prefix + name);
            out.print(" = ");
            out.print("\"" + (value == null ? name : value) + "\"");
        } else {
            out.print(name);
            if (value != null) {
                out.print("(\"" + value + "\")");
            }
        }

        out.println(" = " + id);
    }

    /**
     * Prints  flex rules for literals, keywords and operators
     * @param   out     the output stream
     * @param   prefix      the literal prefix
     */
    public void printFlexRule(PrintWriter out, String prefix) {
        switch (type) {
        case LITERAL:
        case KEYWORD:
        case OPERATOR:
            printFlexRegex(out, value == null ? name : value);
            out.print("\t\t{ return ");
            out.print(TOKEN_PREFIX + (type == LITERAL ? prefix + name : name));
            out.println("; }");
            break;
        case OTHER:
            /* nothing */;
        }
    }

    private void printFlexRegex(PrintWriter out, String text) {
        out.print("\"");
        for (int i = 0; i < text.length(); i++) {
            char    c;

            switch (c = text.charAt(i)) {
            case '^':
            case '$':
                out.print('\\');
                out.print(c);
                break;
            default:
                out.print(c);
            }
        }
        out.print("\"");
    }

    /**
     * Prints the token definition to interface file (java)
     * @param   out     the output stream
     * @param   prefix      the literal prefix
     */
    public void printInterface(PrintWriter out, String prefix) {
        out.print("  int\t");
        if (type == LITERAL) {
            out.print(prefix + name);
        } else {
            out.print(name);
        }
        out.println(" = " + id + ";");
    }

    /**
     * Prints the CToken object to interface file (java)
     * @param   out     the output stream
     * @param   prefix      the literal prefix
     */
    public void printToken(PrintWriter out, String prefix) {
        switch (type) {
        case LITERAL:
        case KEYWORD:
        case OPERATOR:
            out.print("  " + TOKEN_CLASS + "\t");
            out.print(TOKEN_PREFIX + (type == LITERAL ? prefix + name : name));
            out.print(" = new " + TOKEN_CLASS + "(");
            out.print(type == LITERAL ? prefix + name : name);
            out.print(", ");
            out.print("\"" + (value == null ? name : value) + "\"");
            out.println(");");
            break;
        case OTHER:
            /* nothing */;
        }
    }

    /**
     * Adds keywords to vector
     */
    public void putKeyword(Vector keywords, Vector types, String prefix) {
        if (type == LITERAL || type == KEYWORD) {
            keywords.addElement(value == null ? name : value);
            types.addElement(type == LITERAL ? prefix + name : name);
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private static final String     TOKEN_CLASS = "at.dms.compiler.antlr.extra.CToken";
    private static final String     TOKEN_PREFIX = "TOKEN_";

    private final int           type;
    private final String            name;
    private final String            value;

    private int             id;
}
