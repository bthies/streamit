//===========================================================================
//
//   FILE: TestK2S.java:
//   
//   Author: Michael Gordon
//   Date: Wed Sep 26 23:56:02 2001
//
//   Function:  
//
//===========================================================================


package at.dms.kjc;

import java.io.File;
import at.dms.compiler.CompilerMessages;
import at.dms.util.MessageDescription;
import at.dms.compiler.UnpositionedError;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;

public class TestK2S extends Main
{
    /**
     * Entry point
     *
     * @param     args            the command line arguments
     */
    public static void main(String[] args)
    {
   	boolean     success = true;
        
	success = compile(args);
        
        System.exit(success ? 0 : 1);
    }
    
    /**
     * Second entry point
     */
    public static boolean compile(String[] args)
    {
	return new TestK2S().run(args);
    }

    /**
     * Runs a compilation session
     *
     * @param     args            the command line arguments
     */
    public boolean run(String[] args)
    {
        if (!parseArguments(args))
            return false;
        
        initialize();
        
        if (infiles.size() == 0)
	    {
		options.usage();
		inform(KjcMessages.NO_INPUT_FILE);
		return false;
	    }

        if (verboseMode())
	    {
		inform(CompilerMessages.COMPILATION_STARTED,
		       new Integer(infiles.size()));
	    }

	try
	    {
		infiles = verifyFiles(infiles);
	    }
	catch (UnpositionedError e)
	    {
		reportTrouble(e);
		return false;
	    }
   
        options.destination = checkDestination(options.destination);
        
        JCompilationUnit[]  tree = new JCompilationUnit[infiles.size()];

        if (options.proc > tree.length)
            options.proc = tree.length;

        if (options.multi) {
            parseMultiProc(tree);
	}
        else {
            for (int count = 0; count < tree.length; count++)
                tree[count] = parseFile((File)infiles.elementAt(count));
	}
        infiles = null;

        if (errorFound)
	    return false;

	for (int count = 0; count < tree.length; count++)
	    checkInterface(tree[count]);

        if (errorFound)
	    return false;

        for (int count = 0; count < tree.length; count++)
	    checkInitializers(tree[count]);
        
        if (errorFound)
	    return false;

        for (int count = 0; count < tree.length; count++)
	    checkBody(tree[count]);
        
        if (errorFound)
	    return false;
	
	genCode();

	Kopi2SIR k2s = new Kopi2SIR(tree);
	SIRStream[] topLevel = new SIRStream[tree.length];
	
	for (int count = 0; count < tree.length; count++)
	    {
		//Return topLevel for each tree
		topLevel[count] = (SIRStream)tree[count].accept(k2s);
	    }
	
	//	System.out.println("---------------------------");

	

	for (int count = 0; count < tree.length; count++)
	    {
		SIRPrinter sirPrinter = new SIRPrinter("ir1.txt");
		//Return topLevel for each tree
		IterFactory.createFactory().createIter(topLevel[count]).accept(sirPrinter);
		sirPrinter.close();
	    }

	//	System.out.println("---------------------------");
	/*
	sirPrinter = new SIRPrinter();
	topLevel[0] = SIRBuilder.buildHello6();
	topLevel[0].accept(sirPrinter);

	sirPrinter.close();
	
	*/
	/*	k2s = new Kopi2SIR();
	
	CSourceClass[] csc = getClasses();
	for (int i=0; i<csc.length; i++) {
	    CClass CClazz = csc[i].getOwner();
	    if (CClazz != null) {
		System.out.println("cClass - " + CClazz.getIdent());
		if (CClazz.getSuperClass() != null)
		    System.out.println("cSuper - " + CClazz.getSuperClass().getIdent());
	    }
	    CMethod[] cm = csc[i].getMethods();
	    for (int j=0; j<cm.length; j++) {
		if (cm[j] instanceof CSourceMethod) {
		    ((CSourceMethod)cm[j]).getBody().accept(k2s);
		}
	    }
	}
	*/
	if (verboseMode())
	    inform(CompilerMessages.COMPILATION_ENDED);
	
	CodeSequence.endSession();
	
	//System.out.println("---------------------------");
       	
	
	/*	k2s = new Kopi2SIR();

	for (int i = 0; i < JPhylum.regSize(); i++) 
	    JPhylum.get(i).accept(k2s);
	*/
	
	return true;
    
	
    }
    
}
