package at.dms.kjc.spacedynamic;

public class Coordinate {
    private int row;
    private int column;

    public Coordinate(int r, int c) 
    {
	row = r;
	column = c;
    }
    
    public int getColumn() 
    {
	return column;
    }
    
    public int getRow() 
    {
	return row;
    }

    public String toString() {
	return "(" + row + ", " + column + ")";
    }
}

    
