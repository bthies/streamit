/*
 * VariableDeclaration.java: record class for variable declarations
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: VariableDeclaration.java,v 1.1 2002-06-12 17:57:41 dmaze Exp $
 */

package streamit.frontend.tojava;

public class VariableDeclaration
{
    public String type, name;
    
    public String getDecl() 
    {
        return type + " " + name;
    }

    public String getField(String indent)
    {
        return indent + "private " + getDecl() + ";\n";
    }
    
    public String getParam()
    {
        return "final " + getDecl();
    }
}
