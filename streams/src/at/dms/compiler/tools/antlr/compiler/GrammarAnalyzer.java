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
 * $Id: GrammarAnalyzer.java,v 1.1 2001-08-30 16:32:35 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

/**
 * A GrammarAnalyzer computes lookahead from Grammar (which contains
 * a grammar symbol table) and can then answer questions about the
 * grammar.
 *
 * To access the RuleBlock for a rule name, the grammar symbol table
 * is consulted.
 *
 * There should be no distinction between static & dynamic analysis.
 * In other words, some of the easy analysis can be done statically
 * and then the part that is hard statically can be deferred to
 * parse-time.  Interestingly, computing LL(k) for k>1 lookahead
 * statically is O(|T|^k) where T is the grammar vocabulary, but,
 * is O(k) at run-time (ignoring the large constant associated with
 * the size of the grammar).  In English, the difference can be
 * described as "find the set of all possible k-sequences of input"
 * versus "does this specific k-sequence match?".
 */
public interface GrammarAnalyzer {
  /**
   * The epsilon token type is an imaginary type used
   * during analysis.  It indicates an incomplete look() computation.
   * Must be kept consistent with Token constants to be between
   * MIN_USER_TYPE and INVALID_TYPE.
   */
  int		NONDETERMINISTIC = Integer.MAX_VALUE; // lookahead depth
  int		LOOKAHEAD_DEPTH_INIT = -1;
}
