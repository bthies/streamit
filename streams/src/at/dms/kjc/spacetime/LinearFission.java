package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.util.Utils;

public class LinearFission {
    //Fiss linear FilterContent into num pieces
    public static FilterContent[] fiss(FilterContent content,int num) {
	//num/=2;
	if(!content.isLinear())
	    Utils.fail("Trying to Linear fiss nonlinear filter");
	double[] array=content.getArray();
	double constant=content.getConstant();
	final int len=(int)Math.ceil(((double)array.length)/num);
	final int realNum=(int)Math.ceil(((double)array.length)/len);
	System.out.println("NUM: "+num);
	System.out.println("LEN: "+len);
	System.out.println("REALNUM: "+realNum);
	FilterContent[] out=new FilterContent[realNum];
	for(int i=0;i<realNum;i++)
	    out[i]=new FilterContent(content);
	double[] temp=new double[len];
	if(realNum>1) {
	    System.arraycopy(array,0,temp,0,len);
	    out[0].setArray(temp);
	    out[0].setBegin(true);
	    out[0].setEnd(false);
	    out[0].setPos(realNum-1);
	    int idx=len;
	    for(int i=1;i<realNum-1;i++,idx+=len) {
		temp=new double[len];
		System.arraycopy(array,idx,temp,0,len);
		out[i].setArray(temp);
		out[i].setPos(realNum-i-1);
		out[i].setBegin(false);
		out[i].setEnd(false);
	    }
	    temp=new double[len];
	    System.arraycopy(array,idx,temp,0,array.length-idx);
	    out[realNum-1].setArray(temp);
	    out[realNum-1].setBegin(false);
	    out[realNum-1].setEnd(true);
	    out[realNum-1].setPos(0);
	} else {
	    System.arraycopy(array,0,temp,0,array.length);
	    out[0].setArray(temp);
	    out[0].setBegin(true);
	    out[0].setEnd(true);
	    out[0].setPos(0);
	}
	return out;
    }
}
