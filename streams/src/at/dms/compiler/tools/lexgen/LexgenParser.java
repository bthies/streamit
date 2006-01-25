// $ANTLR 1.5A: "Lexgen.g" -> "LexgenParser.java"$
package at.dms.compiler.tools.lexgen; 
import at.dms.compiler.tools.antlr.runtime.*;

import java.util.Vector;

public class LexgenParser extends at.dms.compiler.tools.antlr.runtime.LLkParser
    implements LexgenTokenTypes
{

    protected LexgenParser(TokenBuffer tokenBuf, int k) {
        super(tokenBuf,k);
        tokenNames = _tokenNames;
    }

    public LexgenParser(TokenBuffer tokenBuf) {
        this(tokenBuf,2);
    }

    protected LexgenParser(TokenStream lexer, int k) {
        super(lexer,k);
        tokenNames = _tokenNames;
    }

    public LexgenParser(TokenStream lexer) {
        this(lexer,2);
    }

    public LexgenParser(ParserSharedInputState state) {
        super(state,2);
        tokenNames = _tokenNames;
    }

    public final DefinitionFile  aCompilationUnit(
                                                  String sourceFile
                                                  ) throws RecognitionException, TokenStreamException {
        DefinitionFile self;
        
        
        String      prefix = null;
        String      packageName = null;
        String      vocabulary = null;
        Vector      definitions = new Vector();
        TokenDefinition def;
        
        
        {
            switch ( LA(1)) {
            case 4:
                {
                    match(4);
                    prefix=aIdentifier();
                    break;
                }
            case 5:
            case 6:
                {
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        {
            switch ( LA(1)) {
            case 5:
                {
                    match(5);
                    packageName=aName();
                    break;
                }
            case 6:
                {
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        match(6);
        vocabulary=aIdentifier();
        {
            _loop5:
            do {
                if (((LA(1) >= 10 && LA(1) <= 13))) {
                    def=aTokenDefinition();
                    definitions.addElement(def);
                }
                else {
                    break _loop5;
                }
            
            } while (true);
        }
        self = new DefinitionFile(sourceFile, packageName, vocabulary, prefix, definitions);
        return self;
    }
    
    private final String  aIdentifier(
        
    ) throws RecognitionException, TokenStreamException {
        String self = null;
        
        Token  t = null;
        
        t = LT(1);
        match(IDENT);
        self = t.getText();
        return self;
    }
    
    private final String  aName(
        
    ) throws RecognitionException, TokenStreamException {
        String self;
        
        
        String  ident;
        
        
        ident=aIdentifier();
        self = ident;
        {
            _loop9:
            do {
                if ((LA(1)==DOT)) {
                    match(DOT);
                    ident=aIdentifier();
                    self += "." + ident;
                }
                else {
                    break _loop9;
                }
            
            } while (true);
        }
        return self;
    }
    
    private final TokenDefinition  aTokenDefinition(
        
    ) throws RecognitionException, TokenStreamException {
        TokenDefinition self;
        
        
        int     tokenType = TokenDefinition.OTHER;
        String  ident;
        String  value = null;
        
        
        {
            switch ( LA(1)) {
            case 10:
                {
                    match(10);
                    tokenType = TokenDefinition.LITERAL;
                    break;
                }
            case 11:
                {
                    match(11);
                    tokenType = TokenDefinition.KEYWORD;
                    break;
                }
            case 12:
                {
                    match(12);
                    tokenType = TokenDefinition.OPERATOR;
                    break;
                }
            case 13:
                {
                    match(13);
                    tokenType = TokenDefinition.OTHER;
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        ident=aIdentifier();
        {
            switch ( LA(1)) {
            case STRING:
                {
                    value=aString();
                    break;
                }
            case EOF:
            case 10:
            case 11:
            case 12:
            case 13:
                {
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        self = new TokenDefinition(tokenType, ident, value);
        return self;
    }
    
    private final String  aString(
        
    ) throws RecognitionException, TokenStreamException {
        String self;
        
        Token  t = null;
        
        t = LT(1);
        match(STRING);
        self = t.getText();
        return self;
    }
    
    
    public static final String[] _tokenNames = {
        "<0>",
        "EOF",
        "<2>",
        "NULL_TREE_LOOKAHEAD",
        "\"@prefix\"",
        "\"@package\"",
        "\"@vocabulary\"",
        "IDENT",
        "DOT",
        "STRING",
        "\"@literal\"",
        "\"@keyword\"",
        "\"@operator\"",
        "\"@token\""
    };
    
    
}
