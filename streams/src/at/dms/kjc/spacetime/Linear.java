package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.*;
import at.dms.compiler.PositionedError;

//If filter is linear

public class Linear extends RawExecutionCode implements Constants {
    private static final JMethodDeclaration linearInit=new JMethodDeclaration(null,0,CStdType.Void,"linearInit",new JFormalParameter[0],new CClassType[0],new JBlock(null,new JStatement[]{new InlineAssembly("mtsri BR_INCR,1")},null),null,null);
    private static final JMethodDeclaration[] emptyMethods=new JMethodDeclaration[0];
    //private static final JFieldDeclaration[] emptyFields=new JFieldDeclaration[0];
    private static final String WEIGHT_PREFIX="w_";
    private static final String CONSTANT_PREFIX="w_c_";
    private static final String LABEL_PREFIX="lin_";
    private static final String[] tempRegs=new String[]{"$1","$2","$3","$4"};
    private static final String[] regs=new String[]{"$5","$6","$7","$8","$9","$10","$11","$12","$13","$14","$15","$16","$17","$18","$19","$20","$21","$22","$23","$28","$30","$31"};
    private static long guin=0;
    private double[] array;
    private boolean begin;
    private double constant;
    private int popCount;
    private int[] idx;
    private long uin;
    private int pos;
    
    public Linear(FilterInfo filterInfo) {
	super(filterInfo);
	System.out.println("Generating code for " + filterInfo.filter + " using Linear.");
	FilterContent content=filterInfo.filter;
	array=content.getArray();
	begin=content.getBegin();
	constant=content.getConstant();
	popCount=content.getPopCount();
	int num=array.length/popCount;
	pos=content.getPos();
	idx=new int[num];
	for(int i=0,j=0;j<num;i+=popCount,j++) {
	    System.err.println("Adding idx: "+i);
	    idx[j]=i;
	}
	uin=guin++;
    }
    
    public JBlock getSteadyBlock() {
	JStatement[] body;
	if(begin)
	    body=new JStatement[array.length+3];
	else
	    body=new JStatement[array.length+2];
	InlineAssembly inline=new InlineAssembly();
	inline.add(".set noat");
	body[0]=inline;
	for(int i=0;i<array.length;i++) {
	    inline=new InlineAssembly();
	    inline.add("lw "+regs[i]+", %0");
	    inline.addInput("\"m\"("+getWeight(i)+")");
	    body[1+i]=inline;
	}
	if(begin) {
	    inline=new InlineAssembly();
	    inline.add("lw "+regs[regs.length-1]+", %0");
	    inline.addInput("\"m\"("+getConstant()+")");
	    body[body.length-2]=inline;
	}
	inline=new InlineAssembly();
	body[body.length-1]=inline;
	for(int i=0;i<idx.length-1;i++)
	    for(int k=0;k<popCount;k++)
		for(int j=i;j>=0;j--) {
		    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
		    inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
		}
	for(int turn=0;turn<pos;turn++)
	    for(int k=0;k<popCount;k++)
		for(int j=idx.length-1;j>=0;j--) {
		    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
		    inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
		}
	//inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
	//inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
	final int mult=getMult(array.length);
	/*for(int i=0;i<mult;i++)
	  for(int j=0;j<popCount;j++)
	  for(int k=idx.length-1;k>=0;k--) {
	  int offset=idx[k]+j;
	  inline.add("mul.s "+tempRegs[times]+",\\t$csti,\\t"+regs[offset]);
	  inline.add("add.s "+getInterReg(false,popNum,elem)+",\\t"+getInterReg(true,popNum,elem)+",\\t"+tempRegs[l]);
	  }*/
	inline.add(getLabel()+": #LOOP");
	int times=0;
	int[] oldIdx=new int[4];
	int[] oldPop=new int[4];
	for(int i=0;i<mult;i++)
	    for(int j=0;j<popCount;j++)
		for(int k=idx.length-1;k>=0;k--) {
		    int offset=idx[k]+j;
		    inline.add("mul.s "+tempRegs[times]+",\\t$csti,\\t"+regs[offset]);
		    oldIdx[times]=k;
		    oldPop[times]=j;
		    times++;
		    if(times==4) {
			times=0;
			for(int l=0;l<4;l++) {
			    int popNum=oldIdx[l];
			    int elem=oldPop[l];
			    inline.add("add.s "+getInterReg(false,popNum,elem)+",\\t"+getInterReg(true,popNum,elem)+",\\t"+tempRegs[l]);
			}
		    }
		}
	inline.add("j "+getLabel());
	inline.add(".set at");
	return new JBlock(null,body,null);
    }

    private String getInterReg(boolean src,int popNum,int elem) {
	if(src&&elem==0)
	    if(popNum==0)
		if(begin)
		    return regs[regs.length-1];
		else
		    return "$csti2";
	    else
		popNum--;
	else if(!src&&elem==popCount-1&&popNum==idx.length-1)
	    return "$csto";
	System.out.println("Array: "+array.length+" "+popNum+" "+regs.length);
	return regs[array.length+popNum];
    }
    
    public static int getMult(int num) {
	int mod=num%4;
	if(mod==0)
	    return 1;
	int i=1;
	int rem=mod;
	while(rem!=0) {
	    i++;
	    rem+=mod;
	    if(rem>=4)
		rem-=4;
	}
	System.out.println("MULTIPLE: "+i);
	return i;
    }

    private String getWeight(int i) {
	return WEIGHT_PREFIX+uin+"_"+i;
    }

    private String getConstant() {
	return CONSTANT_PREFIX+uin;
    }

    private String getLabel() {
	return LABEL_PREFIX+uin;
    }

    public JMethodDeclaration getInitStageMethod() {
	return linearInit;
    }
    
    public JMethodDeclaration[] getHelperMethods() {
	return emptyMethods;
    }

    public JFieldDeclaration[] getVarDecls() {
	JFieldDeclaration[] fields=new JFieldDeclaration[array.length+1];
	for(int i=0;i<array.length;i++)
	    try {
		fields[i]=new JFieldDeclaration(null,new JVariableDefinition(null,0,CStdType.Float,getWeight(i),new JFloatLiteral(null,Float.toString((float)array[i]))),null,null);
	    } catch(PositionedError e) {
		Utils.fail("Couldn't convert weight "+i+": "+array[i]);
	    }
	try {
	    fields[array.length]=new JFieldDeclaration(null,new JVariableDefinition(null,0,CStdType.Float,getConstant(),new JFloatLiteral(null,Float.toString((float)constant))),null,null);
	} catch(PositionedError e) {
	    Utils.fail("Couldn't convert constant: "+constant);
	}
	return fields;
    }
}


