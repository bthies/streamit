package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This flattens certain cases of split/joins into a single filter.
 */
public class SJFlatten 
{
    /**
     * Performs a depth-first traversal of an SIRStream tree, and
     * calls flatten() on any SIRSplitJoins as a post-pass.
     */
    public static SIRStream doFlatten(SIRStream str)
    {
        // First, visit children (if any).
        if (str instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
            fl.setBody((SIRStream)doFlatten(fl.getBody()));
            fl.setLoop((SIRStream)doFlatten(fl.getLoop()));
        }
        if (str instanceof SIRPipeline)
        {
            SIRPipeline pl = (SIRPipeline)str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                pl.replace(child, child, (SIRStream)doFlatten(child));
            }
        }
        if (str instanceof SIRSplitJoin)
        {
            SIRSplitJoin sj = (SIRSplitJoin)str;
            // This is painful, but I don't feel like adding code to
            // SIRSplitJoin right now.
            LinkedList ll = new LinkedList();
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                ll.add(doFlatten(child));
            }
            // Replace the children of the split/join with the flattened list.
            sj.setParallelStreams(ll);
        }
        
        // Having recursed, do the flattening, if it's appropriate.
        if (str instanceof SIRSplitJoin)
            str = flatten((SIRSplitJoin)str);
        
        // All done, return the object.
        return str;
    }
    
    /**
     * Flattens a split/join with a duplicate splitter and a
     * round-robin joiner.  This is subject to the following
     * constraints:
     *  1. The pop ratio of each of the child objects is equal.  (This
     *     implementation requires that ratio to be exactly 1.)
     *  2. The joiner is a round-robin or weighted round-robin, and
     *     the push ratios of the child objects exactly match the
     *     round-robin weights.
     * If these conditions are met, we can flatten the split/join down
     * to a single filter, eliminating the copies for the splitter and
     * joiner.
     */
    public static SIRStream flatten(SIRSplitJoin sj)
    {
        // Things that we use often:
        SIRSplitter splitter = sj.getSplitter();
        SIRJoiner joiner = sj.getJoiner();
        boolean dupSplitter;
        
        // Check that we meet the conditions; return the original
        // split/join if we don't.
        if (splitter.getType() == SIRSplitType.DUPLICATE)
            dupSplitter = true;
        else if (splitter.getType() == SIRSplitType.ROUND_ROBIN ||
                 splitter.getType() == SIRSplitType.WEIGHTED_RR)
            dupSplitter = false;
        else
            return sj;
        if (joiner.getType() != SIRJoinType.ROUND_ROBIN &&
            joiner.getType() != SIRJoinType.WEIGHTED_RR)
            return sj;
        
        // Check the ratios.
        Iterator childIter = sj.getParallelStreams().iterator();
        int i = 0;
        int maxPeek = 0;
        int pushes = 0;
        while (childIter.hasNext())
        {
            SIRStream str = (SIRStream)childIter.next();
            if (!(str instanceof SIRFilter))
                return sj;
            SIRFilter filter = (SIRFilter)str;
            if (dupSplitter && filter.getPopInt() != 1)
                return sj;
            if (splitter.getType() == SIRSplitType.ROUND_ROBIN &&
                filter.getPopInt() != 1)
                return sj;
            if (splitter.getType() == SIRSplitType.WEIGHTED_RR &&
                filter.getPopInt() != splitter.getWeights()[i])
                return sj;
            if (!dupSplitter && filter.getPeekInt() != filter.getPopInt())
                return sj;
            if (joiner.getType() == SIRJoinType.ROUND_ROBIN &&
                filter.getPushInt() != 1)
                return sj;
            if (joiner.getType() == SIRJoinType.WEIGHTED_RR &&
                filter.getPushInt() != joiner.getWeights()[i])
                return sj;
            // Looks okay.  Increment counts for things.
            if (filter.getPeekInt() > maxPeek)
                maxPeek = filter.getPeekInt();
            if (joiner.getType() == SIRJoinType.ROUND_ROBIN)
                pushes++;
            else if (joiner.getType() == SIRJoinType.WEIGHTED_RR)
                pushes += filter.getPushInt();
            i++;
        }

        // Rename all of the child streams of this.
        RenameAll ra = new RenameAll();
        List children = new LinkedList();
        Iterator iter = sj.getParallelStreams().iterator();
        while (iter.hasNext())
        {
            SIRFilter str = (SIRFilter)iter.next();
            children.add(ra.renameFilter(str));
        }

        // So at this point we have an eligible split/join; flatten it.
        JMethodDeclaration newWork =
            getWorkFunction(sj, children, dupSplitter);
        JFieldDeclaration[] newFields = getFields(sj, children);
        JMethodDeclaration[] newMethods = getMethods(sj, children);

        // Make names of things within the split/join unique.
        ra.findDecls(newFields);
        ra.findDecls(newMethods);
        for (i = 0; i < newFields.length; i++)
            newFields[i] = (JFieldDeclaration)newFields[i].accept(ra);
        for (i = 0; i < newMethods.length; i++)
            newMethods[i] = (JMethodDeclaration)newMethods[i].accept(ra);
        newWork = (JMethodDeclaration)newWork.accept(ra);

        // Get the init function now, using renamed names of things.
        JMethodDeclaration newInit = getInitFunction(sj, children, ra);

        // Build the new filter.
        SIRFilter newFilter = new SIRFilter(sj.getParent(),
                                            "SJFlatten_" + sj.getIdent(),
                                            newFields,
                                            newMethods,
                                            new JIntLiteral(maxPeek),
                                            // For now, pop fixed at 1.
                                            new JIntLiteral(1),
                                            new JIntLiteral(pushes),
                                            newWork,
                                            sj.getInputType(),
                                            sj.getOutputType());
        
        // Use the new init function.
        newFilter.setInit(newInit);

        // Replace the init function in the parent.
	//        RenameAll.replaceParentInit(sj, newFilter);

        return newFilter;
    }

    private static JMethodDeclaration getInitFunction(SIRSplitJoin sj,
                                                      List children,
                                                      final RenameAll ra)
    {
        // Start with the init function from the split/join.
        JMethodDeclaration md = sj.getInit();
        // Replace any init statements with inlined versions of
        // the relevant functions.  Since renaming has already
        // happened (yes?), the parameters have unique names.
        md = (JMethodDeclaration)md.accept(new SLIRReplacingVisitor() {
                public Object visitInitStatement(SIRInitStatement self,
                                                 JExpression[] args,
                                                 SIRStream target)
                {
                    JMethodDeclaration initFn = target.getInit();
                    initFn = (JMethodDeclaration)initFn.accept(ra);
                    JBlock block = new JBlock(null, new JStatement[0], null);
                    // Add declarations/assignments for the parameters.
                    JVariableDefinition[] vardefs =
                        new JVariableDefinition[args.length];
                    for (int i = 0; i < args.length; i++)
                    {
                        JFormalParameter param = initFn.getParameters()[i];
                        vardefs[i] =
                            new JVariableDefinition(self.getTokenReference(),
                                                    param.getModifiers(),
                                                    param.getType(),
                                                    param.getIdent(),
                                                    args[i]);
                    }
                    JVariableDeclarationStatement vds =
                    new JVariableDeclarationStatement(self.getTokenReference(),
                                                      vardefs, null);
                    block.addStatement(vds);
                    
                    // Add the body of the init statement.
                    block.addAllStatements(initFn.getStatements());
                    return block;
                }
            });
        return md;
    }

    private static JMethodDeclaration getWorkFunction(SIRSplitJoin sj,
                                                      List children,
                                                      boolean dupSplitter)
    {
        // Build the new work function; add the list of statements
        // from each of the component filters.
        JBlock newStatements = new JBlock(null, new JStatement[0], null);
        Iterator childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            // Get the old work function (have to clone the function
            // instead of the list at this point.)
            JMethodDeclaration old = filter.getWork();
            JMethodDeclaration oldClone = 
		(JMethodDeclaration)ObjectDeepCloner.deepCopy(old);
	    // get the new statements
	    List statements = oldClone.getStatements();
            // Add a block containing these statements.
            JStatement[] stmts = new JStatement[statements.size()];
            stmts = (JStatement[])statements.toArray(stmts);
            newStatements.addStatement(new JBlock(null, stmts, null));
        }
        
        // If there's a duplicating splitter, we need to turn pops
        // into peeks and add in a pop at the end to make everything
        // work.  For a round-robin splitter, leaving pops as pops
        // DTRT.
        if (dupSplitter)
        {
            // Replace every pop with a peek(0).
            // TODO: support more than one pop with more intelligent
            // peeking.
            // TODO: think harder about the location of peeks in
            // relation to the pop.
            newStatements.accept(new SLIRReplacingVisitor() {
                    public Object visitPopExpression(SIRPopExpression oldSelf,
                                                     CType oldTapeType)
                    {
                        // Recurse into children.
                        SIRPopExpression self = (SIRPopExpression)
                            super.visitPopExpression(oldSelf,
                                                     oldTapeType);
                        // Produce the new peek expression.
                        SIRPeekExpression peek =
                            new SIRPeekExpression(self.getTokenReference(),
                                                  new JIntLiteral(0));
                        // Set the tape type appropriately.
                        peek.setTapeType(self.getType());
                        // And return it.
                        return peek;
                    }
                });
            
            // Add a single pop at the end.
            SIRPopExpression pop = new SIRPopExpression();
            pop.setTapeType(sj.getInputType());
            newStatements.addStatement(new JExpressionStatement
                                       (null, pop, null));
        }
        
        // Now make a new work function based on this.
        JMethodDeclaration newWork =
            new JMethodDeclaration(null,
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   "work",
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   newStatements,
                                   null,
                                   null);

        return newWork;
    }

    private static JFieldDeclaration[] getFields(SIRSplitJoin sj,
                                                 List children)
    {
        Iterator childIter;
        
        // Walk the list of children to get the total field length.
        int numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            numFields += filter.getFields().length;
        }
        
        // Now make the field array...
        JFieldDeclaration[] newFields = new JFieldDeclaration[numFields];
        
        // ...and copy all of the fields in.
        numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            for (int i = 0; i < filter.getFields().length; i++)
                newFields[numFields + i] = filter.getFields()[i];
            numFields += filter.getFields().length;
        }
        
        // All done.
        return newFields;
    }

    private static JMethodDeclaration[] getMethods(SIRSplitJoin sj,
                                                   List children)
    {
        // Just copy all of the methods into an array.
        Iterator childIter;
        
        // Walk the list of children to get the total number of methods.
        // Skip init and work functions wherever necessary.
        int numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            numMethods += filter.getMethods().length - 2;
        }
        
        // Now make the method array...
        JMethodDeclaration[] newMethods = new JMethodDeclaration[numMethods];
        
        // ...and copy all of the methods in.
        numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            for (int i = 0; i < filter.getMethods().length; i++)
            {
                JMethodDeclaration method = filter.getMethods()[i];
                if (method != filter.getInit() &&
                    method != filter.getWork())
                    newMethods[numMethods++] = method;
            }
        }
        
        // All done.
        return newMethods;
    }
}

