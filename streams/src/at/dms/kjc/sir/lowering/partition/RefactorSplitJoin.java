package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;

public class RefactorSplitJoin {

    public static SIRSplitJoin addHierarchicalChildren2(SIRSplitJoin sj, PartitionGroup partition) {
	int[] oldSplit=sj.getSplitter().getWeights();
	int[] oldJoin=sj.getJoiner().getWeights();
	JExpression[] newSplit=new JExpression[partition.size()];
	JExpression[] newJoin=new JExpression[partition.size()];
	SIRSplitJoin newSplitJoin=new SIRSplitJoin();
	newSplitJoin.setParent(sj.getParent());
	newSplitJoin.setIdent(sj.getIdent());
	newSplitJoin.setFields(sj.getFields());
	newSplitJoin.setMethods(sj.getMethods());
	for(int i=0,j=0;i<partition.size();i++) {
	    int incr=partition.get(i);
	    if(incr==1) {
		newSplitJoin.add(sj.get(j));
		newSplit[i]=new JIntLiteral(oldSplit[j]);
		newJoin[i]=new JIntLiteral(oldJoin[j]);
	    } else {
		int sumSplit=0;
		int sumJoin=0;
		JExpression[] childSplit=new JExpression[incr];
		JExpression[] childJoin=new JExpression[incr];
		SIRSplitJoin childSplitJoin=new SIRSplitJoin();
		for(int k=incr,l=j,m=0;k>0;k--,l++,m++) {
		    sumSplit+=oldSplit[l];
		    sumJoin+=oldJoin[l];
		    childSplit[m]=new JIntLiteral(oldSplit[l]);
		    childJoin[m]=new JIntLiteral(oldJoin[l]);
		    childSplitJoin.add(sj.get(l));
		}
		newSplit[i]=new JIntLiteral(sumSplit);
		newJoin[i]=new JIntLiteral(sumJoin);
		childSplitJoin.setSplitter(SIRSplitter.create(childSplitJoin,sj.getSplitter().getType(),childSplit));
		childSplitJoin.setJoiner(SIRJoiner.create(childSplitJoin,sj.getJoiner().getType(),childJoin));
		newSplitJoin.add(childSplitJoin);
	    }
	    j+=incr;
	}
	newSplitJoin.setSplitter(SIRSplitter.create(newSplitJoin,sj.getSplitter().getType(),newSplit));
	newSplitJoin.setJoiner(SIRJoiner.create(newSplitJoin,sj.getJoiner().getType(),newJoin));
	// replace in parent
	sj.getParent().replace(sj,newSplitJoin);

    return newSplitJoin;
}

    /**
     * Given a splitjoin <sj> and a partitioning <partition> of its
     * children, returns a new splitjoin in which all the elements of
     */
    public static SIRSplitJoin addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
	// the new and old weights for the splitter and joiner
	int[] oldSplit=sj.getSplitter().getWeights();
	int[] oldJoin=sj.getJoiner().getWeights();
	JExpression[] newSplit=new JExpression[partition.size()];
	JExpression[] newJoin=new JExpression[partition.size()];

	// new splitjoin
	SIRSplitJoin newSplitJoin=new SIRSplitJoin(sj.getParent(), 
						   sj.getIdent(),
						   sj.getFields(), 
						   sj.getMethods());

	// for all the partitions...
	for(int i=0;i<partition.size();i++) {
	    int partSize=partition.get(i);
	    if (partSize==1) {
		// if there is only one stream in the partition, then
		// we don't need to do anything; just add the children
		int pos = partition.getFirst(i);
		newSplitJoin.add(sj.get(pos), sj.getParams(pos));
		newSplit[i]=new JIntLiteral(oldSplit[pos]);
		newJoin[i]=new JIntLiteral(oldJoin[pos]);
	    } else {
		int sumSplit=0;
		int sumJoin=0;
		// child split and join weights
		JExpression[] childSplit=new JExpression[partSize];
		JExpression[] childJoin=new JExpression[partSize];
		// the child splitjoin
		SIRSplitJoin childSplitJoin=new SIRSplitJoin(newSplitJoin,
							     newSplitJoin.getIdent() + "_child" + i,
							     JFieldDeclaration.EMPTY(),
							     JMethodDeclaration.EMPTY());
		// move children into hierarchical splitjoin
		for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
		    sumSplit+=oldSplit[l];
		    sumJoin+=oldJoin[l];
		    childSplit[k]=new JIntLiteral(oldSplit[l]);
		    childJoin[k]=new JIntLiteral(oldJoin[l]);
		    childSplitJoin.add(sj.get(l), sj.getParams(l));
		}
		// in the case of a duplicate splitter, <create>
		// disregards the weights array and makes them all 1
		childSplitJoin.setSplitter(SIRSplitter.create(childSplitJoin,sj.getSplitter().getType(),childSplit));
		childSplitJoin.setJoiner(SIRJoiner.create(childSplitJoin,sj.getJoiner().getType(),childJoin));

		// update new toplevel splitjoin
		newSplit[i]=new JIntLiteral(sumSplit);
		newJoin[i]=new JIntLiteral(sumJoin);
		newSplitJoin.add(childSplitJoin);
	    }
	}
	// set the splitter and joiner types according to the new weights
	newSplitJoin.setSplitter(SIRSplitter.create(newSplitJoin,sj.getSplitter().getType(),newSplit));
	newSplitJoin.setJoiner(SIRJoiner.create(newSplitJoin,sj.getJoiner().getType(),newJoin));
	// replace in parent
	sj.getParent().replace(sj,newSplitJoin);

	// return new sj
	return newSplitJoin;
    }

}
