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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import texteditor.streamit.StreamItPartitionScanner;

/** 
 * The StreamItDocumentProvider provides the IDocuments used by StreamIt editors.
 */

public class StreamItDocumentProvider extends FileDocumentProvider {
    
    private final static String[] TYPES = new String[] { 
	StreamItPartitionScanner.STREAMIT_DOC, 
	StreamItPartitionScanner.STREAMIT_MULTILINE_COMMENT 
    };
    
    private static StreamItPartitionScanner fgScanner = null;
    
    public StreamItDocumentProvider() {
	super();
    }
    
    /* (non-StreamItdoc)
     * Method declared on AbstractDocumentProvider
     */
    protected IDocument createDocument(Object element) throws CoreException {
	IDocument document = super.createDocument(element);
	if (document != null) {
	    IDocumentPartitioner partitioner = createStreamItPartitioner();
	    document.setDocumentPartitioner(partitioner);
	    partitioner.connect(document);
	}
	return document;
    }
    
    /**
     * Return a partitioner for .str files.
     */
    private IDocumentPartitioner createStreamItPartitioner() {
	return new DefaultPartitioner(getStreamItPartitionScanner(), TYPES);
    }
    
    /**
     * Return a scanner for creating StreamIt partitions.
     */
    private StreamItPartitionScanner getStreamItPartitionScanner() {
	if (fgScanner == null)
	    fgScanner = new StreamItPartitionScanner();
	return fgScanner;
    }
}
