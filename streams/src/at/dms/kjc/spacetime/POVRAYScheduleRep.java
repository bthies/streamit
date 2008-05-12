/**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.*;
import java.util.*;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Slice;

/**
 * This class will create a 3D representation of the Space-Time 
 * schedule to be rendered by the POVRAY rendering program installed
 * on the CAG farms.  Thanks to Bill for pointing me to this program. 
 * 
 * @author mgordon
 *
 */
public class POVRAYScheduleRep {
    /** The file we are creating */
    private FileWriter fw;
    /** The maximum height of a tile or else it will be off the screen */
    private static double MAX_HEIGHT = 3.0;
    /** the multipler of scaling the height of the representation */
    private double heightScale; 
    /** a unique ID used to label the color assigned to each trace */
    private static int colorID = 0;
    /** Map the trace to the name of the color created for it */
    private HashMap<Slice, String> colorName;
    
    private ScheduleModel scheduleModel;
    private SpaceTimeSchedule spaceTime;
    private Layout<RawTile> layout;
    
    /**
     * 
     * @param spaceTime
     * @param file
     */
    public POVRAYScheduleRep(SpaceTimeSchedule spaceTime, Layout<RawTile> layout, String file) {
        this.spaceTime = spaceTime;
        this.layout = layout;
        
        colorName = new HashMap<Slice, String>();
        
        //get the work estimation model for the schedule 
        scheduleModel = new ScheduleModel(spaceTime, layout, 
                spaceTime.getScheduleList());
        //create the model
        if (KjcOptions.noswpipe)
            scheduleModel.createModelNoSWPipe();
        else 
            scheduleModel.createModel();
        
        try {
            fw = new FileWriter(file);
        }
        catch (Exception e) {
            System.err.println("Error opening " + file + " for POVRAY file.");
            e.printStackTrace();
        }
    }
    
    /**
     * 
     *
     */
    public void create() {
        try {
            calculateHeightScale();
            setupScene();
            setupColors(spaceTime);
            for (int i = 0; i < spaceTime.getSlicer().getSliceGraph().length; i++) {
                
                createSliceShape(spaceTime.getSlicer().getSliceGraph()[i], layout);
            }
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Error creating POVRAY file!");
            e.printStackTrace();
        }
    }

    /**
     * We scale everything by the tile that does the most work, so
     * calculate the scaling factor.
     */
    private void calculateHeightScale() {
        System.out.println(MAX_HEIGHT  + " / " + (double)scheduleModel.getBottleNeckCost());
        heightScale = MAX_HEIGHT / (double)scheduleModel.getBottleNeckCost();
    }
    
    /**
     * 
     * @param slice
     * @param layout
     * @throws Exception
     */
    private void createSliceShape(Slice slice, Layout<RawTile> layout) 
            throws Exception  {
        //don't do anything for file reader and writer traces
        if (spaceTime.getSlicer().isIO(slice))
            return;
        
        fw.write("//-------------------------------------------\n");
        fw.write("// Objects for " + slice + "\n");
        for (FilterSliceNode filter : slice.getFilterNodes()) {
            RawTile tile = layout.getComputeNode(filter);
            int lowerLeftX = getLowerLeftX(tile);
            int lowerLeftY = getLowerLeftY(tile); 
            /* double lowerLeftZ = (double)scheduleModel.getFilterStart(filter) *
                heightScale; */
            double upperLeftZ = (double)scheduleModel.getFilterEnd(filter) *
                heightScale;
            double lowerLeftZ =  
                (double)(scheduleModel.getFilterEnd(filter) - 
                        (double)spaceTime.getSlicer().getFilterWorkSteadyMult(filter));
                                    
            assert lowerLeftZ >= (double)scheduleModel.getFilterStart(filter) : 
		filter + " " + lowerLeftZ + " " + scheduleModel.getFilterStart(filter);
            
            lowerLeftZ = lowerLeftZ * heightScale;
            
            fw.write("box {\n");
            //create the coordinates of box in the following form:
            //2 points: <lower_left>, <upper_right>
            //where each point has <L/R, height, top/bottom> or <X, Z, Y>
            fw.write("  <" + lowerLeftX + ", " + lowerLeftZ + ", " + lowerLeftY + ">, " +
                    "<" + (lowerLeftX + 1) + ", " + (upperLeftZ) + ", " + (lowerLeftY + 1) + ">\n");
            
            fw.write("  pigment {" + colorName.get(slice) + "}\n");
            fw.write("}\n");
        }
    }
    
    /**
     * For each slice, randomly generate a color that will be used to represent it.
     * 
     * @param spaceTime The space time schedule.
     * @throws Exception FileWriter crap.
     */
    private void setupColors(SpaceTimeSchedule spaceTime) throws Exception {
        Random random = new Random(17); 
        fw.write("//-------------------------------------------\n");
        fw.write("//Setup colors for the slices\n");
        for (int i = 0; i < spaceTime.getSlicer().getSliceGraph().length; i++) {
            colorName.put(spaceTime.getSlicer().getSliceGraph()[i], 
                    "color" + colorID++);
            fw.write("#declare " + colorName.get(spaceTime.getSlicer().getSliceGraph()[i]) +
                    " = rgb<" + random.nextDouble() + ", " + random.nextDouble() + ", " +
                    random.nextDouble() + ">;\n");
        }
    }
    
    private int getLowerLeftX(RawTile tile) {
        return tile.getX() - (tile.rawChip.getXSize() / 2);
    }
    
    private int getLowerLeftY(RawTile tile) {
        return -(tile.getY()) + ((tile.rawChip.getYSize() / 2) - 1);
    }
    
    /**
     * Setup the raw tile base, the camera, and the light sources.
     * 
     * @throws Exception FileWriter crap.
     */
    private void setupScene() throws Exception {
        fw.write("#version 3.5;\n");
        fw.write("\n");
        fw.write("#include \"stdinc.inc\"\n");
        fw.write("\n");
        fw.write("#declare slice_something = rgb<.5, .34, .8>;\n");
        fw.write("\n");
        fw.write("//-------------------------------------------\n");
        fw.write("\n");
        fw.write("global_settings {\n");
        fw.write("   assumed_gamma 1\n");
        fw.write("   max_trace_level 10\n");
        fw.write("}\n");
        fw.write("\n");
        fw.write("#default { finish { \n");
        fw.write("             diffuse 0.7\n");
        fw.write("             specular 1 roughness 0.0035 metallic\n");
        fw.write("           }\n");
        fw.write("         }\n");
        fw.write("\n");
        fw.write("camera {\n");
        fw.write("   location <-2.0, 5.5,-8.5>\n");
        fw.write("   up y  right x*image_width/image_height\n");
        fw.write("   angle 45\n");
        fw.write("   look_at <0.0, 0.7, 0.0>\n");
        fw.write("   rotate   <0,-360*(clock+0.10),0>\n");
        fw.write("}\n");
        fw.write("\n");
        fw.write("light_source {<-150, 200,-100>, color 1.2 * <1.0, 1.0, 0.6>} // yellowish light from the left\n");
        fw.write("light_source {< 150, 100,-100>, color 1.1 * <0.6, 0.6, 1.0>} // blueish light from the right\n");
        fw.write("\n");
        fw.write("//-------------------------------------------\n");
        fw.write("// represent raw\n");
        fw.write("\n");
        fw.write("box {\n");
        fw.write("   <-2,0,-2>, <2,0,2>\n");
        fw.write("   texture {\n");
        fw.write("      pigment {checker color <0.1, 0.3, 0.4>, color <0.2, 0.5, 0.7>}\n");
        fw.write("      finish {diffuse 0.7 reflection 0.2}\n");
        fw.write("   }\n");
        fw.write("}\n");
        fw.write("\n");
    }
}
