/*
 * VariableDeclaration.java: record class for variable declarations
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: VariableDeclaration.java,v 1.3 2002-08-13 19:26:13 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Type;

public class VariableDeclaration
{
    public Type type;
    public String name;
    public Expression init;
    
    public String getDecl(NodesToJava n2j) 
    {
        return n2j.convertType(type) + " " + name;
    }

    public String getField(NodesToJava n2j, String indent)
    {
        return indent + "private " + getDecl(n2j) + ";\n";
    }
    
    public String getParam(NodesToJava n2j)
    {
        return "final " + getDecl(n2j);
    }
}
