
package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.raw.Util;
import java.lang.*;
import java.util.HashMap;

public class DataEstimate {

    // SIRFilter -> Integer (size of global fields)
    private static HashMap saved_globals = new HashMap();
    private static HashMap saved_locals = new HashMap();

    public static int getTypeSize(CType type) {

	if (type.getTypeID() == CType.TID_VOID) return 0;
	if (type.getTypeID() == CType.TID_INT) return 4;
	if (type.getTypeID() == CType.TID_FLOAT) return 4;
	if (type.getTypeID() == CType.TID_DOUBLE) return 8;
	if (type.getTypeID() == CType.TID_BOOLEAN) return 1;

	assert (1 == 0);
	return 0;
    }


    public static int estimateDWS(SIROperator oper) {

	int globals;
	int locals;

	if (oper instanceof SIRFilter) {
	    
	    // this should cause calculation only when method is first called during
	    // code generation, on subsequent calls examine hashed values

	    SIRFilter filter = (SIRFilter)oper;
	    globals = DataEstimate.filterGlobalsSize(filter);

	    if (saved_locals.containsKey(filter)) {
		locals = ((Integer)saved_locals.get(filter)).intValue();
	    } else {
		CodeEstimate est = new CodeEstimate(filter);
		est.visitFilter(filter);
		locals = est.getLocalsSize();
		saved_locals.put(filter, new Integer(locals));
	    }

	    return globals + locals;
	}
	
	if (oper instanceof SIRJoiner) {
	    //SIRJoiner joiner = (SIRJoiner)oper;
	    //CType baseType = Util.getBaseType(Util.getJoinerType(node));
	    //int sum = joiner.getSumOfWeights();
	    //return DataEstimate.getTypeSize(baseType) * sum; 
	    return 32;
	}

	if (oper instanceof SIRSplitter) {
	    //SIRSplitter splitter = (SIRSplitter)oper;
	    //CType baseType = Util.getBaseType(Util.getOutputType(node));
	    //int sum = splitter.getSumOfWeights();
	    //return DataEstimate.getTypeSize(baseType) * sum; 
	    return 32;
	}

	return 0;
    }

    public static int estimateIOSize(SIROperator oper) {
	int id = NodeEnumerator.getSIROperatorId(oper);
	FlatNode node = NodeEnumerator.getFlatNode(id);
	Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
	int steady_int = 0;
	if (steady != null) steady_int = (steady).intValue();

	if (oper instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)oper;
	    CType input_type = filter.getInputType();
	    CType output_type = filter.getOutputType();
	    int pop_n = filter.getPopInt();
	    int peek_n = filter.getPeekInt();
	    int push_n = filter.getPushInt();
	    return (steady_int*pop_n+(peek_n-pop_n))*getTypeSize(input_type)+
		steady_int*push_n*getTypeSize(output_type);
	}
	
	if (oper instanceof SIRJoiner) {
	    SIRJoiner joiner = (SIRJoiner)oper;
	    CType baseType = Util.getBaseType(Util.getJoinerType(node));
	    int sum = joiner.getSumOfWeights();
	    return DataEstimate.getTypeSize(baseType)*sum*2; 
	}

	if (oper instanceof SIRSplitter) {
	    SIRSplitter splitter = (SIRSplitter)oper;
	    CType baseType = Util.getBaseType(Util.getOutputType(node));
	    int sum = splitter.getSumOfWeights();
	    return DataEstimate.getTypeSize(baseType)*sum*2; 
	}

	return 0;
    }


    public static int filterGlobalsSize(SIRFilter filter) {

	if (saved_globals.containsKey(filter)) {
	    return ((Integer)saved_globals.get(filter)).intValue();
	}

    	JFieldDeclaration[] fields = filter.getFields();
	int data_size = 0;

	for (int i = 0; i < fields.length; i++) {

	    CType type = fields[i].getType();
	    String ident = fields[i].getVariable().getIdent();
	    int size = 0;

	    if (type.isArrayType()) {

		String dims[] = ArrayDim.findDim((SIRFilter)filter, ident);
		CType base = ((CArrayType)type).getBaseType();
		
		if (dims != null && dims[0] != null) {
		    size = getTypeSize(base) * Integer.valueOf(dims[0]).intValue();
		}
	    } else {

		size = getTypeSize(type);
	    }

	    //System.out.println("filter: "+filter+" field: "+ident+" size: "+size);
	    data_size += size;
	}    

	saved_globals.put(filter, new Integer(data_size));
	
	return data_size;
    }
}



