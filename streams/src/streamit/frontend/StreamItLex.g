/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

/*
 * StreamItLex.g: Lexical tokens for StreamIt
 * $Id: StreamItLex.g,v 1.17 2003-10-09 19:50:53 dmaze Exp $
 */

header {
	package streamit.frontend;
}

options {
	mangleLiteralPrefix = "TK_";
	// language="Cpp";
}

class StreamItLex extends Lexer;
options {
	exportVocab=StreamItLex;
	charVocabulary = '\3'..'\377';
	k=3;
}

tokens {
	// Stream types:
	"filter"; "pipeline"; "splitjoin"; "feedbackloop";
	// Messaging:
	"portal"; "to"; "handler";
	// Composite streams:
	"add";
	// Splitters and joiners:
	"split"; "join";
	"duplicate"; "roundrobin";
	// Feedback loops:
	"body"; "loop"; "enqueue";
	// Special functions:
	"init"; "prework"; "work"; "phase";
	// Manipulating tapes:
	"peek"; "pop"; "push";
	// Basic types:
	"boolean"; "float"; "bit"; "int"; "void"; "double"; "complex";
	// Complicated types:
	"struct"; "template";
	// Control flow:
	"if"; "else"; "while"; "for"; "switch"; "case"; "default"; "break";
	"continue"; "return";
	// Intrinsic values:
	"pi"; "true"; "false";
}

ARROW :	"->" ;

WS	:	(' '
	|	'\t'
	|	'\n'	{newline();}
	|	'\r')
		{ _ttype = Token.SKIP; }
	;


SL_COMMENT : 
	"//" 
	(~'\n')* '\n'
	{ _ttype = Token.SKIP; newline(); }
	;

ML_COMMENT
	:	"/*"
		(	{ LA(2)!='/' }? '*'
		|	'\n' { newline(); }
		|	~('*'|'\n')
		)*
		"*/"
			{ $setType(Token.SKIP); }
	;


LPAREN
//options {
//	paraphrase="'('";
//}
	:	'('
	;

RPAREN
//options {
//	paraphrase="')'";
//}
	:	')'
	;

LCURLY:	'{' ;
RCURLY:	'}'	;
LSQUARE: '[' ;
RSQUARE: ']' ;
PLUS: '+' ;
PLUS_EQUALS: "+=" ;
INCREMENT: "++" ;
MINUS: '-' ;
MINUS_EQUALS: "-=" ;
DECREMENT: "--" ;
STAR: '*';
STAR_EQUALS: "*=" ;
DIV: '/';
DIV_EQUALS: "/=" ;
MOD: '%';
LOGIC_AND: "&&";
LOGIC_OR: "||";
BITWISE_AND: "&";
BITWISE_OR: "|";
BITWISE_XOR: "^";
ASSIGN: '=';
EQUAL: "==";
NOT_EQUAL: "!=";
LESS_THAN: '<';
LESS_EQUAL: "<=";
MORE_THAN: '>';
MORE_EQUAL: ">=";
QUESTION: '?';
COLON: ':';
SEMI: ';';
COMMA: ',';
DOT: '.';
BANG: '!';

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
		|	'"'
		|	'\''
		|	'\\'
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	DIGIT
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	DIGIT
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	DIGIT
			)?
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

NUMBER
	:	 (DIGIT)+ (DOT (DIGIT)+ )? (('e' | 'E') ('+'|'-')? (DIGIT)+ )? ('i')?
	;

ID
options {
	testLiterals = true;
	paraphrase = "an identifier";
}
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;


