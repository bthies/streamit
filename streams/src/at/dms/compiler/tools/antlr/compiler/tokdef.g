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
 * $Id: tokdef.g,v 1.1 2001-08-30 16:32:36 thies Exp $
 */

header {
package at.dms.compiler.tools.antlr.compiler;
}

/** Simple lexer/parser for reading token definition files
  in support of the import/export vocab option for grammars.
 */
class ANTLRTokdefParser extends Parser;
options {
	k=3;
	interactive=true;
}

file [ImportVocabTokenManager tm] :
	name:ID
	(line[tm])*;

line [ImportVocabTokenManager tm]
{ Token t=null; Token s=null; }
	:	(	s1:STRING {s = s1;}
		|	lab:ID {t = lab;} ASSIGN s2:STRING {s = s2;}
		|	id:ID {t=id;} LPAREN para:STRING RPAREN
		|	id2:ID {t=id2;}
		)
		ASSIGN
		i:INT
		{
		Integer value = Integer.valueOf(i.getText());
		// if literal found, define as a string literal
		if ( s!=null ) {
			tm.define(s.getText(), value.intValue());
			// if label, then label the string and map label to token symbol also
			if ( t!=null ) {
				StringLiteralSymbol sl =
					(StringLiteralSymbol) tm.getTokenSymbol(s.getText());
				sl.setLabel(t.getText());
				tm.mapToTokenSymbol(t.getText(), sl);
			}
		}
		// define token (not a literal)
		else if ( t!=null ) {
			tm.define(t.getText(), value.intValue());
			if ( para!=null ) {
				TokenSymbol ts = tm.getTokenSymbol(t.getText());
				ts.setParaphrase(
					para.getText()
				);
			}
		}
		}
	;

class ANTLRTokdefLexer extends Lexer;
options {
	k=2;
	testLiterals=false;
	interactive=true;
}

WS	:	(	' '
		|	'\t'
		|	'\r' ('\n')?	{newline();}
		|	'\n'		{newline();}
		)
		{ _ttype = Token.SKIP; }
	;

SL_COMMENT :
	"//"
	(~('\n'|'\r'))* ('\n'|'\r'('\n')?)
	{ _ttype = Token.SKIP; newline(); }
	;

ML_COMMENT :
   "/*"
   (
			'\n' { newline(); }
		|	'*' ~'/'
		|	~'*'
	)*
	"*/"
	{ _ttype = Token.SKIP; }
	;

LPAREN : '(' ;
RPAREN : ')' ;

ASSIGN : '=' ;

STRING
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
		|	('0'..'3') ( DIGIT (DIGIT)? )?
		|	('4'..'7') (DIGIT)?
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

ID :
	('a'..'z'|'A'..'Z')
	('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;

INT : (DIGIT)+
	;
