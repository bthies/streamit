/*******************************************************************************
 * StreamIt Editor adapted from Example Java Editor
 * modifier - Kimberly Kuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package texteditor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.graphics.RGB;
import texteditor.streamit.*;
import texteditor.streamitdoc.StreamItDocCompletionProcessor;
import texteditor.util.StreamItColorProvider;

/**
 * Example configuration for an <code>SourceViewer</code> which shows 
 * StreamIt code.
 */
public class StreamItSourceViewerConfiguration 
    extends SourceViewerConfiguration {
    
    /**
     * Single token scanner.
     */
    static class SingleTokenScanner extends BufferedRuleBasedScanner {
	public SingleTokenScanner(TextAttribute attribute) {
	    setDefaultReturnToken(new Token(attribute));
	}
    };
    
    /**
     * Default constructor.
     */
    public StreamItSourceViewerConfiguration() {
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
	return new StreamItAnnotationHover();
    }
	
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer,
						     String contentType) {
	return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) 
		? new StreamItAutoIndentStrategy() 
		: new DefaultAutoIndentStrategy());
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
	return new String[] { 
	    IDocument.DEFAULT_CONTENT_TYPE, 
		StreamItPartitionScanner.STREAMIT_DOC, 
		StreamItPartitionScanner.STREAMIT_MULTILINE_COMMENT 
		};
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
	
	ContentAssistant assistant = new ContentAssistant();
	assistant.setContentAssistProcessor(new StreamItCompletionProcessor(), 
					    IDocument.DEFAULT_CONTENT_TYPE);
	assistant.setContentAssistProcessor(new StreamItDocCompletionProcessor(),
					    StreamItPartitionScanner.
					    STREAMIT_DOC);
	
	assistant.enableAutoActivation(true);
	assistant.setAutoActivationDelay(500);
	assistant.setProposalPopupOrientation(IContentAssistant.
					      PROPOSAL_OVERLAY);
	assistant.setContextInformationPopupOrientation(IContentAssistant.
							CONTEXT_INFO_ABOVE);
	assistant
	    .setContextInformationPopupBackground(StreamItEditorEnvironment.
						  getStreamItColorProvider().
						  getColor(new 
							   RGB(150, 150, 0)));
	
	return assistant;
    }
	
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public String getDefaultPrefix(ISourceViewer sourceViewer, 
				   String contentType) {
	return (IDocument.
		DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null);
	//$NON-NLS-1$
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer 
							   sourceViewer,
							   String contentType) {
	return new StreamItDoubleClickSelector();
    }
	
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public String[] getIndentPrefixes(ISourceViewer sourceViewer, 
				      String contentType) {
	return new String[] { "\t", "    " }; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public IPresentationReconciler getPresentationReconciler(ISourceViewer 
							     sourceViewer) {
	
	StreamItColorProvider provider 
	    = StreamItEditorEnvironment.getStreamItColorProvider();
	PresentationReconciler reconciler = new PresentationReconciler();
	
	DefaultDamagerRepairer dr = 
	    new DefaultDamagerRepairer(StreamItEditorEnvironment.
				       getStreamItCodeScanner());
	reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
	reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	
	dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(StreamItColorProvider.STREAMITDOC_DEFAULT))));
	reconciler.setDamager(dr, StreamItPartitionScanner.STREAMIT_DOC);
	reconciler.setRepairer(dr, StreamItPartitionScanner.STREAMIT_DOC);
	
	dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(StreamItColorProvider.MULTI_LINE_COMMENT))));
	reconciler.setDamager(dr, StreamItPartitionScanner. 
			      STREAMIT_MULTILINE_COMMENT);
	reconciler.setRepairer(dr, StreamItPartitionScanner. 
			       STREAMIT_MULTILINE_COMMENT);
	
	return reconciler;
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public int getTabWidth(ISourceViewer sourceViewer) {
	return 4;
    }
    
    /* (non-StreamItdoc)
     * Method declared on SourceViewerConfiguration
     */
    public ITextHover getTextHover(ISourceViewer sourceViewer, 
				   String contentType) {
	return new StreamItTextHover();
    }
}
