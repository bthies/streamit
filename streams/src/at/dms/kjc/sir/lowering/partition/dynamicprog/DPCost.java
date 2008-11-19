package at.dms.kjc.sir.lowering.partition.dynamicprog;

class DPCost {
    private final long maxCost;
    private final long sumCost;
    private final long iCodeSize;

    public DPCost(long maxCost, long sumCost, long iCodeSize) {
        this.maxCost = maxCost;
        this.sumCost = sumCost;
        this.iCodeSize = iCodeSize;
    }

    public long getMaxCost() {
        return maxCost;
    }

    public long getSumCost() {
        return sumCost;
    }

    public long getICodeSize() {
        return iCodeSize;
    }
}
