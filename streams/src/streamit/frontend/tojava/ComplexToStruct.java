package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Replace primitive complex types with the <code>Complex</code>
 * structure.  This looks through all of the structure declarations
 * and replaces types in variable and field declarations and
 * parameters with the correct actual structure type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ComplexToStruct.java,v 1.1 2003-07-15 19:23:09 dmaze Exp $
 */
public class ComplexToStruct extends FEReplacer
{
    private TypeStruct cplxType;

    private Type remapType(Type type)
    {
        if (type.isComplex())
            return cplxType;
        if (type instanceof TypeArray)
        {
            TypeArray ta = (TypeArray)type;
            type = new TypeArray(remapType(ta.getBase()), ta.getLength());
        }
        return type;
    }

    public ComplexToStruct()
    {
        cplxType = null;
    }
    
    public Object visitProgram(Program prog)
    {
        // Go through the list of structures, and notice them all.
        // We also need to rewrite the structures, in case there are
        // structs that contain structs.
        List newStructs = new java.util.ArrayList();
        // Pass 1: find the struct named "Complex".
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            if (struct.getName().equals("Complex"))
                cplxType = struct;
        }
        // Pass 2: rewrite existing structures.
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct struct = (TypeStruct)iter.next();
            List newNames = new java.util.ArrayList();
            List newTypes = new java.util.ArrayList();
            for (int i = 0; i < struct.getNumFields(); i++)
            {
                String name = struct.getField(i);
                Type type = remapType(struct.getType(name));
                newNames.add(name);
                newTypes.add(type);
            }
            struct = new TypeStruct(struct.getContext(), struct.getName(),
                                    newNames, newTypes);
            newStructs.add(struct);
        }
        prog = new Program(prog.getContext(), prog.getStreams(), newStructs);
        return super.visitProgram(prog);
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        List newTypes = new java.util.ArrayList();
        for (int i = 0; i < field.getNumFields(); i++)
            newTypes.add(remapType(field.getType(i)));
        return new FieldDecl(field.getContext(), newTypes,
                             field.getNames(), field.getInits());
    }

    public Object visitFunction(Function func)
    {
        // Visit the parameter list, then let FEReplacer do the
        // rest of the work.
        List newParams = new java.util.ArrayList();
        for (Iterator iter = func.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Type type = remapType(param.getType());
            param = new Parameter(type, param.getName());
            newParams.add(param);
        }
        Type returnType = remapType(func.getReturnType());
        return super.visitFunction(new Function(func.getContext(),
                                                func.getCls(),
                                                func.getName(),
                                                returnType,
                                                newParams,
                                                func.getBody()));
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List newTypes = new java.util.ArrayList();
        for (int i = 0; i < stmt.getNumVars(); i++)
            newTypes.add(remapType(stmt.getType(i)));
        return new StmtVarDecl(stmt.getContext(), newTypes,
                               stmt.getNames(), stmt.getInits());
    }

    public Object visitStreamSpec(StreamSpec ss)
    {
        // Visit the parameter list, then let FEReplacer do the
        // rest of the work.
        List newParams = new java.util.ArrayList();
        for (Iterator iter = ss.getParams().iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Type type = remapType(param.getType());
            param = new Parameter(type, param.getName());
            newParams.add(param);
        }
        return super.visitStreamSpec(new StreamSpec(ss.getContext(),
                                                    ss.getType(),
                                                    ss.getStreamType(),
                                                    ss.getName(),
                                                    newParams,
                                                    ss.getVars(),
                                                    ss.getFuncs()));
    }

    public Object visitStreamType(StreamType st)
    {
        return new StreamType(st.getContext(),
                              remapType(st.getIn()),
                              remapType(st.getOut()));
    }
}
