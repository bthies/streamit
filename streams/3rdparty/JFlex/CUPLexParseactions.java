package JFlex;

import java.util.*;
import java.io.*;

/** Cup generated class to encapsulate user supplied action code.*/
class CUPLexParseactions {



  LexScan     scanner;
  CharClasses charClasses = new CharClasses(127);
  RegExps     regExps     = new RegExps();
  Macros      macros      = new Macros();
  Integer     stateNumber;
  Timer       t           = new Timer();
  EOFActions  eofActions  = new EOFActions();

  void fatalError(int message, int line, int col) {
    syntaxError(message, line, col);
    throw new GeneratorException();
  }

  void fatalError(int message) {
    fatalError(message, scanner.currentLine(), -1);
    throw new GeneratorException();
  }

  void syntaxError(int message) {
    Out.error(scanner.file, message, scanner.currentLine(), -1);
  }
  
  void syntaxError(int message, int line) {
    Out.error(scanner.file, message, line, -1);
  }

  void syntaxError(int message, int line, int col) {
    Out.error(scanner.file, message, line, col);
  }


  private boolean check(int type, char c) {
    switch (type) {
      case sym.JLETTERCLASS:
        return Character.isJavaIdentifierStart(c);
        
      case sym.JLETTERDIGITCLASS:
        return Character.isJavaIdentifierPart(c);
        
      case sym.LETTERCLASS:
        return Character.isLetter(c);
        
      case sym.DIGITCLASS:
        return Character.isDigit(c);
        
      case sym.UPPERCLASS: 
        return Character.isUpperCase(c);
        
      case sym.LOWERCLASS: 
        return Character.isLowerCase(c);
        
      default: return false;
    }
  }
  
  private Vector makePreClass(int type) {
    
    Vector result = new Vector();
    
    char c = 0;
    char start = 0;
    char last = charClasses.getMaxCharCode();
    
    boolean prev, current;
    
    prev = check(type,'\u0000');
    
    for (c = 1; c < last; c++) {
      
      current = check(type,c);
      
      if (!prev && current) start = c;
      if (prev && !current) {
        result.addElement(new Intervall(start, (char)(c-1)));
      }
      
      prev = current;
    }
    
    // the last iteration is moved out of the loop to
    // avoid an endless loop if last == maxCharCode and
    // last+1 == 0
    current = check(type,c);
    
    if (!prev && current) result.addElement(new Intervall(c,c));
    if (prev && current)  result.addElement(new Intervall(start, c));    
    if (prev && !current) result.addElement(new Intervall(start, (char)(c-1)));

    return result;
  }
  
  private RegExp makeRepeat(RegExp r, int n1, int n2, int line, int col) {

    if (n1 <= 0 && n2 <= 0) {
      syntaxError(ErrorMessages.REPEAT_ZERO, line, col);
      return null;
    }

    if (n1 > n2) {
      syntaxError(ErrorMessages.REPEAT_GREATER, line, col);
      return null;
    }
    
    int i;
    RegExp result;    

    if (n1 > 0) {
      result = r;
      n1--; n2--; // we need one concatenation less than the number of expressions to match
    }
    else {
      result = new RegExp1(sym.QUESTION,r);
      n2--;
    }

    for (i = 0; i < n1; i++) 
      result = new RegExp2(sym.CONCAT, result, r);
      
    n2-= n1;  
    for (i = 0; i < n2; i++)
      result = new RegExp2(sym.CONCAT, result, new RegExp1(sym.QUESTION,r));
    
    return result;
  }

  private RegExp makeCaseless(String s) {

    if ( s == null ) return null;

    if ( s.equals("") ) return new RegExp1(sym.STRING, s);

    RegExp result, union, r1, r2;
    Vector ccl;
    char lower, upper;

    upper = Character.toUpperCase( s.charAt(0) );
    lower = Character.toLowerCase( s.charAt(0) );
    
    ccl = new Vector();
    ccl.addElement( new Intervall(upper,upper) );
    ccl.addElement( new Intervall(lower,lower) );
    charClasses.makeClass( ccl );
    
    r1 = new RegExp1(sym.CHAR, new Character(upper)); 
    r2 = new RegExp1(sym.CHAR, new Character(lower)); 
      
    result = new RegExp2(sym.BAR, r1, r2);    
    
    for (int i = 1; i < s.length(); i++) {
      upper = Character.toUpperCase( s.charAt(i) );
      lower = Character.toLowerCase( s.charAt(i) );
      
      ccl = new Vector();
      ccl.addElement( new Intervall(upper,upper) );
      ccl.addElement( new Intervall(lower,lower) );
      charClasses.makeClass( ccl );
      
      r1 = new RegExp1(sym.CHAR, new Character(upper)); 
      r2 = new RegExp1(sym.CHAR, new Character(lower)); 
      
      union = new RegExp2(sym.BAR, r1, r2);    

      result = new RegExp2(sym.CONCAT, result, union);
    }
     
    return result;
  } 

  private RegExp makeCaseless(Character c) {
    char upper = c.toUpperCase( c.charValue() );
    char lower = c.toLowerCase( c.charValue() );

    Vector ccl = new Vector();
    ccl.addElement( new Intervall(upper,upper) );
    ccl.addElement( new Intervall(lower,lower) );
    charClasses.makeClass( ccl );
    
    RegExp r1 = new RegExp1(sym.CHAR, new Character(upper)); 
    RegExp r2 = new RegExp1(sym.CHAR, new Character(lower)); 

    return new RegExp2(sym.BAR, r1, r2);    
  }

  private RegExp makeNL() {
    Vector list = new Vector();
    list.addElement(new Intervall('\n','\r'));
    list.addElement(new Intervall('\u0085','\u0085'));
    list.addElement(new Intervall('\u2028','\u2029'));

    charClasses.makeClass(list);
    charClasses.makeClass('\n');
    charClasses.makeClass('\r');

    RegExp1   c = new RegExp1(sym.CCLASS, list);
    Character n = new Character('\n');
    Character r = new Character('\r');

    return new RegExp2(sym.BAR, 
                       c, 
                       new RegExp2(sym.CONCAT, 
                                   new RegExp1(sym.CHAR, r), 
                                   new RegExp1(sym.CHAR, n)));
  }
  

  private final LexParse parser;

  /** Constructor */
  CUPLexParseactions(LexParse parser) {
    this.parser = parser;
  }

  /** Method with the actual generated action code. */
  public final java_cup.runtime.Symbol CUPLexParsedo_action(
    int                        CUPLexParseact_num,
    java_cup.runtime.lr_parser CUPLexParseparser,
    java.util.Stack            CUPLexParsestack,
    int                        CUPLexParsetop)
    throws java.lang.Exception
    {
      /* Symbol object for return from actions */
      java_cup.runtime.Symbol CUPLexParseresult;

      /* select the action based on the action number */
      switch (CUPLexParseact_num)
        {
          /*. . . . . . . . . . . . . . . . . . . .*/
          case 69: // preclass ::= LOWERCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.LOWERCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 68: // preclass ::= UPPERCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.UPPERCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 67: // preclass ::= DIGITCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.DIGITCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 66: // preclass ::= LETTERCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.LETTERCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 65: // preclass ::= JLETTERDIGITCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.JLETTERDIGITCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 64: // preclass ::= JLETTERCLASS 
            {
              Vector RESULT = null;
		 RESULT = makePreClass(sym.JLETTERCLASS); 
              CUPLexParseresult = new java_cup.runtime.Symbol(15/*preclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 63: // classcontentelem ::= CHAR 
            {
              Intervall RESULT = null;
		int cleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int cright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Character c = (Character)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new Intervall(c.charValue(), c.charValue()); 
              CUPLexParseresult = new java_cup.runtime.Symbol(11/*classcontentelem*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 62: // classcontentelem ::= CHAR DASH CHAR 
            {
              Intervall RESULT = null;
		int c1left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int c1right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		Character c1 = (Character)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int c2left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int c2right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Character c2 = (Character)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new Intervall(c1.charValue(), c2.charValue()); 
              CUPLexParseresult = new java_cup.runtime.Symbol(11/*classcontentelem*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 61: // classcontent ::= MACROUSE 
            {
              Vector RESULT = null;
		int identleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int identright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		String ident = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     syntaxError(ErrorMessages.CHARCLASS_MACRO, identleft, identright);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 60: // classcontent ::= classcontent MACROUSE 
            {
              Vector RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int identleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int identright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		String ident = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     syntaxError(ErrorMessages.CHARCLASS_MACRO, identleft, identright);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 59: // classcontent ::= preclass 
            {
              Vector RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = list; 
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 58: // classcontent ::= classcontent preclass 
            {
              Vector RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int plistleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int plistright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Vector plist = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     for (Enumeration e = plist.elements(); e.hasMoreElements();)
                       list.addElement(e.nextElement());
                     RESULT = list;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 57: // classcontent ::= classcontentelem 
            {
              Vector RESULT = null;
		int elemleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int elemright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Intervall elem = (Intervall)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     Vector list = new Vector();
                     list.addElement(elem);
                     RESULT = list;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 56: // classcontent ::= classcontent classcontentelem 
            {
              Vector RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int elemleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int elemright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Intervall elem = (Intervall)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     list.addElement(elem);
                     RESULT = list;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(14/*classcontent*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 55: // charclass ::= OPENCLASS HAT DASH classcontent CLOSECLASS 
            {
              RegExp RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int closeleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int closeright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object close = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       list.addElement(new Intervall('-','-'));
                       charClasses.makeClassNot(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, closeleft, closeright);
                     }
                     RESULT = new RegExp1(sym.CCLASSNOT,list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 54: // charclass ::= OPENCLASS DASH classcontent CLOSECLASS 
            {
              RegExp RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int closeleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int closeright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object close = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       list.addElement(new Intervall('-','-'));
                       charClasses.makeClass(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, closeleft, closeright);
                     }
                     RESULT = new RegExp1(sym.CCLASS,list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 53: // charclass ::= OPENCLASS HAT classcontent CLOSECLASS 
            {
              RegExp RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int closeleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int closeright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object close = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       charClasses.makeClassNot(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, closeleft, closeright);
                     }
                     RESULT = new RegExp1(sym.CCLASSNOT,list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 52: // charclass ::= OPENCLASS HAT CLOSECLASS 
            {
              RegExp RESULT = null;
		int closeleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int closeright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object close = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     Vector list = new Vector();
                     list.addElement(new Intervall((char)0,charClasses.maxChar));
                     try {
                       charClasses.makeClass(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, closeleft, closeright);
                     }
                     RESULT = new RegExp1(sym.CCLASS,list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 51: // charclass ::= OPENCLASS classcontent CLOSECLASS 
            {
              RegExp RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int closeleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int closeright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object close = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       charClasses.makeClass(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, closeleft, closeright);
                     }
                     RESULT = new RegExp1(sym.CCLASS,list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 50: // charclass ::= OPENCLASS CLOSECLASS 
            {
              RegExp RESULT = null;
		 
                     RESULT = new RegExp1(sym.CCLASS,null);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(9/*charclass*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 49: // regexp ::= CHAR 
            {
              RegExp RESULT = null;
		int cleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int cright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Character c = (Character)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       if ( scanner.caseless ) {            
                         RESULT = makeCaseless( c );
                       }
                       else {
                         charClasses.makeClass( c.charValue() );
                         RESULT = new RegExp1(sym.CHAR, c); 
                       }
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CS2SMALL_CHAR, cleft, cright);
                     }

                   
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 48: // regexp ::= POINT 
            {
              RegExp RESULT = null;
		 
                      Vector any = new Vector();
                      any.addElement(new Intervall('\n','\n'));
                      charClasses.makeClass( '\n' );
                      RESULT = new RegExp1(sym.CCLASSNOT, any); 
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 47: // regexp ::= STRING 
            {
              RegExp RESULT = null;
		int strleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int strright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		String str = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                     try {
                       if ( scanner.caseless ) {
                         RESULT = makeCaseless(str);
                       }
                       else {
                         charClasses.makeClass( str );
                         RESULT = new RegExp1(sym.STRING, str); 
                       }
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CS2SMALL_STRING, strleft, strright);
                     }

                   
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 46: // regexp ::= preclass 
            {
              RegExp RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     try {
                       charClasses.makeClass(list);
                     }
                     catch (CharClassException e) {
                       syntaxError(ErrorMessages.CHARSET_2_SMALL, listleft);
                     }
                     RESULT = new RegExp1(sym.CCLASS, list);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 45: // regexp ::= charclass 
            {
              RegExp RESULT = null;
		int cleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int cright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp c = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = c; 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 44: // regexp ::= MACROUSE 
            {
              RegExp RESULT = null;
		int identleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int identright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		String ident = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 
                      if ( !scanner.macroDefinition ) {
                        if ( ! macros.markUsed(ident) ) 
                          throw new ScannerException(scanner.file, ErrorMessages.MACRO_UNDECL, 
                                                     identleft, identright);
                      }
                      RESULT = new RegExp1(sym.MACROUSE, ident); 
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 43: // regexp ::= OPENBRACKET series CLOSEBRACKET 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = r; 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 42: // regexp ::= regexp REPEAT REPEAT RBRACE 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-3)).value;
		int n1left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int n1right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		Integer n1 = (Integer)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int n2left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int n2right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Integer n2 = (Integer)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = makeRepeat(r, n1.intValue(), n2.intValue(), n1left, n2right); 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 41: // regexp ::= regexp REPEAT RBRACE 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int nleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int nright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Integer n = (Integer)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int bleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int bright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object b = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = makeRepeat(r, n.intValue(), n.intValue(), bleft, bright); 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 40: // regexp ::= regexp QUESTION 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = new RegExp1(sym.QUESTION, r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 39: // regexp ::= regexp PLUS 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = new RegExp1(sym.PLUS, r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 38: // regexp ::= regexp STAR 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = new RegExp1(sym.STAR, r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(8/*regexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 37: // nregexp ::= TILDE nregexp 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new RegExp1(sym.TILDE, r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(7/*nregexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 36: // nregexp ::= BANG nregexp 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new RegExp1(sym.BANG, r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(7/*nregexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 35: // nregexp ::= regexp 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = r; 
              CUPLexParseresult = new java_cup.runtime.Symbol(7/*nregexp*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 34: // concs ::= nregexp 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = r; 
              CUPLexParseresult = new java_cup.runtime.Symbol(6/*concs*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 33: // concs ::= concs nregexp 
            {
              RegExp RESULT = null;
		int r1left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int r1right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp r1 = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int r2left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int r2right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r2 = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new RegExp2(sym.CONCAT, r1, r2); 
              CUPLexParseresult = new java_cup.runtime.Symbol(6/*concs*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 32: // series ::= BAR 
            {
              RegExp RESULT = null;
		int bleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int bright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object b = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 syntaxError(ErrorMessages.REGEXP_EXPECTED, bleft, bright); 
              CUPLexParseresult = new java_cup.runtime.Symbol(5/*series*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 31: // series ::= concs 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = r; 
              CUPLexParseresult = new java_cup.runtime.Symbol(5/*series*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 30: // series ::= series BAR concs 
            {
              RegExp RESULT = null;
		int r1left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int r1right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		RegExp r1 = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int r2left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int r2right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r2 = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new RegExp2(sym.BAR, r1, r2); 
              CUPLexParseresult = new java_cup.runtime.Symbol(5/*series*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 29: // hatOPT ::= 
            {
              Boolean RESULT = null;
		 RESULT = new Boolean(false); 
              CUPLexParseresult = new java_cup.runtime.Symbol(17/*hatOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 28: // hatOPT ::= HAT 
            {
              Boolean RESULT = null;
		 charClasses.makeClass('\n');
                      RESULT = new Boolean(true); 
              CUPLexParseresult = new java_cup.runtime.Symbol(17/*hatOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 27: // states ::= IDENT COMMA 
            {
              Vector RESULT = null;
		int cleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int cright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object c = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 syntaxError(ErrorMessages.REGEXP_EXPECTED, cleft, cright+1); 
              CUPLexParseresult = new java_cup.runtime.Symbol(12/*states*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 26: // states ::= IDENT 
            {
              Vector RESULT = null;
		int idleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int idright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		String id = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     Vector list = new Vector();
                     stateNumber = scanner.states.getNumber( id );
                     if ( stateNumber != null )
                       list.addElement( stateNumber ); 
                     else {
                       throw new ScannerException(scanner.file, ErrorMessages.LEXSTATE_UNDECL, 
                                                  idleft, idright);
                     }
                     RESULT = list;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(12/*states*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 25: // states ::= IDENT COMMA states 
            {
              Vector RESULT = null;
		int idleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int idright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		String id = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		
                     stateNumber = scanner.states.getNumber( id );
                     if ( stateNumber != null )
                       list.addElement( stateNumber ); 
                     else {
                       throw new ScannerException(scanner.file, ErrorMessages.LEXSTATE_UNDECL, 
                                                  idleft, idright);
                     }
                     RESULT = list;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(12/*states*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 24: // statesOPT ::= 
            {
              Vector RESULT = null;
		 RESULT = new Vector(); 
              CUPLexParseresult = new java_cup.runtime.Symbol(13/*statesOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 23: // statesOPT ::= LESSTHAN states MORETHAN 
            {
              Vector RESULT = null;
		int listleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int listright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector list = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = list; 
              CUPLexParseresult = new java_cup.runtime.Symbol(13/*statesOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 22: // actions ::= NOACTION 
            {
              Action RESULT = null;
		 RESULT = null; 
              CUPLexParseresult = new java_cup.runtime.Symbol(18/*actions*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 21: // actions ::= REGEXPEND ACTION 
            {
              Action RESULT = null;
		int aleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int aright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Action a = (Action)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = a; 
              CUPLexParseresult = new java_cup.runtime.Symbol(18/*actions*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 20: // lookaheadOPT ::= LOOKAHEAD series DOLLAR 
            {
              RegExp RESULT = null;
		int sleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int sright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp s = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 RESULT = new RegExp2(sym.CONCAT, s, makeNL()); 
              CUPLexParseresult = new java_cup.runtime.Symbol(10/*lookaheadOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 19: // lookaheadOPT ::= 
            {
              RegExp RESULT = null;
		 RESULT = null; 
              CUPLexParseresult = new java_cup.runtime.Symbol(10/*lookaheadOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 18: // lookaheadOPT ::= LOOKAHEAD series 
            {
              RegExp RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = r; 
              CUPLexParseresult = new java_cup.runtime.Symbol(10/*lookaheadOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 17: // lookaheadOPT ::= DOLLAR 
            {
              RegExp RESULT = null;
		 RESULT = makeNL(); 
              CUPLexParseresult = new java_cup.runtime.Symbol(10/*lookaheadOPT*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 16: // rule ::= error 
            {
              Integer RESULT = null;

              CUPLexParseresult = new java_cup.runtime.Symbol(3/*rule*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 15: // rule ::= statesOPT EOFRULE ACTION 
            {
              Integer RESULT = null;
		int sleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int sright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		Vector s = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int aleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int aright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Action a = (Action)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new Integer(regExps.insert(s, a)); 
              CUPLexParseresult = new java_cup.runtime.Symbol(3/*rule*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 14: // rule ::= statesOPT hatOPT series lookaheadOPT actions 
            {
              Integer RESULT = null;
		int sleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).left;
		int sright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).right;
		Vector s = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-4)).value;
		int bolleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left;
		int bolright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).right;
		Boolean bol = (Boolean)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-3)).value;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-2)).right;
		RegExp r = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-2)).value;
		int lleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int lright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp l = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int aleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int aright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Action a = (Action)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new Integer(regExps.insert(rleft, s, r, a, bol, l)); 
              CUPLexParseresult = new java_cup.runtime.Symbol(3/*rule*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 13: // rules ::= rule 
            {
              Vector RESULT = null;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Integer r = (Integer)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 RESULT = new Vector(); RESULT.addElement(r); 
              CUPLexParseresult = new java_cup.runtime.Symbol(16/*rules*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 12: // rules ::= LESSTHAN states MORETHAN LBRACE rules RBRACE 
            {
              Vector RESULT = null;
		int statesleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).left;
		int statesright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).right;
		Vector states = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-4)).value;
		int rlistleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rlistright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector rlist = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 
                     Enumeration rs = rlist.elements();
                     while ( rs.hasMoreElements() ) {
                       Integer elem = (Integer) rs.nextElement();
                       regExps.addStates( elem.intValue(), states );
                     }                       
                     RESULT = rlist;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(16/*rules*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-5)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 11: // rules ::= rules LESSTHAN states MORETHAN LBRACE rules RBRACE 
            {
              Vector RESULT = null;
		int rlist1left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-6)).left;
		int rlist1right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-6)).right;
		Vector rlist1 = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-6)).value;
		int statesleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).left;
		int statesright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-4)).right;
		Vector states = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-4)).value;
		int rlist2left = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rlist2right = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector rlist2 = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 
                     Enumeration rs = rlist2.elements();
                     while ( rs.hasMoreElements() ) {
                       Integer elem = (Integer) rs.nextElement();
                       regExps.addStates( elem.intValue(), states );
                       rlist1.addElement( elem );
                     }                       
                     RESULT = rlist1;
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(16/*rules*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-6)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 10: // rules ::= rules rule 
            {
              Vector RESULT = null;
		int rlistleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int rlistright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		Vector rlist = (Vector)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		int rleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int rright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Integer r = (Integer)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 rlist.addElement(r); RESULT = rlist; 
              CUPLexParseresult = new java_cup.runtime.Symbol(16/*rules*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 9: // macro ::= IDENT EQUALS 
            {
              Object RESULT = null;
		int eleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left;
		int eright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right;
		Object e = (Object)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-0)).value;
		 syntaxError(ErrorMessages.REGEXP_EXPECTED, eleft, eright); 
              CUPLexParseresult = new java_cup.runtime.Symbol(2/*macro*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 8: // macro ::= IDENT EQUALS series REGEXPEND 
            {
              Object RESULT = null;
		int nameleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left;
		int nameright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).right;
		String name = (String)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-3)).value;
		int definitionleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int definitionright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		RegExp definition = (RegExp)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		 macros.insert(name, definition); 
              CUPLexParseresult = new java_cup.runtime.Symbol(2/*macro*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 7: // macro ::= UNICODE 
            {
              Object RESULT = null;
		 charClasses.setMaxCharCode(0xFFFF); 
              CUPLexParseresult = new java_cup.runtime.Symbol(2/*macro*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 6: // macro ::= FULL 
            {
              Object RESULT = null;
		 charClasses.setMaxCharCode(255); 
              CUPLexParseresult = new java_cup.runtime.Symbol(2/*macro*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 5: // macros ::= error 
            {
              Object RESULT = null;

              CUPLexParseresult = new java_cup.runtime.Symbol(1/*macros*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 4: // macros ::= macros macro 
            {
              Object RESULT = null;

              CUPLexParseresult = new java_cup.runtime.Symbol(1/*macros*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 3: // macros ::= 
            {
              Object RESULT = null;

              CUPLexParseresult = new java_cup.runtime.Symbol(1/*macros*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 2: // specification ::= 
            {
              NFA RESULT = null;
		 
                     fatalError(ErrorMessages.NO_LEX_SPEC);
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(4/*specification*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 1: // specification ::= USERCODE macros DELIMITER rules 
            {
              NFA RESULT = null;
		
                     scanner.t.stop();

                     Out.checkErrors();
                     
                     Out.time("Parsing took "+t);
                     
                     macros.expand();
                     Enumeration unused = macros.unused();                     
                     while ( unused.hasMoreElements() ) {
                       Out.warning("Macro \""+unused.nextElement()+"\" has been declared but never used.");
                     }

                     SemCheck.check(regExps, macros, charClasses.getMaxCharCode(), scanner.file);
  
                     regExps.checkActions();

                     if (Out.DUMP) charClasses.dump();

                     Out.print("Constructing NFA : ");

                     t.start();
                     int num = regExps.getNum();
                     
                     RESULT = new NFA(charClasses.getNumClasses(), 
                                      scanner, regExps, macros, charClasses);
                     
                     eofActions.setNumLexStates(scanner.states.number());

                     for (int i = 0; i < num; i++) {
                       if (regExps.isEOF(i))
                         eofActions.add( regExps.getStates(i), regExps.getAction(i) );
                       else
                         RESULT.addRegExp(i);
                     }
                     
                     if (scanner.standalone) RESULT.addStandaloneRule();
                     t.stop();
                     
                     Out.time(Out.NL+"NFA construction took "+t);
                     
                   
              CUPLexParseresult = new java_cup.runtime.Symbol(4/*specification*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-3)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          return CUPLexParseresult;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 0: // START ::= specification EOF 
            {
              Object RESULT = null;
		int start_valleft = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left;
		int start_valright = ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).right;
		NFA start_val = (NFA)((java_cup.runtime.Symbol) CUPLexParsestack.elementAt(CUPLexParsetop-1)).value;
		RESULT = start_val;
              CUPLexParseresult = new java_cup.runtime.Symbol(0/*START*/, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-1)).left, ((java_cup.runtime.Symbol)CUPLexParsestack.elementAt(CUPLexParsetop-0)).right, RESULT);
            }
          /* ACCEPT */
          CUPLexParseparser.done_parsing();
          return CUPLexParseresult;

          /* . . . . . .*/
          default:
            throw new Exception(
               "Invalid action number found in internal parse table");

        }
    }
}

