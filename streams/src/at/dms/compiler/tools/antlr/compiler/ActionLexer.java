// $ANTLR 1.5A: "action.g" -> "ActionLexer.java"$

package at.dms.compiler.tools.antlr.compiler;

import java.io.InputStream;
import java.io.Reader;
import java.util.Hashtable;
import at.dms.compiler.tools.antlr.runtime.*;

import java.io.StringReader;

/** Perform the following translations:

    Text related translations

	$append(x)		-> text.append(x)
	$setText(x)		-> text.setLength(_begin); text.append(x)
	$getText		-> new String(text.getBuffer(),_begin,text.length()-_begin)
	$setToken(x)	-> _token = x
	$setType(x)		-> _ttype = x
 */
public class ActionLexer extends at.dms.compiler.tools.antlr.runtime.CharScanner implements ActionLexerTokenTypes, TokenStream
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
public ActionLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public ActionLexer(Reader in) {
	this(new CharBuffer(in));
}
public ActionLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public ActionLexer(LexerSharedInputState state) {
	super(state);
	literals = new Hashtable();
caseSensitiveLiterals = true;
setCaseSensitive(true);
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				if ((_tokenSet_0.member(LA(1)))) {
					mACTION(true);
					theRetToken=_returnToken;
				}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
				}
				
				if ( _returnToken==null ) { continue tryAgain; } // found SKIP token
				_ttype = _returnToken.getType();
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mACTION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ACTION;
		int _saveIndex;
		
		{
		int _cnt3=0;
		_loop3:
		do {
			switch ( LA(1)) {
			case '\u0003':  case '\u0004':  case '\u0005':  case '\u0006':
			case '\u0007':  case '\u0008':  case '\t':  case '\n':
			case '\u000b':  case '\u000c':  case '\r':  case '\u000e':
			case '\u000f':  case '\u0010':  case '\u0011':  case '\u0012':
			case '\u0013':  case '\u0014':  case '\u0015':  case '\u0016':
			case '\u0017':  case '\u0018':  case '\u0019':  case '\u001a':
			case '\u001b':  case '\u001c':  case '\u001d':  case '\u001e':
			case '\u001f':  case ' ':  case '!':  case '"':
			case '%':  case '&':  case '\'':  case '(':
			case ')':  case '*':  case '+':  case ',':
			case '-':  case '.':  case '/':  case '0':
			case '1':  case '2':  case '3':  case '4':
			case '5':  case '6':  case '7':  case '8':
			case '9':  case ':':  case ';':  case '<':
			case '=':  case '>':  case '?':  case '@':
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':  case '[':  case '\\':
			case ']':  case '^':  case '_':  case '`':
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':  case '{':  case '|':
			case '}':  case '~':
			{
				mSTUFF(false);
				break;
			}
			case '$':
			{
				mTEXT_ITEM(false);
				break;
			}
			default:
			{
				if ( _cnt3>=1 ) { break _loop3; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
			}
			}
			_cnt3++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSTUFF(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STUFF;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '"':
		{
			mSTRING(false);
			break;
		}
		case '\'':
		{
			mCHAR(false);
			break;
		}
		case '\n':
		{
			match('\n');
			newline();
			break;
		}
		case '\u0003':  case '\u0004':  case '\u0005':  case '\u0006':
		case '\u0007':  case '\u0008':  case '\t':  case '\u000b':
		case '\u000c':  case '\u000e':  case '\u000f':  case '\u0010':
		case '\u0011':  case '\u0012':  case '\u0013':  case '\u0014':
		case '\u0015':  case '\u0016':  case '\u0017':  case '\u0018':
		case '\u0019':  case '\u001a':  case '\u001b':  case '\u001c':
		case '\u001d':  case '\u001e':  case '\u001f':  case ' ':
		case '!':  case '%':  case '&':  case '(':
		case ')':  case '*':  case '+':  case ',':
		case '-':  case '.':  case '0':  case '1':
		case '2':  case '3':  case '4':  case '5':
		case '6':  case '7':  case '8':  case '9':
		case ':':  case ';':  case '<':  case '=':
		case '>':  case '?':  case '@':  case 'A':
		case 'B':  case 'C':  case 'D':  case 'E':
		case 'F':  case 'G':  case 'H':  case 'I':
		case 'J':  case 'K':  case 'L':  case 'M':
		case 'N':  case 'O':  case 'P':  case 'Q':
		case 'R':  case 'S':  case 'T':  case 'U':
		case 'V':  case 'W':  case 'X':  case 'Y':
		case 'Z':  case '[':  case '\\':  case ']':
		case '^':  case '_':  case '`':  case 'a':
		case 'b':  case 'c':  case 'd':  case 'e':
		case 'f':  case 'g':  case 'h':  case 'i':
		case 'j':  case 'k':  case 'l':  case 'm':
		case 'n':  case 'o':  case 'p':  case 'q':
		case 'r':  case 's':  case 't':  case 'u':
		case 'v':  case 'w':  case 'x':  case 'y':
		case 'z':  case '{':  case '|':  case '}':
		case '~':
		{
			{
			match(_tokenSet_1);
			}
			break;
		}
		default:
			if ((LA(1)=='/') && (LA(2)=='*'||LA(2)=='/')) {
				mCOMMENT(false);
			}
			else if ((LA(1)=='\r') && (LA(2)=='\n')) {
				match("\r\n");
				newline();
			}
			else if ((LA(1)=='/') && (_tokenSet_2.member(LA(2)))) {
				match('/');
				{
				match(_tokenSet_2);
				}
			}
			else if ((LA(1)=='\r') && (true)) {
				match('\r');
				newline();
			}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTEXT_ITEM(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TEXT_ITEM;
		int _saveIndex;
		Token a1=null;
		Token a2=null;
		Token a3=null;
		Token a4=null;
		
		if ((LA(1)=='$') && (LA(2)=='a')) {
			match("$append(");
			mTEXT_ARG(true);
			a1=_returnToken;
			match(')');
			
						String t = "text.append("+a1.getText()+")";
						text.setLength(_begin); text.append(t);
					
		}
		else if ((LA(1)=='$') && (LA(2)=='s')) {
			match("$set");
			{
			if ((LA(1)=='T') && (LA(2)=='e')) {
				match("Text(");
				mTEXT_ARG(true);
				a2=_returnToken;
				match(')');
				
							String t;
							t = "text.setLength(_begin); text.append("+a2.getText()+")";
							text.setLength(_begin); text.append(t);
							
			}
			else if ((LA(1)=='T') && (LA(2)=='o')) {
				match("Token(");
				mTEXT_ARG(true);
				a3=_returnToken;
				match(')');
				
							String t="_token = "+a3.getText();
							text.setLength(_begin); text.append(t);
							
			}
			else if ((LA(1)=='T') && (LA(2)=='y')) {
				match("Type(");
				mTEXT_ARG(true);
				a4=_returnToken;
				match(')');
				
							String t="_ttype = "+a4.getText();
							text.setLength(_begin); text.append(t);
							
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
			}
			
			}
		}
		else if ((LA(1)=='$') && (LA(2)=='g')) {
			match("$getText");
			
						text.setLength(_begin); text.append("new String(text.getBuffer(),_begin,text.length()-_begin)");
					
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCOMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMMENT;
		int _saveIndex;
		
		if ((LA(1)=='/') && (LA(2)=='/')) {
			mSL_COMMENT(false);
		}
		else if ((LA(1)=='/') && (LA(2)=='*')) {
			mML_COMMENT(false);
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSTRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRING;
		int _saveIndex;
		
		match('"');
		{
		_loop53:
		do {
			switch ( LA(1)) {
			case '\\':
			{
				mESC(false);
				break;
			}
			case '\u0003':  case '\u0004':  case '\u0005':  case '\u0006':
			case '\u0007':  case '\u0008':  case '\t':  case '\n':
			case '\u000b':  case '\u000c':  case '\r':  case '\u000e':
			case '\u000f':  case '\u0010':  case '\u0011':  case '\u0012':
			case '\u0013':  case '\u0014':  case '\u0015':  case '\u0016':
			case '\u0017':  case '\u0018':  case '\u0019':  case '\u001a':
			case '\u001b':  case '\u001c':  case '\u001d':  case '\u001e':
			case '\u001f':  case ' ':  case '!':  case '#':
			case '$':  case '%':  case '&':  case '\'':
			case '(':  case ')':  case '*':  case '+':
			case ',':  case '-':  case '.':  case '/':
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':  case ':':  case ';':
			case '<':  case '=':  case '>':  case '?':
			case '@':  case 'A':  case 'B':  case 'C':
			case 'D':  case 'E':  case 'F':  case 'G':
			case 'H':  case 'I':  case 'J':  case 'K':
			case 'L':  case 'M':  case 'N':  case 'O':
			case 'P':  case 'Q':  case 'R':  case 'S':
			case 'T':  case 'U':  case 'V':  case 'W':
			case 'X':  case 'Y':  case 'Z':  case '[':
			case ']':  case '^':  case '_':  case '`':
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':  case '{':  case '|':
			case '}':  case '~':
			{
				matchNot('"');
				break;
			}
			default:
			{
				break _loop53;
			}
			}
		} while (true);
		}
		match('"');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCHAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CHAR;
		int _saveIndex;
		
		match('\'');
		{
		switch ( LA(1)) {
		case '\\':
		{
			mESC(false);
			break;
		}
		case '\u0003':  case '\u0004':  case '\u0005':  case '\u0006':
		case '\u0007':  case '\u0008':  case '\t':  case '\n':
		case '\u000b':  case '\u000c':  case '\r':  case '\u000e':
		case '\u000f':  case '\u0010':  case '\u0011':  case '\u0012':
		case '\u0013':  case '\u0014':  case '\u0015':  case '\u0016':
		case '\u0017':  case '\u0018':  case '\u0019':  case '\u001a':
		case '\u001b':  case '\u001c':  case '\u001d':  case '\u001e':
		case '\u001f':  case ' ':  case '!':  case '"':
		case '#':  case '$':  case '%':  case '&':
		case '(':  case ')':  case '*':  case '+':
		case ',':  case '-':  case '.':  case '/':
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':  case ':':  case ';':
		case '<':  case '=':  case '>':  case '?':
		case '@':  case 'A':  case 'B':  case 'C':
		case 'D':  case 'E':  case 'F':  case 'G':
		case 'H':  case 'I':  case 'J':  case 'K':
		case 'L':  case 'M':  case 'N':  case 'O':
		case 'P':  case 'Q':  case 'R':  case 'S':
		case 'T':  case 'U':  case 'V':  case 'W':
		case 'X':  case 'Y':  case 'Z':  case '[':
		case ']':  case '^':  case '_':  case '`':
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':  case '{':  case '|':
		case '}':  case '~':
		{
			matchNot('\'');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		match('\'');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTEXT_ARG(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TEXT_ARG;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '\t':  case '\n':  case '\r':  case ' ':
		{
			mWS(false);
			break;
		}
		case '"':  case '$':  case '\'':  case '+':
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':  case 'A':  case 'B':
		case 'C':  case 'D':  case 'E':  case 'F':
		case 'G':  case 'H':  case 'I':  case 'J':
		case 'K':  case 'L':  case 'M':  case 'N':
		case 'O':  case 'P':  case 'Q':  case 'R':
		case 'S':  case 'T':  case 'U':  case 'V':
		case 'W':  case 'X':  case 'Y':  case 'Z':
		case '_':  case 'a':  case 'b':  case 'c':
		case 'd':  case 'e':  case 'f':  case 'g':
		case 'h':  case 'i':  case 'j':  case 'k':
		case 'l':  case 'm':  case 'n':  case 'o':
		case 'p':  case 'q':  case 'r':  case 's':
		case 't':  case 'u':  case 'v':  case 'w':
		case 'x':  case 'y':  case 'z':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		{
		int _cnt13=0;
		_loop13:
		do {
			if ((_tokenSet_3.member(LA(1))) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				mTEXT_ARG_ELEMENT(false);
				{
				if ((_tokenSet_4.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
					mWS(false);
				}
				else if ((_tokenSet_5.member(LA(1))) && (true)) {
				}
				else {
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				
				}
			}
			else {
				if ( _cnt13>=1 ) { break _loop13; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
			}
			
			_cnt13++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mWS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WS;
		int _saveIndex;
		
		{
		int _cnt73=0;
		_loop73:
		do {
			if ((LA(1)==' ') && (_tokenSet_6.member(LA(2)))) {
				match(' ');
			}
			else if ((LA(1)=='\t') && (_tokenSet_6.member(LA(2)))) {
				match('\t');
			}
			else if ((LA(1)=='\r') && (LA(2)=='\n')) {
				match('\r');
				match('\n');
				newline();
			}
			else if ((LA(1)=='\r') && (_tokenSet_6.member(LA(2)))) {
				match('\r');
				newline();
			}
			else if ((LA(1)=='\n') && (_tokenSet_6.member(LA(2)))) {
				match('\n');
				newline();
			}
			else {
				if ( _cnt73>=1 ) { break _loop73; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
			}
			
			_cnt73++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTEXT_ARG_ELEMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TEXT_ARG_ELEMENT;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':  case '_':  case 'a':
		case 'b':  case 'c':  case 'd':  case 'e':
		case 'f':  case 'g':  case 'h':  case 'i':
		case 'j':  case 'k':  case 'l':  case 'm':
		case 'n':  case 'o':  case 'p':  case 'q':
		case 'r':  case 's':  case 't':  case 'u':
		case 'v':  case 'w':  case 'x':  case 'y':
		case 'z':
		{
			mTEXT_ARG_ID_ELEMENT(false);
			break;
		}
		case '"':
		{
			mSTRING(false);
			break;
		}
		case '\'':
		{
			mCHAR(false);
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			mINT_OR_FLOAT(false);
			break;
		}
		case '$':
		{
			mTEXT_ITEM(false);
			break;
		}
		case '+':
		{
			match('+');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mTEXT_ARG_ID_ELEMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TEXT_ARG_ID_ELEMENT;
		int _saveIndex;
		Token id=null;
		
		mID(true);
		id=_returnToken;
		{
		if ((_tokenSet_4.member(LA(1))) && (_tokenSet_7.member(LA(2)))) {
			mWS(false);
		}
		else if ((_tokenSet_7.member(LA(1))) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		
		}
		{
		switch ( LA(1)) {
		case '(':
		{
			match('(');
			{
			if ((_tokenSet_4.member(LA(1))) && (_tokenSet_8.member(LA(2)))) {
				mWS(false);
			}
			else if ((_tokenSet_8.member(LA(1))) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
			}
			
			}
			{
			_loop22:
			do {
				if ((_tokenSet_9.member(LA(1))) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
					mTEXT_ARG(false);
					{
					_loop21:
					do {
						if ((LA(1)==',')) {
							match(',');
							mTEXT_ARG(false);
						}
						else {
							break _loop21;
						}
						
					} while (true);
					}
				}
				else {
					break _loop22;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case '\t':  case '\n':  case '\r':  case ' ':
			{
				mWS(false);
				break;
			}
			case ')':
			{
				break;
			}
			default:
			{
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
			}
			}
			}
			match(')');
			break;
		}
		case '[':
		{
			{
			int _cnt27=0;
			_loop27:
			do {
				if ((LA(1)=='[')) {
					match('[');
					{
					if ((_tokenSet_4.member(LA(1))) && (_tokenSet_9.member(LA(2)))) {
						mWS(false);
					}
					else if ((_tokenSet_9.member(LA(1))) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
					}
					else {
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
					}
					
					}
					mTEXT_ARG(false);
					{
					switch ( LA(1)) {
					case '\t':  case '\n':  case '\r':  case ' ':
					{
						mWS(false);
						break;
					}
					case ']':
					{
						break;
					}
					default:
					{
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
					}
					}
					}
					match(']');
				}
				else {
					if ( _cnt27>=1 ) { break _loop27; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
				}
				
				_cnt27++;
			} while (true);
			}
			break;
		}
		case '.':
		{
			match('.');
			mTEXT_ARG_ID_ELEMENT(false);
			break;
		}
		case '\t':  case '\n':  case '\r':  case ' ':
		case '"':  case '$':  case '\'':  case ')':
		case '+':  case ',':  case '0':  case '1':
		case '2':  case '3':  case '4':  case '5':
		case '6':  case '7':  case '8':  case '9':
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':  case ']':  case '_':
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mINT_OR_FLOAT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INT_OR_FLOAT;
		int _saveIndex;
		
		{
		int _cnt67=0;
		_loop67:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_10.member(LA(2)))) {
				mDIGIT(false);
			}
			else {
				if ( _cnt67>=1 ) { break _loop67; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
			}
			
			_cnt67++;
		} while (true);
		}
		{
		if ((LA(1)=='L') && (_tokenSet_11.member(LA(2)))) {
			match('L');
		}
		else if ((LA(1)=='l') && (_tokenSet_11.member(LA(2)))) {
			match('l');
		}
		else if ((LA(1)=='.')) {
			match('.');
			{
			_loop70:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_11.member(LA(2)))) {
					mDIGIT(false);
				}
				else {
					break _loop70;
				}
				
			} while (true);
			}
		}
		else if ((_tokenSet_11.member(LA(1))) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ID;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			matchRange('a','z');
			break;
		}
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':
		{
			matchRange('A','Z');
			break;
		}
		case '_':
		{
			match('_');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		{
		_loop39:
		do {
			if ((_tokenSet_12.member(LA(1))) && (_tokenSet_7.member(LA(2)))) {
				{
				switch ( LA(1)) {
				case 'a':  case 'b':  case 'c':  case 'd':
				case 'e':  case 'f':  case 'g':  case 'h':
				case 'i':  case 'j':  case 'k':  case 'l':
				case 'm':  case 'n':  case 'o':  case 'p':
				case 'q':  case 'r':  case 's':  case 't':
				case 'u':  case 'v':  case 'w':  case 'x':
				case 'y':  case 'z':
				{
					matchRange('a','z');
					break;
				}
				case 'A':  case 'B':  case 'C':  case 'D':
				case 'E':  case 'F':  case 'G':  case 'H':
				case 'I':  case 'J':  case 'K':  case 'L':
				case 'M':  case 'N':  case 'O':  case 'P':
				case 'Q':  case 'R':  case 'S':  case 'T':
				case 'U':  case 'V':  case 'W':  case 'X':
				case 'Y':  case 'Z':
				{
					matchRange('A','Z');
					break;
				}
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':
				{
					matchRange('0','9');
					break;
				}
				case '_':
				{
					match('_');
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				}
				}
			}
			else {
				break _loop39;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mARG(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ARG;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '"':
		{
			mSTRING(false);
			break;
		}
		case '\'':
		{
			mCHAR(false);
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			mINT_OR_FLOAT(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		{
		_loop34:
		do {
			if ((_tokenSet_13.member(LA(1))) && (_tokenSet_14.member(LA(2)))) {
				{
				switch ( LA(1)) {
				case '\t':  case '\n':  case '\r':  case ' ':
				{
					mWS(false);
					break;
				}
				case '*':  case '+':  case '-':  case '/':
				{
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				}
				}
				{
				switch ( LA(1)) {
				case '+':
				{
					match('+');
					break;
				}
				case '-':
				{
					match('-');
					break;
				}
				case '*':
				{
					match('*');
					break;
				}
				case '/':
				{
					match('/');
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				}
				}
				{
				switch ( LA(1)) {
				case '\t':  case '\n':  case '\r':  case ' ':
				{
					mWS(false);
					break;
				}
				case '"':  case '\'':  case '0':  case '1':
				case '2':  case '3':  case '4':  case '5':
				case '6':  case '7':  case '8':  case '9':
				{
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				}
				}
				mARG(false);
			}
			else {
				break _loop34;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mVAR_ASSIGN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = VAR_ASSIGN;
		int _saveIndex;
		
		match('=');
		
				
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSL_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SL_COMMENT;
		int _saveIndex;
		
		match("//");
		{
		_loop44:
		do {
			// nongreedy exit test
			if ((LA(1)=='\n'||LA(1)=='\r') && (true)) { break _loop44; }
			if (((LA(1) >= '\u0003' && LA(1) <= '~')) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				matchNot(EOF_CHAR);
			}
			else {
				break _loop44;
			}
			
		} while (true);
		}
		{
		if ((LA(1)=='\r') && (LA(2)=='\n')) {
			match("\r\n");
		}
		else if ((LA(1)=='\n')) {
			match('\n');
		}
		else if ((LA(1)=='\r') && (true)) {
			match('\r');
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		
		}
		newline();
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mML_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ML_COMMENT;
		int _saveIndex;
		
		match("/*");
		{
		_loop48:
		do {
			// nongreedy exit test
			if ((LA(1)=='*') && (LA(2)=='/')) { break _loop48; }
			if ((LA(1)=='\r') && (LA(2)=='\n')) {
				match('\r');
				match('\n');
				newline();
			}
			else if ((LA(1)=='\r') && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				match('\r');
				newline();
			}
			else if ((LA(1)=='\n') && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				match('\n');
				newline();
			}
			else if (((LA(1) >= '\u0003' && LA(1) <= '~')) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				matchNot(EOF_CHAR);
			}
			else {
				break _loop48;
			}
			
		} while (true);
		}
		match("*/");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ESC;
		int _saveIndex;
		
		match('\\');
		{
		switch ( LA(1)) {
		case 'n':
		{
			match('n');
			break;
		}
		case 'r':
		{
			match('r');
			break;
		}
		case 't':
		{
			match('t');
			break;
		}
		case 'b':
		{
			match('b');
			break;
		}
		case 'f':
		{
			match('f');
			break;
		}
		case '"':
		{
			match('"');
			break;
		}
		case '\'':
		{
			match('\'');
			break;
		}
		case '\\':
		{
			match('\\');
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		{
			{
			matchRange('0','3');
			}
			{
			if (((LA(1) >= '0' && LA(1) <= '9')) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				mDIGIT(false);
				{
				if (((LA(1) >= '0' && LA(1) <= '9')) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
					mDIGIT(false);
				}
				else if (((LA(1) >= '\u0003' && LA(1) <= '~')) && (true)) {
				}
				else {
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
				}
				
				}
			}
			else if (((LA(1) >= '\u0003' && LA(1) <= '~')) && (true)) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
			}
			
			}
			break;
		}
		case '4':  case '5':  case '6':  case '7':
		{
			{
			matchRange('4','7');
			}
			{
			if (((LA(1) >= '0' && LA(1) <= '9')) && ((LA(2) >= '\u0003' && LA(2) <= '~'))) {
				mDIGIT(false);
			}
			else if (((LA(1) >= '\u0003' && LA(1) <= '~')) && (true)) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
			}
			
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DIGIT;
		int _saveIndex;
		
		matchRange('0','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mINT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INT;
		int _saveIndex;
		
		{
		int _cnt64=0;
		_loop64:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				mDIGIT(false);
			}
			else {
				if ( _cnt64>=1 ) { break _loop64; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
			}
			
			_cnt64++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] _tokenSet_0_data_ = { -34359738376L, 9223372036854775807L, 0L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long[] _tokenSet_1_data_ = { -141407503262728L, 9223372036854775807L, 0L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long[] _tokenSet_2_data_ = { -145135534866440L, 9223372036854775807L, 0L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	private static final long[] _tokenSet_3_data_ = { 287958332923183104L, 576460745995190270L, 0L, 0L };
	public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
	private static final long[] _tokenSet_4_data_ = { 4294977024L, 0L, 0L };
	public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
	private static final long[] _tokenSet_5_data_ = { 287978128427460096L, 576460746532061182L, 0L, 0L };
	public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
	private static final long[] _tokenSet_6_data_ = { 288229916590220800L, 576460746666278910L, 0L, 0L };
	public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
	private static final long[] _tokenSet_7_data_ = { 288049596683265536L, 576460746666278910L, 0L, 0L };
	public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);
	private static final long[] _tokenSet_8_data_ = { 287960536241415680L, 576460745995190270L, 0L, 0L };
	public static final BitSet _tokenSet_8 = new BitSet(_tokenSet_8_data_);
	private static final long[] _tokenSet_9_data_ = { 287958337218160128L, 576460745995190270L, 0L, 0L };
	public static final BitSet _tokenSet_9 = new BitSet(_tokenSet_9_data_);
	private static final long[] _tokenSet_10_data_ = { 288228817078593024L, 576460746532061182L, 0L, 0L };
	public static final BitSet _tokenSet_10 = new BitSet(_tokenSet_10_data_);
	private static final long[] _tokenSet_11_data_ = { 288158448334415360L, 576460746532061182L, 0L, 0L };
	public static final BitSet _tokenSet_11 = new BitSet(_tokenSet_11_data_);
	private static final long[] _tokenSet_12_data_ = { 287948901175001088L, 576460745995190270L, 0L, 0L };
	public static final BitSet _tokenSet_12 = new BitSet(_tokenSet_12_data_);
	private static final long[] _tokenSet_13_data_ = { 189120294954496L, 0L, 0L };
	public static final BitSet _tokenSet_13 = new BitSet(_tokenSet_13_data_);
	private static final long[] _tokenSet_14_data_ = { 288138588405638656L, 0L, 0L };
	public static final BitSet _tokenSet_14 = new BitSet(_tokenSet_14_data_);
	
	}
