package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Intended to reflect all the content of a filter needed by a backend
 * After these are constructed the old SIRFilters can be garbage collected
 * Will migrate methods from SIRFilter and SIRTwoStageFilter as needed
 * Unifies the representation of SIRFilter and SIRTwoStageFilter
 * Information is transferred from SIRFilter at construction time
 * FilterContent is immutable and a more compact representation
 * Truly flat. No pointers back to any previously existing structure.
 */
public class FilterContent {
    private String name;
    private SIRWorkFunction[] init,steady;
    private CType inputType,outputType;
    private int initMult, steadyMult;//, primePump;
    private JMethodDeclaration[] methods;
    private List paramList;
    private JMethodDeclaration initFunction;
    private boolean is2stage;
    private JFieldDeclaration[] fields;
    //private LinearFilterRepresentation linear;
    //LinearRepresentation
    private double[] array;
    private double constant;
    private int popCount;
    private boolean linear;
    private boolean begin;
    private boolean end;
    private int pos;
    private int total;

    /*public FilterContent(String name,SIRWorkFunction[] init,SIRWorkFunction[] work,CType inputType,CType outputType,int initMult,int steadyMult,JMethodDeclaration[] methods,List paramList) {
      this.name=name;
      this.init=init;
      this.work=work;
      this.inputType=inputType;
      this.outputType=outputType;
      this.initMult=initMult;
      this.steadyMult=steadyMult;
      this.methods=methods;
      this.paramList=paramList;
      }*/
    
    public FilterContent(FilterContent content) {
	name=content.name;
	init=content.init;
	steady=content.steady;
	inputType=content.inputType;
	outputType=content.outputType;
	initMult=content.initMult;
	steadyMult=content.steadyMult;
	//primePump=content.primePump;
	methods=content.methods;
	paramList=content.paramList;
	initFunction=content.initFunction;
	is2stage=content.is2stage;
	fields=content.fields;
	array=content.array;
	constant=content.constant;
	popCount=content.popCount;
	linear=content.linear;
	begin=content.begin;
	end=content.end;
	pos=content.pos;
	total=content.total;
    }

    public FilterContent(SIRPhasedFilter filter) {
	name=filter.getName();
	init=filter.getInitPhases();
	steady=filter.getPhases();
	inputType=filter.getInputType();
	outputType=filter.getOutputType();
	methods=filter.getMethods();
	fields = filter.getFields();
	paramList=filter.getParams();
	initFunction = filter.getInit();
	is2stage = steady.length > 1;
	linear=false;
	total=1;
    }

    public FilterContent(UnflatFilter unflat) {
	SIRFilter filter=unflat.filter;
	name=filter.getName();
	inputType=filter.getInputType();
	outputType=filter.getOutputType();
	initMult=unflat.initMult;
	steadyMult=unflat.steadyMult;
	array=unflat.array;
	if(array!=null) {
	    linear=true;
	    constant=unflat.constant;
	    popCount=unflat.popCount;
	    int mod=array.length%popCount;
	    if(mod!=0) {
		final int len=array.length+popCount-mod;
		double[] temp=new double[len];
		System.arraycopy(array,0,temp,0,array.length);
		array=temp;
	    }
	    begin=true;
	    end=true;
	    pos=0;
	    total=1;
	    methods=filter.getMethods(); //Keep nonlinear rep
	    steady=filter.getPhases(); //Keep nonlinear rep
	    fields=filter.getFields();
	    paramList = filter.getParams();
	    initFunction=filter.getInit();
	    init=filter.getInitPhases();
	} else {
	    linear=false;
	    init=filter.getInitPhases();
	    steady=filter.getPhases();
	    /*int[] ans=(int[])execCounts[0].get(filter);
	      if(ans!=null)
	      initMult=ans[0];
	      ans=(int[])execCounts[1].get(filter);
	      if(ans!=null)
	      steadyMult=ans[0];
	    */
	    methods=filter.getMethods();
	    fields = filter.getFields();
	    paramList=filter.getParams();
	    initFunction = filter.getInit();
	    is2stage = steady.length > 1;
	    //if(lfa!=null)
	    //linear=lfa.getLinearRepresentation(filter);
	}
    }
    
    //public FilterContent(String name,double[] array,double constant) {
    //}
    
    /*public LinearFilterRepresentation getLinear() {
	//return linear;
	return null;
	}*/
    
    public boolean isLinear() {
	return linear;
    }

    public void setArray(double[] array) {
	//this.array=array;
	int mod=array.length%popCount;
	if(mod!=0) {
	    final int len=array.length+popCount-mod;
	    double[] temp=new double[len];
	    System.arraycopy(array,0,temp,0,array.length);
	    array=temp;
	} else
	    this.array=array;
    }

    public void setBegin(boolean begin) {
	this.begin=begin;
    }

    public boolean getBegin() {
	return begin;
    }

    public void setEnd(boolean end) {
	this.end=end;
    }

    public boolean getEnd() {
	return end;
    }

    public void setPos(int pos) {
	this.pos=pos;
    }

    public int getPos() {
	return pos;
    }

    public void setTotal(int total) {
	this.total=total;
    }

    public int getTotal() {
	return total;
    }

    public double[] getArray() {
	return array;
    }

    public double getConstant() {
	return constant;
    }

    public int getPopCount() {
	return popCount;
    }

    /*public void setLinear(LinearFilterRepresentation linear) {
      this.linear=linear;
      }*/

    public void setPrimePump(int pp) 
    {
	//primePump = pp;
	throw new RuntimeException("Deprecated");
    }
    
    /*public int getPrimePump() 
      {
      return primePump;
      }*/

    public boolean isTwoStage() 
    {
	return is2stage;
    }
    
    public String toString() {
	//return String.valueOf(System.identityHashCode(this));
	if(array==null)
	    return name;
	else {
	    return name+" ["+array.length+","+popCount+"]";
	    /*StringBuffer out=new StringBuffer(name);
	      out.append("[");
	      out.append(popCount);
	      out.append("][");
	      double[] array=this.array;
	      final int len=array.length;
	      if(len>0) {
	      out.append(array[0]);
	      for(int i=1;i<len;i++) {
	      out.append(",");
	      out.append(array[i]);
	      }
	      }
	      out.append("]");
	      out.append(begin);
	      out.append(",");
	      out.append(end);
	      out.append(",");
	      out.append(pos);
	      out.append(",");
	      out.append(total);
	      return out.toString();*/
	}
    }

    public String getName() {
	return name;
    }

    public CType getInputType() {
	return inputType;
    }

    public CType getOutputType () {
	return outputType;
    }

    public SIRWorkFunction[] getSteadyList() {
	return steady;
    }
    
    public SIRWorkFunction[] getInitList() {
	return init;
    }

    public JMethodDeclaration getWork() {
	return steady[0].getWork();
    }

    public JMethodDeclaration getInit() {
	return initFunction;
    }

    public int getInitMult() {
	return initMult;
    }
    
    public void multSteadyMult(int mult) 
    {
	steadyMult *= mult;
    }
    

    public int getSteadyMult() {
	return steadyMult;
    }

    public int getPushInt() {
	if(linear)
	    return 1;
	return steady[0].getPushInt();
    }

    public int getPopInt() {
	if(linear)
	    return getPopCount();
	return steady[0].getPopInt();
    }

    public int getPeekInt() {
	return steady[0].getPeekInt();
    }

    public int getInitPush() {
	return init[0].getPushInt();
    }

    public int getInitPop() {
	return init[0].getPopInt();
    }

    public int getInitPeek() {
	return init[0].getPeekInt();
    }

    public JMethodDeclaration[] getMethods() {
	return methods;
    }
    
    public JFieldDeclaration[] getFields() 
    {
	return fields;
    }
    

    public JMethodDeclaration getInitWork() {
        return init[0].getWork();
    }
    
    public List getParams() {
	return paramList;
    }
}
