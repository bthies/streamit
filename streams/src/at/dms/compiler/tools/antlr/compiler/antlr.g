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
 * $Id: antlr.g,v 1.1 2001-08-30 16:32:36 thies Exp $
 */

header {
package at.dms.compiler.tools.antlr.compiler;
}

{
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
}

class ANTLRParser extends Parser;
options {
	exportVocab=ANTLR;
	defaultErrorHandler=false;
	k=2;
}

tokens {
	"tokens";
}

{
	ANTLRGrammarParseBehavior behavior;
	Main tool;
	protected int blockNesting= -1;

	public ANTLRParser(
		TokenBuffer tokenBuf,
		ANTLRGrammarParseBehavior behavior_,
		Main tool_
	) {
		super(tokenBuf, 1);
		tokenNames = _tokenNames;
		behavior = behavior_;
		tool = tool_;
	}

	private boolean lastInRule() throws TokenStreamException {
		if ( blockNesting==0 && (LA(1)==SEMI || LA(1)==LITERAL_exception || LA(1)==OR) ) {
			return true;
		}
		return false;
	}

	private void checkForMissingEndRule(Token label) {
		if ( label.getColumn()==1 ) {
			Utils.warning("did you forget to terminate previous rule?", getFilename(), label.getLine());
		}
	}
}

grammar
   :
	( "header" (n:STRING_LITERAL)? h:ACTION {behavior.refHeaderAction(n,h);} )*
	( fileOptionsSpec )?
	( classDef )*
	EOF
	;
	exception catch [RecognitionException ex] {
		reportError("rule grammar trapped: "+ex.toString());
		consumeUntil(EOF);
	}

classDef
{String doc=null;}
	:
	( a:ACTION { behavior.refPreambleAction(a);} )?
	( d:DOC_COMMENT {doc=d.getText();} )?
	(	("class" id "extends" "Lexer" ) => lexerSpec[doc]
	|	parserSpec[doc]
	)
	rules
	{ behavior.endGrammar(); }
	;
	exception catch [RecognitionException ex] {
		if ( ex instanceof NoViableAltException ) {
			NoViableAltException e = (NoViableAltException)ex;
			if ( e.token.getType()==DOC_COMMENT ) {
				reportError("line "+ex.line+": JAVADOC comments may only prefix rules and grammars");
			} else {
				reportError("rule classDef trapped: "+ex.toString());
			}
		} else {
			reportError("rule classDef trapped: "+ex.toString());
		}
		behavior.abortGrammar();
		boolean consuming = true;
		// consume everything until the next class definition or EOF
		while (consuming) {
			consume();
			switch(LA(1)) {
			case LITERAL_class:
			case EOF:
				consuming = false;
				break;
			}
		}
	}

fileOptionsSpec
{ Token idTok; Token value; }
	:	OPTIONS
		(
			idTok = id
			ASSIGN
			value = optionValue
			{ behavior.setFileOption(idTok, value,getInputState().filename); }
			SEMI
		)*
		RCURLY
	;

parserOptionsSpec
{ Token idTok; Token value; }
	:	OPTIONS
		(
			idTok = id
			ASSIGN
			value = optionValue
			{ behavior.setGrammarOption(idTok, value); }
			SEMI
		)*
		RCURLY
	;

lexerOptionsSpec
{ Token idTok; Token value; BitSet b; }
	:
	OPTIONS
	(	// Special case for vocabulary option because it has a bit-set
		"charVocabulary"
		ASSIGN
		b = charSet
		SEMI
		{ behavior.setCharVocabulary(b); }

	|	idTok = id
		ASSIGN
		value = optionValue
		{ behavior.setGrammarOption(idTok, value); }
		SEMI
	)*
	RCURLY
	;

subruleOptionsSpec
{ Token idTok; Token value; }
	:	OPTIONS
		(	idTok = id
			ASSIGN
			value = optionValue
			{ behavior.setSubruleOption(idTok, value); }
			SEMI
		)*
		RCURLY
	;

// optionValue returns a Token which may be one of several things:
//    STRING_LITERAL -- a quoted string
//    CHAR_LITERAL -- a single quoted character
//		INT -- an integer
//		RULE_REF or TOKEN_REF -- an identifier
optionValue
returns [ Token retval ]
{ retval = null; }
	:	retval = qualifiedID
	|	sl:STRING_LITERAL { retval = sl; }
	|	cl:CHAR_LITERAL { retval = cl; }
	|	il:INT { retval = il; }
	;

charSet
returns [ BitSet b ]
{
	b = null;
	BitSet tmpSet = null;
}
	:
		// TODO: generate a bit set
		b = setBlockElement
		(
			OR
			tmpSet = setBlockElement
			{ b.orInPlace(tmpSet); }
		)*
	;

setBlockElement
returns [ BitSet b ]
{
	b = null;
	int rangeMin = 0;
}
	:
	c1:CHAR_LITERAL
	{
		rangeMin = ANTLRLexer.tokenTypeForCharLiteral(c1.getText());
		b = BitSet.of(rangeMin);
	}
	(
		RANGE c2:CHAR_LITERAL
		{
			int rangeMax = ANTLRLexer.tokenTypeForCharLiteral(c2.getText());
			if (rangeMax < rangeMin) {
				tool.error("Malformed range line "+c1.getLine());
			}
			for (int i = rangeMin+1; i <= rangeMax; i++) {
				b.add(i);
			}
		}
	)?
	;

tokensSpec
	:	TOKENS
			(	(	{s1=null;}
					t1:TOKEN_REF
					( ASSIGN s1:STRING_LITERAL )?
					{behavior.defineToken(t1, s1);}
					(tokensSpecOptions[t1])?
				|	s3:STRING_LITERAL
					{behavior.defineToken(null, s3);}
					(tokensSpecOptions[s3])?
				)
				SEMI
			)+
		RCURLY
	;

tokensSpecOptions[Token t]
{
	Token o=null, v=null;
}
	:	OPEN_ELEMENT_OPTION
		o=id ASSIGN v=optionValue
		{behavior.refTokensSpecElementOption(t,o,v);}
		(
			SEMI
			o=id ASSIGN v=optionValue
			{behavior.refTokensSpecElementOption(t,o,v);}
		)*
		CLOSE_ELEMENT_OPTION
	;


parserSpec[String doc]
{
	Token idTok;
}
:
  "class" idTok = id "extends" "Parser"
		{behavior.startParser(getFilename(), idTok, doc);}
		SEMI
		(parserOptionsSpec)?
		{ behavior.endOptions(); }
		(tokensSpec)?
		( a:ACTION {behavior.refMemberAction(a);} )?
	;

lexerSpec[String doc]
{
	Token idTok;
}
:
  "class" idTok = id "extends" "Lexer"
		{behavior.startLexer(getFilename(), idTok, doc);}
		SEMI
		(lexerOptionsSpec)?
		{ behavior.endOptions(); }
		(tokensSpec)?
		( a:ACTION {behavior.refMemberAction(a);} )?
	;

rules
    :   (
			options {
				// limitation of appox LL(k) says ambig upon
				// DOC_COMMENT TOKEN_REF, but that's an impossible sequence
				warnWhenFollowAmbig=false;
			}
		:	rule
		)+
    ;

rule
{
	String access=null;
	Token idTok;
	String doc=null;
	blockNesting = -1;	// block increments, so -1 to make rule at level 0
}
	:
	(	d:DOC_COMMENT	{doc=d.getText();}
	)?
	(	p1:"protected"	{access=p1.getText();}
	|	p2:"public"		{access=p2.getText();}
	|	p3:"private"	{access=p3.getText();}
	)?
	idTok = id
	{
		behavior.defineRuleName(idTok, access, doc);
	}
	( aa:ARG_ACTION { behavior.refArgAction(aa); }  )?
	( "returns" rt:ARG_ACTION { behavior.refReturnAction(rt); } )?
	( throwsSpec )?
	( ruleOptionsSpec )?
	(a:ACTION {behavior.refInitAction(a);})?
	COLON block SEMI
	( exceptionGroup )?
	{behavior.endRule(idTok.getText());}
	;
/*
	//
	// for now, syntax error in rule aborts the whole grammar
	//
	exception catch [ParserException ex] {
		behavior.abortRule(idTok);
		behavior.hasError();
		// Consume until something that looks like end of a rule
		consume();
		while (LA(1) != SEMI && LA(1) != EOF) {
			consume();
		}
		consume();
	}
*/

ruleOptionsSpec
{ Token idTok; Token value; }
	:	OPTIONS
		(
			idTok = id
			ASSIGN
			value = optionValue
			{ behavior.setRuleOption(idTok, value); }
			SEMI
		)*
		RCURLY
	;

throwsSpec
{
	String t=null;
	Token a,b;
}
	:	"throws" a=id {t=a.getText();}
		( COMMA b=id {t+=","+b.getText();} )*
		{ behavior.setUserExceptions(t);	}
	;

block
    :   {blockNesting++;}
		alternative ( OR alternative )*
		{blockNesting--;}
    ;

alternative
    :
		{behavior.beginAlt(true);}
		( element )* ( exceptionSpecNoLabel )?
		{behavior.endAlt();}
    ;

exceptionGroup
	:	{ behavior.beginExceptionGroup(); }
		( exceptionSpec )+
		{ behavior.endExceptionGroup(); }
   ;

exceptionSpec
{ Token labelAction = null; }
   :
   "exception"
   ( aa:ARG_ACTION { labelAction = aa; } )?
   { behavior.beginExceptionSpec(labelAction); }
   ( exceptionHandler )*
   { behavior.endExceptionSpec(); }
   ;

exceptionSpecNoLabel
   :
   "exception"
   { behavior.beginExceptionSpec(null); }
   ( exceptionHandler )*
   { behavior.endExceptionSpec(); }
   ;

exceptionHandler
{ Token exType; Token exName; }
   :
   "catch"
   a1:ARG_ACTION
   a2:ACTION
   { behavior.refExceptionHandler(a1, a2); }
   ;

element
	:	elementNoOptionSpec (elementOptionSpec)?
	;

elementOptionSpec
{
	Token o=null, v=null;
}
	:	OPEN_ELEMENT_OPTION
		o=id ASSIGN v=optionValue
		{behavior.refElementOption(o,v);}
		(
			SEMI
			o=id ASSIGN v=optionValue
			{behavior.refElementOption(o,v);}
		)*
		CLOSE_ELEMENT_OPTION
	;

elementNoOptionSpec
{
	Token label = null;
	Token assignId = null;
	Token args = null;
}
	:	assignId=id
		ASSIGN
		( label=id COLON {checkForMissingEndRule(label);} )?
		(	rr:RULE_REF
			( aa:ARG_ACTION { args=aa; } )?
			{ behavior.refRule(assignId, rr, label, args); }
		|	// this syntax only valid for lexer
			tr:TOKEN_REF
			( aa2:ARG_ACTION { args=aa2; } )?
			{ behavior.refToken(assignId, tr, label, args, false, lastInRule()); }
		)
	|
		(label=id COLON {checkForMissingEndRule(label);} )?
		(	r2:RULE_REF
			( aa3:ARG_ACTION { args=aa3; } )?
			{ behavior.refRule(assignId, r2, label, args); }
		|
			range [label]
		|
			terminal [label]
		|
			NOT_OP
			(	notTerminal[label]
			|	ebnf[label,true]
			)
		|
			ebnf[label,false]
		)
	|
		a:ACTION	{ behavior.refAction(a);}
	|
		p:SEMPRED	{ behavior.refSemPred(p);}
	;

rootNode
{ Token label = null; }
	:
		(label=id COLON {checkForMissingEndRule(label);} )?
		terminal[label]
//	|	range[null]
	;

ebnf
[ Token label, boolean not ]
	:	lp:LPAREN
		{behavior.beginSubRule(label, lp.getLine(), not);}

		(
			// 2nd alt and optional branch ambig due to
			// linear approx LL(2) issue.  COLON ACTION
			// matched correctly in 2nd alt.
			options {
				warnWhenFollowAmbig = false;
			}
		:
			subruleOptionsSpec
			( aa:ACTION {behavior.refInitAction(aa);} )?
			COLON
		|	ab:ACTION {behavior.refInitAction(ab);}
			COLON
		)?

		block
		RPAREN

		(	(	QUESTION{behavior.optionalSubRule();}
			|	STAR	{behavior.zeroOrMoreSubRule();;}
			|	PLUS	{behavior.oneOrMoreSubRule();}
			)?
		|
			IMPLIES	{behavior.synPred();}
		)
		{behavior.endSubRule();}
	;

range
[ Token label ]
{
	Token trLeft=null;
	Token trRight=null;
}
	:	crLeft:CHAR_LITERAL RANGE crRight:CHAR_LITERAL
		{ behavior.refCharRange(crLeft, crRight, label, lastInRule()); }
	|
		(t:TOKEN_REF{trLeft=t;}|u:STRING_LITERAL{trLeft=u;})
		RANGE
		(v:TOKEN_REF{trRight=v;}|w:STRING_LITERAL{trRight=w;})
		{ behavior.refTokenRange(trLeft, trRight, label, lastInRule()); }
	;


terminal
[ Token label ]
{
	Token args=null;
}
	:
		cl:CHAR_LITERAL
		{behavior.refCharLiteral(cl, label, false, lastInRule());}
	|
		tr:TOKEN_REF
		// Args are only valid for lexer
		( aa:ARG_ACTION { args=aa; } )?
		{ behavior.refToken(null, tr, label, args, false, lastInRule()); }
	|
		sl:STRING_LITERAL
		{behavior.refStringLiteral(sl, label, lastInRule());}
	|
		wi:WILDCARD
		{behavior.refWildcard(wi, label);}
	;

notTerminal
[ Token label ]
	:
		cl:CHAR_LITERAL
		{behavior.refCharLiteral(cl, label, true, lastInRule());}
	|
		tr:TOKEN_REF
		{behavior.refToken(null, tr, label, null, true, lastInRule());}
	;

/** Match a.b.c.d qualified ids; WILDCARD here is overloaded as
 *  id separator; that is, I need a reference to the '.' token.
 */
qualifiedID returns [Token qidTok=null]
{
	StringBuffer buf = new StringBuffer(30);
	Token a;
}
	:	a=id {buf.append(a.getText());}
		(	WILDCARD a=id
			{buf.append('.'); buf.append(a.getText());}
		)*
		{
		 // can use either TOKEN_REF or RULE_REF; should
		 // really create a QID or something instead.
		 qidTok = new CommonToken(TOKEN_REF, buf.toString());
		 qidTok.setLine(a.getLine());
		}
	;

id
returns [ Token idTok ]
{ idTok = null; }
	:	a:TOKEN_REF			{idTok = a;}
	|	b:RULE_REF			{idTok = b;}
	;

class ANTLRLexer extends Lexer;
options {
	k=2;
	exportVocab=ANTLR;
	testLiterals=false;
	interactive=true;
}

tokens {
	"options";
}

{
	/**Convert 'c' to an integer char value. */
	public static int escapeCharValue(String cs) {
		//System.out.println("escapeCharValue("+cs+")");
		if ( cs.charAt(1)!='\\' ) return 0;
		switch ( cs.charAt(2) ) {
		case 'b' : return '\b';
		case 'r' : return '\r';
		case 't' : return '\t';
		case 'n' : return '\n';
		case 'f' : return '\f';
		case '"' : return '\"';
		case '\'' :return '\'';
		case '\\' :return '\\';

		case 'u' :
			// Unicode char
			if (cs.length() != 8) {
				return 0;
			} else {
				return
					Character.digit(cs.charAt(3), 16) * 16 * 16 * 16 +
					Character.digit(cs.charAt(4), 16) * 16 * 16 +
					Character.digit(cs.charAt(5), 16) * 16 +
					Character.digit(cs.charAt(6), 16);
			}

		case '0' :
		case '1' :
		case '2' :
		case '3' :
			if ( cs.length()>5 && Character.isDigit(cs.charAt(4)) ) {
				return (cs.charAt(2)-'0')*8*8 + (cs.charAt(3)-'0')*8 + (cs.charAt(4)-'0');
			}
			if ( cs.length()>4 && Character.isDigit(cs.charAt(3)) ) {
				return (cs.charAt(2)-'0')*8 + (cs.charAt(3)-'0');
			}
			return cs.charAt(2)-'0';

		case '4' :
		case '5' :
		case '6' :
		case '7' :
			if ( cs.length()>4 && Character.isDigit(cs.charAt(3)) ) {
				return (cs.charAt(2)-'0')*8 + (cs.charAt(3)-'0');
			}
			return cs.charAt(2)-'0';

		default :
			return 0;
		}
	}

	public static int tokenTypeForCharLiteral(String lit) {
		if ( lit.length()>3 ) {  // does char contain escape?
			return escapeCharValue(lit);
		} else {
			return lit.charAt(1);
		}
	}
}

WS	:	(	/*	'\r' '\n' can be matched in one alternative or by matching
				'\r' in one iteration and '\n' in another.  I am trying to
				handle any flavor of newline that comes in, but the language
				that allows both "\r\n" and "\r" and "\n" to all be valid
				newline is ambiguous.  Consequently, the resulting grammar
				must be ambiguous.  I'm shutting this warning off.
			 */
			options {
				generateAmbigWarnings=false;
			}
		:	' '
		|	'\t'
		|	'\r' '\n'	{newline();}
		|	'\r'		{newline();}
		|	'\n'		{newline();}
		)
		{ $setType(Token.SKIP); }
	;

COMMENT :
	( SL_COMMENT | t:ML_COMMENT {$setType(t.getType());} )
	{if ( _ttype != DOC_COMMENT ) $setType(Token.SKIP);}
	;

protected
SL_COMMENT :
	"//"
	( ~('\n'|'\r') )*
	(
		/*	'\r' '\n' can be matched in one alternative or by matching
			'\r' and then in the next token.  The language
			that allows both "\r\n" and "\r" and "\n" to all be valid
			newline is ambiguous.  Consequently, the resulting grammar
			must be ambiguous.  I'm shutting this warning off.
		 */
			options {
				generateAmbigWarnings=false;
			}
		:	'\r' '\n'
		|	'\r'
		|	'\n'
	)
	{ newline(); }
	;

protected
ML_COMMENT :
	"/*"
	(	{ LA(2)!='/' }? '*' {$setType(DOC_COMMENT);}
	|
	)
	(
		/*	'\r' '\n' can be matched in one alternative or by matching
			'\r' and then in the next token.  The language
			that allows both "\r\n" and "\r" and "\n" to all be valid
			newline is ambiguous.  Consequently, the resulting grammar
			must be ambiguous.  I'm shutting this warning off.
		 */
		options {
			greedy=false;  // make it exit upon "*/"
			generateAmbigWarnings=false; // shut off newline errors
		}
	:	'\r' '\n'	{newline();}
	|	'\r'		{newline();}
	|	'\n'		{newline();}
	|	~('\n'|'\r')
	)*
	"*/"
	;

OPEN_ELEMENT_OPTION
	:	'<'
	;

CLOSE_ELEMENT_OPTION
	:	'>'
	;

COMMA : ',';

QUESTION :	'?' ;

LPAREN:	'(' ;

RPAREN:	')' ;

COLON :	':' ;

STAR:	'*' ;

PLUS:	'+' ;

ASSIGN : '=' ;

IMPLIES : "=>" ;

SEMI:	';' ;

OR	:	'|' ;

WILDCARD : '.' ;

RANGE : ".." ;

NOT_OP :	'~' ;

RCURLY:	'}'	;

CHAR_LITERAL
	:	'\'' (ESC|~'\'') '\''
	;

STRING_LITERAL
	:	'"' (ESC|~'"')* '"'
	;

protected
ESC	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'w'
		|	'a'
		|	'"'
		|	'\''
		|	'\\'
		|	('0'..'3')
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:
	('0'..'9')
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:
	'0'..'9'
				)?
			)?
		|	('4'..'7')
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:
	('0'..'9')
			)?
		|	'u' XDIGIT XDIGIT XDIGIT XDIGIT
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

protected
XDIGIT :
		'0' .. '9'
	|	'a' .. 'f'
	|	'A' .. 'F'
	;

protected
VOCAB
	:	'\3'..'\176'	// common ASCII
	;

INT	:	('0'..'9')+
	;


ARG_ACTION
   :
	NESTED_ARG_ACTION
	{ setText(Utils.stripFrontBack(getText(), "[", "]")); }
	;

protected
NESTED_ARG_ACTION :
	'['
	(
		/*	'\r' '\n' can be matched in one alternative or by matching
			'\r' and then '\n' in the next iteration.
		 */
		options {
			generateAmbigWarnings=false; // shut off newline errors
		}
	:	NESTED_ARG_ACTION
	|	'\r' '\n'	{newline();}
	|	'\r'		{newline();}
	|	'\n'		{newline();}
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	~']'
	)*
	']'
	;


ACTION
{int actionLine=getLine();}
	:	NESTED_ACTION
		(	'?'	{_ttype = SEMPRED;} )?
		{
			if ( _ttype==ACTION ) {
				setText(Utils.stripFrontBack(getText(), "{", "}"));
			} else {
				setText(Utils.stripFrontBack(getText(), "{", "}?"));
			}
			CommonToken t = new CommonToken(_ttype,$getText);
			t.setLine(actionLine);	// set action line to start
			$setToken(t);
		}
	;

protected
NESTED_ACTION :
	'{'
	(
		options {
			greedy = false; // exit upon '}'
		}
	:
		(
			options {
				generateAmbigWarnings = false; // shut off newline warning
			}
		:	'\r' '\n'	{newline();}
		|	'\r' 		{newline();}
		|	'\n'		{newline();}
		)
	|	NESTED_ACTION
	|	CHAR_LITERAL
	|	COMMENT
	|	STRING_LITERAL
	|	.
	)*
	'}'
   ;


TOKEN_REF
options { testLiterals = true; }
	:	'A'..'Z'
		(	// scarf as many letters/numbers as you can
			options {
				warnWhenFollowAmbig=false;
			}
		:
			'a'..'z'|'A'..'Z'|'_'|'0'..'9'
		)*
	;

// we get a warning here when looking for options '{', but it works right
RULE_REF
{
	int t=0;
}
	:	t=INTERNAL_RULE_REF {_ttype=t;}
		(	{t==LITERAL_options}? WS_LOOP ('{' {_ttype = OPTIONS;})?
		|	{t==LITERAL_tokens}? WS_LOOP ('{' {_ttype = TOKENS;})?
		|
		)
	;

protected
WS_LOOP
	:	(	// grab as much WS as you can
			options {
				greedy=true;
			}
		:
			WS
		|	COMMENT
		)*
	;

protected
INTERNAL_RULE_REF returns [int t]
{
	t = RULE_REF;
}
	:	'a'..'z'
		(	// scarf as many letters/numbers as you can
			options {
				warnWhenFollowAmbig=false;
			}
		:
			'a'..'z'|'A'..'Z'|'_'|'0'..'9'
		)*
		{t = testLiteralsTable(t);}
	;

protected
WS_OPT :
	(WS)?
	;

// remove after the class variants of the scanners/parsers go
// away. this rule, just forces the lexer to use the most
// complicated class so I can get rid of the others.
protected
NOT_USEFUL
	:	('a')=>'a'
	|	'a'
	;
