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

package streamit.library;

import java.io.*;
import java.util.*;

/**
 * This class records the elapsed time spent in each filter / operator.
 * It is active when the -profile option is passed to strc.
 * Note that this differs from Profile.java, which handles the --countops option.
 */
public class Timing {
    static long lastBegin; // time that the last operator started executing
    static Operator lastOp; // operator that was last running
    static long totalElapsedTime = 0; // the total elapsed time allotted to filters
    static HashMap typeToElapsed = new HashMap(); // records the elapsed time spent in each operator type
    // to handle pull scheduling (dynamic rates), keep a stack of currently executing operators
    static Stack executingOps = new Stack(); 

    /**
     * When -profile is turned on, these functions are called to
     * indicate the beginning and end of a given operator's execution.
     * (Note that they are also called when the operator is suspended,
     * in the case of dyanmic rates.)
     */
    public static void registerBegin(Operator op) {
        // don't profile connect filters
        if (op instanceof ChannelConnectFilter) return;

        // if we were executing someone else (pull schedule), suspend them
        if (lastOp!=null) {
            finishTiming(lastOp);
            executingOps.push(lastOp);
        }

        lastBegin = System.currentTimeMillis();
        lastOp = op;
    }

    /**
     * Like registerBegin, but registers the end (or interruption) of
     * an operator's execution.
     */
    public static void registerEnd(Operator op) {
        // don't profile connect filters
        if (op instanceof ChannelConnectFilter) return;

        finishTiming(op);

        // if we were previously executing some other filter, resume timing that one
        if (!executingOps.empty()) {
            lastOp = (Operator)executingOps.pop();
            lastBegin = System.currentTimeMillis();
        } else {
            lastOp = null;
        }
    }

    // finishes a single timing segment for the given operator, which
    // must be the one that is currently executing
    private static void finishTiming(Operator op) {
        // we should finish executing all operators that we start
        assert lastOp == op : "lastOp = " + lastOp + " but op = " + op;
        long elapsed = System.currentTimeMillis() - lastBegin;

        // record by class name -- this will collapsed separate
        // instances, which is a good thing for simplifying big graphs
        totalElapsedTime += elapsed;
        Class type = op.getClass();
        if (typeToElapsed.containsKey(type)) {
            elapsed += ((Long)typeToElapsed.get(type)).longValue();
        }
        typeToElapsed.put(type, new Long(elapsed));
    }

    /**
     * Writes the recorded profiling information to "profile.java.log"
     */
    public static void saveTimingData() {
        // sort the entries by decreasing amount of work done
        List entryList = new ArrayList(typeToElapsed.entrySet());
        Collections.sort(entryList, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Map.Entry e1 = (Map.Entry) o1;
                    Map.Entry e2 = (Map.Entry) o2;
                    Comparable c1 = (Comparable)e1.getValue();
                    Comparable c2 = (Comparable)e2.getValue();
                    // sort in reverse order
                    return c2.compareTo(c1);
                }
            });
        
        // pretty-print entries to file
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("profile.java.log")));

            for (Iterator it = entryList.iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                String type = ((Class)entry.getKey()).toString();
                Long value = (Long)entry.getValue();

                // format nicely -- e.g., "Saturation: 25.05%"
                String shortName = type.substring(type.lastIndexOf(" ")+1);
                double percentTime = 100.0* (double)value / (double)totalElapsedTime;
                // make sure "percentTime" includes a decimal point
                percentTime += 0.00000001;
                String longTime = "" + percentTime;
                String shortTime = longTime.substring(0, (int)Math.min(longTime.indexOf(".")+3,
                                                                       longTime.length()-1));
                
                out.println(shortName + ": " + shortTime + "%");
            }
            out.close();
            System.out.println("Profling data written to profile.java.log.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
    
