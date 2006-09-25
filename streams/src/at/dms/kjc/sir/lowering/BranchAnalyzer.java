package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;
import at.dms.compiler.TokenReference;

/**
 * This class aggressively analyzes branches in control flow for information
 * gained and calls constant prop on the rest of the method with the
 * new information gained.
 */
public class BranchAnalyzer extends SLIRReplacingVisitor {
    private JBlock block;

    private int index;

    private int size;

    private int ifDepth;

    public BranchAnalyzer() {
        //Want to just build up constants in the beginning
        //Only when we gather new information do we start writing
        //super(new Hashtable(),false);
        ifDepth=0;
    }
    
    public BranchAnalyzer(Hashtable table) {
        //Want to just build up constants in the beginning
        //Only when we gather new information do we start writing
        //super(table,false);
        ifDepth=0;
    }

    // ----------------------------------------------------------------------
    // Analyzing branches
    // ----------------------------------------------------------------------

    public void analyzeBranches(SIRStream str) {
        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                analyzeBranches(fl.getBody());
                analyzeBranches(fl.getLoop());
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        analyzeBranches(child);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        analyzeBranches(child);
                    }
            }
        if (str instanceof SIRFilter || str instanceof SIRPhasedFilter)
            for (int i = 0; i < str.getMethods().length; i++) {
                str.getMethods()[i].accept(this);
                str.getMethods()[i].accept(new Propagator(new Hashtable()));
            }
    }
    
    /**
     * Visits a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                   JExpression cond,
                                   JStatement thenClause,
                                   JStatement elseClause) {
        ifDepth++;
        //System.out.println("Visiting If:"+ifDepth);
        //cond.accept(this);
        //Propagator thenProp=new Propagator(new Hashtable());
        //Propagator elseProp=new Propagator(new Hashtable());
        //boolean analyzeThen=false;
        //boolean analyzeElse=false;
        /*if((cond instanceof JRelationalExpression)&&(((JRelationalExpression)cond).getOper()==OPE_EQ)) {
          JRelationalExpression equal=(JRelationalExpression)cond;
          JLocalVariable var=null;
          JLiteral lit=null;
          if(equal.getLeft() instanceof JLocalVariableExpression)
          var=((JLocalVariableExpression)equal.getLeft()).getVariable();
          else if(equal.getRight() instanceof JLocalVariableExpression)
          var=((JLocalVariableExpression)equal.getRight()).getVariable();
          if(equal.getLeft() instanceof JLiteral)
          lit=(JLiteral)equal.getLeft();
          else if(equal.getRight() instanceof JLiteral)
          lit=(JLiteral)equal.getRight();
          if((var!=null)&&(lit!=null)) {
          analyzeThen=true;
          thenProp.constants.put(var,lit);
          }
          }
          if((elseClause!=null)&&(cond instanceof JRelationalExpression)&&(((JRelationalExpression)cond).getOper()==OPE_NE)) {
          JRelationalExpression equal=(JRelationalExpression)cond;
          JLocalVariable var=null;
          JLiteral lit=null;
          if(equal.getLeft() instanceof JLocalVariableExpression)
          var=((JLocalVariableExpression)equal.getLeft()).getVariable();
          else if(equal.getRight() instanceof JLocalVariableExpression)
          var=((JLocalVariableExpression)equal.getRight()).getVariable();
          if(equal.getLeft() instanceof JLiteral)
          lit=(JLiteral)equal.getLeft();
          else if(equal.getRight() instanceof JLiteral)
          lit=(JLiteral)equal.getRight();
          if((var!=null)&&(lit!=null)) {
          analyzeElse=true;
          elseProp.constants.put(var,lit);
          }
          }*/
        /*thenClause.accept(thenProp);
          if(elseClause!=null)
          elseClause.accept(elseProp);
          if(thenProp.added)
          analyzeThen=true;
          if(elseProp.added)
          analyzeElse=true;*/
        if(!(cond instanceof JEqualityExpression)) {
            //System.out.println("Rejecting:"+cond);
            ifDepth--;
            return self;
        }
        BlockFlattener flat=new BlockFlattener();
        //if(analyzeThen||analyzeElse) {
        JBlock thenCopy=split();
        JBlock elseCopy=(JBlock)ObjectDeepCloner.deepCopy(thenCopy);
        //if(analyzeThen) {
        //thenProp.write=true;
        //thenCopy.accept(thenProp);
        //thenCopy.accept(flat);
        //}
        //if(analyzeElse) {
        //elseProp.write=true;
        //elseCopy.accept(elseProp);
        //thenCopy.accept(flat);
        //}
        //if(thenCopy.size()!=0) {
        self.setThenClause(addStatement(thenCopy,thenClause));
        //self.getThenClause().accept(thenProp);
        self.getThenClause().accept(flat);
        //} else
        //System.out.println("Skipping Then");
        //if(elseCopy.size()!=0) {
        self.setElseClause(addStatement(elseCopy,elseClause));
        //self.getElseClause().accept(elseProp);
        self.getElseClause().accept(flat);
        //} else
        //System.out.println("Skipping Else");
        //Hashtable thenTable=thenProp.constants;
        //Hashtable elseTable=elseProp.constants;
        flat=null;
        //thenProp=null;
        //elseProp=null;
        //if(analyzeThen)
        //self.getThenClause().accept;
        //if(ifDepth<2) {
        //if(thenCopy.size()!=0) {
        thenCopy=null;
        //self.getThenClause().accept(this);
        //}
        //if(elseCopy.size()!=0) {
        elseCopy=null;
        //self.getElseClause().accept(this);
        //}
        //}
        if(ifDepth<2) {
            self.getThenClause().accept(this);
            self.getElseClause().accept(this);
        }
        ifDepth--;
        return self;
        //}
        //return self;
    }
    
    /**
     * Takes rest of block after if and puts it in a block
     */
    private JBlock split() {
        JStatement[] body=new JStatement[size-index-1];
        LinkedList list=(LinkedList)block.getStatements();
        int start=index+1;
        for(int i=start;i<size;i++) {
            body[i-start]=(JStatement)list.get(i);
        }
        index++;
        for(;index<size;index++) {
            ((LinkedList)list).removeLast();
        }
        return new JBlock(null,body,null);
    }

    /**
     * visits a for statement
     */
    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
        //init.accept(this);
        //incr.accept(this);
        //cond.accept(this);
        if(body instanceof JBlock)
            body.accept(this);
        else {
            //System.out.println("Creating body for");
            JBlock newBlock=new JBlock(body.getTokenReference(),new JStatement[]{body},null);
            newBlock.accept(this);
            self.setBody(newBlock);
        }
        return self;
    }

    //Returns 2nd block concatenated to first block
    //private void catBlocks(JBlock first,JBlock second) {
    //first.getStatements().addAll(second.getStatements());
    //}
    
    //Adds state to beginning of block and returns this new block
    //May mutate either block or state
    private JBlock addStatement(JBlock block,JStatement state) {
        if(state==null)
            return block;
        if(state instanceof JBlock) {
            ((JBlock)state).getStatements().addAll(block.getStatements());
            return (JBlock)state;
        } else {
            block.addStatementFirst(state);
            return block;
        }
    }
    
    //Copy block starting from index statement
    private JBlock copyBlock(int index,JBlock block) {
        JBlock out=(JBlock)ObjectDeepCloner.deepCopy(index,block);
        LinkedList list=(LinkedList)out.getStatements();
        for(int i=0;i<index;i++)
            list.removeFirst();
        return out;
    }
    
    public Object visitBlockStatement(JBlock self,
                                      JavaStyleComment[] comments) {
        JBlock saveBlock=block;
        int saveIndex=index;
        int saveSize=size;
        block=self;
        size=self.size();
        List statements=self.getStatements();
        for(index=0;index<size;index++) {
            JStatement oldBody=(JStatement)statements.get(index);
            Object newBody=oldBody.accept(this);
            if (!(newBody instanceof JStatement))
                continue;
            if (newBody!=null && newBody!=oldBody) {
                statements.set(index,(JStatement)newBody);
            }
            /*if(split==true) {
              split=false;
              index++;
              break;
              }*/
        }
        block=saveBlock;
        index=saveIndex;
        size=saveSize;
        return self;
    }
}
