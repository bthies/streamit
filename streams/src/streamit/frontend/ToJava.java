package streamit.frontend;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import antlr.BaseAST;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import streamit.frontend.nodes.*;
import streamit.frontend.tojava.*;

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

    private boolean useNewPath = false;
    private boolean printHelp = false;
    private String outputFile = null;
    private List inputFiles = new ArrayList();

    public void doOptions(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("--adhoc"))
                useNewPath = false;
            else if (args[i].equals("--full"))
                useNewPath = true;
            else if (args[i].equals("--help"))
                printHelp = true;
            else if (args[i].equals("--"))
            {
                // Add all of the remaining args as input files.
                for (i++; i < args.length; i++)
                    inputFiles.add(args[i]);
            }
            else if (args[i].equals("--output"))
                outputFile = args[++i];
            else
                // Maybe check for unrecognized options.
                inputFiles.add(args[i]);
        }
    }

    public void run(String[] args)
    {
        doOptions(args);
        if (printHelp)
        {
            printUsage();
            return;
        }
        if (useNewPath)
            runNew();
        else
            runOld();
    }

    public void runOld()
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

    public void runNew()
    {
        StreamItParserFE parser = null;
        Program prog = null;
        Writer outWriter;

        try
        {
            if (outputFile != null)
                outWriter = new FileWriter(outputFile);
            else
                outWriter = new OutputStreamWriter(System.out);

            for (Iterator iter = inputFiles.iterator(); iter.hasNext(); )
            {
                InputStream inStream = new FileInputStream((String)iter.next());
                DataInputStream dis = new DataInputStream(inStream);
                StreamItLex lexer = new StreamItLex(dis);
                parser = new StreamItParserFE(lexer);
                prog = parser.program();
            }
            String javaOut = (String)prog.accept(new NodesToJava(null));
            outWriter.write(javaOut);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            return;
        }
        
    }
    
    public static void main(String[] args)
    {
        new ToJava().run(args);
    }
}

