package streamit.eclipse.grapheditor;


   import org.eclipse.ui.IWorkbenchWindow;
   import org.eclipse.ui.IWorkbenchWindowActionDelegate;
   import org.eclipse.jface.action.IAction;
   import org.eclipse.jface.dialogs.MessageDialog;
   import org.eclipse.jface.viewers.ISelection;
   import org.eclipse.swt.widgets.Shell;

/*
 * 
 * <extension point = "org.eclipse.ui.actionSets">
           <actionSet
               id="grapheditor.HelloWorldActionSet"
               label="Hello World"
               visible="true"
               description="The action set for the Eclipse Hello World example">
               <menu
                   id="grapheditor.HelloWorldMenu"
                   label="Samples">
                   <separator name="samples"/>
               </menu>
               <action id="grapheditor.actions.HelloWorldAction"
                   menubarPath="grapheditor.HelloWorldMenu/samples"
                   toolbarPath="Normal"
                   label="Hello World"
                   tooltip="Press to see a message"
                   icon="icons/helloworld.gif"
                   class="grapheditor.HelloWorldAction"/>
           </actionSet>
       </extension>
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */	
	
	
	

   /** HelloWorldAction is a simple example of using an
	* action set to extend the Eclipse Workbench with a menu 
	* and toolbar action that prints the "Hello World" message.
	*/
   public class HelloWorldAction implements IWorkbenchWindowActionDelegate {
		   IWorkbenchWindow activeWindow = null;
		
			
		   /** Run the action. Display the Hello World message
			*/             
		   public void run(IAction proxyAction) {
				   // proxyAction has UI information from manifest file (ignored)
				   try{
				   
				   Shell shell = activeWindow.getShell();
				   System.out.println("Before the creation of the pipeline");

				   //GEPipeline pipe = new GEPipeline("Juan's Pipeline");
				  // StreamItEditor.Test();
				  // at.dms.kjc.StreaMITMain.test();
				   System.out.println("TESTING");
				  MessageDialog.openInformation(shell, "Hello", "Hello");
				   } catch (Exception e)
				   {
				   	
				   		System.out.println(" The exception is " + e.toString());
				   }
				// System.out.println("name" + pipe.getName());
		   }

		   // IActionDelegate method
		   public void selectionChanged(IAction proxyAction, ISelection selection) {
				   // do nothing, action is not dependent on the selection
		   }
           
		   // IWorkbenchWindowActionDelegate method
		   public void init(IWorkbenchWindow window) {
				   activeWindow = window;
		   }
           
		   // IWorkbenchWindowActionDelegate method
		   public void dispose() {
				   //  nothing to do
		   }
   }