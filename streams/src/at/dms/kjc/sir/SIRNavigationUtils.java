package at.dms.kjc.sir;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



public class SIRNavigationUtils {

    /**
     * Get set of all SIROperators that are not SIRContainers immediately preceeding a SIROperator
     * (If passed first oper in graph then returns empty set).
     * Difference from {@link SIRNavigationUtils#getPredecessorOper(SIRStream)} is that handles SIRSplitter and
     * handles SIRJoiner returning set containing last operator on each incoming edge.
     * @param str SIROperator that we want to find preceeding operators for.
     * @return Set of preceeding operators.
     */
     public static Set<SIROperator> getPredecessorOpers(SIROperator str) {
        if (str instanceof SIRSplitter) {
            if (str.getParent() instanceof SIRFeedbackLoop) {
                return Collections.singleton(SIRNavigationUtils.getLastOper(((SIRFeedbackLoop)str.getParent()).getBody()));
            } else {
                return getPredecessorOpers(str.getParent());
            }
        } else if (str instanceof SIRJoiner) {
            Set<SIROperator> retval = new HashSet<SIROperator>();
            if (str.getParent() instanceof SIRFeedbackLoop) {
                retval.add(SIRNavigationUtils.getLastOper(((SIRFeedbackLoop)str.getParent()).getLoop()));
                retval.addAll(getPredecessorOpers(str.getParent()));
            } else {
                for (SIRStream s : ((SIRSplitJoin)str.getParent()).getParallelStreams()) {
                    retval.add(SIRNavigationUtils.getLastOper(s));
                }
            }
            return retval;
        } else {
            SIROperator op = SIRNavigationUtils.getPredecessorOper((SIRStream)str);
            if (op == null) {
                return Collections.EMPTY_SET;
            } else {
                return Collections.singleton(op);
            }
        }
    }

    /**
     * Return the final non-container operator in the graph structure of <b>op</b>
     * @param op A SIROperator, may be a SIRContainer, not null
     * @return The final SIROperator in the passed structure that is not a SIRContainer.
     */
    public static SIROperator getLastOper(SIROperator op) {
        // recurse down through op to find it's first
        // child that is not a container.
        while (op instanceof SIRContainer) {
            if (op instanceof SIRFeedbackLoop) {
                // Feedback loop children are ordered as joiner, body, splitter, loop
                // but last SIROperator in the graph is the splitter.
                return ((SIRFeedbackLoop)op).getSplitter();
            }
            // for pipeline or splitjoin, taking the last child works.
            List<SIROperator> succChildren = ((SIRContainer)op).getChildren();
            op = succChildren.get(succChildren.size() - 1);
        }
        return op;
    }

    /**
     * Return the first non-container operator in the graph structure of <b>op</b>
     * @param op  A SIROperator, may be a SIRContainer, not null
     * @return The first SIROperator in the passed structure that is not a SIRContainer.
     */
    public static SIROperator getFirstOper(SIROperator op) {
        // recurse down through op to find it's first
        // child that is not a container.
        while (op instanceof SIRContainer) {
            op = (SIROperator)((SIRContainer)op).getChildren().get(0);
        }
        return op;
    }

    /**
     * Get set of all SIROperators that are not SIRContainers immediately following a SIROperator
     * (If passed last oper in graph then returns empty set).
     * Difference from {@link SIRNavigationUtils#getSuccessorOper(SIRStream)} is that handles SIRJoiner and
     * handles SIRSplitter returning set containing first operator on each outgoing edge.
     * @param str SIROperator that we want to find following operators for.
     * @return Set of following operators.
     */
    public static Set<SIROperator> getSuccessorOpers(SIROperator str) {
        if (str instanceof SIRJoiner) {
            if (str.getParent() instanceof SIRFeedbackLoop) {
                return Collections.singleton(getFirstOper(((SIRFeedbackLoop)str.getParent()).getBody()));
            } else {
                return getSuccessorOpers(str.getParent());
            }
        } else if (str instanceof SIRSplitter) {
            Set<SIROperator> retval = new HashSet<SIROperator>();
            if (str.getParent() instanceof SIRFeedbackLoop) {
                retval.add(getFirstOper(((SIRFeedbackLoop)str.getParent()).getLoop()));
                retval.addAll(getSuccessorOpers(str.getParent()));
            } else {
                for (SIRStream s : ((SIRSplitJoin)str.getParent()).getParallelStreams()) {
                    retval.add(getFirstOper(s));
                }
            }
            return retval;
        } else {
            SIROperator op = SIRNavigationUtils.getSuccessorOper((SIRStream)str);
            if (op == null) {
                return Collections.EMPTY_SET;
            } else {
                return Collections.singleton(op);
            }
        }
    }

    /**
     * Find a stream's predecessor operator in its parent.
     * Should return filter, splitter, joiner, or possibly recursive stub.
     * @param str  stream that we wish to find predecessor of.
     * @return  null if at beginning of program, else following SIROperator which is not a SIRContainer in stream graph.
     */
    public static SIROperator getPredecessorOper(SIRStream str) {
        SIRContainer parent = str.getParent();
        if (parent instanceof SIRSplitJoin) {
            return ((SIRSplitJoin)parent).getSplitter();
        } else if (parent instanceof SIRFeedbackLoop) {
            if (str == ((SIRFeedbackLoop)parent).getBody()) {
                return ((SIRFeedbackLoop)parent).getJoiner();
            } else {
                assert str == ((SIRFeedbackLoop)parent).getLoop()
                : "Parent link for " + str + " points to " + parent + " which does not include it.";
                return ((SIRFeedbackLoop)parent).getSplitter();
            }
        } else {
            assert parent instanceof SIRPipeline;
            List<SIROperator> children = parent.getChildren();
            int i = children.indexOf(str);
            assert i != -1 : "Parent link for " + str + " points to " + parent + " which does not include it.";
            SIROperator pred;
            if (i == 0) {
                // if it's at the end of the whole program, return null
                if (parent.getParent()==null) {
                    return null;
                } else {
                    // otherwise, go for first element in parent's successor
                    pred = getPredecessorOper(parent);
                }
            } else {
                pred = children.get(i - 1);
            }
            return getLastOper(pred);
        }
    }

    /**
     * Find a stream's successor operator in its parent.
     * Should return filter, splitter, joiner.
     * @param str  stream that we wish to find successor of.
     * @return  null if at end of program, else following SIROperator which is not a SIRContainer in stream graph.
     */
    public static SIROperator getSuccessorOper(SIRStream str) {
        SIRContainer parent = str.getParent();
        if (parent instanceof SIRSplitJoin) {
            return ((SIRSplitJoin)parent).getJoiner();
        } else if (parent instanceof SIRFeedbackLoop) {
            if (str == ((SIRFeedbackLoop)parent).getBody()) {
                return ((SIRFeedbackLoop)parent).getSplitter();
            } else {
                assert str == ((SIRFeedbackLoop)parent).getLoop()
                : "Parent link for " + str + " points to " + parent + " which does not include it.";
                return ((SIRFeedbackLoop)parent).getJoiner();
            }
        } else {
            assert parent instanceof SIRPipeline;
            List<SIROperator> children = parent.getChildren();
            int i = children.indexOf(str);
            assert i != -1 : "Parent link for " + str + " points to " + parent + " which does not include it.";
            SIROperator succ;
            if (i == children.size() - 1) {
                // if it's at the end of the whole program, return null
                if (parent.getParent() == null) {
                    return null;
                } else {
                    // otherwise, go for first element in parent's successor
                    succ = getSuccessorOper(parent);
                }
            } else {
                succ = children.get(i + 1);
            }
            return getFirstOper(succ);
        }
    }

    /**
     * Get the common ancestor of all SIROperators in collection.
     * The usual boundary cases apply:
     * The common ancestor of an empty collection is null;
     * The common ancestor of a singleton collection is the element;
     * The common ancestor of a collection where two elements have no common ancestor is null.
     * @param ss a collection of SIROperators (or of any subtype of SIROperator).
     * @return the common ancestor, may be null.
     */
    public static <T extends SIROperator> SIROperator commonAncestor (Collection<T> ss) {
        // just call pairwise version.
        Iterator<T> iter = ss.iterator();
        SIROperator common = null;
        if (iter.hasNext()) {
            common = iter.next();
        }
        while (iter.hasNext() && common != null) {
            common = SIRNavigationUtils.commonSIRAncestor(common,iter.next());
        }
        return common;
    }

    /**
     * Get the common ancestor of <b>s1</b> and <b>s2</b>.
     * Common ancestor of <b>s</b> and <b>s</b> is <b>s</b>.
     * Return null if no common ancestor.
     * @param s1 one SIROperator
     * @param s2 the other SIROperator
     * @return the common ancestor or null
     */
    
    public static SIROperator commonSIRAncestor (SIROperator s1, SIROperator s2) {
        // brute force method: return last element of common prefix of parents.
        if (s1 == s2) { return s1; }
        List<SIROperator> anc1 = s1.getAncestors();
        List<SIROperator> anc2 = s2.getAncestors();
        SIROperator common = null;
        for (int i = anc1.size()-1, j = anc2.size() - 1; i >= 0 && j>= 0; i--, j--) {
            if (anc1.get(i) == anc2.get(j)) {
                common = anc1.get(i);
            } else {
                break;
            }
        }
        return common;
    }

}
