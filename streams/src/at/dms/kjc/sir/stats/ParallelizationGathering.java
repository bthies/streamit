package at.dms.kjc.sir.stats;

import at.dms.kjc.*;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.raw.*;

import java.util.*;
import java.io.*;

/**
 * This class gathers statistics about the parallelization potential
 * of stream programs.  It overrides the VisitFilter, PostVisitPipeline,
 * and PostVisitSplitJoin methods (extends EmptyStreamVisitor)
 * 11/12/03, v1.00 jnwong
 * v.1.10:
 *  Added communication cost parallelization, refined nocost
 *  option such that it only halves filters along the critical path.
*/

public class ParallelizationGathering {
    


    public ParallelizationGathering() {
	
    }

    //procedure that's called by Flattener.java
    public static void doit(SIRStream str, int processors) {
	new ParallelWork().doit(str, processors);
    }

    static class MutableInt {
	static int mutint;
    }
    
    
    static class ParallelWork extends SLIREmptyVisitor{

	//Toggles for type of synchronization I want to do
        static boolean commcost;
	static boolean syncremoval;
	/** Mapping from streams to work estimates. **/
	static HashMap<SIRFilter, Long> filterWorkEstimates;
	static HashMap<SIRFilter, Integer> filterCommunicationCosts;
	static HashMap<SIRFilter, Long> filterTimesFizzed;
	static Vector statelessFilters;
	static MutableInt numFilters;
	static SIRFilter lastFizzedFilter;
	static int[] computationCosts;
	static int[] communicationCosts;
	static Vector<String> bottleneckFilters;
	//additions for syncremoval support:
	static HashMap<SIRFilter, SIRFilter> fizzedFiltersToOriginal;
	static SIRStream originalStream;
	static HashMap<SIRStream, SIRStream> origToNewGraph;
	static HashMap<SIRStream, SIRStream> newToOrigGraph;
	static boolean printgraphs;
	static boolean synccost;

	public ParallelWork() {
	    filterWorkEstimates = new HashMap<SIRFilter, Long>();
	    filterTimesFizzed = new HashMap<SIRFilter, Long>();
	    filterCommunicationCosts = new HashMap<SIRFilter, Integer>();
	    statelessFilters = new Vector();
	    numFilters = new MutableInt();
	    lastFizzedFilter = null;
	    bottleneckFilters = new Vector<String>();
	    fizzedFiltersToOriginal = new HashMap<SIRFilter, SIRFilter>();
	    originalStream = null;
	    origToNewGraph = null;
	    newToOrigGraph = null;
	    printgraphs = false;
	    commcost = true;
	    synccost = true;
	    syncremoval = false;
	}


	public static void doit(SIRStream str, int numprocessors)
	{
	    originalStream = str;
	    computationCosts = new int[numprocessors];
	    communicationCosts = new int[numprocessors];
	    	    
	    if(syncremoval)
	    {
		//StatelessDuplicate.USE_ROUNDROBIN_SPLITTER = true;
	    }
	    else
	    {
		//StatelessDuplicate.USE_ROUNDROBIN_SPLITTER = false;
	    }
	    if(commcost) {       	
		SIRStream copiedStr = (SIRStream) ObjectDeepCloner.deepCopy(str);
		GraphFlattener initFlatGraph = new GraphFlattener(copiedStr);
		HashMap[] initExecutionCounts = RawBackend.returnExecutionCounts(copiedStr, initFlatGraph);

		origToNewGraph = new HashMap<SIRStream, SIRStream>();
		newToOrigGraph = new HashMap<SIRStream, SIRStream>();
		WorkEstimate initWork = WorkEstimate.getWorkEstimate(copiedStr);
		
		createFilterMappings(origToNewGraph, newToOrigGraph, str, copiedStr);  //sync thing!
		IterFactory.createFactory().createIter(str).accept(new BasicVisitor(filterTimesFizzed));
		IterFactory.createFactory().createIter(copiedStr).accept
		    (new ParallelVisitor(initFlatGraph, initWork, newToOrigGraph, fizzedFiltersToOriginal, 
					 filterWorkEstimates, filterCommunicationCosts, filterTimesFizzed, 
					 initExecutionCounts, numFilters));	       
	    
		SIRFilter initHighestFilter = getHighestWorkFilterSync(1);
		for(int i = 2; i < numprocessors + 1; i++)
		{		
		    //sync implementation
		    filterWorkEstimates = new HashMap<SIRFilter, Long>();
		    filterCommunicationCosts = new HashMap<SIRFilter, Integer>();
		    statelessFilters = new Vector();
		    GraphFlattener flatGraph = new GraphFlattener(copiedStr);
		    if(syncremoval){
			//System.err.println("lifting!");		
			Lifter.lift(copiedStr);
			//String testOutputDot = new String("fizz-" + i + ".dot");
			//StreamItDot.printGraph(copiedStr, testOutputDot);
		    }		   
		    //Lifter.lift(copiedStr);    //do sync removal if syncremoval is true		    
		    //get number of prints, so I can divide everything by it to get
		    //executions per steady state
		    String outputDot = new String("at-fizz-" + i + ".dot");
		    StreamItDot.printGraph(copiedStr, outputDot);
		    HashMap[] filterExecutionCosts = RawBackend.returnExecutionCounts(copiedStr, flatGraph);
		    WorkEstimate work = WorkEstimate.getWorkEstimate(copiedStr);
		    //System.err.println("Now visiting " + copiedStr.getName());
		    IterFactory.createFactory().createIter(copiedStr).accept
			(new ParallelSyncVisitor(flatGraph, work, filterExecutionCosts, 
						 filterWorkEstimates, filterCommunicationCosts, 
						 fizzedFiltersToOriginal, filterTimesFizzed, 
						 newToOrigGraph, synccost));
		    copiedStr = (SIRStream) ObjectDeepCloner.deepCopy(originalStream);
		    parallelizesync(copiedStr, i);	
		}	    		
		generateOutputFiles();
		//int numfilters = filterWorkEstimates.size();	
		//System.err.println("Number of filters is " + MutableInt.mutint);

	    }//if(commcost)
	    else
	    {
		WorkEstimate initWork = WorkEstimate.getWorkEstimate(str);
		GraphFlattener initFlatGraph = new GraphFlattener(str);
		HashMap[] initExecutionCounts = RawBackend.returnExecutionCounts(str, initFlatGraph);
		
		IterFactory.createFactory().createIter(str).accept
		    (new NoSyncVisitor(initWork, filterWorkEstimates,
				       filterCommunicationCosts, filterTimesFizzed,
				       initExecutionCounts, initFlatGraph, numFilters));
		report(1);
		for(int i = 2; i < numprocessors + 1; i++)
		{
		    parallelize();
		    report(i);
		}
		generateOutputFiles();
	    }//endif(commcost)

	}


	/**   parallelizesync(copiedStr):
	 * Similar to parallelize, but it does not use greedy approach, calculates everything straight 
	 * from originalStream.  Also increments part of HashMap filterTimesFizzed, depending on what it fizzed.
	 * returns the new parallelized stream in copiedStr.
	 */

	public static void parallelizesync(SIRStream copiedStr, int numprocessors)
	{	    	    
	    SIRFilter nextFizzedFilter = getHighestWorkFilterSync(numprocessors);  //returns real original filter, reports
	    //copiedStr = (SIRStream) ObjectDeepCloner.deepCopy(originalStream);
	    //System.err.println("next fizzed filter is " + nextFizzedFilter.getName());	    
	    int oldfizzfactor = filterTimesFizzed.get(nextFizzedFilter).intValue();
	    filterTimesFizzed.put(nextFizzedFilter, new Long(oldfizzfactor + 1));
	    Iterator<SIRFilter> filtersToFizz = filterTimesFizzed.keySet().iterator();

	    //System.err.println("next fizzed filter is " + nextFizzedFilter.getName() 
	    //	       + " with fizzfactor " + oldfizzfactor);

	    //do the mappings!
	    origToNewGraph.clear(); 
	    newToOrigGraph.clear();
	    createFilterMappings(origToNewGraph, newToOrigGraph, originalStream, copiedStr);

	    while(filtersToFizz.hasNext())
	    {
		SIRFilter currentFilter = filtersToFizz.next();
		int timestofizz = filterTimesFizzed.get(currentFilter).intValue();
		//System.err.println("Fizzing " + timestofizz + " times");
		if(timestofizz > 1)
		{
		    SIRFilter fizzingFilter = (SIRFilter)origToNewGraph.get(currentFilter);	       
		    //System.err.println("Fizzing " + fizzingFilter.getName());
		    if(StatelessDuplicate.isFissable(fizzingFilter))
		    {
			SIRSplitJoin newFilters = StatelessDuplicate.doit(fizzingFilter, timestofizz);
			//populate fizzedFiltersToOriginal HashMap
			for(int i = 0; i < newFilters.getParallelStreams().size(); i++)
			{			
			    SIRFilter splitFilter = (SIRFilter) newFilters.get(i); 		    
			    fizzedFiltersToOriginal.put(splitFilter, currentFilter);
			}
		    }
		}
  
	    }
	    //System.err.println("This stream is called " + copiedStr.getName());

	}//void parallelizesync


	/**   getHighestWorkFilterSync:
	 *  Returns the *ORIGINAL* filter in the input stream with the highest overall cost (comp plus comm.),
	 *  also populates computationCosts, communicationCosts[] for reporting
	 */

	public static SIRFilter getHighestWorkFilterSync(int numprocessors)
	{
	    SIRFilter maxFilter = null;
	    long maxwork = Long.MIN_VALUE;
	    Iterator<SIRFilter> sirFilters = filterWorkEstimates.keySet().iterator();
	    int maxcomm = 0;
	    int maxcomp = 0;

	    while(sirFilters.hasNext())
	    {
		SIRFilter currentFilter = sirFilters.next();
		int currentcompcost = filterWorkEstimates.get(currentFilter).intValue();
		int currentcomcost = filterCommunicationCosts.get(currentFilter).intValue();
		int currentwork = currentcompcost + currentcomcost;
		//System.err.println("currentFilter of " + currentFilter.getName() 
		//		   + " has work " + currentwork);
		if(currentwork >= maxwork)
		{
		    maxFilter = currentFilter;
		    maxwork = currentwork;
		    maxcomm = currentcomcost;
		    maxcomp = currentcompcost;
		}
	    }
	    //System.err.println("inserting at " + (numprocessors - 1));
	    computationCosts[numprocessors - 1] = maxcomp;
	    communicationCosts[numprocessors - 1] = maxcomm;
	    bottleneckFilters.add(maxFilter.toString());
	    //System.err.println("fake filter is " + maxFilter.getName()
	    //     + " with maxwork " + maxwork + " and communication cost " + maxcomm);
	    SIRFilter realMaxFilter = null;
	    if(fizzedFiltersToOriginal.containsKey(maxFilter))
		realMaxFilter = fizzedFiltersToOriginal.get(maxFilter);
	    else
		realMaxFilter = (SIRFilter)newToOrigGraph.get(maxFilter);
	    return realMaxFilter;

	}//SIRFilter getHighestWorkFilterSync


	/**   createFilterMappings:
	 *  produces a mapping of filters from the original stream to the new stream and vice versa,
	 *  and stores these in two HashMaps.
	 */

	public static void createFilterMappings(HashMap<SIRStream, SIRStream> origToNew, HashMap<SIRStream, SIRStream> newToOrig, SIRStream origStream, SIRStream newStream)
	{
	    if(origStream instanceof SIRFilter)
	    {
		newToOrig.put(newStream, origStream);
		origToNew.put(origStream, newStream);
	    }
	    if(origStream instanceof SIRContainer)
	    {
		SIRContainer origContainer = (SIRContainer)origStream;
		for(int i=0; i < origContainer.size(); i++)
		{	
		    createFilterMappings(origToNew, newToOrig, origContainer.get(i), ((SIRContainer)newStream).get(i));
		}
	    }

	}//createFilterMappings
    
	/**    generateOutputFiles():
	 *  prints to various output files the results of the parallel modelling experiments.
	 *
	 */

	public static void generateOutputFiles() 
	{
	    try
	    {
		File compSyncFile = new File("compsync.txt");
		File commSyncFile = new File("commsync.txt");
		File totalSyncFile = new File("totalsync.txt");
		File filterFile = new File("bfilters.txt");
		File costsFile = new File("costs.txt");

		FileWriter compSync = new FileWriter(compSyncFile);
		FileWriter commSync = new FileWriter(commSyncFile);
		FileWriter totalSync = new FileWriter(totalSyncFile);
		FileWriter filter = new FileWriter(filterFile);
		FileWriter costsSync = new FileWriter(costsFile);
	       
		costsSync.write("Processors; Computation Cost; Communication Cost; Overall Cost; \n");
		for(int i = 0; i < computationCosts.length; i++)
		{
		    Long compCost = new Long(computationCosts[i]);
		    Integer commCost = new Integer(communicationCosts[i]);
		    long totalcost = compCost.intValue() + commCost.intValue();
		    Long totalCost = new Long(totalcost);
		    compSync.write(compCost.toString());
		    compSync.write(";");
		    commSync.write(commCost.toString());
		    commSync.write(";");
		    totalSync.write(totalCost.toString());
		    totalSync.write(";");
		    filter.write(bottleneckFilters.get(i));
		    filter.write(";");

		    //costsFile writing
		    Integer currentProcessor = new Integer(i+1);
		    costsSync.write(currentProcessor.toString());
		    String costLine = new String(";" + compCost.toString() + ";" + 
						 commCost.toString() + ";" + 
						 totalCost.toString() + "; \n");
		    costsSync.write(costLine);
		    //costsSync.write(";");
		    //costsSync.write(compCost.toString());
		    //costsSync.write(";");

		}

		costsSync.close();
		totalSync.close();
		filter.close();
		compSync.close();
		commSync.close();
	    }
	    
	    catch(IOException e)
	    {
		System.err.println("File Output Error with message " + e.getMessage());
	    }


	}//generateOutputFiles()



	/*  report():
	 *  Print to int[]computationCosts and int[]communicationCosts 
	 *  both the communication and the computation costs of the bottleneck filter, takes current number of processors as an input.	
	 */
	public static void report(int currentprocessors)
	{
	    SIRFilter maxFilter = getHighestWorkFilter();
	    int maxwork = filterWorkEstimates.get(maxFilter).intValue();
	    int commcost = filterCommunicationCosts.get(maxFilter).intValue();
	    computationCosts[currentprocessors - 1] = maxwork;
	    communicationCosts[currentprocessors - 1] = commcost;
	    bottleneckFilters.add(maxFilter.toString());
	    //System.err.println(maxwork + "        " + fancost);	    
	}
    
	
	/*  parallelize();
	 *  takes the highest work stateless filter on the critical path, cuts it in half, putting
	 *  new work into filterWorkEstimates (replacing the old mapping)
	 */
	public static void parallelize()
	{
	    //SIRFilter maxFilter = getHighestWorkFilter();
	    SIRFilter maxFilter = getOptimalFizzFilter();
	    if(StatelessDuplicate.isFissable(maxFilter)) 
	    {
		int oldfizzfactor = filterTimesFizzed.get(maxFilter).intValue();

		//System.err.println("highest filter is " + maxFilter.toString());
		Long maxWork = filterWorkEstimates.get(maxFilter);
		long mwork = maxWork.intValue();
		long totalwork = mwork * oldfizzfactor;
		
		long newwork = totalwork / (oldfizzfactor + 1);  //simulate the fizzing
		filterTimesFizzed.put(maxFilter, new Long(oldfizzfactor + 1));
		filterWorkEstimates.put(maxFilter, new Long(newwork));
		//System.err.println("Highest work filter is " + maxFilter.getName() + " from " + mwork + " to " + newwork);
	    }
	    //else
	    //System.err.println("Filter " + maxFilter.getName() + " is unfissable");
	}//void parallelize()
	
		
	/**   getHighestWorkFilter
	 *  Returns the filter with the highest overall work
	 *  (computation + communication cost)
	 */

	public static SIRFilter getHighestWorkFilter()
	{
	    SIRFilter maxFilter = null;
	    long maxwork = Long.MIN_VALUE;
	    Iterator<SIRFilter> sirFilters = filterWorkEstimates.keySet().iterator();

	    while(sirFilters.hasNext())
	    {
		SIRFilter currentFilter = sirFilters.next();
		long currentcompcost = filterWorkEstimates.get(currentFilter).intValue();
		int currentcomcost = filterCommunicationCosts.get(currentFilter).intValue();
		long currentwork = currentcompcost + currentcomcost;

		if(currentwork >= maxwork)
		{
		    maxFilter = currentFilter;
		    maxwork = currentwork;
		}

	    }
	    
	    return maxFilter;


	}//getHighestWorkFilter

	/**   getOptimalFizzFilter:  Returns the filter that would most
	 *  benefit from being fizzed, using filterWorkEstimates()
	 *  and filterTimesFizzed(). 
	 */

	public static SIRFilter getOptimalFizzFilter()
	{
	    long worksavings = Long.MIN_VALUE;
	    SIRFilter optFilter = null;
	    Iterator<SIRFilter> sirFilters = filterWorkEstimates.keySet().iterator();
	    while(sirFilters.hasNext())
	    {
		SIRFilter currentFilter = sirFilters.next();
		Long currentWork = filterWorkEstimates.get(currentFilter);
		long work = currentWork.longValue();
		long fizzfactor = filterTimesFizzed.get(currentFilter).longValue();
		long totalwork = work * fizzfactor;
		long nwork = totalwork / (fizzfactor + 1);
		long currentsavings = work - nwork;

		if(currentsavings >= worksavings)
		{
		    worksavings = currentsavings;
		    optFilter = currentFilter;
		}
	    }
	    
	    return optFilter;
	} //getOptimalFizzFilter()


	/**  getOptimalCommFizzFilter:  Similar to getOptimalFilter,
	 *  but incorporates fan-in/fan-out cost k-(k/N), where
	 *  k is constant.
	 */

	public static SIRFilter getOptimalCommFizzFilter()
	{
	    int fancost = 10;

	    long worksavings = Long.MIN_VALUE;
	    SIRFilter optFilter = null;
	    Iterator<SIRFilter> sirFilters = filterWorkEstimates.keySet().iterator();
	    while(sirFilters.hasNext())
	    {
		SIRFilter currentFilter = sirFilters.next();
		Long currentWork = filterWorkEstimates.get(currentFilter);
		long work = currentWork.intValue();
		long fizzfactor = filterTimesFizzed.get(currentFilter).longValue();
		long totalwork = work * fizzfactor;
		long cfanwork = fancost - (fancost/fizzfactor);
		
		long nwork = totalwork / (fizzfactor + 1);
		long nfanwork = fancost - (fancost/(fizzfactor + 1));
		long currentsavings = cfanwork + work - (nwork + nfanwork);	

		if(currentsavings >= worksavings)
		{	
		    worksavings = currentsavings;
		    optFilter = currentFilter;	
		    //System.err.println("blah with worksavings " + currentsavings);
		} 
	    }
	    //System.err.println("Saving " + worksavings);
	    return optFilter;
	    

	}//getOptimalCommFizzFilter
	
    }


    //Static class ParallelWork
     
    static class ParallelVisitor extends EmptyStreamVisitor {

	
	/** Mapping from streams to work estimates. **/
	static HashMap<SIRStream, SIRStream> newToOrigGraph;
	static HashMap<SIRFilter, SIRFilter> fizzedFiltersToOriginal;
	static HashMap<SIRFilter, Long> filterWorkEstimates;
	static HashMap<SIRFilter, Long> filterTimesFizzed;
	static HashMap<SIRFilter, Integer> filterCommunicationCosts;
	static MutableInt numFilters;
	static int currentfilters;
	static WorkEstimate work;
	static HashMap[] executionCounts;
	static GraphFlattener flatGraph;
	//static int printsperexec;

	public ParallelVisitor
	    (GraphFlattener graph, WorkEstimate wrk, HashMap<SIRStream, SIRStream> newormap, HashMap<SIRFilter, SIRFilter> filtermapping, 
	     HashMap<SIRFilter, Long> filters, HashMap<SIRFilter, Integer> comcosts, HashMap<SIRFilter, Long> timesfizzed, HashMap[] execcounts, MutableInt num)
	{
	    flatGraph = graph;
	    work = wrk;
	    newToOrigGraph = newormap;
	    fizzedFiltersToOriginal = filtermapping;
	    filterTimesFizzed = timesfizzed;
	    filterWorkEstimates = filters;
	    filterCommunicationCosts = comcosts;
	    numFilters = num;
	    currentfilters = 0;
	    executionCounts = execcounts;
	    //printsperexec = 0;
	}

    //Visitor Methods:

    /** visitFilter:
     *  Gets the work estimate of the filter, and stores it in 
     *  filterWorkEstimates.  If the filter is stateless, put in statelessFilters vector
     *  also populates the communication cost hashmap, based on pushing, popping, and peeking
     */
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {	
	int costperitem = 3;  //can be varied to see how it affects things!
	//print debug info?

	NumberGathering execNumbers = new NumberGathering();
	execNumbers.doit(flatGraph.getFlatNode(self), executionCounts);
	int printsperexec = execNumbers.printsPerSteady;
	long workestimate = work.getWork(self);
	//System.err.println("Initial work of " + self.getName() + " is " + workestimate);
	int numreps = work.getReps(self);  
	//System.err.println("Self is " + numreps);
	boolean nostate = !StatelessDuplicate.hasMutableState(self);

	//from Bill:  work.getWork already incorporates numreps!
	if(printsperexec > 0)
	    filterWorkEstimates.put(self, new Long(workestimate /printsperexec));
	else
	    filterWorkEstimates.put(self, new Long(workestimate));
	//System.err.println("Putting fizzfactor of 1 from filter " + self.getName());
	//filterTimesFizzed.put(self, new Integer(1));

	//when reverting back to no-sync, comment me out!
	fizzedFiltersToOriginal.put(self, (SIRFilter)newToOrigGraph.get(self));

	//add initial comm cost for all filters, based on their push/pop/peek rates
	int numpop = self.getPopInt();
	int numpush = self.getPushInt();
	int numpeek = self.getPeekInt();
	
	int initcost = costperitem * (numpop + numpush + numpeek) * numreps;	
	if (printsperexec > 0)
	    initcost = initcost / printsperexec;       
	filterCommunicationCosts.put(self, new Integer(initcost));
	
	//end addition for constant communication cost

	//System.err.println("Is filter " + self.getName() + " stateless?  " + nostate);
	numFilters.mutint = numFilters.mutint + 1;
	
	return;
	}


    /** postVisitPipeline
     *  Requires that all the children have already been visited, and their
     *  estimated work calculations already be in streamsToWorkEstimates or filterWorkEstimates
     *  (Hence, this being postVisit, and not preVisit).  Since it is just a pipeline,
     *  function just sums up all the work of its children.
     *  Also puts the work of the pipeline in streamsToWorkEstimates.
     */

    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter)
    {
	
	return;

    }
    

    /** postVisitSplitJoin
     *  Requires that all the children have already been visited, and their
     *  estimated work calculations already be in either streamsToWorkEstimates
     *  or filterWorkEstimates.
     *  Since it is a splitjoin, it will take the maximum of the work
     *  of its children (bottleneck path)
     */

    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter)
    {
	
	return;
    }

       
    }//class ParallelVisitor


    /**  ParallelSyncVisitor
     *  Similar to ParallelVisitor, except it populates FilterCommunicationCosts with the synchronization
     *  costs dynamically.  
     */

    static class ParallelSyncVisitor extends EmptyStreamVisitor {

	static HashMap<SIRFilter, Long> filterWorkEstimates;
	static HashMap<SIRFilter, Integer> filterCommunicationCosts;
	static HashMap[] executionCounts;
	static GraphFlattener flatGraph;
	//static int printsperexec;
	static WorkEstimate work;
	//static Vector splittersVisited;
	static HashMap<SIRFilter, Long> filterTimesFizzed;
	static HashMap<SIRFilter, SIRFilter> fizzedFiltersToOriginal;
	static HashMap<SIRStream, SIRStream> newToOrigGraph;
	boolean synccost;

	public ParallelSyncVisitor(GraphFlattener flat, WorkEstimate wrk,
				   HashMap[] execcounts, HashMap<SIRFilter, Long> workcosts, HashMap<SIRFilter, Integer> comcosts, 
				   HashMap<SIRFilter, SIRFilter> fizzedtooriginal, HashMap<SIRFilter, Long> timesfizzed, 
				   HashMap<SIRStream, SIRStream> tooriggraph, boolean scost)
	{
	    //splittersVisited = new Vector();
	    executionCounts = execcounts;
	    flatGraph = flat;
	    filterWorkEstimates = workcosts;
	    filterCommunicationCosts = comcosts;
	    filterTimesFizzed = timesfizzed;
	    //printsperexec = 0;
	    fizzedFiltersToOriginal = fizzedtooriginal;
	    newToOrigGraph = tooriggraph;
	    work = wrk;
	    synccost = scost;
	}



	//Visitor Methods:

	/**  visitFilter:
	 * Gets the workEstimate of the filter, and stores it in filterWorkEstimates.  Also
	 * populates initial communication costs
	 */
	 
	public void visitFilter(SIRFilter self, SIRFilterIter iter) 
	{
	    NumberGathering execNumbers = new NumberGathering();
	    execNumbers.doit(flatGraph.getFlatNode(self), executionCounts);
	    int printsperexec = execNumbers.printsPerSteady;
	    //System.err.println("prints per exec in filter is " + printsperexec);
	    //System.err.println("visiting filter " + self.getName());
	    int costperitem = 3; //can be varied
	    //final WorkEstimate work = WorkEstimate.getWorkEstimate(self);
	    long workestimate = work.getWork(self);
	    //System.err.println("Work of " + self.getName() + " is " + workestimate);
	    int numreps = work.getReps(self);
	    if(printsperexec > 0)
		filterWorkEstimates.put(self, new Long(workestimate /printsperexec));
	    else
		filterWorkEstimates.put(self, new Long(workestimate));

	    //add initial comm cost for all filters, based on their push/pop/peek rates
	    int numpop = self.getPopInt();
	    int numpush = self.getPushInt();
	    int numpeek = self.getPeekInt();
	    
	    //sync one
	    int initcost = costperitem * numreps * (numpop + numpush);
	    if(printsperexec > 0)
		initcost = initcost / printsperexec;
	    //System.err.println("syncfactor bool is " + synccost);
	    
	    if(synccost)
	    {//addition on 1/27/04, hopefully final fix!
		SIRFilter origFilter = null;
		if(fizzedFiltersToOriginal.containsKey(self))
		    origFilter = fizzedFiltersToOriginal.get(self);
		else
		    origFilter = (SIRFilter)newToOrigGraph.get(self);
		int fizzfactor = filterTimesFizzed.get(origFilter).intValue();
		//System.err.println("Synchro fizzfactor is " + fizzfactor);
		int synchrocost = initcost * (fizzfactor - 1) / fizzfactor;
		initcost += synchrocost;

	    }
	    
	    //int initcost = costperitem * (numpop + numpush);
	    //System.err.println("pop is " + numpop + " for " + self.getName());
	    //System.err.println("numreps is " + numreps + " with pushpop of " + (numpop + numpush));
	    //System.err.println("initcost is " + initcost + " with printsperexec of " + printsperexec);
	    filterCommunicationCosts.put(self, new Integer(initcost));
	}//void visitFilter
	


	/**   postVisitSplitJoin:
	 * Ah, annoying procedure #1.  Use getSplitFactor to return what N value should be
	 * used in synchronization calculation, based on potential parent splitjoins.  
	 * Then use flatnode to find the (filtery) children and add the synchro. cost to
	 * its current synchro cost.
	 */

	public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter)
	{
	    
	    //NumberGathering execNumbers = new NumberGathering();
	    //execNumbers.getPrintsPerSteady(flatGraph.getFlatNode(self), executionCounts);
	    //int printsperexec = execNumbers.printsPerSteady;

	    int fconstant = 2;
	    MutableInt syncfactor = new MutableInt();
	    SIRSplitter splitter = self.getSplitter();
	    FlatNode flatSplitter = flatGraph.getFlatNode(splitter);
	    //syncfactor.mutint = self.getParallelStreams().size();
	    //System.err.println("Split type is " + splitter.getType());
	    
	    SIRSplitType splitterType = splitter.getType();
	    if((splitterType.equals(SIRSplitType.ROUND_ROBIN)) || (splitterType.equals(SIRSplitType.DUPLICATE)) ||
	       splitterType.equals(SIRSplitType.WEIGHTED_RR))
	    {
		//System.err.println("Split type is " + splitter.getType());
		if(((splitterType == SIRSplitType.ROUND_ROBIN) || 
		   (splitterType == SIRSplitType.WEIGHTED_RR)) && 
		   (synccost))//bill fix:  only add size when it's a RR!
		{
		    syncfactor.mutint = self.getParallelStreams().size();
		    //StreamItDot.printGraph(self, "test.dot");
		    int blah = self.size();
		    //System.err.println("blah is " + blah);
		}
		else
		{
		    syncfactor.mutint = 1; //if duplicate, don't include the size in the cost
		}
		//System.err.println("Bee!");
		getSplitFactor(syncfactor, flatSplitter);   //recursion happens in this procedure   
		for(int i = 0; i < flatSplitter.getEdges().length; i++)
		{
		    if(flatSplitter.getEdges()[i].contents instanceof SIRFilter)
		    {
			SIRFilter currentFilter = (SIRFilter)flatSplitter.getEdges()[i].contents;			
			NumberGathering execNumbers = new NumberGathering();
			int numreps = work.getReps(currentFilter);
			execNumbers.doit(flatGraph.getFlatNode(currentFilter), executionCounts);
			int printsperexec = execNumbers.printsPerSteady;
			//System.err.println("prints per exec here is " + printsperexec
				//	   + " with " + numreps + " reps");
			int oldCommCost = filterCommunicationCosts.get(currentFilter).intValue();
			int fancost = 0;
			if(printsperexec > 0)
			    fancost = (fconstant * currentFilter.getPopInt() * numreps) / printsperexec;
			else
			    fancost = fconstant * currentFilter.getPopInt() * numreps;
			//System.err.println("fancost is " + fancost + " with mutint " + syncfactor.mutint);
			//System.err.println("printsperexec is " + printsperexec + " vs. " + currentFilter.getPopInt() + " pop rate");
			//System.err.println("Old comm cost is " + oldCommCost);
			int scost = fancost - (fancost/(syncfactor.mutint));
			//System.err.println("Sync cost is " + scost);
			int newCommCost = oldCommCost;
			if(syncfactor.mutint != 0)
			    newCommCost = oldCommCost + fancost - (fancost/(syncfactor.mutint));
			
			filterCommunicationCosts.put(currentFilter, new Integer(newCommCost));
		    }
		    //if not a filter, do nothing!
		}
	    }
	    
	}


	/**  getSplitFactor(MutableInt syncfactor, FlatNode flatsplitter)
	 * recurses up to find if it has any top level splitters.  If so, keep adding to syncfactor
	 */

	public void getSplitFactor(MutableInt syncfactor, FlatNode flatsplitter)
	{
	    //System.err.println("entering procedure");
	    FlatNode incomingNode = flatsplitter.incoming[0];
	    if(incomingNode.contents instanceof SIRSplitter)
	    {
		SIRSplitter topSplitter = (SIRSplitter)incomingNode.contents;
		//System.err.println("Splitter type is " + topSplitter.getType());
		if ((topSplitter.getType().equals(SIRSplitType.ROUND_ROBIN)) || (topSplitter.getType().equals(SIRSplitType.WEIGHTED_RR)))
		{
		    //System.err.println("gets here!");
		    syncfactor.mutint += topSplitter.getWays();
		    getSplitFactor(syncfactor, incomingNode);
		}
		if (topSplitter.getType().equals(SIRSplitType.DUPLICATE))
		{
		    getSplitFactor(syncfactor, incomingNode);
		}
		//if(syncfactor.mutint > 1)
		    //System.err.println("Sync factor is now " + syncfactor.mutint);
	    }	 
	    else
	    {
		SIRSplitter splitter = (SIRSplitter)flatsplitter.contents;
		if ((splitter.getType().equals(SIRSplitType.ROUND_ROBIN)) || (splitter.getType().equals(SIRSplitType.WEIGHTED_RR)))
		{
		    //System.err.println("gets here b!");
		    syncfactor.mutint += splitter.getWays();		
		}

	    }

	}//getSplitFactor

    }//class ParallelSyncVisitor


    /**    BasicVisitor:
     *  Class that takes the original input stream, and populates filterTimesFizzed with (1) values
     *  as an initial state
     */

    static class BasicVisitor extends EmptyStreamVisitor
    {
	static HashMap<SIRFilter, Long> filterTimesFizzed;

	public BasicVisitor(HashMap<SIRFilter, Long> fizzmapping)
	{
	    filterTimesFizzed = fizzmapping;
	}

	public void visitFilter(SIRFilter self, SIRFilterIter iter)
	{
	    filterTimesFizzed.put(self, new Long(1));
	}

    }//class BasicVisitor


    /**   NoSyncVisitor:
     *  Used in no communication model.  All it does is increment numfilters, 
     *  and populate filterWorkEstimates and filterCommunicationCosts
     */

    static class NoSyncVisitor extends EmptyStreamVisitor {
	
	static MutableInt numFilters;
	static HashMap<SIRFilter, Long> filterWorkEstimates;
	static HashMap<SIRFilter, Integer> filterCommunicationCosts;
	static HashMap<SIRFilter, Long> filterTimesFizzed;
	static WorkEstimate work;
	static GraphFlattener flatGraph;
	static HashMap[] executionCounts;
	

	public NoSyncVisitor
	    (WorkEstimate wrk, HashMap<SIRFilter, Long> filterwork, HashMap<SIRFilter, Integer> filtercomm, 
	     HashMap<SIRFilter, Long> fizzmapping, HashMap[] execcounts, 
	     GraphFlattener graph, MutableInt nfilts)
	{
	    numFilters = nfilts;
	    filterWorkEstimates = filterwork;
	    filterCommunicationCosts = filtercomm;
	    work = wrk;
	    flatGraph = graph;
	    executionCounts = execcounts;
	    filterTimesFizzed = fizzmapping;
	}

	public void visitFilter(SIRFilter self, SIRFilterIter iter) 
	{
	    filterTimesFizzed.put(self, new Long(1));

	    NumberGathering execNumbers = new NumberGathering();
	    execNumbers.doit(flatGraph.getFlatNode(self), executionCounts);
	    int printsperexec = execNumbers.printsPerSteady;
	    //System.err.println("prints per exec in filter is " + printsperexec);
	    //System.err.println("visiting filter " + self.getName());
	    int costperitem = 3; //can be varied
	    long workestimate = work.getWork(self);
	    int numreps = work.getReps(self);
	    if(printsperexec > 0)
		filterWorkEstimates.put(self, new Long(workestimate /printsperexec));
	    else
		filterWorkEstimates.put(self, new Long(workestimate));

	    //add initial comm cost for all filters, based on their push/pop/peek rates
	    int numpop = self.getPopInt();
	    int numpush = self.getPushInt();
	    int numpeek = self.getPeekInt();
	    
	    //sync one
	    int initcost = costperitem * numreps * (numpop + numpush);
	    if(printsperexec > 0)
		initcost = initcost / printsperexec;

	    //int initcost = costperitem * (numpop + numpush);
	    //System.err.println("pop is " + numpop + " for " + self.getName());
	    //System.err.println("initcost is " + initcost + " with printsperexec of " + printsperexec);
	    filterCommunicationCosts.put(self, new Integer(initcost));

	    numFilters.mutint = numFilters.mutint + 1;
	}


	    
    }//class NoSyncVisitor


}//class ParallelizationGathering





   

