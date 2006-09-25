package at.dms.kjc.spacetime;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.iterator.*;
import at.dms.kjc.flatgraph.*;

// need to get parameters for file readers / file writers
// remove parameters from add pipeline, put them into new subpipeline

/**
 * Separate FileReaders from splitters, FileWriters from joiners. 
 * 
 * <p>The spacetime compiler can not cope with a FileReader directly
 * before a splitter or a FileWriter directly after a joiner.  This
 * little pass inserts identity filters to avoid these cases.
 * 
 * The filters are not the SIRIdentity builtin filter which  
 * sir/lowering/fusion/Lifter removes (and possibly other optimization
 * passes also remove).
 * </p>
 * @author dimock
 *
 */
public class SafeFileReaderWriterPositions {
    /**
     * constructor.
     *
     */
    private SafeFileReaderWriterPositions() {}
    /**
     * Make FileReader positions safe with respect to splitters and
     * FileWriter positions safe with respect to joiners.
     * 
     * @param str the root SIRStream
     */
    public static void doit(SIRStream str) {
        new SafeFileReaderWriterPositions().doitInternal(str);
    }
    /**
     * FileReaders preceeding splitters
     */
    private static HashSet<SIROperator> readerBeforeSplitter;
    /**
     * FileWriters following joiners
     */
    private static HashSet<SIROperator> writerAfterJoiner;
    
    
    private void doitInternal(SIRStream str) {
        readerBeforeSplitter = new HashSet<SIROperator>();
        writerAfterJoiner = new HashSet<SIROperator>();
        
        // use FlatGraph to eliminate intermediate levels of pipelines
        // when looking at stream.
        GraphFlattener fg = new GraphFlattener(str);
        // fill out readerBeforeSplitter, writerAfterJoiner
        fg.top.accept(new CheckGraph(), new HashSet<FlatNode>(), false);
        // any work needing doing?
        if (readerBeforeSplitter.isEmpty()
            &&  writerAfterJoiner.isEmpty()) {
            return;
        }
        // OK, there is work to do.  Iterate over the Str representation
        // replacing readerBeforeSplitter with a pipeline of reader and identity
        // and replacing writerAfterJoiner with a pipeline of identity and writer.
        SIRIterator it = IterFactory.createFactory().createIter(str);
        it.accept(new FixGraph());
    }

    private class CheckGraph implements FlatVisitor {
        public void visitNode(FlatNode node) {
            if (node.contents instanceof SIRFileReader) {
                FlatNode[] outgoing = node.edges;
                if (outgoing.length == 1
                        && outgoing[0].contents instanceof SIRSplitter) {
                    readerBeforeSplitter.add(node.contents);
                }
            } else if (node.contents instanceof SIRFileWriter) {
                FlatNode[] incoming = node.incoming;
                if (incoming.length == 1
                        && incoming[0].contents instanceof SIRJoiner) {
                    writerAfterJoiner.add(node.contents);
                }
            }
        }
    }

    /**
     * A StreamVisitor for use with createIter.accept to change stream rep.
     * 
     * <p>Uses private information in surrounding class to identify where
     * a filter should be replaced by a pipeline consisting of the original
     * filter preceeded or followed by an identity filter.</p>
     * @author dimock
     *
     */
    private class FixGraph extends EmptyStreamVisitor {
        /**
         * Insert between FileReader and splitter, between joiner and FileWriter.
         * 
         * <p>Uses private information in surrounding class to identify where
         * a filter should be replaced by a pipeline consisting of the original
         * filter preceeded or followed by an identity filter.</p>
         */
        public void visitFilter(SIRFilter self,
                SIRFilterIter iter) {
            if (self instanceof SIRFileReader
                && readerBeforeSplitter.contains(self)) {
                CType outputType = self.getOutputType();
                SIRContainer parent = (SIRContainer)iter.getParent().getStream();
                LinkedList<Object> pipeChildren = new LinkedList<Object>();
                SIRFilter id = makeIdentityFilter(outputType);
                //SIRFilter id = new SIRIdentity(outputType);
                RenameAll.renameAllFilters(id);
                pipeChildren.add(self);
                pipeChildren.add(id);
                makePipeline(self,parent,pipeChildren);
            } else if (self instanceof SIRFileWriter
                    && writerAfterJoiner.contains(self)) {
                CType inputType = self.getInputType();
                SIRContainer parent = (SIRContainer)iter.getParent().getStream();
                LinkedList<Object> pipeChildren = new LinkedList<Object>();
                SIRFilter id = makeIdentityFilter(inputType);
                //SIRFilter id = new SIRIdentity(inputType);
                RenameAll.renameAllFilters(id);
                pipeChildren.add(id);
                pipeChildren.add(self);
                makePipeline(self,parent,pipeChildren);
            }
        }
    }
 
    private void makePipeline(SIRFilter self, SIRContainer parent, LinkedList/*<SIRFilter>*/<Object>pipeChildren) {
        JMethodDeclaration init = makeInit();
        SIRPipeline pipe = new SIRPipeline(parent,
                "generatedPipeline"+makeNewSuffix(),
                new JFieldDeclaration[]{},
                new JMethodDeclaration[]{init});
        pipe.setChildren(pipeChildren);
        pipe.setInit(init);
        parent.replace(self,pipe);
    }
    
    private static JMethodDeclaration makeInit() {
        JMethodDeclaration init =
            new JMethodDeclaration(
                CStdType.Void,
                "init",
                new JFormalParameter[]{},
                new JBlock());
        return init;
    }
    
    /** How many things have been renamed.  Used to uniquify names. */
    private static int counter = 0;
    
    /** Unique name -- we hope.
     * 
     *  Why is there no centralized facility for creating unique names?
     * 
     * @return         a (hopefully) unique string
     */
    private static String makeNewSuffix() {
        String suffix = "__" + counter;
        counter++;
        return suffix;
    }
  
    /**
     * Create a new identity filter at a specified type.
     * 
     * @param typ  input and output CType for identity filter
     * @return     a new identity filter of the requested type
     *             that will need a setParent before being usable.
     */
    public static SIRFilter makeIdentityFilter(CType typ) {
        JMethodDeclaration init = makeInit();
        String suffix = makeNewSuffix();
        JVariableDefinition vdefn =
        new JVariableDefinition(typ, "tmp"+suffix);
        JVariableDeclarationStatement vdecl =
            new JVariableDeclarationStatement(vdefn);
        JBlock idBlock = new JBlock(
                new JStatement[]{
                    vdecl, 
                    new JExpressionStatement(
                        new JAssignmentExpression(
                                new JLocalVariableExpression(vdefn),
                                new SIRPopExpression(typ))),
                        new JExpressionStatement(
                           new SIRPushExpression(
                                   new JLocalVariableExpression(vdefn),
                                   typ))});     
        JMethodDeclaration work = new JMethodDeclaration(
            CStdType.Void,
            "work"/* + suffix*/,
            new JFormalParameter[]{},
            idBlock);
        SIRFilter id = new SIRFilter(
                null, /* parent set later */
                "generatedIdFilter"+suffix,
                new JFieldDeclaration[]{},
                new JMethodDeclaration[]{init,work},
                new JIntLiteral(1), /* peek */
                new JIntLiteral(1), /* push */
                new JIntLiteral(1), /* pop */
                work, typ, typ
                );
        id.setWork(work);
        id.setInit(init);
        return id;
    }

}
