/*
 * InitFunction.java: container class to represent an init function
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InitFunction.java,v 1.1 2002-06-12 17:57:41 dmaze Exp $
 */

package streamit.frontend.tojava;
import java.util.List;
import java.util.Iterator;

public class InitFunction
{
    public String body;

    public String getText(int indent, List params, StreamType type,
                          WorkFunction work)
    {
        String paramAssigns = "";
        String t = getIndent(indent) + "public void init(";
        if (params != null)
        {
            Iterator iter = params.iterator();
            boolean first = true;
            while (iter.hasNext())
            {
                if (first) first = false; else t += ", ";
                VariableDeclaration param = (VariableDeclaration)iter.next();
                t += param.getParam();
                paramAssigns += getIndent(indent+1) + "this." + param.name +
                    " = " + param.name + ";\n";
            }
        }
        t += ") ";
        
        // Splice the required I/O declarations into the body.
        String body = this.body;
        if (body == null)
            body = "{\n" + getIndent(indent) + "}";
        
        // Find the first open-brace:
        int lbrace = body.indexOf('{') + 1;
        // For cleanliness, search forward after this to the first newline.
        for (; lbrace < body.length(); lbrace++)
        {
            char c = body.charAt(lbrace);
            if (c == '\n')
            {
                lbrace++;
                break;
            }
            if (!Character.isWhitespace(c))
                break;
        }

        String body_before = body.substring(0, lbrace);
        String body_after = body.substring(lbrace);
        
        // Generate the I/O declarations:
        String iodecls = "";
        if (type != null && !type.fromType.equals("void"))
        {
            iodecls += getIndent(indent + 1) + "input = new Channel(";
            iodecls += type.typeToClass(type.fromType);
            iodecls += ", " + work.popRate;
            if (!work.peekRate.equals("0"))
                iodecls += ", " + work.peekRate;
            iodecls += ");\n";
        }
        if (type != null && !type.toType.equals("void"))
        {
            iodecls += getIndent(indent + 1) + "output = new Channel(";
            iodecls += type.typeToClass(type.toType);
            iodecls += ", " + work.pushRate;
            iodecls += ");\n";
        }

        // Reassemble the body.
        body = body_before + paramAssigns + iodecls + body_after;

        t += body;
        t += "\n";
        return t;
    }

    public String getConstructor(int indent, List params, String classname)
    {
        if (params == null)
            return "";
        
        String superArgs = "";
        String t = getIndent(indent) + "public " + classname + "(";
        Iterator iter = params.iterator();
        boolean first = true;
        while (iter.hasNext())
        {
            if (first)
                first = false;
            else
            {
                t += ", ";
                superArgs += ", ";
            }
            VariableDeclaration param = (VariableDeclaration)iter.next();
            t += param.getParam();
            superArgs += param.name;
        }
        t += ") {\n";
        t += getIndent(indent+1) + "super(" + superArgs + ");\n";
        t += getIndent(indent) + "}\n";
        return t;
    }

  private String getIndent (int indent)
  {
    String result = "";
    int x = indent;
    while (x-- > 0) result = result + "    ";
    return result;
  }
}
