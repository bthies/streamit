package streamit.eclipse.debugger.texteditor;

import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import streamit.eclipse.debugger.core.IStreamItCoreConstants;
import streamit.eclipse.debugger.core.StrJavaData;
import streamit.eclipse.debugger.core.StrJavaMapper;

/**
 * @author kkuo
 */
public class StreamItContentProvider implements ITreeContentProvider {
	
	private final static String SEGMENTS = "__streamit_segments";
	private IPositionUpdater fPositionUpdater = new DefaultPositionUpdater(SEGMENTS);
	private TreeSet fContent;
	private IEditorInput fEditorInput;
	protected IDocumentProvider fDocumentProvider;

	public StreamItContentProvider(IDocumentProvider documentProvider, IEditorInput input) {
		super();
		fContent = new TreeSet();
		fEditorInput = input;
		fDocumentProvider = documentProvider;
	}
	
	protected void parse(IDocument document) {
		IFile fStrFile = (IFile) fEditorInput.getAdapter(IFile.class);
		StrJavaData data = StrJavaMapper.getInstance().loadStrFile(fStrFile, true);
		if (data == null) return;
			
		// get all types
		IFile fJavaFile = data.getJavaFile();
		ICompilationUnit unit = (ICompilationUnit) JavaCore.create(fJavaFile);
		try {
			synchronized (unit) {
				unit.reconcile();
			}

			IType[] types = unit.getAllTypes();
			IType streamType;
			StreamSegment parentSeg, seg;
			ISourceRange range;
			IDocument javaDocument = data.getJavaDocument();
			int strLineNumber;
			IRegion region;
			Position p;
			String superclassName, name;
			int type;
			for (int i = 0; i < types.length; i++) {
				streamType = types[i];
				superclassName = streamType.getSuperclassName();

				if (superclassName.equals(IStreamItCoreConstants.FILTER_CLASS)) {
					type = StreamSegment.FILTER_TYPE;
					name = streamType.getElementName() + IStreamItEditorConstants.WHITESPACED_COLON_STRING + superclassName;
				} else if (superclassName.equals(IStreamItCoreConstants.PIPELINE_CLASS) ||
							superclassName.equals(IStreamItCoreConstants.STREAMIT_CLASS)) {
					type = StreamSegment.PIPELINE_TYPE;
					name = streamType.getElementName() + IStreamItEditorConstants.WHITESPACED_COLON_STRING + IStreamItCoreConstants.PIPELINE_CLASS;
				} else if (superclassName.equals(IStreamItCoreConstants.SPLITJOIN_CLASS)) {
					type = StreamSegment.SPLITJOIN_TYPE;
					name = streamType.getElementName() + IStreamItEditorConstants.WHITESPACED_COLON_STRING + superclassName;
				} else if (superclassName.equals(IStreamItCoreConstants.FEEDBACKLOOP_CLASS)) {
					type = StreamSegment.FEEDBACKLOOP_TYPE;
					name = streamType.getElementName() + IStreamItEditorConstants.WHITESPACED_COLON_STRING + superclassName;
				} else {
					continue;
				}

				p = getPosition(types[i], javaDocument, document, data);
				document.addPosition(SEGMENTS, p);
				parentSeg = new StreamSegment(name, p, null, type);
				fContent.add(parentSeg);

				IField[] fields = streamType.getFields();
				for (int j = 0; j < fields.length; j++) {
					name = fields[j].getSource();
					name = fields[j].getElementName() + IStreamItEditorConstants.WHITESPACED_COLON_STRING + name.substring(0, name.indexOf(IStreamItEditorConstants.WHITESPACE_STRING));
					p = getPosition(fields[j], javaDocument, document, data);
					if (p == null) continue;
					document.addPosition(SEGMENTS, p);
					parentSeg.addVariable(new StreamSegment(name, p, parentSeg, StreamSegment.FIELD_TYPE));
				}
					
				IMethod[] methods = streamType.getMethods();
				String methodName;
				for (int j = 0; j < methods.length; j++) {
					methodName = methods[j].getElementName();
					if (superclassName.equals(IStreamItCoreConstants.FILTER_CLASS) &&
						(methodName.equals(IStreamItEditorConstants.INIT_METHOD) || methodName.equals(IStreamItEditorConstants.WORK_METHOD))) {
						p = getPosition(methods[j], javaDocument, document, data);
						document.addPosition(SEGMENTS, p);
						parentSeg.addMethod(new StreamSegment(methodName, p, parentSeg, StreamSegment.METHOD_TYPE));
					}
				}
			}		
		} catch (JavaModelException jme) {
		} catch (BadLocationException ble) {
		} catch (BadPositionCategoryException bpce) {
		}		
	}
	
	private Position getPosition(IMember type, IDocument javaDocument, IDocument strDocument, StrJavaData data) throws JavaModelException, BadLocationException {
		ISourceRange range = type.getSourceRange();
		int javaLineNumber = javaDocument.getLineOfOffset(range.getOffset()) + 1;
		if (data.getStreamItLineNumber(javaLineNumber) == null) return null;
		int strLineNumber = data.getStreamItLineNumber(javaLineNumber).getLineNumber();
		IRegion region = strDocument.getLineInformation(strLineNumber - 1);
		return new Position(region.getOffset(), region.getLength());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement == fEditorInput) return fContent.toArray();
		else if (parentElement instanceof StreamSegment) return ((StreamSegment) parentElement).getChildren();
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof StreamSegment) return ((StreamSegment) element).getParent();
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof StreamSegment) return ((StreamSegment) element).hasChildren();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return fContent.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		if (fContent == null) return;
		fContent.clear();
		fContent = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (oldInput != null) {
			IDocument document = fDocumentProvider.getDocument(oldInput);
			if (document != null) {
				try {
					document.removePositionCategory(SEGMENTS);
				} catch (BadPositionCategoryException x) {
				}
				document.removePositionUpdater(fPositionUpdater);
			}
		}
		    
		fContent.clear();
		    
		if (newInput != null) {
			IDocument document = fDocumentProvider.getDocument(newInput);
			if (document != null) {
				document.addPositionCategory(SEGMENTS);
				document.addPositionUpdater(fPositionUpdater);
				    
				parse(document);
			}
		}
	}

	/*
	 * @see IContentProvider#isDeleted(Object)
	 */
	public boolean isDeleted(Object element) {
		return false;
	}
}
