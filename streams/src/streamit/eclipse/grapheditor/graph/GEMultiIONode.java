/*
 * Created on Apr 7, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.jgraph.JGraph;

/**
 * @author Administrator 
 */
public class GEMultiIONode extends GEStreamNode{

	/**
	 * The weights corresponding to the node.
	 */
	protected int[] weights;

	
	/**
	 * GEMultiIONode constructor.
	 * @param name The name of the GEMultiIONode.
	 * @param weights The weights of the GEMultiIONode.
	 */
	public GEMultiIONode(String type, String name,int[] weights)
	{
		super(type , name);
		this.weights = weights;
		
	}
	
	/**
	 * GEMultiIONode constructor.
	 * @param name The name of the GEMultiIONode.
	 */
	public GEMultiIONode(String type, String name)
	{
		super(type , name);
		this.weights = null;	
	}
	
	
	/**
	 * Get the weights of this 
	 * @return The weights corresponding to the GEMultiIONode
	 */
	public int[] getWeights()
	{
		return this.weights;
	}
	
	/**
	 * Set the weights of the GEMultiIONode
	 * @param weigths 
	 */
	public void setWeights(int[] weights)
	{
		this.weights = weights;
	}
	




	/**
	 * Get the weight as a string of the form: "(weight1, weight2, weight3,... , weightN)".
	 * @return String representation of the weights of the GEMultiIONode.
	 */
	public String getWeightsAsString()
	{
		String strWeight = "(";
		for(int i = 0; i < this.weights.length; i++)
		{
			if (i != 0)
			{
				strWeight += ", ";
			}
			strWeight += this.weights[i];	
		}
		strWeight += ")";
		return strWeight;
	}

	/**
	 * Change the display information of the GEMultiIONode.
	 * @param jgraph JGraph
	 */
	public void setDisplay(JGraph jgraph)
	{
		this.setInfo(this.getWeightsAsString());
		super.setDisplay(jgraph);
	}

	/**
	 * Get the input tape of the GEMultiIONode (it will be the same as the one of its encapsualting parent)
	 * @return String output tape of the node. If the node has no encapsulating node, return null.
	 */
	public String getInputTape()
	{
		if (this.encapsulatingNode != null)
		{
			return this.encapsulatingNode.getInputTape();
		}
		return null;
	}


	/**
	 * Get the output tape of the node (it will be the same as the one of its encapsualting parent)
	 * @return String output tape of the node. If the splitter has no encapsulating node, return null.
	 */
	public String getOutputTape()
	{
		if (this.encapsulatingNode != null)
		{
			return this.encapsulatingNode.getOutputTape();
		}
		return null;
	}


	/**
	 * Removes the weight at the given index
	 * @param index int that must be greater thatn zero and less than number of weights.
	 * @return True if it was possible to remove the weight; otherwise, return fasle.
	 */
	public boolean removeWeightAt(int index)
	{

		int arrayLength =  this.weights.length;	
		/** must have selected a valid index */
		if ((index >= arrayLength) || (index < 0))
		{
			return false;
		}
		
		int[] w = new int[arrayLength - 1];
		for (int i = 0; i < index; i++)
		{
				w[i] = weights[i];
		}
		int j = index + 1;
		for (int i = index; i < w.length; i++)
		{
			w[i] = weights[j];
			j++;
		}
		this.weights = w;
		return true;
	}

	public boolean addWeightAt(int index, int weightValue)
	{
		int arrayLength =  this.weights.length;
		int indexToAdd = index;
		if ((index >= arrayLength) || (index < 0))
		{
			indexToAdd = arrayLength;
		}
		int [] w = new int[arrayLength + 1];
		
		for (int i=0; i < indexToAdd; i++)
		{
			w[i] = this.weights[i];	
		}
		
		/** Set the weight at index to the default weight */
		w[indexToAdd] = weightValue;
		
		int j = indexToAdd;
		
		for (int i = indexToAdd + 1 ; i < w.length; i++)
		{
			w[i] = this.weights[j];
		}
		
		this.weights = w;
		return true;
	}
	
	public int getWeightAt(int index)
	{
		return this.weights[index];
	}
	
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds){};

	/**
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.
	 * @param nameList List of the names of the nodes that have already been added to the template code.  
	 */
	public void outputCode(StringBuffer strBuff, ArrayList nameList){};
	
	/**
	 * Contructs the GEJoiner by setting its label and its draw attributes..
	 * @return GEStreamNode (the GEJoiner).
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvl){return null;}
}
