package streamit.eclipse.debugger.properties;

import org.eclipse.ui.views.properties.IPropertySheetEntry;

import streamit.eclipse.debugger.graph.IStreamItGraphConstants;
import streamit.eclipse.debugger.model.Pipeline;

/**
 * @author kkuo
 */
public class StreamStructurePropertyFactory {
	
	private static StreamStructurePropertyFactory fInstance = new StreamStructurePropertyFactory();
	
	/**
	 * Creates a new StreamStructurePropertyFactory.
	 */
	private StreamStructurePropertyFactory() {
	}
	
	/**
	 * Returns the singleton StreamStructurePropertyFactory.
	 */
	public static StreamStructurePropertyFactory getInstance() {
		return fInstance;
	}
	
	public IPropertySheetEntry makeStream(Pipeline topLevelPipeline) {
		return new StreamGraphPropertySheetEntry(topLevelPipeline.getNameWithRuntimeId(), IStreamItGraphConstants.EMPTY_STRING, topLevelPipeline);
	}
}
