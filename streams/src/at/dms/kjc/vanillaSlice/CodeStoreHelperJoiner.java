package at.dms.kjc.vanillaSlice;

import at.dms.kjc.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import at.dms.util.Utils;

public class CodeStoreHelperJoiner extends CodeStoreHelper {

    
    
    public CodeStoreHelperJoiner(SliceNode node, BackEndFactory backEndBits) {
        super(node, backEndBits);
    }
    @Override
    public JMethodDeclaration getInitStageMethod() {
        if (getWorkMethod() == null) {
            return null;
        }
        // if we have a work method, iterate it enough
        // for downstream filter.
        JBlock statements = new JBlock();
        FilterInfo filterInfo = FilterInfo.getFilterInfo(sliceNode.getNext().getAsFilter());

        // channel code before work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).beginInitRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).beginInitWrite()) {
            statements.addStatement(stmt);
        }
        // work block
        statements.addStatement(getWorkFunctionBlock(filterInfo.initItemsReceived()));
        // channel code after work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).endInitRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).endInitWrite()) {
            statements.addStatement(stmt);
        }
        
        
        return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                initStage + uniqueID,
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                statements,
                null,
                null);
    }

    
    @Override
    public JMethodDeclaration getPrimePumpMethod() {
        if (primePumpMethod != null) {
            return primePumpMethod;
        }
        JBlock statements = new JBlock();
        FilterInfo filterInfo = FilterInfo.getFilterInfo(sliceNode.getNext().getAsFilter());
        
        // channel code before work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).beginSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).beginSteadyWrite()) {
            statements.addStatement(stmt);
        }
        // code for a steady-state iteration
        statements.addStatement(getWorkFunctionBlock(filterInfo.totalItemsReceived(SchedulingPhase.PRIMEPUMP)));
        // channel code after work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).endSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).endSteadyWrite()) {
            statements.addStatement(stmt);
        }

        
        //return the method
        primePumpMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                      CStdType.Void,
                                      primePumpStage + uniqueID,
                                      JFormalParameter.EMPTY,
                                      CClassType.EMPTY,
                                      statements,
                                      null,
                                      null);
        return primePumpMethod;
    }

    @Override
    public JBlock getSteadyBlock() {
        if (getWorkMethod() == null) {
            return null;
        }
        JBlock statements = new JBlock();
        FilterInfo filterInfo = FilterInfo.getFilterInfo(sliceNode.getNext().getAsFilter());
        
        // channel code before work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).beginSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).beginSteadyWrite()) {
            statements.addStatement(stmt);
        }
        // work block / work call
        statements.addStatement(getWorkFunctionBlock(filterInfo.totalItemsReceived(SchedulingPhase.STEADY)));
        // channel code after work block
        for (InterSliceEdge e : sliceNode.getAsInput().getSourceList()) {
            for (JStatement stmt : backEndBits.getChannel(e).endSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        for (JStatement stmt : backEndBits.getChannel(sliceNode.getEdgeToNext()).endSteadyWrite()) {
            statements.addStatement(stmt);
        }
        return statements;
    }


    @Override
    protected JBlock getWorkFunctionBlock(int mult) {
        JBlock block = new JBlock();
        JStatement workStmt = getWorkFunctionCall();
        JVariableDefinition loopCounter = new JVariableDefinition(null,
                0,
                CStdType.Integer,
                workCounter,
                null);

        JStatement loop = 
            Utils.makeForLoopLocalIndex(workStmt, loopCounter, new JIntLiteral(mult));
        block.addStatement(new JVariableDeclarationStatement(null,
                loopCounter,
                null));

        block.addStatement(loop);
        return block;
    }
}
