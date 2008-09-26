package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.spacetime.SafeFileReaderWriterPositions;

/**
 * Intended to reflect all the content of a filter needed by a
 * backend. After these are constructed the old SIRFilters can be
 * garbage collected. Can migrate methods from SIRFilter and
 * SIRTwoStageFilter as needed. Unifies the representation of
 * SIRFilter and SIRTwoStageFilter. Information is transferred from
 * SIRFilter at construction time. FilterContent is immutable and a
 * more compact representation. Truly flat. No pointers back to any
 * previously existing structure.
 * 
 * @author jasperln
 */
public class FilterContent implements SIRCodeUnit, at.dms.kjc.DeepCloneable {
    /** Static unique id used in new name if one FilterContent created from another. */
    private static int unique_ID = 0; 
    /** The unique id given to this FilterContent for use in constructing names */
    protected int my_unique_ID;
    /** Filter name */
    private String name; 
    /** PreWork and Work method declarations */
    protected JMethodDeclaration[] prework, steady; 
    /** Input and output types */
    protected CType inputType,outputType; 
    /** Multiplicities from scheduler */
    private int initMult, steadyMult; 
    /** Other method declarations */
    private JMethodDeclaration[] methods;
    /** Init function for filter */
    protected JMethodDeclaration initFunction; 
    /** Is true when two-stage filter */
    private boolean is2stage; 
    /** Field declarations */  
    private JFieldDeclaration[] fields; 
    /** For linear filters, the pop count **/
    private int popCount;
    /** For linear filters, the peek count **/
    private int peek;

    /////////////////////////
    //Linear Representation
    /////////////////////////
    /** true if the filter is linear **/
    private boolean linear;
    /** if the filter is linear, then array stores the A in Ax + b **/
    private double[] array;
    /** if the filter is linear, then constant holds the b in Ax + b **/
    private double constant;
    /** true if this filter is linear and it is the first filter of
        the fissed filters representing an original linear filter **/
    private boolean begin;
    /** true if this filter is linear and it is the last filter of the
        fissed filters representing an original linear filter **/
    private boolean end;
    /** if this filter is linear, the position of the filter in the pipeline
        of fissed filters that were generated from the original linear filter **/
    private int pos;
    /** if this filter is linear, the total number of filters in the pipeline of 
        fissed filters that were generated from the original linear filter **/
    private int total;
    
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected FilterContent() {
    }

    /**
     * Copy constructor for FilterContent
     * @param content The FilterContent to copy.
     */
    public FilterContent(FilterContent content) {
        my_unique_ID = unique_ID++;
        name = content.name + my_unique_ID;
        prework = content.prework;
        steady  =  content.steady;
        inputType = content.inputType;
        outputType = content.outputType;
        initMult = content.initMult;
        steadyMult = content.steadyMult;
        methods = content.methods;
        //paramList = content.paramList;
        initFunction = content.initFunction;
        is2stage = content.is2stage;
        fields = content.fields;
        array = content.array;
        constant = content.constant;
        popCount = content.popCount;
        peek = content.peek;
        linear = content.linear;
        begin = content.begin;
        end = content.end;
        pos = content.pos;
        total = content.total;
    }

    /**
     * Constructor FilterContent from SIRPhasedFilter.
     * @param filter SIRPhasedFilter to construct from.
     */
    public FilterContent(SIRPhasedFilter filter) {
        my_unique_ID = unique_ID++;
        name = filter.getName();
        prework = filter.getInitPhases();
        steady = filter.getPhases();
        inputType = filter.getInputType();
        outputType = filter.getOutputType();
        methods = filter.getMethods();
        fields  =  filter.getFields();
        //paramList = filter.getParams();
        initFunction  =  filter.getInit();
        assert prework.length < 2 && steady.length == 1;
        //if this filter is two stage, then it has the 
        //init work function as the only member of the init phases
        is2stage = prework.length == 1;
        //is2stage = steady.length > 1;
        linear = false;
        //total=1;
    }

    /**
     * Constructor FilterContent from UnflatFilter.
     * @param unflat UnflatFilter to construct from.
     */
    public FilterContent(UnflatFilter unflat) {
        SIRFilter filter = unflat.filter;
        //assert filter != null : unflat.toString();
        if (filter == null) {
            UnflatFilter upstream = unflat.in[0].src;
            CType type = upstream.filter.getOutputType();
            filter = SafeFileReaderWriterPositions.makeIdentityFilter(type);
            RenameAll.renameAllFilters(filter);
            unflat.filter = filter;
            unflat.initMult = calculateIDInitMult(unflat);
            unflat.steadyMult = calculateIDSteadyMult(unflat);
            System.out.println("Creating ID with " + unflat.initMult + " iniMult " + 
                    unflat.steadyMult + " steadyMult");
        }
        
        my_unique_ID = unique_ID++;
        name = filter.getName();
        inputType = filter.getInputType();
        outputType = filter.getOutputType();

        initMult = unflat.initMult;
        steadyMult = unflat.steadyMult;
        array = unflat.array;
        
        //we have found a linear filter if it has an array
        if (array != null) { 
            //removed by Mgordon
            //int reg=20-array.length/unflat.popCount-1;
            //if(array.length<=reg) {
            for (int i = 0; i < array.length; i++)
                System.out.println("A[" + i + "] = " + array[i]);

            linear = true;
            constant = unflat.constant;
            popCount = unflat.popCount;
            assert popCount>0 :"Pop count on linear filter is not > 0";
            peek = array.length;
            int mod = array.length%popCount;
            if(mod!=0) {
                final int len = array.length+popCount-mod;
                double[] temp = new double[len];
                System.arraycopy(array,0,temp,0,array.length);
                array = temp;
            }
            begin = true;
            end = true;
            pos = 0;
            total = 1;
            //methods=filter.getMethods(); //Keep nonlinear rep
            //steady=filter.getPhases(); //Keep nonlinear rep
            //fields=filter.getFields(); //Keep nonlinear rep
            //paramList = filter.getParams(); //Keep nonlinear rep
            //initFunction=filter.getInit(); //Keep nonlinear rep
            //init=filter.getInitPhases(); //Keep nonlinear rep
            /*}  Mgordon: Commenting out some weird stuff!
              else {
              linear=false;
              init=filter.getInitPhases();
              steady=filter.getPhases();
              methods=filter.getMethods();
              fields = filter.getFields();
              paramList=filter.getParams();
              initFunction = filter.getInit();
        
              assert init.length < 2 && steady.length == 1;
              //if this filter is two stage, then it has the 
              //init work function as the only member of the init phases
              is2stage = init.length == 1;
        
              //        is2stage = steady.length > 1;
              }*/
        } else {
            linear = false;
            prework = filter.getInitPhases();
            steady = filter.getPhases();
            methods = filter.getMethods();
            fields = filter.getFields();
            //paramList = filter.getParams();
            initFunction = filter.getInit();
            assert prework.length < 2 && steady.length == 1;
            //if this filter is two stage, then it has the 
            //init work function as the only member of the init phases
            is2stage = prework.length == 1;

            //is2stage = steady.length > 1;
        }
    }
    
    private int calculateIDInitMult(UnflatFilter id) {
        int initMult = 0;
        if (id.in.length == 1) {
            UnflatFilter upstream = id.in[0].src;
            initMult = upstream.filter.getPushInt() * upstream.initMult;
            if (upstream.filter instanceof SIRTwoStageFilter) {
                initMult -= upstream.filter.getPushInt();
                initMult += ((SIRTwoStageFilter)upstream.filter).getInitPushInt();
            }
        } else {
            //this identity filter joins, 
            //find the multiplicity of the join, but finding the min multiplicity
            //that is possible given the multiplicity of the upstream guys
            int minMult = 0;
            Iterator<UnflatEdge> edges = id.getInEdgeSet().iterator();
            while (edges.hasNext()) {
                UnflatEdge edge = edges.next();
                UnflatFilter upstream = edge.src;
                System.out.println(upstream);
                int itemsProduced = upstream.initMult * upstream.filter.getPushInt();
                if (upstream.filter instanceof SIRTwoStageFilter) {
                    itemsProduced -= upstream.filter.getPushInt();
                    itemsProduced += ((SIRTwoStageFilter)upstream.filter).getInitPushInt();
                }
                int mult = 
                    (int)Math.floor(((double)itemsProduced) / ((double)id.inWeight(edge)));
                if (mult < minMult)
                    minMult = mult;
            }
            //the init mult is the min mult * the total weight
            initMult = (minMult * id.totalInWeights());
        }
        return initMult;
    }
    
    private int calculateIDSteadyMult(UnflatFilter id) {
        int steadyMult = 0;
        if (id.in.length == 1) {
            UnflatFilter upstream = id.in[0].src;
            steadyMult = upstream.filter.getPushInt() * upstream.steadyMult;
        } else {
            //this identity filter joins, 
            //find the multiplicity of the join, but finding the min multiplicity
            //that is possible given the multiplicity of the upstream guys
            int mult = -1;
            Iterator<UnflatEdge> edges = id.getInEdgeSet().iterator();
            while (edges.hasNext()) {
                UnflatEdge edge = edges.next();
                UnflatFilter upstream = edge.src;
                int itemsProduced = upstream.steadyMult * upstream.filter.getPushInt();
                int myMult = (int)Math.floor(((double)itemsProduced) / ((double)id.inWeight(edge)));
                /*if (mult != -1)
                    assert myMult == mult; */
                mult = myMult;
            }
            //the init mult is the min mult * the total weight
            steadyMult = (mult * id.totalInWeights());
        }
        return steadyMult;
    }
    
    /**
     * Return if filter is linear.
     */
    public boolean isLinear() {
        return linear;
    }

    /**
     * Set array for linear filters. The A in Ax+b.
     * @param array The array to set.
     */
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

    /**
     * Set begin for linear filters. True if this filter is the first
     * filter of the fissed filters representing the original linear
     * filter.
     * @param begin The boolean to set for begin.
     */
    public void setBegin(boolean begin) {
        this.begin=begin;
    }

    /**
     * Returns true if this filter is the first filter of the fissed
     * filters representing the original linear filter.
     */
    public boolean getBegin() {
        return begin;
    }

    /**
     * Set end for linear filters. True if this filter is the last
     * filter of the fissed filters representing the original linear
     * filter.
     * @param end The boolean to set for end.
     */
    public void setEnd(boolean end) {
        this.end=end;
    }

    /**
     * Returns true if this filter is the last filter of the fissed
     * filters representing the original linear filter.
     */
    public boolean getEnd() {
        return end;
    }

    /**
     * Set position for linear filters. The position of the filter in
     * the pipeline of fissed filters that were generated from the
     * original linear filter.
     * @param pos The int to set for pos.
     */
    public void setPos(int pos) {
        this.pos=pos;
    }

    /**
     * Return the position of the linear filter in the pipeline of
     * fissed filters that were generated from the original linear
     * filter.
     */
    public int getPos() {
        return pos;
    }

    /**
     * Set total number for linear filters. The total number of
     * filters in the pipeline of fissed filters that were generated
     * from the original linear filter.
     * @param total The int to set for total.
     */
    public void setTotal(int total) {
        this.total=total;
    }

    /**
     * Return the total number of linear filters in the pipeline of
     * fissed filters that were generated from the original linear
     * filter.
     */
    public int getTotal() {
        return total;
    }

    /**
     * For linear filters, returns the array A in Ax+b that represents
     * the filter.
     */
    public double[] getArray() {
        return array;
    }

    /**
     * For linear filters, returns the constant b in Ax+b that
     * represents the filter.
     */
    public double getConstant() {
        return constant;
    }

    /**
     * Returns the pop count of this filter.
     */
    public int getPopCount() {
        return popCount;
    }

    /**
     * Returns the peek amount of this filter.
     */
    public int getPeek() {
        return peek;
    }

    /**
     * Returns if this filter is two-stage or not.
     */
    public boolean isTwoStage() 
    {
        return is2stage;
    }
    
    /**
     * Returns string representation of this FilterContent.
     */
    public String toString() {
        if(array==null)
            return name;
        else {
            if(true)
                return name+" ["+array.length+","+popCount+"]";
            else {
                StringBuffer out=new StringBuffer(name);
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
                //out.append(",");
                //out.append(total);
                return out.toString();
            }
        }
    }

    /**
     * Returns filter name of this FilterContent.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns input type of this FilterContent.
     */
    public CType getInputType() {
        return inputType;
    }

    /**
     * Returns output type of this FilterContent.
     */
    public CType getOutputType () {
        return outputType;
    }

    /**
     * Returns list of steady state method declarations.
     */
    public JMethodDeclaration[] getSteadyList() {
        return steady;
    }
    
    /**
     * Returns array of initialization state methods.
     */
    public JMethodDeclaration[] getPrework() {
        return prework;
    }

    /**
     * Returns work function.
     */
    public JMethodDeclaration getWork() {
        if(steady!=null)
            return steady[0];
        else
            return null;
    }

    /**
     * Returns init function.
     */
    public JMethodDeclaration getInit() {
        return initFunction;
    }

    /**
     * Returns multiplicity of init schedule.
     */
    public int getInitMult() {
        return initMult;
    }
    
    /**
     * Multiplies the steady state schedule by mult amount.
     * @param mult The number of times to multiply the steady state schedule by.
     */
    public void multSteadyMult(int mult) 
    {
        steadyMult *= mult;
    }
    
    /**
     * Returns the multiplicity of steady state schedule.
     */
    public int getSteadyMult() {
        return steadyMult;
    }

    /**
     * Set the name of this filter to n;
     *
     * @param n The new name of this filter
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * Set the init multiplicity of this fitler to im;
     * 
     * @param im The new init multiplicity.
     */
    public void setInitMult(int im) {
        initMult = im;
    }
   
    
    /** 
     * return the number of items produced in the init stage.
     * 
     * @return the number of items produced in the init stage.
     */
    public int initItemsPushed() {
        int items = steady[0].getPushInt() * initMult;
        if (isTwoStage()) {
            items -= steady[0].getPushInt();
            items += getPreworkPush();
        }
        return items;
    }
    
    /**
     * Return the number of items needed for this filter to fire 
     * in the initialization schedule.
     * 
     * @return the number of items needed for this filter to fire 
     * in the initialization schedule.
     */
    public int initItemsNeeded() {
        if (getInitMult() < 1)
            return 0;
        //the number of items needed after the prework function
        //executes and before the work function executes
        int bottomPeek = 0;
        //the init mult assuming everything is a two stage
        int myInitMult = getInitMult();
        int initPeek = 0;
        
        if (isTwoStage()) { 
            bottomPeek = Math.max(0, peek - (getPreworkPeek() - getPreworkPop()));
            //can't call init peek on non-twostages
            initPeek = getPreworkPeek();
        }
        else //if it is not a two stage, fake it for the following calculation
            myInitMult++;
            
        //(prePeek + bottomPeek + Math.max((initFire - 2), 0) * pop);
        return 
            initPeek + bottomPeek + Math.max((myInitMult - 2), 0) * 
                 getPopInt(); 
    }
    
    /**
     * Set the steady multiplicity of this filter to sm.
     * 
     * @param sm The new steady multiplicity.
     */
    public void setSteadyMult(int sm) {
        steadyMult = sm;
    }
    
    /**
     * Returns push amount.
     */
    public int getPushInt() {
        if(linear)
            return 1;
        return steady[0].getPushInt();
    }

    /**
     * Returns pop amount.
     */
    public int getPopInt() {
        if(linear)
            return getPopCount();
        return steady[0].getPopInt();
    }

    /**
     * Returns peek amount.
     */
    public int getPeekInt() {
        return steady[0].getPeekInt();
    }

    /**
     * Returns push amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPush() {
        return prework[0].getPushInt();
    }

    /**
     * Returns pop amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPop() {
        return prework[0].getPopInt();
    }

    /**
     * Returns peek amount of init stage.
     * result may be garbage or error if !isTwoStage()
     */
    public int getPreworkPeek() {
        return prework[0].getPeekInt();
    }

    /**
     * Returns method declarations.
     */
    public JMethodDeclaration[] getMethods() {
        return methods;
    }
    
    /**
     * Returns field declarations.
     */
    public JFieldDeclaration[] getFields() 
    {
        return fields;
    }
    
    /**
     * Returns init-work method declaration.
     * result may be garbage or error if !isTwoStage()
     */
    public JMethodDeclaration getInitWork() {
        return prework[0];
    }
    
    /**
     * Set the init work of this filter to meth.
     * result may be garbage or error if !isTwoStage()
     * 
     * @param meth The new init work method.
     */
    public void setPrework(JMethodDeclaration meth) {
	if(meth == null) {
	    is2stage = false;
	    prework = new JMethodDeclaration[0];
	}
	else {
	    if (prework == null || prework.length == 0) {
		prework = new JMethodDeclaration[1];
	    }

	    is2stage = true;
	    prework[0] = meth;
	    addMethod(meth);
	}
    }

    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void addField(JFieldDeclaration field) {
        throw new AssertionError("should not call");
    }

    /** but subclasses can add fields */
    protected void addAField(JFieldDeclaration field) {
        JFieldDeclaration[] newFields = 
            new JFieldDeclaration[fields.length + 1];
        for (int i=0; i<fields.length; i++) {
            newFields[i] = fields[i];
        }
        newFields[fields.length] = field;
        this.fields = newFields;
    }
    
    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void addFields(JFieldDeclaration[] fields) {
        throw new AssertionError("should not call");
    }

    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void addMethod(JMethodDeclaration method) {
        JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length + 1];
        for (int i = 0; i < methods.length; i++)
            newMethods[i] = methods[i];
        newMethods[newMethods.length - 1] = method;
        methods = newMethods;
    }

    /** but subclasses can add methods */
    protected void addAMethod(JMethodDeclaration method) {
        JMethodDeclaration[] newMethods = 
            new JMethodDeclaration[methods.length + 1];
        for (int i=0; i<methods.length; i++) {
            newMethods[i] = methods[i];
        }
        newMethods[methods.length] = method;
        this.methods = newMethods;
    }
    
    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void addMethods(JMethodDeclaration[] methods) {
        throw new AssertionError("should not call");
    }

    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void setFields(JFieldDeclaration[] fields) {
        this.fields = fields;
        //throw new AssertionError("should not call");
    }

    /**
     * Method exists to allow SIRCodeUnit interface but should not be called.
     */
    public void setMethods(JMethodDeclaration[] methods) {
        throw new AssertionError("should not call");
    }
    
    /**
     * Allow subclasses to replace methods array
     */
    protected void setTheMethods(JMethodDeclaration[] methods) {
        this.methods = methods;
    }
    
    
    
    /**
     * Returns list of paramters.
     
    public List getParams() {
        return paramList;
    }
    */

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.FilterContent other = new at.dms.kjc.slicegraph.FilterContent();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.FilterContent other) {
        other.my_unique_ID = this.my_unique_ID;
        other.name = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.name);
        other.prework = (at.dms.kjc.JMethodDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.prework);
        other.steady = (at.dms.kjc.JMethodDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.steady);
        other.inputType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.inputType);
        other.outputType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.outputType);
        other.initMult = this.initMult;
        other.steadyMult = this.steadyMult;
        other.methods = (at.dms.kjc.JMethodDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.methods);
        other.initFunction = (at.dms.kjc.JMethodDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.initFunction);
        other.is2stage = this.is2stage;
        other.fields = (at.dms.kjc.JFieldDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.fields);
        other.popCount = this.popCount;
        other.peek = this.peek;
        other.linear = this.linear;
        other.array = (double[])at.dms.kjc.AutoCloner.cloneToplevel(this.array);
        other.constant = this.constant;
        other.begin = this.begin;
        other.end = this.end;
        other.pos = this.pos;
        other.total = this.total;
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
