package at.dms.kjc.rstream;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * A file writer class that implements a file writer as a normal
 * (non-predefined) filter in the SIR graph.  The filter calls
 * fopen in its init function and then fprintf in its work
 * function.  This can be fused by the partitioner, fclose is 
 * never called.
 *
 * @author Michael Gordon
 */

public class File_Writer extends SIRFilter  
{

    private static int uniqueID = 0;
    
    public File_Writer(SIRFileWriter fw) 
    {
	String fileVar = "__file__" + uniqueID++;

	//set I/O rates
	this.setParent(fw.getParent());
	this.setPush(0);
	this.setPeek(1);
	this.setPop(1);
	this.setIdent("File_Writer" + uniqueID);
	this.setOutputType(CStdType.Void);
	this.setInputType(fw.getInputType());

		//create fields
	JFieldDeclaration file = 
	    new JFieldDeclaration(null,
				  new JVariableDefinition(null,
							  0,
							  CStdType.Integer,
							  fileVar,
							  null),
				  null, null);
	this.addField(file);

	//create init function
	JBlock initBlock = new JBlock(null, new JStatement[0], null);
	//create the file open command
	JExpression[] params = 
	    {
		new JStringLiteral(null, fw.getFileName()),
		new JStringLiteral(null, "w")
	    };
	
		

	JMethodCallExpression fopen = 
	    new JMethodCallExpression(null, new JThisExpression(null),
				      "fopen", params);
	//assign to the file handle
	JAssignmentExpression fass = 
	    new JAssignmentExpression(null, 
				      new JFieldAccessExpression(null,
								 new JThisExpression(null),
								 file.getVariable().getIdent()),
				      fopen);

	initBlock.addStatement(new JExpressionStatement(null, fass, null));
	//set this as the init function...
	this.setInit(new JMethodDeclaration(null,
					    at.dms.kjc.Constants.ACC_PUBLIC,
					    CStdType.Void,
					    "init_filewrite" + uniqueID ,
					    JFormalParameter.EMPTY,
					    CClassType.EMPTY,
					    initBlock,
					    null,
					    null));

	//create work function
	JBlock workBlock = new JBlock(null, new JStatement[0], null);

	SIRPopExpression pop = new SIRPopExpression(fw.getInputType());
	
	//the params for the fprintf call
	JExpression[] fprintfParams = new JExpression[3];
	fprintfParams[0] = new JFieldAccessExpression(null, new JThisExpression(null),
						      file.getVariable().getIdent());
	fprintfParams[1] = new JStringLiteral(null,
					   fw.getInputType().isFloatingPoint() ?
					      "%f\\n" : "%d\\n");
	fprintfParams[2] = pop;

	JMethodCallExpression fprintf = 
	    new JMethodCallExpression(null, new JThisExpression(null),
				      Names.fprintf,
				      fprintfParams);
	
	workBlock.addStatement(new JExpressionStatement(null, fprintf, null));
	this.setWork(new JMethodDeclaration(null,
					    at.dms.kjc.Constants.ACC_PUBLIC,
					    CStdType.Void,
					    "work_filewrite" + uniqueID ,
					    JFormalParameter.EMPTY,
					    CClassType.EMPTY,
					    workBlock,
					    null,
					    null));
    }
}
