/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Replace "reference" types with their actual types.  Currently, this
 * replaces <code>streamit.frontend.nodes.TypeStructRef</code> with
 * <code>streamit.frontend.nodes.TypeStruct</code>, where the former
 * is just "the user used a name as a type".  This looks through all
 * of the structure declarations and replaces types in variable and
 * field declarations and parameters with the correct actual structure
 * type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: NoRefTypes.java,v 1.7 2004-05-01 01:22:17 thies Exp $
 */
public class NoRefTypes extends FEReplacer
{
    // maps name of structure to TypeStruct
    private HashMap structs;

    private Type remapType(Type type)
    {
	if (type instanceof TypeArray) {
	    TypeArray ta = (TypeArray)type;
	    Type newBase = remapType(ta.getBase());
	    type = new TypeArray(newBase, ta.getLength());
	}
        if (type instanceof TypeStructRef)
        {
            TypeStructRef tsr = (TypeStructRef)type;
            String name = tsr.getName();
            if (!structs.containsKey(name))
                throw new UnrecognizedVariableException(name);
            type = (Type)structs.get(name);
        }
        return type;
    }

    public NoRefTypes()
    {
        structs = new HashMap();
    }
    
    public Object visitProgram(Program prog)
    {
        // Go through the list of structures, and notice them all.
        // We also need to rewrite the structures, in case there are
        // structs that contain structs.
        List newStructs = new java.util.ArrayList();
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
            structs.put(struct.getName(), struct);
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
                              remapType(st.getOut()),
                              remapType(st.getLoop()));
    }
}
