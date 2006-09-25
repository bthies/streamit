
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;

import java.util.*;

/**
 * A greedy implementation of a constrained scheduler.
 * (This should actually be optimal in number of phases generated!)
 */

class Greedy {

    HashMap<SIROperator, Integer> iteration; // SIROperator -> Integer
    HashMap<SIROperator, Integer> credit; // SIROperator -> Integer
    HashMap<Vector<SIROperator>, Integer> queue_size; // Vector -> Integer

    HashMap<Vector<Integer>, Integer> phase_last_seen; // Vector -> Integer
    Vector<Vector<Integer>> phases; // Vcetor of Vectors

    DiscoverSchedule sched; 

    int num;
    int ph;

    int phase_num;
    int loop_start;

    int combine_ptr;

    Greedy(DiscoverSchedule sched) {

        if (ClusterBackend.debugPrint)
            System.out.println("============== Greedy ===============");

        this.sched = sched;

        phase_last_seen = new HashMap<Vector<Integer>, Integer>();
        phases = new Vector<Vector<Integer>>();
        phase_num = 0;
    
        combine_ptr = 0;

        init();

        if (ClusterBackend.debugPrint)
            System.out.println("============== Greedy ===============");

    }
    
    void init() {

        credit = new HashMap<SIROperator, Integer>();
        iteration = new HashMap<SIROperator, Integer>();
        queue_size = new HashMap<Vector<SIROperator>, Integer>();

        num = NodeEnumerator.getNumberOfNodes();
        //System.out.println("Number of nodes: "+num);

        for (int y = 0; y < num; y++) {
            SIROperator oper = NodeEnumerator.getOperator(y);

            credit.put(oper, new Integer(-1));
            iteration.put(oper, new Integer(0));

            for (Tape ns : RegisterStreams.getNodeOutStreams(oper)) {
              if (ns != null) {
                SIROperator dst = NodeEnumerator.getOperator(ns.getDest());
                queue_size.put(makeVector(oper, dst), new Integer(0));
              }
            }
        }

        ph = sched.getNumberOfPhases();
        //System.out.println("Number of phases: "+ph);

        for (int y = 0; y < ph; y++) {
            HashSet<SIROperator> p = sched.getAllOperatorsInPhase(y);
            //System.out.println("phase "+y+" has size: "+p.size());

            Iterator<SIROperator> it = p.iterator();

            while (it.hasNext()) {
                SIROperator oper = it.next();
                if (oper instanceof SIRFilter) {
                    SIRFilter src = (SIRFilter)oper;
                    HashSet<LatencyConstraint> cons = LatencyConstraints.getOutgoingConstraints((SIRFilter)oper);
                    Iterator<LatencyConstraint> ci = cons.iterator();
                    while (ci.hasNext()) {
                        LatencyConstraint c = ci.next();
                        SIRFilter dst = (SIRFilter)c.getReceiver();
                        boolean down = LatencyConstraints.isMessageDirectionDownstream(src, dst);
                        int init_c = c.getSourceInit();
                        //System.out.println("Constraint from: "+src+" to: "+dst+(down?" DOWN":" UP")+" init-credit: "+init_c);

                        if (down) {
                            credit.put(dst, new Integer(0));
                        } else {
                            credit.put(dst, new Integer(init_c));
                        }
                    }
                }
            }
        }

    
    } 

    static Vector<SIROperator> makeVector(SIROperator src, SIROperator dst) {
        Vector<SIROperator> v = new Vector<SIROperator>();
        v.add(src);
        v.add(dst);
        return v;
    }


    static int getPeek(SIROperator oper, int way) {
        assert(way >= 0);
        if (oper instanceof SIRFilter) {
            assert(way == 0);
            return ((SIRFilter)oper).getPeekInt();
        }
        return getPop(oper, way);
    }

    static int getPop(SIROperator oper, int way) {
        assert(way >= 0);
        if (oper instanceof SIRFilter) {
            assert(way == 0);
            return ((SIRFilter)oper).getPopInt();
        }
        if (oper instanceof SIRSplitter) {
            assert(way == 0);
            SIRSplitter s = (SIRSplitter)oper;
            if (s.getType().isDuplicate()) return 1;
            return ((SIRSplitter)oper).getSumOfWeights();
        }
        if (oper instanceof SIRJoiner) {
            SIRJoiner joiner = (SIRJoiner)oper;
            assert(way < joiner.getWays());
            return joiner.getWeight(way);
        }
        assert false;
        return 0;
    }

    static int getPush(SIROperator oper, int way) {
        assert(way >= 0);
        if (oper instanceof SIRFilter) {
            assert(way == 0);
            return ((SIRFilter)oper).getPushInt();
        }
        if (oper instanceof SIRJoiner) {
            assert(way == 0);
            return ((SIRJoiner)oper).getSumOfWeights();
        }
        if (oper instanceof SIRSplitter) {
            SIRSplitter s = (SIRSplitter)oper;
            assert(way < s.getWays());
            return s.getWeight(way);
        }
        assert false;
        return 0;
    }

    int nextPhase() {

        Vector<Integer> phase = new Vector<Integer>();

        if (ClusterBackend.debugPrint)
            System.out.println("-------------------------------------");

        for (int y = 0; y < ph; y++) {
            HashSet<SIROperator> p = sched.getAllOperatorsInPhase(y);
            Iterator<SIROperator> it = p.iterator();
            while (it.hasNext()) {
                SIROperator oper = it.next();
                int id = NodeEnumerator.getSIROperatorId(oper);
                int steady_count = ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(id)).intValue();

                int _iter = iteration.get(oper).intValue();
                int _credit = credit.get(oper).intValue();

                int exec = steady_count;

                if (_credit >= 0 && _iter + steady_count > _credit) {
                    exec = _credit - _iter; 
                }

                int input = 2000000000;

//                List in = RegisterStreams.getNodeInStreams(oper);
//                List out = RegisterStreams.getNodeOutStreams(oper);
        
                {
                int z = 0;
                for (Tape ns : RegisterStreams.getNodeInStreams(oper)) {
                  if (ns != null) {
                    SIROperator src = NodeEnumerator.getOperator(ns.getSource());       
                    int qsize = queue_size.get(makeVector(src, oper)).intValue();
                    int peek = getPeek(oper, z);
                    int pop = getPop(oper, z);
                    if (peek < pop) peek = pop;
                    int extra = peek - pop;

                    int can = (qsize-extra)/pop;
                    if (can < 0) can = 0;
                    if (can < input) input = can;
                  }
                  z++;
                }
                }
                
                if (input < exec) exec = input;

                //System.out.println("CREDIT = "+_credit+" INPUT = "+input+" EXEC = "+exec);
        
                {
                int z = 0;
                for (Tape ns : RegisterStreams.getNodeInStreams(oper)) {
                  if (ns != null) {
                    SIROperator src = NodeEnumerator.getOperator(ns.getSource());       
                    int qsize = queue_size.get(makeVector(src, oper)).intValue();
                    qsize -= getPop(oper, z) * exec;
                    queue_size.put(makeVector(src, oper), 
                                   new Integer(qsize));
                  }
                  z++;
                  
                }
                }
                
                {
                int z = 0;
                for (Tape ns : RegisterStreams.getNodeOutStreams(oper)) {
                  if (ns != null) {
                    SIROperator dst = NodeEnumerator.getOperator(ns.getDest());     
                    int qsize = queue_size.get(makeVector(oper, dst)).intValue();
                    qsize += getPush(oper, z) * exec;
                    queue_size.put(makeVector(oper, dst), 
                                   new Integer(qsize));
                  }
                  z++;
                }
                }
                
                if (ClusterBackend.debugPrint)
                    System.out.println(oper.getName()+" Exec = "+exec+"/"+steady_count);

                phase.add(new Integer(exec)); // push exec count to phase vector

                for (int z = 0; z < exec; z++, _iter++) {
                    if (!(oper instanceof SIRFilter)) continue;
            
                    HashSet<LatencyConstraint> cons = LatencyConstraints.getOutgoingConstraints((SIRFilter)oper);
                    Iterator<LatencyConstraint> ci = cons.iterator();
                    while (ci.hasNext()) {

                        //System.out.println("latency constraint");

                        LatencyConstraint c = ci.next();
                        SIRFilter dst = (SIRFilter)c.getReceiver();
                        boolean down = LatencyConstraints.isMessageDirectionDownstream((SIRFilter)oper, (SIRFilter)dst);
                        int init_c = c.getSourceInit();
            
                        if (down) {
                
                            if (_iter >= init_c) {
                
                                int s_steady = c.getSourceSteadyExec();
                                int cycle = (_iter - init_c) / s_steady;
                                int offs = (_iter - init_c) % s_steady;
                
                                int dep = c.getDependencyData(offs);

                                //System.out.println("dep = "+dep);
                                //System.out.println("c = "+cycle);
                                //System.out.println("o = "+offs);

                                if (dep > 0) {
                                    int cc = dep + cycle * c.getDestSteadyExec();
                                    credit.put(dst, new Integer(cc));
                                    if (ClusterBackend.debugPrint)
                                        System.out.println("Send credit: "+cc);
                                }
                            }
                
                        } else {
                
                            int s_steady = c.getSourceSteadyExec();
                            int cycle = _iter / s_steady;
                            int offs = _iter % s_steady;
                
                            int dep = c.getDependencyData(offs);
                            if (dep > 0) {
                                int cc = dep + cycle * c.getDestSteadyExec();
                                credit.put(dst, new Integer(cc));
                                if (ClusterBackend.debugPrint)
                                    System.out.println("Send credit: "+cc);
                            }
                        }
                    }
                }

                iteration.put(oper, new Integer(_iter));
            }
        }   

        boolean match = false;
        int ratio = -1;

        if (phase_last_seen.containsKey(phase)) {
            int last = phase_last_seen.get(phase).intValue();
        
            match = true;

            int node_id = 0;
            for (int y = 0; y < ph; y++) {
                HashSet<SIROperator> p = sched.getAllOperatorsInPhase(y);
                Iterator<SIROperator> it = p.iterator();
                while (it.hasNext()) {
                    SIROperator oper = it.next();
                    int id = NodeEnumerator.getSIROperatorId(oper);
                    int steady_count = ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(id)).intValue();
            
                    int sum = 0;

                    for (int z = last; z < phase_num; z++) {
                        sum += ((Integer)phases.get(z).get(node_id)).intValue();
                    }
        
                    if (sum % steady_count == 0 &&
                        sum / steady_count >= 1) {
                        if (ratio >= 1 && sum / steady_count != ratio) match = false;
                        if (ratio < 1) ratio = sum / steady_count;
                    } else {
                        match = false;
                    }

                    node_id++;
                }
            }

            if (ClusterBackend.debugPrint)
                System.out.println("---- have seen phase at: "+last+" ----");

            if (match) {
                loop_start = last;
                if (ClusterBackend.debugPrint)
                    System.out.println("---- A loop contains "+ratio+" steady states ----");
            }

        }

        if (!match) {
            phases.add(phase);
            phase_last_seen.put(phase, new Integer(phase_num));
            phase_num++;
        }

        if (ClusterBackend.debugPrint)
            System.out.println("-------------------------------------");

        if (match) return ratio; else return 0; 

    }

    void combineInit() {

        int node_id;

        if (combine_ptr + 1 >= loop_start) return;

        init();

        if (ClusterBackend.debugPrint) {
            System.out.println("-------------------------------------");
            System.out.println("----------- COMBINING ---------------");
            System.out.println("-------------------------------------");
        }

        for (int curr = 0; curr < combine_ptr; curr++) {

            node_id = 0;

            for (int y = 0; y < ph; y++) {
                HashSet<SIROperator> p = sched.getAllOperatorsInPhase(y);
                Iterator<SIROperator> it = p.iterator();
                while (it.hasNext()) {
                    SIROperator oper = it.next();
                    int id = NodeEnumerator.getSIROperatorId(oper);
                    int exec = ((Integer)phases.get(curr).get(node_id)).intValue();

                    int _iter = iteration.get(oper).intValue();

                    List<Tape> in = RegisterStreams.getNodeInStreams(oper);
                    List<Tape> out = RegisterStreams.getNodeOutStreams(oper);

                    for (int z = 0; z < in.size(); z++) {
                      Tape ns = in.get(z);
                      if (ns != null) {
                        SIROperator src = NodeEnumerator.getOperator(ns.getSource());       
                        int qsize = queue_size.get(makeVector(src, oper)).intValue();
                        qsize -= getPop(oper, z) * exec;
                        queue_size.put(makeVector(src, oper), 
                                       new Integer(qsize));
                      }
                    }
                    
                    for (int z = 0; z < out.size(); z++) {
                      Tape ns = out.get(z); 
                      if (ns != null) {  
                        SIROperator dst = NodeEnumerator.getOperator(ns.getDest());     
                        int qsize = queue_size.get(makeVector(oper, dst)).intValue();
                        qsize += getPush(oper, z) * exec;
                        queue_size.put(makeVector(oper, dst), 
                                       new Integer(qsize));
                      }
                    }

                    for (int z = 0; z < exec; z++, _iter++) {
                        if (!(oper instanceof SIRFilter)) continue;
            
                        HashSet<LatencyConstraint> cons = LatencyConstraints.getOutgoingConstraints((SIRFilter)oper);
                        Iterator<LatencyConstraint> ci = cons.iterator();
                        while (ci.hasNext()) {
                
                            //System.out.println("latency constraint");
                
                            LatencyConstraint c = ci.next();
                            SIRFilter dst = (SIRFilter)c.getReceiver();
                            boolean down = LatencyConstraints.isMessageDirectionDownstream((SIRFilter)oper, (SIRFilter)dst);
                            int init_c = c.getSourceInit();
                
                            if (down) {
                
                                if (_iter >= init_c) {
                    
                                    int s_steady = c.getSourceSteadyExec();
                                    int cycle = (_iter - init_c) / s_steady;
                                    int offs = (_iter - init_c) % s_steady;
                    
                                    int dep = c.getDependencyData(offs);
                    
                                    if (dep > 0) {
                                        int cc = dep + cycle * c.getDestSteadyExec();
                                        credit.put(dst, new Integer(cc));
                                        if (ClusterBackend.debugPrint)
                                            System.out.println("Send credit: "+cc);
                                    }
                                }
                
                            } else {
                
                                int s_steady = c.getSourceSteadyExec();
                                int cycle = _iter / s_steady;
                                int offs = _iter % s_steady;
                
                                int dep = c.getDependencyData(offs);
                                if (dep > 0) {
                                    int cc = dep + cycle * c.getDestSteadyExec();
                                    credit.put(dst, new Integer(cc));
                                    if (ClusterBackend.debugPrint)
                                        System.out.println("Send credit: "+cc);
                                }
                            }
                        }
                    }

                    iteration.put(oper, new Integer(_iter));


                }

        

            }    
        }



        boolean success = true;

        node_id = 0;

        Vector<Integer> new_phase = new Vector<Integer>();

        for (int y = 0; y < ph; y++) {
            HashSet<SIROperator> p = sched.getAllOperatorsInPhase(y);
            Iterator<SIROperator> it = p.iterator();
            while (it.hasNext()) {
                SIROperator oper = it.next();
                int id = NodeEnumerator.getSIROperatorId(oper);

                int steady_count = ((Integer)phases.get(combine_ptr).get(node_id)).intValue() +
                    ((Integer)phases.get(combine_ptr+1).get(node_id)).intValue();

                node_id++;

                int _iter = iteration.get(oper).intValue();
                int _credit = credit.get(oper).intValue();

                int exec = steady_count;

                if (_credit >= 0 && _iter + steady_count > _credit) {
                    exec = _credit - _iter; 
                }

                int input = 2000000000;

                List<Tape> in = RegisterStreams.getNodeInStreams(oper);
                List<Tape> out = RegisterStreams.getNodeOutStreams(oper);
        
                for (int z = 0; z < in.size(); z++) {
                  Tape ns = in.get(z);
                  if (ns != null) {
                    SIROperator src = NodeEnumerator.getOperator(ns.getSource());       
                    int qsize = queue_size.get(makeVector(src, oper)).intValue();
                    int peek = getPeek(oper, z);
                    int pop = getPop(oper, z);
                    if (peek < pop) peek = pop;
                    int extra = peek - pop;

                    int can = (qsize-extra)/pop;
                    if (can < 0) can = 0;
                    if (can < input) input = can;
                  }
                }
                
                if (input < exec) exec = input;

                //System.out.println("CREDIT = "+_credit+" INPUT = "+input+" EXEC = "+exec);
        
                for (int z = 0; z < in.size(); z++) {
                  Tape ns = in.get(z);
                  if (ns != null) {
                    SIROperator src = NodeEnumerator.getOperator(ns.getSource());       
                    int qsize = queue_size.get(makeVector(src, oper)).intValue();
                    qsize -= getPop(oper, z) * exec;
                    queue_size.put(makeVector(src, oper), 
                                   new Integer(qsize));
                  }
                }

                for (int z = 0; z < out.size(); z++) {
                  Tape ns = out.get(z);
                  if (ns != null) {
                    SIROperator dst = NodeEnumerator.getOperator(ns.getDest());     
                    int qsize = queue_size.get(makeVector(oper, dst)).intValue();
                    qsize += getPush(oper, z) * exec;
                    queue_size.put(makeVector(oper, dst), 
                                   new Integer(qsize));
                  }
                }

                if (ClusterBackend.debugPrint)
                    System.out.println(oper.getName()+" Exec = "+exec+"/"+steady_count);

                if (exec < steady_count) success = false;

                new_phase.add(new Integer(exec)); // push exec count to phase vector

                for (int z = 0; z < exec; z++, _iter++) {
                    if (!(oper instanceof SIRFilter)) continue;
            
                    HashSet<LatencyConstraint> cons = LatencyConstraints.getOutgoingConstraints((SIRFilter)oper);
                    Iterator<LatencyConstraint> ci = cons.iterator();
                    while (ci.hasNext()) {

                        //System.out.println("latency constraint");

                        LatencyConstraint c = ci.next();
                        SIRFilter dst = (SIRFilter)c.getReceiver();
                        boolean down = LatencyConstraints.isMessageDirectionDownstream((SIRFilter)oper, (SIRFilter)dst);
                        int init_c = c.getSourceInit();
            
                        if (down) {
                
                            if (_iter >= init_c) {
                
                                int s_steady = c.getSourceSteadyExec();
                                int cycle = (_iter - init_c) / s_steady;
                                int offs = (_iter - init_c) % s_steady;
                
                                int dep = c.getDependencyData(offs);

                                //System.out.println("dep = "+dep);
                                //System.out.println("c = "+cycle);
                                //System.out.println("o = "+offs);

                                if (dep > 0) {
                                    int cc = dep + cycle * c.getDestSteadyExec();
                                    credit.put(dst, new Integer(cc));
                                    if (ClusterBackend.debugPrint)
                                        System.out.println("Send credit: "+cc);
                                }
                            }
                
                        } else {
                
                            int s_steady = c.getSourceSteadyExec();
                            int cycle = _iter / s_steady;
                            int offs = _iter % s_steady;
                
                            int dep = c.getDependencyData(offs);
                            if (dep > 0) {
                                int cc = dep + cycle * c.getDestSteadyExec();
                                credit.put(dst, new Integer(cc));
                                if (ClusterBackend.debugPrint)
                                    System.out.println("Send credit: "+cc);
                            }
                        }
                    }
                }

                iteration.put(oper, new Integer(_iter));
            }
        }
    
        if (ClusterBackend.debugPrint)
            System.out.println(success?"SUCCESS":"FAILED");

        if (success) {
            phases.set(combine_ptr, new_phase);
            phases.remove(combine_ptr+1);
            loop_start--;

        } else {
            combine_ptr++;
        }
    }

}
