
package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.HashMap;

class DataEstimate {

    public static int getTypeSize(CType type) {

	if (type.getTypeID() == CType.TID_INT) return 4;
	if (type.getTypeID() == CType.TID_FLOAT) return 4;
	if (type.getTypeID() == CType.TID_DOUBLE) return 8;
	if (type.getTypeID() == CType.TID_BOOLEAN) return 1;

	assert (1 == 0);
	return 0;
    }

    public static int filterDataEstimate(SIRFilter filter) {

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

	return data_size;
    }
}



