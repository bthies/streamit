
package at.dms.kjc.cluster;

import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.compiler.JavaStyleComment; // for debugging
/**
 * Obsolete class to increase peek scaling.
 * 
 * Was almost always a slowdown rather than a speedup.
 * 
 * @author Janis
 * @deprecated
 */
class IncreaseFilterMult implements StreamVisitor {

    class WorkInfo {
        public JMethodDeclaration work;
        public int push, pop, peek, multiple;   
    }

//    private int mult;
    private int sj_counter;
//    private boolean start_of_pipeline;

    // SIRStream -> WorkInfo
    private static HashMap<SIRFilter, WorkInfo> previous_work = new HashMap<SIRFilter, WorkInfo>(); 
    
    private int CODE_CACHE_SIZE;

    /**
     * Constructor
     * 
     * @param mult                  Odd: never referenced in code!
     * @param code_cache_size
     */
    
    public IncreaseFilterMult(int mult, int code_cache_size) {
//        this.mult = mult;
        this.sj_counter = 0;
//        this.start_of_pipeline = false;
        this.CODE_CACHE_SIZE = code_cache_size;
    }


    // returns implicit schedule scaling after increasing/decreasing mult.
    // before hash map SIRStream->int[] steady state execution count
    // after hash map SIRStream->int[] steady state execution count

    public static int scheduleMultAfterScaling(HashMap before, HashMap after) {

        Set keySet = before.keySet();
        Iterator iter = keySet.iterator();

        int mult = -1;

        while (iter.hasNext()) {
            SIROperator oper = (SIROperator)iter.next();
            int i1[] = (int[])before.get(oper);
            int i2[] = (int[])after.get(oper);

            if (i1 == null || i2 == null) {
        
                //System.out.println("Warning! "+oper+
                //         " schedule1("+i1+") "+
                //         " schedule2("+i2+")");
                continue;
            }


            if (i1 == null && i2 == null) continue;

            if (previous_work.containsKey(oper)) {
                //SIRFilter f = (SIRFilter)oper;
                WorkInfo info = previous_work.get(oper);
                i2[0] *= info.multiple;
            }

            int ratio = i2[0] / i1[0];
            assert(i2[0] % i1[0] == 0);

            assert (mult == -1 || mult == ratio);
            mult = ratio;
        }   
        assert (mult!=-1);
        return mult;
    }


    public void preVisitPipeline(SIRPipeline self, 
                                 SIRPipelineIter iter) {
//        start_of_pipeline = true;
    }

    public void preVisitSplitJoin(SIRSplitJoin self,
                                  SIRSplitJoinIter iter) {
        sj_counter++;
//        start_of_pipeline = true;
    }

    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
        sj_counter--;
    }


    static public void inc(SIRStream str, int mult, int code_cache) {
        IterFactory.createFactory().createIter(str).accept(new IncreaseFilterMult(mult, code_cache));
    }


    private static boolean isFusedWith(SIROperator oper1, SIROperator oper2, 
                                       HashMap partitionMap) {
        assert(oper2 instanceof SIRPhasedFilter);
        if (oper1 instanceof SIRPhasedFilter) {
            return partitionMap.get(oper1).equals(partitionMap.get(oper2));
        }
        if (oper1 instanceof SIRPipeline) {
            SIRPipeline pipe = (SIRPipeline)oper1;
            return isFusedWith(pipe.get(pipe.size()-1),oper2,partitionMap);
        }
        if (oper1 instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin)oper1;
            return isFusedWith(sj.get(0),oper2,partitionMap);
        }
        assert(0==1);//fail
        return false;
    }

    // this function is used to find out if a filter with deep peeking 
    // will have its peek buffer managed in the fused code
    // if a splitjoin has RR splitter then this is guaranteed
    // if all split joins are Duplicate then must see if splitjoin
    // is fused with previous element in the pipeline

    private static boolean fusedWithPrev(SIROperator oper, HashMap partitionMap) {
        SIRStream child = (SIRStream)oper;
        SIRContainer parent = oper.getParent();

        assert(!(parent instanceof SIRFeedbackLoop)); 

        for (;;) {

            if (parent instanceof SIRSplitJoin) {

                if (((SIRSplitJoin)parent).getSplitter().getType().isRoundRobin()) {
                    // check if splitjoin is fused
                    boolean fused = true;
            
                    for (int i = 0; i < parent.size(); i++) {
                        SIRStream str = (SIRStream)parent.get(i);
                        if (!isFusedWith(str, oper, partitionMap)) {
                            fused = false;
                            break;
                        }
                    }

                    // if splitjoin fused peek buffers managed by fused code 
                    return fused; 
                }
        
            }

            if (parent instanceof SIRPipeline) {
                int child_index = parent.indexOf(child);
                if (child_index > 0) {
                    return isFusedWith(parent.get(child_index-1),oper,partitionMap);
                }
            }

            // recurse up if split join has duplicate splitter
            // or if we are the first element in the pipeline
        
            child = (SIRStream)parent;
            parent = parent.getParent();
        
            assert(!(parent instanceof SIRFeedbackLoop)); 
            if (parent == null) break; // returns null
        }

        return false;
    }

    public static boolean decreaseMult(HashMap partitionMap) {

        boolean decreased = false;

        Set keys = partitionMap.keySet();
        Iterator iter = keys.iterator();
    
        while (iter.hasNext()) {
            SIROperator oper = (SIROperator)iter.next();

            if (oper instanceof SIRFilter) {
                if (!fusedWithPrev(oper, partitionMap)) {
            
                    //System.out.println("Filter: "+oper+" not fused with previous operator!");

                    if (previous_work.containsKey(oper)) {

                        WorkInfo w = 
                            previous_work.get(oper);

                        if (ClusterBackend.debugging)
                            System.out.println("Filter: "+oper+" Restoring mult to 1");
                        ((SIRFilter)oper).setWork(w.work);
                        ((SIRFilter)oper).setPop(w.pop);
                        ((SIRFilter)oper).setPush(w.push);
                        ((SIRFilter)oper).setPeek(w.peek);
                        previous_work.remove(oper);
                        decreased = true;
                    }
                }
            }
        }
        return decreased;
    }


    public int calcMult(SIRFilter filter) {

        int mult = 1;
    
        int pop = filter.getPopInt();
        int peek = filter.getPeekInt();
        int extra = 0;
        if (peek > pop) extra = peek - pop;

        // make sure that filter's pop rate is at least 25% of
        // of what it peeks beyond consumed items.

        if (KjcOptions.peekratio <= 0) { 
            return 1; 
        } else {
            while (pop * mult / KjcOptions.peekratio < extra) { 
                mult = mult + 1;
            }
        }

        return mult;
    }
    
    public void visitFilter(SIRFilter filter,
                            SIRFilterIter iter) { 
    
        int _mult = calcMult(filter);

        // only increase mult if not inside a split join
        //if (sj_counter != 0) _mult = 1; 

        if (_mult == 1) return;

        if (ClusterBackend.debugging)
            System.out.print("IncMult visiting: "+filter.getName()+
                             " mult: "+_mult);

        //if (_mult == 1) {
        //    System.out.println(" No change!");
        //    return;
        //}

        JMethodDeclaration work = filter.getWork();

        if (ClusterBackend.debugging)
            System.out.print(" work: "+work.getName());

        //
        // adding a work2 method
        //

    
        JBlock block = new JBlock(null, new JStatement[0], null);

        JVariableDefinition counter = 
            new JVariableDefinition(null, 
                                    0, 
                                    CStdType.Integer,
                                    "____i",
                                    null);
    

        JExpression initExpr =
            new JAssignmentExpression(null,
                                      new JLocalVariableExpression(null,
                                                                   counter),
                                      new JIntLiteral(0));

        JStatement init = new JExpressionStatement(null, initExpr, null);

        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(null,
                                                                counter));

        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);

        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JLocalVariableExpression(null,counter),
                                      new JIntLiteral(_mult));
    
//        JMethodCallExpression callExpr = 
//            new JMethodCallExpression(null, 
//                                      new JThisExpression(null), 
//                                      work.getName()+"__2", 
//                                      new JExpression[0]);

        //JStatement call = new JExpressionStatement(null, callExpr, null);

        //JBlock do_block = new JBlock(null, new JStatement[0], null);
        //do_block.addStatement(call);
        //do_block.addStatement(incr);
    

        block.addStatement(
                           new JVariableDeclarationStatement(null, counter, null));
        block.addStatement(init);

        JBlock for_block = new JBlock(null, new JStatement[0], null);
        //for_block.addStatement(call);
        for_block.addAllStatements(work.getBody());

        JForStatement for_stmt = new JForStatement(null, init, cond, incr,
                for_block, 
                new JavaStyleComment[] {
                new JavaStyleComment("IncreaseFilterMult", true,
                        false, false)
        });


        // allow unrolling only if multiplicity increase 
        // is small or the filter has small code size

        int code_size = CodeEstimate.estimateCode(filter)*_mult;

        if ((_mult <= 8 && code_size < CODE_CACHE_SIZE / 2)) {

            for_stmt.setUnrolled(false); // allow unrolling

        } else {

            for_stmt.setUnrolled(true); // disable unrolling
        } 

        block.addStatement(for_stmt);

        //block.addStatement(
        //     new JDoStatement(null, cond, do_block, null));


        //JBlock body = new JBlock(null, new JStatement[0], null);
        //body.addStatement(new JExpressionStatement(null, new JMethodCallExpression(null, work.getName(), new JExpression[0]), null));

        /*
          JBlock work_body = work.getBody();
    
          JMethodDeclaration old_work = 
          new JMethodDeclaration(null, 
          at.dms.kjc.Constants.ACC_PUBLIC,
          CStdType.Void,
          work.getName()+"__2",
          JFormalParameter.EMPTY,
          CClassType.EMPTY,
          work_body,
          null,
          null);

          filter.addMethod(old_work); 
        */


        JMethodDeclaration new_work = 
            new JMethodDeclaration(null, 
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   work.getName(),
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   block,
                                   null,
                                   null);

        // store the original work function

        WorkInfo w = new WorkInfo();
        w.work = filter.getWork();
        w.pop = filter.getPopInt();
        w.push = filter.getPushInt();
        w.peek = filter.getPeekInt();
        w.multiple = _mult;

        previous_work.put(filter, w);

        filter.setWork(new_work); 

        //
        // increasing peek, pop, push rates
        //
    
        int pop = filter.getPopInt();
        int push = filter.getPushInt();
        int peek = filter.getPeekInt();
        int extra = 0;

        if (peek > pop) extra = peek - pop;

        filter.setPop(pop * _mult);
        filter.setPush(push * _mult);
        filter.setPeek(pop * _mult + extra);

        if (ClusterBackend.debugging)
            System.out.println(" new work: "+filter.getWork().getName());
    }

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    /**
     * PRE-VISITS 
     */
        
    /* pre-visit a pipeline */
    //public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
    
    /* pre-visit a splitjoin */
    //public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}
    
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    
    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
   
    /* post-visit a splitjoin */
    //public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}

}
