/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

/**
 * GEWorkFunction is the graph editor's internal representation of a work function.
 * @author jcarlos
 */
public class GEWorkFunction 
{
	private String name;
	private int pushValue;
	private int popValue;
	private int peekValue;
	
	public GEWorkFunction(String name, int push, int pop, int peek)
	{
		this.name = name;
		this.pushValue = push;
		this.popValue = pop;
		this.peekValue =  peek;
	}
	
	// get the name of GEWorkFunction
	public String getName()
	{
		return this.name;
	}

	//	get the push value of GEWorkFunction
	public int getPushValue()
	{
		return this.pushValue;
	}
	 
	//	get the pop value of GEWorkFunction
	public int getPopValue()
	{
		return this.popValue;
	}
	 
	//	get the peek value of GEWorkFunction
	public int getPeekValue()
	{
		return this.peekValue;
	}
	 
	//	set the name of GEWorkFunction
	public void setName(String name)
	{
		this.name = name;
	}
	 
	//	set the pushValue of GEWorkFunction
	public void setPushValue(int pop)
	{
		this.pushValue = pop;
	}

	//	set the popValue of GEWorkFunction
	public void setPopValue(int pop)
	{
		this.popValue = pop;
	}
	//	set the peek value of GEWorkFunction
	public void setPeekValue(int peek)
	{
		this.peekValue = peek;
	}
	 
}
