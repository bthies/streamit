package streamit.eclipse.debugger.texteditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

/**
 * @author kkuo
 */
public class StreamItDocumentProvider extends CompilationUnitDocumentProvider {
	
	protected class StreamItAnnotationModel extends ResourceMarkerAnnotationModel {
		protected StreamItAnnotationModel(IFile file) {
			super(file);
		}
		protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
			return new JavaMarkerAnnotation(marker);
		}
	}
	
	/*
	 * @see AbstractDocumentProvider#createAnnotationModel(Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;
			return new StreamItAnnotationModel(input.getFile());
		}
		return super.createAnnotationModel(element);
	}
}