/*
 * Created on Nov 14, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author jcarlos
 *
 */
public class GEStreamNodeControllerRegistry {


	/** 
	 * A list with registered the registered GEStreamNode controllers.  
	 */
	protected static ArrayList streamNodeControllers = new ArrayList();

	/** At this static initializer the default GEStreamNodeControllers 
	 *  will register
	 */
	static {
		System.out.println("*##*#*#*#*#*#*#*#*#*#* INSIDE STATIC INITIALIZER");
		streamNodeControllers.add(new GEPipelineController());
		streamNodeControllers.add(new GEFilterController());
		streamNodeControllers.add(new GESplitterController());
		streamNodeControllers.add(new GESplitJoinController());
		streamNodeControllers.add(new GEJoinerController());
//		streamNodeControllers.add(new GEFeedbackLoopController());
		
		sort();
	}

	/** Adds a GEStreamNodeController to this 
	 *  registry
	 */
	public static void addLayoutController(GEStreamNodeController controller) {
		streamNodeControllers.add(controller);
		sort();
	}

	/** Removes a GEStreamNodeController from this 
	 *  registry.
	 */
	public static void removeLayoutController(GEStreamNodeController controller) {
		streamNodeControllers.remove(controller);
	}

	/** Returns all registered GEStreamNodeController.
	 */
	public static Iterator registeredControllers() {
		return streamNodeControllers.iterator();
	}

	protected static void sort() {
		Collections.sort(streamNodeControllers, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((GEStreamNodeController) o1).toString().compareTo(
					((GEStreamNodeController) o2).toString());
			}

			public boolean equals(Object obj) {
				return false;
			}
		});
	}



}
