package streamit.eclipse.debugger;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * @author kkuo
 */ 
public class StreamItSaveParticipant implements ISaveParticipant {

	public void doneSaving(ISaveContext context) {
		WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.CLOSE_EDITORS_ON_EXIT,true);
	}

	public void prepareToSave(ISaveContext context) {
	} 

	public void rollback(ISaveContext context) {
		WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.CLOSE_EDITORS_ON_EXIT,true);
	} 

	public void saving(ISaveContext context) {
	}
}
