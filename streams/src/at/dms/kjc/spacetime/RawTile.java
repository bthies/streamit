package at.dms.kjc.spacetime;

public class RawTile {
    private int X;
    private int Y;
    private int tileNumber;
    //true if this tile has switch code
    private boolean switches;
    //true if this tile has compute code
    private boolean computes;

    private SwitchCodeStore switchCode;
    private ComputeCodeStore computeCode;

    private RawChip rawChip;

    public RawTile(int row, int col, RawChip rawChip) {
	this.rawChip = rawChip;
	X = row;
	Y = col;
	setTileNumber();
	computes = false;
	switches = false;
	switchCode = new SwitchCodeStore(this);
	computeCode = new ComputeCodeStore(this);
    }

    private void setTileNumber() {
	//because the simulator only simulates 4x4 or 8x8 we
	//have to translate the tile number according to these layouts
	int columns = 4;
	if (rawChip.getYSize() > 4)
	    columns = 8;
	tileNumber = (X * columns) + Y;
    }

    public RawChip getRawChip() {
	return rawChip;
    }

    public int getTileNumber() {
	return tileNumber;
    }

    public boolean hasComputeCode() {
	return computes;
    }

    public boolean hasSwitchCode() {
	return switches;
    }

    public SwitchCodeStore getSwitchCode() {
	return switchCode;
    }

    public ComputeCodeStore getComputeCode() {
	return computeCode;
    }

    public void setSwitchCode() {
	switches = true;
    }

    public void setComputeCode() {
	computes = true;
    }
    
    public int getX() {
	return X;
    }

    public int getY() {
	return Y;
    }
}
