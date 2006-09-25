package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CodegenPrintWriter;

import java.util.HashMap;

/**
 * If KjcOptions.profiling is enabled, this class inserts timers
 * around certain sections of code.  The code sections are indicated
 * with SIRBeginMarker's and SIREndMarker's.  The time spent in the
 * indicated regions are output to a file at the end of execution.
 *
 * This interfaces with the "timer" and "proc_timer" classes in the
 * cluster library.
 */
public class InsertTimers extends InsertCounters implements Constants {
    /**
     * The maximum ID number given to any timer.
     */
    private static int MAX_ID = 0;
    /**
     * Map of names for code regions to associated ID. (String -&gt; Integer)
     */
    private static HashMap<String, Integer> nameToId = new HashMap<String, Integer>();
    /**
     * Inverse map of nameToId (Integer -> String)
     */
    private static HashMap<Integer, String> idToName = new HashMap<Integer, String>();

    public InsertTimers() {
        super();
    }

    public InsertTimers(CodegenPrintWriter p) {
        super(p);
    }

    /**
     * Returns number of timers that have appeared in output code.
     */
    public static int getNumTimers() {
        return MAX_ID;
    }

    /**
     * Returns the name for a given timer ID.
     */
    public static String getTimerName(int id) {
        Integer i = new Integer(id);
        assert idToName.containsKey(i) : "Looking up unknown timer.";
        return idToName.get(i);
    }

    /**
     * Returns name of base of timers array.
     */
    public static String getIdentifier() {
        return "timers";
    }

    /**
     * Returns complete C++ access expression for timer of the given
     * name.  (For example, "timers[10]").
     */
    private static String getAccess(String name) {
        // sanitize name 
        name = name.replace('\"', '\'');

        // lookup name
        int id;
        if (nameToId.containsKey(name)) {
            // cached name
            id = nameToId.get(name).intValue();
        } else {
            // unknown name
            id = MAX_ID++;
            nameToId.put(name, new Integer(id));
            idToName.put(new Integer(id), name);
        }
        return getIdentifier() + "[" + id + "]";
    }

    /******************************************************
     * Visitors
     ******************************************************/

    public void visitMarker(SIRMarker self) {
        // still emit comment
        super.visitMarker(self);

        if (KjcOptions.profile) {
            // start timer at begin marker
            if (self instanceof SIRBeginMarker) {
                String name = getAccess(((SIRBeginMarker)self).getName());
                p.print(name + ".start();\n");
            }
        
            // end timer at begin marker
            if (self instanceof SIREndMarker) {
                String name = getAccess(((SIREndMarker)self).getName());
                p.print(name + ".stop();\n");
            }
        }
    }
    
    /******************************************************
     * End visitors
     ******************************************************/
}
