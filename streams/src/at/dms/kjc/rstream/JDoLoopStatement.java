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
 * $Id: JDoLoopStatement.java,v 1.1 2004-08-13 18:06:13 mgordon Exp $
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
    
}
