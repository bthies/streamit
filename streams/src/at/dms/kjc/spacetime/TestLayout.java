package at.dms.kjc.spacetime;

public class TestLayout {
    private TestLayout() {}

    public static SpaceTimeSchedule layout(Trace[] traces,int row,int col) {
	SpaceTimeSchedule sched=new SpaceTimeSchedule(row,col);
	final int len=row*2+col*2-4;
	int[] rows=new int[len];
	int[] cols=new int[len];
	rows[0]=0;
	cols[0]=0;
	row--;
	col--;
	int idx=1;
	int posX=0;
	int posY=1;
	for(;idx<row;idx++) {
	    rows[idx]=posX;
	    cols[idx]=posY++;
	    //System.out.println("("+rows[idx]+","+cols[idx]+")");
	}
	for(int i=0;i<col;i++,idx++) {
	    rows[i]=posX++;
	    cols[i]=posY;
	    //System.out.println("("+rows[i]+","+cols[i]+")");
	}
	for(int i=0;i<row;i++,idx++) {
	    rows[i]=posX;
	    cols[i]=posY--;
	    //System.out.println("("+rows[i]+","+cols[i]+")");
	}
	for(int i=0;i<col;i++,idx++) {
	    rows[i]=posX--;
	    cols[i]=posY;
	    //System.out.println("("+rows[i]+","+cols[i]+")");
	}
	idx=0;
	final int tracesLen=traces.length;
	for(int i=0;i<tracesLen;i++) {
	    Trace trace=traces[i];
	    //System.out.println("Laying out: "+trace);
	    int size=trace.size();
	    TraceNode filter=trace.getHead().getNext();
	    //System.out.println(idx);
	    int curRow=rows[idx];
	    int curCol=cols[idx];
	    ((FilterTraceNode)filter).setXY(curRow,curCol);
	    sched.addHead(trace,curRow,curCol);
	    idx++;
	    if(idx==len)
		idx=0;
	    filter=filter.getNext();
	    for(int j=1;j<size;j++) {
		//System.out.println(idx);
		curRow=rows[idx];
		curCol=cols[idx];
		((FilterTraceNode)filter).setXY(curRow,curCol);
		sched.add(trace,curRow,curCol);
		idx++;
		if(idx==len)
		    idx=0;
		filter=filter.getNext();
	    }
	}
	return sched;
    }
}
