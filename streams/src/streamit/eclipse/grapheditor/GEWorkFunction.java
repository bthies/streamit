/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

/**
 * @author jcarlos
 *
 * GEWorkFunction is a the graph editor's internal representation of a work function. 
 * 
 */
public class GEWorkFunction 
{
	private String name;
	private String pushValue;
	private String popValue;
	private String peekValue;
	
	public GEWorkFunction(String name, String push, String pop, String peek)
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
	public String getPushValue()
	{
		return this.pushValue;
	}
	 
	//	get the pop value of GEWorkFunction
	public String getPopValue()
	{
		return this.popValue;
	}
	 
	//	get the peek value of GEWorkFunction
	public String getPeekValue()
	{
		return this.peekValue;
	}
	 
	//	set the name of GEWorkFunction
	public void setName(String name)
	{
		this.name = name;
	}
	 
	//	set the pushValue of GEWorkFunction
	public void setPushValue(String pop)
	{
		this.pushValue = pop;
	}

	//	set the popValue of GEWorkFunction
	public void setPopValue(String pop)
	{
		this.popValue = pop;
	}
	//	set the peek value of GEWorkFunction
	public void setPeekValue(String peek)
	{
		this.peekValue = peek;
	}
	 
}
