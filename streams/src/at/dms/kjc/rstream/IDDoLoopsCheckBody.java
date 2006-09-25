package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Check the body of a for loop for various to see if it can
 * be converted to a do loop.
 *
 *
 * @author Michael Gordon
 * 
 */

class IDDoLoopsCheckBody extends SLIREmptyVisitor 
{
    private DoLoopInformation info;
    private HashSet<Object> varsToCheck;
    private HashSet<Object> varsAssigned;
    private boolean hasFields;
    private boolean hasMethods;

    /**
     * Check the body of a for loop to see if it can be converted to a 
     * do loop.  Make sure the induction var is not assigned in the body, 
     * make sure the condition and test are not altered in the loop.
     *
     * @param info The do loop information with all fields filled
     * @param body The body of the for loop
     *
     * @return True if all tests pass, otherwise false.
     */
    public static boolean check(DoLoopInformation info, JStatement body)
    {
        IDDoLoopsCheckBody check = new IDDoLoopsCheckBody(info, body);

        Iterator<Object> it;    
        //check for method calls
        body.accept(check);
        

        if (check.hasFields && check.hasMethods)
            return false;
        
        it  = check.varsAssigned.iterator();
        /*System.out.println("*** Vars assigned: ");
          while (it.hasNext()) {
          Object cur = it.next();
          System.out.println("  " + cur);
          }
          System.out.println("*** Vars assigned.  ");
        */

        //for all the variables we want to check,
        //make sure they are not assigned
        it = check.varsToCheck.iterator();
        while (it.hasNext()) {
            Object var = it.next();
            if (check.varsAssigned.contains(var)) {
                //System.out.println("Cannot formulate do loop, var " + var + 
                //         " assigned in loop ");
                return false;
            }
        
        }
        //System.out.println("Body okay");
        return true;
    }
    
    private IDDoLoopsCheckBody(DoLoopInformation info, JStatement body) 
    {
        this.info = info;
        varsToCheck = new HashSet<Object>();
        hasFields = false;
        hasMethods = false;
        findVarsToCheck();
        //get all the vars assigned in the body...
        varsAssigned =  VarsAssigned.getVarsAssigned(body);
    }
    
    private void findVarsToCheck() 
    {
        //add the induction variable
        varsToCheck.add(info.induction);
        //find all the vars to check if they are assigned,
        //anything used in the cond init or incr...
        StrToRStream.addAll(varsToCheck, VariablesDefUse.getVars(info.cond));
        StrToRStream.addAll(varsToCheck, VariablesDefUse.getVars(info.incr));

        Iterator<Object> it = varsToCheck.iterator();
        while (it.hasNext()) {
            Object cur = it.next();
            if (cur instanceof String) 
                hasFields = true;
        }
    }

    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        hasMethods = true;
        for (int i = 0; i < args.length; i++) 
            args[i].accept(this);    
    }
}
