package at.dms.kjc.common;

import java.util.HashSet;
import java.util.Set;

import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.lowering.partition.WorkList;

public class CheckStatefulFilters {

    public static void doit(SIRStream str) {
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();

        Set<String> undeclared_stateful_filter_names = new HashSet<String>();
        Set<String> declared_stateless_filter_names = new HashSet<String>();

        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            boolean hasMutableState = StatelessDuplicate.hasMutableState(filter);
            boolean declaredStateful = filter.isStateful();
            if (hasMutableState && !declaredStateful) {
              undeclared_stateful_filter_names.add(filter.getIdent());
            } else if (!hasMutableState && declaredStateful) {
              declared_stateless_filter_names.add(filter.getIdent());
            }
        }

        if (declared_stateless_filter_names.size() > 0) {
            System.out.println("Warning: stateless filter(s) declared stateful: "
                               + declared_stateless_filter_names);
        }
        if (undeclared_stateful_filter_names.size() > 0) {
            assert (undeclared_stateful_filter_names.size() > 0) :
                "Error: stateful filter(s) not declared stateful: "
                + undeclared_stateful_filter_names;
            throw new RuntimeException(
                "Error: stateful filter(s) not declared stateful: "
                + undeclared_stateful_filter_names);
        }
    }
}
