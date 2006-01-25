// $ANTLR 1.5A: "Msggen.g" -> "MsggenParser.java"$
package at.dms.compiler.tools.msggen; 
import at.dms.compiler.tools.antlr.runtime.*;

import java.util.Vector;

import at.dms.compiler.tools.common.MessageDescription;
import at.dms.compiler.tools.common.Utils;

public class MsggenParser extends at.dms.compiler.tools.antlr.runtime.LLkParser
    implements MsggenTokenTypes
{

    protected MsggenParser(TokenBuffer tokenBuf, int k) {
        super(tokenBuf,k);
        tokenNames = _tokenNames;
    }

    public MsggenParser(TokenBuffer tokenBuf) {
        this(tokenBuf,2);
    }

    protected MsggenParser(TokenStream lexer, int k) {
        super(lexer,k);
        tokenNames = _tokenNames;
    }

    public MsggenParser(TokenStream lexer) {
        this(lexer,2);
    }

    public MsggenParser(ParserSharedInputState state) {
        super(state,2);
        tokenNames = _tokenNames;
    }

    public final DefinitionFile  aCompilationUnit(
                                                  String sourceFile
                                                  ) throws RecognitionException, TokenStreamException {
        DefinitionFile self;
        
        
        String      fileHeader = null;
        String      packageName = null;
        String      prefix = null;
        String      parent = null;
        MessageDefinition[] definitions = null;
        
        
        {
            switch ( LA(1)) {
            case HEADER:
                {
                    fileHeader=aFileHeader();
                    break;
                }
            case EOF:
            case LITERAL_package:
            case LITERAL_prefix:
            case LITERAL_parent:
            case LITERAL_message:
            case LITERAL_error:
            case LITERAL_caution:
            case LITERAL_warning:
            case LITERAL_notice:
            case LITERAL_info:
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
            case LITERAL_package:
                {
                    match(LITERAL_package);
                    packageName=aName();
                    break;
                }
            case EOF:
            case LITERAL_prefix:
            case LITERAL_parent:
            case LITERAL_message:
            case LITERAL_error:
            case LITERAL_caution:
            case LITERAL_warning:
            case LITERAL_notice:
            case LITERAL_info:
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
            case LITERAL_prefix:
                {
                    match(LITERAL_prefix);
                    prefix=aIdentifier();
                    break;
                }
            case EOF:
            case LITERAL_parent:
            case LITERAL_message:
            case LITERAL_error:
            case LITERAL_caution:
            case LITERAL_warning:
            case LITERAL_notice:
            case LITERAL_info:
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
            case LITERAL_parent:
                {
                    match(LITERAL_parent);
                    parent=aName();
                    break;
                }
            case EOF:
            case LITERAL_message:
            case LITERAL_error:
            case LITERAL_caution:
            case LITERAL_warning:
            case LITERAL_notice:
            case LITERAL_info:
                {
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        definitions=aDefinitions();
        
        self = new DefinitionFile(sourceFile,
                                  fileHeader,
                                  packageName,
                                  prefix,
                                  parent,
                                  definitions);
        
        return self;
    }
    
    private final String  aFileHeader(
        
    ) throws RecognitionException, TokenStreamException {
        String self = null;
        
        Token  t = null;
        
        t = LT(1);
        match(HEADER);
        self = t.getText().substring(1, t.getText().length() - 1);
        return self;
    }
    
    private final String  aName(
        
    ) throws RecognitionException, TokenStreamException {
        String self;
        
        
        String  ident;
        
        
        ident=aIdentifier();
        self = ident;
        {
            _loop13:
            do {
                if ((LA(1)==DOT)) {
                    match(DOT);
                    ident=aIdentifier();
                    self += "." + ident;
                }
                else {
                    break _loop13;
                }
            
            } while (true);
        }
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
    
    private final MessageDefinition[]  aDefinitions(
        
    ) throws RecognitionException, TokenStreamException {
        MessageDefinition[] self = null;
        
        
        Vector      container = new Vector();
        MessageDefinition   def;
        
        
        {
            _loop8:
            do {
                if (((LA(1) >= LITERAL_message && LA(1) <= LITERAL_info))) {
                    def=aMessageDefinition();
                    container.addElement(def);
                }
                else {
                    break _loop8;
                }
            
            } while (true);
        }
        self = (MessageDefinition[])Utils.toArray(container, MessageDefinition.class);
        return self;
    }
    
    private final MessageDefinition  aMessageDefinition(
        
    ) throws RecognitionException, TokenStreamException {
        MessageDefinition self;
        
        
        int     type = 0;
        String  name;
        String  message;
        String  reference = null;
        
        
        {
            switch ( LA(1)) {
            case LITERAL_message:
                {
                    match(LITERAL_message);
                    type = -1;
                    break;
                }
            case LITERAL_error:
                {
                    match(LITERAL_error);
                    type = MessageDescription.LVL_ERROR;
                    break;
                }
            case LITERAL_caution:
                {
                    match(LITERAL_caution);
                    type = MessageDescription.LVL_CAUTION;
                    break;
                }
            case LITERAL_warning:
                {
                    match(LITERAL_warning);
                    type = MessageDescription.LVL_WARNING;
                    break;
                }
            case LITERAL_notice:
                {
                    match(LITERAL_notice);
                    type = MessageDescription.LVL_NOTICE;
                    break;
                }
            case LITERAL_info:
                {
                    match(LITERAL_info);
                    type = MessageDescription.LVL_INFO;
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        name=aIdentifier();
        message=aString();
        {
            switch ( LA(1)) {
            case STRING:
                {
                    reference=aString();
                    break;
                }
            case EOF:
            case LITERAL_message:
            case LITERAL_error:
            case LITERAL_caution:
            case LITERAL_warning:
            case LITERAL_notice:
            case LITERAL_info:
                {
                    break;
                }
            default:
                {
                    throw new NoViableAltException(LT(1), getFilename());
                }
            }
        }
        self = new MessageDefinition(name, message, reference, type);
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
        "\"package\"",
        "\"prefix\"",
        "\"parent\"",
        "HEADER",
        "IDENT",
        "DOT",
        "\"message\"",
        "\"error\"",
        "\"caution\"",
        "\"warning\"",
        "\"notice\"",
        "\"info\"",
        "STRING"
    };
    
    
}
