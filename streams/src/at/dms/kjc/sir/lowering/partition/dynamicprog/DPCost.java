package at.dms.kjc.sir.lowering.partition.dynamicprog;

class DPCost {
    private final int maxCost;
    private final int sumCost;

    public DPCost(int maxCost, int sumCost) {
	this.maxCost = maxCost;
	this.sumCost = sumCost;
    }

    public int getMaxCost() {
	return maxCost;
    }

    public int getSumCost() {
	return sumCost;
    }
}
