/*
 * InitFunction.java: container class to represent an init function
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InitFunction.java,v 1.9 2002-08-20 20:04:30 dmaze Exp $
 */

package streamit.frontend.tojava;

import java.util.Iterator;
import java.util.List;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;

public class InitFunction
{
    public String body;

    public String getText(int indent, List params, List fields,
                          StreamType type, WorkFunction work,
                          NodesToJava n2j, SymbolTable symtab)
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
                t += param.getParam(n2j);
                paramAssigns += getIndent(indent+1) + "this." + param.name +
                    " = " + param.name + ";\n";
            }
        }
        if (fields != null)
        {
            Iterator iter = fields.iterator();
            while (iter.hasNext())
            {
                VariableDeclaration param = (VariableDeclaration)iter.next();
                if (param.type.isComplex() ||
                    !(param.type instanceof TypePrimitive))
                    paramAssigns += getIndent(indent+1) + "this." +
                        param.name + " = " +
                        n2j.makeConstructor(param.type) + ";\n";
                // Add a variable assignment, if we have an initializer:
                if (param.init != null)
                    paramAssigns += getIndent(indent+1) +
                        n2j.doAssignment(new ExprVar(null, param.name),
                                         param.init, symtab) + ";\n";
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
        if (type != null &&
            !(type.getIn() instanceof TypePrimitive &&
              ((TypePrimitive)type.getIn()).getType() ==
              TypePrimitive.TYPE_VOID))
        {
            iodecls += getIndent(indent + 1) + "input = new Channel(";
            iodecls += n2j.typeToClass(type.getIn());
            iodecls += ", " + work.popRate;
            if (!work.peekRate.equals("0"))
                iodecls += ", " + work.peekRate;
            iodecls += ");\n";
        }
        if (type != null &&
            !(type.getOut() instanceof TypePrimitive &&
              ((TypePrimitive)type.getOut()).getType() ==
              TypePrimitive.TYPE_VOID))
        {
            iodecls += getIndent(indent + 1) + "output = new Channel(";
            iodecls += n2j.typeToClass(type.getOut());
            iodecls += ", " + work.pushRate;
            iodecls += ");\n";
        }

        // Reassemble the body.
        body = body_before + paramAssigns + iodecls + body_after;

        t += body;
        t += "\n";
        return t;
    }

    public String getConstructor(int indent, List params, String classname,
                                 NodesToJava n2j)
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
            t += param.getParam(n2j);
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
