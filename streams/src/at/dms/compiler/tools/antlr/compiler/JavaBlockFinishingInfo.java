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
 * $Id: JavaBlockFinishingInfo.java,v 1.1 2001-08-30 16:32:35 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

class JavaBlockFinishingInfo {
  String postscript;		// what to generate to terminate block
  boolean generatedSwitch;// did block finish with "default:" of switch?
  boolean generatedAnIf;

  /**
   * When generating an if or switch, end-of-token lookahead sets
   *  will become the else or default clause, don't generate an
   *  error clause in this case.
   */
  boolean needAnErrorClause;


  public JavaBlockFinishingInfo() {
    postscript=null;
    generatedSwitch=generatedSwitch = false;
    needAnErrorClause = true;
  }
  public JavaBlockFinishingInfo(String ps, boolean genS, boolean generatedAnIf, boolean n) {
    postscript = ps;
    generatedSwitch = genS;
    this.generatedAnIf = generatedAnIf;
    needAnErrorClause = n;
  }
}
