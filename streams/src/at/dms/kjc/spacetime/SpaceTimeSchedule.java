package at.dms.kjc.spacetime;

import java.util.*;

public class SpaceTimeSchedule {
    private ArrayList[][] tiles;
    private HashMap trace2loc; //Trace -> int[]{row,col,pos}
    private int rows;
    private int cols;
    private Trace[][][] sched; //Cached matrix representation of schedule

    public SpaceTimeSchedule(int row,int col) {
	tiles=new ArrayList[row][col];
	for(int i=0;i<row;i++)
	    for(int j=0;j<col;j++)
		tiles[i][j]=new ArrayList();
	trace2loc=new HashMap();
	rows=row;
	cols=col;
    }

    public void addHead(Trace trace,int row,int col) {
	ArrayList list=tiles[row][col];
	/*ArrayList positions=(ArrayList)trace2loc.get(trace);
	  if(positions==null) {
	  positions=new ArrayList();
	  trace2loc.put(trace,positions);
	  }
	  positions.add(new int[]{row,col,list.size()});*/
	trace2loc.put(trace,new int[]{row,col,list.size()});
	list.add(trace);
    }

    public void add(Trace trace,int row,int col) {
	ArrayList list=tiles[row][col];
	list.add(trace);
    }

    /*public void add(Trace trace,int[] row,int[] col) {
      int len=row.length;
      assert len>0&&len==col.length:"Bad Input To add(Trace trace,int[] row,int[] col)";
      ArrayList positions=(ArrayList)trace2loc.get(trace);
      if(positions==null) {
      positions=new ArrayList();
      trace2loc.put(trace,positions);
      }
      for(int i=0;i<len;i++) {
      int curRow=row[i];
      int curCol=col[i];
      ArrayList list=tiles[curRow][curCol];
      positions.add(new int[]{curRow,curCol,list.size()});
      list.add(trace);
      }
      }*/

    public Trace[] getTraces(int row,int col) {
	ArrayList temp=tiles[row][col];
	Trace[] out=new Trace[temp.size()];
	temp.toArray(out);
	return out;
    }

    public int[] getPosition(Trace trace) {
	//return Collections.unmodifiableList((ArrayList)trace2loc.get(trace));
	/*ArrayList temp=(ArrayList)trace2loc.get(trace);
	  int[][] out=new int[temp.size()][];
	  temp.toArray(out);
	  return out;*/
	return (int[])trace2loc.get(trace);
    }

    public Trace[][][] getSchedule() {
	if(sched==null) {
	    sched=new Trace[rows][cols][];
	    for(int i=0;i<rows;i++)
		for(int j=0;j<cols;j++) {
		    ArrayList list=tiles[i][j];
		    Trace[] array=new Trace[list.size()];
		    list.toArray(array);
		    sched[i][j]=array;
		}
	}
	return sched;
    }
}
