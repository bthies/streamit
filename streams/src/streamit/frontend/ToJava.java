package streamit.frontend;
import java.io.DataInputStream;
import antlr.BaseAST;

class ToJava
{
    public int indent = 0;
    public String getIndent ()
    {
        int x = indent;
        String r = "";
        while (x-- > 0) r = r + "  ";
        return r;
    }

    public String toStringTree(BaseAST t) {
        String ts=getIndent ();
        ts += t.toString() + "\n";
        if ( t.getFirstChild()!=null ) indent ++;
        if ( t.getFirstChild()!=null ) {
            ts += (toStringTree ((BaseAST)t.getFirstChild()));
        }
        if ( t.getFirstChild()!=null ) indent--;
        if ( t.getNextSibling()!=null ) {
            ts += (toStringTree ((BaseAST)t.getNextSibling()));
        }
        return ts;
    }

    /** Print out a child-sibling tree in LISP notation */
    public String toStringList(BaseAST t) {
        String ts="";
        if ( t.getFirstChild()!=null ) ts+=" <#";
        ts += " "+t.toString();
        if ( t.getFirstChild()!=null ) {
            ts += (toStringList ((BaseAST)t.getFirstChild()));
        }
        if ( t.getFirstChild()!=null ) ts+=" #>";
        if ( t.getNextSibling()!=null ) {
            ts += (toStringList ((BaseAST)t.getNextSibling()));
        }
        return ts;
    }

    public void printUsage()
    {
        System.err.println(
"streamit.frontend.ToJava: StreamIt syntax translator\n" +
"Usage: java streamit.frontend.ToJava < in.str > out.java\n" +
"\n" +
"Options:\n" +
"  --adhoc        Use partial parsing and ad-hoc translation (default)\n" +
"  --full         Use full parsing (experimental)\n" +
"  --help         Print this message\n" +
"  --output file  Write output to file, not stdout\n" +
"\n");
    }

    public void run(String [] args)
    {
        StreamItParser parser = null;
        try
        {
            StreamItLex lexer = new StreamItLex (new DataInputStream(System.in));
            parser = new StreamItParser (lexer);
            System.out.println ("/*");
            System.out.println ("parsing:");
            parser.program ();
            System.out.println ("done parsing. outputing:");
            System.out.println(toStringList ((BaseAST)parser.getAST()));
            System.out.println ("done outputing. walking:");
            System.out.println ("*/");
            StreamItJavaTP walker = new StreamItJavaTP();
            walker.program(parser.getAST());  // walk tree
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    public static void main(String[] args)
    {
        new ToJava().run(args);
    }
}

