package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.common.*;

/**
 * Predefined FilterContent for file input, expands to implement a FileReader.
 * @author jasperln
 */
public class FileInputContent extends InputContent {
    private String filename; //The filename
    
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private FileInputContent() {
        super();
    }

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
     * The C file will need to include <stdio.h>
     * current version handles int and floating point only.
     * Should be expanded to handle structs and arrays.
     */
    public void createContent() {
        // on entry: 
        // name is set to something reasonable
        // input and output types set correctly
        // bogus init function exists
        // bogus work function exists
        // methods set to the two bogus functions.
        
        // push rate referred to in a few places.
        int pushrate = 1;
        
        String fileVar = "_fileReader_" + my_unique_ID;
        
        //create fields
        JFieldDeclaration file = 
            new JFieldDeclaration(null,
                                  new JVariableDefinition(null,
                                                          0,
                                                          new CEmittedTextType("FILE *"),
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
        // do some standard C error checking here.
        // do we need to put this in a separate method to allow subclass to override?
        initBlock.addStatement(new JExpressionStatement(
                new JEmittedTextExpression(new Object[]{
                        "if (",
                        new JFieldAccessExpression(
                                new JThisExpression(null),
                                file.getVariable().getIdent()),
                        " == NULL) { perror(\"error opening "+ filename + "\"); }"
                })));
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
        this.initFunction = initMethod;
        
        //create work function
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
        ALocalVariable tmp = ALocalVariable.makeTmp(getOutputType());
        workBlock.addStatement(tmp.getDecl());
        
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
            fscanfParams[2] = tmp.getRef();
            
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
            
            addressofParameters[0] = tmp.getRef();
            
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
            new SIRPushExpression(tmp.getRef(), getOutputType());
        
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
        this.steady = new JMethodDeclaration[]{workMethod};
        // discard old dummy methods and set up the new ones.
        this.setTheMethods(new JMethodDeclaration[]{initMethod,workMethod});
    }

    

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.FileInputContent other = new at.dms.kjc.slicegraph.FileInputContent();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.FileInputContent other) {
        super.deepCloneInto(other);
        other.filename = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.filename);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
