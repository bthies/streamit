package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

class WorkInfo {

    /**
     * Returns total amount of work enclosed in a node.
     */
    private int totalWork;

    public WorkInfo(int totalWork) {
	this.totalWork = totalWork;
    }

    public String toString() {
	return "totalWork = " + totalWork;
    }

    public int totalWork() {
	return this.totalWork;
    }

    public void incrementWork(int work) {
	this.totalWork += work;
    }
}

