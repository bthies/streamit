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
 * $Id: JavaCodeGenerator.java,v 1.6 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Enumeration;
import java.io.IOException;
import java.io.PrintWriter;

import at.dms.compiler.tools.antlr.runtime.*;

/**
 * A Java code generator.
 *
 * <p>
 * A JavaCodeGenerator knows about a Grammar data structure and
 * a grammar analyzer.  The Grammar is walked to generate the
 * appropriate code for both a parser and lexer (if present).
 * This interface may change slightly so that the lexer is
 * itself living inside of a Grammar object (in which case,
 * this class generates only one recognizer).  The main method
 * to call is <tt>gen()</tt>, which initiates all code gen.
 *
 * <p>
 * The interaction of the code generator with the analyzer is
 * simple: each subrule block calls deterministic() before generating
 * code for the block.  Method deterministic() sets lookahead caches
 * in each Alternative object.  Technically, a code generator
 * doesn't need the grammar analyzer if all lookahead analysis
 * is done at runtime, but this would result in a slower parser.
 *
 * <p>
 * This class provides a set of support utilities to handle argument
 * list parsing and so on.
 *
 * @author  Terence Parr, John Lilley
 * @version 2.00a
 */

public class JavaCodeGenerator {
    /**
     * Current tab indentation for code output
     */
    private int tabs=0;
    /**
     * Current output Stream
     */
    transient private PrintWriter currentOutput; // SAS: for proper text i/o
    /**
     * The grammar for which we generate code
     */
    private Grammar grammar = null;
    /**
     * List of all bitsets that must be dumped.  These are Vectors of BitSet.
     */
    private Vector bitsetsUsed;
    /**
     * The main class
     */
    private Main tool;
    /**
     * The grammar behavior
     */
    private DefineGrammarSymbols behavior;
    /**
     * The LLk analyzer
     */
    private LLkGrammarAnalyzer analyzer;
    /**
     * Object used to format characters in the target language.
     * subclass must initialize this to the language-specific formatter
     */
    private CharFormatter charFormatter;

    /**
     * Default values for code-generation thresholds
     */
    private static final int DEFAULT_MAKE_SWITCH_THRESHOLD = 2;
    private static final int DEFAULT_BITSET_TEST_THRESHOLD = 4;

    /**
     * This is a hint for the language-specific code generator.
     * A switch() or language-specific equivalent will be generated instead
     * of a series of if/else statements for blocks with number of alternates
     * greater than or equal to this number of non-predicated LL(1) alternates.
     * This is modified by the grammar option "codeGenMakeSwitchThreshold"
     */
    private int makeSwitchThreshold = DEFAULT_MAKE_SWITCH_THRESHOLD;

    /**
     * This is a hint for the language-specific code generator.
     * A bitset membership test will be generated instead of an
     * ORed series of LA(k) comparisions for lookahead sets with
     * degree greater than or equal to this value.
     * This is modified by the grammar option "codeGenBitsetTestThreshold"
     */
    private int bitsetTestThreshold = DEFAULT_BITSET_TEST_THRESHOLD;

    public static String TokenTypesFileSuffix = "TokenTypes";
    public static String TokenTypesFileExt = ".txt";

    /**
     * Output a String to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output
     */
    private void _print(String s) {
        if (s != null) {
            currentOutput.print(s);
        }
    }

    /**
     * Print an action without leading tabs, attempting to
     * preserve the current indentation level for multi-line actions
     * Ignored if string is null.
     * @param s The action string to output
     */
    private void _printAction(String s) {
        if (s == null) {
            return;
        }

        // Skip leading newlines, tabs and spaces
        int start = 0;
        while (start < s.length() && Character.isSpaceChar(s.charAt(start)) ) {
            start++;
        }

        // Skip leading newlines, tabs and spaces
        int end = s.length()-1;
        while ( end > start && Character.isSpaceChar(s.charAt(end)) ) {
            end--;
        }

        char c=0;
        for (int i = start; i <= end;) {
            c = s.charAt(i);
            i++;
            boolean newline = false;
            switch (c)
                {
                case '\n':
                    newline=true;
                    break;
                case '\r':
                    if ( i<=end && s.charAt(i)=='\n' ) {
                        i++;
                    }
                    newline=true;
                    break;
                default:
                    currentOutput.print(c);
                    break;
                }
            if ( newline ) {
                currentOutput.println();
                printTabs();
                // Absorb leading whitespace
                while (i <= end && Character.isSpaceChar(s.charAt(i)) ) {
                    i++;
                }
                newline=false;
            }
        }
        currentOutput.println();
    }

    /**
     * Output a String followed by newline, to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output
     */
    private void _println(String s) {
        if (s != null) {
            currentOutput.println(s);
        }
    }

    /**
     * Test if a set element array represents a contiguous range.
     * @param elems The array of elements representing the set, usually from BitSet.toArray().
     * @return true if the elements are a contiguous range (with two or more).
     */
    public static boolean elementsAreRange(int[] elems) {
        if (elems.length==0) {
            return false;
        }
        int begin = elems[0];
        int end = elems[elems.length-1];
        if ( elems.length<=2 ) {
            // Not enough elements for a range expression
            return false;
        }
        if ( end-begin+1 > elems.length ) {
            // The set does not represent a contiguous range
            return false;
        }
        int v = begin+1;
        for (int i=1; i<elems.length-1; i++) {
            if ( v != elems[i] ) {
                // The set does not represent a contiguous range
                return false;
            }
            v++;
        }
        return true;
    }

    /**
     * Get the identifier portion of an argument-action token.
     * The ID of an action is assumed to be a trailing identifier.
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param t The action token
     * @return A string containing the text of the identifier
     */
    private String extractIdOfAction(Token t) {
        return extractIdOfAction(t.getText(), t.getLine());
    }

    /**
     * Get the identifier portion of an argument-action.
     * The ID of an action is assumed to be a trailing identifier.
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param s The action text
     * @param line Line used for error reporting.
     * @return A string containing the text of the identifier
     */
    private String extractIdOfAction(String s, int line) {
        s = removeAssignmentFromDeclaration(s);
        // Search back from the end for a non alphanumeric.  That marks the
        // beginning of the identifier
        for (int i = s.length()-2; i >=0; i--) {
            // TODO: make this work for language-independent identifiers?
            if (!Character.isLetterOrDigit(s.charAt(i)) && s.charAt(i) != '_') {
                // Found end of type part
                return s.substring(i+1);
            }
        }
        // Something is bogus, but we cannot parse the language-specific
        // actions any better.  The compiler will have to catch the problem.
        Utils.warning("Ill-formed action", grammar.getFilename(), line);
        return "";
    }

    /**
     * Get the type string out of an argument-action token.
     * The type of an action is assumed to precede a trailing identifier
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param t The action token
     * @return A string containing the text of the type
     */
    private String extractTypeOfAction(Token t) {
        return extractTypeOfAction(t.getText(), t.getLine());
    }

    /**
     * Get the type portion of an argument-action.
     * The type of an action is assumed to precede a trailing identifier
     * Specific code-generators may want to override this
     * if the language has unusual declaration syntax.
     * @param s The action text
     * @param line Line used for error reporting.
     * @return A string containing the text of the type
     */
    private String extractTypeOfAction(String s, int line) {
        s = removeAssignmentFromDeclaration(s);
        // Search back from the end for a non alphanumeric.  That marks the
        // beginning of the identifier
        for (int i = s.length()-2; i >=0; i--) {
            // TODO: make this work for language-independent identifiers?
            if (!Character.isLetterOrDigit(s.charAt(i)) && s.charAt(i) != '_') {
                // Found end of type part
                return s.substring(0,i+1);
            }
        }
        // Something is bogus, but we cannot parse the language-specific
        // actions any better.  The compiler will have to catch the problem.
        Utils.warning("Ill-formed action", grammar.getFilename(), line);
        return "";
    }

    /**
     * Generate the token types as a text file for persistence across shared lexer/parser
     */
    private void genTokenInterchange(TokenManager tm) throws IOException {
        // Open the token output Java file and set the currentOutput stream
        String fName = tm.getName() + TokenTypesFileSuffix+TokenTypesFileExt;
        currentOutput = tool.openOutputFile(fName);

        println("// $ANTLR "+Main.version+": "+
                Utils.fileMinusPath(tool.grammarFile)+
                " -> "+
                fName+
                "$");

        tabs = 0;

        // Header
        println(tm.getName() + "    // output token vocab name");

        // Generate a definition for each token type
        Vector v = tm.getVocabulary();
        for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
            String s = (String)v.elementAt(i);
            if (s != null && !s.startsWith("<") ) {
                // if literal, find label
                if ( s.startsWith("\"") ) {
                    StringLiteralSymbol sl = (StringLiteralSymbol)tm.getTokenSymbol(s);
                    if ( sl!=null && sl.label != null ) {
                        print(sl.label+"=");
                    }
                    println(s + "=" + i);
                } else {
                    print(s);
                    // check for a paraphrase
                    TokenSymbol ts = (TokenSymbol)tm.getTokenSymbol(s);
                    if ( ts==null ) {
                        Utils.warning("undefined token symbol: "+s);
                    } else {
                        if ( ts.getParaphrase()!=null ) {
                            print("("+ts.getParaphrase()+")");
                        }
                    }
                    println("=" + i);
                }
            }
        }

        // Close the tokens output file
        currentOutput.close();
        currentOutput = null;
    }

    /**
     * Process a string for an simple expression for use in xx/action.g
     * it is used to cast simple tokens/references to the right type for
     * the generated language.
     * @param str A String.
     */
    public String processStringForASTConstructor( String str ) {
        return str;
    }
    /**
     * Given the index of a bitset in the bitset list, generate a unique name.
     * Specific code-generators may want to override this
     * if the language does not allow '_' or numerals in identifiers.
     * @param index  The index of the bitset in the bitset list.
     */
    private String getBitsetName(int index)
    {
        return "_tokenSet_" + index;
    }

    public static String lexerRuleName(String id) {
        return "m"+id;
    }

    /**
     * Add a bitset to the list of bitsets to be generated.
     * if the bitset is already in the list, ignore the request.
     * Always adds the bitset to the end of the list, so the
     * caller can rely on the position of bitsets in the list.
     * The returned position can be used to format the bitset
     * name, since it is invariant.
     * @param p Bit set to mark for code generation
     * @param forParser true if the bitset is used for the parser, false for the lexer
     * @return The position of the bitset in the list.
     */
    private int markBitsetForGen(BitSet p) {
        // Is the bitset (or an identical one) already marked for gen?
        for (int i = 0; i < bitsetsUsed.size(); i++) {
            BitSet set = (BitSet)bitsetsUsed.elementAt(i);
            if (p.equals(set)) {
                // Use the identical one already stored
                return i;
            }
        }

        // Add the new bitset
        bitsetsUsed.appendElement(p.clone());
        return bitsetsUsed.size()-1;
    }

    /**
     * Output tab indent followed by a String, to the currentOutput stream.
     * Ignored if string is null.
     * @param s The string to output.
     */
    private void print(String s) {
        if (s != null) {
            printTabs();
            currentOutput.print(s);
        }
    }

    /**
     * Print an action with leading tabs, attempting to
     * preserve the current indentation level for multi-line actions
     * Ignored if string is null.
     * @param s The action string to output
     */
    private void printAction(String s) {
        if (s != null) {
            printTabs();
            _printAction(s);
        }
    }

    /**
     * Output tab indent followed by a String followed by newline,
     * to the currentOutput stream.  Ignored if string is null.
     * @param s The string to output
     */
    private void println(String s) {
        if (s != null) {
            printTabs();
            currentOutput.println(s);
        }
    }

    /**
     * Output the current tab indentation.  This outputs the number of tabs
     * indicated by the "tabs" variable to the currentOutput stream.
     */
    private void printTabs() {
        for (int i=1; i<=tabs; i++) {
            currentOutput.print("\t");
        }
    }

    /**
     * Lexically process tree-specifiers in the action.
     *  This will replace #id and #(...) with the appropriate
     *  function calls and/or variables.
     *
     *  This is the default Java action translator, but I have made
     *  it work for C++ also.
     */
    private String processActionForTreeSpecifiers(String actionStr, int line, RuleBlock currentRule) {
        if ( actionStr==null || actionStr.length()==0 ) {
            return null;
        }
        // The action trans info tells us (at the moment) whether an
        // assignment was done to the rule's tree root.
        if (grammar==null) {
            return actionStr;
        }
        if (grammar instanceof LexerGrammar && actionStr.indexOf('$') != -1) {
            // Create a lexer to read an action and return the translated version
            ActionLexer lexer = new ActionLexer(actionStr, currentRule, this);
            lexer.setLineOffset(line);
            lexer.setTool(tool);
            try {
                lexer.mACTION(true);
                actionStr = lexer.getTokenObject().getText();
                // System.out.println("action translated: "+actionStr);
            } catch (RecognitionException ex) {
                lexer.reportError(ex);
                return actionStr;
            } catch (TokenStreamException tex) {
                Utils.panic("Error reading action:"+actionStr);
                return actionStr;
            } catch (CharStreamException io) {
                Utils.panic("Error reading action:"+actionStr);
                return actionStr;
            }
        }
        return actionStr;
    }

    /**
     * Remove the assignment portion of a declaration, if any.
     * @param d the declaration
     * @return the declaration without any assignment portion
     */
    private String removeAssignmentFromDeclaration(String d) {
        // If d contains an equal sign, then it's a declaration
        // with an initialization.  Strip off the initialization part.
        if (d.indexOf('=') >= 0) {
            d = d.substring(0, d.indexOf('=')).trim();
        }
        return d;
    }

    /**
     * Set all fields back like one just created
     */
    private void reset() {
        tabs = 0;
        // Allocate list of bitsets tagged for code generation
        bitsetsUsed = new Vector();
        currentOutput = null;
        grammar = null;
        makeSwitchThreshold = DEFAULT_MAKE_SWITCH_THRESHOLD;
        bitsetTestThreshold = DEFAULT_BITSET_TEST_THRESHOLD;
    }

    public static String reverseLexerRuleName(String id) {
        return id.substring(1,id.length());
    }

    public void setAnalyzer(LLkGrammarAnalyzer analyzer_) {
        analyzer = analyzer_;
    }

    public void setBehavior(DefineGrammarSymbols behavior_) {
        behavior = behavior_;
    }

    /**
     * Set a grammar for the code generator to use
     */
    private void setGrammar(Grammar g) {
        reset();
        grammar = g;
        // Lookup make-switch threshold in the grammar generic options
        if (grammar.hasOption("codeGenMakeSwitchThreshold")) {
            try {
                makeSwitchThreshold = grammar.getIntegerOption("codeGenMakeSwitchThreshold");
                //System.out.println("setting codeGenMakeSwitchThreshold to " + makeSwitchThreshold);
            } catch (NumberFormatException e) {
                tool.error(
                           "option 'codeGenMakeSwitchThreshold' must be an integer",
                           grammar.getClassName(),
                           grammar.getOption("codeGenMakeSwitchThreshold").getLine()
                           );
            }
        }

        // Lookup bitset-test threshold in the grammar generic options
        if (grammar.hasOption("codeGenBitsetTestThreshold")) {
            try {
                bitsetTestThreshold = grammar.getIntegerOption("codeGenBitsetTestThreshold");
                //System.out.println("setting codeGenBitsetTestThreshold to " + bitsetTestThreshold);
            } catch (NumberFormatException e) {
                tool.error(
                           "option 'codeGenBitsetTestThreshold' must be an integer",
                           grammar.getClassName(),
                           grammar.getOption("codeGenBitsetTestThreshold").getLine()
                           );
            }
        }
    }

    public void setTool(Main tool_) {
        tool = tool_;
    }

    // ######################################################################

    // non-zero if inside syntactic predicate generation
    private int syntacticPredLevel = 0;

    // Are we saving the text consumed (for lexers) right now?
    private boolean saveText = false;

    // Grammar parameters set up to handle different grammar classes.
    // These are used to get instanceof tests out of code generation
    String labeledElementType;
    String labeledElementInit;
    String commonExtraArgs;
    String commonExtraParams;
    String commonLocalVars;
    String lt1Value;
    String exceptionThrown;
    String throwNoViable;

    /**
     * Tracks the rule being generated.  Used for mapTreeId
     */
    RuleBlock currentRule;

    /**
     * Tracks the rule or labeled subrule being generated.  Used for
     AST generation. */
    String currentASTResult;

    /* Count of unnamed generated variables */
    int astVarNumber = 1;

    public static final int caseSizeThreshold = 127; // ascii is max

    /**
     * Create a Java code-generator using the given Grammar.
     * The caller must still call setTool, setBehavior, and setAnalyzer
     * before generating code.
     */
    public JavaCodeGenerator() {
        super();
        charFormatter = new JavaCharFormatter();
    }

    public void exitIfError() {
        if (tool.hasError) {
            System.out.println("Exiting due to errors.");
            System.exit(1);
        }
    }
    /**
     * Generate the parser, lexer, treeparser, and token types in Java
     */
    public void gen() {
        // Do the code generation
        try {
            // Loop over all grammars
            Enumeration<Grammar> grammarIter = behavior.grammars.elements();
            while (grammarIter.hasMoreElements()) {
                Grammar g = grammarIter.nextElement();
                // Connect all the components to each other
                g.setGrammarAnalyzer(analyzer);
                analyzer.setGrammar(g);
                // To get right overloading behavior across hetrogeneous grammars
                setupGrammarParameters(g);
                g.generate(this);
                // print out the grammar with lookahead sets (and FOLLOWs)
                // System.out.print(g.toString());
                exitIfError();
            }

            // Loop over all token managers (some of which are lexers)
            Enumeration<TokenManager> tmIter = behavior.tokenManagers.elements();
            while (tmIter.hasMoreElements()) {
                TokenManager tm = tmIter.nextElement();
                if (!tm.isReadOnly()) {
                    // Write the token manager tokens as Java
                    // this must appear before genTokenInterchange so that
                    // labels are set on string literals
                    genTokenTypes(tm);
                    // Write the token manager tokens as plain text
                    genTokenInterchange(tm);
                }
                exitIfError();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    /**
     * Generate code for the given grammar element.
     * @param action The {...} action to generate
     */
    public void gen(ActionElement action) {
        if ( action.isSemPred ) {
            genSemPred(action.actionText, action.line);
        } else {
            if ( grammar.hasSyntacticPredicate ) {
                println("if ( inputState.guessing==0 ) {");
                tabs++;
            }

            String actionStr = processActionForTreeSpecifiers(action.actionText, action.getLine(), currentRule);

            // dump the translated action
            printAction(actionStr);

            if ( grammar.hasSyntacticPredicate ) {
                tabs--;
                println("}");
            }
        }
    }
    /**
     * Generate code for the given grammar element.
     * @param blk The "x|y|z|..." block to generate
     */
    public void gen(AlternativeBlock blk) {
        println("{");
        genBlockPreamble(blk);

        // Tell AST generation to build subrule result
        String saveCurrentASTResult = currentASTResult;
        if (blk.getLabel() != null) {
            currentASTResult = blk.getLabel();
        }

        boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

        JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, true);
        genBlockFinish(howToFinish, throwNoViable);

        println("}");

        // Restore previous AST generation
        currentASTResult = saveCurrentASTResult;
    }
    /**
     * Generate code for the given grammar element.
     * @param end The block-end element to generate.  Block-end
     * elements are synthesized by the grammar parser to represent
     * the end of a block.
     */
    public void gen(BlockEndElement end) {
    }
    /**
     * Generate code for the given grammar element.
     * @param atom The character literal reference to generate
     */
    public void gen(CharLiteralElement atom) {
        if ( atom.getLabel()!=null ) {
            println(atom.getLabel() + " = " + lt1Value + ";");
        }

        genMatch(atom);
    }
    /**
     * Generate code for the given grammar element.
     * @param r The character-range reference to generate
     */
    public void gen(CharRangeElement r) {
        if ( r.getLabel()!=null  && syntacticPredLevel == 0) {
            println(r.getLabel() + " = " + lt1Value + ";");
        }
        println("matchRange("+r.beginText+","+r.endText+");");
    }
    /**
     * Generate the lexer Java file
     */
    public  void gen(LexerGrammar g) throws IOException {
        setGrammar(g);
        if (!(grammar instanceof LexerGrammar)) {
            Utils.panic("Internal error generating lexer");
        }

        // SAS: moved output creation to method so a subclass can change
        //      how the output is generated (for VAJ interface)
        setupOutput(grammar.getClassName());

        saveText = true;    // save consumed characters.

        tabs=0;

        // Generate header common to all Java output files
        genHeader();
        // Do not use printAction because we assume tabs==0
        println(behavior.getHeaderAction(""));

        // Generate header specific to lexer Java file
        // println("import java.io.FileInputStream;");
        println("import java.io.InputStream;");
        println("import java.io.Reader;");
        println("import java.util.Hashtable;");
        println("import at.dms.compiler.antlr.runtime.*;");

        // Generate user-defined lexer file preamble
        println(grammar.preambleAction.getText());

        // Generate lexer class definition
        String sup=null;
        if ( grammar.superClass!=null ) {
            sup = grammar.superClass;
        } else {
            sup = "at.dms.compiler.antlr.runtime." + grammar.getSuperClass();
        }

        // print javadoc comment if any
        if ( grammar.comment!=null ) {
            _println(grammar.comment);
        }

        print("public class " + grammar.getClassName() + " extends "+sup);
        println(" implements " + grammar.tokenManager.getName() + TokenTypesFileSuffix+", TokenStream");
        Token tsuffix = grammar.options.get("classHeaderSuffix");
        if ( tsuffix != null ) {
            String suffix = Utils.stripFrontBack(tsuffix.getText(),"\"","\"");
            if ( suffix != null ) {
                print(", "+suffix); // must be an interface name for Java
            }
        }
        println(" {");

        // Generate user-defined lexer class members
        print(
              processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule)
              );

        //
        // Generate the constructor from InputStream, which in turn
        // calls the ByteBuffer constructor
        //
        println("public " + grammar.getClassName() + "(InputStream in) {");
        tabs++;
        println("this(new ByteBuffer(in));");
        tabs--;
        println("}");

        //
        // Generate the constructor from Reader, which in turn
        // calls the CharBuffer constructor
        //
        println("public " + grammar.getClassName() + "(Reader in) {");
        tabs++;
        println("this(new CharBuffer(in));");
        tabs--;
        println("}");

        println("public " + grammar.getClassName() + "(InputBuffer ib) {");
        tabs++;
        println("this(new LexerSharedInputState(ib));");
        tabs--;
        println("}");

        //
        // Generate the constructor from InputBuffer (char or byte)
        //
        println("public " + grammar.getClassName() + "(LexerSharedInputState state) {");
        tabs++;

        println("super(state);");

        // Generate the initialization of a hashtable
        // containing the string literals used in the lexer
        // The literals variable itself is in CharScanner
        println("literals = new Hashtable();");
        Enumeration<String> keys = grammar.tokenManager.getTokenSymbolKeys();
        while ( keys.hasMoreElements() ) {
            String key = keys.nextElement();
            if ( key.charAt(0) != '"' ) {
                continue;
            }
            TokenSymbol sym = grammar.tokenManager.getTokenSymbol(key);
            if ( sym instanceof StringLiteralSymbol ) {
                StringLiteralSymbol s = (StringLiteralSymbol)sym;
                println("literals.put(new ANTLRHashString(" + s.getId() + ", this), new Integer(" + s.getTokenType() + "));");
            }
        }
        tabs--;

        Enumeration ids;
        // Generate the setting of various generated options.
        println("caseSensitiveLiterals = " + g.caseSensitiveLiterals + ";");
        println("setCaseSensitive("+g.caseSensitive+");");
        println("}");

        // Generate nextToken() rule.
        // nextToken() is a synthetic lexer rule that is the implicit OR of all
        // user-defined lexer rules.
        genNextToken();

        // Generate code for each rule in the lexer
        ids = grammar.rules.elements();
        int ruleNum=0;
        while ( ids.hasMoreElements() ) {
            RuleSymbol sym = (RuleSymbol) ids.nextElement();
            // Don't generate the synthetic rules
            if (!sym.getId().equals("mnextToken")) {
                genRule(sym, false, ruleNum++);
            }
            exitIfError();
        }

        // Generate the bitsets used throughout the lexer
        genBitsets(bitsetsUsed, ((LexerGrammar)grammar).charVocabulary.size());

        println("");
        println("}");

        // Close the lexer output stream
        currentOutput.close();
        currentOutput = null;
    }
    /**
     * Generate code for the given grammar element.
     * @param blk The (...)+ block to generate
     */
    public void gen(OneOrMoreBlock blk) {
        String label;
        String cnt;
        println("{");
        genBlockPreamble(blk);
        if ( blk.getLabel() != null ) {
            cnt = "_cnt_"+blk.getLabel();
        } else {
            cnt = "_cnt" + blk.ID;
        }
        println("int "+cnt+"=0;");
        if ( blk.getLabel() != null ) {
            label = blk.getLabel();
        } else {
            label = "_loop" + blk.ID;
        }
        println(label+":");
        println("do {");
        tabs++;

        // Tell AST generation to build subrule result
        String saveCurrentASTResult = currentASTResult;
        if (blk.getLabel() != null) {
            currentASTResult = blk.getLabel();
        }

        boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

        // generate exit test if greedy set to false
        // and an alt is ambiguous with exit branch
        // or when lookahead derived purely from end-of-file
        // Lookahead analysis stops when end-of-file is hit,
        // returning set {epsilon}.  Since {epsilon} is not
        // ambig with any real tokens, no error is reported
        // by deterministic() routines and we have to check
        // for the case where the lookahead depth didn't get
        // set to NONDETERMINISTIC (this only happens when the
        // FOLLOW contains real atoms + epsilon).
        boolean generateNonGreedyExitPath = false;
        int nonGreedyExitDepth = grammar.maxk;

        if ( !blk.greedy &&
             blk.exitLookaheadDepth<=grammar.maxk &&
             blk.exitCache[blk.exitLookaheadDepth].containsEpsilon() )
            {
                generateNonGreedyExitPath = true;
                nonGreedyExitDepth = blk.exitLookaheadDepth;
            } else if ( !blk.greedy &&
                        blk.exitLookaheadDepth==LLkGrammarAnalyzer.NONDETERMINISTIC )
                {
                    generateNonGreedyExitPath = true;
                }

        // generate exit test if greedy set to false
        // and an alt is ambiguous with exit branch
        if ( generateNonGreedyExitPath ) {
            String predictExit =
                getLookaheadTestExpression(blk.exitCache,
                                           nonGreedyExitDepth);
            println("// nongreedy exit test");
            println("if ( "+cnt+">=1 && "+predictExit+") break "+label+";");
        }

        JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
        genBlockFinish(
                       howToFinish,
                       "if ( "+cnt+">=1 ) { break "+label+"; } else {" + throwNoViable + "}"
                       );

        println(cnt+"++;");
        tabs--;
        println("} while (true);");
        println("}");

        // Restore previous AST generation
        currentASTResult = saveCurrentASTResult;
    }
    /**
     * Generate the parser Java file
     */
    public void gen(ParserGrammar g) throws IOException {

        setGrammar(g);
        if (!(grammar instanceof ParserGrammar)) {
            Utils.panic("Internal error generating parser");
        }

        // Open the output stream for the parser and set the currentOutput
        // SAS: moved file setup so subclass could do it (for VAJ interface)
        setupOutput(grammar.getClassName());

        tabs = 0;

        // Generate the header common to all output files.
        genHeader();
        // Do not use printAction because we assume tabs==0
        println(behavior.getHeaderAction(""));

        // Generate header for the parser
        println("import at.dms.compiler.antlr.runtime.*;");

        // Output the user-defined parser preamble
        println(grammar.preambleAction.getText());

        // Generate parser class definition
        String sup=null;
        if ( grammar.superClass != null ) {
            sup = grammar.superClass;
        } else{
            sup = "at.dms.compiler.antlr.runtime." + grammar.getSuperClass();
        }

        // print javadoc comment if any
        if ( grammar.comment!=null ) {
            _println(grammar.comment);
        }

        println("public class " + grammar.getClassName() + " extends "+sup);
        println("       implements " + grammar.tokenManager.getName() + TokenTypesFileSuffix);

        Token tsuffix = grammar.options.get("classHeaderSuffix");
        if ( tsuffix != null ) {
            String suffix = Utils.stripFrontBack(tsuffix.getText(),"\"","\"");
            if ( suffix != null ) {
                print(", "+suffix); // must be an interface name for Java
            }
        }
        println(" {");

        // Generate user-defined parser class members
        print(
              processActionForTreeSpecifiers(grammar.classMemberAction.getText(), 0, currentRule)
              );

        if (grammar.superClass != null) {
            println("// Generated by at.dms.compiler.tools.antlr");
            println("private static final int MAX_LOOKAHEAD = " + grammar.maxk + ";");
            println("{");
            println("  tokenNames = _tokenNames;");
            println("}");
            println("// Generated by at.dms.compiler.tools.antlr");
            println("");
        } else {
            // Generate parser class constructor from TokenBuffer
            println("");
            println("protected " + grammar.getClassName() + "(TokenBuffer tokenBuf, int k) {");
            println("  super(tokenBuf,k);");
            println("  tokenNames = _tokenNames;");
            println("}");
            println("");

            println("public " + grammar.getClassName() + "(TokenBuffer tokenBuf) {");
            println("  this(tokenBuf," + grammar.maxk + ");");
            println("}");
            println("");

            // Generate parser class constructor from TokenStream
            println("protected " + grammar.getClassName()+"(TokenStream lexer, int k) {");
            println("  super(lexer,k);");
            println("  tokenNames = _tokenNames;");

            println("}");
            println("");

            println("public " + grammar.getClassName()+"(TokenStream lexer) {");
            println("  this(lexer," + grammar.maxk + ");");
            println("}");
            println("");

            println("public " + grammar.getClassName()+"(ParserSharedInputState state) {");
            println("  super(state," + grammar.maxk + ");");
            println("  tokenNames = _tokenNames;");
            println("}");
            println("");
        }

        // Generate code for each rule in the grammar
        Enumeration ids = grammar.rules.elements();
        int ruleNum=0;
        while ( ids.hasMoreElements() ) {
            GrammarSymbol sym = (GrammarSymbol) ids.nextElement();
            if ( sym instanceof RuleSymbol) {
                RuleSymbol rs = (RuleSymbol)sym;
                genRule(rs, rs.references.size()==0, ruleNum++);
            }
            exitIfError();
        }

        // Generate the token names
        genTokenStrings();

        // Generate the bitsets used throughout the grammar
        genBitsets(bitsetsUsed, grammar.tokenManager.maxTokenType());

        // Close class definition
        println("");
        println("}");

        // Close the parser output stream
        currentOutput.close();
        currentOutput = null;
    }
    /**
     * Generate code for the given grammar element.
     * @param rr The rule-reference to generate
     */
    public void gen(RuleRefElement rr) {
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
        if (rs == null || !rs.isDefined()) {
            // Is this redundant???
            tool.error("Rule '" + rr.targetRule + "' is not defined", grammar.getFilename(), rr.getLine());
            return;
        }

        genErrorTryForElement(rr);

        // if in lexer and ! on rule ref or alt or rule, save buffer index to kill later
        if ( grammar instanceof LexerGrammar && !saveText) {
            println("_saveIndex=text.length();");
        }

        // Process return value assignment if any
        printTabs();
        if (rr.idAssign != null) {
            // Warn if the rule has no return type
            if (rs.block.returnAction == null) {
                Utils.warning("Rule '" + rr.targetRule + "' has no return type", grammar.getFilename(), rr.getLine());
            }
            _print(rr.idAssign + "=");
        } else {
            // Warn about return value if any, but not inside syntactic predicate
            if ( !(grammar instanceof LexerGrammar) && syntacticPredLevel == 0 && rs.block.returnAction != null) {
                Utils.warning("Rule '" + rr.targetRule + "' returns a value", grammar.getFilename(), rr.getLine());
            }
        }

        // Call the rule
        GenRuleInvocation(rr);

        // if in lexer and ! on element or alt or rule, save buffer index to kill later
        if ( grammar instanceof LexerGrammar && !saveText) {
            println("text.setLength(_saveIndex);");
        }

        // if not in a syntactic predicate
        if (syntacticPredLevel == 0) {
            boolean doNoGuessTest = false;

            if (doNoGuessTest) {
                println("if (inputState.guessing==0) {");
                tabs++;
            }

            // if a lexer and labeled, Token label defined at rule level, just set it here
            if ( grammar instanceof LexerGrammar && rr.getLabel() != null ) {
                println(rr.getLabel()+"=_returnToken;");
            }

            if (doNoGuessTest) {
                tabs--;
                println("}");
            }
        }
        genErrorCatchForElement(rr);
    }
    /**
     * Generate code for the given grammar element.
     * @param atom The string-literal reference to generate
     */
    public void gen(StringLiteralElement atom) {
        // Variable declarations for labeled elements
        if (atom.getLabel()!=null && syntacticPredLevel == 0) {
            println(atom.getLabel() + " = " + lt1Value + ";");
        }

        // matching
        genMatch(atom);
    }

    /**
     * Generate code for the given grammar element.
     * @param r The token-range reference to generate
     */
    public void gen(TokenRangeElement r) {
        genErrorTryForElement(r);
        if ( r.getLabel()!=null  && syntacticPredLevel == 0) {
            println(r.getLabel() + " = " + lt1Value + ";");
        }

        // match
        println("matchRange("+r.beginText+","+r.endText+");");
        genErrorCatchForElement(r);
    }

    /**
     * Generate code for the given grammar element.
     * @param atom The token-reference to generate
     */
    public void gen(TokenRefElement atom) {
        if ( grammar instanceof LexerGrammar ) {
            Utils.panic("Token reference found in lexer");
        }
        genErrorTryForElement(atom);
        // Assign Token value to token label variable
        if ( atom.getLabel()!=null && syntacticPredLevel == 0) {
            println(atom.getLabel() + " = " + lt1Value + ";");
        }

        // matching
        genMatch(atom);
        genErrorCatchForElement(atom);
    }

    /**
     * Generate code for the given grammar element.
     * @param wc The wildcard element to generate
     */
    public void gen(WildcardElement wc) {
        // Variable assignment for labeled elements
        if (wc.getLabel()!=null && syntacticPredLevel == 0) {
            println(wc.getLabel() + " = " + lt1Value + ";");
        }

        // Match anything but EOF
        if (grammar instanceof LexerGrammar) {
            if (!saveText) {
                println("_saveIndex=text.length();");
            }
            println("matchNot(EOF_CHAR);");
            if (!saveText) {
                println("text.setLength(_saveIndex);"); // kill text atom put in buffer
            }
        } else {
            println("matchNot(" + getValueString(Token.EOF_TYPE) + ");");
        }
    }

    /**
     * Generate code for the given grammar element.
     * @param blk The (...)* block to generate
     */
    public void gen(ZeroOrMoreBlock blk) {
        println("{");
        genBlockPreamble(blk);
        String label;
        if ( blk.getLabel() != null ) {
            label = blk.getLabel();
        } else {
            label = "_loop" + blk.ID;
        }
        println(label+":");
        println("do {");
        tabs++;

        // Tell AST generation to build subrule result
        String saveCurrentASTResult = currentASTResult;
        if (blk.getLabel() != null) {
            currentASTResult = blk.getLabel();
        }

        boolean ok = grammar.theLLkAnalyzer.deterministic(blk);

        // generate exit test if greedy set to false
        // and an alt is ambiguous with exit branch
        // or when lookahead derived purely from end-of-file
        // Lookahead analysis stops when end-of-file is hit,
        // returning set {epsilon}.  Since {epsilon} is not
        // ambig with any real tokens, no error is reported
        // by deterministic() routines and we have to check
        // for the case where the lookahead depth didn't get
        // set to NONDETERMINISTIC (this only happens when the
        // FOLLOW contains real atoms + epsilon).
        boolean generateNonGreedyExitPath = false;
        int nonGreedyExitDepth = grammar.maxk;

        if ( !blk.greedy &&
             blk.exitLookaheadDepth<=grammar.maxk &&
             blk.exitCache[blk.exitLookaheadDepth].containsEpsilon() )
            {
                generateNonGreedyExitPath = true;
                nonGreedyExitDepth = blk.exitLookaheadDepth;
            } else if ( !blk.greedy &&
                        blk.exitLookaheadDepth==LLkGrammarAnalyzer.NONDETERMINISTIC )
                {
                    generateNonGreedyExitPath = true;
                }
        if ( generateNonGreedyExitPath ) {
            String predictExit =
                getLookaheadTestExpression(blk.exitCache,
                                           nonGreedyExitDepth);
            println("// nongreedy exit test");
            println("if ("+predictExit+") { break " + label + "; }");
        }

        JavaBlockFinishingInfo howToFinish = genCommonBlock(blk, false);
        genBlockFinish(howToFinish, "break " + label + ";");

        tabs--;
        println("} while (true);");
        println("}");

        // Restore previous AST generation
        currentASTResult = saveCurrentASTResult;
    }

    /**
     * Generate an alternative.
     * @param alt  The alternative to generate
     * @param blk The block to which the alternative belongs
     */
    private void genAlt(Alternative alt, AlternativeBlock blk) {
        boolean oldsaveTest = saveText;
        saveText = saveText && alt.getAutoGen();

        // Generate try block around the alt for  error handling
        if (alt.exceptionSpec != null) {
            println("try {      // for error handling");
            tabs++;
        }

        AlternativeElement elem = alt.head;
        while ( !(elem instanceof BlockEndElement) ) {
            elem.generate(this); // alt can begin with anything. Ask target to gen.
            elem = elem.next;
        }

        if (alt.exceptionSpec != null) {
            // close try block
            tabs--;
            println("}");
            genErrorHandler(alt.exceptionSpec);
        }

        saveText = oldsaveTest;
    }

    /**
     * Generate all the bitsets to be used in the parser or lexer
     * Generate the raw bitset data like "long[] _tokenSet1_data = {...};"
     * and the BitSet object declarations like "BitSet _tokenSet1 = new BitSet(_tokenSet1_data);"
     * Note that most languages do not support object initialization inside a
     * class definition, so other code-generators may have to separate the
     * bitset declarations from the initializations (e.g., put the initializations
     * in the generated constructor instead).
     * @param bitsetList The list of bitsets to generate.
     * @param maxVocabulary Ensure that each generated bitset can contain at least this value.
     */
    private void genBitsets(
                            Vector bitsetList,
                            int maxVocabulary
                            ) {
        println("");
        for (int i = 0; i < bitsetList.size(); i++) {
            BitSet p = (BitSet)bitsetList.elementAt(i);
            // Ensure that generated BitSet is large enough for vocabulary
            p.growToInclude(maxVocabulary);
            // initialization data
            println(
                    "private static final long[] " + getBitsetName(i) + "_data_ = { " +
                    p.toStringOfWords() +
                    " };"
                    );
            // BitSet object
            println(
                    "public static final BitSet " + getBitsetName(i) + " = new BitSet(" +
                    getBitsetName(i) + "_data_" +
                    ");"
                    );
        }
    }

    /**
     * Generate the finish of a block, using a combination of the info
     * returned from genCommonBlock() and the action to perform when
     * no alts were taken
     * @param howToFinish The return of genCommonBlock()
     * @param noViableAction What to generate when no alt is taken
     */
    private void genBlockFinish(JavaBlockFinishingInfo howToFinish, String noViableAction) {
        if (howToFinish.needAnErrorClause &&
            (howToFinish.generatedAnIf || howToFinish.generatedSwitch)) {
            if ( howToFinish.generatedAnIf ) {
                println("else {");
            } else {
                println("{");
            }
            tabs++;
            println(noViableAction);
            tabs--;
            println("}");
        }

        if ( howToFinish.postscript!=null ) {
            println(howToFinish.postscript);
        }
    }

    /**
     * Generate the header for a block, which may be a RuleBlock or a
     * plain AlternativeBLock.  This generates any variable declarations,
     * init-actions, and syntactic-predicate-testing variables.
     * @blk The block for which the preamble is to be generated.
     */
    private void genBlockPreamble(AlternativeBlock blk) {
        // define labels for rule blocks.
        if ( blk instanceof RuleBlock ) {
            RuleBlock rblk = (RuleBlock)blk;
            if ( rblk.labeledElements!=null ) {
                for (int i=0; i<rblk.labeledElements.size(); i++) {

                    AlternativeElement a = (AlternativeElement)rblk.labeledElements.elementAt(i);
                    //System.out.println("looking at labeled element: "+a);
                    //Variables for labeled rule refs and
                    //subrules are different than variables for
                    //grammar atoms.  This test is a little tricky
                    //because we want to get all rule refs and ebnf,
                    //but not rule blocks or syntactic predicates
                    if (
                        a instanceof RuleRefElement ||
                        a instanceof AlternativeBlock &&
                        !(a instanceof RuleBlock) &&
                        !(a instanceof SynPredBlock)
                        ) {

                        if (
                            !(a instanceof RuleRefElement) &&
                            ((AlternativeBlock)a).not &&
                            analyzer.subruleCanBeInverted(((AlternativeBlock)a), grammar instanceof LexerGrammar)
                            ) {
                            // Special case for inverted subrules that
                            // will be inlined.  Treat these like
                            // token or char literal references
                            println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
                        } else {
                            if ( grammar instanceof LexerGrammar ) {
                                println("Token "+a.getLabel()+"=null;");
                            }
                        }
                    } else {
                        // It is a token or literal reference.  Generate the
                        // correct variable type for this grammar
                        println(labeledElementType + " " + a.getLabel() + " = " + labeledElementInit + ";");
                    }
                }
            }
        }

        // dump out init action
        if ( blk.initAction!=null ) {
            printAction(
                        processActionForTreeSpecifiers(blk.initAction, 0, currentRule)
                        );
        }
    }

    /**
     * Generate a series of case statements that implement a BitSet test.
     * @param p The Bitset for which cases are to be generated
     */
    private void genCases(BitSet p) {
        int[] elems;

        elems = p.toArray();
        // Wrap cases four-per-line for lexer, one-per-line for parser
        int wrap = (grammar instanceof LexerGrammar) ? 4 : 1;
        int j=1;
        boolean startOfLine = true;
        for (int i = 0; i < elems.length; i++) {
            if (j==1) {
                print("");
            } else {
                _print("  ");
            }
            _print("case " + getValueString(elems[i]) + ":");

            if (j==wrap) {
                _println("");
                startOfLine = true;
                j=1;
            } else {
                j++;
                startOfLine = false;
            }
        }
        if (!startOfLine) {
            _println("");
        }
    }

    /**
     * Generate common code for a block of alternatives; return a
     * postscript that needs to be generated at the end of the
     * block.  Other routines may append else-clauses and such for
     * error checking before the postfix is generated.  If the
     * grammar is a lexer, then generate alternatives in an order
     * where alternatives requiring deeper lookahead are generated
     * first, and EOF in the lookahead set reduces the depth of
     * the lookahead.  @param blk The block to generate @param
     * noTestForSingle If true, then it does not generate a test
     * for a single alternative.
     */
    public JavaBlockFinishingInfo genCommonBlock(AlternativeBlock blk,
                                                 boolean noTestForSingle)
    {
        int nIF=0;
        boolean createdLL1Switch = false;
        int closingBracesOfIFSequence = 0;
        JavaBlockFinishingInfo finishingInfo = new JavaBlockFinishingInfo();

        boolean oldsaveTest = saveText;
        saveText = saveText && blk.getAutoGen();

        // Is this block inverted?  If so, generate special-case code
        if (
            blk.not &&
            analyzer.subruleCanBeInverted(blk, grammar instanceof LexerGrammar)
            ) {
            Lookahead p = analyzer.look(1, blk);
            // Variable assignment for labeled elements
            if (blk.getLabel() != null && syntacticPredLevel == 0) {
                println(blk.getLabel() + " = " + lt1Value + ";");
            }

            // match the bitset for the alternative
            println("match(" + getBitsetName(markBitsetForGen(p.fset)) + ");");

            return finishingInfo;
        }

        // Special handling for single alt
        if (blk.getAlternatives().size() == 1) {
            Alternative alt = blk.getAlternativeAt(0);
            // Generate a warning if there is a synPred for single alt.
            if (alt.synPred != null) {
                Utils.warning(
                              "Syntactic predicate superfluous for single alternative",
                              grammar.getFilename(),
                              blk.getAlternativeAt(0).synPred.getLine()
                              );
            }
            if (noTestForSingle) {
                if (alt.semPred != null) {
                    // Generate validating predicate
                    genSemPred(alt.semPred, blk.line);
                }
                genAlt(alt, blk);
                return finishingInfo;
            }
        }

        // count number of simple LL(1) cases; only do switch for
        // many LL(1) cases (no preds, no end of token refs)
        // We don't care about exit paths for (...)*, (...)+
        // because we don't explicitly have a test for them
        // as an alt in the loop.
        //
        // Also, we now count how many unicode lookahead sets
        // there are--they must be moved to DEFAULT or ELSE
        // clause.
        int nLL1 = 0;
        for (int i=0; i<blk.getAlternatives().size(); i++) {
            Alternative a = blk.getAlternativeAt(i);
            if ( suitableForCaseExpression(a) ) {
                nLL1++;
            }
        }

        // do LL(1) cases
        if ( nLL1 >= makeSwitchThreshold) {
            // Determine the name of the item to be compared
            String testExpr = lookaheadString(1);
            createdLL1Switch = true;
            println("switch ( "+testExpr+") {");
            for (int i=0; i<blk.alternatives.size(); i++) {
                Alternative alt = blk.getAlternativeAt(i);
                // ignore any non-LL(1) alts, predicated alts,
                // or end-of-token alts for case expressions
                if ( !suitableForCaseExpression(alt) ) {
                    continue;
                }
                Lookahead p = alt.cache[1];
                if (p.fset.degree() == 0 && !p.containsEpsilon()) {
                    Utils.warning("Alternate omitted due to empty prediction set",
                                  grammar.getFilename(),
                                  alt.head.getLine());
                } else {
                    genCases(p.fset);
                    println("{");
                    tabs++;
                    genAlt(alt, blk);
                    println("break;");
                    tabs--;
                    println("}");
                }
            }
            println("default:");
            tabs++;
        }

        // do non-LL(1) and nondeterministic cases This is tricky in
        // the lexer, because of cases like: STAR : '*' ; ASSIGN_STAR
        // : "*="; Since nextToken is generated without a loop, then
        // the STAR will have end-of-token as it's lookahead set for
        // LA(2).  So, we must generate the alternatives containing
        // trailing end-of-token in their lookahead sets *after* the
        // alternatives without end-of-token.  This implements the
        // usual lexer convention that longer matches come before
        // shorter ones, e.g.  "*=" matches ASSIGN_STAR not STAR
        //
        // For non-lexer grammars, this does not sort the alternates
        // by depth Note that alts whose lookahead is purely
        // end-of-token at k=1 end up as default or else clauses.
        int startDepth = (grammar instanceof LexerGrammar) ? grammar.maxk : 0;
        for (int altDepth = startDepth; altDepth >= 0; altDepth--) {
            for (int i=0; i<blk.alternatives.size(); i++) {
                Alternative alt = blk.getAlternativeAt(i);
                // if we made a switch above, ignore what we already took care
                // of.  Specifically, LL(1) alts with no preds
                // that do not have end-of-token in their prediction set
                // and that are not giant unicode sets.
                if ( createdLL1Switch && suitableForCaseExpression(alt) ) {
                    continue;
                }
                String e;

                boolean unpredicted = false;

                if (grammar instanceof LexerGrammar) {
                    // Calculate the "effective depth" of the alt,
                    // which is the max depth at which
                    // cache[depth]!=end-of-token
                    int effectiveDepth = alt.lookaheadDepth;
                    if (effectiveDepth == GrammarAnalyzer.NONDETERMINISTIC) {
                        // use maximum lookahead
                        effectiveDepth = grammar.maxk;
                    }
                    while ( effectiveDepth >= 1 &&
                            alt.cache[effectiveDepth].containsEpsilon() ) {
                        effectiveDepth--;
                    }
                    // Ignore alts whose effective depth is other than
                    // the ones we are generating for this iteration.
                    if (effectiveDepth != altDepth) {
                        continue;
                    }
                    unpredicted = lookaheadIsEmpty(alt, effectiveDepth);
                    e = getLookaheadTestExpression(alt, effectiveDepth);
                } else {
                    unpredicted = lookaheadIsEmpty(alt, grammar.maxk);
                    e = getLookaheadTestExpression(alt, grammar.maxk);
                }

                // Was it a big unicode range that forced unsuitability
                // for a case expression?
                if ( alt.cache[1].fset.degree()>caseSizeThreshold ) {
                    if ( nIF==0 ) {
                        println("if " + e + " {");
                    } else {
                        println("else if " + e + " {");
                    }
                } else if (unpredicted &&
                           alt.semPred==null &&
                           alt.synPred==null) {
                    // The alt has empty prediction set and no
                    // predicate to help out.  if we have not
                    // generated a previous if, just put {...} around
                    // the end-of-token clause
                    if ( nIF==0 ) {
                        println("{");
                    } else {
                        println("else {");
                    }
                    finishingInfo.needAnErrorClause = false;
                } else { // check for sem and syn preds

                    // Add any semantic predicate expression to the
                    // lookahead test
                    if ( alt.semPred != null ) {
                        // if debugging, wrap the evaluation of the
                        // predicate in a method translate $ and #
                        // references
                        String actionStr =
                            processActionForTreeSpecifiers(alt.semPred,
                                                           blk.line,
                                                           currentRule);
                        e = "("+e+"&&("+actionStr +"))";
                    }

                    // Generate any syntactic predicates
                    if ( nIF>0 ) {
                        if ( alt.synPred != null ) {
                            println("else {");
                            tabs++;
                            genSynPred( alt.synPred, e );
                            closingBracesOfIFSequence++;
                        } else {
                            println("else if " + e + " {");
                        }
                    } else {
                        if ( alt.synPred != null ) {
                            genSynPred( alt.synPred, e );
                        } else {
                            println("if " + e + " {");
                        }
                    }
                }

                nIF++;
                tabs++;
                genAlt(alt, blk);
                tabs--;
                println("}");
            }
        }
        String ps = "";
        for (int i=1; i<=closingBracesOfIFSequence; i++) {
            ps+="}";
        }

        // restore save text state
        saveText=oldsaveTest;

        // Return the finishing info.
        if ( createdLL1Switch ) {
            tabs--;
            finishingInfo.postscript = ps+"}";
            finishingInfo.generatedSwitch = true;
            finishingInfo.generatedAnIf = nIF>0;
            //return new JavaBlockFinishingInfo(ps+"}",true,nIF>0); // close up switch statement
        } else {
            finishingInfo.postscript = ps;
            finishingInfo.generatedSwitch = false;
            finishingInfo.generatedAnIf = nIF>0;
            // return new JavaBlockFinishingInfo(ps, false,nIF>0);
        }
        return finishingInfo;
    }

    private static boolean suitableForCaseExpression(Alternative a) {
        return
            a.lookaheadDepth == 1 &&
            a.semPred == null &&
            !a.cache[1].containsEpsilon() &&
            a.cache[1].fset.degree()<=caseSizeThreshold;
    }

    /**
     * Close the try block and generate catch phrases
     * if the element has a labeled handler in the rule
     */
    private void genErrorCatchForElement(AlternativeElement el) {
        if (el.getLabel() == null) {
            return;
        }
        String r = el.enclosingRuleName;
        if ( grammar instanceof LexerGrammar ) {
            r = JavaCodeGenerator.lexerRuleName(el.enclosingRuleName);
        }
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
        if (rs == null) {
            Utils.panic("Enclosing rule not found!");
        }
        ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
        if (ex != null) {
            tabs--;
            println("}");
            genErrorHandler(ex);
        }
    }

    /**
     * Generate the catch phrases for a user-specified error handler
     */
    private void genErrorHandler(ExceptionSpec ex) {
        // Each ExceptionHandler in the ExceptionSpec is a separate catch
        for (int i = 0; i < ex.handlers.size(); i++) {
            ExceptionHandler handler = (ExceptionHandler)ex.handlers.elementAt(i);
            // Generate catch phrase
            println("catch (" + handler.exceptionTypeAndName.getText() + ") {");
            tabs++;
            if (grammar.hasSyntacticPredicate) {
                println("if (inputState.guessing==0) {");
                tabs++;
            }

            // When not guessing, execute user handler action
            printAction(
                        processActionForTreeSpecifiers(handler.action.getText(), 0, currentRule)
                        );

            if (grammar.hasSyntacticPredicate) {
                tabs--;
                println("} else {");
                tabs++;
                // When guessing, rethrow exception
                println(
                        "throw " +
                        extractIdOfAction(handler.exceptionTypeAndName) +
                        ";"
                        );
                tabs--;
                println("}");
            }
            // Close catch phrase
            tabs--;
            println("}");
        }
    }
    /**
     * Generate a try { opening if the element has a labeled handler in the rule
     */
    private void genErrorTryForElement(AlternativeElement el) {
        if (el.getLabel() == null) {
            return;
        }
        String r = el.enclosingRuleName;
        if ( grammar instanceof LexerGrammar ) {
            r = JavaCodeGenerator.lexerRuleName(el.enclosingRuleName);
        }
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(r);
        if (rs == null) {
            Utils.panic("Enclosing rule not found!");
        }
        ExceptionSpec ex = rs.block.findExceptionSpec(el.getLabel());
        if (ex != null) {
            println("try { // for error handling");
            tabs++;
        }
    }

    /**
     * Generate a header that is common to all Java files
     */
    private void genHeader() {
        println("// $ANTLR "+tool.version+": "+
                "\""+Utils.fileMinusPath(tool.grammarFile)+"\""+
                " -> "+
                "\""+grammar.getClassName()+".java\"$");
    }

    private void genLiteralsTest() {
        println("_ttype = testLiteralsTable(_ttype);");
    }

    private void genLiteralsTestForPartialToken() {
        println("_ttype = testLiteralsTable(new String(text.getBuffer(),_begin,text.length()-_begin),_ttype);");
    }

    private void genMatch(BitSet b) {
    }

    private void genMatch(GrammarAtom atom) {
        if ( atom instanceof StringLiteralElement ) {
            if ( grammar instanceof LexerGrammar ) {
                genMatchUsingAtomText(atom);
            } else {
                genMatchUsingAtomTokenType(atom);
            }
        } else if ( atom instanceof CharLiteralElement ) {
            if ( grammar instanceof LexerGrammar ) {
                genMatchUsingAtomText(atom);
            } else {
                tool.error("cannot ref character literals in grammar: "+atom);
            }
        } else if ( atom instanceof TokenRefElement ) {
            genMatchUsingAtomText(atom);
        }
    }
    private void genMatchUsingAtomText(GrammarAtom atom) {
        // if in lexer and ! on element, save buffer index to kill later
        if ( grammar instanceof LexerGrammar && !saveText) {
            println("_saveIndex=text.length();");
        }

        print(atom.not ? "matchNot(" : "match(");
        _print("");

        // print out what to match
        if (atom.atomText.equals("EOF")) {
            // horrible hack to handle EOF case
            _print("Token.EOF_TYPE");
        } else {
            _print(atom.atomText);
        }
        _println(");");

        if ( grammar instanceof LexerGrammar && !saveText) {
            println("text.setLength(_saveIndex);");     // kill text atom put in buffer
        }
    }
    private void genMatchUsingAtomTokenType(GrammarAtom atom) {
        // If the literal can be mangled, generate the symbolic constant instead
        String mangledName = null;
        String s = getValueString(atom.getType());

        // matching
        println( (atom.not ? "matchNot(" : "match(") + s + ");");
    }

    /**
     * Generate the nextToken() rule.  nextToken() is a synthetic
     * lexer rule that is the implicit OR of all user-defined
     * lexer rules.
     */
    public void genNextToken() {
        // Are there any public rules?  If not, then just generate a
        // fake nextToken().
        boolean hasPublicRules = false;
        for (int i = 0; i < grammar.rules.size(); i++) {
            RuleSymbol rs = (RuleSymbol)grammar.rules.elementAt(i);
            if ( rs.isDefined() && rs.access.equals("public") ) {
                hasPublicRules = true;
                break;
            }
        }
        if (!hasPublicRules) {
            println("");
            println("public Token nextToken() throws TokenStreamException {");
            println("\ttry {uponEOF();}");
            println("\tcatch(CharStreamIOException csioe) {");
            println("\t\tthrow new TokenStreamIOException(csioe.io);");
            println("\t}");
            println("\tcatch(CharStreamException cse) {");
            println("\t\tthrow new TokenStreamException(cse.getMessage());");
            println("\t}");
            println("\treturn new CommonToken(Token.EOF_TYPE, \"\");");
            println("}");
            println("");
            return;
        }

        // Create the synthesized nextToken() rule
        RuleBlock nextTokenBlk = MakeGrammar.createNextTokenRule(grammar, grammar.rules, "nextToken");
        // Define the nextToken rule symbol
        RuleSymbol nextTokenRs = new RuleSymbol("mnextToken");
        nextTokenRs.setDefined();
        nextTokenRs.setBlock(nextTokenBlk);
        nextTokenRs.access = "private";
        grammar.define(nextTokenRs);
        // Analyze the nextToken rule
        boolean ok = grammar.theLLkAnalyzer.deterministic(nextTokenBlk);

        // Generate the next token rule
        String filterRule=null;
        if ( ((LexerGrammar)grammar).filterMode ) {
            filterRule = ((LexerGrammar)grammar).filterRule;
        }

        println("");
        println("public Token nextToken() throws TokenStreamException {");
        tabs++;
        println("Token theRetToken=null;");
        _println("tryAgain:");
        println("for (;;) {");
        tabs++;
        println("Token _token = null;");
        println("int _ttype = Token.INVALID_TYPE;");
        if ( ((LexerGrammar)grammar).filterMode ) {
            println("setCommitToPath(false);");
            if ( filterRule!=null ) {
                // Here's a good place to ensure that the filter rule actually exists
                if ( !grammar.isDefined(JavaCodeGenerator.lexerRuleName(filterRule)) ) {
                    grammar.tool.error("Filter rule "+filterRule+" does not exist in this lexer");
                } else {
                    RuleSymbol rs = (RuleSymbol)grammar.getSymbol(JavaCodeGenerator.lexerRuleName(filterRule));
                    if ( !rs.isDefined() ) {
                        grammar.tool.error("Filter rule "+filterRule+" does not exist in this lexer");
                    } else if ( rs.access.equals("public") ) {
                        grammar.tool.error("Filter rule "+filterRule+" must be protected");
                    }
                }
                println("int _m;");
                println("_m = mark();");
            }
        }
        println("resetText();");

        println("try {   // for char stream error handling");
        tabs++;

        // Generate try around whole thing to trap scanner errors
        println("try {   // for lexical error handling");
        tabs++;

        // Test for public lexical rules with empty paths
        for (int i=0; i<nextTokenBlk.getAlternatives().size(); i++) {
            Alternative a = nextTokenBlk.getAlternativeAt(i);
            if ( a.cache[1].containsEpsilon() ) {
                Utils.warning("found optional path in nextToken()");
            }
        }

        // Generate the block
        String newline = System.getProperty("line.separator");
        JavaBlockFinishingInfo howToFinish = genCommonBlock(nextTokenBlk, false);
        String errFinish = "if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}";
        errFinish += newline+"\t\t\t\t";
        if ( ((LexerGrammar)grammar).filterMode ) {
            if ( filterRule==null ) {
                errFinish += "else {consume(); continue tryAgain;}";
            } else {
                errFinish += "else {"+newline+
                    "\t\t\t\t\tcommit();"+newline+
                    "\t\t\t\t\ttry {m"+filterRule+"(false);}"+newline+
                    "\t\t\t\t\tcatch(RecognitionException e) {"+newline+
                    "\t\t\t\t\t // catastrophic failure"+newline+
                    "\t\t\t\t\t reportError(e);"+newline+
                    "\t\t\t\t\t consume();"+newline+
                    "\t\t\t\t\t}"+newline+
                    "\t\t\t\t\tcontinue tryAgain;"+newline+
                    "\t\t\t\t}";
            }
        } else {
            errFinish += "else {"+throwNoViable+"}";
        }
        genBlockFinish(howToFinish, errFinish);

        // at this point a valid token has been matched, undo "mark" that was done
        if ( ((LexerGrammar)grammar).filterMode && filterRule!=null ) {
            println("commit();");
        }

        // Generate literals test if desired
        // make sure _ttype is set first; note _returnToken must be
        // non-null as the rule was required to create it.
        println("if ( _returnToken==null ) { continue tryAgain; } // found SKIP token");
        println("_ttype = _returnToken.getType();");
        if ( ((LexerGrammar)grammar).getTestLiterals()) {
            genLiteralsTest();
        }

        // return token created by rule reference in switch
        println("_returnToken.setType(_ttype);");
        println("return _returnToken;");

        // Close try block
        tabs--;
        println("}");
        println("catch (RecognitionException e) {");
        tabs++;
        if ( ((LexerGrammar)grammar).filterMode ) {
            if ( filterRule==null ) {
                println("if ( !getCommitToPath() ) {consume(); continue tryAgain;}");
            } else {
                println("if ( !getCommitToPath() ) {");
                tabs++;
                println("rewind(_m);");
                println("resetText();");
                println("try {m"+filterRule+"(false);}");
                println("catch(RecognitionException ee) {");
                println("   // horrendous failure: error in filter rule");
                println("   reportError(ee);");
                println("   consume();");
                println("}");
                println("continue tryAgain;");
                tabs--;
                println("}");
            }
        }
        if ( nextTokenBlk.getDefaultErrorHandler() ) {
            println("reportError(e);");
            println("consume();");
        } else {
            // pass on to invoking routine
            println("throw new TokenStreamRecognitionException(e);");
        }
        tabs--;
        println("}");

        // close CharStreamException try
        tabs--;
        println("}");
        println("catch (CharStreamException cse) {");
        println("   if ( cse instanceof CharStreamIOException ) {");
        println("       throw new TokenStreamIOException(((CharStreamIOException)cse).io);");
        println("   }");
        println("   else {");
        println("       throw new TokenStreamException(cse.getMessage());");
        println("   }");
        println("}");

        // close for-loop
        tabs--;
        println("}");

        // close method nextToken
        tabs--;
        println("}");
        println("");
    }
    /**
     * Gen a named rule block.
     * ASTs are generated for each element of an alternative unless
     * the rule or the alternative have a '!' modifier.
     *
     * If an alternative defeats the default tree construction, it
     * must set <rule>_AST to the root of the returned AST.
     *
     * @param s The name of the rule to generate
     * @param startSymbol true if the rule is a start symbol (i.e., not referenced elsewhere)
     * @param ruleNum
     */
    public void genRule(RuleSymbol s, boolean startSymbol, int ruleNum) {
        tabs=1;
        if ( !s.isDefined() ) {
            tool.error("undefined rule: "+ s.getId());
            return;
        }

        // Generate rule return type, name, arguments
        RuleBlock rblk = s.getBlock();
        currentRule = rblk;
        currentASTResult = s.getId();

        // boolean oldsaveTest = saveText;
        saveText = rblk.getAutoGen();

        // print javadoc comment if any
        if ( s.comment!=null ) {
            _println(s.comment);
        }

        // Gen method access and final qualifier
        print(s.access + " final ");

        // Gen method return type (note lexer return action set at rule creation)
        if (rblk.returnAction != null) {
            // Has specified return value
            _print(extractTypeOfAction(rblk.returnAction, rblk.getLine()) + " ");
        } else {
            // No specified return value
            _print("void ");
        }

        // Gen method name
        _print(s.getId() + "(");

        // Additional rule parameters common to all rules for this grammar
        _print(commonExtraParams);
        if (commonExtraParams.length() != 0 && rblk.argAction != null ) {
            _print(",");
        }

        // Gen arguments
        if (rblk.argAction != null) {
            // Has specified arguments
            _println("");
            tabs++;
            println(rblk.argAction);
            tabs--;
            print(")");
        } else {
            // No specified arguments
            _print(")");
        }

        // Gen throws clause and open curly
        _print(" throws " + exceptionThrown);
        if ( grammar instanceof ParserGrammar ) {
            _print(", TokenStreamException");
        } else if ( grammar instanceof LexerGrammar ) {
            _print(", CharStreamException, TokenStreamException");
        }
        // Add user-defined exceptions unless lexer (for now)
        if ( rblk.throwsSpec!=null ) {
            if ( grammar instanceof LexerGrammar ) {
                tool.error("user-defined throws spec not allowed (yet) for lexer rule "+rblk.ruleName);
            } else {
                _print(", "+rblk.throwsSpec);
            }
        }

        _println(" {");
        tabs++;

        // Convert return action to variable declaration
        if (rblk.returnAction != null) {
            println(rblk.returnAction + ";");
        }

        // print out definitions needed by rules for various grammar types
        println(commonLocalVars);

        if ( grammar instanceof LexerGrammar ) {
            // lexer rule default return value is the rule's token name
            // This is a horrible hack to support the built-in EOF lexer rule.
            if (s.getId().equals("mEOF")) {
                println("_ttype = Token.EOF_TYPE;");
            } else {
                println("_ttype = "+ s.getId().substring(1)+";");
            }
            println("int _saveIndex;");     // used for element! (so we can kill text matched for element)
            /*
              println("boolean old_saveConsumedInput=saveConsumedInput;");
              if ( !rblk.getAutoGen() ) {       // turn off "save input" if ! on rule
              println("saveConsumedInput=false;");
              }
            */
        }

        genBlockPreamble(rblk);
        println("");

        // Search for an unlabeled exception specification attached to the rule
        ExceptionSpec unlabeledUserSpec = rblk.findExceptionSpec("");

        // Generate try block around the entire rule for  error handling
        if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
            println("try {      // for error handling");
            tabs++;
        }

        // Generate the alternatives
        if ( rblk.alternatives.size()==1 ) {
            // One alternative -- use simple form
            Alternative alt = rblk.getAlternativeAt(0);
            String pred = alt.semPred;
            if ( pred!=null ) {
                genSemPred(pred, currentRule.line);
            }
            if (alt.synPred != null) {
                Utils.warning(
                              "Syntactic predicate ignored for single alternative",
                              grammar.getFilename(), alt.synPred.getLine()
                              );
            }
            genAlt(alt, rblk);
        } else {
            // Multiple alternatives -- generate complex form
            boolean ok = grammar.theLLkAnalyzer.deterministic(rblk);

            JavaBlockFinishingInfo howToFinish = genCommonBlock(rblk, false);
            genBlockFinish(howToFinish, throwNoViable);
        }

        // Generate catch phrase for error handling
        if (unlabeledUserSpec != null || rblk.getDefaultErrorHandler() ) {
            // Close the try block
            tabs--;
            println("}");
        }

        // Generate user-defined or default catch phrases
        if (unlabeledUserSpec != null) {
            genErrorHandler(unlabeledUserSpec);
        } else if (rblk.getDefaultErrorHandler()) {
            // Generate default catch phrase
            println("catch (" + exceptionThrown + " ex) {");
            tabs++;
            // Generate code to handle error if not guessing
            if (grammar.hasSyntacticPredicate) {
                println("if (inputState.guessing==0) {");
                tabs++;
            }
            println("reportError(ex);");

            // Generate code to consume until token in k==1 follow set
            Lookahead follow = grammar.theLLkAnalyzer.FOLLOW(1, rblk.endNode);
            String followSetName = getBitsetName(markBitsetForGen(follow.fset));
            println("consume();");
            println("consumeUntil(" + followSetName + ");");

            if (grammar.hasSyntacticPredicate) {
                tabs--;
                // When guessing, rethrow exception
                println("} else {");
                println("  throw ex;");
                println("}");
            }
            // Close catch phrase
            tabs--;
            println("}");
        }

        // Generate literals test for lexer rules so marked
        if (rblk.getTestLiterals()) {
            if ( s.access.equals("protected") ) {
                genLiteralsTestForPartialToken();
            } else {
                genLiteralsTest();
            }
        }

        // if doing a lexer rule, dump code to create token if necessary
        if ( grammar instanceof LexerGrammar ) {
            println("if ( _createToken && _token==null && _ttype!=Token.SKIP ) {");
            println("   _token = makeToken(_ttype);");
            println("   _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));");
            println("}");
            println("_returnToken = _token;");
        }

        // Gen the return statement if there is one (lexer has hard-wired return action)
        if (rblk.returnAction != null) {
            println("return " + extractIdOfAction(rblk.returnAction, rblk.getLine()) + ";");
        }

        tabs--;
        println("}");
        println("");

        // restore char save state
        // saveText = oldsaveTest;
    }
    private void GenRuleInvocation(RuleRefElement rr) {
        // dump rule name
        _print(rr.targetRule + "(");

        // lexers must tell rule if it should set _returnToken
        if ( grammar instanceof LexerGrammar ) {
            // if labeled, could access Token, so tell rule to create
            if ( rr.getLabel() != null ) {
                _print("true");
            } else {
                _print("false");
            }
            if (commonExtraArgs.length() != 0 || rr.args!=null ) {
                _print(",");
            }
        }

        // Extra arguments common to all rules for this grammar
        _print(commonExtraArgs);
        if (commonExtraArgs.length() != 0 && rr.args!=null ) {
            _print(",");
        }

        // Process arguments to method, if any
        RuleSymbol rs = (RuleSymbol)grammar.getSymbol(rr.targetRule);
        if (rr.args != null) {
            // When not guessing, execute user arg action
            String args = processActionForTreeSpecifiers(rr.args, 0, currentRule);
            _print(args);

            // Warn if the rule accepts no arguments
            if (rs.block.argAction == null) {
                Utils.warning("Rule '" + rr.targetRule + "' accepts no arguments", grammar.getFilename(), rr.getLine());
            }
        } else {
            // For C++, no warning if rule has parameters, because there may be default
            // values for all of the parameters
            if (rs.block.argAction != null) {
                Utils.warning("Missing parameters on reference to rule "+rr.targetRule, grammar.getFilename(), rr.getLine());
            }
        }
        _println(");");
    }

    private void genSemPred(String pred, int line) {
        // translate $ and # references
        pred = processActionForTreeSpecifiers(pred, line, currentRule);
        // ignore translation info...we don't need to do anything with it.
        String escapedPred = charFormatter.escapeString(pred);

        println("if (!(" + pred + "))");
        println("  throw new SemanticException(\"" + escapedPred + "\");");
    }
    private void genSynPred(SynPredBlock blk, String lookaheadExpr) {
        // Dump synpred result variable
        println("boolean synPredMatched" + blk.ID + " = false;");
        // Gen normal lookahead test
        println("if (" + lookaheadExpr + ") {");
        tabs++;

        println("int _m" + blk.ID + " = mark();");

        // Once inside the try, assume synpred works unless exception caught
        println("synPredMatched" + blk.ID + " = true;");
        println("inputState.guessing++;");

        syntacticPredLevel++;
        println("try {");
        tabs++;
        gen((AlternativeBlock)blk);     // gen code to test predicate
        tabs--;
        //println("System.out.println(\"pred "+blk+" succeeded\");");
        println("}");
        println("catch (" + exceptionThrown + " pe) {");
        tabs++;
        println("synPredMatched"+blk.ID+" = false;");
        //println("System.out.println(\"pred "+blk+" failed\");");
        tabs--;
        println("}");

        // Restore input state
        println("rewind(_m"+blk.ID+");");

        println("inputState.guessing--;");

        syntacticPredLevel--;
        tabs--;

        // Close lookahead test
        println("}");

        // Test synred result
        println("if ( synPredMatched"+blk.ID+" ) {");
    }
    /**
     * Generate a static array containing the names of the tokens,
     * indexed by the token type values.  This static array is used
     * to format error messages so that the token identifers or literal
     * strings are displayed instead of the token numbers.
     *
     * If a lexical rule has a paraphrase, use it rather than the
     * token label.
     */
    public void genTokenStrings() {
        // Generate a string for each token.  This creates a static
        // array of Strings indexed by token type.
        println("");
        println("public static final String[] _tokenNames = {");
        tabs++;

        // Walk the token vocabulary and generate a Vector of strings
        // from the tokens.
        Vector v = grammar.tokenManager.getVocabulary();
        for (int i = 0; i < v.size(); i++) {
            String s = (String)v.elementAt(i);
            if (s == null) {
                s = "<"+String.valueOf(i)+">";
            }
            if ( !s.startsWith("\"") && !s.startsWith("<") ) {
                TokenSymbol ts = (TokenSymbol)grammar.tokenManager.getTokenSymbol(s);
                if ( ts!=null && ts.getParaphrase()!=null ) {
                    s = Utils.stripFrontBack(ts.getParaphrase(), "\"", "\"");
                }
            }
            print(charFormatter.literalString(s));
            if (i != v.size()-1) {
                _print(",");
            }
            _println("");
        }

        // Close the string array initailizer
        tabs--;
        println("};");
    }
    /**
     * Generate the token types Java file
     */
    private void genTokenTypes(TokenManager tm) throws IOException {
        // Open the token output Java file and set the currentOutput stream
        // SAS: file open was moved to a method so a subclass can override
        //      This was mainly for the VAJ interface
        setupOutput(tm.getName() + TokenTypesFileSuffix);

        tabs = 0;

        // Generate the header common to all Java files
        genHeader();
        // Do not use printAction because we assume tabs==0
        println(behavior.getHeaderAction(""));

        // Encapsulate the definitions in an interface.  This can be done
        // because they are all constants.
        println("public interface " + tm.getName() + TokenTypesFileSuffix+" {");
        tabs++;


        // Generate a definition for each token type
        Vector v = tm.getVocabulary();

        // Do special tokens manually
        println("int EOF = " + Token.EOF_TYPE + ";");
        println("int NULL_TREE_LOOKAHEAD = " + Token.NULL_TREE_LOOKAHEAD + ";");

        for (int i = Token.MIN_USER_TYPE; i < v.size(); i++) {
            String s = (String)v.elementAt(i);
            if (s != null) {
                if ( s.startsWith("\"") ) {
                    // a string literal
                    StringLiteralSymbol sl = (StringLiteralSymbol)tm.getTokenSymbol(s);
                    if ( sl==null ) {
                        Utils.panic("String literal "+s+" not in symbol table");
                    } else if ( sl.label != null ) {
                        println("int " + sl.label + " = " + i + ";");
                    } else {
                        String mangledName = mangleLiteral(s);
                        if (mangledName != null) {
                            // We were able to create a meaningful mangled token name
                            println("int " + mangledName + " = " + i + ";");
                            // if no label specified, make the label equal to the mangled name
                            sl.label = mangledName;
                        } else {
                            println("// " + s + " = " + i);
                        }
                    }
                } else if ( !s.startsWith("<") ) {
                    println("int " + s + " = " + i + ";");
                }
            }
        }

        // Close the interface
        tabs--;
        println("}");

        // Close the tokens output file
        currentOutput.close();
        currentOutput = null;
        exitIfError();
    }

    private String getLookaheadTestExpression(Lookahead[] look, int k) {
        StringBuffer e = new StringBuffer(100);
        boolean first = true;

        e.append("(");
        for (int i = 1; i <= k; i++) {
            BitSet p = look[i].fset;
            if (!first) {
                e.append(") && (");
            }
            first = false;

            // Syn preds can yield <end-of-syn-pred> (epsilon) lookahead.
            // There is no way to predict what that token would be.  Just
            // allow anything instead.
            if (look[i].containsEpsilon()) {
                e.append("true");
            } else {
                e.append(getLookaheadTestTerm(i, p));
            }
        }
        e.append(")");

        return e.toString();
    }

    /**
     * Generate a lookahead test expression for an alternate.  This
     * will be a series of tests joined by '&amp;&amp;' and enclosed by '()',
     * the number of such tests being determined by the depth of the lookahead.
     */
    private String getLookaheadTestExpression(Alternative alt, int maxDepth) {
        int depth = alt.lookaheadDepth;
        if ( depth == GrammarAnalyzer.NONDETERMINISTIC ) {
            // if the decision is nondeterministic, do the best we can: LL(k)
            // any predicates that are around will be generated later.
            depth = grammar.maxk;
        }

        if ( maxDepth==0 ) {
            // empty lookahead can result from alt with sem pred
            // that can see end of token.  E.g., A : {pred}? ('a')? ;
            return "true";
        }


        /*
          boolean first = true;
          for (int i=1; i<=depth && i<=maxDepth; i++) {
          BitSet p = alt.cache[i].fset;
          if (!first) {
          e.append(") && (");
          }
          first = false;

          // Syn preds can yield <end-of-syn-pred> (epsilon) lookahead.
          // There is no way to predict what that token would be.  Just
          // allow anything instead.
          if ( alt.cache[i].containsEpsilon() ) {
          e.append("true");
          } else {
          e.append(getLookaheadTestTerm(i, p));
          }
          }

          e.append(")");
        */

        return "(" + getLookaheadTestExpression(alt.cache,depth) + ")";
    }

    /**
     * Generate a depth==1 lookahead test expression given the BitSet.
     * This may be one of:
     * 1) a series of 'x==X||' tests
     * 2) a range test using &gt;= &amp;&amp; &lt;= where possible,
     * 3) a bitset membership test for complex comparisons
     * @param k The lookahead level
     * @param p The lookahead set for level k
     */
    private String getLookaheadTestTerm(int k, BitSet p) {
        // Determine the name of the item to be compared
        String ts = lookaheadString(k);

        // Generate a range expression if possible
        int[] elems = p.toArray();
        if (elementsAreRange(elems)) {
            return getRangeExpression(k, elems);
        }

        // Generate a bitset membership test if possible
        StringBuffer e;
        int degree = p.degree();
        if ( degree == 0 ) {
            return "true";
        }

        if (degree >= bitsetTestThreshold) {
            int bitsetIdx = markBitsetForGen(p);
            return getBitsetName(bitsetIdx) + ".member(" + ts + ")";
        }

        // Otherwise, generate the long-winded series of "x==X||" tests
        e = new StringBuffer();
        for (int i = 0; i < elems.length; i++) {
            // Get the compared-to item (token or character value)
            String cs = getValueString(elems[i]);

            // Generate the element comparison
            if ( i>0 ) {
                e.append("||");
            }
            e.append(ts);
            e.append("==");
            e.append(cs);
        }
        return e.toString();
    }

    /**
     * Return an expression for testing a contiguous renage of elements
     * @param k The lookahead level
     * @param elems The elements representing the set, usually from BitSet.toArray().
     * @return String containing test expression.
     */
    public String getRangeExpression(int k, int[] elems) {
        if (!elementsAreRange(elems)) {
            Utils.panic("getRangeExpression called with non-range");
        }
        int begin = elems[0];
        int end = elems[elems.length-1];
        return
            "(" + lookaheadString(k) + " >= " + getValueString(begin) + " && " +
            lookaheadString(k) + " <= " + getValueString(end) + ")";
    }

    /**
     * getValueString: get a string representation of a token or char value
     * @param value The token or char value
     */
    private String getValueString(int value) {
        String cs;
        if ( grammar instanceof LexerGrammar ) {
            cs = charFormatter.literalChar(value);
        } else {
            TokenSymbol ts = grammar.tokenManager.getTokenSymbolAt(value);
            if ( ts == null ) {
                return ""+value; // return token type as string
                // Utils.panic("vocabulary for token type " + value + " is null");
            }
            String tId = ts.getId();
            if ( ts instanceof StringLiteralSymbol ) {
                // if string literal, use predefined label if any
                // if no predefined, try to mangle into LITERAL_xxx.
                // if can't mangle, use int value as last resort
                StringLiteralSymbol sl = (StringLiteralSymbol)ts;
                String label = sl.getLabel();
                if ( label!=null ) {
                    cs = label;
                } else {
                    cs = mangleLiteral(tId);
                    if (cs == null) {
                        cs = String.valueOf(value);
                    }
                }
            } else {
                cs = tId;
            }
        }
        return cs;
    }

    /**
     * Is the lookahead for this alt empty?
     */
    private boolean lookaheadIsEmpty(Alternative alt, int maxDepth) {
        int depth = alt.lookaheadDepth;
        if ( depth == GrammarAnalyzer.NONDETERMINISTIC ) {
            depth = grammar.maxk;
        }
        for (int i=1; i<=depth && i<=maxDepth; i++) {
            BitSet p = alt.cache[i].fset;
            if (p.degree() != 0) {
                return false;
            }
        }
        return true;
    }

    private String lookaheadString(int k) {
        return "LA(" + k + ")";
    }

    /**
     * Mangle a string literal into a meaningful token name.  This is
     * only possible for literals that are all characters.  The resulting
     * mangled literal name is literalsPrefix with the text of the literal
     * appended.
     * @return A string representing the mangled literal, or null if not possible.
     */
    private String mangleLiteral(String s) {
        String mangled = tool.literalsPrefix;
        for (int i = 1; i < s.length()-1; i++) {
            if (!Character.isLetter(s.charAt(i)) &&
                s.charAt(i) != '_') {
                return null;
            }
            mangled += s.charAt(i);
        }
        if (tool.upperCaseMangledLiterals ) {
            mangled = mangled.toUpperCase();
        }
        return mangled;
    }

    private void setupGrammarParameters(Grammar g) {
        if (g instanceof ParserGrammar) {
            labeledElementType = "Token ";
            labeledElementInit = "null";
            commonExtraArgs = "";
            commonExtraParams = "";
            commonLocalVars = "";
            lt1Value = "LT(1)";
            exceptionThrown = "RecognitionException";
            throwNoViable = "throw new NoViableAltException(LT(1), getFilename());";
        } else if (g instanceof LexerGrammar) {
            labeledElementType = "char ";
            labeledElementInit = "'\\0'";
            commonExtraArgs = "";
            commonExtraParams = "boolean _createToken";
            commonLocalVars = "int _ttype; Token _token=null; int _begin=text.length();";
            lt1Value = "LA(1)";
            exceptionThrown = "RecognitionException";
            throwNoViable = "throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());";
        } else {
            Utils.panic("Unknown grammar type");
        }
    }

    /**
     * This method exists so a subclass, namely VAJCodeGenerator,
     *  can open the file in its own evil way.  JavaCodeGenerator
     *  simply opens a text file...
     */
    public void setupOutput(String className) throws IOException {
        currentOutput = tool.openOutputFile(className + ".java");
    }
}
