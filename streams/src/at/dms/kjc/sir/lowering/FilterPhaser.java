package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import java.util.*;

/**
 * This class resolves phases in work functions.  It should be run
 * after constant prop.  Coming out of Kopi2SIR, a work function in a
 * phased filter is a normal function which happens to contain
 * SIRPhaseInvocation statements.  After constant prop, it should have
 * no control flow at all.  This pass examines the work function.  Any
 * local variables are promoted to filter fields, then any remaining
 * code is converted into phases.  Following this, the work function
 * should consist only of a series of phase invocations.  The phases
 * array of the filter is populated with the referenced phases, in
 * order.
 */
public class FilterPhaser extends EmptyStreamVisitor
{
	private BlockFlattener blockFlattener;
	private VarDeclRaiser varDeclRaiser;
	private int phaseCount;
	
	/**
	 * Internal constructor.
	 */
	private FilterPhaser()
	{
		blockFlattener = new BlockFlattener();
		varDeclRaiser = new VarDeclRaiser();
		phaseCount = 0;
	}
	
	/**
	 * Resolve phases in work functions.
	 * @see         at.dms.kjc.sir.lowering.FilterPhaser
	 * @param str   The stream object to resolve phases in
	 */
    public static void resolvePhasedFilters(SIRStream str)
    {
        FilterPhaser phaser = new FilterPhaser();
        IterFactory.createFactory().createIter(str).accept(phaser);
    }
    
    public void visitPhasedFilter(SIRPhasedFilter str, SIRPhasedFilterIter iter)
    {
    	JMethodDeclaration work = str.getWork();
		work.accept(blockFlattener);
		work.accept(varDeclRaiser);
		promoteLocalsToFields(str, work);
		createExtraPhases(str, work);
		buildPhaseList(str, work);
    }
    
    /**
     * Promote local variables in a work function to fields in a stream.
     * Requires that the variables be the first statements in work, so
     * a VarDeclRaiser pass should have already been run over it.
     * 
     * @param str    Stream in which to produce fields
     * @param work   Function to promote locals from
     */
    private void promoteLocalsToFields(SIRStream str, JMethodDeclaration work)
    {
    	JBlock body = work.getBody();
    	int pos = 0;
    	
    	while (pos < body.size())
    	{
    		JStatement stmt = body.getStatement(pos);
    		// All done if this isn't a variable declaration.
    		if (!(stmt instanceof JVariableDeclarationStatement)) return;
    		JVariableDefinition[] vars = ((JVariableDeclarationStatement)stmt).getVars();
    		JFieldDeclaration[] fields = new JFieldDeclaration[vars.length];
    		for (int i = 0; i < vars.length; i++)
    		{
    			fields[i] = new JFieldDeclaration(vars[i].getTokenReference(),
    											  vars[i], null, null);
    		}
    		str.addFields(fields);
    		body.removeStatement(pos);
    	}
    }
    
    /**
     * Create extra phases from the body of a work function.  Find
     * sequences of statements that aren't SIRPhaseInvocation
     * statements, and convert them into new phases.
     * 
     * @param str    Phased filter to create extra phases in
     * @param work   Work function to scan for excess code
     */
    private void createExtraPhases(SIRPhasedFilter str,
                                   JMethodDeclaration work)
    {
    	JBlock body = work.getBody();
        int pos = 0;
    	JBlock currentBody = null;
    	JExpression zero = new JIntLiteral(0);
    	
    	// Loop through statements.
    	while (pos < body.size())
    	{
            JStatement stmt = body.getStatement(pos);
            // Is this a forbidden statement?  We only want to allow
            // a couple of things...
            if (stmt instanceof SIRPhaseInvocation)
            {
                pos++;
                // Create currentBody as a block if it exists.
                if (currentBody != null)
                {
                    String phaseName = createPhase(str, currentBody);
                    JMethodCallExpression call =
                        new JMethodCallExpression(null,
                                                  new JThisExpression(null),
                                                  phaseName,
                                                  new JExpression[0]);
                    SIRPhaseInvocation invocation =
                        new SIRPhaseInvocation(null, call,
                                               zero, zero, zero, null);
                    body.addStatement(pos, invocation);
                    pos++;
                    currentBody = null;
                }
            }
            else if (stmt instanceof JEmptyStatement ||
                     stmt instanceof JExpressionListStatement ||
                     stmt instanceof JExpressionStatement)
            {
                // Convert local variable expressions to field references.
                stmt = (JStatement)stmt.accept(new ReplacingVisitor() {
                        public Object visitLocalVariableExpression
                            (JLocalVariableExpression self, String ident)
                        {
                            return new JFieldAccessExpression
                                (self.getTokenReference(),
                                 new JThisExpression(self.getTokenReference()),
                                 ident);
                        }
                    });
                // These are okay.  Create a block if we need to:
                if (currentBody == null)
                    currentBody = new JBlock(null, Collections.EMPTY_LIST,
                                             null);
                currentBody.addStatement(stmt);
                body.removeStatement(pos);
            }
            else
            {
                at.dms.util.Utils.fail("Unexpected statement " + stmt + " in phased filter work function");
            }
    	}
    	// Deal with a trailing block, if one exists.
        if (currentBody != null)
        {
            String phaseName = createPhase(str, currentBody);
            JMethodCallExpression call =
                new JMethodCallExpression(null, new JThisExpression(null),
                                          phaseName, new JExpression[0]);
            SIRPhaseInvocation invocation =
                new SIRPhaseInvocation(null, call, zero, zero, zero, null);
            body.addStatement(invocation);
        }
    }
    
    /**
     * Scan a work function and produce a list of phases for a phased filter.
     * The work function must consist of only SIRPhaseInvocation statements
     * (meaning createExtraPhases() should have already been called).  This will
     * destroy the phase list in str, if it already has one.
     * 
     * @param str    Phased filter to create phase list in
     * @param work   Work function to scan for phases
     */
    private void buildPhaseList(SIRPhasedFilter str, JMethodDeclaration work)
    {
		JBlock body = work.getBody();
		JMethodDeclaration[] methods = str.getMethods();
		SIRWorkFunction[] phases = new SIRWorkFunction[body.size()];
		for (int i = 0; i < body.size(); i++)
		{
			SIRPhaseInvocation invocation = (SIRPhaseInvocation)(body.getStatement(i));
			String target = invocation.getCall().getIdent();
			// Look for the method:
			JMethodDeclaration decl = null;
			for (int j = 0; j < methods.length; j++)
			{
				if (methods[j].getName().equals(target))
				{
					decl = methods[j];
					break;
				}
			}
			at.dms.util.Utils.assert(decl != null);
			SIRWorkFunction wf =
				new SIRWorkFunction(invocation.getPeek(), invocation.getPop(),
								    invocation.getPush(), decl);
			phases[i] = wf;
		}
		str.setPhases(phases);
    }
    
    /**
     * Returns the name of the next phase to be generated, incrementing the
     * phase count.
     * 
     * @return String  Name of the next phase
     */
    private String nextPhaseName()
    {
    	String result = "_phase_" + phaseCount;
    	phaseCount++;
    	return result;
    }
    
	/**
     * Actually do the work of creating a phase, given the stream to create the
     * phase in and the body of the phase.
     * 
     * @param body
     */
    private String createPhase(SIRStream stream, JBlock body)
    {
    	String name = nextPhaseName();
    	JMethodDeclaration phase =
    		new JMethodDeclaration(null, 0, CStdType.Void, name, new JFormalParameter[0], new CClassType[0], body,
    							   null, null);
    	JMethodDeclaration[] newPhases = new JMethodDeclaration[1];
    	newPhases[0] = phase;
    	stream.addMethods(newPhases);
    	return name;
    }
}
