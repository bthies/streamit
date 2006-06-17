/**
 * 
 */
package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.*;

/**
* This class stores statistics needed to generate automatic performance 
* statistics on the raw simulator.  For each file writer, this class calculates
* how many items will be written for the init and for each steady-state.
* 
* @author mgordon
*
*/
public class NumberGathering 
{
   /** the file writers of the application */
   public HashSet<FlatNode> fileWriters;
   /** holds the number of items each fw writes in steady state */ 
   public HashMap<FlatNode, Integer> steady;
   /** holds the number of items each fw writes in init */ 
   public HashMap<FlatNode, Integer> skip;
   /** holds the index of each filewriter */
   public HashMap<FlatNode, Integer> index;
   /** total number of items written in init stage */
   public int totalSteadyItems;
   
   public void doit(SpdStreamGraph streamGraph) {
       steady = new HashMap<FlatNode, Integer>();
       skip = new HashMap<FlatNode, Integer>();
       fileWriters = new HashSet<FlatNode>();
       index = new HashMap<FlatNode, Integer>();
       int id = 0;
       
       totalSteadyItems = 0;
       boolean staticApp = true;
       if (streamGraph.getStaticSubGraphs().length > 1) {
           assert KjcOptions.ssoutputs > 0 : 
               "--number <int> requires the use of --ssoutputs with a dynamic rate app.";
           staticApp = false;
       }
       for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
           SpdStaticStreamGraph ssg = 
               (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i];
           Iterator<FlatNode> flatNodes = ssg.getFlatNodes().iterator();
           while (flatNodes.hasNext()) {
               FlatNode node = flatNodes.next();
               if (node.isFilter() && node.getFilter() instanceof SIRFileWriter) {
                   //we have found a file writer!
                   //now record its information
                   assert node.getFilter().getInputType().isNumeric() :
                       "Non-numeric type for input to filewriter not supported.";
                   
                   fileWriters.add(node);
                   index.put(node, id++);
                   if (staticApp) {
                       steady.put(node, ssg.getMult(node, false));
                       //we want to skip the first steady state also
                       skip.put(node, 
                               ssg.getMult(node, true) + ssg.getMult(node, false));
                       totalSteadyItems += ssg.getMult(node, false);
                   }
                   else {
                       //dynamic app, just use the ssoutputs option
                       steady.put(node, KjcOptions.ssoutputs);
                       skip.put(node, 2 * KjcOptions.ssoutputs);
                       totalSteadyItems += KjcOptions.ssoutputs;
                   }
               }
           }
       }
       assert fileWriters.size() > 0 : "Error in number gathering: no file writer";
   }
}