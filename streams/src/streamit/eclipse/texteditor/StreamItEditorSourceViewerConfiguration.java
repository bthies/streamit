/*******************************************************************************
 * StreamIt Editor adapted from JavaSourceViewerConfiguration.java
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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.*;

/**
 * Configuration for a source viewer which shows StreamIt code.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class StreamItEditorSourceViewerConfiguration extends SourceViewerConfiguration {
	
	private JavaTextTools fJavaTextTools;	
	private ITextEditor fTextEditor;
	//	CHANGED by kkuo
	private JavaSourceViewerConfiguration fJavaSVC;
	
	/**
	 * Creates a new StreamIt source viewer configuration for viewers in the given editor.
	 *
	 * @param editor the editor in which the configured viewer(s) will reside
	 */
	public StreamItEditorSourceViewerConfiguration(ITextEditor editor) {
		// CHANGED by kkuo
		fJavaTextTools = new JavaTextTools(PreferenceConstants.getPreferenceStore());
		fTextEditor = editor;
		fJavaSVC = new JavaSourceViewerConfiguration(fJavaTextTools, editor);
	}
	
	/**
	 * Returns the Java source code scanner for this configuration.
	 *
	 * @return the Java source code scanner
	 */
	protected RuleBasedScanner getCodeScanner() {
		// CHANGED by kkuo
		return new StreamItEditorCodeScanner(getColorManager(), getPreferenceStore());
	}
	
	/**
	 * Returns the Java multiline comment scanner for this configuration.
	 *
	 * @return the Java multiline comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getMultilineCommentScanner() {
		return fJavaTextTools.getMultilineCommentScanner();
	}
	
	/**
	 * Returns the Java singleline comment scanner for this configuration.
	 *
	 * @return the Java singleline comment scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getSinglelineCommentScanner() {
		return fJavaTextTools.getSinglelineCommentScanner();
	}
	
	/**
	 * Returns the Java string scanner for this configuration.
	 *
	 * @return the Java string scanner
	 * @since 2.0
	 */
	protected RuleBasedScanner getStringScanner() {
		return fJavaTextTools.getStringScanner();
	}
	
	/**
	 * Returns the JavaDoc scanner for this configuration.
	 *
	 * @return the JavaDoc scanner
	 */
	protected RuleBasedScanner getJavaDocScanner() {
		return fJavaTextTools.getJavaDocScanner();
	}
	
	/**
	 * Returns the color manager for this configuration.
	 *
	 * @return the color manager
	 */
	protected IColorManager getColorManager() {
		return fJavaTextTools.getColorManager();
	}

	/**
	 * Returns the preference store used by this configuration to initialize
	 * the individual bits and pieces.
	 * 
	 * @return the preference store used to initialize this configuration
	 * 
	 * @since 2.0
	 */
	protected IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}

	// CHANGED by kkuo
	// some unused functions deleted ...
	
	/*
	 * @see SourceViewerConfiguration#getPresentationReconciler(ISourceViewer)
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler= new PresentationReconciler();

		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr= new DefaultDamagerRepairer(getJavaDocScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_DOC);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_DOC);

		dr= new DefaultDamagerRepairer(getMultilineCommentScanner());		
		reconciler.setDamager(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);

		dr= new DefaultDamagerRepairer(getSinglelineCommentScanner());		
		reconciler.setDamager(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
		
		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_STRING);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_STRING);
		
		dr= new DefaultDamagerRepairer(getStringScanner());
		reconciler.setDamager(dr, IJavaPartitions.JAVA_CHARACTER);
		reconciler.setRepairer(dr, IJavaPartitions.JAVA_CHARACTER);
		
		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getContentAssistant(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getReconciler(ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getReconciler(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getAutoIndentStrategy(ISourceViewer, String)
	 */
	public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getAutoIndentStrategy(sourceViewer, contentType);
	}

	/*
	 * @see SourceViewerConfiguration#getDoubleClickStrategy(ISourceViewer, String)
	 */
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getDoubleClickStrategy(sourceViewer, contentType);
	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer, String)
	 * @since 2.0
	 */
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getDefaultPrefixes(sourceViewer, contentType);
	}

	/*
	 * @see SourceViewerConfiguration#getIndentPrefixes(ISourceViewer, String)
	 */
	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getIndentPrefixes(sourceViewer, contentType);
	}

	/*
	 * @see SourceViewerConfiguration#getTabWidth(ISourceViewer)
	 */
	public int getTabWidth(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getTabWidth(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getAnnotationHover(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer, String)
	 * @since 2.1
	 */
	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
	}
	
	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
	 * @since 2.1
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
		// CHANGED by kkuo
		return fJavaSVC.getTextHover(sourceViewer, contentType, stateMask);
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		// CHANGED by kkuo
		return fJavaSVC.getTextHover(sourceViewer, contentType);
	}
	
	/*
	 * @see SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
	 */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getConfiguredContentTypes(sourceViewer);
	}
	
	/*
	 * @see SourceViewerConfiguration#getContentFormatter(ISourceViewer)
	 */
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getContentFormatter(sourceViewer);
	}
	
	/*
	 * @see SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getInformationControlCreator(sourceViewer);
	}
	
	/*
	 * @see SourceViewerConfiguration#getInformationPresenter(ISourceViewer)
	 * @since 2.0
	 */
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		// CHANGED by kkuo
		return fJavaSVC.getInformationPresenter(sourceViewer);
	}

	/**
	 * Returns the outline presenter which will determine and shown
	 * information requested for the current cursor position.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute the Java element 
	 * @return an information presenter
	 * @since 2.1
	 */
	public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
		// CHANGED by kkuo
		return fJavaSVC.getOutlinePresenter(sourceViewer, doCodeResolve);
	}
}