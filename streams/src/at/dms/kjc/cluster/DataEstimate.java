
package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph.*;
import java.util.HashMap;
import at.dms.kjc.common.CommonUtils;

/**
 * Estimates the data working set of an operator. Currently
 * returns a magic number for splitters and joiners. For filters
 * add the size of globals with the size of locals.
 */

public class DataEstimate {

    // SIRFilter -> Integer (size of global fields)
    private static HashMap<SIRFilter, Integer> saved_globals = new HashMap<SIRFilter, Integer>();

    /**
     * Returns the size of a variable with given type.
     * @param type type of the variable
     * @return size of the variable
     */

    public static int getTypeSize(CType type) {

        if (type.getTypeID() == CType.TID_VOID) return 0;

        if (type.getTypeID() == CType.TID_BYTE) return 1;
        if (type.getTypeID() == CType.TID_SHORT) return 2;
        if (type.getTypeID() == CType.TID_CHAR) return 1;

        if (type.getTypeID() == CType.TID_INT) return 4;
        if (type.getTypeID() == CType.TID_LONG) return 4;

        if (type.getTypeID() == CType.TID_FLOAT) return 4;
        if (type.getTypeID() == CType.TID_DOUBLE) return 8;

        if (type.getTypeID() == CType.TID_BOOLEAN) return 1;
        if (type.getTypeID() == CType.TID_BIT) return 1;
        
        if (type instanceof CVectorType) {
            // KjcOptions.vectorize is a numeric value giving size of vector in bytes.
            return KjcOptions.vectorize;
        }

        if (type.isArrayType()) {
            CArrayType aty = (CArrayType)type;
            JExpression[] dims = aty.getDims();
            int numelts = 1;
            for (int i = 0; i < dims.length; i++) {
                assert dims[i] instanceof JIntLiteral : "Can not calculate indeger array dimension for" + dims[i].toString();
                numelts *= ((JIntLiteral)dims[i]).intValue();
            }
            return numelts * getTypeSize(aty.getBaseType());
        }
        
        if (type.isClassType()) {
            if (type instanceof CNullType) {
                return 0;
            }
            if (type.toString().equals("java.lang.String")) {
                return 4;  // return something, don't look at internals of String type. 
            }
            // Assume class is something we defined: size is sum of size of fields.
            // (not true of a builtin class)
            int size = 0;
            CClass c = ((CClassType)type).getCClass();
            for (CField f : c.getRawFields().values()) {
                size += getTypeSize(f.getType());
            }

            //System.err.println("Class type: ["+type+"] size: "+size);
        
            return size;
        }

        assert false : "DataEstimate: unknown type ["+type+" TID: "+type.getTypeID()+" stack_size: "+type.getSize()+"]";
        return 0;
    }

    /**
     * Estimates data working set for a {@link SIROperator}
     * @param oper the operator
     * @return estimated size of data working set
     */

    public static int estimateDWS(SIROperator oper) {

        int globals;
        int locals;

        if (oper instanceof SIRFilter) {
        
            // this should cause calculation only when method is first called during
            // code generation, on subsequent calls examine hashed values

            SIRFilter filter = (SIRFilter)oper;
            globals = DataEstimate.filterGlobalsSize(filter);
            locals = CodeEstimate.estimateLocals(filter);
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

    /**
     * Estimates the size of data buffers needed to store input and
     * output for a single execution of a {@link SIROperator}
     * @param oper the operator
     * @return estimated size of data buffers
     */

    public static int estimateIOSize(SIROperator oper) {
        int id = NodeEnumerator.getSIROperatorId(oper);
        FlatNode node = NodeEnumerator.getFlatNode(id);
        Integer steady = ClusterBackend.steadyExecutionCounts.get(node);
        int steady_int = 0;
        if (steady != null) steady_int = (steady).intValue();

        if (oper instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)oper;
            CType input_type = filter.getInputType();
            CType output_type = filter.getOutputType();
            int pop_n = filter.getPopInt();
            //int peek_n = filter.getPeekInt();
            int push_n = filter.getPushInt();
            return steady_int*pop_n*getTypeSize(input_type)+
                //(peek_n-pop_n)*getTypeSize(input_type)+
                steady_int*push_n*getTypeSize(output_type);
        }
    
        if (oper instanceof SIRJoiner) {
            SIRJoiner joiner = (SIRJoiner)oper;
            CType baseType = CommonUtils.getBaseType(CommonUtils.getJoinerType(node));
            int sum = joiner.getSumOfWeights();
            return DataEstimate.getTypeSize(baseType)*sum*2; 
        }

        if (oper instanceof SIRSplitter) {
            SIRSplitter splitter = (SIRSplitter)oper;
            CType baseType = CommonUtils.getBaseType(CommonUtils.getOutputType(node));
            int sum = splitter.getSumOfWeights();
            return DataEstimate.getTypeSize(baseType)*sum*2; 
        }

        return 0;
    }


    /**
     * Returns the size of globals for a {@link SIRFilter}. 
     * Also implements a cache, so that if size of globals has 
     * been calculated we just look up the value in the cache.
     * @param filter the filter
     * @return size of globals (possibly cached)
     */

    public static int filterGlobalsSize(SIRFilter filter) {

        if (saved_globals.containsKey(filter)) {
            return saved_globals.get(filter).intValue();
        }

        int data_size = computeFilterGlobalsSize(filter);
        saved_globals.put(filter, new Integer(data_size));
        return data_size;
    }

    /**
     * Computes the size of globals for a {@link SIRFilter}. 
     * @param filter the filter
     * @return computed size of globals
     */

    public static int computeFilterGlobalsSize(SIRFilter filter) {

        JFieldDeclaration[] fields = filter.getFields();
        int data_size = 0;

        for (int i = 0; i < fields.length; i++) {

            CType type = fields[i].getType();
            String ident = fields[i].getVariable().getIdent();
            int size = 0;

            if (type.isArrayType()) {

                String dims[] = (new FlatIRToCluster()).makeArrayStrings(((CArrayType)type).getDims());
                CType base = ((CArrayType)type).getBaseType();
        
                if (dims != null) {
                    size = getTypeSize(base);
                    for (int y = 0; y < dims.length; y++) {
                        if (dims[y] == null) break;
                        try {
                            size *= Integer.valueOf(dims[y]).intValue();
                        } catch (NumberFormatException ex) {
                            System.out.println("Warning! Could not estimate size of an array: "+ident);
                        }
                    }
                }

            } else {

                size = getTypeSize(type);
            }

            //System.out.println("filter: "+filter+" field: "+ident+" size: "+size);
            data_size += size;
        }    

        return data_size;
    }
}



