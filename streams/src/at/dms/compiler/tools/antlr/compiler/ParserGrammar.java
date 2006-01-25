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
 * $Id: ParserGrammar.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.IOException;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * Parser-specific grammar subclass
 */
class ParserGrammar extends Grammar {

    ParserGrammar(String className_, Main tool_) {
        super(className_, tool_, null);
    }
    /**
     * Top-level call to generate the code for this grammar
     */
    public void generate(JavaCodeGenerator generator) throws IOException {
        generator.gen(this);
    }
    // Get name of class from which generated parser/lexer inherits
    protected String getSuperClass() {
        return "LLkParser";
    }

    /**
     * Set parser options -- performs action on the following options:
     */
    public boolean setOption(String key, Token value) {
        String s = value.getText();

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
        if (key.equals("superClass")) {
            if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                superClass = s.substring(1, s.length() - 1);
                return true;
            } else {
                tool.error("superClass option must be a string", getFilename(), value.getLine());
                return false;
            }
        }
        if (key.equals("access")) {
            if (s.equals("\"public\"")) {
                setDefaultAccess("public");
                return true;
            } else if (s.equals("\"protected\"")) {
                setDefaultAccess("protected");
                return true;
            } else if (s.equals("\"private\"")) {
                setDefaultAccess("private");
                return true;
            } else {
                tool.error("access option must be in {\"public\", \"protected\", \"private\"}",
                           getFilename(), value.getLine());
                return false;
            }
        }
        if (super.setOption(key, value)) {
            return true;
        }
        tool.error("Invalid option: " + key, getFilename(), value.getLine());
        return false;
    }
}
