/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Main.java,v 1.20 2003-10-02 04:31:31 jasperln Exp $
 */

package at.dms.kjc;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import at.dms.classfile.ClassPath;
import at.dms.classfile.ClassFileFormatException;
import at.dms.compiler.CWarning;
import at.dms.compiler.Compiler;
import at.dms.compiler.CompilerMessages;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.compiler.WarningFilter;
import at.dms.compiler.antlr.extra.InputBuffer;
import at.dms.compiler.antlr.runtime.ParserException;
import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

/**
 * This class implements the entry point of the Java compiler
 */
public class Main extends Compiler {

    // ----------------------------------------------------------------------
    // ENTRY POINT
    // ----------------------------------------------------------------------

    /**
     * Entry point
     *
     * @param	args		the command line arguments
     */
    public static void main(String[] args) {
	boolean	success;

	success = compile(args);

	System.exit(success ? 0 : 1);
    }

    /**
     * Second entry point
     */
    public static boolean compile(String[] args) {
	return new Main().run(args);
    }

    // ----------------------------------------------------------------------
    // RUN FROM COMMAND LINE
    // ----------------------------------------------------------------------

    /**
     * Runs a compilation session
     *
     * @param	args		the command line arguments
     */
    public boolean run(String[] args) {
	if (!parseArguments(args)) {
	    return false;
	}

	initialize();

	if (infiles.size() == 0) {
	    options.usage();
	    inform(KjcMessages.NO_INPUT_FILE);
	    return false;
	}

	if (verboseMode()) {
	    inform(CompilerMessages.COMPILATION_STARTED, new Integer(infiles.size()));
	}

	try {
	    infiles = verifyFiles(infiles);
	} catch (UnpositionedError e) {
	    reportTrouble(e);
	    return false;
	}

	options.destination = checkDestination(options.destination);

	JCompilationUnit[]	tree = new JCompilationUnit[infiles.size()];

	if (options.proc > tree.length) {
	    options.proc = tree.length;
	}

	if (options.multi) {
	    parseMultiProc(tree);
	} else {
	    for (int count = 0; count < tree.length; count++) {
		tree[count] = parseFile((File)infiles.elementAt(count));
	    }
	}

	infiles = null;

	if (errorFound) {
	    return false;
	}

	if (!options.beautify) {
	    long	lastTime = System.currentTimeMillis();
	    for (int count = 0; count < tree.length; count++) {
		checkInterface(tree[count]);
	    }
	    if (verboseMode()) {
		inform(CompilerMessages.INTERFACES_CHECKED, new Long(System.currentTimeMillis() - lastTime));
	    }

	    if (errorFound) {
		return false;
	    }

	    for (int count = 0; count < tree.length; count++) {
		checkInitializers(tree[count]);
	    }

	    if (errorFound) {
		return false;
	    }

	    if (options.multi) {
		checkBodyMultiProc(tree);
	    } else {
		for (int count = 0; count < tree.length; count++) {
		    checkBody(tree[count]);
		    if (!options.java && !options.beautify && !options.streamit) {
			tree[count] = null;
		    }
		}
	    }

	    if (errorFound) {
		return false;
	    }
	}

	// do streamit pass 
	if (options.streamit) {
	    StreaMITMain.compile(tree);
	}

	if (!options.nowrite && !options.streamit) {
	    if (options.java || options.beautify) {
		if (options.multi) {
		    acceptMultiProc(tree);
		} else {
		    for (int count = 0; count < tree.length; count++) {
			tree[count].accept(this, options.destination);
			tree[count] = null;
		    }
		}
	    } else {
		genCode();
	    }
	}

	if (errorFound) {
	    return false;
	}

	if (verboseMode()) {
	    inform(CompilerMessages.COMPILATION_ENDED);
	}

	CodeSequence.endSession();

	return true;
    }

    /**
     * Parse the argument list
     */
    public boolean parseArguments(String[] args) {
	options = new KjcOptions();
	if (!options.parseCommandLine(args)) {
	    return false;
	}
	infiles = Utils.toVector(options.nonOptions);
	return true;
    }

    /**
     * Generates the code from an array of compilation unit and
     * a destination
     *
     * @param	destination	the directory where to write classfiles
     */
    public void genCode() {
	CSourceClass[]	classes = getClasses();
	BytecodeOptimizer	optimizer = new BytecodeOptimizer(0);//options.optimize);

	this.classes.setSize(0);

	try {
	    if (options.multi) {
		genCodeMultiProc(classes, optimizer, options.destination);
	    } else {
		for (int count = 0; count < classes.length; count++) {
		    long		lastTime = System.currentTimeMillis();

		    classes[count].genCode(optimizer, options.destination);
		    if (verboseMode() && !classes[count].isNested()) {
			inform(CompilerMessages.CLASSFILE_GENERATED,
			       classes[count].getQualifiedName().replace('/', '.'),
			       new Long(System.currentTimeMillis() - lastTime));
		    }
		    classes[count] = null;
		}
	    }
	} catch (ClassFileFormatException e) {
	    e.printStackTrace();
	    reportTrouble(new UnpositionedError(CompilerMessages.FORMATTED_ERROR, e.getMessage()));
	} catch (IOException e) {
	    reportTrouble(new UnpositionedError(CompilerMessages.IO_EXCEPTION,
						"classfile",	//!!!FIXME !!!
						e.getMessage()));
	}
    }

    /**
     * Initialize the compiler (read classpath, check classes.zip)
     */
    protected void initialize() {
	ClassPath.init(options.classpath);
	CTopLevel.initSession(this);
	CStdType.init(this);
    }

    /**
     * returns true iff compilation in verbose mode is requested.
     */
    public boolean verboseMode() {
	return options.verbose;
    }

    /**
     * ...
     */
    public KjcPrettyPrinter getPrettyPrinter(String fileName) {
	return new KjcPrettyPrinter(fileName);
    }

    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    /**
     * parse the givven file and return a compilation unit
     * side effect: increment error number
     * @param	file		the name of the file (assert exists)
     * @return	the compilation unit defined by this file
     */
    protected JCompilationUnit parseFile(File file) {
	InputBuffer		buffer;

	try {
	    buffer = new InputBuffer(file, options.encoding);
	} catch (UnsupportedEncodingException e) {
	    reportTrouble(new UnpositionedError(CompilerMessages.UNSUPPORTED_ENCODING,
						options.encoding));
	    return null;
	} catch (IOException e) {
	    reportTrouble(new UnpositionedError(CompilerMessages.IO_EXCEPTION,
						file.getPath(),
						e.getMessage()));
	    return null;
	}

	KjcParser		parser;
	JCompilationUnit	unit;
	long		lastTime = System.currentTimeMillis();

	parser = new KjcParser(this, buffer);

	try {
	    unit = parser.jCompilationUnit();
	} catch (ParserException e) {
	    reportTrouble(parser.beautifyParseError(e));
	    unit = null;
	} catch (Exception e) {
	    //err.println("{" + file.getPath() + ":" + scanner.getLine() + "} " + e.getMessage());
	    e.printStackTrace();
	    errorFound = true;
	    unit = null;
	}

	if (verboseMode()) {
	    inform(CompilerMessages.FILE_PARSED, file.getPath(), new Long(System.currentTimeMillis() - lastTime));
	}

	try {
	    buffer.close();
	} catch (IOException e) {
	    reportTrouble(new UnpositionedError(CompilerMessages.IO_EXCEPTION,
						file.getPath(),
						e.getMessage()));
	}

	return unit;
    }

    /**
     * check that interface of a given compilation unit is correct
     * side effect: increment error number
     * @param	cunit		the compilation unit
     */
    protected void checkInterface(JCompilationUnit cunit) {
	try {
	    cunit.checkInterface(this);
	} catch (PositionedError e) {
	    reportTrouble(e);
	}
    }

    /**
     * check that interface of a given compilation unit is correct
     * side effect: increment error number
     * @param	cunit		the compilation unit
     */
    protected void checkInitializers(JCompilationUnit cunit) {
	try {
	    cunit.checkInitializers(this, classes);
	} catch (PositionedError e) {
	    reportTrouble(e);
	}
    }

    /**
     * check that body of a given compilation unit is correct
     * side effect: increment error number
     * @param	cunit		the compilation unit
     */
    protected void checkBody(JCompilationUnit cunit) {
	long	lastTime = System.currentTimeMillis();

	try {
	    cunit.checkBody(this, classes);
	} catch (PositionedError e) {
	    reportTrouble(e);
	}

	if (verboseMode()) {
	    inform(CompilerMessages.BODY_CHECKED, cunit.getFileName(), new Long(System.currentTimeMillis() - lastTime));
	}
    }

    /**
     * generate the source code of parsed compilation unit
     * @param	cunit		the compilation unit
     */
    protected void generateJavaCode(JCompilationUnit cunit) {
	long	lastTime = System.currentTimeMillis();

	cunit.accept(this, options.destination);
	if (verboseMode()) {
	    inform(CompilerMessages.JAVA_CODE_GENERATED, cunit.getFileName(), new Long(System.currentTimeMillis() - lastTime));
	}
    }

    /**
     * Parse each file in multi thread
     */
    protected void parseMultiProc(final JCompilationUnit[] tree) {
	try {
	    Thread[]		parsers = new Thread[options.proc];
	    int		length = tree.length / options.proc;

	    for (int i = 0; i < options.proc; i++) {
		parsers[i] = new ThreadedParser(this, infiles, tree, i * length, i == options.proc - 1 ? tree.length : (i + 1) * length);
		parsers[i].start();
	    }

	    for (int i = 0; i < options.proc; i++) {
		parsers[i].join();
	    }
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}
    }

    /**
     *
     */
    protected void checkBodyMultiProc(final JCompilationUnit[] tree) {
	try {
	    Thread[]		checkers = new Thread[tree.length];
	    int		length = tree.length / options.proc;

	    for (int i = 0; i < options.proc; i++) {
		checkers[i] = new ThreadedChecker(this, tree, i * length, i == options.proc - 1 ? tree.length : (i + 1) * length);
		checkers[i].start();
	    }

	    for (int i = 0; i < options.proc; i++) {
		checkers[i].join();
	    }
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}
    }

    /**
     *
     */
    protected void acceptMultiProc(final JCompilationUnit[] tree) {
	try {
	    Thread[]		writers = new Thread[tree.length];
	    int		length = tree.length / options.proc;

	    for (int i = 0; i < options.proc; i++) {
		writers[i] = new ThreadedVisitor(options.destination, tree, i * length, i == options.proc - 1 ? tree.length : (i + 1) * length, this);
		writers[i].start();
	    }

	    for (int i = 0; i < options.proc; i++) {
		writers[i].join();
	    }
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}
    }

    /**
     *
     */
    protected void genCodeMultiProc(final CSourceClass[] tree,
				    BytecodeOptimizer optimizer,
				    final String destination)
    {
	try {
	    Thread[]		writers = new Thread[tree.length];
	    int		length = tree.length / options.proc;

	    for (int i = 0; i < options.proc; i++) {
		writers[i] = new ThreadedGenerator(tree,
						   optimizer,
						   destination,
						   i * length,
						   i == options.proc - 1 ? tree.length : (i + 1) * length);
		writers[i].start();
	    }

	    for (int i = 0; i < options.proc; i++) {
		writers[i].join();
	    }
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}
    }

    // --------------------------------------------------------------------
    // COMPILER
    // --------------------------------------------------------------------

    /**
     * Reports a trouble (error or warning).
     *
     * @param	trouble		a description of the trouble to report.
     */
    public void reportTrouble(PositionedError trouble) {
	if (trouble instanceof CWarning) {
	    if (options.warning != 0 && filterWarning((CWarning)trouble)) {
		inform(trouble);
	    }
	} else {
	    if (trouble.getTokenReference() != TokenReference.NO_REF) {
		inform(trouble);
		errorFound = true;
	    }
	}
    }

    /**
     * Reports a trouble.
     *
     * @param	trouble		a description of the trouble to report.
     */
    public void reportTrouble(UnpositionedError trouble) {
	inform(trouble);
	errorFound = true;
    }

    protected boolean filterWarning(CWarning warning) {
	WarningFilter	filter = getFilter();
	int			value = filter.filter(warning);

	switch (value) {
	case WarningFilter.FLT_REJECT:
	    return false;
	case WarningFilter.FLT_FORCE:
	    return true;
	case WarningFilter.FLT_ACCEPT:
	    return warning.getSeverityLevel() <= options.warning;
	default:
	    throw new InconsistencyException();
	}
    }

    protected WarningFilter getFilter() {
	if (filter == null) {
	    if (options.filter != null) {
		try {
		    filter = (WarningFilter)Class.forName(options.filter).newInstance();
		} catch (Exception e) {
		    inform(KjcMessages.FILTER_NOT_FOUND, options.filter);
		}
	    }
	    if (filter == null) {
		filter = new DefaultFilter();
	    }
	}

	return filter;
    }

    /**
     * Returns true iff comments should be parsed (false if to be skipped)
     */
    public boolean parseComments() {
	return options.deprecation || (options.beautify && !options.nowrite);
    }

    /**
     * Returns the classes to generate
     */
    public CSourceClass[] getClasses() {
	return (CSourceClass[])at.dms.util.Utils.toArray(classes, CSourceClass.class);
    }

    // ----------------------------------------------------------------------
    // PROTECTED DATA MEMBERS
    // ----------------------------------------------------------------------

    protected Vector		infiles = new Vector();
    protected boolean		errorFound;

    protected KjcOptions		options;

    private WarningFilter		filter = new DefaultFilter();

    // all generated classes
    private Vector		classes = new Vector(100);

    // deported here because of a javac bug
    static class ThreadedParser extends Thread {
	ThreadedParser(Main compiler, Vector infiles, JCompilationUnit[] tree, int start, int end) {
	    this.compiler = compiler;
	    this.start = start;
	    this.end = end;
	    this.tree = tree;
	    this.infiles = infiles;
	}

	public void run() {
	    for (int count = start; count < end; count++) {
		tree[count] = compiler.parseFile((File)infiles.elementAt(count));
	    }
	}

	private Main			compiler;
	private int			start;
	private int			end;
	private JCompilationUnit[]	tree;
	private Vector		infiles;
    }

    static class ThreadedChecker extends Thread {
	ThreadedChecker(Main compiler, JCompilationUnit[] tree, int start, int end) {
	    this.compiler = compiler;
	    this.start = start;
	    this.end = end;
	    this.tree = tree;
	}

	public void run() {
	    for (int count = start; count < end; count++) {
		compiler.checkBody(tree[count]);
		tree[count] = null;
	    }
	}

	private Main			compiler;
	private int			start;
	private int			end;
	private JCompilationUnit[]	tree;
    }

    static class ThreadedVisitor extends Thread {
	ThreadedVisitor(String destination, JCompilationUnit[] tree, int start, int end, Main compiler) {
	    this.start = start;
	    this.end = end;
	    this.tree = tree;
	    this.destination = destination;
	    this.compiler = compiler;
	}

	public void run() {
	    for (int count = start; count < end; count++) {
		tree[count].accept(compiler, destination);
	    }
	}

	private int			start;
	private int			end;
	private JCompilationUnit[]	tree;
	private String		destination;
	private Main			compiler;
    }

    static class ThreadedGenerator extends Thread {
	ThreadedGenerator(CSourceClass[] tree,
			  BytecodeOptimizer optimizer,
			  String destination,
			  int start,
			  int end)
	{
	    this.start = start;
	    this.end = end;
	    this.tree = tree;
	    this.optimizer = optimizer;
	    this.destination = destination;
	}

	public void run() {
	    try {
		for (int count = start; count < end; count++) {
		    tree[count].genCode(optimizer,destination);
		    tree[count] = null;
		}
	    } catch (IOException e) {
		System.err.println(e.getMessage());
	    } catch (ClassFileFormatException e) {
		System.err.println(e.getMessage());
	    }
	}

	private int			start;
	private int			end;
	private CSourceClass[]	tree;
	private BytecodeOptimizer	optimizer;
	private String		destination;
    }
}
