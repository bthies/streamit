/*
 * PrintAST.java: print the abstract syntax tree for a StreamIt program
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: PrintAST.java,v 1.1 2002-06-12 17:57:29 dmaze Exp $
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
