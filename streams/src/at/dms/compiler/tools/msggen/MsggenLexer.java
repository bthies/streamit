// $ANTLR 1.5A: "Msggen.g" -> "MsggenLexer.java"$
package at.dms.compiler.tools.msggen; 
import java.io.InputStream;
import java.io.Reader;
import java.util.Hashtable;
import at.dms.compiler.tools.antlr.runtime.*;

public class MsggenLexer extends at.dms.compiler.tools.antlr.runtime.CharScanner implements MsggenLexerTokenTypes, TokenStream
{
    public MsggenLexer(InputStream in) {
        this(new ByteBuffer(in));
    }
    public MsggenLexer(Reader in) {
        this(new CharBuffer(in));
    }
    public MsggenLexer(InputBuffer ib) {
        this(new LexerSharedInputState(ib));
    }
    public MsggenLexer(LexerSharedInputState state) {
        super(state);
        literals = new Hashtable<ANTLRHashString, Integer>();
        literals.put(new ANTLRHashString("prefix", this), new Integer(5));
        literals.put(new ANTLRHashString("error", this), new Integer(11));
        literals.put(new ANTLRHashString("caution", this), new Integer(12));
        literals.put(new ANTLRHashString("notice", this), new Integer(14));
        literals.put(new ANTLRHashString("warning", this), new Integer(13));
        literals.put(new ANTLRHashString("package", this), new Integer(4));
        literals.put(new ANTLRHashString("info", this), new Integer(15));
        literals.put(new ANTLRHashString("message", this), new Integer(10));
        literals.put(new ANTLRHashString("parent", this), new Integer(6));
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
                    switch ( LA(1)) {
                    case '\t':  case '\n':  case '\u000c':  case '\r':
                    case ' ':
                        {
                            mWS(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    case '.':
                        {
                            mDOT(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    case '{':
                        {
                            mHEADER(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    case '"':
                        {
                            mSTRING(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    case '$':  case 'A':  case 'B':  case 'C':
                    case 'D':  case 'E':  case 'F':  case 'G':
                    case 'H':  case 'I':  case 'J':  case 'K':
                    case 'L':  case 'M':  case 'N':  case 'O':
                    case 'P':  case 'Q':  case 'R':  case 'S':
                    case 'T':  case 'U':  case 'V':  case 'W':
                    case 'X':  case 'Y':  case 'Z':  case '_':
                    case 'a':  case 'b':  case 'c':  case 'd':
                    case 'e':  case 'f':  case 'g':  case 'h':
                    case 'i':  case 'j':  case 'k':  case 'l':
                    case 'm':  case 'n':  case 'o':  case 'p':
                    case 'q':  case 'r':  case 's':  case 't':
                    case 'u':  case 'v':  case 'w':  case 'x':
                    case 'y':  case 'z':
                        {
                            mIDENT(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    case '@':
                        {
                            mDUMMY(true);
                            theRetToken=_returnToken;
                            break;
                        }
                    default:
                        if ((LA(1)=='/') && (LA(2)=='/')) {
                            mSL_COMMENT(true);
                            theRetToken=_returnToken;
                        }
                        else if ((LA(1)=='/') && (LA(2)=='*')) {
                            mML_COMMENT(true);
                            theRetToken=_returnToken;
                        }
                        else {
                            if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
                            else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());}
                        }
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

    public final void mWS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = WS;
        int _saveIndex;
        
        {
            switch ( LA(1)) {
            case ' ':
                {
                    match(' ');
                    break;
                }
            case '\t':
                {
                    match('\t');
                    break;
                }
            case '\u000c':
                {
                    match('\f');
                    break;
                }
            case '\r':
                {
                    match('\r');
                    {
                        if ((LA(1)=='\n')) {
                            match('\n');
                        }
                        else {
                        }
            
                    }
                    newline();
                    break;
                }
            case '\n':
                {
                    match('\n');
                    newline();
                    break;
                }
            default:
                {
                    throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                }
            }
        }
        _ttype = Token.SKIP;
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mDOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = DOT;
        int _saveIndex;
        
        match('.');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mHEADER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = HEADER;
        int _saveIndex;
        
        match("{");
        {
            _loop24:
            do {
                if ((LA(1)=='\n')) {
                    match('\n');
                    newline();
                }
                else if ((_tokenSet_0.member(LA(1)))) {
                    matchNot('}');
                }
                else {
                    break _loop24;
                }
            
            } while (true);
        }
        match("}");
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mSL_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = SL_COMMENT;
        int _saveIndex;
        
        match("//");
        {
            _loop28:
            do {
                if ((_tokenSet_1.member(LA(1)))) {
                    {
                        match(_tokenSet_1);
                    }
                }
                else {
                    break _loop28;
                }
            
            } while (true);
        }
        {
            switch ( LA(1)) {
            case '\n':
                {
                    match('\n');
                    break;
                }
            case '\r':
                {
                    match('\r');
                    {
                        if ((LA(1)=='\n')) {
                            match('\n');
                        }
                        else {
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
        _ttype = Token.SKIP; newline();
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mML_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = ML_COMMENT;
        int _saveIndex;
        
        match("/*");
        {
            _loop33:
            do {
                if ((LA(1)=='*') && (_tokenSet_2.member(LA(2)))) {
                    match('*');
                    matchNot('/');
                }
                else if ((LA(1)=='\n')) {
                    match('\n');
                    newline();
                }
                else if ((_tokenSet_3.member(LA(1)))) {
                    matchNot('*');
                }
                else {
                    break _loop33;
                }
            
            } while (true);
        }
        match("*/");
        _ttype = Token.SKIP;
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mSTRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = STRING;
        int _saveIndex;
        
        match('"');
        {
            _loop36:
            do {
                if ((LA(1)=='\\')) {
                    mESC(false);
                }
                else if ((_tokenSet_4.member(LA(1)))) {
                    matchNot('"');
                }
                else {
                    break _loop36;
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
                    mOCT_DIGIT(false);
                    mOCT_DIGIT(false);
                    break;
                }
            case 'u':
                {
                    match('u');
                    mHEX_DIGIT(false);
                    mHEX_DIGIT(false);
                    mHEX_DIGIT(false);
                    mHEX_DIGIT(false);
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
    
    protected final void mOCT_DIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = OCT_DIGIT;
        int _saveIndex;
        
        matchRange('0','7');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    protected final void mHEX_DIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = HEX_DIGIT;
        int _saveIndex;
        
        switch ( LA(1)) {
        case '0':  case '1':  case '2':  case '3':
        case '4':  case '5':  case '6':  case '7':
        case '8':  case '9':
            {
                matchRange('0','9');
                break;
            }
        case 'A':  case 'B':  case 'C':  case 'D':
        case 'E':  case 'F':
            {
                matchRange('A','F');
                break;
            }
        case 'a':  case 'b':  case 'c':  case 'd':
        case 'e':  case 'f':
            {
                matchRange('a','f');
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
    
    public final void mIDENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = IDENT;
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
            case '$':
                {
                    match('$');
                    break;
                }
            default:
                {
                    throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine());
                }
            }
        }
        {
            _loop45:
            do {
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
                case '0':  case '1':  case '2':  case '3':
                case '4':  case '5':  case '6':  case '7':
                case '8':  case '9':
                    {
                        matchRange('0','9');
                        break;
                    }
                case '$':
                    {
                        match('$');
                        break;
                    }
                default:
                    {
                        break _loop45;
                    }
                }
            } while (true);
        }
        _ttype = testLiteralsTable(_ttype);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    public final void mDUMMY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = DUMMY;
        int _saveIndex;
        
        match('@');
        mIDENT(false);
        _ttype = testLiteralsTable(_ttype);
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    protected final void mVOCAB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
        int _ttype; Token _token=null; int _begin=text.length();
        _ttype = VOCAB;
        int _saveIndex;
        
        matchRange('\3','\377');
        if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
            _token = makeToken(_ttype);
            _token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
        }
        _returnToken = _token;
    }
    
    
    private static final long[] _tokenSet_0_data_ = { -1032L, -2305843009213693953L, -1L, -1L, 0L, 0L, 0L, 0L };
    public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
    private static final long[] _tokenSet_1_data_ = { -9224L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
    public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
    private static final long[] _tokenSet_2_data_ = { -140737488355336L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
    public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
    private static final long[] _tokenSet_3_data_ = { -4398046512136L, -1L, -1L, -1L, 0L, 0L, 0L, 0L };
    public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
    private static final long[] _tokenSet_4_data_ = { -17179869192L, -268435457L, -1L, -1L, 0L, 0L, 0L, 0L };
    public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
    
}
