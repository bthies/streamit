package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.*;

//If filter is linear

public class Linear extends RawExecutionCode implements Constants {
    //private static JMethodDeclaration emptyInit=new JMethodDeclaration(null,0,CStdType.Void,"emptyInit",new JFormalParameter[0],new CClassType[0],new JBlock(null,new JStatement[0],null),null,null);
    private static JMethodDeclaration[] emptyMethods=new JMethodDeclaration[0];
    private static JFieldDeclaration[] emptyFields=new JFieldDeclaration[0];

    public Linear(FilterInfo filterInfo) {
	super(filterInfo);
	System.out.println("Generating code for " + filterInfo.filter + " using Linear.");
    }

    public JBlock getSteadyBlock() {
	JStatement[] body=new JStatement[1];
	InlineAssembly inline=new InlineAssembly();
	body[0]=inline;
	inline.add("#FOO");
	inline.add("#BAR");
	return new JBlock(null,body,null);
    }
    
    public JMethodDeclaration getInitStageMethod() {
	//return emptyInit;
	return null;
    }
    
    public JMethodDeclaration[] getHelperMethods() {
	return emptyMethods;
    }

    public JFieldDeclaration[] getVarDecls() {
	return emptyFields;
    }
}
