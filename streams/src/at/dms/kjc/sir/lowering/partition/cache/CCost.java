package at.dms.kjc.sir.lowering.partition.cache;

class CCost {
    /**
     * The overall cost (includes icode cost, if the icode threshold
     * is exceeded.)  Taken as the sum over child nodes.
     */
    private final int cost;
    /**
     * The maximum icode size of any component within the container.
     */
    private final int iCodeSize;

    public CCost(int cost, int iCodeSize) {
	this.cost = cost;
	this.iCodeSize = iCodeSize;
    }

    public int getCost() {
	return cost;
    }

    public int getICodeSize() {
	return iCodeSize;
    }

    /**
     * Returns a conceptual "maximum cost" that will be greater than
     * everything.
     */
    public static CCost MAX_VALUE() {
	return new CCost(Integer.MAX_VALUE/2-1,
			 Integer.MAX_VALUE/2-1);
    }

    /**
     * Returns whether or not this is greather than <other>.
     */
    public boolean greaterThan(CCost other) {
	return this.cost > other.cost;
    }

    /**
     * Returns the combined cost of <cost1> and <cost2>, if they are
     * running in different partitions (that is, running in parallel
     * partitions, either in a splitjoin or pipeline).
     */
    public static CCost combine(CCost cost1, CCost cost2) {
	int cost = cost1.getCost() + cost2.getCost();
	int iCode = Math.max(cost1.getICodeSize(), cost2.getICodeSize());	

	// keep within range so that we can add again
	if (cost > Integer.MAX_VALUE/2-1) {
	    cost = Integer.MAX_VALUE/2-1;
	}
	if (iCode > Integer.MAX_VALUE/2-1) {
	    iCode = Integer.MAX_VALUE/2-1;
	}

	return new CCost(cost, iCode);
    }

    /**
     * Returns the sum of <cost1> and <cost2>, for the case in which
     * they are running in the same partition.  Includes a cost of
     * some fusion overhead.
     */
    public static CCost add(CCost cost1, CCost cost2) {
	// first just take sums
	int cost = cost1.getCost() + cost2.getCost();
	int iCode = cost1.getICodeSize() + cost2.getICodeSize();
	
	// if iCode threshold is exceeded, then bump up the other
	// costs.
	if (iCode > CachePartitioner.ICODE_THRESHOLD) {
	    cost = Integer.MAX_VALUE/2-1;
	}

	return new CCost(cost, iCode);
    }

    public boolean equals(Object o) {
	if (!(o instanceof CCost)) {
	    return false;
	}
	CCost other = (CCost)o;
	return other.cost==this.cost && other.iCodeSize==this.iCodeSize;
    }

    public String toString() {
	return "[cost=" + cost + ", iCode=" + iCodeSize + "]";
    }
}
