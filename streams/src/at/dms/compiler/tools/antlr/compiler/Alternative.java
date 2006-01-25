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
 * $Id: Alternative.java,v 1.2 2006-01-25 17:00:49 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Hashtable;

/**
 * Intermediate data class holds information about an alternative
 */
class Alternative {
    // Tracking alternative linked list
    AlternativeElement head;   // head of alt element list
    AlternativeElement tail;  // last element added

    // Syntactic predicate block if non-null
    protected SynPredBlock synPred;
    // Semantic predicate action if non-null
    protected String semPred;
    // Exception specification if non-null
    protected ExceptionSpec exceptionSpec;
    // Init action if non-null;
    protected Lookahead[] cache;    // lookahead for alt.  Filled in by
    // deterministic() only!!!!!!!  Used for
    // code gen after calls to deterministic()
    // and used by deterministic for (...)*, (..)+,
    // and (..)? blocks.  1..k
    protected int lookaheadDepth;   // each alt has different look depth possibly.
    // depth can be NONDETERMINISTIC too.
    // 0..n-1
    // True if text generation is on for this alt
    private boolean doAutoGen;


    public Alternative() {
    }
    public Alternative(AlternativeElement firstElement) {
        addElement(firstElement);
    }
    public void addElement(AlternativeElement e) {
        // Link the element into the list
        if ( head == null ) {
            head = tail = e;
        } else {
            tail.next = e;
            tail = e;
        }
    }
    public boolean atStart() { return head == null; }
    public boolean getAutoGen() {
        return doAutoGen;
    }
    public void setAutoGen(boolean doAutoGen_) {
        doAutoGen = doAutoGen_;
    }
}
