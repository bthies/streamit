/*
 * Created on Jun 20, 2003
 */

package streamit.eclipse.grapheditor.graph;

import java.io.Serializable;

/**
 * GEWorkFunction is representation of a work function.
 * @author jcarlos
 */
public class GEWorkFunction implements Serializable
{
	/**
	 * The name of the GEWorkFunction.
	 */
	private String name;
	
	/**
	 * The push value of the GEWorkFunction.
	 */
	private int pushValue;
	
	/**
	 * The pop value of the GEWorkFunction.
	 */
	private int popValue;
	
	/**
	 * The peek value of the GEWorkFunction.
	 */
	private int peekValue;
	
	/**
	 * GEWorkFunction constructor.
	 * @param name Name of the GEWorkFunction.
	 * @param push int that specifies the push value of the GEWorkFunction.
	 * @param pop int that specifies the pop value of the GEWorkFunction.
	 * @param peek int that specifies the peek value of the GEWorkFunction.
	 */
	public GEWorkFunction(String name, int push, int pop, int peek)
	{
		this.name = name;
		this.pushValue = push;
		this.popValue = pop;
		this.peekValue =  peek;
	}
	
	/**
	 * Get the name of <this>.
	 * @return name of GEWorkFunction. 
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Get the push value of <this>.
	 * @return Push value of GEWorkFunction.
	 */
	public int getPushValue()
	{
		return this.pushValue;
	}
	 
	/**
	 * Get the pop value of <this>.
	 * @return Pop value of GEWorkFunction.
	 */
	public int getPopValue()
	{
		return this.popValue;
	}
	
	/**
	 * Get the peek value of <this>
	 * @return Peek value of GEWorkFunction
	 */
	public int getPeekValue()
	{
		return this.peekValue;
	}
	 
	/**
	 * Set the name of <this>.
	 * @param name Name of GEWorkFunction.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	 
	/**
	 * Set the push value of <this>.
	 * @param push Push value of GEWorkFunction.
	 */
	public void setPushValue(int push)
	{
		this.pushValue = push;
	}

	/**
	 * Set the pop value of <this>
	 * @param pop Pop value of GEWorkFunction
	 */	
	public void setPopValue(int pop)
	{
		this.popValue = pop;
	}
	
	/**
	 * Set the peek value of <this>.
	 * @param peek Peek value of GEWorkFunction.
	 */
	public void setPeekValue(int peek)
	{
		this.peekValue = peek;
	}
	 
}
