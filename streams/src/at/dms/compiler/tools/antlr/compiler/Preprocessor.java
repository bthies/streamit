// $ANTLR 1.5A: "preproc.g" -> "Preprocessor.java"$

package at.dms.compiler.tools.antlr.compiler;

import at.dms.compiler.tools.antlr.runtime.*;

import java.util.Hashtable;

public class Preprocessor extends at.dms.compiler.tools.antlr.runtime.LLkParser
       implements PreprocessorTokenTypes
 {

protected Preprocessor(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public Preprocessor(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected Preprocessor(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public Preprocessor(TokenStream lexer) {
  this(lexer,1);
}

public Preprocessor(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void grammarFile(
		Hierarchy hier, String file
	) throws RecognitionException, TokenStreamException {
		
		Token  hdr = null;
		
			GrammarDefinition gr;
			IndexedVector opt=null;
		
		
		try {      // for error handling
			{
			_loop3:
			do {
				if ((LA(1)==HEADER_ACTION)) {
					hdr = LT(1);
					match(HEADER_ACTION);
					hier.getFile(file).addHeaderAction(hdr.getText());
				}
				else {
					break _loop3;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				opt=optionSpec(null);
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
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
			_loop6:
			do {
				if ((LA(1)==ACTION||LA(1)==LITERAL_class)) {
					gr=class_def(file, hier);
					
								if ( gr!=null && opt!=null ) {
									hier.getFile(file).setOptions(opt);
								}
								if ( gr!=null ) {
									gr.setFileName(file);
									hier.addGrammar(gr);
								}
								
				}
				else {
					break _loop6;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_0);
		}
	}
	
	public final IndexedVector  optionSpec(
		GrammarDefinition gr
	) throws RecognitionException, TokenStreamException {
		IndexedVector options;
		
		Token  op = null;
		Token  rhs = null;
		
			options = new IndexedVector();
		
		
		try {      // for error handling
			match(OPTIONS_START);
			{
			_loop16:
			do {
				if ((LA(1)==ID)) {
					op = LT(1);
					match(ID);
					rhs = LT(1);
					match(ASSIGN_RHS);
					
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
				else {
					break _loop16;
				}
				
			} while (true);
			}
			match(RCURLY);
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_1);
		}
		return options;
	}
	
	public final GrammarDefinition  class_def(
		String file, Hierarchy hier
	) throws RecognitionException, TokenStreamException {
		GrammarDefinition gr;
		
		Token  preamble = null;
		Token  sub = null;
		Token  sup = null;
		Token  tk = null;
		Token  memberA = null;
		
			gr=null;
			IndexedVector rules = new IndexedVector(100);
			IndexedVector classOptions = null;
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case ACTION:
			{
				preamble = LT(1);
				match(ACTION);
				break;
			}
			case LITERAL_class:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LITERAL_class);
			sub = LT(1);
			match(ID);
			match(LITERAL_extends);
			sup = LT(1);
			match(ID);
			match(SEMI);
			
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
					
			{
			switch ( LA(1)) {
			case OPTIONS_START:
			{
				classOptions=optionSpec(gr);
				break;
			}
			case ACTION:
			case ID:
			case TOKENS_SPEC:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			
					if ( gr!=null ) {
						gr.setOptions(classOptions);
					}
					
			{
			switch ( LA(1)) {
			case TOKENS_SPEC:
			{
				tk = LT(1);
				match(TOKENS_SPEC);
				gr.setTokenSection(tk.getText());
				break;
			}
			case ACTION:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
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
			case ACTION:
			{
				memberA = LT(1);
				match(ACTION);
				gr.setMemberAction(memberA.getText());
				break;
			}
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
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
			int _cnt13=0;
			_loop13:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					rule(gr);
				}
				else {
					if ( _cnt13>=1 ) { break _loop13; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt13++;
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_3);
		}
		return gr;
	}
	
	public final void rule(
		GrammarDefinition gr
	) throws RecognitionException, TokenStreamException {
		
		Token  r = null;
		Token  arg = null;
		Token  ret = null;
		Token  init = null;
		Token  blk = null;
		
			IndexedVector o = null;	// options for rule
			String vis = null;
			String eg=null, thr="";
		
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_protected:
			{
				match(LITERAL_protected);
				vis="protected";
				break;
			}
			case LITERAL_private:
			{
				match(LITERAL_private);
				vis="private";
				break;
			}
			case LITERAL_public:
			{
				match(LITERAL_public);
				vis="public";
				break;
			}
			case ID:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			r = LT(1);
			match(ID);
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				arg = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case LITERAL_returns:
			case RULE_BLOCK:
			case LITERAL_throws:
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
			case LITERAL_returns:
			{
				match(LITERAL_returns);
				ret = LT(1);
				match(ARG_ACTION);
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case RULE_BLOCK:
			case LITERAL_throws:
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
			case LITERAL_throws:
			{
				thr=throwsSpec();
				break;
			}
			case ACTION:
			case OPTIONS_START:
			case RULE_BLOCK:
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
			case OPTIONS_START:
			{
				o=optionSpec(null);
				break;
			}
			case ACTION:
			case RULE_BLOCK:
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
			case ACTION:
			{
				init = LT(1);
				match(ACTION);
				break;
			}
			case RULE_BLOCK:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			blk = LT(1);
			match(RULE_BLOCK);
			eg=exceptionGroup();
			
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
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
	}
	
	public final String  throwsSpec() throws RecognitionException, TokenStreamException {
		String t;
		
		Token  a = null;
		Token  b = null;
		t="throws ";
		
		try {      // for error handling
			match(LITERAL_throws);
			a = LT(1);
			match(ID);
			t+=a.getText();
			{
			_loop26:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					b = LT(1);
					match(ID);
					t+=","+b.getText();
				}
				else {
					break _loop26;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_5);
		}
		return t;
	}
	
	public final String  exceptionGroup() throws RecognitionException, TokenStreamException {
		String g;
		
		String e=null; g="";
		
		try {      // for error handling
			{
			_loop29:
			do {
				if ((LA(1)==LITERAL_exception)) {
					e=exceptionSpec();
					g += e;
				}
				else {
					break _loop29;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_4);
		}
		return g;
	}
	
	public final String  exceptionSpec() throws RecognitionException, TokenStreamException {
		String es;
		
		Token  aa = null;
		String h=null;
		es = System.getProperty("line.separator")+"exception ";
		
		
		try {      // for error handling
			match(LITERAL_exception);
			{
			switch ( LA(1)) {
			case ARG_ACTION:
			{
				aa = LT(1);
				match(ARG_ACTION);
				es += aa.getText();
				break;
			}
			case EOF:
			case ACTION:
			case LITERAL_class:
			case ID:
			case LITERAL_protected:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_exception:
			case LITERAL_catch:
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
			_loop33:
			do {
				if ((LA(1)==LITERAL_catch)) {
					h=exceptionHandler();
					es += h;
				}
				else {
					break _loop33;
				}
				
			} while (true);
			}
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_6);
		}
		return es;
	}
	
	public final String  exceptionHandler() throws RecognitionException, TokenStreamException {
		String h;
		
		Token  a1 = null;
		Token  a2 = null;
		h=null;
		
		try {      // for error handling
			match(LITERAL_catch);
			a1 = LT(1);
			match(ARG_ACTION);
			a2 = LT(1);
			match(ACTION);
			h = System.getProperty("line.separator")+
						 "catch "+a1.getText()+" "+a2.getText();
		}
		catch (RecognitionException ex) {
			reportError(ex);
			consume();
			consumeUntil(_tokenSet_7);
		}
		return h;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"tokens\"",
		"HEADER_ACTION",
		"ACTION",
		"\"class\"",
		"ID",
		"\"extends\"",
		"SEMI",
		"TOKENS_SPEC",
		"OPTIONS_START",
		"ASSIGN_RHS",
		"RCURLY",
		"\"protected\"",
		"\"private\"",
		"\"public\"",
		"ARG_ACTION",
		"\"returns\"",
		"RULE_BLOCK",
		"\"throws\"",
		"COMMA",
		"\"exception\"",
		"\"catch\"",
		"SUBRULE_BLOCK",
		"ALT",
		"ELEMENT",
		"ID_OR_KEYWORD",
		"CURLY_BLOCK_SCARF",
		"WS",
		"NEWLINE",
		"COMMENT",
		"SL_COMMENT",
		"ML_COMMENT",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"ESC",
		"DIGIT",
		"XDIGIT"
	};
	
	private static final long[] _tokenSet_0_data_ = { 2L, 0L };
	public static final BitSet _tokenSet_0 = new BitSet(_tokenSet_0_data_);
	private static final long[] _tokenSet_1_data_ = { 1280450L, 0L };
	public static final BitSet _tokenSet_1 = new BitSet(_tokenSet_1_data_);
	private static final long[] _tokenSet_2_data_ = { 229632L, 0L };
	public static final BitSet _tokenSet_2 = new BitSet(_tokenSet_2_data_);
	private static final long[] _tokenSet_3_data_ = { 194L, 0L };
	public static final BitSet _tokenSet_3 = new BitSet(_tokenSet_3_data_);
	private static final long[] _tokenSet_4_data_ = { 229826L, 0L };
	public static final BitSet _tokenSet_4 = new BitSet(_tokenSet_4_data_);
	private static final long[] _tokenSet_5_data_ = { 1052736L, 0L };
	public static final BitSet _tokenSet_5 = new BitSet(_tokenSet_5_data_);
	private static final long[] _tokenSet_6_data_ = { 8618434L, 0L };
	public static final BitSet _tokenSet_6 = new BitSet(_tokenSet_6_data_);
	private static final long[] _tokenSet_7_data_ = { 25395650L, 0L };
	public static final BitSet _tokenSet_7 = new BitSet(_tokenSet_7_data_);
	
	}
