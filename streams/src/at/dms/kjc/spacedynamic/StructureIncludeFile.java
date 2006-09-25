package at.dms.kjc.spacedynamic;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph.*;

/*******************************************************************************
 * This class will create any necessary c type definitions that represent the
 * structures of the application. Also it will create functions to send/receive
 * the structures over the static/dynamic network
 ******************************************************************************/

public class StructureIncludeFile {
    private StreamGraph streamGraph;

    private HashSet<SIRStructure> structSet;

    /**
     * Create structures include file in current directory.
     */
    public static void doit(SIRStructure[] structs, StreamGraph sg) {
        doit(structs, sg, ".");
    }

    /**
     * Create structures include file in directory <pre>dir</pre>.
     */
    public static void doit(SIRStructure[] structs, StreamGraph sg, String dir) {
        if (structs == null || structs.length == 0)
            return;

        new StructureIncludeFile(structs, sg, dir);
    }

    public StructureIncludeFile(SIRStructure[] structs, StreamGraph sg,
                                String dir) {
        this.streamGraph = sg;
        structSet = new HashSet<SIRStructure>();
        for (int i = 0; i < structs.length; i++)
            structSet.add(structs[i]);

        // CClass[] passedStructs;
        // passedStructs = (CClass[])getPassedStructs().toArray(new CClass[0]);

        try {
            FileWriter fw = new FileWriter(dir + "/structs.h");
            fw.write("unsigned " + FlatIRToC.DYNMSGHEADER + ";\n");
            createStructureDefs(structs, fw);
            if (!KjcOptions.standalone) {
                createPushPopFunctions(structs, fw, "Dynamic");
                createPushPopFunctions(structs, fw, "Static");
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error creating structure include file");
        }
    }

    /**
     * create a c header file with all the structure definitions as typedef'ed
     * structs.
     */
    private void createStructureDefs(SIRStructure[] structs, FileWriter fw)
        throws Exception {
        fw.write("#ifndef round\n#define round(x) (floorf((x)+0.5))\n#endif\n");
        fw.write("#ifndef roundf\n#define roundf(x) (floorf((x)+0.5))\n#endif\n");
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];
            fw.write("typedef struct __" + current.getIdent() + " {\n");
            for (int j = 0; j < current.getFields().length; j++) {
                fw.write("\t" + current.getFields()[j].getType() + " "
                         + current.getFields()[j].getVariable().getIdent()
                         + ";\n");
            }
            fw.write("} " + current.getIdent() + ";\n");
            // write the defs for the push/pop functions
            if (!KjcOptions.standalone) {
                String network;
                for (int x = 0; x < 2; x++) {
                    if (x == 0)
                        network = "Dynamic";
                    else
                        network = "Static";

                    if (x == 0) {
                        // for the dynamic network we must create two push
                        // functions
                        // one that sends a header and one that does not,
                        // the one that does not send the header is used by
                        // enclosing structs
                        // push functions, only one header is sent and it
                        // accounts for the
                        // total size of the struct
                        fw.write("inline void pushNested" + network
                                 + current.getIdent() + "(" + current.getIdent()
                                 + "*);\n");
                    }

                    fw.write("inline void push" + network + current.getIdent()
                             + "(" + current.getIdent() + "*);\n");
                    fw.write("inline " + current.getIdent() + " pop" + network
                             + current.getIdent() + "();\n");
                    fw.write("inline void "
                             + RawExecutionCode.structReceivePrefix + network
                             + current.getIdent() + "(" + current.getIdent()
                             + "*);\n\n");
                }
            }
        }
    }

    /** create the push/pop functions for the structs, over <pre>network</pre> * */
    private void createPushPopFunctions(SIRStructure[] structs, FileWriter fw,
                                        String network) throws Exception {
        assert network.equals("Static") || network.equals("Dynamic");
        // which network we are using
        boolean dynamic = false;
        if (network.equals("Dynamic"))
            dynamic = true;

        // create the pop functions
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];

            fw.write("inline " + current.getIdent() + " pop" + network
                     + current.getIdent() + "() {\n");
            fw.write("\t" + current.getIdent() + "* temp = ("
                     + current.getIdent() + "*)" + "malloc(sizeof("
                     + current.getIdent() + "));\n");
            for (int j = 0; j < current.getFields().length; j++) {
                fw.write("\t//" + current.getFields()[j].getType() + "\n");
                if (current.getFields()[j].getType().isArrayType()) {
                    // System.out.println(((CArrayType)current.getFields()[j].getType()).getDims());
                    // assert false;
                } else if (current.getFields()[j].getType().isClassType()) {

                    fw.write("\ttemp->"
                             + current.getFields()[j].getVariable().getIdent()
                             + " = pop" + network
                             + current.getFields()[j].getType() + "();\n");
                } else {
                    fw.write("\t");
                    fw.write("temp->"
                             + current.getFields()[j].getVariable().getIdent());
                    fw.write(" = ");
                    fw.write(Util.networkReceive(dynamic,
                                                 current.getFields()[j].getType())
                             + ";\n");
                }
            }
            fw.write("\treturn *temp;\n}\n");

            // create the pop functions that take a pointer argument
            // these are more efficent, we use these when we can
            fw.write("inline void " + RawExecutionCode.structReceivePrefix
                     + network + current.getIdent() + "(" + current.getIdent()
                     + "* temp) {\n");
            for (int j = 0; j < current.getFields().length; j++) {
                if (current.getFields()[j].getType().isArrayType()) {
                    // assert false;
                } else if (current.getFields()[j].getType().isClassType()) {
                    // if this is struct field, call the struct's popPointer
                    // method
                    fw.write("\t" + RawExecutionCode.structReceivePrefix
                             + network + current.getFields()[j].getType()
                             + "(&temp->"
                             + current.getFields()[j].getVariable().getIdent()
                             + ");\n");
                } else {
                    fw.write("\t");
                    fw.write("temp->"
                             + current.getFields()[j].getVariable().getIdent());
                    fw.write(" = ");
                    fw.write(Util.networkReceive(dynamic,
                                                 current.getFields()[j].getType())
                             + ";\n");
                }
            }
            fw.write("}\n");

            // create the push functions,
            // for the dynamic network we must create two push functions
            // one that sends a header and one that does not,
            // the one that does not send the header is used by enclosing
            // structs
            // push functions, only one header is sent and it accounts for the
            // total size of the struct
            for (int funct = 0; funct < 2; funct++) {
                // if we are generating a dynamic push function that does not
                // need to send a header
                // set this to "Nested"
                String dynHeader = "";

                // the static network only needs one push method
                if (network.equals("Static") && funct == 1)
                    break;
                // set dynHeader to nested if this is dynamic and 2nd function
                // decl
                if (network.equals("Dynamic") && funct == 1)
                    dynHeader = "Nested";

                fw.write("inline void push" + dynHeader + network
                         + current.getIdent() + "(" + current.getIdent()
                         + "* temp) {\n");

                if (network.equals("Dynamic") && funct == 0) {
                    // we must generate the header for the dynamic network send
                    // (not the nested version)
                    fw.write(Util.CGNOINTVAR + " = " + FlatIRToC.DYNMSGHEADER
                             + ";\n");
                }

                for (int j = 0; j < current.getFields().length; j++) {
                    // if this field is a struct type, use its method to push
                    // the field
                    if (current.getFields()[j].getType().isArrayType()) {
                        // System.out.println(((CArrayType)current.getFields()[j].getType()).getDims()[0]);
                        // assert false;
                    } else if (current.getFields()[j].getType().isClassType()) {
                        // if we have a nested struct in this struct and we are
                        // sending the struct
                        // over the dynamic network, we use the push function of
                        // the nested struct that
                        // does not send over a header, call this function
                        // pushNested***
                        String nested = "";
                        if (network.equals("Dynamic")) {
                            nested = "Nested";
                        }

                        fw.write("push"
                                 + nested
                                 + network
                                 + current.getFields()[j].getType()
                                 + "(&temp->"
                                 + current.getFields()[j].getVariable()
                                 .getIdent() + ");\n");
                    } else {
                        fw.write("\t"
                                 + Util.networkSendPrefix(dynamic, current
                                                          .getFields()[j].getType()));
                        fw.write("temp->"
                                 + current.getFields()[j].getVariable()
                                 .getIdent());
                        fw.write(Util.networkSendSuffix(dynamic) + ";\n");
                    }
                }
                fw.write("}\n");
            }

        }
    }

    // iterator thru all the flat nodes and remember the structs that are passed
    // over tapes
    private Vector<CClass> getPassedStructs() {
        Vector<CClass> passedStructs = new Vector<CClass>();

        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            StaticStreamGraph ssg = (StaticStreamGraph)streamGraph.getStaticSubGraphs()[i];

            Iterator flatNodes = ssg.getFlatNodes().iterator();
            while (flatNodes.hasNext()) {
                FlatNode node = (FlatNode) flatNodes.next();

                if (node.isFilter()) {
                    // if this is a struct then add it to the passed types set
                    // System.out.println(node.getFilter().getInputType());
                    // System.out.println(node.getFilter().getOutputType());
                    if (node.getFilter().getOutputType().isClassType()
                        && !passedStructs.contains(node.getFilter()
                                                   .getOutputType().getCClass())) {
                        passedStructs.add(node.getFilter().getOutputType()
                                          .getCClass());
                        System.out.println(node.getFilter().getOutputType()
                                           + " is passed on the tape");
                    }

                    if (node.getFilter().getInputType().isClassType()
                        && !passedStructs.contains(node.getFilter()
                                                   .getInputType().getCClass())) {
                        passedStructs.add(node.getFilter().getInputType()
                                          .getCClass());
                        System.out.println(node.getFilter().getInputType()
                                           + " is passed on the tape");
                    }
                }
            }
        }
        return passedStructs;
    }
}
