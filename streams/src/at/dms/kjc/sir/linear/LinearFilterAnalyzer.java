package at.dms.kjc.sir.linear;

//import at.dms.util.Utils;
import java.util.HashMap;
import java.util.Iterator;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * The LinearFilterAnalyzer visits all of the Filter definitions in
 * a StreamIT program. It determines which filters calculate linear
 * functions of their inputs, and for those that do, it keeps a mapping from
 * the filter name to the filter's matrix representation.
 *
 * $Id: LinearFilterAnalyzer.java,v 1.1 2002-08-14 18:13:19 aalamb Exp $
 **/
public class LinearFilterAnalyzer extends EmptyStreamVisitor {
    /** Mapping from filters to linear forms. never would have guessed that, would you? **/
    HashMap filtersToLinearForm;
    
    /** use findLinearFilters to instantiate a LinearFilterAnalyzer **/
    private LinearFilterAnalyzer() {
	this.filtersToLinearForm = new HashMap();
	checkRep();
    }
    
    /**
     * Main entry point -- searches the passed stream for
     * linear filters and calculates their associated matricies.
     **/
    public static LinearFilterAnalyzer findLinearFilters(SIRStream str) {
	System.out.println("aal--In linear filter visitor");
	LinearFilterAnalyzer lfv = new LinearFilterAnalyzer();
	IterFactory.createIter(str).accept(lfv);
	return lfv;
    }
    

    /** More or less get a callback for each stram **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	System.out.println("Visiting " + self.getIdent());
    }

    private void checkRep() {
	// make sure that all keys in FiltersToMatricies are strings, and that all
	// values are LinearForms.
	Iterator keyIter = this.filtersToLinearForm.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object o = keyIter.next();
	    if (!(o instanceof String)) {throw new RuntimeException("Non string key in LinearFilterAnalyzer");}
	    String key = (String)o;
	    Object val = this.filtersToLinearForm.get(key);
	    if (val == null) {throw new RuntimeException("Null value found in LinearFilterAnalyzer");}
	    if (!(val instanceof LinearForm)) {throw new RuntimeException("Non FilterMatric found in LinearFilterAnalyzer");}
	}
    }
}




/**
 * A visitor class that goes through all of the expressions in the work function
 * of a filter to determine if the filter is linear and if it is what matrix
 * corresponds to the filter.
 **/
class LinearFilterVisitor {
}
