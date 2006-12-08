package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.*;
import at.dms.compiler.PositionedError;
import java.util.ArrayList;

//If filter is linear

public class Linear extends BufferedCommunication implements Constants {
    //private static final JMethodDeclaration linearInit=new JMethodDeclaration(null,0,CStdType.Void,"linearInit",new JFormalParameter[0],new CClassType[0],new JBlock(null,new JStatement[]{new InlineAssembly("mtsri BR_INCR,1")},null),null,null);
    
    /** Set the BNEA instruction to add 1 when executing **/
    private static final JStatement initStatement = new InlineAssembly("mtsri BR_INCR,1");
    
    private static final JMethodDeclaration[] emptyMethods = new JMethodDeclaration[0];
    
    //private static final JFieldDeclaration[] emptyFields = new JFieldDeclaration[0];
    private static final String WEIGHT_PREFIX = "w_";
    private static final String CONSTANT_PREFIX = "w_c_";
    private static final String LABEL_PREFIX = "lin_";
    
    private static final String zeroReg = "$0";
    
    private static final String[] tempRegs = new String[]{"$1","$2","$3","$4"};
    private static final String tempReg = "$5"; //Loop and address reg
    
    //The actual number of registers usable as coefficients is regs.length-array.length/popCount-1
    //The -1 can be potentially gotten rid of if constant =0
    private static final String[] regs =
        new String[]{"$6","$7","$8","$9","$10","$11","$12","$13","$14","$15",
                     "$16","$17","$18","$19","$20","$21","$22","$23","$30","$31"}; //28
    /** unique id counter **/
    private static long guin=0;
    private double[] array;
    /** is this the beginning of the pipeline of fissed filters **/
    private boolean begin;
    /** is this the end of the pipeline of fissed filters **/
    private boolean end;
    /** the b in for Ax + b **/
    private double constant;
    /** Pop rate **/
    private int popCount;
    /** peek rate **/
    private int peek;
    /** idx.length == num, 0, popCount, 2*popCount, 3*popCount, ... **/
    private int[] idx;
    /** array.length / popCount, number of regs needed - 1 **/
    private int num; 
    /** num - 1, this is used to make indexing of for loops easier **/
    private int topPopNum; 
    /** this filter's unique id **/
    private long uin;
    /** The position in the pipeline of fissed filter from back, 
        the first filter is labeled (size - 1) **/
    private int pos;
    /** The position in the pipeline of fissed filters from front **/
    private int index;

    public Linear(RawTile tile, FilterInfo filterInfo) {
        super(tile, filterInfo, null);
        assert false;
        //assert filterInfo.remaining<=0:"Items remaining in buffer not supported for linear filters.";
        FilterSliceNode node = filterInfo.traceNode;
        System.out.println(" Generating code for " + filterInfo.filter + " using Linear.");
        assert filterInfo.initMult < 1 :
            "Still need to create init function: "+filterInfo.initMult;
        System.out.println("STEADYSTATE: "+filterInfo.steadyMult);
        FilterContent content = filterInfo.filter;
        array = content.getArray();
        begin = content.getBegin();
        end = content.getEnd();
        constant = content.getConstant();
        popCount = content.getPopCount();
        peek = content.getPeek();
        pos = content.getPos();
        //set the position from the front
        index = content.getTotal()-pos-1;
    
        //Check this calculation!!!
        assert array.length <= regs.length - array.length / popCount - 1 : 
            "Not enough registers for coefficients: " + array.length;

        num = array.length / popCount;

        System.out.println("POS: "+pos);

        idx = new int[num];
        topPopNum = num-1;
        for(int i = 0,j = 0;j<num;i+=popCount,j++) {
            //System.err.println("Adding idx: "+i);
            idx[j] = i;
        }
        uin = guin++;
    }
    
    

    public JBlock getSteadyBlock() {
        JStatement[] body;
        if(begin)
            body=new JStatement[array.length+7];
        else
            body=new JStatement[array.length+6];
        //Filling register with Constants
        InlineAssembly inline=new InlineAssembly();

        //Adding clobber
        inline.addClobber("$1");
        inline.addClobber("$2");
        inline.addClobber("$3");
        inline.addClobber("$4");
        inline.addClobber("$5");
    
        if(begin)
            inline.addClobber(regs[regs.length-1]);
        for(int i=0;i<array.length+num;i++)
            inline.addClobber(regs[i]);

        //tell the assembler not to complain about using $at ($1)
        inline.add(".set noat");
    
        //Default number of turns
        int turns = pos * num; 
    
        //how many
        final int mult = getMult(array.length);
    
        //this will be handled by the 
        final int target = filterInfo.steadyMult - (int)Math.ceil(((double)peek)/popCount);

        //this is the mult of the inner loop
        final int newSteadyMult = target / mult - 1;
    
        final int remainingExec = target - (newSteadyMult + 1) * mult;
    
        //Remaining executions??
        turns += remainingExec; 
    
        System.out.println(filterInfo.toString() + " array.length = " + array.length + 
                           " steadyMult = " + filterInfo.steadyMult +
                           " mult = " + mult + " target = " + target + " newSteadyMult = " + newSteadyMult);
    
        assert newSteadyMult > 0:"SteadyMult on linear filter not high enough! " + filterInfo.toString() + 
            "\nmult = " + mult + " target = " + target + " newSteadyMult = " + newSteadyMult ;

        //Send steadyMult to switch
        inline.add("addiu! "+zeroReg+",\\t"+zeroReg+",\\t"+(newSteadyMult-1)); 

        //TODO: Save registers here
    
        //set the first inline assembly instruction of the body
        body[0]=inline;

        //load weights (A array) into registers 6-31, but not 24-29
        for(int i=0;i<array.length;i++) {
            inline=new InlineAssembly();
            inline.add("lw "+regs[i]+", %0");
            inline.addInput("\"m\"("+getWeight(i)+")");
            //add it to the body 
            body[1+i]=inline;
        }
    
        //if the first filter in the sequence, load the constant (b) into register 31
        if(begin) {
            inline=new InlineAssembly();
            inline.add("lw "+regs[regs.length-1]+", %0");
            inline.addInput("\"m\"("+getConstant()+")");
            //add it to the body at body.length - 6 
            body[body.length-6]=inline;
        }

        //Start Template
        inline=new InlineAssembly();
        body[body.length-5]=inline;
    
        for(int i = 0;i <= topPopNum; i++) {
            for(int j = 0;j < popCount; j++) {
                for(int k = i; k >= 0; k--) {
                    inline.add("mul.s " + tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
                    inline.add("add.s " + getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
                }
            }
        }
    
        for(int turn=0; turn < turns; turn++) { 
            for(int j=0;j<popCount;j++) {
                for(int k=topPopNum;k>=0;k--) {
                    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
                    inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
                }
            }
        }

        //Loop Counter
        inline = new InlineAssembly();
        body[body.length-4] = inline;
        //load newSteady - 1 into tempreg $5
        inline.add("addiu "+tempReg+",\\t"+zeroReg+",\\t-"+(newSteadyMult-1));

        //Innerloop
        inline=new InlineAssembly();
        body[body.length-3] = inline;
        for(int repeat = 0; repeat < 2; repeat++) {
            if(repeat == 1)
                inline.add(getLabel()+": #LOOP");
            int times=0;
            int[] oldPopNum=new int[4];
            int[] oldElem=new int[4];
            for(int i=0;i<mult;i++)
                for(int j=0;j<popCount;j++)
                    for(int k=topPopNum;k>=0;k--) {
                        int offset=idx[k]+j;
                        inline.add("mul.s "+tempRegs[times]+",\\t$csti,\\t"+regs[offset]);
                        oldPopNum[times]=k;
                        oldElem[times]=j;
                        times++;
                        if(times==4) {
                            times=0;
                            for(int l=0;l<4;l++) {
                                int popNum=oldPopNum[l];
                                int elem=oldElem[l];
                                inline.add("add.s "+getInterReg(false,popNum,elem)+",\\t"+getInterReg(true,popNum,elem)+",\\t"+tempRegs[l]);
                            }
                        }
                    }
            if(repeat == 1)
                inline.add("bnea "+tempReg+",\\t"+zeroReg+",\\t"+getLabel());
        }

        //Remainder InnerLoop, this shouldn't be needed anymore
        inline=new InlineAssembly("#BLAH");
        body[body.length-2]=inline;


        //Postloop
        inline=new InlineAssembly();
        body[body.length-1]=inline;

        //turns=index*num+extra;
        //turns=pos*num;
        turns = index*num; //+(int)Math.ceil(((double)bufferSize)/popCount); //Make sure to fill peekbuffer
        System.out.println("TILE TURNS: "+turns);
    
        //Order reversed
        for(int turn=0;turn<turns;turn++)
            for(int j=0;j<popCount;j++)
                for(int k=topPopNum;k>=0;k--) {
                    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
                    inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
                }
        for(int i=0;i<topPopNum;i++)
            for(int j=0;j<popCount;j++)
                for(int k=topPopNum;k>i;k--) {
                    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
                    inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
                }
    
        //TODO: Restore regs 
        inline.add(".set at");
        return new JBlock(null,body,null);
    }

    //Get the reg to read or write to
    private String getInterReg(boolean src, int popNum, int elem) {
        if(src && elem==0)
            if (popNum == 0)
                if (begin)
                    return regs[regs.length-1];
                else
                    return "$csti2";
            else
                popNum--;
        else if (!src&&elem==popCount-1&&popNum==topPopNum)
            return "$csto";
        //System.out.println("Array: "+array.length+" "+popNum+" "+regs.length);
        return regs[array.length+popNum];
    }
    
    //Get how many executions need to be in the inner loop
    public static int getMult(int num) {
        int mod = num % 4;

        /*if(mod==0)
          return 1;
          int i=1;
          int rem=mod;
          while(rem!=0) {
          i++;
          rem+=mod;
          if(rem>=4)
          rem-=4;
          }
          //System.out.println("MULTIPLE: "+num+" "+i);
          return i;*/

        switch(mod) {
        case 0:
            return 1;
        case 1:
            return 4;
        case 2:
            return 2;
        case 3:
            return 4;
        default:
            throw new AssertionError("Bad mod");
        }
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
    /*    
          public JBlock getSlowExecute(int mult,int buffer) {
          JStatement[] blockBody=new JStatement[1];
          InlineAssembly inline=new InlineAssembly();
          blockBody[0]=inline;
          inline.add(".set noat");
          final int turns=mult-num;
          if(begin) {
          inline.addInput("\"i\"("+generatedVariables.recvBuffer.getIdent()+")");
          inline.add("la "+tempReg+", %0");
          int readIndex=0;
          int writeIndex=0;
          int bufferRemaining=buffer; //Use peek buffer while bufferRemaining>0 else use net
          assert mult>=num:"Not handled yet: "+mult+" "+num;
          //preloop
          for(int i=0;i<num;i++)
          for(int j=0;j<popCount;j++) {
          assert bufferRemaining>0:"Buffer shouldn't run out here!";
          inline.add("lw    "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          readIndex+=4;
          bufferRemaining--;
          for(int k=i;k>=0;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          }
          }
          //steadyloop
          for(int i=0;i<turns;i++)
          for(int j=0;j<popCount;j++) {
          if(bufferRemaining>0) {
          //Load value and send to switch
          if(end)
          inline.add("lw    "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          else
          inline.add("lw!   "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          readIndex+=4;
          bufferRemaining--;
          } else
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          for(int k=topPopNum;k>=0;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          }
          }
          //postloop
          for(int i=0;i<topPopNum;i++)
          for(int j=0;j<popCount;j++) {
          if(bufferRemaining>0) {
          //Load value and send to switch
          if(end)
          inline.add("lw    "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          else
          inline.add("lw!   "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          readIndex+=4;
          bufferRemaining--;
          } else
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          inline.add("sw    "+tempRegs[0]+",\\t"+writeIndex+"("+tempReg+")");
          writeIndex+=4;
          for(int k=topPopNum;k>i;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          }
          }
          //forward values
          final int numForward=pos*num;
          for(int i=0;i<numForward;i++) {
          if(bufferRemaining>0) {
          if(end)
          inline.add("lw    "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          else
          inline.add("lw!   "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          readIndex+=4;
          bufferRemaining--;
          } else
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          inline.add("sw    "+tempRegs[0]+",\\t"+writeIndex+"("+tempReg+")");
          writeIndex+=4;
          }
          //Transfer rest of buffer
          final int excess=bufferSize-popCount*(num+mult-1+topPopNum)-numForward;
          for(int i=0;i<excess;i++) {
          inline.add("lw    "+tempRegs[0]+",\\t"+readIndex+"("+tempReg+")");
          readIndex+=4;
          inline.add("sw    "+tempRegs[0]+",\\t"+writeIndex+"("+tempReg+")");
          writeIndex+=4;
          }
          } else {
          //preloop
          for(int i=0;i<num;i++)
          for(int j=0;j<popCount;j++) {
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          for(int k=i;k>=0;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          //inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
          //inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
          }
          }
          //innerloop
          for(int i=0;i<turns;i++)
          for(int j=0;j<popCount;j++) {
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          for(int k=topPopNum;k>=0;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          //inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
          //inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
          }
          }
          //postloop
          for(int i=0;i<topPopNum;i++)
          for(int j=0;j<popCount;j++) {
          inline.add("move  "+tempRegs[0]+",\\t$csti");
          for(int k=topPopNum;k>i;k--) {
          inline.add("mul.s "+tempRegs[1]+",\\t"+tempRegs[0]+",\\t"+regs[idx[k]+j]);
          inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[1]);
          //inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[k]+j]);
          //inline.add("add.s "+getInterReg(false,k,j)+",\\t"+getInterReg(true,k,j)+",\\t"+tempRegs[0]);
          }
          }
          }
          inline.add(".set at");
          return new JBlock(null,blockBody,null);
          //return new JMethodDeclaration(null,at.dms.kjc.Constants.ACC_PUBLIC,CStdType.Void,primePumpStage+uniqueID,JFormalParameter.EMPTY,CClassType.EMPTY,block,null,null);
          }
    */
    public JMethodDeclaration getInitStageMethod() {
        //return linearInit;
        JMethodDeclaration method=super.getInitStageMethod();
        method.addStatementFirst(initStatement);
        return method;
    }
    
    public JMethodDeclaration[] getHelperMethods() {
        return emptyMethods;
    }

    public JFieldDeclaration[] getVarDecls() {
        //Get varDecls from BufferedCommunication
        JFieldDeclaration[] superVarDecls=super.getVarDecls();
        FilterContent filter = filterInfo.filter;
        JFieldDeclaration[] fields=new JFieldDeclaration[array.length+1+superVarDecls.length];
        //Fill in BufferedCommunication varDecls
        System.arraycopy(superVarDecls,0,fields,array.length+1,superVarDecls.length);
    
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

    protected JStatement getWorkFunctionCall(FilterContent filter) {
        return new JEmptyStatement(null,null);
        //throw new AssertionError("Shouldn't be called");
    }

    /*public JMethodDeclaration getPrimePumpMethod() {
      JBlock block=getSlowExecute(filterInfo.primePump,bufferSize);
      return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
      CStdType.Void,
      primePumpStage + uniqueID,
      JFormalParameter.EMPTY,
      CClassType.EMPTY,
      block,
      null,
      null); 
      }*/


    public JBlock getSteadyBlockOld() {
        JStatement[] body;
        if(begin)
            body=new JStatement[array.length+4];
        else
            body=new JStatement[array.length+3];
        //Filling register with Constants
        InlineAssembly inline=new InlineAssembly();
        inline.add(".set noat");
        //TODO: Save registers here
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
        inline.add("addiu "+tempReg+",\\t"+zeroReg+",\\t-"+filterInfo.steadyMult);
        inline.add("addiu! "+zeroReg+",\\t"+zeroReg+",\\t"+filterInfo.steadyMult); //Send steadyMult to switch
        //Start Template
        inline=new InlineAssembly();
        body[body.length-1]=inline;
        //Preloop
        for(int i=0;i<topPopNum;i++)
            for(int k=0;k<popCount;k++)
                for(int j=i;j>=0;j--) {
                    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
                    inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
                }
        for(int turn=0;turn<pos;turn++)
            for(int k=0;k<popCount;k++)
                for(int j=topPopNum;j>=0;j--) {
                    inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
                    inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
                }
        //inline.add("mul.s "+tempRegs[0]+",\\t$csti,\\t"+regs[idx[j]+k]);
        //inline.add("add.s "+getInterReg(false,j,k)+",\\t"+getInterReg(true,j,k)+",\\t"+tempRegs[0]);
        //Innerloop
        final int mult=getMult(array.length);
        inline.add(getLabel()+": #LOOP");
        int times=0;
        int[] oldIdx=new int[4];
        int[] oldPop=new int[4];
        //System.out.println("Times: "+mult+" "+popCount+" "+idx.length);
        //for(int turn=0;turn<pos+1;turn++) { //Synch up linear tiles
        for(int i=0;i<mult;i++)
            for(int j=0;j<popCount;j++)
                for(int k=topPopNum;k>=0;k--) {
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
        //if(turn==pos-1)
        //inline.add(getLabel()+": #LOOP");
        //}
        //inline.add("j "+getLabel());
        inline.add("bnea "+tempReg+",\\t"+zeroReg+",\\t"+getLabel());
        //Postloop
        inline.add(".set at");
        return new JBlock(null,body,null);
    }
}