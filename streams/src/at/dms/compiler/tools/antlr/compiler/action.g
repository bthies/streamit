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
 * $Id: action.g,v 1.1 2001-08-30 16:32:36 thies Exp $
 */

header {
package at.dms.compiler.tools.antlr.compiler;
}

{
import java.io.StringReader;
}

/** Perform the following translations:

    Text related translations

	$append(x)		-> text.append(x)
	$setText(x)		-> text.setLength(_begin); text.append(x)
	$getText		-> new String(text.getBuffer(),_begin,text.length()-_begin)
	$setToken(x)	-> _token = x
	$setType(x)		-> _ttype = x
 */
class ActionLexer extends Lexer;
options {
	k=2;
	charVocabulary='\3'..'\176';
	testLiterals=false;
	interactive=true;
}

{
	protected RuleBlock currentRule;
	protected JavaCodeGenerator generator;
	protected int lineOffset = 0;
	private Main tool;	// main

 	public ActionLexer(String s, RuleBlock currentRule, JavaCodeGenerator generator) {
	  this(new StringReader(s));
	  this.currentRule = currentRule;
	  this.generator = generator;
	}

	public void setLineOffset(int lineOffset) {
	  // this.lineOffset = lineOffset;
	  setLine(lineOffset);
	}

	public void setTool(Main tool) {
	  this.tool = tool;
	}

	// Override of error-reporting for syntax
	public void reportError(RecognitionException e) {
	  System.err.print("Syntax error in action: ");
	  super.reportError(e);
	}
}

// rules are protected because we don't care about nextToken().

public
ACTION
	:	(	STUFF

		|	TEXT_ITEM
		)+
	;

// stuff in between #(...) and #id items
protected
STUFF
	:	COMMENT
	|	STRING
	|	CHAR
	|	"\r\n" 		{newline();}
	|	'\r' 		{newline();}
	|	'\n'		{newline();}
	|	'/'	~('/'|'*')	// non-comment start '/'
//	|	( ~('/'|'\n'|'\r'|'$'|'#'|'"'|'\'') )+
	|	~('/'|'\n'|'\r'|'$'|'#'|'"'|'\'')
	;

protected
TEXT_ITEM
	:	"$append(" a1:TEXT_ARG ')'
		{
			String t = "text.append("+a1.getText()+")";
			$setText(t);
		}
	|	"$set"
		(	"Text(" a2:TEXT_ARG ')'
			{
			String t;
			t = "text.setLength(_begin); text.append("+a2.getText()+")";
			$setText(t);
			}
		|	"Token(" a3:TEXT_ARG ')'
			{
			String t="_token = "+a3.getText();
			$setText(t);
			}
		|	"Type(" a4:TEXT_ARG ')'
			{
			String t="_ttype = "+a4.getText();
			$setText(t);
			}
		)
	|	"$getText"
		{
			$setText("new String(text.getBuffer(),_begin,text.length()-_begin)");
		}
	;

protected
TEXT_ARG
	:	(WS)? ( TEXT_ARG_ELEMENT (options {greedy=true;}:WS)? )+
	;

protected
TEXT_ARG_ELEMENT
	:	TEXT_ARG_ID_ELEMENT
	|	STRING
	|	CHAR
	|	INT_OR_FLOAT
	|	TEXT_ITEM
	|	'+'
	;

protected
TEXT_ARG_ID_ELEMENT
	:	id:ID (options {greedy=true;}:WS)?
		(	'(' (options {greedy=true;}:WS)? ( TEXT_ARG (',' TEXT_ARG)* )* (WS)? ')'	// method call
		|	( '[' (WS)? TEXT_ARG (WS)? ']' )+				// array reference
		|	'.' TEXT_ARG_ID_ELEMENT
		|
		)
	;

protected
ARG	:	(	STRING
		|	CHAR
		|	INT_OR_FLOAT
		)
		(options {greedy=true;} : (WS)? ( '+'| '-' | '*' | '/' ) (WS)? ARG )*
	;

protected
ID	:	('a'..'z'|'A'..'Z'|'_')
		(options {greedy=true;} : ('a'..'z'|'A'..'Z'|'0'..'9'|'_'))*
	;

protected
VAR_ASSIGN
	:	'='
		{
		}
	;

protected
COMMENT
	:	SL_COMMENT
	|	ML_COMMENT
	;

protected
SL_COMMENT
	:	"//" (options {greedy=false;}:.)* ('\n'|"\r\n"|'\r')
		{newline();}
	;

protected
ML_COMMENT :
	"/*"
	(	options {greedy=false;}
	:	'\r' '\n'	{newline();}
	|	'\r' 		{newline();}
	|	'\n'		{newline();}
	|	.
	)*
	"*/"
	;

protected
CHAR :
	'\''
	( ESC | ~'\'' )
	'\''
	;

protected
STRING :
	'"'
	(ESC|~'"')*
	'"'
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
		|	('0'..'3')
			(	options {greedy=true;}
			:	DIGIT
				(	options {greedy=true;}
				:	DIGIT
				)?
			)?
		|	('4'..'7') (options {greedy=true;}:DIGIT)?
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

protected
INT	:	(DIGIT)+
	;

protected
INT_OR_FLOAT
	:	(options {greedy=true;}:DIGIT)+
		(	options {greedy=true;}
		:	'.' (options {greedy=true;}:DIGIT)*
		|	'L'
		|	'l'
		)?
	;

protected
WS	:	(	options {greedy=true;}
		: 	' '
		|	'\t'
		|	'\r' '\n'	{newline();}
		|	'\r'		{newline();}
		|	'\n'		{newline();}
		)+
	;
