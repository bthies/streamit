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
 * $Id: Kjc.flex,v 1.6 2007-02-22 20:15:01 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.Compiler;
import at.dms.compiler.CompilerMessages;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.antlr.extra.CToken;
import at.dms.compiler.antlr.extra.InputBuffer;
import at.dms.compiler.antlr.extra.Scanner;
import at.dms.compiler.antlr.runtime.Token;

%%

%public
%class KjcScanner
%extends at.dms.compiler.antlr.extra.Scanner
%implements KjcTokenTypes

%function nextTokenImpl
%type at.dms.compiler.antlr.runtime.Token

%unicode
%pack

%{
  public KjcScanner(Compiler compiler, InputBuffer buffer) {
    super(compiler, buffer);
    this.buffer = buffer;
  }

  /**
   * Creates a character literal token.
   */
  private CToken buildCharacterLiteral(char image) {
    return new CToken(CHARACTER_LITERAL, String.valueOf(image));
  }


  private final StringBuffer	string = new StringBuffer();
%}

%init{
  // dummy: we provide our own constructor
  super(null, null);
%init}

%eofval{
  return TOKEN_EOF;
%eofval}

/*
 * macros
 */
// white space
W =	[ \t\f]

// line terminator
T =	\r|\n|\r\n

// decimal digit
D =	[0-9]

// hexadecimal digit
H =	[0-9a-fA-F]

// octal digit
O =	[0-7]

// exponent part
E =	[eE] [+\-]? {D}+


%state STRINGLITERAL, CHARLITERAL, TRADITIONALCOMMENT, ENDOFLINECOMMENT

%%

<YYINITIAL> {

"abstract"		{ return TOKEN_LITERAL_abstract; }
"boolean"		{ return TOKEN_LITERAL_boolean; }
"break"		{ return TOKEN_LITERAL_break; }
"byte"		{ return TOKEN_LITERAL_byte; }
"case"		{ return TOKEN_LITERAL_case; }
"catch"		{ return TOKEN_LITERAL_catch; }
"char"		{ return TOKEN_LITERAL_char; }
"class"		{ return TOKEN_LITERAL_class; }
"const"		{ return TOKEN_LITERAL_const; }
"continue"		{ return TOKEN_LITERAL_continue; }
"default"		{ return TOKEN_LITERAL_default; }
"do"		{ return TOKEN_LITERAL_do; }
"double"		{ return TOKEN_LITERAL_double; }
"else"		{ return TOKEN_LITERAL_else; }
"extends"		{ return TOKEN_LITERAL_extends; }
"false"		{ return TOKEN_LITERAL_false; }
"final"		{ return TOKEN_LITERAL_final; }
"finally"		{ return TOKEN_LITERAL_finally; }
"float"		{ return TOKEN_LITERAL_float; }
"for"		{ return TOKEN_LITERAL_for; }
"goto"		{ return TOKEN_LITERAL_goto; }
"if"		{ return TOKEN_LITERAL_if; }
"implements"		{ return TOKEN_LITERAL_implements; }
"import"		{ return TOKEN_LITERAL_import; }
"instanceof"		{ return TOKEN_LITERAL_instanceof; }
"int"		{ return TOKEN_LITERAL_int; }
"interface"		{ return TOKEN_LITERAL_interface; }
"long"		{ return TOKEN_LITERAL_long; }
"native"		{ return TOKEN_LITERAL_native; }
"new"		{ return TOKEN_LITERAL_new; }
"null"		{ return TOKEN_LITERAL_null; }
"package"		{ return TOKEN_LITERAL_package; }
"private"		{ return TOKEN_LITERAL_private; }
"protected"		{ return TOKEN_LITERAL_protected; }
"public"		{ return TOKEN_LITERAL_public; }
"return"		{ return TOKEN_LITERAL_return; }
"short"		{ return TOKEN_LITERAL_short; }
"static"		{ return TOKEN_LITERAL_static; }
"strictfp"		{ return TOKEN_LITERAL_strictfp; }
"super"		{ return TOKEN_LITERAL_super; }
"switch"		{ return TOKEN_LITERAL_switch; }
"synchronized"		{ return TOKEN_LITERAL_synchronized; }
"this"		{ return TOKEN_LITERAL_this; }
"throw"		{ return TOKEN_LITERAL_throw; }
"throws"		{ return TOKEN_LITERAL_throws; }
"transient"		{ return TOKEN_LITERAL_transient; }
"true"		{ return TOKEN_LITERAL_true; }
"try"		{ return TOKEN_LITERAL_try; }
"void"		{ return TOKEN_LITERAL_void; }
"volatile"		{ return TOKEN_LITERAL_volatile; }
"while"		{ return TOKEN_LITERAL_while; }
"="		{ return TOKEN_ASSIGN; }
"&"		{ return TOKEN_BAND; }
"&="		{ return TOKEN_BAND_ASSIGN; }
"~"		{ return TOKEN_BNOT; }
"|"		{ return TOKEN_BOR; }
"|="		{ return TOKEN_BOR_ASSIGN; }
">>>"		{ return TOKEN_BSR; }
">>>="		{ return TOKEN_BSR_ASSIGN; }
"\^"		{ return TOKEN_BXOR; }
"\^="		{ return TOKEN_BXOR_ASSIGN; }
":"		{ return TOKEN_COLON; }
","		{ return TOKEN_COMMA; }
"--"		{ return TOKEN_DEC; }
"."		{ return TOKEN_DOT; }
"=="		{ return TOKEN_EQUAL; }
">="		{ return TOKEN_GE; }
">"		{ return TOKEN_GT; }
"++"		{ return TOKEN_INC; }
"&&"		{ return TOKEN_LAND; }
"["		{ return TOKEN_LBRACK; }
"{"		{ return TOKEN_LCURLY; }
"<="		{ return TOKEN_LE; }
"!"		{ return TOKEN_LNOT; }
"||"		{ return TOKEN_LOR; }
"("		{ return TOKEN_LPAREN; }
"<"		{ return TOKEN_LT; }
"-"		{ return TOKEN_MINUS; }
"-="		{ return TOKEN_MINUS_ASSIGN; }
"!="		{ return TOKEN_NOT_EQUAL; }
"%"		{ return TOKEN_PERCENT; }
"%="		{ return TOKEN_PERCENT_ASSIGN; }
"+"		{ return TOKEN_PLUS; }
"+="		{ return TOKEN_PLUS_ASSIGN; }
"?"		{ return TOKEN_QUESTION; }
"]"		{ return TOKEN_RBRACK; }
"}"		{ return TOKEN_RCURLY; }
")"		{ return TOKEN_RPAREN; }
";"		{ return TOKEN_SEMI; }
"<<"		{ return TOKEN_SL; }
"/"		{ return TOKEN_SLASH; }
"/="		{ return TOKEN_SLASH_ASSIGN; }
"<<="		{ return TOKEN_SL_ASSIGN; }
">>"		{ return TOKEN_SR; }
">>="		{ return TOKEN_SR_ASSIGN; }
"*"		{ return TOKEN_STAR; }
"*="		{ return TOKEN_STAR_ASSIGN; }

  /* string literal */
  \"    			{ yybegin(STRINGLITERAL); string.setLength(0); }

  /* character literal */
  \'    			{ yybegin(CHARLITERAL); }

  /* numeric literals */
  (0 | [1-9]{D}*) [lL]?		{ return new CToken(INTEGER_LITERAL, yytext()); }
  (0 [xX] {H}+) [lL]?		{ return new CToken(INTEGER_LITERAL, yytext()); }
  (0 {O}+) [lL]?		{ return new CToken(INTEGER_LITERAL, yytext()); }

  {D}+ \. {D}* {E}? [fFdD]?	{ return new CToken(REAL_LITERAL, yytext()); }
  \. {D}+ {E}? [fFdD]?		{ return new CToken(REAL_LITERAL, yytext()); }
  {D}+ {E} [fFdD]?		{ return new CToken(REAL_LITERAL, yytext()); }
  {D}+ [fFdD]			{ return new CToken(REAL_LITERAL, yytext()); }

  /* comments */
  "/*"				{ yybegin(TRADITIONALCOMMENT); string.setLength(0); }
  "//"				{ yybegin(ENDOFLINECOMMENT); string.setLength(0); }

  /* whitespace */
  {T}				{ incrementLine(); }
  {W}				{ /* ignore */ }

  /* identifiers */
  [:jletter:][:jletterdigit:]*	{ return new CToken(IDENT, yytext().intern()); }
}

<STRINGLITERAL> {
  \"    			{ yybegin(YYINITIAL); return new CToken(STRING_LITERAL, string.toString()); }

  [^\r\n\"\\]+			{ string.append(yytext()); }

  /* escape sequences */
  "\\b" 			{ string.append('\b'); }
  "\\t" 			{ string.append('\t'); }
  "\\n" 			{ string.append('\n'); }
  "\\f" 			{ string.append('\f'); }
  "\\r" 			{ string.append('\r'); }
  "\\\""			{ string.append('\"'); }
  "\\'" 			{ string.append('\''); }
  "\\\\"			{ string.append('\\'); }
  \\[0-3]?{O}?{O}		{
				  int		val;

				  val = Integer.parseInt(yytext().substring(1), 8);
				  string.append((char)val);
				}

  /* error cases */
  \\.   			{ reportTrouble(CompilerMessages.BAD_ESCAPE_SEQUENCE, new Object[]{ yytext() }); }
  {T}				{ reportTrouble(CompilerMessages.BAD_END_OF_LINE, new Object[]{ "string literal" }); }
}

<CHARLITERAL> {
  [^\r\n\'\\]\'			{ yybegin(YYINITIAL); return buildCharacterLiteral(yytext().charAt(0)); }

  /* escape sequences */
  "\\b"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\b'); }
  "\\t"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\t'); }
  "\\n"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\n'); }
  "\\f"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\f'); }
  "\\r"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\r'); }
  "\\\""\'      		{ yybegin(YYINITIAL); return buildCharacterLiteral('\"'); }
  "\\'"\'       		{ yybegin(YYINITIAL); return buildCharacterLiteral('\''); }
  "\\\\"\'      		{ yybegin(YYINITIAL); return buildCharacterLiteral('\\'); }
  \\[0-3]?{O}?{O}\'		{
				  yybegin(YYINITIAL);

				  int		val;

				  val = Integer.parseInt(yytext().substring(1, yylength()-1), 8);
				  return buildCharacterLiteral((char)val);
				}

  /* error cases */
  \\.   			{ reportTrouble(CompilerMessages.BAD_ESCAPE_SEQUENCE, new Object[]{ yytext() }); }
  {T}				{ reportTrouble(CompilerMessages.BAD_END_OF_LINE, new Object[]{ "character literal" }); }
}

<TRADITIONALCOMMENT> {

  // !!! handle /* in traditional comment

  \*+ "/"			{
				  yybegin(YYINITIAL);

				  if (string.length() > 0 && string.charAt(0) == '*') {
				    //!!! graf 001222: first '*' should be removed
				    addComment(new JavadocComment(string.toString(), false, false));
				  } else {
				    addComment(new JavaStyleComment(string.toString(), false, false, false));
				  }
				}

  [^\r\n*]+			{ string.append(yytext()); }
  \*+ [^*/\r\n]			{ string.append(yytext()); }
  \*+ {T}			{
				  incrementLine();
				  string.append(yytext());
				}
  {T}				{
				  incrementLine();
				  string.append(yytext());
				}
  <<EOF>>			{
				  reportTrouble(CompilerMessages.EOF_IN_TRADITIONAL_COMMENT, null);
				  return TOKEN_EOF;
				}
}

<ENDOFLINECOMMENT> {

  [^\r\n]+			{ string.append(yytext()); }
  {T}				{
				  yybegin(YYINITIAL);
				  incrementLine();
				  addComment(new JavaStyleComment(string.toString(), true, false, false));
				}
  <<EOF>>			{
				  reportTrouble(new CWarning(getTokenReference(),
							     CompilerMessages.EOF_IN_ENDOFLINE_COMMENT));
				  return TOKEN_EOF;
				}
}

/* error fallback */
.|\n				{ reportTrouble(CompilerMessages.ILLEGAL_CHAR, new Object[]{ yytext() }); }
