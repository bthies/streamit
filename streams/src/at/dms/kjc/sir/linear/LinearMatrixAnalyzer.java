package at.dms.kjc.sir.linear;

//import at.dms.util.Utils;
import java.util.HashMap;
import java.util.Iterator;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * The LinearMatrixAnalyzer visits all of the Filter definitions in
 * a StreamIT program determining which filters calculate linear
 * combinations for their inputs. Those which calculate linear functions
 * of their inputs can be represented by a matrix multiply on an input vector
 * which consists of the pop'ed data items.
 *  The LinearMatrixAnalyzer maintains a map of filter names to their
 * corresponding matricies if it can find such a mapping.
 *
 * $Id: LinearMatrixAnalyzer.java,v 1.1 2002-08-12 21:12:33 aalamb Exp $
 **/
public class LinearMatrixAnalyzer extends EmptyStreamVisitor {
    /** Mapping from filters to matricies. never would have guessed that, would you? **/
    HashMap filtersToMatricies;
    
    /** use findLinearFilters to instantiate a LinearMatrixAnalyzer **/
    private LinearMatrixAnalyzer() {
	this.filtersToMatricies = new HashMap();
	checkRep();
    }
    
    /**
     * Main entry point -- searches the passed stream for
     * linear filters and calculates their associated matricies.
     **/
    public static LinearMatrixAnalyzer findLinearFilters(SIRStream str) {
	System.out.println("!!!!In linear filter visitor");
	LinearMatrixAnalyzer lfv = new LinearMatrixAnalyzer();
	IterFactory.createIter(str).accept(lfv);
	return lfv;
    }
    

    /** More or less get a callback for each stram **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	System.out.println("Visiting " + self.getIdent());
    }

    private void checkRep() {
	// make sure that all keys in FiltersToMatricies are strings, and that all
	// values are FilterMatricies.
	Iterator keyIter = this.filtersToMatricies.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object o = keyIter.next();
	    if (!(o instanceof String)) {throw new RuntimeException("Non string key in LinearMatrixAnalyzer");}
	    String key = (String)o;
	    Object val = this.filtersToMatricies.get(key);
	    if (val == null) {throw new RuntimeException("Null value found in LinearMatrixAnalyzer");}
	    if (!(val instanceof FilterMatrix)) {throw new RuntimeException("Non FilterMatric found in LinearMatrixAnalyzer");}
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
