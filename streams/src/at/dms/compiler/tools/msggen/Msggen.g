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
 * $Id: Msggen.g,v 1.1 2001-08-30 16:32:45 thies Exp $
 */

// Import the necessary classes
header { package at.dms.compiler.tools.msggen; }

{
  import java.util.Vector;

  import at.dms.util.MessageDescription;
  import at.dms.util.Utils;
}


//-----------------------------------------------------------------------------
// THE PARSER STARTS HERE
//-----------------------------------------------------------------------------

class MsggenParser extends Parser;

options {
  k = 2;				// two token lookahead
  exportVocab=Msggen;			// call the vocabulary
  codeGenMakeSwitchThreshold = 2;	// Some optimizations
  codeGenBitsetTestThreshold = 3;
  defaultErrorHandler = false;		// Don't generate parser error handlers
  access = "private";			// Set default rule access
}

public aCompilationUnit [String sourceFile]
  returns [DefinitionFile self]
{
  String		fileHeader = null;
  String		packageName = null;
  String		prefix = null;
  String		parent = null;
  MessageDefinition[]	definitions = null;
}
:
  ( fileHeader = aFileHeader[] )?
  ( "package" packageName = aName[] )?
  ( "prefix" prefix = aIdentifier[] ) ?
  ( "parent" parent = aName[] ) ?
  definitions = aDefinitions[]
    {
      self = new DefinitionFile(sourceFile,
				fileHeader,
				packageName,
				prefix,
				parent,
				definitions);
    }
;

aDefinitions []
  returns [MessageDefinition[] self = null]
{
  Vector		container = new Vector();
  MessageDefinition	def;
}
:
  (
    def = aMessageDefinition[]
      { container.addElement(def); }
  )*
    { self = (MessageDefinition[])Utils.toArray(container, MessageDefinition.class); }
;

aFileHeader []
  returns [String self = null]
:
  t : HEADER
    { self = t.getText().substring(1, t.getText().length() - 1); }
;

aIdentifier []
  returns [String self = null]
:
  t : IDENT
    { self = t.getText(); }
;

aName []
  returns [String self]
{
  String	ident;
}
:
  ident = aIdentifier[]
    { self = ident; }
  (
    DOT ident = aIdentifier[]
      { self += "." + ident; }
  )*
;

aMessageDefinition []
  returns [MessageDefinition self]
{
  int		type = 0;
  String	name;
  String	message;
  String	reference = null;
}
:
  (
    "message"
      { type = -1; }
  |
    "error"
      { type = MessageDescription.LVL_ERROR; }
  |
    "caution"
      { type = MessageDescription.LVL_CAUTION; }
  |
    "warning"
      { type = MessageDescription.LVL_WARNING; }
  |
    "notice"
      { type = MessageDescription.LVL_NOTICE; }
  |
    "info"
      { type = MessageDescription.LVL_INFO; }
  )
  name = aIdentifier[]
  message = aString[]
  ( reference = aString[] ) ?
    { self = new MessageDefinition(name, message, reference, type); }
;

aString []
  returns [String self]
:
  t : STRING
    { self = t.getText(); }
;

//-----------------------------------------------------------------------------
// THE SCANNER STARTS HERE
//-----------------------------------------------------------------------------

class MsggenLexer extends Lexer;

options {
  importVocab=Msggen;       // call the vocabulary
  testLiterals=false;    // don't automatically test for literals
  k=2;                   // 2 characters of lookahead
}

WS :
  (
    ' '
  | '\t'
  | '\f'
  | '\r' ('\n')?	{ newline(); }
  | '\n'		{ newline(); }
  )
    { _ttype = Token.SKIP; }
;

DOT		: '.';

// file header to copy into the output file
HEADER :
	"{"
	(
	  '\n' { newline(); }
	|
	  ~'}'
	)*
	"}"
;

// Single-line comments
SL_COMMENT :
	"//"
	(~('\n'|'\r'))* ('\n'|'\r'('\n')?)
	{ _ttype = Token.SKIP; newline(); }
	;

// multiple-line comments
ML_COMMENT :
  "/*"
  (
    '\n' { newline(); }
  | '*' ~'/'
  | ~'*'
  )*
  "*/"
    { _ttype = Token.SKIP; }
;

STRING :
  '"' (ESC|~'"')* '"'
;

protected
ESC :
  '\\'
  (
    'n'
  | 'r'
  | 't'
  | 'b'
  | 'f'
  | '"'
  | '\''
  | '\\'
  | ('0'..'3') OCT_DIGIT OCT_DIGIT
  | 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  )
;

protected
OCT_DIGIT :
  '0'..'7'
;

protected
HEX_DIGIT :
  '0'..'9'
| 'A'..'F'
| 'a'..'f'
;

// an identifier.  Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
IDENT
  options { testLiterals=true; }
:
  ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|'$')*
;

// an identifier preceded by a '@' Note that testLiterals is set to true!  This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifer
DUMMY
  options { testLiterals=true; }
:
  '@' IDENT
;

// a dummy rule to force vocabulary to be all characters except special
// ones that ANTLR uses internally (0 to 2)
protected
VOCAB :
  '\3'..'\377'
;
