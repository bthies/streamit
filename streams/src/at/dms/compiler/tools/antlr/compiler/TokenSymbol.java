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
 * $Id: TokenSymbol.java,v 1.2 2006-01-25 17:00:50 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

class TokenSymbol extends GrammarSymbol {
    protected int ttype;
    /**
     * describes what token matches in "human terms"
     */
    protected String paraphrase = null;

    public TokenSymbol(String r) {
        super(r);
        ttype = Token.INVALID_TYPE;
    }

    public String getParaphrase() {return paraphrase;}

    public int getTokenType() {
        return ttype;
    }

    public void setParaphrase(String p) {paraphrase = p;}

    public void setTokenType(int t) {
        ttype = t;
    }
}
