package at.dms.kjc.sir.lowering.partition.dynamicprog;

class DPCost {
    private final int maxCost;
    private final int sumCost;
    private final int iCodeSize;

    public DPCost(int maxCost, int sumCost, int iCodeSize) {
	this.maxCost = maxCost;
	this.sumCost = sumCost;
	this.iCodeSize = iCodeSize;
    }

    public int getMaxCost() {
	return maxCost;
    }

    public int getSumCost() {
	return sumCost;
    }

    public int getICodeSize() {
	return iCodeSize;
    }
}
