/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

/*
 * PrintAST.java: print the abstract syntax tree for a StreamIt program
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: PrintAST.java,v 1.2 2003-10-09 19:50:53 dmaze Exp $
 */

package streamit.frontend;
import java.io.FileReader;

class PrintAST
{
    public static void main(String[] args)
    {
        try
        {
            StreamItLex lexer = new StreamItLex(new FileReader(args[0]));
            StreamItParser parser = new StreamItParser(lexer);
            parser.program();
            System.out.println(parser.getAST().toStringTree());
        }
        catch(Exception e)
        {
            System.err.println(e);
        }
    }
}
