package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;

/**
 * Intended to reflect all the content of a filter needed by a backend
 * After these are constructed the old SIRFilters can be garbage collected
 * Will migrate methods from SIRFilter and SIRTwoStageFilter as needed
 * Unifies the representation of SIRFilter and SIRTwoStageFilter
 * Information is transferred from SIRFilter at construction time so
 * is immutable and a more compact representation
 * Truly flat. No pointers back to any previously existing structure.
 */
public class FilterContent {
    private String name;
    private SIRWorkFunction[] init,steady;
    private CType inputType,outputType;
    private int initMult, steadyMult, primePump;
    private JMethodDeclaration[] methods;
    private List paramList;
    private JMethodDeclaration initFunction;
    private boolean is2stage;
    private JFieldDeclaration[] fields;

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
    
    public FilterContent(SIRPhasedFilter filter,HashMap[] execCounts) {
	name=filter.getName();
	init=filter.getInitPhases();
	steady=filter.getPhases();
	inputType=filter.getInputType();
	outputType=filter.getOutputType();
	int[] ans=(int[])execCounts[0].get(filter);
	if(ans!=null)
	    initMult=ans[0];
	ans=(int[])execCounts[1].get(filter);
	if(ans!=null)
	    steadyMult=ans[0];
	methods=filter.getMethods();
	fields = filter.getFields();
	paramList=filter.getParams();
	initFunction = filter.getInit();
	is2stage = steady.length > 1;
    }
    
    public void setPrimePump(int pp) 
    {
	primePump = pp;
    }
    
    public int getPrimePump() 
    {
	return primePump;
    }

    public boolean isTwoStage() 
    {
	return is2stage;
    }
    
    public String toString() {
	return name;
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
    
    public int getSteadyMult() {
	return steadyMult;
    }

    public int getPushInt() {
	return steady[0].getPushInt();
    }

    public int getPopInt() {
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
