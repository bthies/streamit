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
 * $Id: preproc.g,v 1.1 2001-08-30 16:32:36 thies Exp $
 */

header {
package at.dms.compiler.tools.antlr.compiler;
}

{
import java.util.Hashtable;
}

class Preprocessor extends Parser;
options {
	k=1;
	interactive=true;
}

tokens {
	"tokens";
}

grammarFile[Hierarchy hier, String file]
{
	GrammarDefinition gr;
	IndexedVector opt=null;
}
	:	( hdr:HEADER_ACTION { hier.getFile(file).addHeaderAction(hdr.getText()); } )*
		( opt=optionSpec[null] )?
		(	gr=class_def[file, hier]
			{
			if ( gr!=null && opt!=null ) {
				hier.getFile(file).setOptions(opt);
			}
			if ( gr!=null ) {
				gr.setFileName(file);
				hier.addGrammar(gr);
			}
			}
		)*
		EOF
	;

class_def[String file, Hierarchy hier] returns [GrammarDefinition gr]
{
	gr=null;
	IndexedVector rules = new IndexedVector(100);
	IndexedVector classOptions = null;
}
	:	( preamble:ACTION )?
		"class" sub:ID "extends" sup:ID SEMI
		{
			gr = (GrammarDefinition)hier.getGrammar(sub.getText());
			if ( gr!=null ) {
//				Utils.toolError("redefinition of grammar "+gr.getName()+" ignored");
				gr=null;
				throw new SemanticException("redefinition of grammar "+sub.getText(), file, sub.getLine());
			} else {
				gr = new GrammarDefinition(sub.getText(), sup.getText(), rules);
				if ( preamble!=null ) {
					gr.setPreambleAction(preamble.getText());
				}
			}
		}
		( classOptions = optionSpec[gr] )?
		{
		if ( gr!=null ) {
			gr.setOptions(classOptions);
		}
		}
		( tk:TOKENS_SPEC {gr.setTokenSection(tk.getText());} )?
		( memberA:ACTION {gr.setMemberAction(memberA.getText());} )?
		( rule[gr] )+
	;

optionSpec[GrammarDefinition gr] returns [IndexedVector options]
{
	options = new IndexedVector();
}
	:	OPTIONS_START
			(	op:ID rhs:ASSIGN_RHS
				{
				Option newOp = new Option(op.getText(),rhs.getText(),gr);
				options.appendElement(newOp.getName(),newOp);
				if ( gr!=null && op.getText().equals("importVocab") ) {
					gr.setImportVocabulary(rhs.getText());
				} else if ( gr!=null && op.getText().equals("exportVocab") ) {
					// don't want ';' included in outputVocab.
					// This is heinously inconsistent!  Ugh.
					gr.setExportVocabulary(rhs.getText().substring(0,rhs.getText().length()-1));
				}
				}
			)*
//		{gr.fixupVocabOptionsForInheritance();}
		RCURLY
	;

rule[GrammarDefinition gr]
{
	IndexedVector o = null;	// options for rule
	String vis = null;
	String eg=null, thr="";
}
	:	(	"protected"	{vis="protected";}
		|	"private"	{vis="private";}
		|	"public"	{vis="public";}
		)?
		r:ID
		( arg:ARG_ACTION )?
		( "returns" ret:ARG_ACTION )?
		( thr=throwsSpec )?
		( o = optionSpec[null] )?
		( init:ACTION )?
		blk:RULE_BLOCK
		eg=exceptionGroup
		{
		String rtext = blk.getText()+eg;
		Rule ppr = new Rule(r.getText(),rtext,o,gr);
		ppr.setThrowsSpec(thr);
		if ( arg!=null ) {
			ppr.setArgs(arg.getText());
		}
		if ( ret!=null ) {
			ppr.setReturnValue(ret.getText());
		}
		if ( init!=null ) {
			ppr.setInitAction(init.getText());
		}
		ppr.setVisibility(vis);
		if ( gr!=null ) {
			gr.addRule(ppr);
		}
		}
	;

throwsSpec returns [String t]
{t="throws ";}
	:	"throws" a:ID {t+=a.getText();}
		( COMMA b:ID {t+=","+b.getText();} )*
	;

exceptionGroup returns [String g]
{String e=null; g="";}
	:	( e=exceptionSpec {g += e;} )*
	;

exceptionSpec returns [String es]
{ String h=null;
  es = System.getProperty("line.separator")+"exception ";
}
	:	"exception"
		( aa:ARG_ACTION {es += aa.getText();} )?
		( h=exceptionHandler {es += h;} )*
	;

exceptionHandler returns [String h]
{h=null;}
	:	"catch" a1:ARG_ACTION a2:ACTION
		{h = System.getProperty("line.separator")+
			 "catch "+a1.getText()+" "+a2.getText();}
	;

class PreprocessorLexer extends Lexer;
options {
	k=2;
	charVocabulary = '\3'..'\176';	// common ASCII
	interactive=true;
}

RULE_BLOCK
    :   ':' (options {greedy=true;}:WS)?
		ALT (options {greedy=true;}:WS)?
		( '|' (options {greedy=true;}:WS)? ALT (options {greedy=true;}:WS)? )* ';'
    ;

SUBRULE_BLOCK
	:	'(' (options {greedy=true;}:WS)? ALT
		(	options {greedy=true;}
		:	(WS)? '|' (options {greedy=true;}:WS)? ALT
		)*
		(WS)?
		')'
		(	options {greedy=true;}
		:	'*'
		|	'+'
		|	'?'
		|	"=>"
		)?
	;

protected
ALT	:	(options {greedy=true;} : ELEMENT)*
	;

protected
ELEMENT
	:	COMMENT
	|	ACTION
	|	STRING_LITERAL
	|	CHAR_LITERAL
	|	SUBRULE_BLOCK
	|	NEWLINE
	|	~('\n' | '\r' | '(' | ')' | '/' | '{' | '"' | '\'' | ';')
	;

SEMI:	';'
	;

COMMA:	','
	;

RCURLY
	:	'}'
	;

/** This rule picks off keywords in the lexer that need to be
 *  handled specially.  For example, "header" is the start
 *  of the header action (used to distinguish between options
 *  block and an action).  We do not want "header" to go back
 *  to the parser as a simple keyword...it must pick off
 *  the action afterwards.
 */
ID_OR_KEYWORD
	:	id:ID	{$setType(id.getType());}
		(	{id.getText().equals("header")}? (options {greedy=true;}:WS)?
			(STRING_LITERAL)? (WS|COMMENT)* ACTION
			{$setType(HEADER_ACTION);}
		|	{id.getText().equals("tokens")}? (WS|COMMENT)* CURLY_BLOCK_SCARF
			{$setType(TOKENS_SPEC);}
		|	{id.getText().equals("options")}? (WS|COMMENT)* '{'
			{$setType(OPTIONS_START);}
		)?
	;


protected
CURLY_BLOCK_SCARF
	:	'{'
		(	options {greedy=false;}
		:	NEWLINE
		|	STRING_LITERAL
		|	CHAR_LITERAL
		|	COMMENT
		|	.
		)*
		'}'
	;

protected
ID
options {
	testLiterals=true;
}
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;

ASSIGN_RHS
	:	'='
		(	options {greedy=false;}
		:	STRING_LITERAL
		|	CHAR_LITERAL
		|	NEWLINE
		|	.
		)*
		';'
	;

WS	:	(	options {greedy=true;}
		: 	' '
		|	'\t'
		|	NEWLINE
		)+
		{$setType(Token.SKIP);}
	;

protected
NEWLINE
	:	(	options {
				generateAmbigWarnings=false;
			}
		:	'\r' '\n'	{newline();}
		|	'\r'		{newline();}
		|	'\n'		{newline();}
		)
	;

COMMENT
	:	( SL_COMMENT | ML_COMMENT )
		{$setType(Token.SKIP);}
	;

protected
SL_COMMENT
	:	"//" (options {greedy=false;}:.)* NEWLINE
	;

protected
ML_COMMENT :
	"/*"
	(	options {greedy=false;}
	:	NEWLINE
	|	.
	)*
	"*/"
	;

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
			(	options {greedy=true;}
			:	DIGIT
				(	options {greedy=true;}
				:	DIGIT
				)?
			)?
		|	('4'..'7') (options {greedy=true;}:DIGIT)?
		|	'u' XDIGIT XDIGIT XDIGIT XDIGIT
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

protected
XDIGIT
	:	'0' .. '9'
	|	'a' .. 'f'
	|	'A' .. 'F'
	;

ARG_ACTION
	:	'['
		(
			options {
				greedy=false;
			}
		:	ARG_ACTION
		|	NEWLINE
		|	CHAR_LITERAL
		|	STRING_LITERAL
		|	.
		)*
		']'
	;

ACTION
	:	'{'
		(
			options {
				greedy=false;
			}
		:	NEWLINE
		|	ACTION
		|	CHAR_LITERAL
		|	COMMENT
		|	STRING_LITERAL
		|	.
		)*
		'}'
   ;
