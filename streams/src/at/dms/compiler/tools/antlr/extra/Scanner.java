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
 * $Id: Scanner.java,v 1.4 2006-03-24 20:48:35 dimock Exp $
 */

package at.dms.compiler.tools.antlr.extra;

import java.io.IOException;
import java.util.Vector;

import at.dms.compiler.tools.common.Compiler;
import at.dms.compiler.tools.common.CompilerMessages;
import at.dms.compiler.tools.common.JavaStyleComment;
import at.dms.compiler.tools.common.JavadocComment;
import at.dms.compiler.tools.common.PositionedError;
import at.dms.compiler.tools.common.TokenReference;
import at.dms.compiler.tools.antlr.runtime.Token;
import at.dms.compiler.tools.antlr.runtime.TokenStream;
import at.dms.compiler.tools.common.MessageDescription;
import at.dms.compiler.tools.common.Utils;

/**
 * This class describes the capabilities of lexers (token streams)
 * to share a common input buffer and to handle line numbers.
 */
public abstract class Scanner implements TokenStream {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a new hand written scanner
     */
    protected Scanner(Compiler compiler, InputBuffer buffer) {
        this.compiler = compiler;
        this.buffer = buffer;
        this.comments = new Vector();
    }

    // --------------------------------------------------------------------
    // TOKEN HANDLING
    // --------------------------------------------------------------------

    /**
     * Returns the next token in the input.
     */
    public final Token nextToken() {
        while (true) {
            try {
                return nextTokenImpl();
            } catch (IOException e) {
                reportTrouble(CompilerMessages.IO_EXCEPTION, new Object[]{ buffer.getFile(), e.getMessage()});
            }
        }
    }

    /**
     * Returns the next token in the input.
     */
    public abstract Token nextTokenImpl() throws IOException;

    // --------------------------------------------------------------------
    // OPERATIONS
    // --------------------------------------------------------------------

    /**
     * Returns the reference of the current token in the source file.
     */
    public final TokenReference getTokenReference() {
        return TokenReference.build(buffer.getFile(), buffer.getLine());
    }

    /**
     * Reports that an error has been detected in the lexical analyser.
     * The handling is delegated to the compiler driver.
     * @param   trouble       the error to report
     */
    protected final void reportTrouble(PositionedError trouble) {
        compiler.reportTrouble(trouble);
    }

    /**
     * Reports that an error has been detected in the lexical analyser.
     * The handling is delegated to the compiler driver.
     * @param   mess        the error message
     * @param   params      the array of message parameters
     */
    protected final void reportTrouble(MessageDescription mess, Object[] params) {
        reportTrouble(new PositionedError(getTokenReference(), mess, params));
    }

    // --------------------------------------------------------------------
    // COMMENT HANDLING
    // --------------------------------------------------------------------

    /*
     * Adds a comment
     */
    public final void addComment(JavaStyleComment comment) {
        comments.addElement(comment);
    }

    /**
     *
     */
    public JavaStyleComment[] getStatementComment() {
        JavaStyleComment[]  comm = (JavaStyleComment[])Utils.toArray(comments, JavaStyleComment.class);

        comments.setSize(0);

        return comm;
    }


    /**
     *
     */
    public JavadocComment getJavadocComment() {
        for (int i = comments.size() - 1; i >= 0; i--) {
            if (comments.elementAt(i) instanceof JavadocComment) {
                JavadocComment      jdoc = (JavadocComment)comments.elementAt(i);

                comments.setElementAt(null, i);
                return jdoc;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the input buffer.
     */
    public final InputBuffer getBuffer() {
        return buffer;
    }

    /**
     *
     */
    public void setFile(String file) {
        buffer.setFile(file);
    }

    /**
     * Returns the current line number in the source code.
     */
    public final int getLine() {
        return buffer.getLine();
    }

    /**
     * Sets the current line number in the source code.
     */
    public final void setLine(int line) {
        buffer.setLine(line);
    }

    /**
     * Sets the current line number in the source code.
     */
    public final void incrementLine() {
        buffer.incrementLine();
    }

    // --------------------------------------------------------------------
    // IMPLEMENTED BY ACTUAL SCANNERS
    // --------------------------------------------------------------------

    /**
     * Returns the buffer state.
     */
    public abstract InputBufferState getBufferState();

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    public static final CToken      TOKEN_EOF = new CToken(1, "End of file");

    private final Compiler      compiler;
    private final InputBuffer       buffer;
    private final Vector            comments;
}

//---  // --------------------------------------------------------------------
//---  // ACCESSORS
//---  // --------------------------------------------------------------------
//---
//---
//---  /**
//---   * Returns the current position in the input stream.
//---   */
//---  public final int getPosition() {
//---    return buffer.pos;
//---  }
//---
//---  /**
//---   * Sets the current position in the input stream.
//---   */
//---  public final void setPosition(int pos) {
//---    buffer.pos = pos;
//---  }
//---
//---  /**
//---   * Returns the reference of the current token in the source file.
//---   */
//---  public final TokenReference getTokenReference() {
//---    return TokenReference.build(buffer.getFile(), buffer.getLine());
//---  }
//---
//---  /**
//---   * Returns the next token in the input.
//---   */
//---  protected abstract Token getToken() throws PositionedError;
//---
//---  /**
//---   *  Identifiers/Keywords/true/false/null (start with java letter)
//---   *  numeric literal (start with number)
//---   *  character literal (start with single quote)
//---   *  string (start with double quote)
//---   *  separator (parens, braces, brackets, semicolon, comma, period)
//---   *  operator (equals, plus, minus, etc)
//---   *  whitespace
//---   *  comment (start with slash)
//---   */
//---  public final Token nextToken() {
//---    while (true) {
//---      if (buffer.isEndOfData()) {
//---   return EOF;
//---      } else {
//---   switch (buffer.thisChar()) {
//---
//---     // White space:
//---   case '\n':  // LineTerminator NL
//---     buffer.incrLine();
//---     buffer.skipChar();
//---     continue;
//---
//---   case '\r':  // LineTerminator CR
//---     // No need to handle CR-NL separatly here:
//---     // NL will be caught in the next loop cycle
//---   case ' ':   // ASCII SP
//---   case '\t':  // ASCII HT
//---   case '\f':  // ASCII FF
//---     buffer.skipChar();
//---     continue;
//---
//---     // EOF character:
//---   case '\020': // ASCII SUB
//---     buffer.skipChar();
//---     return EOF;
//---
//---     // else, a Token
//---   default:
//---     try {
//---       Token   tok = getToken();
//---
//---       if (tok != null) {
//---         return tok;
//---       }
//---       continue;
//---     } catch (PositionedError e) {
//---       reportTrouble(e);
//---       buffer.skipChar();
//---     } catch (Exception e) {
//---       if (! buffer.isEndOfData()) {
//---         reportTrouble(CompilerMessages.NO_VIABLE_ALT_FOR_CHAR, "" + buffer.thisChar(), null);
//---       } else {
//---         // not a very pretty way of handling unexpected EOF
//---         reportTrouble(CompilerMessages.UNEXPECTED_EOF, null, null);
//---         return EOF;
//---       }
//---       buffer.skipChar();
//---     }
//---   }
//---      }
//---    }
//---  }
//---
//---  // --------------------------------------------------------------------
//---  // SPECIAL TOKEN SCANNERS PROVIDED FOR CONVENIENCE OF SUB-CLASSES
//---  // --------------------------------------------------------------------
//---  // NOTE: scanXxx procedures do not return tokens
//---
//---  /**
//---   * Scans an escape sequence
//---   *
//---   * @return   the scanned character
//---   */
//---  protected char scanEscapeSequence() throws PositionedError {
//---    if (buffer.nextChar() != '\\') {
//---      throw new PositionedError(getTokenReference(), CompilerMessages.ILLEGAL_CHAR, "\\\\");
//---    }
//---
//---    switch (buffer.thisChar()) {
//---    case 'b':
//---      buffer.skipChar(); return '\b';
//---    case 't':
//---      buffer.skipChar(); return '\t';
//---    case 'n':
//---      buffer.skipChar(); return '\n';
//---    case 'f':
//---      buffer.skipChar(); return '\f';
//---    case 'r':
//---      buffer.skipChar(); return '\r';
//---    case '\"':
//---      buffer.skipChar(); return '\"';
//---    case '\'':
//---      buffer.skipChar(); return '\'';
//---    case '\\':
//---      buffer.skipChar(); return '\\';
//---    case 'u':
//---      buffer.skipChar();
//---      return (char)scanHexCharacter();
//---    default:
//---      return (char)scanOctCharacter();
//---    }
//---  }
//---
//---  private char scanOctCharacter() throws PositionedError {
//---    int        val = 0;
//---    int        i;
//---
//---    for (i = 0; i < 3 && (Character.digit(buffer.thisChar(), 8) != -1); i++) {
//---      val = (8*val) + Character.digit(buffer.nextChar(), 8);
//---    }
//---
//---    if (i == 0 || val > 0xFF) {
//---      throw new PositionedError(getTokenReference(), CompilerMessages.ILLEGAL_CHAR, "" + buffer.thisChar());
//---    }
//---
//---    return (char)val;
//---  }
//---
//---  private char scanHexCharacter() throws PositionedError {
//---    int        val = 0;
//---    int        i;
//---
//---    for (i = 0; i < 4 && (Character.digit(buffer.thisChar(), 16) != -1); i++) {
//---      val = (16*val) + Character.digit(buffer.nextChar(), 16);
//---    }
//---
//---    if (i == 0) {
//---      throw new PositionedError(getTokenReference(), CompilerMessages.ILLEGAL_CHAR, "" + buffer.thisChar());
//---    }
//---
//---    return (char)val;
//---  }
//---
//---  /**
//---   * Determines if the specified character is permissible as
//---   * the first character in a Java identifier.
//---   *
//---   * @return   true iff the character may start a Java identifier
//---   */
//---  protected static boolean isJavaIdentifierStart(char c) {
//---    switch (c) {
//---    case '$':  case 'A':  case 'B':  case 'C':
//---    case 'D':  case 'E':  case 'F':  case 'G':
//---    case 'H':  case 'I':  case 'J':  case 'K':
//---    case 'L':  case 'M':  case 'N':  case 'O':
//---    case 'P':  case 'Q':  case 'R':  case 'S':
//---    case 'T':  case 'U':  case 'V':  case 'W':
//---    case 'X':  case 'Y':  case 'Z':  case '_':
//---    case 'a':  case 'b':  case 'c':  case 'd':
//---    case 'e':  case 'f':  case 'g':  case 'h':
//---    case 'i':  case 'j':  case 'k':  case 'l':
//---    case 'm':  case 'n':  case 'o':  case 'p':
//---    case 'q':  case 'r':  case 's':  case 't':
//---    case 'u':  case 'v':  case 'w':  case 'x':
//---    case 'y':  case 'z':
//---      return true;
//---    default:
//---      return Character.isJavaIdentifierStart(c);
//---    }
//---  }
//---
//---  // --------------------------------------------------------------------
//---  // HANDLING OF JAVA STYLE COMMENTS
//---  // --------------------------------------------------------------------
//---
//---  /**
//---   * Reads a Java style comment (with leading // or /* or /**).
//---   * @return true iff a comment was read (the leading '/' is consumed)
//---   */
//---  protected boolean readJavaComment() {
//---    // line.charAt(line_buffer.pos+0) is '/'
//---
//---    switch (buffer.charAtOffset(1)) {
//---    case '/':
//---      readEndOfLineComment();
//---      return true;
//---    case '*':
//---      readTraditionalComment();
//---      return true;
//---    default:
//---      // it's a token, not a comment
//---      return false;
//---    }
//---  }
//---
//---  private void readEndOfLineComment() {
//---    buffer.skipChars(2);   // skip leading "//"
//---
//---    int        start = buffer.getPosition();
//---
//---    // read until end of line
//---  _loop_:
//---    while (true) {
//---      if (buffer.isEndOfData()) {
//---   //!!! add a warning for
//---   break _loop_;
//---      }
//---
//---      switch (buffer.thisChar()) {
//---      case '\n':
//---      case '\r':
//---   break _loop_;
//---      default:
//---   buffer.skipChar();
//---      }
//---    }
//---
//---    if (compiler.parseComments()) {
//---      addComment(new JavaStyleComment(new String(buffer.data, start, buffer.getPosition() - start),
//---                     true,
//---                     spaceBefore(start),
//---                     spaceAfter(buffer.getPosition())));
//---    }
//---  }
//---
//---  private void readTraditionalComment() {
//---    buffer.skipChars(2);   // skip leading "/*"
//---
//---    int        start = buffer.getPosition();
//---
//---    // read until "*/" found
//---  _loop_:
//---    while (true) {
//---      //!!! graf 000212: check for EOF (needs to declare a possible exception)
//---      //!!! graf 000212: '\r' and '\r\n' are not handled properly
//---      //!!! graf 000212: add warning for "/*" inside comment
//---      switch (buffer.thisChar()) {
//---      case '*':
//---   buffer.skipChar();
//---   while (buffer.thisChar() == '*') {
//---     buffer.skipChar();
//---   }
//---   if (buffer.thisChar() == '/') {
//---     buffer.skipChar();
//---     break _loop_;
//---   }
//---   break;
//---      case '\n':
//---   buffer.incrLine();
//---   buffer.skipChar();
//---   break;
//---      default:
//---   buffer.skipChar();
//---      }
//---    }
//---
//---    if (compiler.parseComments()) {
//---      if (buffer.charAtPosition(start) == '*') {
//---   // documentation comment
//---   addComment(new JavadocComment(new String(buffer.data, start + 1, buffer.getPosition() - start - 3),
//---                     spaceBefore(start),
//---                     spaceAfter(buffer.getPosition())));
//---      } else {
//---   // traditional comment
//---   addComment(new JavaStyleComment(new String(buffer.data, start, buffer.getPosition() - start - 2),
//---                   false,
//---                   spaceBefore(start),
//---                   spaceAfter(buffer.getPosition())));
//---      }
//---    }
//---  }
//---
//---
//---  // --------------------------------------------------------------------
//---  // TOKEN CACHE
//---  // --------------------------------------------------------------------
//---
//---  protected CToken lookupToken(int type, char[] data, int start, int length) {
//---    return cache.lookupToken(type, data, start, length);
//---  }
//---
//---  // --------------------------------------------------------------------
//---  // PROTECTED UTILITIES
//---  // --------------------------------------------------------------------
//---
//---  private int previousReturn(int i) {
//---    for (; i >= 0; i--) {
//---      if (buffer.charAtPosition(i) == '\n') {
//---   return i;
//---      }
//---    }
//---    return 0;
//---  }
//---
//---  private int previousNonBlank(int i) {
//---    for (; i >= 0; i--) {
//---      if (buffer.charAtPosition(i) != ' ' && buffer.charAtPosition(i) != '\t') {
//---   return i;
//---      }
//---    }
//---    return 0;
//---  }
//---
//---  private int nextReturn(int i) {
//---    for (; i < buffer.data.length; i++) {
//---      if (buffer.charAtPosition(i) == '\n' || buffer.charAtPosition(i) == 0) {
//---   return i;
//---      }
//---    }
//---
//---    return buffer.data.length;
//---  }
//---
//---  private int nextNonBlank(int i) {
//---    for (; i < buffer.data.length; i++) {
//---      if ((buffer.charAtPosition(i) != ' ' && buffer.charAtPosition(i) != '\t') || buffer.charAtPosition(i) == 0) {
//---   return i;
//---      }
//---    }
//---
//---    return buffer.data.length;
//---  }
//---
//---  private boolean spaceBefore(int pos) {
//---    int        previousReturn = previousReturn(pos);
//---
//---    return previousReturn == 0 ?
//---      false :
//---      previousNonBlank(previousReturn - 1) <= previousReturn(previousReturn - 1);
//---  }
//---
//---  private boolean spaceAfter(int pos) {
//---    int        nextReturn = nextReturn(pos);
//---
//---    return nextNonBlank(nextReturn + 1) >= nextReturn(nextReturn + 1);
//---  }
//---
//---
