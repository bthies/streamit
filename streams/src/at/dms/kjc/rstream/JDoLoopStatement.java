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
 * $Id: JDoLoopStatement.java,v 1.3 2004-08-19 19:29:00 mgordon Exp $
 */

package at.dms.kjc.rstream;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.*;


//do loop - equivalent to 
//for (<type_of_induction> <induction> = <initExpr>; <induction> < <cond>; <induction> += incr) <body>
public class JDoLoopStatement extends JForStatement
{
    private JLocalVariable induction;
    private JExpression initValue;
    private JExpression incrValue;
    private JExpression condValue;
    private boolean countUp;
    private boolean zeroInit;
  
     
    public JDoLoopStatement (JLocalVariable induction,
			     JExpression initVal,
			     JExpression condVal,
			     JExpression incrVal,
			     JStatement body,
			     boolean countUp)
    {
	//try to construct a legal for loop that will represent this do loop
	super(null, new JExpressionStatement(null, 
					     new JAssignmentExpression
					     (null, 
					      new JLocalVariableExpression(null, induction),
					      initVal), 
					     null),
	      new JRelationalExpression(null,
					Constants.OPE_LT,
new JLocalVariableExpression(null, induction),
					condVal),
	      new JExpressionStatement(null,
				       new JCompoundAssignmentExpression
				       (null, OPE_PLUS,
					new JLocalVariableExpression(null, induction),
					incrVal), 
				       null),
	      body,
	      null);
	
	assert countUp;
	
	this.induction = induction;
	this.initValue = initVal;
	this.incrValue = incrVal;
	this.condValue = condVal;
	this.countUp = countUp;
	this.zeroInit =  (Util.passThruParens(initVal) instanceof JIntLiteral &&
			  ((JIntLiteral)Util.passThruParens(initVal)).intValue() == 0);
	
    }
   
    public JDoLoopStatement (JLocalVariable induction,
			     JExpression initVal,
			     JExpression condVal,
			     JExpression incrVal,
			     JStatement body,
			     boolean countUp,
			     boolean zeroInit)  
    {
	//try to construct a legal for loop that will represent this do loop
	super(null, new JExpressionStatement(null, 
					     new JAssignmentExpression
					     (null, 
					      new JLocalVariableExpression(null, induction),
					      initVal), 
					     null),
	      new JRelationalExpression(null,
					Constants.OPE_LT,
					new JLocalVariableExpression(null, induction),
					condVal),
	      new JExpressionStatement(null,
				       new JCompoundAssignmentExpression
				       (null, OPE_PLUS,
					new JLocalVariableExpression(null, induction),
					incrVal), 
				       null),
	      body,
	      null);
	
	assert countUp;
	
	this.induction = induction;
	this.initValue = initVal;
	this.incrValue = incrVal;
	this.condValue = condVal;
	this.countUp = countUp;
	this.zeroInit = zeroInit;
    }

    public JExpression getCondValue() 
    {
	return condValue;
    }

    public JLocalVariable getInduction() 
    {
	return induction;
    }
    
    public boolean countUp() 
    {
	return true;
    }
    
    public boolean zeroInit() 
    {
	return true;
    }

    public JExpression getInitValue() 
    {
	return initValue;
    }
    
    public JExpression getIncrValue() 
    {
	return incrValue;
    }

    public boolean staticBounds() 
    {
	return (Util.passThruParens(initValue) instanceof JIntLiteral &&
		Util.passThruParens(condValue) instanceof JIntLiteral &&
		Util.passThruParens(incrValue) instanceof JIntLiteral);	
    }
    
    public int getTripCount() 
    {
	assert staticBounds();
	
	int init, cond, incr;
	
	init = ((JIntLiteral)Util.passThruParens(initValue)).intValue();
	cond = ((JIntLiteral)Util.passThruParens(condValue)).intValue();
	incr = ((JIntLiteral)Util.passThruParens(incrValue)).intValue();
	
	int tripCount = (int)java.lang.Math.round((((double)(cond  - init) / (double)incr)));
	
	assert tripCount >= 0;
	
	return tripCount;
    }
    

    
    //for cloning only
    protected JDoLoopStatement() 
    {
	
    }
    

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
	at.dms.kjc.rstream.JDoLoopStatement other = new at.dms.kjc.rstream.JDoLoopStatement();
	at.dms.kjc.AutoCloner.register(this, other);
	deepCloneInto(other);
	return other;
    }
    
    /** Clones all fields of this into <other> */
    protected void deepCloneInto(at.dms.kjc.rstream.JDoLoopStatement other) {
	super.deepCloneInto(other);
	other.initValue = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.initValue);
	other.incrValue = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.incrValue);
	other.condValue = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.condValue);
	other.countUp = countUp;
	other.zeroInit = zeroInit;
	other.induction = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.induction);
    }
}
