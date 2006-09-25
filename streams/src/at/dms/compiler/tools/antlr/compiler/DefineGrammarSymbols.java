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
 * $Id: DefineGrammarSymbols.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.util.Hashtable;

import at.dms.compiler.tools.antlr.runtime.BitSet;
import at.dms.compiler.tools.antlr.runtime.CommonToken;
import at.dms.compiler.tools.antlr.runtime.SemanticException;
import at.dms.compiler.tools.antlr.runtime.Token;

/**
 * DefineGrammarSymbols is a behavior for the ANTLRParser that adds all
 * the token and rule symbols to the grammar symbol table.
 *
 * Token types are assigned to token symbols in this class also.
 * The token type for a token is done in the order seen (lexically).
 */
public class DefineGrammarSymbols implements ANTLRGrammarParseBehavior {

    // Contains all of the defined parser and lexer Grammar's indexed by name
    protected Hashtable<String, Grammar> grammars = new Hashtable<String, Grammar>();
    // Contains all the TokenManagers indexed by name
    protected Hashtable<String, TokenManager> tokenManagers = new Hashtable<String, TokenManager>();
    // Current grammar (parser or lexer)
    protected Grammar grammar;
    // The tool under which this is invoked
    protected Main tool;
    // The grammar analyzer object
    LLkAnalyzer analyzer;
    // The command-line arguments passed to the tool.
    // This allows each grammar to parse the arguments as it is created
    String[] args;
    // Name for default token manager does not match any valid name
    static final String DEFAULT_TOKENMANAGER_NAME = "*default";
    // Header actions apply to all parsers unless redefined
    // Contains all of the header actions indexed by name
    protected Hashtable<String,Token> headerActions = new Hashtable<String,Token>();
    // Place where preamble is stored until a grammar is defined
    Token thePreambleAction = new CommonToken(Token.INVALID_TYPE, ""); // init to empty token

    protected int numLexers = 0;
    protected int numParsers = 0;

    public DefineGrammarSymbols(Main tool_, LLkAnalyzer analyzer_) {
        tool = tool_;
        analyzer = analyzer_;
    }
    public void _refStringLiteral(Token lit, Token label, boolean lastInRule) {
        if (!(grammar instanceof LexerGrammar)) {
            // String literals are treated like tokens except by the lexer
            String str = lit.getText();
            if ( grammar.tokenManager.getTokenSymbol(str) != null ) {
                // string symbol is already defined
                return;
            }
            StringLiteralSymbol sl = new StringLiteralSymbol(str);
            int tt = grammar.tokenManager.nextTokenType();
            sl.setTokenType(tt);
            grammar.tokenManager.define(sl);
        }
    }
    /**
     * Reference a token
     */
    public void _refToken(Token assignId,
                          Token t,
                          Token label,
                          Token args,
                          boolean inverted,
                          boolean lastInRule) {
        String id = t.getText();
        if (!grammar.tokenManager.tokenDefined(id)) {
            int tt = grammar.tokenManager.nextTokenType();
            TokenSymbol ts = new TokenSymbol(id);
            ts.setTokenType(tt);
            grammar.tokenManager.define(ts);
        }
    }

    /**
     * Abort the processing of a grammar due to syntax errors
     */
    public void abortGrammar() {
        if (grammar != null && grammar.getClassName() != null) {
            grammars.remove(grammar.getClassName());
        }
        grammar = null;
    }

    public void beginAlt(boolean doAST_) {
    }

    public void beginChildList() {
    }

    // Exception handling
    public void beginExceptionGroup() {
    }
    public void beginExceptionSpec(Token label) {
    }
    public void beginSubRule(Token label, int line, boolean not) {
    }
    /**
     * Define a lexer or parser rule
     */
    public void defineRuleName(Token r, String access, String docComment)
        throws SemanticException
    {
        String id = r.getText();

        //      if ( Character.isUpperCase(id.charAt(0)) ) {
        if ( r.getType() == ANTLRTokenTypes.TOKEN_REF ) {
            // lexer rule
            id = JavaCodeGenerator.lexerRuleName(id);
            // make sure we define it as token identifier also
            if ( !grammar.tokenManager.tokenDefined(r.getText()) ) {
                int tt = grammar.tokenManager.nextTokenType();
                TokenSymbol ts = new TokenSymbol(r.getText());
                ts.setTokenType(tt);
                grammar.tokenManager.define(ts);
            }
        }

        RuleSymbol rs;
        if ( grammar.isDefined(id) ) {
            // symbol seen before?
            rs = (RuleSymbol) grammar.getSymbol(id);
            // rule just referenced or has it been defined yet?
            if ( rs.isDefined() ) {
                tool.error("redefinition of rule "+id, grammar.getFilename(), r.getLine());
            }
        } else {
            rs = new RuleSymbol(id);
            grammar.define(rs);
        }

        rs.setDefined();
        rs.access = access == null ? grammar.getDefaultAccess() : access;
        rs.comment = docComment;
    }

    /**
     * Define a token from tokens {...}.
     *  Must be label and literal or just label or just a literal.
     */
    public void defineToken(Token tokname, Token tokliteral) {
        String name = null;
        String literal = null;
        if (tokname != null) {
            name = tokname.getText();
        }
        if (tokliteral != null) {
            literal = tokliteral.getText();
        }
        // System.out.println("defining " + name + " with literal " + literal);
        //
        if (literal != null) {
            StringLiteralSymbol sl=(StringLiteralSymbol)grammar.tokenManager.getTokenSymbol(literal);
            if ( sl != null) {
                // This literal is known already.
                // If the literal has no label already, but we can provide
                // one here, then no problem, just map the label to the literal
                // and don't change anything else.
                // Otherwise, labels conflict: error.
                if ( name==null || sl.getLabel()!=null ) {
                    Utils.warning("Redefinition of literal in tokens {...}: " + literal, grammar.getFilename(), tokliteral.getLine());
                    return;
                } else if ( name!=null ) {
                    // The literal had no label, but new def does.  Set it.
                    sl.setLabel(name);
                    // Also, map the label to the literal.
                    grammar.tokenManager.mapToTokenSymbol(name, sl);
                }
            }
            // if they provide a name/label and that name/label already
            // exists, just hook this literal onto old token.
            if (name != null) {
                TokenSymbol ts = (TokenSymbol) grammar.tokenManager.getTokenSymbol(name);
                if (ts != null) {
                    // watch out that the label is not more than just a token.
                    // If it already has a literal attached, then: conflict.
                    if ( ts instanceof StringLiteralSymbol ) {
                        Utils.warning("Redefinition of token in tokens {...}: " + name, grammar.getFilename(), tokliteral.getLine());
                        return;
                    }
                    // a simple token symbol such as DECL is defined
                    // must convert it to a StringLiteralSymbol with a
                    // label by co-opting token type and killing old
                    // TokenSymbol.  Kill mapping and entry in vector
                    // of token manager.
                    // First, claim token type.
                    int ttype = ts.getTokenType();
                    // now, create string literal with label
                    sl = new StringLiteralSymbol(literal);
                    sl.setTokenType(ttype);
                    sl.setLabel(name);
                    // redefine this critter as a string literal
                    grammar.tokenManager.define(sl);
                    // make sure the label can be used also.
                    grammar.tokenManager.mapToTokenSymbol(name, sl);
                    return;
                }
                // here, literal was labeled but not by a known token symbol.
            }
            sl = new StringLiteralSymbol(literal);
            int tt = grammar.tokenManager.nextTokenType();
            sl.setTokenType(tt);
            sl.setLabel(name);
            grammar.tokenManager.define(sl);
            if (name != null) {
                // make the label point at token symbol too
                grammar.tokenManager.mapToTokenSymbol(name, sl);
            }
        }

        // create a token in the token manager not a literal
        else {
            if (grammar.tokenManager.tokenDefined(name)) {
                Utils.warning("Redefinition of token in tokens {...}: " + name, grammar.getFilename(), tokname.getLine());
                return;
            }
            int tt = grammar.tokenManager.nextTokenType();
            TokenSymbol ts = new TokenSymbol(name);
            ts.setTokenType(tt);
            grammar.tokenManager.define(ts);
        }
    }
    public void endAlt() {
    }
    public void endChildList() {
    }
    public void endExceptionGroup() {
    }
    public void endExceptionSpec() {
    }
    public void endGrammar() {
    }
    /**
     * Called after the optional options section, to compensate for
     * options that may not have been set.
     * This method is bigger than it needs to be, but is much more
     * clear if I delineate all the cases.
     */
    public void endOptions() {
        // NO VOCAB OPTIONS
        if ( grammar.exportVocab==null && grammar.importVocab==null ) {
            grammar.exportVocab = grammar.getClassName();
            // Can we get initial vocab from default shared vocab?
            if (tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                // Use the already-defined token manager
                grammar.exportVocab = DEFAULT_TOKENMANAGER_NAME;
                TokenManager tm = tokenManagers.get(DEFAULT_TOKENMANAGER_NAME);
                // System.out.println("No tokenVocabulary for '" + grammar.getClassName() + "', using default '" + tm.getName() + "'");
                grammar.setTokenManager(tm);
                return;
            }
            // no shared vocab for file, make new one
            // System.out.println("No exportVocab for '" + grammar.getClassName() + "', creating default '" + grammar.exportVocab + "'");
            TokenManager tm = new SimpleTokenManager(grammar.exportVocab, tool);
            grammar.setTokenManager(tm);
            // Add the token manager to the list of token managers
            tokenManagers.put(grammar.exportVocab, tm);
            // no default vocab, so make this the default vocab
            tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            return;
        }

        // NO OUTPUT, BUT HAS INPUT VOCAB
        if ( grammar.exportVocab==null && grammar.importVocab!=null ) {
            grammar.exportVocab = grammar.getClassName();
            // first make sure input!=output
            if ( grammar.importVocab.equals(grammar.exportVocab) ) {
                Utils.warning("Grammar " + grammar.getClassName() +
                              " cannot have importVocab same as default output vocab (grammar name); ignored.");
                // kill importVocab option and try again: use default vocab
                grammar.importVocab = null;
                endOptions();
                return;
            }
            // check to see if the vocab is already in memory
            // (defined by another grammar in the file).  Not normal situation.
            if (tokenManagers.containsKey(grammar.importVocab)) {
                // make a copy since we'll be generating a new output vocab
                // and we don't want to affect this one.  Set the name to
                // the default output vocab==classname.
                TokenManager tm = tokenManagers.get(grammar.importVocab);
                // System.out.println("Duping importVocab of " + grammar.importVocab);
                TokenManager dup = (TokenManager)tm.clone();
                dup.setName(grammar.exportVocab);
                // System.out.println("Setting name to " + grammar.exportVocab);
                dup.setReadOnly(false);
                grammar.setTokenManager(dup);
                tokenManagers.put(grammar.exportVocab, dup);
                return;
            }
            // System.out.println("reading in vocab "+grammar.importVocab);
            // Must be a file, go get it.
            ImportVocabTokenManager tm =
                new ImportVocabTokenManager(grammar,
                                            grammar.importVocab + JavaCodeGenerator.TokenTypesFileSuffix + JavaCodeGenerator.TokenTypesFileExt,
                                            grammar.exportVocab,
                                            tool);
            tm.setReadOnly(false); // since renamed, can write out
            // Add this token manager to the list so its tokens will be generated
            tokenManagers.put(grammar.exportVocab, tm);
            // System.out.println("vocab renamed to default output vocab of "+tm.getName());
            // Assign the token manager to this grammar.
            grammar.setTokenManager(tm);

            // set default vocab if none
            if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            }

            return;
        }

        // OUTPUT VOCAB, BUT NO INPUT VOCAB
        if ( grammar.exportVocab!=null && grammar.importVocab==null ) {
            // share with previous vocab if it exists
            if (tokenManagers.containsKey(grammar.exportVocab)) {
                // Use the already-defined token manager
                TokenManager tm = tokenManagers.get(grammar.exportVocab);
                // System.out.println("Sharing exportVocab of " + grammar.exportVocab);
                grammar.setTokenManager(tm);
                return;
            }
            // create new output vocab
            // System.out.println("Creating exportVocab " + grammar.exportVocab);
            TokenManager tm = new SimpleTokenManager(grammar.exportVocab, tool);
            grammar.setTokenManager(tm);
            // Add the token manager to the list of token managers
            tokenManagers.put(grammar.exportVocab, tm);
            // set default vocab if none
            if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
            }
            return;
        }

        // BOTH INPUT AND OUTPUT VOCAB
        if ( grammar.exportVocab!=null && grammar.importVocab!=null ) {
            // don't want input==output
            if (grammar.importVocab.equals(grammar.exportVocab)) {
                // does the input vocab already exist in memory?
                if (tokenManagers.containsKey(grammar.importVocab)) {
                    grammar.setTokenManager(tokenManagers.get(grammar.importVocab));
                } else {
                    // Must be a file, go get it.
                    ImportVocabTokenManager tm =
                        new ImportVocabTokenManager(grammar,
                                                    grammar.importVocab + JavaCodeGenerator.TokenTypesFileSuffix + JavaCodeGenerator.TokenTypesFileExt,
                                                    grammar.exportVocab,
                                                    tool);
                    tm.setReadOnly(true); // write it out as we've changed name
                    // Add this token manager to the list so its tokens will be generated
                    tokenManagers.put(grammar.exportVocab, tm);
                    // Assign the token manager to this grammar.
                    grammar.setTokenManager(tm);

                    // set default vocab if none
                    if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                        tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
                    }
                }
            } else {
                // does the input vocab already exist in memory?
                if (tokenManagers.containsKey(grammar.importVocab)) {
                    // make a copy since we'll be generating a new output vocab
                    // and we don't want to affect this one.
                    TokenManager tm = tokenManagers.get(grammar.importVocab);
                    // System.out.println("Duping importVocab of " + grammar.importVocab);
                    TokenManager dup = (TokenManager)tm.clone();
                    dup.setName(grammar.exportVocab);
                    // System.out.println("Setting name to " + grammar.exportVocab);
                    dup.setReadOnly(false);
                    grammar.setTokenManager(dup);
                    tokenManagers.put(grammar.exportVocab, dup);
                    return;
                }
                // Must be a file, go get it.
                ImportVocabTokenManager tm =
                    new ImportVocabTokenManager(grammar,
                                                grammar.importVocab + JavaCodeGenerator.TokenTypesFileSuffix + JavaCodeGenerator.TokenTypesFileExt,
                                                grammar.exportVocab,
                                                tool);
                tm.setReadOnly(false); // write it out as we've changed name
                // Add this token manager to the list so its tokens will be generated
                tokenManagers.put(grammar.exportVocab, tm);
                // Assign the token manager to this grammar.
                grammar.setTokenManager(tm);

                // set default vocab if none
                if (!tokenManagers.containsKey(DEFAULT_TOKENMANAGER_NAME)) {
                    tokenManagers.put(DEFAULT_TOKENMANAGER_NAME, tm);
                }

                return;
            }
        }
    }
    public void endRule(String r) {
    }
    public void endSubRule() {
    }
    public void hasError() {
    }
    public void oneOrMoreSubRule() {
    }
    public void optionalSubRule() {
    }
    public void setUserExceptions(String thr) {}
    public void refAction(Token action) {
    }
    public void refArgAction(Token action) {
    }
    public void refCharLiteral(Token lit, Token label, boolean inverted, boolean lastInRule) {
    }
    public void refCharRange(Token t1, Token t2, Token label, boolean lastInRule) {
    }

    public void refElementOption(Token option, Token value) {
    }

    public void refTokensSpecElementOption(Token tok, Token option, Token value) {
    }

    public void refExceptionHandler(Token exTypeAndName, Token action) {
    }

    // Header action applies to all parsers and lexers.
    public void refHeaderAction(Token name,Token act) {
        headerActions.put((name==null) ? "" : Utils.stripFrontBack(name.getText(),"\"","\""), act);
    }

    public String getHeaderAction(String name) {
        Token t = headerActions.get(name);

        return (t == null) ? "" : t.getText();
    }

    public void refInitAction(Token action) {
    }
    public void refMemberAction(Token act) {
    }
    public void refPreambleAction(Token act) {
        thePreambleAction = act;
    }
    public void refReturnAction(Token returnAction) {
    }

    public void refRule(Token idAssign, Token r, Token label, Token args) {
        String id = r.getText();

        //      if ( Character.isUpperCase(id.charAt(0)) ) { // lexer rule?
        if ( r.getType() == ANTLRTokenTypes.TOKEN_REF ) {
            // lexer rule?
            id = JavaCodeGenerator.lexerRuleName(id);
        }
        if ( !grammar.isDefined(id) ) {
            grammar.define(new RuleSymbol(id));
        }
    }

    public void refSemPred(Token pred) {
    }

    public void refStringLiteral(Token lit,
                                 Token label,
                                 boolean lastInRule) {
        _refStringLiteral(lit, label, lastInRule);
    }

    /**
     * Reference a token
     */
    public void refToken(Token assignId, Token t, Token label, Token args,
                         boolean inverted, boolean lastInRule) {
        _refToken(assignId, t, label, args, inverted, lastInRule);
    }

    public void refTokenRange(Token t1, Token t2, Token label, boolean lastInRule) {
        // ensure that the DefineGrammarSymbols methods are called; otherwise a range addes more
        // token refs to the alternative by calling MakeGrammar.refToken etc...
        if ( t1.getText().charAt(0) == '"' ) {
            refStringLiteral(t1, null, lastInRule);
        } else {
            _refToken(null, t1, null, null, false, lastInRule);
        }
        if ( t2.getText().charAt(0) == '"' ) {
            _refStringLiteral(t2, null, lastInRule);
        } else {
            _refToken(null, t2, null, null, false, lastInRule);
        }
    }

    public void refWildcard(Token t, Token label) {
    }

    /**
     * Get ready to process a new grammar
     */
    public void reset() {
        grammar = null;
    }

    public void setArgOfRuleRef(Token argaction) {
    }

    /**
     * Set the character vocabulary for a lexer
     */
    public void setCharVocabulary(BitSet b) {
        // grammar should enforce that this is only called for lexer
        ((LexerGrammar)grammar).setCharVocabulary(b);
    }

    /**
     * setFileOption: Associate an option value with a key.
     * This applies to options for an entire grammar file.
     * @param key The token containing the option name
     * @param value The token containing the option value.
     */
    public void setFileOption(Token key, Token value, String filename) {
        if (key.getText().equals("mangleLiteralPrefix")) {
            if (value.getType() == ANTLRParser.STRING_LITERAL) {
                tool.literalsPrefix = Utils.stripFrontBack(value.getText(), "\"","\"");
            } else {
                tool.error("mangleLiteralPrefix option must be string", filename, value.getLine());
            }
        } else if (key.getText().equals("upperCaseMangledLiterals")) {
            if (value.getText().equals("true")) {
                tool.upperCaseMangledLiterals = true;
            } else if (value.getText().equals("false")) {
                tool.upperCaseMangledLiterals = false;
            } else {
                grammar.tool.error("Value for upperCaseMangledLiterals must be true or false", filename, key.getLine());
            }
        } else {
            tool.error("Invalid file-level option: " + key.getText(), filename, key.getLine());
        }
    }

    /**
     * setGrammarOption: Associate an option value with a key.
     * This function forwards to Grammar.setOption for some options.
     * @param key The token containing the option name
     * @param value The token containing the option value.
     */
    public void setGrammarOption(Token key, Token value) {
        if (key.getText().equals("exportVocab")) {
            // Set the token manager associated with the parser
            if (value.getType() == ANTLRParser.RULE_REF || value.getType() == ANTLRParser.TOKEN_REF) {
                grammar.exportVocab = value.getText();
            } else {
                tool.error("exportVocab must be an identifier", grammar.getFilename(), value.getLine());
            }
        } else if (key.getText().equals("importVocab")) {
            if (value.getType() == ANTLRParser.RULE_REF || value.getType() == ANTLRParser.TOKEN_REF) {
                grammar.importVocab = value.getText();
            } else {
                tool.error("importVocab must be an identifier", grammar.getFilename(), value.getLine());
            }
        } else {
            // Forward all unrecognized options to the grammar
            grammar.setOption(key.getText(), value);
        }
    }

    public void setRuleOption(Token key, Token value) {
    }
    public void setSubruleOption(Token key, Token value) {
    }
    /**
     * Start a new lexer
     */
    public void startLexer(String file, Token name, String doc) {
        if ( numLexers>0 ) {
            Utils.panic("You may only have one lexer per grammar file: class "+ name.getText());
        }
        numLexers++;
        reset();
        //System.out.println("Processing lexer '" + name.getText() + "'");
        // Does the lexer already exist?
        Grammar g = grammars.get(name);
        if (g != null) {
            if (!(g instanceof LexerGrammar)) {
                Utils.panic("'" + name.getText() + "' is already defined as a non-lexer");
            } else {
                Utils.panic("Lexer '" + name.getText() + "' is already defined");
            }
        } else {
            // Create a new lexer grammar
            LexerGrammar lg = new LexerGrammar(name.getText(), tool);
            lg.comment = doc;
            lg.setFilename(file);
            grammars.put(lg.getClassName(), lg);
            // Use any preamble action
            lg.preambleAction = thePreambleAction;
            thePreambleAction = new CommonToken(Token.INVALID_TYPE, "");
            // This is now the current grammar
            grammar = lg;
        }
    }
    /**
     * Start a new parser
     */
    public void startParser(String file, Token name, String doc) {
        if ( numParsers>0 ) {
            Utils.panic("You may only have one parser per grammar file: class "+ name.getText());
        }
        numParsers++;
        reset();
        //System.out.println("Processing parser '" + name.getText() + "'");
        // Is this grammar already defined?
        Grammar g = grammars.get(name);
        if (g != null) {
            if (!(g instanceof ParserGrammar)) {
                Utils.panic("'" + name.getText() + "' is already defined as a non-parser");
            } else {
                Utils.panic("Parser '" + name.getText() + "' is already defined");
            }
        } else {
            // Create a new grammar
            grammar = new ParserGrammar(name.getText(), tool);
            grammar.comment = doc;
            grammar.setFilename(file);
            grammars.put(grammar.getClassName(), grammar);
            // Use any preamble action
            grammar.preambleAction = thePreambleAction;
            thePreambleAction = new CommonToken(Token.INVALID_TYPE, "");
        }
    }
    public void synPred() {
    }
    public void zeroOrMoreSubRule() {
    }
}
