package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.*;
import at.dms.compiler.PositionedError;
import java.util.ArrayList;

//If filter is linear

public class Linear extends RawExecutionCode implements Constants {
    private static final JMethodDeclaration linearInit=new JMethodDeclaration(null,0,CStdType.Void,"linearInit",new JFormalParameter[0],new CClassType[0],new JBlock(null,new JStatement[]{new InlineAssembly("mtsri BR_INCR,1")},null),null,null);
    private static final JMethodDeclaration[] emptyMethods=new JMethodDeclaration[0];
    //private static final JFieldDeclaration[] emptyFields=new JFieldDeclaration[0];
    private static final String WEIGHT_PREFIX="w_";
    private static final String CONSTANT_PREFIX="w_c_";
    private static final String LABEL_PREFIX="lin_";
    private static final String zeroReg="$0";
    private static final String[] tempRegs=new String[]{"$1","$2","$3","$4"};
    private static final String loopReg="$5";
    private static final String[] regs=new String[]{"$6","$7","$8","$9","$10","$11","$12","$13","$14","$15","$16","$17","$18","$19","$20","$21","$22","$23","$28","$30","$31"};
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
	//assert filterInfo.remaining<=0:"Items remaining in buffer not supported for linear filters.";
	FilterTraceNode node=filterInfo.traceNode;
	System.out.println("["+node.getX()+","+node.getY()+"] Generating code for " + filterInfo.filter + " using Linear.");
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
	    body=new JStatement[array.length+4];
	else
	    body=new JStatement[array.length+3];
	//Filling register with Constants
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
	    body[body.length-3]=inline;
	}
	//Loop Counter
	inline=new InlineAssembly();
	body[body.length-2]=inline;
	inline.add("addiu "+loopReg+",\\t"+zeroReg+",\\t-"+filterInfo.steadyMult);
	inline.add("addiu! "+zeroReg+",\\t"+zeroReg+",\\t"+filterInfo.steadyMult);
	//Start Template
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
	//inline.add(getLabel()+": #LOOP");
	int times=0;
	int[] oldIdx=new int[4];
	int[] oldPop=new int[4];
	System.out.println("Times: "+mult+" "+popCount+" "+idx.length);
	for(int turn=0;turn<pos+1;turn++) { //Synch up linear tiles
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
	    if(turn==pos-1)
		inline.add(getLabel()+": #LOOP");
	}
	//inline.add("j "+getLabel());
	inline.add("bnea "+loopReg+",\\t"+zeroReg+",\\t"+getLabel());
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
	System.out.println("MULTIPLE: "+num+" "+i);
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
    
    public JMethodDeclaration getPrimePumpMethod() {
	//TEMP
	return getInitStageMethod();
    }
    
    public JMethodDeclaration getInitStageMethod() {
	//return linearInit;

	//Copied From DirectCommunication
	JBlock statements = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;

	//add the calls for the work function in the initialization stage
	statements.addStatement(generateInitWorkLoop(filter));
	//add the calls to the work function in the prime pump stage
	statements.addStatement(getWorkFunctionBlock(filterInfo.primePump));	
	
	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      initStage + uniqueID,
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      statements,
				      null,
				      null);
    }
    
    public JMethodDeclaration[] getHelperMethods() {
	//return emptyMethods;
	
	//Copied From DirectCommunication
	ArrayList methods = new ArrayList();
	
	//add all helper methods, except work function
	for (int i = 0; i < filterInfo.filter.getMethods().length; i++) 
	    if (!(filterInfo.filter.getMethods()[i].equals(filterInfo.filter.getWork())))
		methods.add(filterInfo.filter.getMethods()[i]);
	
	return (JMethodDeclaration[])methods.toArray(new JMethodDeclaration[0]);	
    }

    //Copied from DirectCommunication
    JStatement generateInitWorkLoop(FilterContent filter)
    {
	JBlock block = new JBlock(null, new JStatement[0], null);

	//clone the work function and inline it
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	
	//if we are in debug mode, print out that the filter is firing
	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing (init)."),
				       null));
	}
	
	block.addStatement(workBlock);
	
	//return the for loop that executes the block init - 1
	//times
	return makeForLoop(block, generatedVariables.exeIndex1, 
			   new JIntLiteral(filterInfo.initMult));
    }

    //Copied From DirectCommunication
    private JBlock getWorkFunctionBlock(int mult)
    {
	JBlock block = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;
	//inline the work function in a while loop
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());
	
	//create the for loop that will execute the work function
	//local variable for the work loop
	JVariableDefinition loopCounter = new JVariableDefinition(null,
								  0,
								  CStdType.Integer,
								  workCounter,
								  null);
	
	

	JStatement loop = 
	    makeForLoop(workBlock, loopCounter, new JIntLiteral(mult));
	block.addStatement(new JVariableDeclarationStatement(null,
							     loopCounter,
							     null));
	block.addStatement(loop);
	/*
	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      steadyStage + uniqueID,
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      block,
				      null,
				      null);
	*/
	return block;
    }

    public JFieldDeclaration[] getVarDecls() {
	FilterContent filter = filterInfo.filter;
	JFieldDeclaration[] fields=new JFieldDeclaration[array.length+3 + filter.getFields().length];
	
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

	//Adapted from Gordo's Code
	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex + uniqueID,
				    null);

	//remember the JVarDef for latter (in the raw main function)
	generatedVariables.exeIndex = exeIndexVar;
	fields[array.length+1]=new JFieldDeclaration(null, exeIndexVar, null, null);
	
	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1 + uniqueID,
				    null);

	generatedVariables.exeIndex1 = exeIndex1Var;
	fields[array.length+2]=new JFieldDeclaration(null, exeIndex1Var, null, null);

	//add the fields of the non-linear filter, because we call the non-linear work function in the
	//init and primepump stage
	for (int i = 0; i < filter.getFields().length; i++) 
	    fields[i + array.length + 3] = filter.getFields()[i];
	
	//convert the communication
	//all the communication is in the work function
	filter.getWork().accept(new DirectConvertCommunication());
	//End Gordo's Code.
	return fields;
    }
    
    //Copied from Gordo
    static class DirectConvertCommunication extends SLIRReplacingVisitor 
    {
	public Object visitAssignmentExpression(JAssignmentExpression oldself,
						JExpression oldleft,
						JExpression oldright) 
	{
	    //a little optimization, use the pointer version of the 
	    //structure's pop in struct.h to avoid copying		
	    if (oldright instanceof JCastExpression && 
		(((JCastExpression)oldright).getExpr() instanceof SIRPopExpression)) {
		SIRPopExpression pop = (SIRPopExpression)((JCastExpression)oldright).getExpr();
	    
		if (pop.getType().isClassType()) {
		    JExpression left = (JExpression)oldleft.accept(this);
		
		    JExpression[] arg = 
			{left};
		
		    return new JMethodCallExpression(null, new JThisExpression(null), 
						     structReceiveMethodPrefix + 
						     pop.getType(),
						     arg);
		} 
		if (pop.getType().isArrayType()) {
		    return null;
		}
	    }

	    //otherwise do the normal thing
	    JExpression self = (JExpression)super.visitAssignmentExpression(oldself,
									    oldleft, 
									    oldright);
	    return self;
	}
    

	public Object visitPopExpression(SIRPopExpression oldSelf,
					 CType oldTapeType) {
	
	    // do the super
	    SIRPopExpression self = 
		(SIRPopExpression)
		super.visitPopExpression(oldSelf, oldTapeType);  

	    //if this is a struct, use the struct's pop method, generated in struct.h
	    if (self.getType().isClassType()) {
		return new JMethodCallExpression(null, new JThisExpression(null), 
						 "pop" + self.getType(), 
						 new JExpression[0]);
	    }
	    else if (self.getType().isArrayType()) {
		return null;
	    }
	    else {
		//I am keeping it the was it is because we should use static_receive
		//instead of receiving to memory as in the code in Util
		if (KjcOptions.altcodegen || KjcOptions.decoupled) 
		    return altCodeGen(self);
		else
		    return normalCodeGen(self);
	    }
	}
    
    
	private Object altCodeGen(SIRPopExpression self) {
	    //direct communcation is only generated if the input/output types are scalar
	    if (self.getType().isFloatingPoint())
		return 
		    new JLocalVariableExpression(null,
						 new JGeneratedLocalVariable(null, 0, 
									     CStdType.Float, 
									     Util.CSTIFPVAR,
									     null));
	    else 
		return 
		    new JLocalVariableExpression(null,
						 new JGeneratedLocalVariable(null, 0, 
									     CStdType.Integer,
									     Util.CSTIINTVAR,
									     null));
	}
    
	private Object normalCodeGen(SIRPopExpression self) {
	    String floatSuffix;
	
	    floatSuffix = "";

	    //append the _f if this pop expression pops floats
	    if (self.getType().isFloatingPoint())
		floatSuffix = "_f";
	
	    //create the method call for static_receive()
	    JMethodCallExpression static_receive = 
		new JMethodCallExpression(null, new JThisExpression(null),
					  "static_receive" + floatSuffix, 
					  new JExpression[0]);
	    //store the type in a var that I added to methoddeclaration
	    static_receive.setTapeType(self.getType());
	
	    return static_receive;
	}

	/*public Object visitPeekExpression(SIRPeekExpression oldSelf,
	  CType oldTapeType,
	  JExpression oldArg) {
	  Utils.fail("Should not see a peek expression when generating " +
	  "direct communication");
	  return null;
	  }*/
    }
    //End Copy
}


