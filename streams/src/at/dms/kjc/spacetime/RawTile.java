package at.dms.kjc.spacetime;

public class RawTile {
    private int X;
    private int Y;
    
    public RawTile(int row, int col) {
	X = row;
	Y = col;
    }

    public int getX() {
	return X;
    }

    public int getY() {
	return Y;
    }
}
