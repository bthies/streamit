// $ANTLR 1.5A: "Optgen.g" -> "OptgenParser.java"$
 package at.dms.compiler.tools.optgen; 
import at.dms.compiler.tools.antlr.runtime.*;

  import java.util.Vector;

import at.dms.compiler.tools.common.Utils;

public class OptgenParser extends at.dms.compiler.tools.antlr.runtime.LLkParser
       implements OptgenTokenTypes
 {

 private int optshort=-1;

protected OptgenParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public OptgenParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected OptgenParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public OptgenParser(TokenStream lexer) {
  this(lexer,2);
}

public OptgenParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final DefinitionFile  aCompilationUnit(
		String sourceFile
	) throws RecognitionException, TokenStreamException {
		DefinitionFile self;
		
		
		String		fileHeader = null;
		String		prefix = null;
		String		parent = null;
		String		packageName = null;
		String		version = null;
		String		usage = null;
		String		help = null;
		OptionDefinition[]	definitions = null;
		
		
		{
		switch ( LA(1)) {
		case HEADER:
		{
			fileHeader=aFileHeader();
			break;
		}
		case LITERAL_prefix:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(LITERAL_prefix);
		prefix=aIdentifier();
		{
		switch ( LA(1)) {
		case LITERAL_parent:
		{
			match(LITERAL_parent);
			parent=aName();
			break;
		}
		case EOF:
		case LITERAL_version:
		case LITERAL_usage:
		case LITERAL_help:
		case LITERAL_package:
		case LITERAL_longname:
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
		case LITERAL_version:
		{
			match(LITERAL_version);
			version=aString();
			break;
		}
		case EOF:
		case LITERAL_usage:
		case LITERAL_help:
		case LITERAL_package:
		case LITERAL_longname:
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
		case LITERAL_usage:
		{
			match(LITERAL_usage);
			usage=aString();
			break;
		}
		case EOF:
		case LITERAL_help:
		case LITERAL_package:
		case LITERAL_longname:
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
		case LITERAL_help:
		{
			match(LITERAL_help);
			help=aString();
			break;
		}
		case EOF:
		case LITERAL_package:
		case LITERAL_longname:
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
		case LITERAL_longname:
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
						parent,
						prefix,
			                        version,
						usage,
						help,
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
		
		
		String	ident;
		
		
		ident=aIdentifier();
		self = ident;
		{
		_loop15:
		do {
			if ((LA(1)==DOT)) {
				match(DOT);
				ident=aIdentifier();
				self += "." + ident;
			}
			else {
				break _loop15;
			}
			
		} while (true);
		}
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
	
	private final OptionDefinition[]  aDefinitions(
		
	) throws RecognitionException, TokenStreamException {
		OptionDefinition[] self = null;
		
		
		Vector		container = new Vector();
		OptionDefinition	def;
		
		
		{
		_loop10:
		do {
			if ((LA(1)==LITERAL_longname)) {
				def=aOptionDefinition();
				container.addElement(def);
			}
			else {
				break _loop10;
			}
			
		} while (true);
		}
		self = (OptionDefinition[])Utils.toArray(container, OptionDefinition.class);
		return self;
	}
	
	private final OptionDefinition  aOptionDefinition(
		
	) throws RecognitionException, TokenStreamException {
		OptionDefinition self;
		
		
		String longname;
		String shortname=null;
		String type;
		String defaultValue;
		String argument = null;
		String help;
		
		
		match(LITERAL_longname);
		longname=aString();
		{
		switch ( LA(1)) {
		case LITERAL_shortcut:
		{
			match(LITERAL_shortcut);
			shortname=aString();
			break;
		}
		case LITERAL_type:
		{
			shortname="\""+String.valueOf(optshort)+"\"";optshort--;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(LITERAL_type);
		type=aOptionType();
		match(LITERAL_default);
		defaultValue=aString();
		{
		switch ( LA(1)) {
		case LITERAL_optionalDefault:
		{
			match(LITERAL_optionalDefault);
			argument=aString();
			break;
		}
		case LITERAL_requireArgument:
		{
			match(LITERAL_requireArgument);
			argument = "";
			break;
		}
		case LITERAL_help:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(LITERAL_help);
		help=aString();
		self = new OptionDefinition(longname, shortname, type, defaultValue, argument, help);
		return self;
	}
	
	private final String  aOptionType(
		
	) throws RecognitionException, TokenStreamException {
		String self;
		
		
		switch ( LA(1)) {
		case LITERAL_int:
		{
			match(LITERAL_int);
			self = "int";
			break;
		}
		case LITERAL_boolean:
		{
			match(LITERAL_boolean);
			self = "boolean";
			break;
		}
		case LITERAL_String:
		{
			match(LITERAL_String);
			self = "String";
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return self;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"\"prefix\"",
		"\"parent\"",
		"\"version\"",
		"\"usage\"",
		"\"help\"",
		"\"package\"",
		"HEADER",
		"IDENT",
		"DOT",
		"STRING",
		"\"longname\"",
		"\"shortcut\"",
		"\"type\"",
		"\"default\"",
		"\"optionalDefault\"",
		"\"requireArgument\"",
		"\"int\"",
		"\"boolean\"",
		"\"String\""
	};
	
	
	}
