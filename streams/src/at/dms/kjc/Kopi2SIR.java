//===========================================================================
//
//   FILE: kopi2SIR.java:
//   
//   Author: Michael Gordon
//   Date: Wed Sep 26 23:00:44 2001
//
//   Function:  Implements the KJC to SIR pass using a KJCVisitor
//
//===========================================================================


package at.dms.kjc;

import java.io.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.util.*;
import java.util.*;


public class Kopi2SIR extends Utils implements AttributeVisitor, Cloneable
{
    /* The entire application */
    private JCompilationUnit[] application;

    /* The  one to one parent*/
    private SIRStream parentStream;
    /* The Top Level SIR Operator */
    private SIRStream topLevel;
    
    /*Object used to disregard the return values from the attributed
      visitor methods */
    private Object trash;

    private int num = 0;
    /* the current source line number (set as part of blockstart) */
    private int lineNumber = -1;
    
    //Global state holding the current method we are processing if any 
    //set/reset in visitMethodCall
    private String currentMethod;
    
    //This hashtable acts as a symbol table to store named (not anonymous)
    //SIR classes
    private Hashtable visitedSIROps;
    //This hashtable acts as a symbol table for local variables and 
    //field variables, it is indexed by the JLocalVariable or JFieldVariable
    //and store the corresponding SIR object
    private Hashtable symbolTable;

    //This vector store all the interface declarations for a toplevel prgm
    private Vector interfaceList;

    //This vector stores all the interface tables as derived from the
    //regReceiver statements.  An interface table lists the methods
    //that implement a given interface for a given class.
    private Vector interfaceTableList;

    //This vector stores all of the structure declarations.
    private Vector structureList;

    //The current dependency chain we are following when 
    //trying to resolve a class instantiation to a stream
    //stores strings of the stream names
    private LinkedList searchList;

    //Array of the parameters of the last nonanonymous method
    //Used to correctly handle their reduction in anonymous classes
    private JFormalParameter[] params;

    //Array of names of the parameters
    private String[] paramNames;

    //List of lists of final vars encountered to allow propagation into anonymous classes
    //Each element of finalVars represents the list of finalvars encountered for a method
    //in stack of methods being parsed
    private LinkedList finalVars;

    //Keeps track if current class is anonymous
    private boolean anonCreation;

    //The latency of the next message to be sent
    private SIRLatency nextLatency;

    //Uncomment the println for debugging
    private void printMe(String str) {
	// System.out.println(str);
    }

    public Kopi2SIR() {
	parentStream = null;
	topLevel = null;

	currentMethod = null;
	visitedSIROps = new Hashtable(100);
	symbolTable = new Hashtable(300);
	interfaceList = new Vector(100);
	interfaceTableList = new Vector(100);
        structureList = new Vector(100);
	searchList = new LinkedList();
	application = null;
	initBuiltinFilters();
	finalVars=new LinkedList();
        nextLatency = SIRLatency.BEST_EFFORT;
    }

    public Kopi2SIR(JCompilationUnit[] app) {
	parentStream = null;
	topLevel = null;

	currentMethod = null;
	visitedSIROps = new Hashtable(100);
	symbolTable = new Hashtable(300);
	interfaceList = new Vector(100);
	interfaceTableList = new Vector(100);
        structureList = new Vector(100);
	searchList = new LinkedList();
	this.application = app;
	initBuiltinFilters();
	finalVars=new LinkedList();
        nextLatency = SIRLatency.BEST_EFFORT;
    }

    private String printLine(JPhylum l) {
	return (String.valueOf(l.getTokenReference().getLine()) + ": ");
    }

    //add any special filters to the symbol table
    //when they are added, they will be cloned and any parameters that need to be set
    //will be set...
    private void initBuiltinFilters() {
	SIRFileReader fr = new SIRFileReader();
	addVisitedOp("FileReader", fr);
	
	SIRFileWriter fw = new SIRFileWriter();
	addVisitedOp("FileWriter", fw);

	SIRIdentity sirId = new SIRIdentity(null);
	addVisitedOp("Identity", sirId);
    }	
		     
    private void addVisitedOp(String className, SIROperator sirop)  {
	/* Don't worry about duplicates; this will be caught by the
	 * type checker of Kopi, since you can only have one Java
	 * class defined per package.  In the case of recursive
	 * streams, adding an op a second time will obliterate the
	 * first one, although nothing should rely on this.
	 * 
	if (visitedSIROps.get(className) != null)
	    at.dms.util.Utils.fail("Duplicate Definition of " + className);
	*/
	visitedSIROps.put(className, sirop);
    }

    private SIROperator getVisitedOp(String className) 
    {
	SIROperator visitedOp=(SIROperator)visitedSIROps.get(className);
	if((visitedOp instanceof SIRContainer)&&(searchList.contains(className))) {
	    /*
	    at.dms.util.Utils.fail("Mutually recursive stream defintion of " + 
				       className);
	    */
	    SIRRecursiveStub stub = new SIRRecursiveStub(className, (Kopi2SIR)AutoCloner.deepCopy(this));
	    return stub;
	}
	if (visitedOp!=null) {
	    return visitedOp;
	}
	//if not look for it over the entire application (if given)
	else if (application != null) {
	    //if this class name is already trying to be resolved then
	    //we have a mutually recursive defintion
	    if (searchList.contains(className)) {
		System.err.println(visitedSIROps);
		at.dms.util.Utils.fail("Mutually recursive stream defintion of " + 
				       className);
	    }
	    return searchForOp(className);
	}
	at.dms.util.Utils.fail(lineNumber + ": Cannot find declaration of stream " +
			       className);
	return null;
    }

    /**
     * Searches for <className> and returns associated op.  Does not
     * worry about anything recursive--should call getVisitedOp in the
     * general case.
     */
    public SIROperator searchForOp(String className) {
	//add this class name to the list of streams we are resolving
	searchList.add(className);
	for (int unit = 0; unit < application.length; unit++) {
	    JTypeDeclaration[] decls = application[unit].getTypeDeclarations();
	    for (int i = 0; i < decls.length; i++) {
		if (decls[i] instanceof JClassDeclaration) {
		    //if this class declaration is a match, visit it and 
		    //get the SIRStream
		    if (((JClassDeclaration)decls[i]).
			getSourceClass().getIdent().equals(className)) {
			//visit the class declaration and return the stream
			SIROperator sir = (SIROperator)decls[i].accept(this);
			//visitClassDecl will add the stream to the table
			
			//remove the name from the resolve list
			searchList.remove(className);
			//return the stream
			return sir;
		    }
		}
	    }
	}
	return null;
    }
    
    private void blockStart(String str)  {
	printMe(str);
    }

    private void blockStart(String str, JPhylum getLineFromMe)  {
        if (getLineFromMe != null &&
            getLineFromMe.getTokenReference() != null)
            lineNumber = getLineFromMe.getTokenReference().getLine();
	printMe("attribute_visit" + str +"\n");
    }

    /* creates a new SIROperator setting parentStream to this object
       also, if the SIROperator is one to one, parentStream is set */
    private SIROperator newSIROP(JClassDeclaration clazz)  {
	String TYPE = clazz.getSourceClass().getSuperClass().getIdent();
    
	if (TYPE.equals("Pipeline") || TYPE.equals("StreamIt")) {
	    SIRPipeline current = new SIRPipeline((SIRContainer)parentStream, 
						  clazz.getIdent(),
				   JFieldDeclaration.EMPTY(),
				   JMethodDeclaration.EMPTY());
	    parentStream = current;
	    return current;
	}
        if (TYPE.equals("Structure")) {
            SIRStructure current = new SIRStructure();
            current.setIdent(clazz.getIdent());
            current.setFields(JFieldDeclaration.EMPTY());
            parentStream = current;
            return current;
        }
	if (TYPE.equals("Filter")) {
	    SIRFilter current = new SIRFilter();
	    current.setParent((SIRContainer)parentStream);
	    current.setIdent(clazz.getIdent());
	    current.setInputType(CStdType.Void);
	    current.setOutputType(CStdType.Void);
	    parentStream = current;
	    return current;
	}
        if (TYPE.equals("PhasedFilter")) {
            SIRPhasedFilter current = new SIRPhasedFilter();
            current.setParent((SIRContainer)parentStream);
            current.setIdent(clazz.getIdent());
            current.setInputType(CStdType.Void);
            current.setOutputType(CStdType.Void);
            parentStream = current;
            return current;
        }
	if (TYPE.equals("FeedbackLoop")) {
	    SIRFeedbackLoop current = new SIRFeedbackLoop();
	    current.setParent((SIRContainer)parentStream);
	    current.setIdent(clazz.getIdent());
	    parentStream = current;
	    return current;
	}
	if (TYPE.equals("SplitJoin")) {
	    SIRSplitJoin current = new SIRSplitJoin((SIRContainer)parentStream,
						    clazz.getIdent());
	    parentStream = current;
	    return current;
	}
						    
	at.dms.util.Utils.fail(printLine(clazz) + 
			       "You are creating an unsupported streaMIT Operator: "
			       + TYPE + ".");
	return null;
    }
    
    /**
     * Returns true if the given JStatement defines a call to 
     * an SIR expression
     **/
    private boolean isSIRExp(JMethodCallExpression exp)  
    {

	if (exp.getIdent().startsWith("push") ||
	    exp.getIdent().startsWith("pop")  ||
	    exp.getIdent().startsWith("peek") ||
	    exp.getIdent().startsWith("println") )
	    return true;
	
	return false;
    }

    /* Given a method call expression, this function will create/return a 
       SIR Expression if the method call orignally denoted one. */
    private JPhylum newSIRExp(JMethodCallExpression exp, JExpression[] args)  
    {
	JPhylum newExp = null;
	printMe("In newSIRExp");
	if (args.length > 1)
	    at.dms.util.Utils.fail(printLine(exp) +
				   "Too many args to SIR call.");
	
	if (args.length == 0)
	    args = JExpression.EMPTY;
	else
	    args[0] = (JExpression) args[0].accept(this);
	
	if (exp.getIdent().startsWith("push")) {
	    newExp = new SIRPushExpression(args[0]);
	    ((SIRPushExpression)newExp).setTapeType(parentStream.getOutputType());
	}
	if (exp.getIdent().startsWith("pop")) {
	    newExp = new SIRPopExpression();
	    ((SIRPopExpression)newExp).setTapeType(parentStream.getInputType());
	}
	if (exp.getIdent().startsWith("peek")) {
	    newExp = new SIRPeekExpression(args[0]);
	    ((SIRPeekExpression)newExp).setTapeType(parentStream.getInputType()); 
	}
	if (exp.getIdent().startsWith("println")) {
	    newExp = new SIRPrintStatement(null, args[0], null);
	}
	
	return newExp;
    }
   
    //Perform any post visiting operations after a class is declared
    private void postVisit(SIROperator current) 
    {
	if (current instanceof SIRFilter) {
	    //Check for init statement
	    if (!((SIRFilter)current).hasMethod("init"))
		 at.dms.util.Utils.fail("Filter must have an init statement.");
	}
	
	// rescale weights of splitter/joiner to match the splitjoin
	if (current instanceof SIRSplitJoin) {
	    ((SIRSplitJoin)current).rescale();
	} else if (current instanceof SIRFeedbackLoop) {
	    ((SIRFeedbackLoop)current).rescale();
	}
    }

    private boolean supportedType(String type) 
    {
	if (type.equals("Integer") ||
	    type.equals("Character") ||
	    type.equals("Boolean") ||
	    type.equals("Byte") ||
	    type.equals("Double") ||
	    type.equals("Float")||
	    type.equals("Short") ||
	    type.equals("String") ||
	    type.equals("Long")||
	    type.equals("Bit"))  
	    return true;
	else 
	    return false;
    }
	  
    //this method is called when getting a filter out of the symbol table
    //if the given filter is a builtin filter (FILE i/o) set what needs to be set
    private void setBuiltinArgs(SIROperator stream, JExpression[] args) 
    {
	//set the file name and the type for the SIRFileReader
	if (stream instanceof SIRFileReader) {
	    if (args.length != 2) 
		at.dms.util.Utils.fail(lineNumber + ": Exactly 2 args required for FileReader");
	    if (!(args[0] instanceof JStringLiteral))
		at.dms.util.Utils.fail(lineNumber + ": First argument to FileReader must be a string");
	    ((SIRFileReader)stream).setFileName(((JStringLiteral)args[0]).stringValue());
	    //the second arg will be turned into string by visitFieldExpression because it is a type
	    if (!(args[1] instanceof JStringLiteral))
		at.dms.util.Utils.fail(lineNumber + ": Second argument to FileReader must be a type");
	    ((SIRFileReader)stream).setInputType(CStdType.Void);
	    ((SIRFileReader)stream).setOutputType(getType(((JStringLiteral)args[1]).stringValue()));
	    return;
	}
	else if (stream instanceof SIRFileWriter) {
	    if (args.length != 2) 
		at.dms.util.Utils.fail(lineNumber + ": Exactly 2 args required for FileWriter");
	    if (!(args[0] instanceof JStringLiteral))
		at.dms.util.Utils.fail(lineNumber + ": First argument to FileWriter must be a string");
	    ((SIRFileWriter)stream).setFileName(((JStringLiteral)args[0]).stringValue());
	    //the second arg will be turned into string by visitFieldExpression because it is a type
	    if (!(args[1] instanceof JStringLiteral))
		at.dms.util.Utils.fail(lineNumber + ": Second argument to FileWriter must be a type");
	    ((SIRFileWriter)stream).setInputType(getType(((JStringLiteral)args[1]).stringValue()));
	    ((SIRFileWriter)stream).setOutputType(CStdType.Void);
	    return;
	}
	else if (stream instanceof SIRIdentity) {
	    if (args.length != 1)
		at.dms.util.Utils.fail(lineNumber + ": Exactly 1 arg required for Identity");
	    if (args[0] instanceof JStringLiteral)
		((SIRIdentity)stream).setType(getType(((JStringLiteral)args[0]).stringValue()));
	    else if (args[0] instanceof JMethodCallExpression)
		((SIRIdentity)stream).setType(((JMethodCallExpression)args[0]).getPrefix().getType());
            else if (args[0] instanceof JClassExpression)
                ((SIRIdentity)stream).setType(((JClassExpression)args[0]).getClassType());
	    else 
		Utils.fail(lineNumber + "Illegal arg to Identity");
	}
    }

    private JMethodDeclaration[] buildPortalMethodArray(JClassDeclaration portal, 
							JClassDeclaration clazz) 
    {
	JMethodDeclaration[] portalMethods = portal.getMethods();
	JMethodDeclaration[] clazzMethods = clazz.getMethods();
	JMethodDeclaration[] handlerMethods = new JMethodDeclaration[portalMethods.length];
	
	//For each method declared in the portal class
	//find the corresponding method in the given clazz
	//build the handlerMethods as we go
	for (int i = 0; i < portalMethods.length; i++) {
	    JMethodDeclaration handlerMethod = null;
	    for (int j = 0; j < clazzMethods.length; j++) {
		//Check if the signature of the current method equals the portal method
		//this may not work, I will try it!
		if (portalMethods[i].getMethod().equals(clazzMethods[j].getMethod())) 
		    handlerMethod = clazzMethods[j];
	    }
	    if (handlerMethod == null)
		at.dms.util.Utils.fail(printLine(portal) +
				       "Cannot find a method declaration for portal method " + 
				       portalMethods[i].getName() + " in class " + 
				       clazz.getSourceClass().getIdent());
	    handlerMethods[i] = handlerMethod;
	}
	return handlerMethods;
    }
    
		    
		    
	    
	    
    public Object visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
					     JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls) 
    {
	/* The current SIR Operator we are creating */
	SIROperator current;
	SIRStream oldParentStream = parentStream;
	
	blockStart("ClassDeclaration", self);
	printMe("Class Name: " + ident);
	printMe(self.getSourceClass().getSuperClass().getIdent());

	if (ident.endsWith("Portal"))
	    return null;

	boolean saveAnon=anonCreation;
	if (self.getSourceClass().getSuperClass().getIdent().equals(ident))
	    anonCreation = true;
	else
	    anonCreation = false;
	
	// create a new SIROperator
	current = newSIROP(self);

	/*
	  if this is not an anonymous creation add the SIR op to the 
	  "symbol table"
	  Adding it earlier to support not visit recursive def again
	*/

	if (!anonCreation) {
	    addVisitedOp(ident, current);
	}

	//If this class is public then it is the topLevel stream (entry point)
	if (self.getSourceClass().getSuperClass().getIdent().equals("StreamIt")) {
	    if (topLevel == null)
		topLevel = (SIRStream) current;
	    else
		at.dms.util.Utils.fail(printLine(self) +
				       "Toplevel Stream already set");	
	}
	if (current == null) 
	    printMe("Null");
	
	trash = visitClassBody(decls, fields, methods, body);

	if (current == null) 
	    printMe("Null");
	
	/* Perform any operations needed after the childern are visited */
	postVisit(current);

	// if the name of the class being declared is the same
	// as the name of the superclass then it is anonymous
	/*if (self.getSourceClass().getSuperClass().getIdent().equals(ident))
	  anonCreation = true;
	  else
	  anonCreation = false;*/

	parentStream = oldParentStream;
	printMe( "Out " + self.getSourceClass().getSuperClass().getIdent()
			    + num);
	anonCreation=saveAnon;
	return current;
    }

    public Object visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations)
    {
	/* The beginning of the visit! */
	
	//install a try to catch all runtime errors and report the line
	//number
	try {
	    blockStart("CompilationUnit", self);
	    //The top level declaration of the streamit class if it exists
	    JTypeDeclaration TopLevelDeclaration = null;

	    /*if (packageName.getName().length() > 0)
	      packageName.accept(this);*/
	    
	    /*for (int i = 0; i < importedPackages.length ; i++)
	      importedPackages[i].accept(this);*/
	    
	    /*  for (int i = 0; i < importedClasses.length ; i++)
		importedClasses[i].accept(this);*/


	    //check to see if this is the top level compilation unit
	    //by checking all of the class declarations
	    for (int i = 0; i < typeDeclarations.length ; i++) {
		if (typeDeclarations[i] instanceof JClassDeclaration) {
		    JClassDeclaration decl = (JClassDeclaration)
			typeDeclarations[i];
		    if (decl.getSourceClass().getSuperClass().
			getIdent().equals("StreamIt")) {
			if (TopLevelDeclaration != null)
			    at.dms.util.Utils.fail(printLine(decl) + 
						   "Top level stream already set.");
			TopLevelDeclaration = decl;
		    }
                    else if (decl.getSourceClass().getSuperClass().
                             getIdent().equals("Structure")) {
                        SIRStructure sirStruct;
                        sirStruct = (SIRStructure)decl.accept(this);
                        structureList.add(sirStruct);
                    }
		}
	    }
	    
	    //only visit the topleve declaration, visit other classes when used
	    if (TopLevelDeclaration != null)
		trash = TopLevelDeclaration.accept(this);
	    
	    
	    /* the end of the visit */
	}
	catch (Throwable t) {
	    System.err.println("Error at or around line " + lineNumber);
	    System.err.println("Stack Trace:\n");
	    t.printStackTrace();
	    System.exit(-1);
	}
			
	return topLevel;
    }

    
    public Object visitClassBody(JTypeDeclaration[] decls,
				 JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body) 
    {
	blockStart("ClassBody");
	for (int i = 0; i < decls.length ; i++)
            trash = decls[i].accept(this);
	for (int i = 0; i < body.length ; i++)
            trash = body[i].accept(this);
	
	//force the init method to be visited first!
	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals("init")) {
		methods[i].accept(this);
		break;
	    }
	}
	    


        for (int i = 0; i < methods.length ; i++) {
	    if (!(methods[i].getName().equals("init")))
		trash = methods[i].accept(this); 
	}
     	return null;
    }

     /**
   * visits a class declaration
   */
    public Object visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
					     JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods) 
    {
	printMe("visitInnerClassDeclaration");
        trash = visitClassBody(decls, fields, methods, body);
        return self;
    }
    

    //return the Ctype based on the string type
    private CType getType(String type) 
    {
	if (type.equals("Integer"))
	    return CStdType.Integer;
	else if (type.equals("Character"))
	    return CStdType.Char;
	else if (type.equals("Boolean"))
	    return CStdType.Boolean;
	else if (type.equals("Byte"))
	    return CStdType.Byte;
	else if (type.equals("Double"))
	    return CStdType.Double;
	else if (type.equals("Float"))
	    return CStdType.Float;
	else if (type.equals("Short"))
	    return CStdType.Short;
	else if (type.equals("String"))
	    return CStdType.String;	
	else if (type.equals("Long"))
	    return CStdType.Long;
	else if (type.equals("Bit"))
	    return CStdType.Bit;
	else
	    at.dms.util.Utils.fail("Non-Supported Type for Filter Input");
	return null;
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * visits a field declaration
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) 
    {
	printMe("FieldDeclaration " + ident);
	/* Input declaration set the fields for the current
	   stream */
	/* if the field is static and not final, it could be used
	   for message passing, so we flag an error */ 
	if (CModifier.contains(CModifier.ACC_STATIC, modifiers) &&
	    !CModifier.contains(CModifier.ACC_FINAL, modifiers))
	    at.dms.util.Utils.fail(printLine(self) +
				   "Cannot declare field " + ident +
				   " static (only final static).");
				   

	/* Output declaration set the fields for the current
	   stream */

	if (expr != null) {
	    Object o = expr.accept(this);
	    if (o instanceof JExpression) {
		expr = (JExpression)expr.accept(this);
	    } else {
		at.dms.util.Utils.fail(printLine(self) +
				       "Found a Channel declaration as a field -- new StreamIt syntax requires definition in init function instead.");
	    }
	}
	parentStream.addField(new JFieldDeclaration(null, new JVariableDefinition(null,
										  modifiers,
										  type,
										  ident,
										  expr),
						    null, null));
	return self;
    }


    private boolean ignoreMethodDeclaration(String ident) 
    {
	if (//ident.equals("work") ||
	    ident.equals("add") ||
	    //	    ident.startsWith("initPath") ||
	    ident.equals("initIO"))
	    return true;
	return false;
    }

    /**
     * visits a method declaration
     */
    public Object visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) 
    {
        blockStart("MethodDeclaration: " + ident, self);
	//ignore main method
	if (ident.equals("main") && CModifier.contains(CModifier.ACC_STATIC, modifiers)
		 && CModifier.contains(CModifier.ACC_PUBLIC, modifiers)) {
	    printMe("Main ignored");
	    return self;
	}

	LinkedList newList=new LinkedList();
	finalVars.add(newList);
	for(int i=0;i<parameters.length;i++) {
	    JFormalParameter param=parameters[i];
	    if(param.isFinal())
		newList.add(param);
	}
	JFormalParameter[] saveParams=params;
	String[] saveNames=paramNames;
	if(!anonCreation) {
	    params=parameters;
	    paramNames=new String[params.length];
	    for(int i=0;i<params.length;i++)
		paramNames[i]=CSourceClass.varName(params[i]);
	}
	for (int i = 0; i < parameters.length; i++) {
            trash = parameters[i].accept(this);
	}

        
	body = (JBlock)body.accept(this);
	
	if (exceptions.length == 0)
	    exceptions = CClassType.EMPTY;

	/*Install work functio*/
	if (ident.equals("work")) {
	    if (parentStream instanceof SIRFilter ||
                parentStream instanceof SIRPhasedFilter) {
		parentStream.setWork(new JMethodDeclaration(null,
                                                            modifiers,
                                                            returnType,
                                                            ident,
                                                            parameters,
                                                            exceptions,
                                                            body,
                                                            null,
                                                            null));
	    }
	    else
		at.dms.util.Utils.fail(printLine(self) +
				       "Work Function Declared for Non-Filter");
	}
	
	/*Install init function for filter*/
	else if (ident.equals("init") && (parentStream instanceof SIRStream)) {
	    ((SIRStream)parentStream).setInit(new JMethodDeclaration(null,
								     modifiers,
								     returnType,
								     ident,
								     parameters,
								     exceptions,
								     body,
								     null,
								     null)); 
	}
	else if (ident.startsWith("initPath")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail(printLine(self) +
				       "initPath declared for non-Feedbackloop");
	    ((SIRFeedbackLoop)parentStream).setInitPath(new JMethodDeclaration(null,
									       modifiers,
									       returnType,
									       ident,
									       parameters,
									       exceptions,
									       body,
									       null,
									       null)); 
	}            
	else if (!ignoreMethodDeclaration(ident) && (parentStream instanceof SIRStream))
	    ((SIRStream)parentStream).addMethod(new JMethodDeclaration(null,
								       modifiers,
								       returnType,
								       ident,
								       parameters,
								       exceptions,
								       body,
								       null,
								       null));

	params=saveParams;
	paramNames=saveNames;
	finalVars.removeLast();
	return self;
    }
    
    /**
     * visits a method declaration
     */
    public Object visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body) 
    {
        blockStart("ConstructorDeclaration", self);
	if (parameters.length == 0)
	    parameters = JFormalParameter.EMPTY;
	for (int i = 0; i < parameters.length; i++)
            trash = parameters[i].accept(this);
	trash = body.accept(this);
	if (exceptions.length == 0)
	    exceptions = CClassType.EMPTY;
	return self;
    }

    // ----------------------------------------------------------------------
    // STATEMENTS
    // ----------------------------------------------------------------------

    /**
     * visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) 
    {
        blockStart("WhileStatement", self);
        JExpression newCond = null;
	if (cond != null)
	    newCond = (JExpression)cond.accept(this);
        JStatement newBody = null;
	if (body != null)
	    newBody = (JStatement)body.accept(this);
	return new JWhileStatement(self.getTokenReference(),
				   newCond,
				   newBody,
				   self.getComments());
    }

    /**
   * visits a variable declaration statement
   */
    public Object visitVariableDeclarationStatement
        (JVariableDeclarationStatement self,
         JVariableDefinition[] vars) 
    {
        blockStart("VariableDeclarationStatement", self);
	for (int i = 0; i < vars.length; i++) {
            vars[i] = (JVariableDefinition)vars[i].accept(this);
	}
	for (int i = 0; i < vars.length; i++)
	    if (vars[i] != null)
		return new JVariableDeclarationStatement(self.getTokenReference(),
							 vars,
							 self.getComments());
	return null;
    }

    /**
     * visits a variable declaration statement.  Instead of creating a
     * new variable definition, we mutate the old one and return it so
     * that all of the local variable references that have been
     * resolved to this variable def. will still hold.
     */
    public Object visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) 
    {
        blockStart("VariableDefinition: " + self.getIdent(), self);
	printMe(type.toString());
	if(self.isFinal())
	    ((List)finalVars.getLast()).add(self);
	//The attribute returned from visiting the expression of the variable def
	Object retObj = null;
	//visit the expression
	if (expr != null)
	    retObj = expr.accept(this);

	//we are defining a new variable with a "new" in the code
	if (retObj instanceof JUnqualifiedInstanceCreation) {
	    //Portal creation (normal unqualified instance creation)
	    if (type.toString().endsWith("Portal")) {
		//Portal Definition , SIRCreatePortal add to symbol Table
		//make the SIRCreatePortal the new expression in the definition
		SIRCreatePortal cp = new SIRCreatePortal();
		// mutate and return self
		self.setExpression(cp);
		return self;
		//return new JVariableDefinition(null, modifiers, type, ident, cp);
	    }
            // Don't try to clone structures!
            else if (type.getCClass().getSuperClass().getIdent().equals("Structure"))
            {
                // Just discard the initialization; we don't care.
		self.setInitializer(null);
		return self;
                //return new JVariableDefinition(null, modifiers, type, ident, null);
            }
	    //defining a variable to be a named stream
	    //If the class of this variable is in the SIR table then
	    //we need to add this definition to the symbol table
	    else if (getVisitedOp(type.toString()) != null) {
		
		SIRStream ST = (SIRStream)ObjectDeepCloner.
		    deepCopy((SIRStream)getVisitedOp(type.toString()));
		printMe("Adding " + ident + " to symbol table");
		
		//If this a builtin filter set its args
		setBuiltinArgs(ST, ((JUnqualifiedInstanceCreation)retObj).
				     getParams());
		SIRInitStatement newSIRInit = 
		    new SIRInitStatement(Arrays.asList(((JUnqualifiedInstanceCreation)retObj).
					 getParams()),
					 ST);
		symbolTable.put(self, newSIRInit);
		return null;
		//return new JVariableDefinition(null, modifiers, type, ident, expr);
	    }
	    
	    else 
		at.dms.util.Utils.fail(printLine(self) +
				       "Definition of Stream " + type.toString() + 
				       " not seen before definition of " + ident);
	}
	// defining the var to be a anonymous class
	// visitUnqualifiedAnonymousCreation will return an SIRInitStatement
	else if (retObj instanceof SIRInitStatement) {
	    //just add the SIRInitStatement to the symbol table
	    printMe("Adding " + ident+ " to symbol table");
	    symbolTable.put(self, retObj);
	    return null;
	}

	self.setExpression((JExpression)retObj);
	return self;
	//return new JVariableDefinition(null, modifiers, type, ident, (JExpression)retObj);
    }
    
    /**
     * visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) 
    {
        blockStart("SwitchStatement", self);
	if (expr != null)
	    expr = (JExpression)expr.accept(this);
        for (int i = 0; i < body.length; i++)
            body[i] = (JSwitchGroup)body[i].accept(this);
        return new JSwitchStatement(self.getTokenReference(),
				    expr,
				    body,
				    self.getComments());
    }

    /**
     * visits a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
                                     JExpression expr) 
    {
        blockStart("Return", self);
        if (expr != null) 
	    expr = (JExpression)expr.accept(this);
        return new JReturnStatement(self.getTokenReference(),
				    expr,
				    self.getComments());
    }

    /**
     * visits a labeled statement
     */
    public Object visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) 
    {
        blockStart("LabeledStatement", self);
	if (stmt != null)
	    stmt = (JStatement)stmt.accept(this);
        return new JLabeledStatement(self.getTokenReference(),
				     label, stmt,
				     self.getComments());
    }

    /**
     * visits a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) 
    {
        blockStart("IfStatement", self);
	if (cond != null)
	    cond = (JExpression)cond.accept(this);
	if (thenClause != null)
	    thenClause = (JStatement)thenClause.accept(this);
        if (elseClause != null) 
	    elseClause = (JStatement)elseClause.accept(this);
        return new JIfStatement(self.getTokenReference(),
				cond, thenClause, elseClause,
				self.getComments());
    }

    /**
   * visits a for statement
   */
    public Object visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) 
    {
        blockStart("ForStatement", self);
	if (body != null) 
	    body = (JStatement)body.accept(this);
	else 
	    body = new JEmptyStatement(null, null);
	if (init != null)
	    init = (JStatement)init.accept(this);
	else 
	    init = new JEmptyStatement(null, null);
	if (cond != null)
	    cond = (JExpression)cond.accept(this);
	if (incr != null)
	    incr= (JStatement)incr.accept(this);	
	else 
	    incr = new JEmptyStatement(null, null);
	return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
   * visits a compound statement
   */
    public Object visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) 
    {
        blockStart("CompoundStatement", self);
        for (int i = 0; i < body.length; i++)
            body[i] = (JStatement)body[i].accept(this);
        return new JCompoundStatement(self.getTokenReference(),
				      body);
    }

    /**
     * visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) 
    {
        blockStart("ExpressionStatement", self);
	
	JPhylum attrib = null;
	if (expr != null)
	    attrib = (JPhylum)expr.accept(this);
	

	if (attrib instanceof JExpression)
	    return new JExpressionStatement(self.getTokenReference(),
					    (JExpression)attrib,
					    self.getComments());
	if (attrib == null)   //Used when we want to remove a statement
	    return null;
	else 
	    return attrib;
    }

    /**
     * visits an expression list statement
     */
    public Object visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) 
    {
        blockStart("ExpressionListStatement", self);
        for (int i = 0; i < expr.length; i++)
            expr[i] = (JExpression)expr[i].accept(this);
        return new JExpressionListStatement(self.getTokenReference(),
					    expr,
					    self.getComments());
    }

    /**
   * visits a empty statement
   */
    public Object visitEmptyStatement(JEmptyStatement self) 
    {
        blockStart("EmptyStatement", self);
        return self;
    }

    /**
   * visits a do statement
   */
    public Object visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) 
    {
        blockStart("DoStatement", self);
	if (cond != null) 
	    cond = (JExpression)cond.accept(this);
	if (body != null)
	    body = (JStatement)body.accept(this);
	return new JDoStatement(self.getTokenReference(),
				cond,
				body,
				self.getComments());
    }

    /**
     * visits a continue statement
     */
    public Object visitContinueStatement(JContinueStatement self,
                                       String label) 
    {
        blockStart("ContinueStatement", self);	
	return self;
    }

    /**
     * visits a break statement
     */
    public Object visitBreakStatement(JBreakStatement self,
                                    String label) 
    { 
        blockStart("BreakStatement", self);
	return self;
    }

    /**
     * visits an expression statement
     */
    public Object visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) 
    {
	JStatement[] body = self.getStatementArray();
	JTryCatchStatement removeFlag = new JTryCatchStatement(null, null,
							       null, null);
	
	blockStart("BlockStatement", self);
        for (int i = 0; i < body.length; i++) {
	    Object st = body[i].accept(this);
	    if (st == null)
	    	body[i] = removeFlag;
	    else if (st instanceof JStatement)
		body[i] = (JStatement)st;
	}
	
	//Vector nullVector = new Vector(1);
	//nullVector.add(null);
	LinkedList bodyList = new LinkedList(Arrays.asList(body));
	while (bodyList.remove(removeFlag));
	
	JStatement[] newBody = new JStatement[bodyList.size()];
	for (int i = 0; i< bodyList.size(); i++)
	    newBody[i] = (JStatement)bodyList.get(i);
	
	return new JBlock(self.getTokenReference(),
			  newBody,
			  comments);
	//return new JBlock (null, body, null);
	
    }

    /**
     * visits a type declaration statement
     */
    public Object visitTypeDeclarationStatement(JTypeDeclarationStatement self,
						JTypeDeclaration decl) 
    {
        blockStart("TypeDeclarationStatement", self);
        if (decl != null)
	    trash = decl.accept(this);
	return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr) 
    {
        blockStart("UnaryPlusExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr) 
    {
        blockStart("UnaryMinusExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr) 
    {
        blockStart("BitwiseComplementExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr) 
    {
        blockStart("LogicalComplementExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));   
	return self;
    }

    /**
     * visits a type name expression
     */
    public Object visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) 
    {
        blockStart("TypeNameExpression", self);
	return self;
    }

    /**
     * visits a this expression
     */
    public Object visitThisExpression(JThisExpression self,
                                    JExpression prefix) 
    {
        blockStart("ThisExpression", self);
	//never need to visit a this expression
	return self;
	/*if (prefix != null) {
	    prefix = (JExpression)prefix.accept(this);
	    }
	return new JThisExpression(null, prefix);
	*/
    }

    /**
     * visits a super expression
     */
    public Object visitSuperExpression(JSuperExpression self) 
    {
        blockStart("SuperExpression", self);
	return self;
    }

    /**
     * visits a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) 
    {
        blockStart("ShiftExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a shift expressiona
     */
    public Object visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) 
    {
        blockStart("RelationalExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a prefix expression
     */
    public Object visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) 
    {
        blockStart("PrefixExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) 
    {
        blockStart("PostfixExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) 
    {
        // Not parenthesized?
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitQualifiedAnonymousCreation
        (JQualifiedAnonymousCreation self,
         JExpression prefix,
         String ident,
         JExpression[] params,
         JClassDeclaration decl) 
    {
        blockStart("QualifiedAnonymousCreation", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	for (int i = 0; i < decl.fields.length; i++) {
	    decl = (JClassDeclaration)decl.fields[i].variable.accept(this); 
	}
	return new JQualifiedAnonymousCreation(self.getTokenReference(),
					       prefix, ident, params, decl);
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params) 
    {
        blockStart("QualifiedInstanceCreation", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	return new JQualifiedInstanceCreation(self.getTokenReference(),
					      prefix, ident, params);
    }
    
    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public Object visitUnqualifiedAnonymousCreation
        (JUnqualifiedAnonymousCreation self,
         CClassType type,
         JExpression[] params,
         JClassDeclaration decl) 
    {
        blockStart("UnqualifiedAnonymousCreation", self);
	//Get the SIR Operator back from the Class Declaration
	SIROperator SIROp = (SIROperator)decl.accept(this);
	if (!(SIROp instanceof SIRStream))
	    at.dms.util.Utils.fail(printLine(self) +
				   "Trying to anonymously create non-Stream");
     	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	if (params.length == 0)
	    trash = params = JExpression.EMPTY;
	      /*	
			for (int i = 0; i < decl.fields.length; i++) {
			trash = decl.fields[i].variable.accept(this); 
			}
	      */
	//The only time that we can use an Anonymous class is inside an add statement 
	//which needs to be translated into an SIRInitStatement
	SIRInitStatement sis = new SIRInitStatement(Arrays.asList(params), (SIRStream)SIROp);
	return sis;
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public Object visitUnqualifiedInstanceCreation
        (JUnqualifiedInstanceCreation self,
         CClassType type,
         JExpression[] params) 
    {
        blockStart("UnqualifiedInstanceCreation", self);
	printMe("  Declaring: " + type.getIdent());
	if (params.length == 0)
	    params = JExpression.EMPTY;
	if (type.getIdent().equals("Channel")) 
	    /*Channel declaration, treat the args as special */
	{
	    Vector v = new Vector(3);
            v.add(params[0].accept(this));
	    v.add(params[1].accept(this));
	    if (params.length > 2) { 
		v.add(params[2].accept(this));
	    }
	    return v;
	}
	else {     /* Not a channel */
	    for (int i = 0; i < params.length; i++) 
		params[i] = (JExpression)params[i].accept(this);
	}
	return new JUnqualifiedInstanceCreation(self.getTokenReference(), type, params);
    }

    /**
     * visits an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
					  CType type,
					  JExpression[] dims,
					  JArrayInitializer init) 
    {
        blockStart("NewArrayExpression", self);
	if (dims.length == 0)
	    dims = JExpression.EMPTY;
	for (int i = 0; i < dims.length; i++) {
	    if(dims[i]!=null)
		dims[i] = (JExpression)dims[i].accept(this);
	}
	return new JNewArrayExpression(self.getTokenReference(), type, dims, init);
    }

    /**
     * visits a name expression
     */
    public Object visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) 
    {
        blockStart("NameExpression", self);
	if (prefix != null)
	    self.setPrefix((JExpression)prefix.accept(this));
	return self;
    }

    /**
     * visits an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) 
    {
	blockStart("BinaryExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }


    private SIRMessageStatement createMessageStatement(JMethodCallExpression methCall,
						       CType portal, 
						       JExpression prefix, 
						       JExpression[] args)  
    {
	//index of the method in the portal interface
	int index = -1;
	//Process the args
	for (int i = 0; i < args.length; i++)
	    args[i] = (JExpression) args[i].accept(this);

	//Find the index of the method call
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail(printLine(methCall) +
				   "A portal can only implement one interface at this time");
	
	//Get the portal methods of the portal Interface
        // Save this code; we're not using it now, but it might be useful
        // for finding the right interface, if/when multi-interface
        // portals happen.
        /*
	CMethod[] portalMethods = interfaces[0].getCClass().getMethods();

	for (int i = 0; i < portalMethods.length; i++) {
	    if (portalMethods[i].getIdent().equals(methCall.getIdent())) {
		index = i;
		break;
	    }
	}
	if (index == -1)  //cannot find method
	    at.dms.util.Utils.fail("Cannot find portal method " + methCall.getIdent() + " in portal interface");
        */
        String interfaceName = interfaces[0].getIdent();
        
	return new SIRMessageStatement(prefix, interfaceName,
                                       methCall.getIdent(),
				       args, nextLatency);
    }
		

    //given a method call, a JLocalVariableExpression portal, and a stream, build the 
    //SIRRegReceiveStatement
    private SIRRegReceiverStatement createRegReceiver(JMethodCallExpression methCall,
						      CType portal,
						      JExpression prefix,
						      SIRStream st) 
    {
	CMethod meth = methCall.getMethod();
	//the index of the method in the interface
	
	//Extract the interface from the portal prefix variable
	CClassType[] interfaces = 
	    portal.getCClass().getInterfaces();
	
	if (interfaces.length > 1)
	    at.dms.util.Utils.fail(printLine(methCall) +
				   "A portal can only implement one interface at this time");
	
	//Get the portal methods of the portal Interface
	CMethod[] portalMethods = interfaces[0].getCClass().getMethods();
	
	//get the CMethods from the stream class
	JMethodDeclaration[] streamMethodDecs = st.getMethods();
	
	//Store the methods in the order of the interface
	JMethodDeclaration[] matchedMethods = new JMethodDeclaration[portalMethods.length];
	
	//Build matchedMethods by iterating over all the methods in the portal interface
	//and matching them with the methods of the stream class
	for (int i = 0; i < portalMethods.length; i++) {
	    JMethodDeclaration currentMethod = null;
	    for (int j = 0; j < streamMethodDecs.length; j++) {
		if (portalMethods[i].getIdent().equals(streamMethodDecs[j].getName())) {
		    currentMethod = streamMethodDecs[j];
		    break;
		}
	    }
	    if (currentMethod == null)
		at.dms.util.Utils.fail(printLine(methCall) +
				       "Cannot find portal method in portal interface (RegReceiver)");
	    matchedMethods[i] = currentMethod;
	}
	
	// Keep track of the list of methods for this interface with a 
	// interface table, to be passed on with a callback to Kopi2SIR
        SIRInterfaceTable itable =
            new SIRInterfaceTable(null, 
                                  interfaces[0], 
                                  (JMethodDeclaration[])
                                  matchedMethods.clone());
	interfaceTableList.add(itable);
	return new SIRRegReceiverStatement(prefix,st, itable);
    }

		      


    //This method creates an initStatement from the underlying arguments
    //to the function should be translated into an init statment
    //also, based on the String argument, it registers the SIROp
    //with the Stream
    private SIRInitStatement createInitStatement(Object SIROp, String regMethod) 
    {
	SIRStream newST = null;
	/*
	  //Already an SIRinit statement
	  if (SIROp instanceof SIRInitStatement) {
	  return (SIRInitStatement)SIROp;
	  }
	  //Using a local variable that has been already defined as a stream construct
	  if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;
	*/


	// Already an SIRinit statement (this is a local variable expression or a anonymous
	// creation)
	// we use the init statement from the symbol table (for a local var) or created it in the 
	// or the one created in the AnonymousCreation
	if (SIROp instanceof SIRInitStatement) {
	    //now we must set the parent and do any other steps that are necessary 
	    // when registering
	    newST = (SIRStream)((SIRInitStatement)SIROp).getTarget();
	    newST.setParent((SIRContainer)parentStream);
	}

	if (SIROp instanceof SIRStream)
	    newST = (SIRStream)SIROp;

	//Creating a named class
	//if it is a named creation, lookup in the symbol table for the visited
	//node and add it to the pipeline
	if (SIROp instanceof JUnqualifiedInstanceCreation) {
	    SIRStream st = (SIRStream)getVisitedOp(((JUnqualifiedInstanceCreation)SIROp).
						   getType().getCClass().getIdent());
	    
	    //if we cannot find the stream in the symbol table then 
	    //we have not seen the declaration before the use
	    if (st==null)
		at.dms.util.Utils.fail(lineNumber + 
				       ": cannot find declaration of stream " +
				       ((JUnqualifiedInstanceCreation)SIROp).
				       getType().getCClass().getIdent());

	    // don't clone recursive stubs
	    newST = (SIRStream) ObjectDeepCloner.deepCopy(st);
	    //if this is a builtin filter, set the args
	    setBuiltinArgs(newST, ((JUnqualifiedInstanceCreation)SIROp).getParams());
	    newST.setParent((SIRContainer)parentStream);
	}
	//Die if it is none of the above case
	if (newST == null)
	    at.dms.util.Utils.fail(
				   "Illegal Arg to Stream Add Construct");
	
	// now find the registration method
	if (regMethod.equals("add")) {
	    if (parentStream instanceof SIRPipeline){
		((SIRPipeline)parentStream).add(newST);
	    }
	    else {
		((SIRSplitJoin)parentStream).add(newST);
	    }
	}
	else if (regMethod.equals("setBody"))
	    ((SIRFeedbackLoop)parentStream).setBody(newST);
	else if (regMethod.equals("setLoop"))
	    ((SIRFeedbackLoop)parentStream).setLoop(newST);
	else
	    at.dms.util.Utils.fail(lineNumber + ": Invalid Registration Method");

	//Already an SIRinit statement (this is a local variable expression)
	//we used the init statement from the symbol table
	//Using a local variable that has been already defined as a stream construct
	if (SIROp instanceof SIRInitStatement) 
	    	    return (SIRInitStatement)SIROp;
	else {
	    if (newST instanceof SIRIdentity) 
		return new SIRInitStatement(Arrays.asList(JExpression.EMPTY),
					    newST);
	    else
		return new SIRInitStatement(Arrays.asList(((JUnqualifiedInstanceCreation)SIROp).getParams()),
					    newST);
	}
    }
    
    /**
     * visits a method call expression
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) 
    {
	//Save method we were processing
	String parentMethod = currentMethod;
	blockStart("MethodCallExpression: " + self.getIdent(), self);
        //Set currentMethod to this method, we are processing it!
	currentMethod = self.getIdent();
	
	
	if (isSIRExp(self)) {
	    printMe("SIR Expression " + ident);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    return newSIRExp(self, args);
	}
	else if (ident.equals("regReceiver")) { 
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to add() allowed");
	    SIRStream st = ((SIRInitStatement)args[0].accept(this)).getTarget();
	    currentMethod = parentMethod;
	    //prefix can either be a field or a local var, extract type
	    if (prefix instanceof JLocalVariableExpression)
		return createRegReceiver(self, ((JLocalVariableExpression)prefix).getVariable().getType(),
					 prefix, st);
	    else if (prefix instanceof JFieldAccessExpression)
		return createRegReceiver(self, ((JFieldAccessExpression)prefix).getType(),
					 prefix, st);
	    else
		at.dms.util.Utils.fail(printLine(self) + 
				       "regReceiver all must have a portal prefix");
	    return null;
	}
        else if (ident.equals("setAnyLatency")) {
            nextLatency = SIRLatency.BEST_EFFORT;
            return null;
        }
        else if (ident.equals("setMaxLatency")) {
            if (args.length > 1)
                at.dms.util.Utils.fail(printLine(self) +
                                       "Exactly one arg to setMaxLatency() allowed");
            nextLatency = new SIRLatencyMax(args[0].intValue());
            return null;
        }
        else if (ident.equals("setLatency")) {
            if (args.length > 2)
                at.dms.util.Utils.fail(printLine(self) +
                                       "Exactly two args to setLatency() allowed");
            nextLatency = new SIRLatencyRange(args[0].intValue(),
                                              args[1].intValue());
            return null;
        }
	else if (ident.equals("add")) {            //Handle an add call in a pipeline
	    //Parent must be a pipeline
	    if (!((parentStream instanceof SIRPipeline) || parentStream instanceof SIRSplitJoin)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "Add not called on Pipeline or SplitJoin");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to add() allowed");
	    //Visit the argument (Exactly one))
	    Object SIROp = args[0].accept(this);
	    	    
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement 
	    return createInitStatement(SIROp, ident);
	    
	} else if (ident.equals("setDelay")) {
	    if (!(parentStream instanceof SIRFeedbackLoop))
		at.dms.util.Utils.fail(printLine(self) + 
				       "SetDelay called on Non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Too many args to setDelay");
	    JExpression delay  = (JExpression)args[0].accept(this);
	    ((SIRFeedbackLoop)parentStream).setDelay(delay);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //we want to ignore remove this method from the block
	    return null;
	}
	else if (ident.equals("setBody")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "setBody called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to setBody() allowed");
	    //Visited the argument
	    Object SIROp = args[0].accept(this);
	    //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	}
	else if (ident.equals("setLoop")) {
	    if (!(parentStream instanceof SIRFeedbackLoop)) 
		at.dms.util.Utils.fail(printLine(self) + 
				       "setLoop called on non-FeedbackLoop");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Exactly one arg to setLoop() allowed");
	    //Visited the argument
	    Object SIROp = args[0].accept(this);
	     //reset currentMethod on all returns
	    currentMethod = parentMethod;
	    //create the init statement to return
	    return createInitStatement(SIROp, ident);
	}
       
	else if (ident.equals("setSplitter")) {
	    //we build the splitter here and set the global splitType
	    //then we set the splitter in the postVisit of the stream
	    buildSplitter(args[0]);
	    currentMethod = parentMethod;
	    return null;
	}
	else if (ident.equals("setJoiner")) {
	    //we build the joiner here and set the global joinType
	    //then we set the joiner in the postVisit of the stream
	    buildJoiner(args[0]);
	    currentMethod = parentMethod;
	    //we want to ignore remove this method from the block
	     return null;
	}
        else if (ident.equals("setIOTypes")) {
            // Phased filter syntax for setting I/O types.  This has
            // two arguments, which are null or Class objects.
            args[0] = (JExpression)args[0].accept(this);
            args[1] = (JExpression)args[1].accept(this);
            if (parentStream instanceof SIRFilter)
            {
                SIRFilter filter = (SIRFilter)parentStream;
                if (args[0] instanceof JClassExpression)
                    filter.setInputType(((JClassExpression)args[0]).getClassType());
                else if (args[0] instanceof JStringLiteral)
                    filter.setInputType(getType(((JStringLiteral)args[0]).stringValue()));
		else 
		    Utils.fail(printLine(self) + "Malformed input type on I/O declaration");

                if (args[1] instanceof JClassExpression)
                    filter.setOutputType(((JClassExpression)args[1]).getClassType());
                else if (args[1] instanceof JStringLiteral)
                    filter.setOutputType(getType(((JStringLiteral)args[1]).stringValue()));
		else 
		    Utils.fail(printLine(self) + "Malformed output type on I/O declaration");
            }
            else if (parentStream instanceof SIRPhasedFilter)
            {
                SIRPhasedFilter filter = (SIRPhasedFilter)parentStream;
                if (args[0] instanceof JClassExpression)
                    filter.setInputType(((JClassExpression)args[0]).getClassType());
                else if (args[0] instanceof JStringLiteral)
                    filter.setInputType(getType(((JStringLiteral)args[0]).stringValue()));
		else 
		    Utils.fail(printLine(self) + "Malformed input type on I/O declaration");

                if (args[1] instanceof JClassExpression)
                    filter.setOutputType(((JClassExpression)args[1]).getClassType());
                else if (args[1] instanceof JStringLiteral)
                    filter.setOutputType(getType(((JStringLiteral)args[1]).stringValue()));
		else 
		    Utils.fail(printLine(self) + "Malformed output type on I/O declaration");
            }
            else
                at.dms.util.Utils.fail(printLine(self) +
                                       "setIOTypes() outside a filter");
            return null;
        }
        else if (ident.equals("phase")) {
            // This is special.  There should be one parameter, which
            // is an anonymous stream creator; we need to get information
            // out of this.
            if (!(parentStream instanceof SIRPhasedFilter))
                at.dms.util.Utils.fail(printLine(self) +
                                       "Phase creation outside a phased filter");
	    if (args.length > 1)
		at.dms.util.Utils.fail(printLine(self) + 
				       "Too many args to phase");
            JExpression expr = args[0];
            if (expr instanceof JUnaryPromote)
                expr = ((JUnaryPromote)expr).getExpr();
            // This could go inside a try...catch (ClassCastException) block.
            // This appears to be the right type.  cf. JLS 15.9
            JUnqualifiedAnonymousCreation uac =
                (JUnqualifiedAnonymousCreation)(expr);
            // uac should have three parameters; these are the peek, pop,
            // and push rates, respectively.
            JExpression[] pparams = uac.getParams();
            if (pparams.length != 3)
                at.dms.util.Utils.fail(printLine(self) +
                                       "WorkFunction constructor needs exactly 3 params");
            JExpression peek = pparams[0];
            JExpression pop = pparams[1];
            JExpression push = pparams[2];
            
            // Also, we need to look inside the class body to find the
            // function being called:
            JClassDeclaration decl = uac.getDecl();
            JMethodDeclaration[] methods = decl.getMethods();
            if (methods.length != 1)
                at.dms.util.Utils.fail(printLine(self) +
                                       "WorkFunction body can only have one function");
            if (!(methods[0].getName().equals("work")))
                at.dms.util.Utils.fail(printLine(self) +
                                       "WorkFunction must contain work function");
            // pass on validating other attributes of work()
            JBlock body = methods[0].getBody();
            if (body.size() != 1)
                at.dms.util.Utils.fail(printLine(self) +
                                       "WorkFunction work function must have exactly one statement");
            JExpressionStatement stmt =
                (JExpressionStatement)body.getStatement(0);
            JMethodCallExpression fc =
                (JMethodCallExpression)stmt.getExpression();
            // At this point, we've won; we kind of want to search ourself
            // to find the right function to turn into a phase, but we
            // can do that later.
            return new SIRPhaseInvocation(self.getTokenReference(),
                                          fc, peek, pop, push, null);
        }
	//There are two cases for portal calls
	//Message send to a portal using a local var to store the portal
	//I think that this is the only case when prefix will not be null??? Keep the check any way
	else if (prefix instanceof JLocalVariableExpression &&
		 ((JLocalVariableExpression)prefix).getVariable().getType().toString().endsWith("Portal")) {
	    //check if this is a regSender, if so ignore it
	    if (ident.equals("regSender"))
		return null;
	    return createMessageStatement(self, ((JLocalVariableExpression)prefix).getVariable().getType(), 
					  prefix, args);
	}
	//message send to portal using field as portal
	else if (prefix instanceof JFieldAccessExpression &&
		 ((JFieldAccessExpression)prefix).getType().toString().endsWith("Portal")) {
	    if (ident.equals("regSender"))
		return null;
	    return createMessageStatement(self, ((JFieldAccessExpression)prefix).getType(), prefix, args);
	}	    
      	else {             //Not an SIR call
	    prefix = (JExpression)prefix.accept(this);
	    for (int i = 0; i < args.length; i++)
		args[i] = (JExpression) args[i].accept(this);
	    //reset currentMethod
	    currentMethod = parentMethod;
	    self.setArgs(args);
	    self.setPrefix(prefix);
	    return self;
	}

    }
    

    private void buildSplitter(JExpression type) 
    {
	
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail(printLine(type) + 
				   "arg to setSplitter must be a method call");

	JMethodCallExpression splitterType = (JMethodCallExpression)type;
	JExpression[] args=splitterType.getArgs();
	JExpression[] newArgs=new JExpression[args.length];
	for(int i=0;i<args.length;i++)
	    newArgs[i]=(JExpression)args[i].accept(this);
	splitterType.setArgs(newArgs);
	    
	SIRSplitter splitter = null;

	//build a temporary splitter just to get the type from later
	//after we have visited the entire stream, we will call setSplitter again
	//with the correct number of splitting streams ( in postVisit() )
	if (splitterType.getIdent().equals("ROUND_ROBIN")) {
	    // here we're supporting the syntax where
	    // round_robin(<weight>) will create a uniform weighted
	    // round robin that is extended across the width of the
	    // stream
	    JExpression[] weightArgs = splitterType.getArgs();
	    JExpression weight = (weightArgs.length>0 ? weightArgs[0] : new JIntLiteral(1));
	    splitter = SIRSplitter.createUniformRR((SIRContainer)parentStream, weight);
	} else if (splitterType.getIdent().equals("DUPLICATE"))
	    splitter = SIRSplitter.create((SIRContainer)parentStream, SIRSplitType.DUPLICATE, 2);
	else if (splitterType.getIdent().equals("NULL"))
	    splitter = SIRSplitter.create((SIRContainer)parentStream, SIRSplitType.NULL, 2);
	else if (splitterType.getIdent().equals("WEIGHTED_ROUND_ROBIN"))
	    splitter = SIRSplitter.createWeightedRR((SIRContainer)parentStream, 
						    splitterType.getArgs());
	else
	    at.dms.util.Utils.fail(printLine(type) + 
				   "Unsupported Split Type");

	if (parentStream instanceof SIRSplitJoin)
	    ((SIRSplitJoin)parentStream).setSplitter(splitter);
	if (parentStream instanceof SIRFeedbackLoop)
	    ((SIRFeedbackLoop)parentStream).setSplitter(splitter);
    }

    private void buildJoiner(JExpression type) 
    {
	if (!(type instanceof JMethodCallExpression))
	    at.dms.util.Utils.fail(printLine(type) + 
				   "arg to setJoiner must be a method call");

	JMethodCallExpression joiner = (JMethodCallExpression)type;
	JExpression[] args=joiner.getArgs();
	JExpression[] newArgs=new JExpression[args.length];
	for(int i=0;i<args.length;i++)
	    newArgs[i]=(JExpression)args[i].accept(this);
	joiner.setArgs(newArgs);
	SIRJoiner joinType = null;

	if (joiner.getIdent().equals("ROUND_ROBIN")) {
	    // here we're supporting the syntax where
	    // round_robin(<weight>) will create a uniform weighted
	    // round robin that is extended across the width of the
	    // stream
	    JExpression[] weightArgs = joiner.getArgs();
	    JExpression weight = (weightArgs.length>0 ? weightArgs[0] : new JIntLiteral(1));
	    joinType = SIRJoiner.createUniformRR((SIRContainer)parentStream, weight);
	} else if (joiner.getIdent().equals("COMBINE"))
	    joinType = SIRJoiner.create((SIRContainer)parentStream, SIRJoinType.COMBINE, 2);	  	
	else if (joiner.getIdent().equals("NULL"))
	    joinType = SIRJoiner.create((SIRContainer)parentStream, SIRJoinType.NULL, 2);
	else if (joiner.getIdent().equals("WEIGHTED_ROUND_ROBIN"))
	    joinType = SIRJoiner.createWeightedRR((SIRContainer)parentStream, joiner.getArgs());
	else 
	    at.dms.util.Utils.fail(printLine(type) + 
				   "Unsupported Join Type");

	if (parentStream instanceof SIRSplitJoin)
	    ((SIRSplitJoin)parentStream).setJoiner(joinType);
	if (parentStream instanceof SIRFeedbackLoop)
	    ((SIRFeedbackLoop)parentStream).setJoiner(joinType);
	
    }
    
    /**
     * visits a local variable expression
     */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) 
    {
        blockStart("LocalVariableExpression", self);
	//Lookup the current local variable in the symbol table
	//if it exists then this is a variable of a streamit type
	if (symbolTable.get(self.getVariable()) != null) {
	    printMe("Using symbol table entry for " + ident);
	    return symbolTable.get(self.getVariable());
	}
	return self;
    }

  
    /**
     * visits an equality expression
     */
    public Object visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) 
    {
        blockStart("EqualityExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) 
    {
        blockStart("ConditionalExpression", self);
	if (cond != null)
	    self.setCond((JExpression)cond.accept(this));
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a compound expression
     */
    public Object visitCompoundAssignmentExpression
        (JCompoundAssignmentExpression self,
         int oper,
         JExpression left,
         JExpression right) 
    {
        blockStart("CompoundAssignmentExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits a field expression
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident) 
    {
        blockStart("FieldExpression", self);
	//return a string if this is a field expression that accesses the type
	//argument in a channel instaniation or SIRFile* new expression
	
	if(anonCreation) {
	    for(int i=0;i<params.length;i++) {
		if(self.ident.equals(paramNames[i])) {
		    return new JLocalVariableExpression(params[i].getTokenReference(),params[i]);
		}
	    }
	    if(ident.equals("var$numbflies")) {
		System.err.println("Trying to match var$numbflies");
		for(int i=finalVars.size()-1;i>=0;i--) {
		    System.err.println("Recursing...");
		    List vars=(List)finalVars.get(i);
		    for(int j=0;j<vars.size();j++) {
			JLocalVariable var=(JLocalVariable)vars.get(j);
			System.err.println("Trying:"+var);
			if(CSourceClass.varName(var).equals(ident)) {
			    System.err.println("Success!");
			    return new JLocalVariableExpression(var.getTokenReference(),var);
			}
		    }
		}
	    }
	    
	}
	if (supportedType(left.getType().getCClass().getIdent())) {
	    return new JStringLiteral(self.getTokenReference(), left.getType().getCClass().getIdent());
	}
	//Never need to visit a field expression
	return self;
    }

    /**
   * visits a class expression
   */
    public Object visitClassExpression(JClassExpression self,
                                     CType type) 
    {
        blockStart("ClassExpression", self);
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type) 
    {
        blockStart("CastExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type) 
    {
        blockStart("UnaryPromoteExpression", self);
	if (expr != null)
	    self.setExpr((JExpression)expr.accept(this));
	return self;
    }

    /**
     * visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) 
    {
        blockStart("BitwiseExpression", self);
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }
    
    /**
     * visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) 
    {
        blockStart("AssignmentExpression", self);
	
	//must check for an assignment to input or output inside of
	//an init statement...
	if (left instanceof JFieldAccessExpression) {
	    if (((JFieldAccessExpression)left).getIdent().equals("input")) {
		if (!(parentStream instanceof SIRFilter))
		    at.dms.util.Utils.fail(printLine(self) + 
					   "Input declaration on non-Filter");
		SIRFilter filter = (SIRFilter)parentStream;
		Vector v = (Vector)right.accept(this);
		//set the input type of the filter
                if (v.elementAt(0) instanceof JClassExpression)
                    filter.setInputType(((JClassExpression)v.elementAt(0)).getClassType());
                else if (v.elementAt(0) instanceof JStringLiteral)
                    filter.setInputType(getType(((JStringLiteral)v.elementAt(0)).stringValue()));
		else if (v.elementAt(0) instanceof JMethodCallExpression) {
		    //defining an array to be passed over the tape...
		    JMethodCallExpression methCall = (JMethodCallExpression)v.elementAt(0);
		    if (methCall.getIdent().equals("getClass")) {
			//array, record the dimension
			if (methCall.getPrefix().getType() instanceof CArrayType)
			    ((CArrayType)methCall.getPrefix().getType()).
				setDims(((JNewArrayExpression)methCall.getPrefix()).getDims());
			filter.setInputType(methCall.getPrefix().getType());
		    }
		}
		else 
		    Utils.fail(printLine(self) + "Malformed type on input declaration");

		filter.setPop((JExpression)v.elementAt(1));
		//If a peek value is given, and it is greater than pops
		//set the peek
		if (v.size() > 2) {
		    filter.setPeek((JExpression)v.elementAt(2));
		}
		else  //Otherwise set the peeks to the number of pops
		    filter.setPeek((JExpression)v.elementAt(1));
		return null;
	    }
       	    else if (((JFieldAccessExpression)left).getIdent().equals("output")) {
		if (!(parentStream instanceof SIRFilter))
		    at.dms.util.Utils.fail(printLine(self) + 
					   "Output declaration on non-Filter");
		SIRFilter filter = (SIRFilter)parentStream;
		Vector v = (Vector)right.accept(this);
		filter.setPush((JExpression)v.elementAt(1));
                if (v.elementAt(0) instanceof JClassExpression)
                    filter.setOutputType(((JClassExpression)v.elementAt(0)).getClassType());
                else if (v.elementAt(0) instanceof JStringLiteral) {
		    filter.setOutputType(getType(((JStringLiteral)v.elementAt(0)).stringValue()));
		}
		else if (v.elementAt(0) instanceof JMethodCallExpression) {
		    //defining an array to be passed over the tape...
		    JMethodCallExpression methCall = (JMethodCallExpression)v.elementAt(0);
		    if (methCall.getIdent().equals("getClass")) {
			//array, record the dimension
			if (methCall.getPrefix().getType() instanceof CArrayType)
			    ((CArrayType)methCall.getPrefix().getType()).
				setDims(((JNewArrayExpression)methCall.getPrefix()).getDims());
			filter.setOutputType(methCall.getPrefix().getType());
		    }
		}
		else 
		    Utils.fail(printLine(self) + "Malformed type on output declaration");
		return null;
	    }

	}
	    
	if (left != null)
	    self.setLeft((JExpression)left.accept(this));
	if (right != null)
	    self.setRight((JExpression)right.accept(this));
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) 
    {
        blockStart("ArrayLengthExpression", self);
	if (prefix != null)
	    prefix = (JExpression)prefix.accept(this);
        return new JArrayLengthExpression(self.getTokenReference(), prefix);
    }

    /**
     * visits an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) 
    {
        blockStart("ArrayAccessExpression", self);
	if (prefix != null)
	    self.setPrefix((JExpression)prefix.accept(this));
	if (accessor != null)
	    self.setAccessor((JExpression)accessor.accept(this));
	return self;
    }

  
    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    public Object visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) 
    {
        blockStart("SwitchLabel", self);
	if (expr != null)
	    expr = (JExpression)expr.accept(this);
        return new JSwitchLabel(self.getTokenReference(), expr);
    }

    /**
     * visits an array length expression
     */
    public Object visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) 
    {
        blockStart("SwitchGroup", self);
	for (int i = 0; i < labels.length; i++)
            labels[i] = (JSwitchLabel)labels[i].accept(this);
	for (int i = 0; i < stmts.length; i++)
            stmts[i] = (JStatement)stmts[i].accept(this);
	return new JSwitchGroup(self.getTokenReference(), labels, stmts);
    }

  
    /**
     * visits an array length expression
     */
    public Object visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) 
    {
        blockStart("FormalParameter", self);
	return self;
    }

    /**
     * visits an array length expression
     */
    public Object visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params) 
    {
        blockStart("ConstructorCall", self);
	if (params.length == 0)
	    params = JExpression.EMPTY;
	for (int i = 0; i < params.length; i++)
            params[i] = (JExpression)params[i].accept(this);
	return new JConstructorCall(self.getTokenReference(),
				    functorIsThis, params);
    }

    /**
     * visits an array initializer expression
     */
    public Object visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems) 
    {
        blockStart("ArrayInitializer", self);
        for (int i = 0; i < elems.length; i++)
            elems[i] = (JExpression)elems[i].accept(this);
        return new JArrayInitializer(self.getTokenReference(), elems);
    }

    /**
     * visits a boolean literal
     */
  public Object visitBooleanLiteral(JBooleanLiteral self,
			     boolean value) 
    {
        blockStart("BooleanLiteral", self);
	return new JBooleanLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a byte literal
     */
  public Object visitByteLiteral(JByteLiteral self,
			  byte value) 
    {
        blockStart("ByteLiteral", self);
	return new JByteLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a character literal
     */
  public Object visitCharLiteral(JCharLiteral self,
			  char value) 
    {
        blockStart("CharLiteral", self);
	return new JCharLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a double literal
     */
  public Object visitDoubleLiteral(JDoubleLiteral self,
			    double value) 
    {
        blockStart("DoubleLiteral", self);
	return new JDoubleLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a float literal
     */
  public Object visitFloatLiteral(JFloatLiteral self,
			   float value) 
    {
        blockStart("FloatLiteral", self);
	return new JFloatLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a int literal
     */
  public Object visitIntLiteral(JIntLiteral self,
			 int value) 
    {
        blockStart("IntLiteral", self);
	return new JIntLiteral(self.getTokenReference(),value);
    }

    /**
     * visits a long literal
     */
  public Object visitLongLiteral(JLongLiteral self,
			  long value) 
    {
        blockStart("LongLiteral", self);
	return new JLongLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a short literal
     */
  public Object visitShortLiteral(JShortLiteral self,
			   short value) 
    {
        blockStart("ShortLiteral", self);
	return new JShortLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a string literal
     */
  public Object visitStringLiteral(JStringLiteral self,
			    String value) 
    {
        blockStart("StringLiteral", self);
	return new JStringLiteral(self.getTokenReference(), value);
    }

    /**
     * visits a null literal
     */
  public Object visitNullLiteral(JNullLiteral self) 
    {
        blockStart("NullLiteral", self);
	return new JNullLiteral(self.getTokenReference());
    }

    /*-------------------------------------------------------------------------
      UNUSED (IN STREAMIT) VISITORS 
      -------------------------------------------------------------------------*/

    /**
     * visits a package name declaration
     */
    public Object visitPackageName(String name) 
    {
        blockStart("PackageName");
	return null;
    }

    /**
     * visits a package import declaration
     */
    public Object visitPackageImport(String name) 
    {
        blockStart("PackageImport");
	return null;
    }

    /**
     * visits a class import declaration
     */
    public Object visitClassImport(String name) 
    {
        blockStart("ClassImport");
	return null;
    }

    /**
     * Returns a vector of all the JInterfaceDeclarations for the
     * toplevel stream
     */
    public JInterfaceDeclaration[] getInterfaces()  {
	JInterfaceDeclaration[] ret = (JInterfaceDeclaration[])interfaceList.toArray(new JInterfaceDeclaration[0]);
	return ret;
    }
    
    /**
     * Returns a vector of all the SIRInterfaceTables that were
     * constructed in traversing the toplevel stream
     */
    public SIRInterfaceTable[] getInterfaceTables()  {
	SIRInterfaceTable[] ret = (SIRInterfaceTable[])interfaceTableList.toArray(new SIRInterfaceTable[0]);
	return ret;
    }

    /**
     * Returns a vector of all the SIRStructures that appeared in the
     * program
     */
    public SIRStructure[] getStructures() {
        SIRStructure[] ret = (SIRStructure[])structureList.toArray(new SIRStructure[0]);
        return ret;
    }
    

    /**
     * visits an interface declaration
     */
      public Object visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods) 
    {
	blockStart("visitInterfaceDeclaration", self);
	interfaceList.add(self);
	/*visitClassBody(new JTypeDeclaration[0], methods, body);*/
	return self;
    }
  
    /**
     * visits a try-catch statement
     */
    public Object visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) 
    {
        blockStart("TryCatchStatement", self);
        /*tryClause.accept(this);
	  for (int i = 0; i < catchClauses.length; i++)
	  catchClauses[i].accept(this);*/  
	return self;
    }


    /**
     * visits a try-finally statement
     */
    public Object visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause) 
    {
        blockStart("TryFinallyStatement", self);
	/* tryClause.accept(this);
	   finallyClause.accept(this);*/
        return self;
    }

    /**
     * visits a throw statement
     */
    public Object visitThrowStatement(JThrowStatement self,
                                    JExpression expr) 
    {
        blockStart("ThrowStatement", self);
	/*expr.accept(this);*/
        return self;
    }

    /**
     * visits a synchronized statement
     */
    public Object visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) 
    {
        blockStart("SynchronizedStatement", self);
        /*cond.accept(this);
	  body.accept(this);*/
        return self;
    }


      /**
   * visits an instanceof expression
   */
    public Object visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) 
    {
        blockStart("InstanceOfExpression", self);
	trash = expr.accept(this);
	return self;
    }

      /**
     * visits an array length expression
     */
    public Object visitComments(JavaStyleComment[] comments)  
    {
	blockStart("Comments");
	return comments;
    }

    /**
     * visits an array length expression
     */
    public Object visitComment(JavaStyleComment comment)  
    {
	blockStart("Comment");
	return comment;
    }

    /**
     * visits an array length expression
     */
    public Object visitJavadoc(JavadocComment comment) 
    {
	blockStart("Javadoc");
	return null;
    }

      /**
     * visits an array length expression
     */
    public Object visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) 
    {
        blockStart("CatchClause", self);
        trash = exception.accept(this);
        trash = body.accept(this);
	return self;
    }

 

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.Kopi2SIR other = new at.dms.kjc.Kopi2SIR();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.Kopi2SIR other) {
  super.deepCloneInto(other);
  other.application = (at.dms.kjc.JCompilationUnit[])at.dms.kjc.AutoCloner.cloneToplevel(this.application);
  other.parentStream = (at.dms.kjc.sir.SIRStream)at.dms.kjc.AutoCloner.cloneToplevel(this.parentStream);
  other.topLevel = (at.dms.kjc.sir.SIRStream)at.dms.kjc.AutoCloner.cloneToplevel(this.topLevel);
  other.trash = (java.lang.Object)at.dms.kjc.AutoCloner.cloneToplevel(this.trash);
  other.num = this.num;
  other.lineNumber = this.lineNumber;
  other.currentMethod = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.currentMethod);
  other.visitedSIROps = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.visitedSIROps);
  other.symbolTable = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.symbolTable);
  other.interfaceList = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.interfaceList);
  other.interfaceTableList = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.interfaceTableList);
  other.structureList = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.structureList);
  other.searchList = (java.util.LinkedList)at.dms.kjc.AutoCloner.cloneToplevel(this.searchList);
  other.params = (at.dms.kjc.JFormalParameter[])at.dms.kjc.AutoCloner.cloneToplevel(this.params);
  other.paramNames = (java.lang.String[])at.dms.kjc.AutoCloner.cloneToplevel(this.paramNames);
  other.finalVars = (java.util.LinkedList)at.dms.kjc.AutoCloner.cloneToplevel(this.finalVars);
  other.anonCreation = this.anonCreation;
  other.nextLatency = this.nextLatency;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
