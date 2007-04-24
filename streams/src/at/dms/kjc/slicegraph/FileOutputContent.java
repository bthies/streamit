package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for file output.
 * @author jasperln
 */
public class FileOutputContent extends OutputContent {
    private String filename; //The filename
    private int outputs; //Expected number of outputs

    /**
     * Copy constructor for FileOutputContent.
     * @param content The FileOutputContent to copy.
     */
    public FileOutputContent(FileOutputContent content) {
        super(content);
        outputs = -1;
        filename=content.filename;
    }

    /**
     * Construct FileInputContent from SIRFileWriter.
     * @param filter The SIRFileWriter used to contruct the FileInputContent.
     */
    public FileOutputContent(SIRFileWriter filter) {
        super(filter);
        outputs = -1;
        filename=filter.getFileName();
    }

    /**
     * Construct FileInputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the FileInputContent.
     */
    public FileOutputContent(UnflatFilter unflat) {
        super(unflat);
        outputs = -1;
        filename=((SIRFileWriter)unflat.filter).getFileName();
    }

    /**
     * Returns expected number of outputs.
     */
    public int getOutputs() 
    {
        return outputs;
    }
    
    /**
     * Sets expected number of outputs.
     */
    public void setOutputs(int i) 
    {
        outputs = i;
    }

    /**
     * Returns filename of FileOutputContent.
     */
    public String getFileName() {
        return filename;
    }

    /**
     * Get the type of the file writer
     * . 
     * @return The type.
     */
    public CType getType() {
        return getInputType();
    }
    
    /**
     * Returns if output format of file is floating point.
     */
    public boolean isFP() 
    {
        return getInputType().isFloatingPoint();
    }
    
    /**
     * Create kopi code that when translated to C will manipulate the file.
     */
    public void createContent() {
        String fileVar = this.getName() + "_v";

        this.outputType = CStdType.Void;

        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,
                                  new JVariableDefinition(null,
                                                          0,
                                                          CStdType.Integer,
                                                          fileVar,
                                                          null),
                                  null, null);
        this.addAField(file);
    
        //create init function
        JBlock initBlock = new JBlock(null, new JStatement[0], null);
        //create the file open command
        JExpression[] params = 
            {
                new JStringLiteral(null, getFileName()),
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
        JMethodDeclaration initMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "init_filewrite" + my_unique_ID ,
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                initBlock,
                null,
                null);
        this.addAMethod(initMethod);
        this.initFunction = initMethod;
    
        //create work function
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
    
        SIRPopExpression pop = new SIRPopExpression(getInputType());
    
    
        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //the params for the fprintf call
            JExpression[] fprintfParams = new JExpression[3];
            fprintfParams[0] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                          file.getVariable().getIdent());
            fprintfParams[1] = new JStringLiteral(null,
                                                  getInputType().isFloatingPoint() ?
                                                  "%f\\n" : "%d\\n");
            fprintfParams[2] = pop;
            
            JMethodCallExpression fprintf = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fprintf",
                                          fprintfParams);
            fileio = fprintf;
        }
        else {
            // create the params for fwrite(&variable, sizeof(type), 1, file)
            JExpression[] fwriteParams = new JExpression[4];
            
            // the first parameter: &(variable); treat the & operator as a function call
            JExpression[] addressofParameters = new JExpression[1];
            
            addressofParameters[0] = pop;
            
            JMethodCallExpression addressofCall =
                new JMethodCallExpression(null, "&", addressofParameters);

            fwriteParams[0] = addressofCall;
            
            // the second parameter: the call to sizeof(type)
            JExpression[] sizeofParameters = new JExpression[1];
            
            sizeofParameters[0] = 
                new JLocalVariableExpression(null, 
                                             new JVariableDefinition(null, 0,
                                                                     CStdType.Integer,
                                                                     (getInputType().isFloatingPoint() ? 
                                                                      "float" : "int"),
                                                                     null));
            
            JMethodCallExpression sizeofCall =
                new JMethodCallExpression(null, "sizeof", sizeofParameters);
            sizeofCall.setTapeType(CStdType.Integer);
            
            fwriteParams[1] = sizeofCall;
            
            // the third parameter: read one element at a time
            fwriteParams[2] = new JIntLiteral(1);
            
            // the last parameter: the file pointer
            fwriteParams[3] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                         file.getVariable().getIdent());
            
            JMethodCallExpression fwrite = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fwrite",
                                          fwriteParams);
            fwrite.setTapeType(CStdType.Void);

            fileio = fwrite;
        }

        workBlock.addStatement(new JExpressionStatement(null, fileio, null));
        
        JMethodDeclaration workFunction = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "work_filewrite" + my_unique_ID ,
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                workBlock,
                null,
                null);

        workFunction.setPop(1);
        workFunction.setPeek(1);
        workFunction.setPush(0);
        this.addAMethod(workFunction);
        this.steady = new JMethodDeclaration[]{workFunction};
    }

}
