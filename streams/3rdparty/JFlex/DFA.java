/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.3.2                                                             *
 * Copyright (C) 1998-2001  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * This program is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License. See the file      *
 * COPYRIGHT for more information.                                         *
 *                                                                         *
 * This program is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA                 *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package JFlex;


import java.io.*;


/**
 * DFA representation in JFlex.
 * Contains minimization algorithm.
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:33 $
 */
final public class DFA implements ErrorMessages { 

  /**
   * The initial number of states 
   */
  private static final int STATES = 500;
  
  /**
   * The code for "no target state" in the transition table.
   */
  public static final int NO_TARGET = -1;

  /**
   * table[current_state][character] is the next state for <code>current_state</code>
   * with input <code>character</code>, <code>NO_TARGET</code> if there is no transition for
   * this input in <code>current_state</code>
   */
  int [][] table;


  /**
   * <code>isFinal[state] == true</code> <=> the state <code>state</code> is 
   * a final state.
   */
  boolean [] isFinal;

  /**
   * <code>isPushback[state] == true</code> <=> the state <code>state</code> is 
   * a final state of an expression that can only be matched when followed by
   * a certain lookaead.
   */
  boolean [] isPushback;


  /**
   * <code>isLookEnd[state] == true</code> <=> the state <code>state</code> is 
   * a final state of a lookahead expression.
   */
  boolean [] isLookEnd;


  /**
   * <code>action[state]</code> is the action that is to be carried out in
   * state <code>state</code>, <code>null</code> if there is no action.
   */
  Action [] action;

  /**
   * lexState[i] is the start-state of lexical state i
   */
  int lexState [];

  /**
   * The number of states in this DFA
   */
  int numStates;


  /**
   * The current maximum number of input characters
   */
  int numInput;
    

  public DFA(int numLexStates, int numInp) {
    numInput = numInp; 
    
    int statesNeeded = Math.max(numLexStates, STATES);
    
    table       = new int [statesNeeded] [numInput];
    action      = new Action [statesNeeded];
    isFinal     = new boolean [statesNeeded];
    isPushback  = new boolean [statesNeeded];
    isLookEnd   = new boolean [statesNeeded];
    lexState    = new int [numLexStates];
    numStates   = 0;

    for (int i = 0; i < statesNeeded; i++) {
      for (char j = 0; j < numInput; j++)
	      table [i][j] = NO_TARGET;    
    }
  }


  public void setLexState(int lState, int trueState) {
    lexState[lState] = trueState;
  }

  private void ensureStateCapacity(int newNumStates) {

    int oldLength = isFinal.length;
    
    if ( newNumStates < oldLength ) return;
      
    int newLength = oldLength*2;
    while ( newLength <= newNumStates ) newLength*= 2;

    boolean [] newFinal    = new boolean [newLength];
    boolean [] newPushback = new boolean [newLength];
    boolean [] newLookEnd  = new boolean [newLength];
    Action  [] newAction   = new Action  [newLength];
    int [] []  newTable    = new int [newLength] [numInput];
    
    System.arraycopy(isFinal,0,newFinal,0,numStates);
    System.arraycopy(isPushback,0,newPushback,0,numStates);
    System.arraycopy(isLookEnd,0,newLookEnd,0,numStates);
    System.arraycopy(action,0,newAction,0,numStates);
    System.arraycopy(table,0,newTable,0,oldLength);
  
    int i,j;
  
    for (i = oldLength; i < newLength; i++) {
      for (j = 0; j < numInput; j++) {
        newTable[i][j] = NO_TARGET;
      }
    }

    isFinal    = newFinal;
    isPushback = newPushback;
    isLookEnd  = newLookEnd;
    action     = newAction;
    table      = newTable;
  }


  public void setAction(int state, Action stateAction) {
    action[state]    = stateAction;
    if (stateAction != null)
      isLookEnd[state] = stateAction.isLookAction;
  }
  
  public void setFinal(int state, boolean isFinalState) {
    isFinal[state] = isFinalState;
  }

  public void setPushback(int state, boolean isPushbackState) {
    isPushback[state] = isPushbackState;
  }


  public void addTransition(int start, char input, int dest) {
    int max = Math.max(start,dest)+1;
    ensureStateCapacity(max);
    if (max > numStates) numStates = max;

    //  Out.debug("Adding DFA transition ("+start+", "+(int)input+", "+dest+")");

    table[start][input] = dest;
  }



  public String toString() {
    StringBuffer result = new StringBuffer();

    for (int i=0; i < numStates; i++) {
      result.append("State ");
      if ( isFinal[i] ) result.append("[FINAL] ");
      if ( isPushback[i] ) result.append("[EOL] ");
      result.append(i+":"+Out.NL);
     
      for (char j=0; j < numInput; j++) {
	      if ( table[i][j] >= 0 ) 
	        result.append("  with "+(int)j+" in "+table[i][j]+Out.NL);	
      }
    }
    
    return result.toString();    
  }
  
  
  public void writeDot(File file) {
    try {
      PrintWriter writer = new PrintWriter(new FileWriter(file));
      writer.println(dotFormat());
      writer.close();
    }
    catch (IOException e) {
      Out.error(ErrorMessages.FILE_WRITE, file);
      throw new GeneratorException();
    }
  }


  public String dotFormat() {
    StringBuffer result = new StringBuffer();

    result.append("digraph DFA {"+Out.NL);
    result.append("rankdir = LR"+Out.NL);

    for (int i=0; i < numStates; i++) {
      if ( isFinal[i] || isPushback[i] ) result.append(i);
      if ( isFinal[i] ) result.append(" [shape = doublecircle]");
      if ( isPushback[i] ) result.append(" [shape = box]");
      if ( isFinal[i] || isPushback[i] ) result.append(Out.NL);
    }

    for (int i=0; i < numStates; i++) {
      for (int input = 0; input < numInput; input++) {
	      if ( table[i][input] >= 0 ) {
          result.append(i+" -> "+table[i][input]);
          result.append(" [label=\"["+input+"]\"]"+Out.NL);
          //          result.append(" [label=\"["+classes.toString(input)+"]\"]\n");
        }
      }
    }

    result.append("}"+Out.NL);

    return result.toString();
  }


  public void minimize() {
    
    int i,j;
    char c;

    Out.print(numStates+" states before minimization, ");

    if (numStates == 0) {
      Out.error(ZERO_STATES);
      throw new GeneratorException();
    }

    if (Main.no_minimize) {
      Out.println("minimization skipped.");
      return;
    }

    // notequiv[i][j] == true <=> state i and state j are equivalent
    boolean [] [] equiv = new boolean [numStates] [];

    // list[i][j] contains all pairs of states that have to be marked "not equivalent"
    // if states i and j are recognized to be not equivalent
    StatePairList [] [] list = new StatePairList [numStates] [];

    // construct a triangular matrix equiv[i][j] with j < i
    // and mark pairs (final state, not final state) as not equivalent
    for (i = 1; i < numStates; i++) {
      list[i] = new StatePairList[i];
      equiv[i] = new boolean [i];
      for (j = 0; j < i; j++) {
        // i and j are equivalent, iff :
        // i and j are both final and their actions are equivalent and have same pushback behaviour or
        // i and j are both not final

        if ( isFinal[i] && isFinal[j] && (isPushback[i] == isPushback[j]) && (isLookEnd[i] == isLookEnd[j]) ) 
          equiv[i][j] = action[i].equals(action[j]);        
        else
          equiv[i][j] = !isFinal[j] && !isFinal[i] && (isPushback[i] == isPushback[j]) && (isLookEnd[i] == isLookEnd[j]);
      }
    }


    for (i = 1; i < numStates; i++) {
      
      // Out.debug("Testing state "+i);

      for (j = 0; j < i; j++) {
	
        if ( equiv[i][j] ) {
  
          for (c = 0; c < numInput; c++) {
  
            if (equiv[i][j]) {              

              int p = table[i][c]; 
              int q = table[j][c];
              if (p < q) {
                int t = p;
                p = q;
                q = t;
              }
              if ( p >= 0 || q >= 0 ) {
                // Out.debug("Testing input '"+c+"' for ("+i+","+j+")");
                // Out.debug("Target states are ("+p+","+q+")");
                if ( p!=q && (p == -1 || q == -1 || !equiv[p][q]) ) {
                  equiv[i][j] = false;
                  if (list[i][j] != null) list[i][j].markAll(list,equiv);
                }
                // printTable(equiv);
              } // if (p >= 0) ..
            } // if (equiv[i][j]
          } // for (char c = 0; c < numInput ..
	  	  
          // if i and j are still marked equivalent..
          
          if ( equiv[i][j] ) {
         
            // Out.debug("("+i+","+j+") are still marked equivalent");
      
            for (c = 0; c < numInput; c++) {
      
              int p = table[i][c];
              int q = table[j][c];
              if (p < q) {
                int t = p;
                p = q;
                q = t;
              }
              
              if (p != q && p >= 0 && q >= 0) {
                if ( list[p][q] == null ) {
                  list[p][q] = new StatePairList();
                }
                list[p][q].addPair(i,j);
              }
            }      
          }
          else {
            // Out.debug("("+i+","+j+") are not equivalent");
          }
          
	      } // of first if (equiv[i][j])
      } // of for j
    } // of for i
    // }

    // printTable(equiv);

    // transform the transition table 
    
    // trans[i] is the state j that will replace state i, i.e. when 
    // states i and j are equivalent
    int trans [] = new int [numStates];
    
    // kill[i] is true, iff state i is redundant and can be removed
    boolean kill [] = new boolean [numStates];
    
    // move[i] is the amount line i has to be moved in the transitiontable
    // (because states j < i have been removed)
    int move [] = new int [numStates];
    
    // fill arrays trans[] and kill[]
    for (i = 0; i < numStates; i++) {
      trans[i] = i;
      for (j = 0; j < i; j++) 
        if (equiv[i][j]) {
          trans[i] = j;
          kill[i] = true;
          break;
        }
    }
    
    // fill array move[]
    int amount = 0;
    for (i = 0; i < numStates; i++) {
      if ( kill[i] ) 
        amount++;
      else
        move[i] = amount;
    }
    
    // j is now the index in the new transition table
    
    // the transition table is transformed in place 
    // (without using a new array)
    for (i = 0, j = 0; i < numStates; i++) {
      
      // we only copy lines that have not been removed
      if ( !kill[i] ) {
        
        // translate the target states 
        for (c = 0; c < numInput; c++) {
          if ( table[i][c] >= 0 ) {
            table[j][c] = trans[ table[i][c] ];
            table[j][c]-= move[ table[j][c] ];
          }
          else {
            table[j][c] = table[i][c];
          }
        }

        isFinal[j] = isFinal[i];
        isPushback[j] = isPushback[i];
        isLookEnd[j] = isLookEnd[i];
        action[j] = action[i];
        
        j++;
      }
    }
    
    numStates = j;
    
    // translate lexical states
    for (i = 0; i < lexState.length; i++) {
      lexState[i] = trans[ lexState[i] ];
      lexState[i]-= move[ lexState[i] ];
    }
  
    Out.println(numStates+" states in minimized DFA");  
  }

  public void printTable(boolean [] [] equiv) {

    Out.dump("Equivalence table is : ");
    for (int i = 1; i < numStates; i++) {
      String line = i+" :";
      for (int j = 0; j < i; j++) {
	      if (equiv[i][j]) 
	        line+= " E";
      	else
	        line+= " x";
      }
      Out.dump(line);
    }
  }

}
