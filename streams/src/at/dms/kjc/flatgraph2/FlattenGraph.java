package at.dms.kjc.flatgraph2;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.sir.linear.*;

/**
 * Flatten with new synch removal
 */
public class FlattenGraph {
    //private static int filterCount=0;
    private static ArrayList topLevelNodes=new ArrayList();
    private static HashMap nodes=new HashMap();
    private static HashMap simpleNull=new HashMap();
    private static HashMap complexNull=new HashMap();
    private static LinearAnalyzer lfa;
    private static HashMap[] execCounts;
    
    public static int getFilterCount() {
	//if(nodes!=null)
	return nodes.size();
	//return filterCount;
    }
    
    //Don't call unless not using this anymore
    public static void clear() {
	topLevelNodes.clear();
	topLevelNodes=null;
	nodes.clear();
	nodes=null;
	simpleNull.clear();
	simpleNull=null;
	complexNull.clear();
	complexNull=null;
	lfa=null;
	execCounts=null;
    }

    public static UnflatFilter[] getTopLevelNodes() {
	final int len=topLevelNodes.size();
	UnflatFilter[] out=new UnflatFilter[len];
	Object[] array=topLevelNodes.toArray();
	for(int i=0;i<len;i++)
	    out[i]=(UnflatFilter)array[i];
	return out;
    }
    
    public static void flattenGraph(SIRStream graph,LinearAnalyzer lfa,HashMap[] execCounts) {
	System.out.println("Flattening Graph..");
	FlattenGraph.lfa=lfa;
	FlattenGraph.execCounts=execCounts;
	if(graph instanceof SIRFilter) {
	    UnflatFilter filter=new UnflatFilter(graph);
	    topLevelNodes.add(filter);
	    nodes.put(filter,null);
	} else if(((SIRContainer)graph).size()>0) {
	    UnflatEdge[] top=flatten(graph);
	    UnflatEdge node=top[0];
	    UnflatEdge bottom=top[1];
	    if(node!=null)
		node.dest.in=null;
	    if(bottom!=null)
		bottom.src.out=null;
	    if(!(node==null||topLevelNodes.contains(node.dest)))
		topLevelNodes.add(node.dest);
	}
	System.out.println("INPUT: "+lfa+" "+execCounts);
	//if(lfa!=null&&execCounts!=null)
	extractLinear();
	dumpGraph("newbefore.dot");
	System.out.println("Done Flattening, Starting Sync Removal..");
	syncRemove();
	System.out.println("Done Sync Removal..");
	dumpGraph("newafter.dot");
    }
    
    private static UnflatEdge[] flatten(SIRStream graph) {
	if(graph instanceof SIRFilter) {
	    UnflatEdge in=new UnflatEdge();
	    UnflatEdge out=new UnflatEdge();
	    UnflatFilter inFilter;
	    if(graph instanceof SIRIdentity) {
		inFilter=new UnflatFilter(null,in,out);
		simpleNull.put(inFilter,null);
	    } else {
		SIRFilter filter=(SIRFilter)graph;
		inFilter=new UnflatFilter(graph,in,out);
		nodes.put(inFilter,null);
	    }
	    return new UnflatEdge[] {in,out};
	} else if(graph instanceof SIRFeedbackLoop) {
	    UnflatEdge in=new UnflatEdge();
	    UnflatEdge out=new UnflatEdge();
	    UnflatEdge[] body=flatten(((SIRFeedbackLoop)graph).getBody());
	    UnflatEdge[] loop=flatten(((SIRFeedbackLoop)graph).getLoop());
	    int[] joinWeights=((SIRFeedbackLoop)graph).getJoiner().getWeights();
	    UnflatEdge[] joinIn=new UnflatEdge[]{in,loop[1]};
	    UnflatEdge[][] joinOut=new UnflatEdge[][]{new UnflatEdge[]{body[0]}};
	    UnflatFilter joiner=new UnflatFilter(null,joinWeights,new int[]{1},joinIn,joinOut);
	    simpleNull.put(joiner,null);
	    int[] splitWeights=((SIRFeedbackLoop)graph).getSplitter().getWeights();
	    UnflatEdge[] splitIn=new UnflatEdge[]{body[1]};
	    UnflatEdge[][] splitOut;
	    if(((SIRFeedbackLoop)graph).getSplitter().getType().isRoundRobin())
		splitOut=new UnflatEdge[][]{new UnflatEdge[]{out},new UnflatEdge[]{loop[0]}};
	    else
		splitOut=new UnflatEdge[][]{new UnflatEdge[]{out,loop[0]}};
	    UnflatFilter splitter=new UnflatFilter(null,new int[]{1},splitWeights,splitIn,splitOut);
	    simpleNull.put(splitter,null);
	    return new UnflatEdge[]{in,out};
	} else if(((SIRContainer)graph).size()<1) {
	    UnflatEdge in=new UnflatEdge();
	    UnflatEdge out=new UnflatEdge();
	    UnflatFilter filter=new UnflatFilter(null,in,out);
	    simpleNull.put(filter,null);
	    return new UnflatEdge[] {in,out};
	} else if(graph instanceof SIRPipeline) {
	    UnflatEdge[] firstEdges=flatten(((SIRPipeline)graph).get(0));
	    UnflatEdge in=firstEdges[0];
	    UnflatEdge out=firstEdges[1];
	    for(int i=1;i<((SIRPipeline)graph).size();i++) {
		UnflatEdge[] nextEdges=flatten(((SIRPipeline)graph).get(i));
		out.connect(nextEdges[0]);
		out=nextEdges[1];
	    }
	    return new UnflatEdge[] {in,out};
	} else if(graph instanceof SIRSplitJoin) {
	    final int size=((SIRSplitJoin)graph).size();
	    UnflatEdge in=null;
	    int[] weights=null;
	    UnflatEdge[] splitterOut=new UnflatEdge[size];
	    UnflatFilter splitter=null;
	    for(int i=0;i<size;i++)
		splitterOut[i]=new UnflatEdge();
	    if(((SIRSplitJoin)graph).getSplitter().getType().isRoundRobin()) {
		weights=((SIRSplitJoin)graph).getSplitter().getWeights();
		int[] trimWeights;
		UnflatEdge[][] trimSplitterOut;
		int newSize=0;
		for(int i=0;i<size;i++)
		    if(weights[i]!=0)
			newSize++;
		if(newSize>0) {
		    trimWeights=new int[newSize];
		    trimSplitterOut=new UnflatEdge[newSize][1];
		    if(newSize<size) {
			for(int i=0,j=0;i<size;i++)
			    if(weights[i]!=0) {
				trimWeights[j]=weights[i];
				trimSplitterOut[j][0]=splitterOut[i];
				j++;
			    }
		    } else {
			trimWeights=weights;
			for(int i=0;i<size;i++)
			    trimSplitterOut[i][0]=splitterOut[i];
		    }
		    in=new UnflatEdge();
		    splitter=new UnflatFilter(null,new int[]{1},trimWeights,new UnflatEdge[]{in},trimSplitterOut);
		    simpleNull.put(splitter,null);
		}
	    } else if(((SIRSplitJoin)graph).getSplitter().getType().isDuplicate()) {
		in=new UnflatEdge();
		splitter=new UnflatFilter(null,new int[]{1},new int[]{1},new UnflatEdge[]{in},new UnflatEdge[][]{splitterOut});
		simpleNull.put(splitter,null);
		weights=new int[size];
		for(int i=0;i<size;i++)
		    weights[i]=1;
	    } else if(!((SIRSplitJoin)graph).getSplitter().getType().isNull())
		Utils.fail("Unknown Split Type: "+((SIRSplitJoin)graph).getSplitter().getType());
	    UnflatEdge[] joinerIn=new UnflatEdge[size];
	    UnflatFilter joiner=null;
	    UnflatEdge out=null;
	    int[] weightsOut=null;
	    for(int i=0;i<size;i++)
		joinerIn[i]=new UnflatEdge();
	    if(((SIRSplitJoin)graph).getJoiner().getType().isRoundRobin()) {
		weightsOut=((SIRSplitJoin)graph).getJoiner().getWeights();
		int[] trimWeights;
		UnflatEdge[] trimJoinerIn;
		int newSize=0;
		for(int i=0;i<size;i++)
		    if(weightsOut[i]!=0)
			newSize++;
		if(newSize>0) {
		    trimWeights=new int[newSize];
		    trimJoinerIn=new UnflatEdge[newSize];
		    if(newSize<size) {
			for(int i=0,j=0;i<size;i++)
			    if(weightsOut[i]!=0) {
				trimWeights[j]=weightsOut[i];
				trimJoinerIn[j]=joinerIn[i];
				j++;
			    }
		    } else {
			trimWeights=weightsOut;
			trimJoinerIn=joinerIn;
		    }
		    out=new UnflatEdge();
		    joiner=new UnflatFilter(null,trimWeights,new int[]{1},trimJoinerIn,new UnflatEdge[][]{new UnflatEdge[]{out}});
		    simpleNull.put(joiner,null);
		}
	    } else if(!((SIRSplitJoin)graph).getJoiner().getType().isNull())
		Utils.fail("Unknown Join Type: "+((SIRSplitJoin)graph).getJoiner().getType());
	    if(!((SIRSplitJoin)graph).getSplitter().getType().isNull()) {
		if(!((SIRSplitJoin)graph).getJoiner().getType().isNull()) {
		    for(int i=0;i<size;i++) {
			UnflatEdge[] child=flatten(((SIRSplitJoin)graph).get(i));
			if(weights[i]!=0) {
			    splitterOut[i].connect(child[0]);
			} else {
			    topLevelNodes.add(child[0].dest);
			    child[0].dest.in=null;
			}
			if(weightsOut[i]!=0) {
			    child[1].connect(joinerIn[i]);
			} else
			    child[1].src.out=null;
		    }
		} else {
		    for(int i=0;i<size;i++) {
			UnflatEdge[] child=flatten(((SIRSplitJoin)graph).get(i));
			if(weights[i]!=0) {
			    splitterOut[i].connect(child[0]);
			} else {
			    topLevelNodes.add(child[0].dest);
			    child[0].dest.in=null;
			}
		    }
		}
	    } else if(!((SIRSplitJoin)graph).getJoiner().getType().isNull()) {
		for(int i=0;i<size;i++) {
		    UnflatEdge[] child=flatten(((SIRSplitJoin)graph).get(i));
		    topLevelNodes.add(child[0].dest);
		    if(weightsOut[i]!=0) {
			child[1].connect(joinerIn[i]);
		    } else
			child[1].src.out=null;
		}
	    } else
		for(int i=0;i<size;i++)
		    topLevelNodes.add(flatten(((SIRSplitJoin)graph).get(i))[0].dest);
	    return new UnflatEdge[]{in,out};
	} else
	    Utils.fail("Unhandled Stream Type: "+graph);
	return null;
    }

    private static void extractLinear() {
	Object[] filters=nodes.keySet().toArray();
	for(int i=0;i<filters.length;i++) {
	    UnflatFilter filter=(UnflatFilter)filters[i];
	    System.out.println("VISITING: "+filter);
	    LinearFilterRepresentation linrep=null;
	    if(lfa!=null)
		linrep=lfa.getLinearRepresentation(filter.filter);
	    int initMult=0;
	    int steadyMult=0;
	    int[] ans=(int[])execCounts[0].get(filter.filter);
	    if(ans!=null)
		initMult=ans[0];
	    ans=(int[])execCounts[1].get(filter.filter);
	    if(ans!=null)
		steadyMult=ans[0];
	    System.err.println("MULT: "+initMult+" "+steadyMult);
	    if(linrep!=null) {
		final int cols=linrep.getA().getCols();
		if(cols==1) { 
		    filter.initMult=initMult;
		    filter.steadyMult=steadyMult;
		    filter.array=getArray(linrep,0);
		    filter.constant=getConst(linrep,0);
		    filter.popCount=linrep.getPopCount();
		} else {
		    nodes.remove(filter);
		    System.out.println("Splitting: "+filter);
		    UnflatEdge[] in=filter.in;
		    UnflatEdge[][] out=filter.out;
		    UnflatEdge[] splitterOut=new UnflatEdge[cols];
		    for(int j=0;j<cols;j++)
			splitterOut[j]=new UnflatEdge();
		    UnflatFilter splitter=new UnflatFilter(null,new int[]{1},new int[]{1},in,new UnflatEdge[][]{splitterOut});
		    simpleNull.put(splitter,null);
		    int[] joinerW=new int[cols];
		    for(int j=0;j<cols;j++)
			joinerW[j]=1;
		    UnflatEdge[] joinerIn=new UnflatEdge[cols];
		    for(int j=0;j<cols;j++)
			joinerIn[j]=new UnflatEdge();
		    UnflatFilter joiner=new UnflatFilter(null,joinerW,new int[]{1},joinerIn,out);
		    simpleNull.put(joiner,null);
		    for(int j=0;j<cols;j++) {
			UnflatFilter newFilt=new UnflatFilter(filter.filter,splitterOut[j],joinerIn[j]);
			newFilt.initMult=initMult;
			newFilt.steadyMult=steadyMult;
			newFilt.array=getArray(linrep,j);
			newFilt.constant=getConst(linrep,j);
			newFilt.popCount=linrep.getPopCount();
			nodes.put(newFilt,null);
		    }
		}
	    } else {
		filter.initMult=initMult;
		filter.steadyMult=steadyMult;
	    }
	}
    }
    
    private static double[] getArray(LinearFilterRepresentation linRep,int col) {
	FilterMatrix a=linRep.getA();
	final int rows=a.getRows();
	double[] out=new double[rows];
	for(int i=0;i<rows;i++)
	    out[i]=a.getElement(rows-i-1,col).getReal();
	return out;
    }
    
    private static double getConst(LinearFilterRepresentation linRep,int col) {
	return linRep.getb().getElement(col).getReal();
    }
    
    private static boolean change=false;
    private static boolean first=true;

    private static void syncRemove() {
	removeSimple();
	while(change) {
	    change=false;
	    removeSimple();
	}
	dumpGraph("newmiddle.dot");
	removeComplex();
	dumpGraph("newmiddle2.dot");
	//change=true;
	//while(change) {
	//change=false;
	//first=false;
	removeSimple();
	//}
	//dumpGraph("newmiddle3.dot");
	//coalesce();
    }
    
    private static int[] lcm(int a,int b) {
	int mulA=1,mulB=1;
	int accumA=a,accumB=b;
	while(accumA!=accumB)
	    if(accumA<accumB) {
		accumA+=a;
		mulA++;
	    } else {
		accumB+=b;
		mulB++;
	    }
	return new int[]{accumA,mulA,mulB};
    }
    
    private static int sum(int[] array) {
	int out=0;
	for(int i=0;i<array.length;i++)
	    out+=array[i];
	return out;
    }
    
    private static UnflatEdge[] splice(UnflatEdge[] src,UnflatEdge[] splice,UnflatEdge target) {
	int len=0;
	boolean change=false;
	for(int i=0;i<src.length;i++)
	    if(src[i]==target) {
		len+=splice.length;
		change=true;
	    } else
		len++;
	if(change) {
	    UnflatEdge[] out=new UnflatEdge[len];
	    for(int i=0,j=0;i<src.length;i++)
		if(src[i]==target)
		    for(int k=0;k<splice.length;k++,j++)
			out[j]=splice[k];
		else
		    out[j++]=src[i];
	    return out;
	} else
	    return src;
    }
    
    /*private static boolean contains(UnflatEdge[] array,UnflatEdge edge) {
	for(int i=0;i<array.length;i++)
	    if(array[i]==edge)
		return true;
	return false;
	}*/
    
    private static void removeSimple() {
	Object[] nulls=simpleNull.keySet().toArray();
	for(int i=0;i<nulls.length;i++) {
	    UnflatFilter filter=(UnflatFilter)nulls[i];
	    int[] inW=filter.inWeights;
	    int[] outW=filter.outWeights;
	    UnflatEdge[] in=filter.in;
	    UnflatEdge[][] out=filter.out;
	    if(inW.length==1&&outW.length==1&&out[0].length==1) {
		UnflatEdge[][] prevOut=in[0].src.out;
		for(int j=0;j<prevOut.length;j++)
		    prevOut[j]=splice(prevOut[j],out[0],in[0]);
		for(int j=0;j<out[0].length;j++)
		    out[0][j].src=in[0].src;
		simpleNull.remove(filter);
		change=true;
	    } else if(inW.length==1) {
		UnflatFilter prevFilter=in[0].src;
		int[] prevW=prevFilter.outWeights;
		UnflatEdge[][] prevOut=prevFilter.out;
		for(int j=0;j<out.length;j++) {
		    UnflatEdge[] inner=out[j];
		    for(int k=0;k<inner.length;k++) {
			inner[k].src=prevFilter;
		    }
		}
		if(prevOut.length==1) {
		    prevFilter.outWeights=outW;
		    UnflatEdge[] oneEdge=prevOut[0];
		    if(oneEdge.length==1)
			prevFilter.out=out;
		    else {
			UnflatEdge[][] newOut=new UnflatEdge[out.length][0];
			prevFilter.out=newOut;
			for(int j=0;j<newOut.length;j++)
			    newOut[j]=splice(oneEdge,out[j],in[0]);
		    }
		    simpleNull.remove(filter);
		    change=true;
		} else {
		    boolean[] mergePts=new boolean[prevOut.length];
		    int sumMerge=0;
		    for(int j=0;j<prevOut.length;j++) {
			mergePts[j]=false;
			UnflatEdge[] curEdge=prevOut[j];
			for(int k=0;k<curEdge.length;k++)
			    if(curEdge[k]==in[0]) {
				mergePts[j]=true;
				sumMerge+=prevW[j];
			    }
		    }
		    IntList newWeights=null;
		    EdgeArrayList newEdges=null;
		    IntList curWeights=null;
		    EdgeArrayList curEdges=null;
		    int times=lcm(sumMerge,sum(outW))[1];
		    int curTimes=0;
		    int index=0;
		    int targetIndex=0;
		    int curW=outW[0];
		    UnflatEdge[] curEdge=out[0];
		    int targetW=prevW[0];
		    UnflatEdge[] targetEdge=prevOut[0];
		    if(mergePts[0]) {
			if(curW==targetW) {
			    newWeights=new IntList(targetW);
			    newEdges=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curWeights=newWeights;
			    curEdges=newEdges;
			    index++;
			    if(outW.length>1) {
				curEdge=out[1];
				curW=outW[1];
			    }
			    if(++targetIndex>=mergePts.length) {
				curTimes++;
				targetIndex=0;
			    }
			    while(curTimes<times&&!mergePts[targetIndex]) {
				curWeights.next=new IntList(prevW[targetIndex]);
				curWeights=curWeights.next;
				curEdges.next=new EdgeArrayList(prevOut[targetIndex]);
				curEdges=curEdges.next;
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
			    }
			    targetW=prevW[targetIndex];
			    targetEdge=prevOut[targetIndex];
			} else if(curW>targetW) {
			    newWeights=new IntList(targetW);
			    newEdges=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curWeights=newWeights;
			    curEdges=newEdges;
			    curW=curW-targetW;
			    if(++targetIndex>=mergePts.length) {
				curTimes++;
				targetIndex=0;
			    }
			    while(curTimes<times&&!mergePts[targetIndex]) {
				curWeights.next=new IntList(prevW[targetIndex]);
				curWeights=curWeights.next;
				curEdges.next=new EdgeArrayList(prevOut[targetIndex]);
				curEdges=curEdges.next;
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
			    }
			    targetW=prevW[targetIndex];
			    targetEdge=prevOut[targetIndex];
			} else {
			    newWeights=new IntList(curW);
			    newEdges=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curWeights=newWeights;
			    curEdges=newEdges;
			    targetW=targetW-curW;
			    index++;
			    if(outW.length>1) {
				curEdge=out[1];
				curW=outW[1];
			    }
			}
		    } else {
			newWeights=new IntList(prevW[targetIndex]);
			newEdges=new EdgeArrayList(prevOut[targetIndex]);
			curWeights=newWeights;
			curEdges=newEdges;
			if(++targetIndex>=mergePts.length) {
			    curTimes++;
			    targetIndex=0;
			}
			while(curTimes<times&&!mergePts[targetIndex]) {
			    curWeights.next=new IntList(prevW[targetIndex]);
			    curWeights=curWeights.next;
			    curEdges.next=new EdgeArrayList(prevOut[targetIndex]);
			    curEdges=curEdges.next;
			    if(++targetIndex>=mergePts.length) {
				curTimes++;
				targetIndex=0;
			    }
			}
			targetW=prevW[targetIndex];
			targetEdge=prevOut[targetIndex];
		    }
		    while(curTimes!=times) {
			if(curW==targetW) {
			    curWeights.next=new IntList(targetW);
			    curWeights=curWeights.next;
			    curEdges.next=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curEdges=curEdges.next;
			    if(++index>=outW.length)
				index=0;
			    curEdge=out[index];
			    curW=outW[index];
			    if(++targetIndex>=mergePts.length) {
				curTimes++;
				targetIndex=0;
			    }
			    while(curTimes<times&&!mergePts[targetIndex]) {
				curWeights.next=new IntList(prevW[targetIndex]);
				curWeights=curWeights.next;
				curEdges.next=new EdgeArrayList(prevOut[targetIndex]);
				curEdges=curEdges.next;
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
			    }
			    targetW=prevW[targetIndex];
			    targetEdge=prevOut[targetIndex];
			} else if(curW>targetW) {
			    curWeights.next=new IntList(targetW);
			    curWeights=curWeights.next;
			    curEdges.next=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curEdges=curEdges.next;
			    curW=curW-targetW;
			    if(++targetIndex>=mergePts.length) {
				curTimes++;
				targetIndex=0;
			    }
			    while(curTimes<times&&!mergePts[targetIndex]) {
				curWeights.next=new IntList(prevW[targetIndex]);
				curWeights=curWeights.next;
				curEdges.next=new EdgeArrayList(prevOut[targetIndex]);
				curEdges=curEdges.next;
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
			    }
			    targetW=prevW[targetIndex];
			    targetEdge=prevOut[targetIndex];
			} else {
			    curWeights.next=new IntList(curW);
			    curWeights=curWeights.next;
			    curEdges.next=new EdgeArrayList(splice(targetEdge,curEdge,in[0]));
			    curEdges=curEdges.next;
			    targetW=targetW-curW;
			    if(++index>=outW.length)
				index=0;
			    curEdge=out[index];
			    curW=outW[index];
			}
		    }
		    int length=1;
		    IntList tempWeights=new IntList(newWeights.val);
		    IntList curTempW=tempWeights;
		    UnflatEdge[] tempEdge=newEdges.val;
		    EdgeArrayList tempEdges=new EdgeArrayList(newEdges.val);
		    EdgeArrayList curTempE=tempEdges;
		    curWeights=newWeights.next;
		    curEdges=newEdges.next;
		    while(curWeights!=null) {
			if(UnflatEdge.equal(tempEdge,curEdges.val))
			    curTempW.val=curTempW.val+curWeights.val;
			else {
			    length++;
			    curTempW.next=new IntList(curWeights.val);
			    curTempW=curTempW.next;
			    curTempE.next=new EdgeArrayList(curEdges.val);
			    curTempE=curTempE.next;
			    tempEdge=curEdges.val;
			}
			curEdges=curEdges.next;
			curWeights=curWeights.next;
		    }
		    int[] finalWeights=new int[length];
		    UnflatEdge[][] finalEdges=new UnflatEdge[length][0];
		    int offset=0;
		    while(tempWeights!=null) {
			finalWeights[offset]=tempWeights.val;
			UnflatEdge[] finalEdge=tempEdges.val;
			finalEdges[offset]=finalEdge;
			offset++;
			tempWeights=tempWeights.next;
			tempEdges=tempEdges.next;
		    }
		    prevFilter.outWeights=finalWeights;
		    prevFilter.out=finalEdges;
		    simpleNull.remove(filter);
		    change=true;
		    }
	    } else if(outW.length==1) {
		if(out.length!=1)
		    Utils.fail("Mismatch: "+outW.length+" "+out.length);
		UnflatEdge[] nextEdges=out[0];
		//if(nextEdges.length!=1)
		//System.out.println("NextEdges:"+nextEdges.length);
		{
		    UnflatEdge[][] incomingEdges=new UnflatEdge[inW.length][nextEdges.length];
		    HashMap createdEdges=new HashMap();
		    for(int j=0;j<inW.length;j++) {
			UnflatFilter inFilter=in[j].src;
			UnflatEdge[] oldEdges=(UnflatEdge[])createdEdges.get(in[j]);
			if(oldEdges==null) {
			    oldEdges=new UnflatEdge[nextEdges.length];
			    createdEdges.put(in[j],oldEdges);
			}
			for(int k=0;k<nextEdges.length;k++) {
			    UnflatEdge oldEdge=oldEdges[k];
			    if(oldEdge==null) {
				oldEdge=new UnflatEdge(inFilter,nextEdges[k].dest);
				oldEdges[k]=oldEdge;
			    }
			    incomingEdges[j][k]=oldEdge;
			}
			UnflatEdge[][] edge=inFilter.out;
			for(int k=0;k<edge.length;k++)
			    edge[k]=splice(edge[k],incomingEdges[j],in[j]);
		    }
		    for(int j=0;j<nextEdges.length;j++) {
			UnflatEdge nextEdge=nextEdges[j];
			UnflatFilter nextFilter=nextEdge.dest;
			int[] nextW=nextFilter.inWeights;
			UnflatEdge[] nextIn=nextFilter.in;
			boolean[] mergePts=new boolean[nextIn.length];
			int sumMerge=0;
			for(int k=0;k<nextIn.length;k++) {
			    UnflatEdge curEdge=nextIn[k];
			    if(curEdge==nextEdge) {
				mergePts[k]=true;
				sumMerge+=nextW[k];
			    } else
				mergePts[k]=false;
			}
			IntList newWeights=new IntList(0);
			EdgeList newEdges=new EdgeList(null);
			IntList formedWeights=newWeights;
			EdgeList formedEdges=newEdges;
			int times=lcm(sumMerge,sum(inW))[1];
			int curTimes=0;
			int index=0;
			int targetIndex=0;
			int curW=inW[0];
			UnflatEdge curEdge=in[0];
			int targetW=nextW[0];
			UnflatEdge targetEdge=nextIn[0];
			while(curTimes!=times) {
			    if(mergePts[targetIndex]==false) {
				newWeights.next=new IntList(targetW);
				newEdges.next=new EdgeList(nextIn[targetIndex]);
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
				targetW=nextW[targetIndex];
			    } else if(curW==targetW) {
				newWeights.next=new IntList(curW);
				newEdges.next=new EdgeList(incomingEdges[index][j]);
				if(++index>=inW.length)
				    index=0;
				curW=inW[index];
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
				targetW=nextW[targetIndex];
			    } else if(curW>targetW) {
				newWeights.next=new IntList(targetW);
				newEdges.next=new EdgeList(incomingEdges[index][j]);
				curW-=targetW;
				if(++targetIndex>=mergePts.length) {
				    curTimes++;
				    targetIndex=0;
				}
				targetW=nextW[targetIndex];
			    } else {
				newWeights.next=new IntList(curW);
				newEdges.next=new EdgeList(incomingEdges[index][j]);
				targetW-=curW;
				if(++index>=inW.length)
				    index=0;
				curW=inW[index];
			    }
			    newWeights=newWeights.next;
			    newEdges=newEdges.next;
			}
			formedWeights=formedWeights.next;
			formedEdges=formedEdges.next;
			int length=1;
			UnflatEdge tempEdge=formedEdges.val;
			IntList tempWeights=new IntList(formedWeights.val);
			EdgeList tempEdges=new EdgeList(tempEdge);
			IntList curTempW=tempWeights;
			EdgeList curTempE=tempEdges;
			formedEdges=formedEdges.next;
			formedWeights=formedWeights.next;
			while(formedWeights!=null) {
			    if(tempEdge==formedEdges.val)
				curTempW.val=curTempW.val+formedWeights.val;
			    else {
				length++;
				curTempW.next=new IntList(formedWeights.val);
				curTempW=curTempW.next;
				curTempE.next=new EdgeList(formedEdges.val);
				curTempE=curTempE.next;
				tempEdge=formedEdges.val;
			    }
			    formedEdges=formedEdges.next;
			    formedWeights=formedWeights.next;
			}
			int[] finalWeights=new int[length];
			UnflatEdge[] finalEdges=new UnflatEdge[length];
			int offset=0;
			while(tempWeights!=null) {
			    finalWeights[offset]=tempWeights.val;
			    finalEdges[offset]=tempEdges.val;
			    offset++;
			    tempWeights=tempWeights.next;
			    tempEdges=tempEdges.next;
			}
			nextFilter.inWeights=finalWeights;
			nextFilter.in=finalEdges;
		    }
		    simpleNull.remove(filter);
		    change=true;
		}
	    } else {
		complexNull.put(filter,null);
		simpleNull.remove(filter);
	    }
	}
    }
    
    private static void breakEdgeTo(UnflatFilter from,UnflatFilter to,UnflatEdge edge) {
	EdgeList edges=new EdgeList(null);
	IntList weights=new IntList(0);
	EdgeList curE=edges;
	IntList curW=weights;
	UnflatEdge[][] e=from.out;
	int[] w=from.outWeights;
	for(int i=0;i<e.length;i++) {
	    UnflatEdge[] inner=e[i];
	    for(int j=0;j<inner.length;j++)
		if(inner[j]==edge) {
		    UnflatEdge newEdge=new UnflatEdge(from,null);
		    inner[j]=newEdge;
		    curE.next=new EdgeList(newEdge);
		    curE=curE.next;
		    curW.next=new IntList(w[i]);
		    curW=curW.next;
		}		    
	}
	edges=edges.next;
	weights=weights.next;
	int len=weights.size();
	UnflatEdge[] in=new UnflatEdge[len];
	int[] inW=new int[len];
	int i=0;
	while(weights!=null) {
	    in[i]=edges.val;
	    inW[i]=weights.val;
	    edges=edges.next;
	    weights=weights.next;
	    i++;
	}
	UnflatFilter newFilter=new UnflatFilter(null,inW,new int[]{1},in,new UnflatEdge[][]{new UnflatEdge[]{edge}});
	simpleNull.put(newFilter,null);
    }

    private static void breakEdgeFrom(UnflatFilter from,UnflatFilter to,UnflatEdge edge) {
	EdgeList edges=new EdgeList(null);
	IntList weights=new IntList(0);
	EdgeList curE=edges;
	IntList curW=weights;
	UnflatEdge[] e=to.in;
	int[] w=to.inWeights;
	for(int i=0;i<e.length;i++)
	    if(e[i]==edge) {
		UnflatEdge newEdge=new UnflatEdge(null,to);
		e[i]=newEdge;
		curE.next=new EdgeList(newEdge);
		curE=curE.next;
		curW.next=new IntList(w[i]);
		curW=curW.next;
	    }
	edges=edges.next;
	weights=weights.next;
	int len=weights.size();
	UnflatEdge[][] out=new UnflatEdge[len][0];
	int[] outW=new int[len];
	int i=0;
	while(weights!=null) {
	    out[i]=new UnflatEdge[]{edges.val};
	    outW[i]=weights.val;
	    edges=edges.next;
	    weights=weights.next;
	    i++;
	}
	UnflatFilter newFilter=new UnflatFilter(null,new int[]{1},outW,new UnflatEdge[]{edge},out);
	simpleNull.put(newFilter,null);
    }

    private static void removeComplex() {
	Object[] nulls=complexNull.keySet().toArray();
	for(int i=0;i<nulls.length;i++) {
	    UnflatFilter filter=(UnflatFilter)nulls[i];
	    int[] inW=filter.inWeights,outW=filter.outWeights;
	    UnflatEdge[] in=filter.in;
	    UnflatEdge[][] out=filter.out;
	    {
		HashMap visited=new HashMap();
		for(int j=0;j<in.length;j++)
		    if(!visited.containsKey(in[j])) {
			visited.put(in[j],null);
			breakEdgeFrom(in[j].src,filter,in[j]);
		    }
	    }
	    {
		HashMap visited=new HashMap();
		for(int j=0;j<out.length;j++) {
		    UnflatEdge[] inner=out[j];
		    for(int k=0;k<inner.length;k++)
			if(!visited.containsKey(inner[k])) {
			    visited.put(inner[k],null);
			    breakEdgeTo(filter,inner[k].dest,inner[k]);
			}
		}
	    }
	    int inSum=0,outSum=0;
	    for(int j=0;j<inW.length;j++)
		inSum+=inW[j];
	    for(int j=0;j<outW.length;j++)
		outSum+=outW[j];
	    int[] lcm=lcm(inSum,outSum);
	    int timesIn=lcm[1];
	    int timesOut=lcm[2];
	    System.out.println("Complex:"+timesIn+" "+timesOut);
	    UnflatEdge[][] inbetweenEdges=new UnflatEdge[inW.length][outW.length];
	    for(int j=0;j<inW.length;j++)
		for(int k=0;k<outW.length;k++)
		    inbetweenEdges[j][k]=new UnflatEdge();
	    IntList[] incomingWeights=new IntList[inW.length];
	    EdgeArrayList[] incomingOut=new EdgeArrayList[inW.length];
	    IntList[] curWeights=new IntList[inW.length];
	    EdgeArrayList[] curEdgesIn=new EdgeArrayList[inW.length];
	    for(int j=0;j<incomingWeights.length;j++)
		incomingWeights[j]=new IntList(0);
	    for(int j=0;j<incomingOut.length;j++)
		incomingOut[j]=new EdgeArrayList(null);
	    System.arraycopy(incomingWeights,0,curWeights,0,inW.length);
	    System.arraycopy(incomingOut,0,curEdgesIn,0,inW.length);
	    int index=0;
	    int excess=0;
	    for(int j=0;j<timesIn;j++) {
		for(int k=0;k<inW.length;k++) {
		    IntList curW=curWeights[k];
		    EdgeArrayList curE=curEdgesIn[k];
		    int target=inW[k];
		    while(target>0) {
			int newW=outW[index];
			curE.next=new EdgeArrayList(new UnflatEdge[]{inbetweenEdges[k][index]});
			curE=curE.next;
			if(excess>target) {
			    curW.next=new IntList(target);
			    excess=excess-target;
			    target=0;
			} else if(excess>0) {
			    curW.next=new IntList(excess);
			    target=target-excess;
			    excess=0;
			    if(++index>=outW.length)
				index=0;
			} else if(newW>target) {
			    curW.next=new IntList(target);
			    excess=newW-target;
			    target=0;
			} else if(target>newW) {
			    curW.next=new IntList(newW);
			    target=target-newW;
			    if(++index>=outW.length)
				index=0;
			} else {
			    curW.next=new IntList(newW);
			    target=0;
			    if(++index>=outW.length)
				index=0;
			}
			curW=curW.next;
		    }
		    curWeights[k]=curW;
		    curEdgesIn[k]=curE;
		}
	    }
	    IntList[] outgoingWeights=new IntList[outW.length];
	    EdgeList[] outgoingIn=new EdgeList[outW.length];
	    curWeights=new IntList[outW.length];
	    EdgeList[] curEdgesOut=new EdgeList[outW.length];
	    for(int j=0;j<outgoingWeights.length;j++)
		outgoingWeights[j]=new IntList(0);
	    for(int j=0;j<outgoingIn.length;j++)
		outgoingIn[j]=new EdgeList(null);
	    System.arraycopy(outgoingWeights,0,curWeights,0,outW.length);
	    System.arraycopy(outgoingIn,0,curEdgesOut,0,outW.length);
	    index=0;
	    excess=0;
	    for(int j=0;j<timesOut;j++) {
		for(int k=0;k<outW.length;k++) {
		    IntList curW=curWeights[k];
		    EdgeList curE=curEdgesOut[k];
		    int target=outW[k];
		    while(target>0) {
			int newW=inW[index];
			curE.next=new EdgeList(inbetweenEdges[index][k]);
			curE=curE.next;
			if(excess>target) {
			    curW.next=new IntList(target);
			    excess=excess-target;
			    target=0;
			} else if(excess>0) {
			    curW.next=new IntList(excess);
			    target=target-excess;
			    excess=0;
			    if(++index>=inW.length)
				index=0;
			} else if(newW>target) {
			    curW.next=new IntList(target);
			    excess=newW-target;
			    target=0;
			} else if(target>newW) {
			    curW.next=new IntList(newW);
			    target=target-newW;
			    if(++index>=inW.length)
				index=0;
			} else {
			    curW.next=new IntList(newW);
			    target=0;
			    if(++index>=inW.length)
				index=0;
			}
			curW=curW.next;
		    }
		    curWeights[k]=curW;
		    curEdgesOut[k]=curE;
		}
	    }
	    complexNull.remove(filter);
	    for(int j=0;j<inW.length;j++) {
		IntList weights=incomingWeights[j].next;
		EdgeArrayList edges=incomingOut[j].next;
		int size=weights.size();
		int[] w=new int[size];
		UnflatEdge[][] e=new UnflatEdge[size][0];
		int idx=0;
		while(weights!=null) {
		    w[idx]=weights.val;
		    e[idx]=edges.val;
		    idx++;
		    weights=weights.next;
		    edges=edges.next;
		}
		UnflatFilter newFilter=new UnflatFilter(null,new int[]{1},w,new UnflatEdge[]{in[j]},e);
		simpleNull.put(newFilter,null);
	    }
	    for(int j=0;j<outW.length;j++) {
		IntList weights=outgoingWeights[j].next;
		EdgeList edges=outgoingIn[j].next;
		int size=weights.size();
		int[] w=new int[size];
		UnflatEdge[] e=new UnflatEdge[size];
		int idx=0;
		while(weights!=null) {
		    w[idx]=weights.val;
		    e[idx]=edges.val;
		    idx++;
		    weights=weights.next;
		    edges=edges.next;
		}
		UnflatFilter newFilter=new UnflatFilter(null,w,new int[]{1},e,new UnflatEdge[][]{out[j]});
		simpleNull.put(newFilter,null);
	    }
	}
    }

    /*private static void coalesce() {
	Object[] filters=nodes.keySet().toArray();
	for(int i=0;i<filters.length;i++) {
	    UnflatFilter filter=(UnflatFilter)filters[i];
	    int[] inW=filter.inWeights;
	    UnflatEdge[] in=filter.in;
	    if(inW.length>1) {
		IntList tempInW=new IntList(inW[0]);
		EdgeList tempIn=new EdgeList(in[0]);
		IntList curW=tempInW;
		EdgeList curE=tempIn;
		UnflatEdge currentEdge=in[0];
		int length=1;
		for(int j=1;j<inW.length;j++) {
		    UnflatEdge testEdge=in[j];
		    if(testEdge==currentEdge)
			curW.val=curW.val+inW[j];
		    else {
			length++;
			curW.next=new IntList(inW[j]);
			curW=curW.next;
			curE.next=new EdgeList(in[j]);
			curE=curE.next;
			currentEdge=in[j];
		    }
		}
		if(length<inW.length) {
		    int[] newW=new int[length];
		    UnflatEdge[] newE=new UnflatEdge[length];
		    for(int j=0;j<length;j++) {
			newW[j]=curW.val;
			newE[j]=curE.val;
			curW=curW.next;
			curE=curE.next;
		    }
		    filter.inWeights=newW;
		    filter.in=newE;
		}
	    }
	    int[] outW=filter.outWeights;
	    UnflatEdge[][] out=filter.out;
	    if(outW.length>1) {
		IntList tempOutW=new IntList(outW[0]);
		EdgeArrayList tempOut=new EdgeArrayList(out[0]);
		IntList curW=tempOutW;
		EdgeArrayList curE=tempOut;
		UnflatEdge[] currentEdge=out[0];
		int length=1;
		for(int j=1;j<outW.length;j++) {
		    UnflatEdge[] testEdge=out[j];
		    if(UnflatEdge.equal(testEdge,currentEdge))
			curW.val=curW.val+outW[j];
		    else {
			length++;
			curW.next=new IntList(outW[j]);
			curW=curW.next;
			curE.next=new EdgeArrayList(out[j]);
			curE=curE.next;
			currentEdge=out[j];
		    }
		}
		if(length<outW.length) {
		    int[] newW=new int[length];
		    UnflatEdge[][] newE=new UnflatEdge[length][0];
		    for(int j=0;j<length;j++) {
			newW[j]=curW.val;
			newE[j]=curE.val;
			curW=curW.next;
			curE=curE.next;
		    }
		    filter.outWeights=newW;
		    filter.out=newE;
		}
	    }
	}
	}*/
    
    public static void dumpGraph(String filename) {
	StringBuffer buf=new StringBuffer();
	buf.append("digraph Flattend {\n");
	buf.append("size = \"8, 10.5\";");
	Object[] filters=nodes.keySet().toArray();
	for(int i=0;i<filters.length;i++) {
	    UnflatFilter filter=(UnflatFilter)filters[i];
	    buf.append(filter+"[ label = \"");
	    if(filter.inWeights.length>1)
		buf.append(arrayPrint(filter.inWeights)+"\\n");
	    buf.append(filter);
	    if(filter.outWeights.length>1)
		buf.append("\\n"+arrayPrint(filter.outWeights));
	    buf.append("\"];\n");
	    UnflatEdge[][] out=filter.out;
	    HashMap visited=new HashMap();
	    if(out!=null)
		for(int j=0;j<out.length;j++) {
		    UnflatEdge[] edges=out[j];
		    if(edges.length==1) {
			if(!visited.containsKey(edges[0])) {
			    visited.put(edges[0],null);
			    UnflatFilter target=edges[0].dest;
			    if(target!=null)
				buf.append(filter+" -> "+target+"[label=\""+printNext(edges[0])+"->"+printPrev(edges[0])+"\"];\n");
			}
		    } else
			for(int k=0;k<edges.length;k++) {
			    if(!visited.containsKey(edges[k])) {
				visited.put(edges[k],null);
				UnflatFilter target=edges[k].dest;
				if(target!=null)
				    buf.append(filter+" -> "+target+"[label=\""+printNext(edges[k])+"->"+printPrev(edges[k])+"\", style=dashed];\n");
			    }
			}
		}
	}
	filters=simpleNull.keySet().toArray();
	for(int i=0;i<filters.length;i++) {
	    UnflatFilter filter=(UnflatFilter)filters[i];
	    buf.append(filter+"[ label = \""+arrayPrint(filter.inWeights)+"\\nSIMPLE\\n"+arrayPrint(filter.outWeights)+"\"];\n");
	    UnflatEdge[][] out=filter.out;
	    HashMap visited=new HashMap();
	    for(int j=0;j<out.length;j++) {
		UnflatEdge[] edges=out[j];
		if(edges.length==1) {
		    if(!visited.containsKey(edges[0])) {
			visited.put(edges[0],null);
			UnflatFilter target=edges[0].dest;
			if(target!=null)
			    buf.append(filter+" -> "+target+"[label=\""+printNext(edges[0])+"->"+printPrev(edges[0])+"\"];\n");
		    }
		} else
		    for(int k=0;k<edges.length;k++) {
			if(!visited.containsKey(edges[k])) {
			    visited.put(edges[k],null);
			    UnflatFilter target=edges[k].dest;
			    if(target!=null)
				buf.append(filter+" -> "+target+"[label=\""+printNext(edges[k])+"->"+printPrev(edges[k])+"\", style=dashed];\n");
			}
		    }
	    }
	}
	filters=complexNull.keySet().toArray();
	for(int i=0;i<filters.length;i++) {
	    UnflatFilter filter=(UnflatFilter)filters[i];
	    buf.append(filter+"[ label = \""+arrayPrint(filter.inWeights)+"\\nCOMPLEX\\n"+arrayPrint(filter.outWeights)+"\"];\n");
	    UnflatEdge[][] out=filter.out;
	    HashMap visited=new HashMap();
	    for(int j=0;j<out.length;j++) {
		UnflatEdge[] edges=out[j];
		if(edges.length==1) {
		    if(!visited.containsKey(edges[0])) {
			visited.put(edges[0],null);
			UnflatFilter target=edges[0].dest;
			if(target!=null)
			    buf.append(filter+" -> "+target+"[label=\""+printNext(edges[0])+"->"+printPrev(edges[0])+"\"];\n");
		    }
		} else
		    for(int k=0;k<edges.length;k++) {
			if(!visited.containsKey(edges[k])) {
			    visited.put(edges[k],null);
			    UnflatFilter target=edges[k].dest;
			    if(target!=null)
				buf.append(filter+" -> "+target+"[label=\""+printNext(edges[k])+"->"+printPrev(edges[k])+"\", style=dashed];\n");
			}
		    }
	    }
	}
	buf.append("}\n");
	try {
	    FileWriter fw = new FileWriter(filename);
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Could not print flattened graph");
	}
    }

    private static String arrayPrint(int[] array) {
	StringBuffer out=new StringBuffer("[");
	if(array.length>0) {
	    for(int i=0;i<array.length-1;i++)
		out.append(array[i]+",");
	    out.append(array[array.length-1]);
	}
	out.append("]");
	return out.toString();
    }
    
    private static String printPrev(UnflatEdge edge) {
	IntList ints=null;
	IntList cur=null;
	UnflatEdge[] in=edge.dest.in;
	int i=0;
	for(;i<in.length&&ints==null;i++) {
	    if(in[i]==edge) {
		ints=new IntList(i);
		cur=ints;
	    }
	}
	for(;i<in.length;i++) {
	    if(in[i]==edge) {
		cur.next=new IntList(i);
		cur=cur.next;
	    }
	}
	if(ints!=null) {
	    StringBuffer out=new StringBuffer(String.valueOf(ints.val));
     	    cur=ints.next;
	    while(cur!=null) {
		out.append(","+cur.val);
		cur=cur.next;
	    }
	    return out.toString();
	} else
	    return "";
    }
    
    private static String printNext(UnflatEdge edge) {
	IntList ints=null;
	IntList cur=null;
	UnflatEdge[][] out=edge.src.out;
	int i=0;
	for(;i<out.length&&ints==null;i++) {
	    UnflatEdge[] inner=out[i];
	    for(int j=0;j<inner.length;j++)
		if(inner[j]==edge) {
		    ints=new IntList(i);
		    cur=ints;
		}
	}
	for(;i<out.length;i++) {
	    UnflatEdge[] inner=out[i];
	    for(int j=0;j<inner.length;j++)
		if(inner[j]==edge) {
		    cur.next=new IntList(i);
		    cur=cur.next;
		}
	}
	if(ints!=null) {
	    StringBuffer output=new StringBuffer(String.valueOf(ints.val));
     	    cur=ints.next;
	    while(cur!=null) {
		output.append(","+cur.val);
		cur=cur.next;
	    }
	    return output.toString();
	} else
	    return "";
    }
}

