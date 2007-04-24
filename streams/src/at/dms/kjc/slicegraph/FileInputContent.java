package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for file input.
 * @author jasperln
 */
public class FileInputContent extends InputContent {
    private String filename; //The filename
    
    /**
     * Copy constructor for FileInputContent.
     * @param content The FileInputContent to copy.
     */
    public FileInputContent(FileInputContent content) {
        super(content);
        filename=content.filename;
    }

    /**
     * Construct FileInputContent from SIRFileReader.
     * @param filter The SIRFileReader used to contruct the FileInputContent.
     */
    public FileInputContent(SIRFileReader filter) {
        super(filter);
        filename=filter.getFileName();
    }

    /**
     * Construct FileInputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the FileInputContent.
     */
    public FileInputContent(UnflatFilter unflat) {
        super(unflat);
        filename=((SIRFileReader)unflat.filter).getFileName();
    }

    /**
     * Returns filename of FileInputContent.
     */
    public String getFileName() {
        return filename;
    }
    
    /**
     * Returns if output format of file is floating point.
     */
    public boolean isFP() 
    {
        return getOutputType().isFloatingPoint();
    }
    
    /**
     * Return the type of the file reader
     *  
     * @return The type.
     */
    public CType getType() {
        return getOutputType();
    }
    

    /**
     * Create kopi code that when translated to C will manipulate the file.
     * current version handles int and floating point only.
     * Should be expanded to handle structs and arrays.
     */
    public void createContent() {
        int pushrate = 1;
        
        String fileVar = this.getName() + "_v";
        
        this.inputType = CStdType.Void;

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
        JExpression[] params = {
            new JStringLiteral(null, getFileName()),
            new JStringLiteral(null, "r")
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
                "init_fileread" + my_unique_ID ,
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                initBlock,
                null,
                null);
        this.addAMethod(initMethod);
        this.initFunction = initMethod;
        
        //create work function
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
    
        JVariableDefinition value = new JVariableDefinition(null,
                                                            0,
                                                            getOutputType(),
                                                            "__value__" + my_unique_ID,
                                                            null);
        workBlock.addStatement
            (new JVariableDeclarationStatement(null, value, null));
    
        // RMR { support ascii or binary file operations for reading
        JMethodCallExpression fileio;

        if (KjcOptions.asciifileio) {
            //create a temp variable to hold the value we are reading
            JExpression[] fscanfParams = new JExpression[3];
            //create the params for fscanf
            fscanfParams[0] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                         file.getVariable().getIdent());
            fscanfParams[1] = new JStringLiteral(null,
                                                 getOutputType().isFloatingPoint() ?
                                                 "%f\\n" : "%d\\n");
            fscanfParams[2] = new JLocalVariableExpression(null, value);
            
            //fscanf call
            JMethodCallExpression fscanf = 
                new JMethodCallExpression(null, new JThisExpression(null),
                        "fscanf",
                        fscanfParams);        
            
            fileio = fscanf;
        }
        else {
            // create the params for fread(&variable, sizeof(type), 1, file)
            JExpression[] freadParams = new JExpression[4];
            
            // the first parameter: &(variable); treat the & operator as a function call
            JExpression[] addressofParameters = new JExpression[1];
            
            addressofParameters[0] = new JLocalVariableExpression(null, value);
            
            JMethodCallExpression addressofCall =
                new JMethodCallExpression(null, "&", addressofParameters);
            
            freadParams[0] = addressofCall;
            
            // the second parameter: the call to sizeof(type)
            JExpression[] sizeofParameters = new JExpression[1];
            
            sizeofParameters[0] = 
                new JLocalVariableExpression(null, 
                                             new JVariableDefinition(null, 0,
                                                                     CStdType.Integer,
                                                                     (getOutputType().isFloatingPoint() ? 
                                                                      "float" : "int"),
                                                                     null));
            
            JMethodCallExpression sizeofCall =
                new JMethodCallExpression(null, "sizeof", sizeofParameters);
            
            freadParams[1] = sizeofCall;
            
            // the third parameter: read one element at a time
            freadParams[2] = new JIntLiteral(pushrate);
            
            // the last parameter: the file pointer
            freadParams[3] = new JFieldAccessExpression(null, new JThisExpression(null),
                                                        file.getVariable().getIdent());
            
            JMethodCallExpression fread = 
                new JMethodCallExpression(null, new JThisExpression(null),
                                          "fread",
                                          freadParams);

            fileio = fread;
        }
        // } RMR
    
        workBlock.addStatement(new JExpressionStatement(null, fileio, null));
    
        SIRPushExpression push = 
            new SIRPushExpression(new JLocalVariableExpression(null, value), 
                                  getOutputType());
        
        workBlock.addStatement(new JExpressionStatement(null, push, null));

        JMethodDeclaration workMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "work_fileread" + my_unique_ID ,
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                workBlock,
                null,
                null);
        workMethod.setPop(0);
        workMethod.setPeek(0);
        workMethod.setPush(pushrate);
        this.addAMethod(workMethod);
        this.steady = new JMethodDeclaration[]{workMethod};
    }

    
}
