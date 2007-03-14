package at.dms.kjc.rstream;

import at.dms.kjc.*;

/**
 * Process timers (dumping user and system ticks) in SIR.
 * 
 * This code is to allow process timers to be used in SIR code
 * before it is time to generate strings from the SIR code.
 * However, there are some hacks that should generate C or C++
 * code when put through our code generators, but which are not
 * amenable to symbolic execution of the SIR code.
 * <br/>
 * For compatibility with the rstream backend this class uses
 * local variables (as opposed to fields) for the timers.
 * <p>
 * The constructor takes the name of the timer to be used.
 * <p>
 * timerCIncludes is C / C++ specific list of .h files that need
 * to be included 
 * <p>
 * {@link #timerDeclarations()} returns declarations necessary to use timer.
 * <p>
 * {@link #timerStart()} returns SIR statements to start timing with the timer.
 * <p>
 * {@link #timerEnd()} returns SIR statments to end timing with the timer.
 * <p>
 * {@link #timerPrint()} returns SIR statments to print the elapsed system 
 * and user ticks.
 *  
 * @author Allyn Dimock
 *
 */
public final class TimerCode {

    // hack:
    // timerStartName used as both a variable -- to pass address of to time();
    // and as a class name -- so we can reference tms_utime, tms_stime fields
    // ditto timerEndName
    private String timerStartName;
    private String timerEndName;

    // hack:
    // have to generate "struct tms" co claim that is a class name
    private CType timerType;
    
    // JVariableDefinition's for building JFieldDeclaration's
    private JVariableDefinition timerStartVar;
    private JVariableDefinition timerEndVar;
    private JVariableDefinition timerTicksVar;

    // I prefer to have this manifest constant have just a name
    // rather than a type if I can get away with it.
    private JNameExpression getTickConstantName;
    
    /**
     * Constructor for timer code.
     * 
     * @param timerName is the name of the timer (will have some suffices appended
     *        to become names of some fields)
     */
    public TimerCode(String timerName) {
        timerType = new CEmittedTextType(new Object[]{"struct tms"});
        timerStartName = timerName + "_start";
        timerStartVar = new JVariableDefinition(timerType, timerStartName);
        timerEndName = timerName + "_end";
        String timerTicksName = timerName + "_ticks_per_sec";
        timerEndVar = new JVariableDefinition(timerType, timerEndName);
        timerTicksVar = new JVariableDefinition(CStdType.Integer,
                timerTicksName);
        getTickConstantName = new JNameExpression(null, Names._SC_CLK_TCK);
    }

    
    /**
     * An array of strings for '#include's
     * The caller is reponsible for prefixing with '#include "
     * and for ignoring any strings that would be generated anyway 
     */
    
    public static final String[] timerCIncludes = {
	"<sys/time.h>",
	"<sys/times.h>",
	"<sys/types.h>"//,
//	"<unistd.h>"
    };

    /**
     * Declartions needed for using timer code.
     * 
     * @return  a list of variable declarations
     */
    
    public JVariableDeclarationStatement[] timerDeclarations() {
        JVariableDeclarationStatement[] retval = {
            new JVariableDeclarationStatement(timerStartVar),
            new JVariableDeclarationStatement(timerEndVar),
            new JVariableDeclarationStatement(timerTicksVar)};
        
        return retval;
    }


    /**
     * SIR code to get the start value of a proc timer.
     * 
     * @return a list of statements to be inserted into the calling program
     *         at a place where declarations from timerDeclaration are in scope.
     */

    public JStatement[] timerStart() {
    //  time (&timer_start) 
        
	// trick here to get "&timer_start"
	// fake up a method call to "&"
	// Code generators will produce expression
	// "&(timer_start)

	JExpression [] tStartParamsParams = {
            new JLocalVariableExpression(timerStartVar)};
	JExpression[] tStartParams = {
	    new JMethodCallExpression
	    (null, Names.addressof,tStartParamsParams)};

    JExpression getTime = 
        new JMethodCallExpression
        (null,new JThisExpression(),
         Names.times, tStartParams);
    
    JStatement[] retval = {
      new JExpressionStatement(getTime)
    };
    
	return retval;
    }

    /**
     * SIR code to get the end value of a proc timer.
     * 
     * @return a list of statements to be inserted into the calling program
     *         at a place where code from timerStart has been executed
     *         and where declarations from timerDeclaration are in scope.
     */

    public JStatement[] timerEnd() {
        // time (&timer_end);
        
        // trick here to get "&timer_end"
        // fake up a method call to "&"
        // Code generators will produce expression
        // "&(timer_end)

        JExpression[] tEndParamsParams = { 
            new JLocalVariableExpression(timerEndVar) };
        JExpression[] tEndParams = { 
            new JMethodCallExpression(null,
                Names.addressof, tEndParamsParams) };

        JStatement[] retval = { 
            new JExpressionStatement(
                new JMethodCallExpression(null, new JThisExpression(),
                        Names.times, tEndParams)) };

	return retval;
    }

    /**
     * SIR code to print value of a proc timer.
     * 
     * @return a list of statements to be inserted into the calling program
     *         at a place where code from timerStart and TimerEnd has been executed
     *         and where declarations from timerDeclaration are in scope.
     */

    public JStatement[] timerPrint() {

	JExpression[] sysconfParams = {getTickConstantName};

    // ticks_per_sec = sysconf(_SC_CLK_TCK);
    JExpression getTicksPerSecond =
        new JAssignmentExpression
         (new JLocalVariableExpression(timerTicksVar),
           (new JMethodCallExpression
            (null, new JThisExpression(),
             Names.sysconf,sysconfParams)));
    
	JExpression[] fprintfParams = {
	    new JNameExpression(null,"stderr"),
	    new JStringLiteral
	    (null, "total runtime: user %.02f; sys %.02f (%d ticks/sec)\\n", 
	     false),
	    divTicks(diffTimes("tms_utime")),
	    divTicks(diffTimes("tms_stime")),
	    new JLocalVariableExpression(timerTicksVar)};

//  fprintf(stderr, "total runtime: user %.02f; sys %.02f (%d ticks/sec)\n",
//  (t_end.tms_utime - t_start.tms_utime) / (double)ticks_per_sec,
//  (t_end.tms_stime - t_start.tms_stime) / (double)ticks_per_sec,
//  ticks_per_sec);
    JExpression  printIt =
        new JMethodCallExpression
         (null, new JThisExpression(),
          Names.fprintf, fprintfParams);
    
    
    JStatement[] retval = {
            new JExpressionStatement(getTicksPerSecond),
            new JExpressionStatement(printIt)};
    
	return retval;
    }


    // abstract out division expression in fprintf code above.
    private JExpression divTicks (JExpression numerator) {
    return
        new JDivideExpression
        (null, numerator,
         new JCastExpression
         (null, 
          new JLocalVariableExpression(timerTicksVar),
          new CDoubleType()));
    }

    // abstract out the subtraction expressions in fprintf code above.
    private JExpression diffTimes(String fieldname) {
    return
        new JMinusExpression
        (null,
         new JFieldAccessExpression
          (new JClassExpression (null, new CEmittedTextType(new Object[]{timerEndName}), 0),
           fieldname),
         new JFieldAccessExpression
          (new JClassExpression (null, new CEmittedTextType(new Object[]{timerStartName}), 0),
           fieldname)
           );
    }
}
