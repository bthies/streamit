package streamit.eclipse.grapheditor.editor.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.graph.CellMapper;
import org.jgraph.graph.CellView;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;

import streamit.eclipse.grapheditor.editor.utils.MathExtensions;

/**
 * <h1>GEM Layout Algorithm</h1>
 * <p>
 * Based on the work of Arne Frick, Andreas Ludwig, Heiko Mehldau: 
 * "A Fast Adaptive Layout Algorithm for Undirected Graphs"; 
 * Extended Abstract and System Demonstration; Faculty of Informatik of 
 * the University Karlsruhe; 1994
 * <p>
 * This Algorithm works by giving every cell a position and a temperature.
 * Then for every cell forces are computed. Every other cell repulses the
 * actual calculated cell away. On the other hand, cells, connected by edges
 * are attracted, until a minimum distance is reached. The result of this 
 * forces is a move of the position of the actual cell in the direction of
 * the force and with the length of the temperature. Then the temperature
 * will be decreased, if the last impulse and the current impulse looks like a
 * rotation or a oscillation. this is done for every cell until the temperature
 * of all cells or the average of the temperature of every cell is until a
 * given minimum value or a maximum of rounds is reached.
 * @author winkler
 */

public class GEMLayoutAlgorithm implements LayoutAlgorithm, GraphModelListener {
    
    /**
     * Key used on every cell. This key indicates that in the cell are 
     * attributes stored by this algorithm. The algorithm itself never asks for
     * this key. This is for others developers only, using this algorithm, to
     * find out, where sometimes approaching attributes come from.
     */     
    public final static String KEY_CAPTION         = "GEM-TEMPORARY-DATA";
    
    /**
     * Key used on every cell. Under this key every cell stores temporary the
     * temperature of itself. Temperature is a indicator how far a cell can move
     * and all temperatures together indicating how long the algorithm will run.
     */
    public final static String KEY_TEMPERATURE     = "Temperature";
    
    /**
     * Key used on every cell. Under this key every cell stores temporary the
     * current force impulse affecting the cell.
     */
    public final static String KEY_CURRENT_IMPULSE = "Current_Impulse";
    
    /**
     * Key used on every cell. Under this key every cell stores temporary the
     * last impulse. This is the value of the previous 
     * {@link #KEY_CURRENT_IMPULSE}.
     */
    public final static String KEY_LAST_IMPULSE    = "Last_Impulse";
    
    /**
     * Key used on every cell. Under this key every cell stores the temporary  
     * position on the display, while the calculation is running. This makes
     * the algorithm faster, than asking the Cell everytime for its position.
     * So the algorithm can anytime be canceled, whithout changing something. 
     */
    public final static String KEY_POSITION        = "Position";

    /**
     * Key used on every cell. Under this key every cell stores the temporary
     * skew gauge. This value is for punish rotations of the cells.
     */    
    public final static String KEY_SKEWGAUGE       = "Skew_Gauge";
    
    /**
     * Key used on every Cell. Under this key every cell stores the cells,
     * that are only one edge away from it. The relatives are stored, after the
     * first time, they are desired and calculated by 
     * {@link #getRelatives(CellView)}.
     */
    public final static String KEY_RELATIVES       = "Relatives";
    
    /**
     * Key used on every Cell. This indicates a weight for the number of edges
     * received by {@link #getNodeWeight(CellView)}
     */
    public final static String KEY_MASSINDEX       = "Mass_Index";
    
    /**
     * Key used only with clusters. Under this key a cluster has an ArrayList.
     * This list is filled with the clustered vertices.
     * @see #clusterGraph()
     * @see #moveVerticeToCluster(CellView,CellView)
     */
    public final static String KEY_CLUSTERED_VERTICES = "Clustered Vertices";
    
    /**
     * Key used only with clusters. Under this key vertices have the cluster
     * they belong to.
     * @see #clusterGraph()
     * @see #moveVerticeToCluster(CellView,CellView)
     */
    public final static String KEY_CLUSTER            = "Cluster";
    
    /**
     * Key used only with clusters. Under this key a cluster has a boolean value
     * indicating that this vertice is a cluster (clusters are 
     * VertexView-instances like every other cell).
     * @see #clusterGraph() 
     * @see #isCluster()
     */
    public final static String KEY_IS_CLUSTER         = "is Cluster";
    /**
     * Key used only with clusters. Under this key every cluster has a position,
     * which represents the position of the cluster, right after the clustering
     * process. After the layout update process is finished, the move, resulting
     * of subtracting the position under {@link #KEY_POSITION} from the 
     * position under this value, will be performed to all vertices in the 
     * cluster. By holding the initial position here clustering becomes 
     * possible.
     * 
     * @see #clusterGraph()
     * @see #declusterGraph()
     */
    public final static String KEY_CLUSTER_INIT_POSITION = "initial Position of the Cluster";
    
    
    /** 
     * List of all nodes in the graph.
     */
    private ArrayList  cellList; 
    
    /** 
     * List of all nodes the algorithm should be done for. 
     */
    private ArrayList  applyCellList;
    
    /** 
     * List of all edges in the graph. This is only needed for the optimization
     * Algorithm.
     */
    private ArrayList  edgeList;
    
    
    /** 
     * needed for comperation with other double values if they are 0.0.
     */
    private double equalsNull = 0.00000000000000001;

    /** 
     * starting value for the temperatures of the cells 
     */
    private double initTemperature;
    
    /** 
     * if the temperature of all cells or the average of the temperatures of 
     * all cells is below this value, the algorithm stops
     */
    private double minTemperature;
    
    /** 
     * temperature will never be over this value 
     */
    private double maxTemperature;
    
    /** 
     * the length of the Edges in Pixel, the algorithm tries to keep
     */
    private double prefEdgeLength;
    
    /** 
     * the strength of the gravitation force, directed to the barycenter of the
     * graph, added to all cells. 
     */
    private double gravitation;
    
    /** 
     * length of a force vector with a random direction, added to all cells. 
     */
    private double randomImpulseRange;
    
    /** 
     * opening angle in radiant, that detects oscillations 
     */
    private double alphaOsc;
     
    /** 
     * opening angle in radiant, that detects rotations 
     */
    private double alphaRot;
     
    /** 
     * penalty value for a detected oscillation
     */
    private double sigmaOsc; 
    
    /** 
     * penalty value for a detected rotation 
     */
    private double sigmaRot; 
        
    /**
     * number of rounds until the algorithm will break. This value is 
     * precalculated to a aproximativ value of 4 times the count of Cells in
     * {@link #applyCellList}
     */
    private int    maxRounds;
    
    /**
     * counts the rounds
     */
    private int    countRounds;
    
    /**
     * If the pathlength between an inserted cell and an allready layouted cell
     * is below this value, the allready layouted cell will be layouted again.
     */
    private int    recursionDepth;
    
    /**
     * Describes the distance around a cell, that will be whatched for other
     * cells, intersecting this area. If another cell intersects, a force will
     * be added to the cell, that pushes it away.
     */
    private double overlapDetectWidth;
    
    /**
     * Describes a distance the algorithm tries to keep, when he detects a 
     * overlapping cell. 
     */
    private double overlapPrefDistance;
    
    /**
     * switches the feature to whatch for overlapping on/off
     */
    private boolean avoidOverlapping;
    
    /**
     * describes, what method will be taken, when cells are inserted. Posible
     * values are 
     * {@link GEMLayoutAlgorithm#KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY 
     * KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY} and
     * {@link GEMLayoutAlgorithm#KEY_LAYOUT_UPDATE_METHOD_PERIMETERS 
     * KEY_LAYOUT_UPDATE_METHOD_PERIMETERS}.
     */
    private String layoutUpdateMethod;

    /**
     * condition for method isFrozen(). decides whether the method returns true
     * when the average of all temperatures or all temperatures are below 
     * {@link #minTemperature}. 
     */
    private boolean shouldEndPerAverage;
    /**
     * condition for the method calculate(). decides whether the algorithm
     * is computed for the cellViews every time in the same sequence
     * or if the cellViews are computed every time in a random sequence.
     */
    private boolean shouldComputePermutation;

    /**
     * Switches the skill of the algorithm to perform the layout update process
     */    
    private boolean isActive  = true;
    
    /**
     * Checks if the algorithm is currently running. If this is the case, no
     * GraphModelEvent will be computed and no new run can be initiated.
     */
    private boolean isRunning = false;
    
    /**
     * a reference to the instance of jgraph
     */
    private JGraph   jgraph;
    
    /**
     * the configuration of this algorithm
     */
    protected Properties config;

    /*
     * shows the progress in performing the algorithm on the graphcells
     */    
    private ProgressDialog dlgProgress =
        new ProgressDialog((Frame) null, "Progress:", false);
        
    /* 
     * to determine a somehow correct percentage value for the progress dialog
     */
    private int[] phaseLength = new int[] { 2, 80, 100};//phase i goes up to
    
    /*
     * to detemine a correct labeled progress dialog
     */
    private String[] phaseName = new String[] { "initialization", 
                                               "performing calculation", 
                                               "setting new Vertice Values"};
    
    /**
     * to identify the different phases of the algorithm for the progress dialog
     */
    private final static int PHASE_INITIALISATION = 0;
    /**
     * to identify the different phases of the algorithm for the progress dialog
     */
    private final static int PHASE_CALCULATION    = 1;
    /**
     * to identify the different phases of the algorithm for the progress dialog
     */
    private final static int PHASE_END            = 2;
    
    /**
     * to identify for the method {@link #loadRuntimeValues(int)}, that the 
     * algorithm wants to perform a new run
     */
    protected final static int VALUES_PUR = 0;
    /**
     * to identify for the method {@link #loadRuntimeValues(int)}, that the 
     * algorithm wants to perform a layout update
     */
    protected final static int VALUES_INC = 1;
    
    /**
     * algorithm used for optimizing the result of this algorithm
     */
    private AnnealingLayoutAlgorithm optimizationAlgorithm;
    
    /**
     * switches the usage of the optimizing algorithm
     */
    private boolean        useOptimizeAlgorithm;
    
    /**
     * configuration of the optimizing algorithm
     */
    private Properties      optimizationAlgorithmConfig;
    /**
     * Switches clustering for the layout update process on/off
     */
    private boolean         isClusteringEnabled;
    /**
     * The initial temperature for clusters. It is recommended, that this value
     * is lower than {@link #initTemperature} to get a good looking layout
     */
    private double          clusterInitTemperature;
    /**
     * Scales forces, that are effecting clusters. It is recommendet to take
     * a value between 1.0 and 0.0. This garanties, that clusters move slower
     * than other cells. That rises the chance of getting a good looking layout
     * after the calculation.
     */
    private double          clusterForceScalingFactor;
    /**
     * Effects, how many clusters are created, when the layout update process
     * starts. This affects the initial number of clusters, which is the number
     * of cells available minus the number of cells to layout. The result of
     * that term is divided by this factor, to get the maximum number of 
     * clusters. After this calculation, the clustering algorithm tries to
     * minimize the number of clusters, so there might be less clusters than 
     * the maximum number.
     */
    private double          clusteringFactor;
    
    /**
     * The initial size for the layout update method perimeter. This describes
     * a radius around an inserted cell. Every other inserted cell in this
     * radius increases the radius by {@link #perimeterSizeInc}. After finishing
     * increasing the radius, every cell, from the cells, that are already 
     * layouted, in the radius is added to the list of cells, that'll gain a
     * new position during the layout update process. This should bring up
     * the behaviour, that the previous layouted cells make space for the layout 
     * of the inserted cells.
     */
    private double perimeterInitSize;
    
    /**
     * Inserted cells whithin a radius of {@link #perimeterInitSize} around
     * a inserted cell are counted. After counting the inserted cells around
     * a inserted cell, the initial radius is increased by this increase value
     * times the number of inserted cells around the inserted cell. Every
     * previous layouted cell in the resulting radius around the inserted cell
     * is going to be layouted again.
     */
    private double perimeterSizeInc;
    
    private boolean isDebugging = false;
        
/******************************************************************************/
/**
 * Constructs a new GEM Layout Algorithm.
 */
    public GEMLayoutAlgorithm(AnnealingLayoutAlgorithm optimizer){
        cellList      = new ArrayList();
        applyCellList = new ArrayList();
        edgeList      = new ArrayList();
        optimizationAlgorithm = optimizer;
    }

/******************************************************************************/
	/**
     * Starts the Calculation of a new layout with the GEM-Algorithm
     * @param jgraph View of Graphnodes
     * @param graphCells List of all nodes the layout should be calculated for
     * @param configuration contains the initial values for the Algorithm
     * @see #initialize()
     * @see #calculate()
	 * @see de.fzi.echidna.layout.LayoutAlgorithm#perform(MyJGraph,List,Properties)
	 */
	public void perform(JGraph    graph,
                         boolean   applyToAll,
                         Properties configuration) {
                            
        isRunning = true;
                            
        jgraph = graph;
        config = configuration;
        
        jgraph.getModel().addGraphModelListener(this);
        
                         
        cellList      = new ArrayList();
        applyCellList = new ArrayList();
        
        //extracting the nodes from jgraph, the algorithm should be performed on
        getNodes(jgraph,applyToAll);
        
        
        loadRuntimeValues(VALUES_PUR);

        long starttime = System.currentTimeMillis();
        //ALGORITHM START
        boolean isCanceled = initialize();//initializes algorithm; sets the startvalues in cells
                
        if( !isCanceled )
             isCanceled = calculate();//performs the algorithm on the cells
        //ALGORITHM END

        if( !isCanceled && useOptimizeAlgorithm )
            isCanceled = optimizationAlgorithm.performOptimization(applyCellList,cellList,edgeList,optimizationAlgorithmConfig,dlgProgress);

        if( !isCanceled )
            correctCoordinates();
                                
        //sets the calculated data into cellView's bounds if not canceled
        if( !isCanceled ) 
            isCanceled = setNewCoordinates(jgraph);
                    
        //removes the temporary data, stored by the algorithm, from the nodes
        removeTemporaryLayoutDataFromCells();

        dlgProgress.setMessage("Algorithm finished");
        
        //prepares for next calculation (maybe useless, because instance of 
        //this algorithm might be thrown away)        
        dlgProgress.setVisible(false);
        
        isRunning = false;
	}
    
/******************************************************************************/
/**
 * Loads the actual desired values from the {@link #config configuration} to
 * the fields, where they are used later.
 * 
 * @param valueID {@link #VALUES_PUR} for a normal run or {@link #VALUES_INC}
 * for a layout update process.
 */
    protected void loadRuntimeValues(int valueID){
        
        maxRounds = applyCellList.size() * 4;//estimated value; reached rarely
        
        countRounds = 0;//start value; counts the rounds in calculate()

        isActive = isTrue((String)
                     config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_ENABLED));
        
        recursionDepth = Integer.parseInt((String)config.get(
                                  GEMLayoutController.KEY_LAYOUT_UPDATE_DEPTH));
                                  
        layoutUpdateMethod = (String) 
                       config.get(GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD);
            
        if( valueID == VALUES_PUR ){
        
            initTemperature          = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_INIT_TEMPERATURE));
            minTemperature           = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_MIN_TEMPERATURE));
            maxTemperature           = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_MAX_TEMPERATURE));
            prefEdgeLength           = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_PREF_EDGE_LENGTH));
            gravitation              = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_GRAVITATION));
            randomImpulseRange       = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_RANDOM_IMPULSE_RANGE));        
            overlapDetectWidth       = Double.parseDouble((String)config.get(
                          GEMLayoutController.KEY_OVERLAPPING_DETECTION_WIDTH));
            overlapPrefDistance      = Double.parseDouble((String)config.get(
                            GEMLayoutController.KEY_OVERLAPPING_PREF_DISTANCE));
            shouldEndPerAverage      = isTrue((String)config.get(
                               GEMLayoutController.KEY_COMPUTE_PERMUTATION));
            shouldComputePermutation = isTrue((String)config.get(
                               GEMLayoutController.KEY_END_CONDITION_AVERAGE));
            avoidOverlapping         = isTrue((String)config.get(
                               GEMLayoutController.KEY_AVOID_OVERLAPPING));
            alphaOsc = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_ALPHA_OSC));
            alphaRot = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_ALPHA_ROT));
            sigmaOsc = Double.parseDouble((String)config.get(
                               GEMLayoutController.KEY_SIGMA_OSC));
            sigmaRot = Double.parseDouble((String)config.get(    //gets 1/x
                               GEMLayoutController.KEY_SIGMA_ROT));
            useOptimizeAlgorithm = isTrue((String)config.get(
                           GEMLayoutController.KEY_OPTIMIZE_ALGORITHM_ENABLED));
            optimizationAlgorithmConfig = (Properties) config.get(
                             GEMLayoutController.KEY_OPTIMIZE_ALGORITHM_CONFIG);

            
        }
        else if( valueID == VALUES_INC ){
            
            initTemperature          = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE));
            minTemperature           = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE));
            maxTemperature           = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_MAX_TEMPERATURE));
            prefEdgeLength           = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH));
            gravitation              = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_GRAVITATION));
            randomImpulseRange       = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE));        
            overlapDetectWidth       = Double.parseDouble((String)config.get(
            GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH));
            overlapPrefDistance      = Double.parseDouble((String)config.get(
              GEMLayoutController.KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE));
            shouldEndPerAverage      = isTrue((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION));
            shouldComputePermutation = isTrue((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE));
            avoidOverlapping         = isTrue((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING));
                                    
            alphaOsc = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_OSC));
            alphaRot = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_ALPHA_ROT));
            sigmaOsc = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_OSC));
            sigmaRot = Double.parseDouble((String)config.get(    //gets 1/x
                 GEMLayoutController.KEY_LAYOUT_UPDATE_SIGMA_ROT));
            useOptimizeAlgorithm = isTrue((String)config.get(
             GEMLayoutController.KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED));
            optimizationAlgorithmConfig = (Properties) config.get(
               GEMLayoutController.KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG);
            perimeterInitSize = Double.parseDouble((String)config.get(
             GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE));
            perimeterSizeInc = Double.parseDouble((String)config.get(
              GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC));
            isClusteringEnabled = isTrue((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED));
            clusterInitTemperature = Double.parseDouble((String)config.get(
            GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE));
            clusterForceScalingFactor = Double.parseDouble((String)config.get(
        GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR));
            clusteringFactor = Double.parseDouble((String)config.get(
                 GEMLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR));
        }
        //with that line sigmaRot will be 1/(x*cellCount) with x is configurable
        sigmaRot *= 1.0 / (double) (applyCellList.size() == 0 ? 1 : applyCellList.size());
    }

/******************************************************************************/
/**
 * Helping method. Transforms a String, containing only the characters "true" or
 * "false", regardless if upper or lower case, into a boolean value.
 * 
 * @param boolValue String containing a boolean value
 * @return boolean value represented by the given string
 */
    protected boolean isTrue(String boolValue){
        if( boolValue != null ){
            if( "TRUE".equals(boolValue.toUpperCase()) ){
                return true;
            }
            else if( "FALSE".equals(boolValue.toUpperCase()) ){
                return false;
            }
        }
        return false;        
    }

/******************************************************************************/    
/**
 * Extracts the Cells from JGraph and fills {@link #applyCellList}, 
 * {@link #cellList} and {@link #edgeList}. If applyToAll is 
 * <b><code>false</code></b> only in jgraph selected cells are added to
 * {@link #applyCellList} else all cells are added.
 * 
 * @param jgraph actual instance of jgraph
 * @param applyToAll determines, if this algorithm should be performed on all 
 * cells or only on the selected. 
 */
    private void getNodes(JGraph jgraph, boolean applyToAll){
        
        Object[] cells = jgraph.getRoots();
        Object[] selectedCells = jgraph.getSelectionCells();
        
        CellView[] view = jgraph.getGraphLayoutCache().getMapping(cells,false);
        CellView[] selectedView = jgraph.getGraphLayoutCache().getMapping(
                                                           selectedCells,false);
        
        for (int i = 0; i < view.length; i++)
            if (view[i] instanceof VertexView){
                cellList.add(view[i]);
                if( applyToAll )
                    applyCellList.add(view[i]);
                
            }
            else if( view[i] instanceof EdgeView ){
                edgeList.add(view[i]);
            }
        if( !applyToAll )
            for( int i = 0; i < selectedView.length; i++ )
                if( selectedView[i] instanceof VertexView )
                    applyCellList.add(selectedView[i]);
    }
    
/******************************************************************************/
/**
 * Sets the initial Values, gained from the {@link #config configuration} 
 * into the Cells.
 * 
 * @return Because the progress dialog is allready visible during the 
 * initialisation, <b><code>true</code><b> is returned when cancel is pressed
 * on it.
 */
    private boolean initialize(){
        
        dlgProgress.setVisible(true);
        dlgProgress.setMessage(phaseName[PHASE_INITIALISATION]);
        
        int length = cellList.size();
        
        for( int i = 0; i < length; i++ ){
            
            CellView view = (CellView) cellList.get(i);
            
            initializeVertice(view);
            
            if( updateProgressDialog(PHASE_INITIALISATION,
                                     i,
                                     length) )
                                     
                return true; // canceled        
        }
        
        for( int i = 0; i < applyCellList.size(); i++ )
            computeLastImpulse( (CellView) applyCellList.get(i) );
            
        return false;    //not canceled
    }
    
/******************************************************************************/
/**
 * Sets the initial values for one Cell.
 * 
 * @param view Cell, the initial values should be set for.
 */
    private void initializeVertice(CellView view){
            Map attributes = view.getAttributes();
            if( attributes == null )
                attributes = new Hashtable();
                
            attributes.put(KEY_CAPTION,KEY_CAPTION);
            
            initPosition(view);                       

            if( isCluster(view) ){
                attributes.put(KEY_TEMPERATURE,
                               new Double(clusterInitTemperature));
            }
            else attributes.put(KEY_TEMPERATURE,
                                new Double(initTemperature));
            
            attributes.put(KEY_SKEWGAUGE,  new Double(0.0));
            attributes.put(KEY_CURRENT_IMPULSE,new Point2D.Double());
            attributes.put(KEY_LAST_IMPULSE   ,new Point2D.Double());
    }

/******************************************************************************/
/**
 * Runs the algorithm. First a running sequence is initialised. If 
 * {@link #shouldComputePermuation} is <b><code>true</code><b> then a new
 * permutation is computed for every round, else a single determined sequence is
 * established. After this for every Cell a current impulse is calculated, 
 * position and temperature is updated. This is done, until the graph is frozen,
 * a maximum on rounds is reached or cancel on the progress dialog is pressed.
 * 
 * @return <b><code>true</code></b> when cancel on the progress dialog is 
 * pressed
 * @see #computeCurrentImpulse(CellView)
 * @see #createPermutation(int)
 * @see #isFrozen()
 * @see #updatePosAndTemp(CellView)
 */
    private boolean calculate(){
        
        int length = applyCellList.size();
        int[] sequence = new int[length];
        boolean isCanceled = false;
        
        dlgProgress.setMessage(phaseName[PHASE_CALCULATION]);

        
        //case no permutation is desired, the series is computed one time only        
        if( !shouldComputePermutation ) //else is in the loop below
            for( int i = 0; i < length; i++ )
                sequence[i] = i;
        
        while( !isFrozen() && countRounds <= maxRounds && (!isCanceled) ){

            //case permutation is desired, it's calculated every round
            if( shouldComputePermutation ) 
                sequence = createPermutation(length);

            //loop over all nodes (order is in sequence)
            for( int i = 0; i < sequence.length; i++ ){
                CellView view = (CellView) applyCellList.get(sequence[i]);

                computeCurrentImpulse(view); //computes direction of impulse
                updatePosAndTemp(view);  //computes new position and temperature                
                
                if( updateProgressDialog(PHASE_CALCULATION,
                                         (countRounds*sequence.length)+i,
                                         maxRounds*sequence.length ) )
                    return true;
            }
            countRounds++;
        
        }
        return false;
    }
/******************************************************************************/
/**
 * Helps updating the progress dialog.
 * 
 * @param phase Identifies the phase, the algorithmis doing.
 * @param round current round, the algorithm is performing
 * @param maxRound maximum number of rounds, the algorithm could perform
 */
    private boolean updateProgressDialog(int phase, int round, int maxRound){
        
        int lowValue = 0;
        if( phase != 0 ) 
            lowValue = phaseLength[phase-1];
        
        int maxValue = phaseLength[phase];
        
        int width = maxValue - lowValue;
        
        int value = lowValue+(int)(width*((double)round/(double)maxRound));
        
        dlgProgress.setValue(value);
        
        return dlgProgress.isCanceled();
    }
    
/******************************************************************************/
/**
 * Calculates the current impulse for the given cell.
 * 
 * @param view Cell, the current impulse should be calculated
 * @see #computeImpulse(CellView)
 */    
    private void computeCurrentImpulse(CellView view){
        
        //gets the impulse for view
        Point2D.Double impulse = computeImpulse(view);
        
        //set result into node
        view.getAttributes().put(KEY_CURRENT_IMPULSE,impulse);
    }

/******************************************************************************/
/**
 * Calculates the last impulse for the given cell. This is only nesessary while
 * initializing the cells.
 * 
 * @param view Cell, the last impulse should be calculated
 * @see #computeImpulse(CellView)
 */
    private void computeLastImpulse(CellView view){
        
        //gets the impulse for view
        Point2D.Double impulse = computeImpulse(view);
        
        //set result into node
        view.getAttributes().put(KEY_LAST_IMPULSE,impulse);
    }

/******************************************************************************/
/**
 * Computes an Impulse representing a Force affecting the position of the given 
 * Cell. This impulse consists of a attracting force, pulling the cell to the
 * barycenter of all cells, a pulse with user defined length and random 
 * direction, a force repulsing cells from each other, a force attracting 
 * connected cells together, and, as a additional feature, a force, repulsing 
 * the current cell from overlapping cells.
 * 
 * @param view Cell, the impulse should be computed
 * @return impulse, transformed in a Point2D.Double-Instance.  
 */
    private Point2D.Double computeImpulse(CellView view){
        Point2D.Double impulse = new Point2D.Double();
        Point2D.Double pos     = getPosition(view);
        boolean isCellACluster = isCluster(view);
        
        //the more edges a cell have, the heavier the cell is
        double massIndex = getNodeWeight(view);
            
        //gets the barycenter of all cells
        Point2D.Double barycenter = computeBarycenter(cellList);
        
        //attracting force from the barycenter to every cell
        Point2D.Double gravitationForce = new Point2D.Double(
                    (barycenter.getX() - pos.getX()) * gravitation * massIndex,
                    (barycenter.getY() - pos.getY()) * gravitation * massIndex);
        
        //random glitch is added to force
        Point2D.Double randomImpulse = getRandomVector(randomImpulseRange);
        
        //repulsive Forces
        //from all nodes
        ArrayList repulsiveForce = new ArrayList();
        for( int i = 0 ; i < cellList.size(); i++ ) 
            if( cellList.get(i) != view ){//all cells except the actual view
//                CellView uView = (CellView) cellList.get(i);
//                if( !isCluster(uView)){
                
                Point2D.Double uPos = getPosition(i,cellList);
                
                double deltaX = (double)(pos.getX() - uPos.getX());
                double deltaY = (double)(pos.getY() - uPos.getY());
/*                
                double sgnX = MathExtensions.sgn(deltaX);
                double sgnY = MathExtensions.sgn(deltaY);
                
                if( isCellACluster && isCluster(uView) ){
                    deltaX -= uView.getBounds().getWidth() / 2.0;
                    deltaY -= uView.getBounds().getHeight()/ 2.0;
                }
                if( isCellACluster ){
                    deltaX -= view.getBounds().getWidth() / 2.0;
                    deltaY -= view.getBounds().getHeight()/ 2.0;
                }
                
                if( sgnX != MathExtensions.sgn(deltaX) ){
                    deltaX *= sgnX;
                }
                if( sgnY != MathExtensions.sgn(deltaY) ){
                    deltaY *= sgnY;
                }
*/              
                                
                double absDelta = MathExtensions.abs(deltaX,deltaY);
                
                if( absDelta > equalsNull ){
                    repulsiveForce.add(new Point2D.Double(
                        deltaX * ((prefEdgeLength * prefEdgeLength) / (absDelta * absDelta)),
                        deltaY * ((prefEdgeLength * prefEdgeLength) / (absDelta * absDelta))));
                }
//                } 
            } 
                
        //attractive Forces:
        //from all nodes that have an edge with view
        ArrayList relatives = getRelativesFrom(cellList,view);
        ArrayList attractiveForce = new ArrayList(relatives.size());
        for( int i = 0; i < relatives.size(); i++ ){

//            CellView child = (CellView) relatives.get(i);
            
            Point2D.Double cPos = getPosition(i,relatives);
        
            double deltaX = (double)(pos.getX() - cPos.getX());
            double deltaY = (double)(pos.getY() - cPos.getY());
/*          
            double sgnX = MathExtensions.sgn(deltaX);
            double sgnY = MathExtensions.sgn(deltaY);
            
            if( isCellACluster && isCluster( child )){
                deltaX -= child.getBounds().getWidth() / 2.0;
                deltaY -= child.getBounds().getHeight()/ 2.0;
            }
            if( isCellACluster ){
                deltaX -= view.getBounds().getWidth() / 2.0;
                deltaY -= view.getBounds().getHeight()/ 2.0;
            }
            
            if( sgnX != MathExtensions.sgn(deltaX) )
                deltaX *= sgnX;
            if( sgnY != MathExtensions.sgn(deltaY) )
                deltaY *= sgnY;
*/        
            double absDelta = MathExtensions.abs(deltaX,deltaY);
            
            attractiveForce.add(new Point2D.Double(
                deltaX * (( absDelta * absDelta ) / ( prefEdgeLength * prefEdgeLength * massIndex )),
                deltaY * (( absDelta * absDelta ) / ( prefEdgeLength * prefEdgeLength * massIndex ))));
        }
        
        
        /* the next part is NOT part of the original algorithm */
        /* it adds a force if the actual cell overlapps another cell */
        ArrayList forcesByOverlapping = new ArrayList();
        
        if( avoidOverlapping ){
        
            Rectangle viewBounds = new Rectangle(
                                             (int)pos.x,
                                             (int)pos.y,
                                             (int)view.getBounds().getWidth(),
                                             (int)view.getBounds().getHeight());
            Rectangle viewBorder = new Rectangle(
                        (int)(viewBounds.getX()-overlapDetectWidth),
                        (int)(viewBounds.getY()-overlapDetectWidth),
                        (int)(viewBounds.getWidth()+(2.0*overlapDetectWidth)),
                        (int)(viewBounds.getHeight()+(2.0*overlapDetectWidth)));
                                             
            for( int i = 0; i < cellList.size(); i++ ){
            
                Point2D.Double uPos = getPosition(i,cellList);            
                Rectangle uBounds = new Rectangle(
                      (int)uPos.x,
                      (int)uPos.y,
                      (int)((CellView)cellList.get(i)).getBounds().getWidth(),
                      (int)((CellView)cellList.get(i)).getBounds().getHeight());
            
                if( view != cellList.get(i) &&
                    viewBorder.intersects(uBounds) ){
                
                
                    Dimension viewSize = viewBounds.getSize();
                    Dimension uSize    = uBounds.getSize();
                
                    double minDistance = 
                        (Math.max(viewSize.getWidth(),viewSize.getHeight())/2.0)+
                        (Math.max(uSize.getWidth(),uSize.getHeight())/2.0)+overlapPrefDistance;
                
                    double deltaX = (double)(pos.x - uPos.x);
                    double deltaY = (double)(pos.y - uPos.y);
/*                  
                    if( isCellACluster ){
                        deltaX -= view.getBounds().getWidth() / 2.0;
                        deltaY -= view.getBounds().getHeight()/ 2.0;
                    }
                    if( isCluster((CellView)cellList.get(i))){
                        deltaX -= ((CellView)cellList.get(i)).getBounds().getWidth() / 2.0;
                        deltaY -= ((CellView)cellList.get(i)).getBounds().getHeight()/ 2.0;
                    }*/
                
                    if( deltaX < equalsNull && deltaX >= 0.0 ){
                        deltaX = equalsNull;
                    }
                    else if( deltaX > -equalsNull && deltaX <= 0.0 ){
                        deltaX = -equalsNull;
                    }
                    if( deltaY < equalsNull && deltaY >= 0.0 ){
                        deltaY = equalsNull;
                    }
                    else if( deltaY > -equalsNull && deltaY <= 0.0 ){
                        deltaY = -equalsNull;
                    }
                
                
                    double absDelta = MathExtensions.abs(deltaX,deltaY);
                
                    Point2D.Double force = new Point2D.Double(
                          deltaX*(minDistance*minDistance)/(absDelta*absDelta),
                          deltaY*(minDistance*minDistance)/(absDelta*absDelta));
//                System.out.println("Overlapping Nodes: ("+pos.x+"|"+pos.y+") and ("+uPos.x+"|"+uPos.y+") -> Distance = "+absDelta+" -> Force = "+force);
                    forcesByOverlapping.add(force);
                }
            }
        }
        
        //for classes that extend this algorithm
        ArrayList additionalForce = getAdditionalForces((VertexView)view);
        
        //adding the forces
        impulse = add(impulse,gravitationForce);
        impulse = add(impulse,randomImpulse);
        
        for( int i = 0; i < repulsiveForce.size(); i++ )
            impulse = add(impulse,(Point2D.Double)  repulsiveForce.get(i) );
        for( int i = 0; i < attractiveForce.size(); i++ )
            impulse = sub(impulse,(Point2D.Double) attractiveForce.get(i) );
        for( int i = 0; i < forcesByOverlapping.size(); i++ )
            impulse = add(impulse,(Point2D.Double) forcesByOverlapping.get(i));
        for( int i = 0; i < additionalForce.size(); i++ )
            impulse = add(impulse,(Point2D.Double) additionalForce.get(i) );
        
        return impulse;            
    }
    
/******************************************************************************/
/**
 * Updating the position of the given cell, by taking the direction of the
 * current impulse and the length of the temperature of the cell. After this
 * temperature will fall, when the last impulse and the current impulse are
 * part of a rotation or a oscillation, temperature of the cell will be 
 * decreased.
 * 
 * @param view Cell that should be updated
 */
    private void updatePosAndTemp(CellView view){
        
        Point2D.Double impulse     = (Point2D.Double) view.getAttributes().get(KEY_CURRENT_IMPULSE);
        Point2D.Double lastImpulse = (Point2D.Double) view.getAttributes().get(KEY_LAST_IMPULSE);
        Point2D.Double position    = getPosition(view);
        double localTemperature = ((Double) view.getAttributes().get(KEY_TEMPERATURE)).doubleValue();
        double skewGauge        = ((Double) view.getAttributes().get(KEY_SKEWGAUGE)).doubleValue();
        double absImpulse     = MathExtensions.abs(impulse);
        double absLastImpulse = MathExtensions.abs(lastImpulse);
        
        if( absImpulse > equalsNull ){ //if impulse != 0
            //scaling with temperature
            if( isCluster(view) ){
                impulse.setLocation( 
                    impulse.getX() * localTemperature * clusterForceScalingFactor / absImpulse,
                    impulse.getY() * localTemperature * clusterForceScalingFactor / absImpulse);                
            }
            else {
                impulse.setLocation( 
                                impulse.getX() * localTemperature / absImpulse,
                                impulse.getY() * localTemperature / absImpulse);
            }
                                 
            view.getAttributes().put(KEY_CURRENT_IMPULSE,impulse);
            position.setLocation(position.getX()+impulse.getX(),
                                 position.getY()+impulse.getY());
            view.getAttributes().put(KEY_POSITION,position);
/*            if( isDebugging ){
                check(impulse,"impulse12");
                check(position,"position12");
            }*/
        }
        if( absLastImpulse > equalsNull ){
            
            //beta = angle between new and last impulse
            double beta = MathExtensions.angleBetween(impulse,lastImpulse);
            
            double sinBeta = Math.sin(beta);
            double cosBeta = Math.cos(beta);
            
            //detection for rotations
            if( Math.abs(sinBeta) >= Math.sin((Math.PI/2.0)+(alphaRot/2.0)) )
                skewGauge += sigmaRot * MathExtensions.sgn(sinBeta);            
                
            //detection for oscillation
            if( cosBeta < Math.cos(Math.PI+(alphaOsc/2.0)) )
                localTemperature *= sigmaOsc * Math.abs(cosBeta);            
            
            localTemperature *= 1.0 - Math.abs(skewGauge);
            localTemperature = Math.min(localTemperature,maxTemperature);
        }
        
        //applying changes
        view.getAttributes().put(KEY_TEMPERATURE,new Double(localTemperature));
        view.getAttributes().put(KEY_POSITION   ,position);
        view.getAttributes().put(KEY_SKEWGAUGE  ,new Double(skewGauge));
        view.getAttributes().put(KEY_LAST_IMPULSE,new Point2D.Double(
                                                               impulse.getX(),
                                                               impulse.getY()));
/*        if( isDebugging )
            checkCellList();*/
    }

/******************************************************************************/
/**
 * Adding two forces.
 * @param v1 Force that should be added with v2
 * @param v2 Force that should be added with v1
 * @return Sum of both forces.
 */
    private Point2D.Double add(Point2D.Double v1, Point2D.Double v2){
        return new Point2D.Double(v1.getX()+v2.getX(),v1.getY()+v2.getY());        
    }

/******************************************************************************/
/**
 * Subtracing two forces.
 * @param v1 Force, v2 should be subtracted from
 * @param v2 Force, that should be subtracted from v1.
 */
    private Point2D.Double sub(Point2D.Double v1, Point2D.Double v2){
        return new Point2D.Double(v1.getX()-v2.getX(),v1.getY()-v2.getY());
    }

/******************************************************************************/
/**
 * Returns all Cells, that have a direct connection with the given cell and are
 * a member of the given list.
 * 
 * @param list List of some cells, that should contain some relatives from view
 * @param view Cell, the relatives are requested from
 * @return List of all relatives that are in list.
 * @see #getRelatives(CellView)
 */
    private ArrayList getRelativesFrom(ArrayList list, CellView view){
        ArrayList relatives = getRelatives(view);
        ArrayList result    = new ArrayList();
        for( int i = 0; i < relatives.size(); i++ )
            if( list.contains(relatives.get(i)) )
                result.add(relatives.get(i));
        return result;
    }
        
/******************************************************************************/
/**
 * Returns a list of all cells, that have a direct connection with the given
 * cell via a edge. At the end of this method, the result is stored in the given
 * cell, so it will be available the next time, the method runs. This temporary
 * stored data will stay there, until the algorithm finishes 
 * (successfull or not). 
 * 
 * @param view Cell, the relatives requested from.
 * @return List of all cells, that have a direct connection with a edge to
 * the given cell. 
 */
    private ArrayList getRelatives(CellView view){    

        if( !(view instanceof VertexView) ) {
            new Exception("getRelatives 1").printStackTrace();
            return null;
        }
        
        if( view.getAttributes().containsKey(KEY_RELATIVES) )
            return (ArrayList) view.getAttributes().get(KEY_RELATIVES);
            
        ArrayList relatives = new ArrayList();
                    
        //if view is a cluster, then all clustered cells are extracted and
        //getRelatives is called for every cell again. the resulting relatives
        //are checked, if they are in the same cluster or another cluster.
        //if the last condition is the case, the cluster is added, else the
        //vertex is added to the list of relatives, iff he isn't already in the
        //list
        if( isCluster(view) ){
            ArrayList clusteredVertices = (ArrayList) 
                               view.getAttributes().get(KEY_CLUSTERED_VERTICES);
            for( int i = 0; i < clusteredVertices.size(); i++ ){
                ArrayList vertexRelatives = getRelatives(
                                            (CellView)clusteredVertices.get(i));
                for( int j = 0; j < vertexRelatives.size(); j++ ){
                    CellView relative = (CellView) vertexRelatives.get(j);
                    if( !clusteredVertices.contains(relative) ){
/*                      if( relative.getAttributes().containsKey(KEY_CLUSTER) ){
                relative = (CellView) relative.getAttributes().get(KEY_CLUSTER);
                        }*/
                        if( !relatives.contains(relative))
                            relatives.add(relative);
                    }
                }
            }
        }
        else {        
            
            //runs only for vertices. finds all ports of the vertex. every
            //edge in every port is checked on their source and target.
            //the one, that isn't the vertex, we are searching the relatives of,
            //is added to the list of relatives.
            
            ArrayList portsCells = new ArrayList();
        
            VertexView vertexView = (VertexView)view;
            GraphModel model = vertexView.getModel();
            CellMapper mapper = vertexView.getMapper() ;
            Object vertexCell = vertexView.getCell() ;
        
            for (int i = 0; i < model.getChildCount(vertexCell); i++){
                Object portCell = model.getChild(vertexCell, i);
                portsCells.add(portCell);
            }

            for( int i = 0; i < portsCells.size() ; i++ ){

                Object portCell = portsCells.get(i);

                Iterator edges = model.edges(portCell);

                while (edges.hasNext() ){            
                
                    Object edge = edges.next() ;
                    Object nextPort = null;
                
                    if( model.getSource(edge) != portCell ){
                        nextPort = model.getSource(edge);
                    }
                    else {
                      nextPort = model.getTarget(edge);
                    }
                
                    CellView nextVertex = mapper.getMapping(
                                               model.getParent(nextPort), true);
                    relatives.add(nextVertex);
                }
            }
        }
        view.getAttributes().put(KEY_RELATIVES,relatives);
        return relatives;                
    }

/******************************************************************************/
/**
 * This is a rating method for the cells. It is used during 
 * {@link #computeImpulse(CellView)} scale some forces. 
 * 
 * @param view Cell, the weight is of interest.
 */

//TODO: method doesn't work right for clusters

    private double getNodeWeight(CellView view){
        
        if( view.getAttributes().containsKey(KEY_MASSINDEX) )
            return ((Double)view.getAttributes().get(KEY_MASSINDEX)).
                                                                  doubleValue();
            
        int childCount = getRelatives(view).size();
        double massIndex = (double)(childCount + 1) / 2.0;
        
        view.getAttributes().put(KEY_MASSINDEX,new Double(massIndex));
        
        return massIndex;
    }

/******************************************************************************/
/**
 * Applies the changes to the Cells. This means, that all temporary stored
 * positions are applied to all cells in {@link #applyCellList}
 */
    private boolean setNewCoordinates(JGraph jgraph){
        
        Map viewMap = new Hashtable();
        
//        dlgProgress.setMessage(phaseName[PHASE_END]);
        
        for( int i = 0; i < cellList.size(); i++ ){
            
            Point2D.Double pos  = getPosition(i,cellList);
            Rectangle r = ((CellView)cellList.get(i)).getBounds();

            r.x = (int) (pos.getX() - ((double)r.width /2.0));
            r.y = (int) (pos.getY() - ((double)r.height/2.0));
            
            Object cell = ((CellView) cellList.get(i)).getCell();
  
            Map attributes = GraphConstants.createMap();

            GraphConstants.setBounds(attributes, r);

            viewMap.put(cell, attributes);
          
            
//            if( updateProgressDialog(PHASE_END,i,applyCellList.size()) )
//                return true;
        }
        
        jgraph.getGraphLayoutCache().edit(viewMap,null,null,null);
        
        
        return false;
    }

/******************************************************************************/
/**
 * Clears the temporary data from the cells in {@link #cellList} (all cells).
 */
    private void removeTemporaryLayoutDataFromCells(){
        
        for( int i = 0; i < cellList.size(); i++ )
            ((CellView)cellList.get(i)).getAttributes().clear();
    }

/******************************************************************************/
/**
 * Checks, if the algorithm could break it's calculation earlier, than 
 * performing until {@link #countRounds} is {@link #maxRounds}. This depends on
 * {@link #shouldEndPerAverage}. 
 * 
 * @param <b><code>true</code></b> either when the average the temperature of 
 * all cells is below {@link #minTemperature} or the temperature itself of all 
 * cells is below.
 */
    private boolean isFrozen(){
        double sumOfTemp  = 0.0; //sum of temperatures to get the average value
        double globalTemp = 0.0; //average value of all temperatures
        boolean isFrozen = true;//true while all temperatures <= minTemperature
        
        for( int i = 0; i < applyCellList.size(); i++ ){
            double temperature = getTemperature(i,applyCellList);
            sumOfTemp += temperature;
            isFrozen = isFrozen && (temperature <= minTemperature);
            
            if( !isFrozen && !shouldEndPerAverage )//speeds up a little
                break;
        }
        
        if( shouldEndPerAverage ){
            globalTemp = sumOfTemp / (double)applyCellList.size();
            return globalTemp < minTemperature;
        }
        else return isFrozen;
    }

/******************************************************************************/
/**
 * Erzeugt eine Permutation der Zahlen von 0 bis length
 * 
 * @param length Count and highest value of the generated sequence.
 * @return sequence of numbers, contains every number a single time. The 
 * sequence consists of numbers between 0 and length.
 */

    private int[] createPermutation(int length){
        int[] permutation = new int[length];
        for( int i = 0; i < permutation.length; i++ ){
            int newValue = (int)(Math.random()*(double)length);
            for( int j = 0; j < i; j++ )
                if( newValue == permutation[j] ){
                    newValue = (int)(Math.random()*(double)length);
                    j = -1; // wird auf 0 zurückgesetzt
                }
            permutation[i] = newValue;
        }        
        return permutation;
    }

/******************************************************************************/
/**
 * Creates a random Vector, with a given length and a random direction.
 * 
 * @param length Length of the Vector created by this method
 * @return Vector represented by a Point2D.Double
 */
    private Point2D.Double getRandomVector(double length){
        double alpha  = Math.random()*Math.PI*2;
//        double length = Math.random()*maxLength;
        return new Point2D.Double(length*Math.cos(alpha),
                                  length*Math.sin(alpha));
    }

/******************************************************************************/
/**
 * Calculates the barycenter of a graph, given by a list. This calculation is
 * done by summing the coordinates and dividing them with the number of 
 * coordinates.
 * 
 * @param list List of CellView's
 * @return Position of the barycenter
 */
    private Point2D.Double computeBarycenter(ArrayList list){
        
        double sumX = 0.0;
        double sumY = 0.0;
        
        for( int i = 0; i < list.size(); i++ ){
            CellView view = (CellView) list.get(i);
            
            initPosition(view);
            
            Point2D.Double pos = getPosition(view);
            sumX += pos.x;
            sumY += pos.y;
        }
        return new Point2D.Double(sumX/((double)list.size()),
                                  sumY/((double)list.size()));
    }

/******************************************************************************/
/**
 * Initialilzes the position of a CellView to the center point of the bounds
 * of the cell. This initialization is only be done, when the cell isn't 
 * initialised before.
 * 
 * @param view Cell, the position should be initialized.
 */
    private void initPosition(CellView view){
        if( !view.getAttributes().containsKey(KEY_POSITION) )
             view.getAttributes().put(KEY_POSITION,new Point2D.Double(
                                                view.getBounds().getCenterX(),
                                                view.getBounds().getCenterY()));
    }

/******************************************************************************/
/**
 * Moves the graph to the upper left corner of the drawing space. This is done,
 * after a successfull run of the algorithm, to correct it's output.
 */
    private void correctCoordinates(){
        Rectangle boundingBox = getBoundingBox();
        if( boundingBox != null ){
            for( int i = 0; i < cellList.size(); i++ ){
                CellView view = (CellView) cellList.get(i);
                Point2D.Double pos = getPosition(view);
                Point2D.Double newPos = new Point2D.Double(
                                                      pos.x-boundingBox.getX(),
                                                      pos.y-boundingBox.getY());
                view.getAttributes().put(KEY_POSITION,newPos);
            }
        }
    }

/******************************************************************************/
/**
 * Computes the bounding box of the whole graph. The result is a Rectangle, 
 * parallel to the X- and Y-axises of the drawing system, closing about the 
 * whole graph.
 * @return Rectangle, that contains the whole graph.
 * @see #getBoundingBox(ArrayList) 
 */
    private Rectangle getBoundingBox(){
        return getBoundingBox(cellList);
    }

/******************************************************************************/
/**
 * Computes the bounding box of the graph in the given list of CellViews. 
 * The result is a Rectangle, parallel to the X- and Y-axises of the drawing 
 * system, closing about the graph in the given list.
 * 
 * @param verticeList List containing the CellViews, the bounding box is of
 * interest.
 * @return Rectangle, that contains the whole graph, linked in the given list. 
 */
    private Rectangle getBoundingBox(ArrayList verticeList){
        
        if( verticeList.size() > 0 ){
            
            Point2D.Double vertexPos = getPosition(0,verticeList);
            Dimension vertexSize = ((CellView)verticeList.get(0)).getBounds().getSize();
            
            double minX = vertexPos.getX();
            double minY = vertexPos.getX();
            double maxX = vertexPos.getX()+vertexSize.getWidth();
            double maxY = vertexPos.getX()+vertexSize.getHeight();
            
            for( int i = 1; i < verticeList.size(); i++ ){
                
                vertexPos  = getPosition(i,verticeList);
                vertexSize =((CellView)verticeList.get(i)).getBounds().getSize();
                
                if( minX > vertexPos.getX() )
                    minX = vertexPos.getX();
                if( minY > vertexPos.getY() )
                    minY = vertexPos.getY();
                if( maxX < vertexPos.getX()+vertexSize.getWidth() )
                    maxX = vertexPos.getX()+vertexSize.getWidth();
                if( maxY < vertexPos.getY()+vertexSize.getHeight() )
                    maxY = vertexPos.getY()+vertexSize.getHeight();
                    
            }
            
            Rectangle boundingBox = new Rectangle((int)minX,
                                                  (int)minY,
                                                  (int)(maxX-minX),
                                                  (int)(maxY-minY));
            return boundingBox;
        }
        return null;
    }

/******************************************************************************/
/**
 * Returns the Position of a Cell contained in {@link #applyCellList}.
 * 
 * @param index Identifies the cell. This is the index of the cell in 
 * the given list of CellViews
 * @param list List containing only CellViews
 * @see #getAttribute(int,String,ArrayList)
 */
    private Point2D.Double getPosition(int index, ArrayList list){
        
        return (Point2D.Double) getAttribute(index,KEY_POSITION,list);
    }

/******************************************************************************/
/**
 * Returns the temperature of a cell contained in a given list.
 * 
 * @param index Identifies the cell. This is the index of the cell in 
 * a given list of CellViews
 * @param list List containing only CellViews
 * @see #getAttribute(int,String,ArrayList)
 */
    private double getTemperature(int index, ArrayList list){
        
        Double temperature = (Double) getAttribute(index,KEY_TEMPERATURE,list);
        return temperature.doubleValue();
    }

/******************************************************************************/
/**
 * Returns the Position of a Cell.
 * 
 * @param cell The cell, that holds the position of interest. 
 */
    private Point2D.Double getPosition(CellView cell){
        return (Point2D.Double) cell.getAttributes().get(KEY_POSITION);
    }

/******************************************************************************/
/**
 * Returns an attribute from a cell contained in a given list of CellViews.
 * 
 * @param index Identifies the cell. This is the index of the cell in 
 * the given list of CellViews
 * @param key Identifies the Attribute, that should be retrieved.
 * @param list List containing only CellViews
 */
    private Object getAttribute(int index, String key, ArrayList list){
        
        CellView view = (CellView) list.get(index);
                
        return view.getAttributes().get(key);
    }

/******************************************************************************/
/**
 * Arranges the initial cositions for the inserted cells. This is done, by 
 * placing the inserted cells in the barycenter of their relatives.
 * (possible with errors ... might be fixed soon) 
 */
    private void arrangePlacement(CellView[] views){
        for( int i = 0; i < cellList.size(); i++ )
            initPosition((CellView)cellList.get(i));
            
        if( views != null ){
            if( views.length > 0 ){

                ArrayList cellLevelList = new ArrayList();           

                for( int i = 0; i < views.length; i++ ){
                    
                    if( views[i] instanceof VertexView ){
                        
                        ArrayList relatives = getRelativesFrom(cellList,
                                                               views[i]);
                        
                        if( relatives.size() > 0 ){
                            if( views[i].getAttributes() == null )
                                views[i].setAttributes(new Hashtable());
                                
                            views[i].getAttributes().put(KEY_POSITION,
                                                  computeBarycenter(relatives));
                            
                            cellLevelList.add(views[i]);
                        }
                    }
                }
                
                for( int i = 0; i < cellLevelList.size(); i++ )
                    cellList.add(cellLevelList.get(i));
                
                int childViewCount = 0;
                CellView[] possibleChildViews = 
                                new CellView[views.length-cellLevelList.size()];
                
                for( int i = 0; i < views.length; i++ )
                    if( !cellLevelList.contains(views[i]) )
                        possibleChildViews[childViewCount++] = views[i];
                        
                arrangePlacement(possibleChildViews);
            }
        }
    }

/******************************************************************************/
/**
 * Method for the process of layout update. Adds inserted cells to 
 * {@link #applyCellList} and some of their neighbors. Adding of neighbors is
 * deceided by {@link #layoutUpdateMethod}. If a method is choosen with
 * perimeter, than the inserted cells are counted, that have a position whithin
 * the basic radius around inserted cells. then a new radius is calculated by
 * multiplying the increasial radius with the number of inserted cells found and
 * adding it to the basic radius. Then all cells, previously layouted whithin
 * this radius are also added to {@link #applyCellList}. After this, cells
 * within a given pathlength smaller than {@link #recursionDepth} are added
 * to {@link #applyCellList} too.
 * 
 * @param vertexList List of all inserted Vertices. 
 */
    public void addApplyableVertices(VertexView[] vertexList){
    
    
        for( int i = 0; i < vertexList.length; i++ ){
            if( !applyCellList.contains(vertexList[i]) )
                applyCellList.add(vertexList[i]);
            if( !cellList.contains(vertexList[i]) )
                cellList.add(vertexList[i]);
        }
        
        if( GEMLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETERS.equals(layoutUpdateMethod) ){
            for( int i = 0; i < vertexList.length; i++ ){
                double perimeterSize = perimeterInitSize;
                Point2D.Double pos = getPosition(vertexList[i]);
                for( int j = 0; j < vertexList.length; j++ ){
                    if( i != j ){
                        Point2D.Double oPos = getPosition(vertexList[j]);
                        if( Math.abs(pos.distance(oPos)) < (perimeterInitSize / 2.0) )
                            perimeterSize += perimeterSizeInc;
                    }
                }
                for( int j = 0; j < cellList.size(); j++ ){
                    
                    Point2D.Double uPos = getPosition(j,cellList);
                    
                    if( Math.abs(pos.distance(uPos)) < (perimeterSize / 2.0) && 
                        !applyCellList.contains(cellList.get(j)) )
                        
                        applyCellList.add(cellList.get(j));
                                        
                }
            }
            vertexList = new VertexView[applyCellList.size()];
            for( int i = 0; i < applyCellList.size(); i++ )
                vertexList[i] = (VertexView) applyCellList.get(i);
        }
        
        
           
        if( recursionDepth > 0 )
            addRelativesToList(vertexList,recursionDepth);
        
    }

/******************************************************************************/
/**
 * Recursiv method for adding all relatives whithin a given pathlength away
 * from the given array of Vertices to {@link #applyCellList}.
 * 
 * @param vertexList List of Vertices, which relatives might be added.
 * @param depth pathlength, the vertices adding to {@link #applyCellList} could
 * be away from the given array's vertices.
 */
    private void addRelativesToList(VertexView[] vertexList, int depth){
        
        if( vertexList == null )     return;
        if( vertexList.length == 0 ) return;
        if( depth == 0 )             return;
        
        for( int i = 0; i  < vertexList.length; i++ ){
                
            ArrayList relatives = getRelatives(vertexList[i]);
            VertexView[] relativeList = new VertexView[relatives.size()];
            
            for( int j = 0; j < relatives.size(); j++ ){
                if( !applyCellList.contains(relatives.get(j)) )
                    applyCellList.add(relatives.get(j));
                if( !cellList.contains(relatives.get(j)) )
                    cellList.add(relatives.get(j));
                relativeList[j] = (VertexView) relatives.get(j);
            }
                
            addRelativesToList(relativeList,depth-1);
        }
    }

/******************************************************************************/
/**
 * Method for Classes that extend this Algorithm. Will be called when performing
 * {@link #computeImpulse(CellView)}.
 */
    protected ArrayList getAdditionalForces(VertexView view){
        return new ArrayList();
    }

/******************************************************************************/
/**
 * Will be called, when cells are inserted or removed. When cells are removed,
 * they are also removed from {@link #cellList}, {@link #applyCellList} and
 * {@link #edgeList}. If cells are inserted a new layout update process starts.
 */
    public void graphChanged(GraphModelEvent e){
        
        if( !isRunning && isActive ){
            
            isRunning = true;
            
            GraphModelEvent.GraphModelChange change = e.getChange();
            
            Object[] objRem = change.getRemoved();
            Object[] objIns = change.getInserted();
            
            
            
            if( objRem == null && objIns != null ){ // nodes inserted
                
                for( int i = 0; i < cellList.size(); i++ )
                    initPosition((CellView)cellList.get(i));
                
                CellView[] viewInserted = jgraph.getGraphLayoutCache().getMapping(objIns,false);
                
                applyCellList = new ArrayList();

                /*extracting vertices into []*/                
                int vertexViewCount = 0;
                for( int i = 0; i < viewInserted.length; i++ )
                    if( viewInserted[i] instanceof VertexView )
                        vertexViewCount++;
                        
                
                VertexView[] vertexList = new VertexView[vertexViewCount];
                vertexViewCount = 0;
                for( int i = 0; i < viewInserted.length; i++ )
                    if( viewInserted[i] instanceof VertexView )
                        vertexList[vertexViewCount++] = (VertexView) viewInserted[i];
                /*extracting vertices into [] done*/
                
                //stops inserting process, if no vertex was inserted                
                if( vertexList.length == 0 ){
                    isRunning = false;
                    return;
                }
                
                //initialising runtime values from config
                loadRuntimeValues(VALUES_INC);
                
                //the number of cells in applyCellList will probably change
                sigmaRot /= 1.0 / (double) applyCellList.size();
                
                //positioning the new nodes in the barycenter of old relatives                
                arrangePlacement(vertexList);                
                                      
                //add new vertices and some relatives to applyCellList
                addApplyableVertices(vertexList); 
                
                if( applyCellList.size() == 0 ){
                    isRunning = false;
                    return;
                }                
                
//                showCellList(applyCellList,Color.GREEN);

                if( isClusteringEnabled ){
                    clusterGraph();       
                }
                
                
                //the number of cells in applyCellList has changed probably
                sigmaRot *= 1.0 / (double) applyCellList.size();
                maxRounds = applyCellList.size() * 4;
                 
                // performing algorithm on all nodes in applyCellList
                initialize();
                calculate();
                // algorithm done
                
                if( isClusteringEnabled )
                    declusterGraph();                
                
                if( useOptimizeAlgorithm )
                    optimizationAlgorithm.perform(jgraph,false,optimizationAlgorithmConfig);

                //moves graph to the upper left corner
                correctCoordinates();
             
                //taking changes   
                setNewCoordinates(jgraph);
                
                //removing algorithms attributes from nodes
                removeTemporaryLayoutDataFromCells();
                
            }
            else if( objRem != null && objIns == null ){ // nodes removed
                
                CellView[] viewRemoved = jgraph.getGraphLayoutCache().getMapping(objRem,false);
                                
                for( int i = 0; i < viewRemoved.length; i++ ){
                    if( viewRemoved[i] instanceof VertexView &&
                        cellList.contains(viewRemoved[i]) ){
                            
                        applyCellList.remove(viewRemoved[i]);
                        cellList.remove(viewRemoved[i]);
                    }
                }
            }            
            
            isRunning = false;
        }
    }

/******************************************************************************/
/******************** CLUSTERING METHODS **************************************/
/******************************************************************************/

/**
 * Clusters a graph. Cells, contained in {@link #cellList} and not contained
 * in {@link #applyCellList} are clustered by this short algorithm. The
 * algorithm first tries to identify how many cells it should cluster. This
 * is calculated by subtracting the size of {@link #applyCellList} from
 * the size of {@link #cellList} and dividing the result by the 
 * {@link #clusteringFactor}. In the next step, the identified number of
 * clusters are created, and their position is initialised by random. Then
 * every clusterable cell is added to the cluster where the distance of the
 * vertex and the cluster is minimal. After adding a cell, the clusters position
 * is recalculated. Finishing this step, the algorithm tries to minimize the
 * number of clusters, by sorting the clustered vertices, if there is another
 * cluster, that distance is shorter than the distance to the cluster, the
 * vertice is actually in. This can happen, because by moving vertices into the
 * clusters, the position of the clusters are changed. The minimization runs
 * until no vertice can be moved anymore. empty clusters are removed and finaly 
 * the clusters are added to {@link #applyCellList}, because they should move
 * while the upcoming next calculations. That move can later be retrieved by
 * subtracting the attributes {@link #KEY_POSITION} and 
 * {@link #KEY_CLUSTER_INIT_POSITION}.
 * 
 * @see #declusterGraph()
 * @see #computeClusterPosition(CellView)
 * @see #moveVerticeToCluster(CellView)
 */
    protected void clusterGraph(){
        //initialisation
        int maxClusters = (int)((double)(cellList.size() - applyCellList.size()) / clusteringFactor );
        if( maxClusters == 0 ){
            System.out.println("maxClusters = 0");
            return;
        }
        
        if( cellList.size() <= 1 ){
            System.out.println("cellList.size() <= 1");
            return;
        }
            
        ArrayList clusterList    = new ArrayList();
        ArrayList cellsToCluster = new ArrayList();
        
        //identifying all cells, that are clusterable
        for( int i = 0; i < cellList.size(); i++ )
            if( !applyCellList.contains(cellList.get(i)) )
                cellsToCluster.add(cellList.get(i));
        
        //initialize clusters
        VertexView[] clusters = new VertexView[maxClusters];
        CellMapper mapper = ((VertexView)cellList.get(0)).getMapper(); 
        Rectangle boundingBox = getBoundingBox();
        for( int i = 0; i < clusters.length; i++ ){        
            clusters[i] = new VertexView(null,jgraph,mapper);
            Map attributes = clusters[i].getAttributes();
            attributes.put(KEY_IS_CLUSTER,"true");
            attributes.put(KEY_POSITION,new Point2D.Double(
                                             Math.random()*boundingBox.width,
                                             Math.random()*boundingBox.height));
            clusterList.add(clusters[i]);
        }
        
        //cluster all available cells
        for( int i = 0; i < cellsToCluster.size(); i++ ){
            
            VertexView cell = (VertexView) cellsToCluster.get(i);
            Point2D.Double cellPos = getPosition(cell);
            int clusterID = 0;
            Point2D.Double clusterPos = getPosition((CellView)clusterList.get(0));
            double minDistance = MathExtensions.getEuclideanDistance(cellPos,clusterPos);
            
            //search for nearest cluster
            for( int j = 1; j < clusterList.size(); j++ ){
                clusterPos = getPosition(j,clusterList);
                double distance = MathExtensions.getEuclideanDistance(cellPos,clusterPos);
                if( minDistance > distance ){
                    minDistance = distance;
                    clusterID = j;
                }
            }
            VertexView cluster = (VertexView) clusterList.get(clusterID);
            moveVerticeToCluster(cell,cluster);
        }
        //initialization done
            
        //sorting the clustered vertices. if a vertice is nearer to a clusters
        //barycenter then to it's own clusters barycenter the vertice is moved
        //to that cluster. The coordinates of both clusters are recalculated.
        //this is done, until nothing could be done better.
        boolean couldMakeItBetter = false;        
        do {
            couldMakeItBetter = false;
            for( int i = 0; i < cellsToCluster.size(); i++ ){
                VertexView cell = (VertexView) cellsToCluster.get(i);
                VertexView oldCluster = (VertexView) cell.getAttributes().get(KEY_CLUSTER);
                Point2D.Double cellPos = getPosition(cell);
                Point2D.Double clusterPos = getPosition(oldCluster);
                double distance = MathExtensions.getEuclideanDistance(cellPos,clusterPos);
                for( int j = 0; j < clusterList.size(); j++ ){
                    VertexView cluster = (VertexView) clusterList.get(j);
                    if( cluster != oldCluster ){
                        clusterPos = getPosition(cluster);
                        double newDistance = MathExtensions.getEuclideanDistance(cellPos,clusterPos);
                        if( newDistance < distance ){
                            moveVerticeToCluster(cell,cluster);
                            couldMakeItBetter = true;
                            break;
                        }
                    }
                }
            }
        }
        while( couldMakeItBetter );
        
        //empty clusters are removed
        for( int i = 0; i < clusterList.size(); i++ ){
            if( !((VertexView)clusterList.get(i)).getAttributes().containsKey(KEY_CLUSTERED_VERTICES)){
                clusterList.remove(i--);            
            }
            else if( ((ArrayList)((VertexView)clusterList.get(i)).getAttributes().get(KEY_CLUSTERED_VERTICES)).size() == 0 ){            
                clusterList.remove(i--);
            }
        }
                
        //remove clustered vertices from cellList
        for( int i = 0; i < cellsToCluster.size(); i++ )
            cellList.remove(cellsToCluster.get(i));
            
        //adding clusters to applyCellList and cellList
        for( int i = 0; i < clusterList.size(); i++ ){
            applyCellList.add(clusterList.get(i));
            cellList.add(clusterList.get(i));
        }
            
        //storing a copy of position, to move vertices while declustering
        for( int i = 0; i < clusterList.size(); i++ ){
            VertexView cluster = (VertexView) clusterList.get(i);
            Map attribs = cluster.getAttributes();
            Point2D.Double clusterPos = (Point2D.Double) attribs.get(KEY_POSITION); 
            attribs.put(KEY_CLUSTER_INIT_POSITION,
                        new Point2D.Double( clusterPos.x,
                                            clusterPos.y));
        }
        
        for( int i = 0; i < clusterList.size(); i++ ){
            VertexView cluster = (VertexView)clusterList.get(i); 
            cluster.setCachedBounds(getBoundingBox((ArrayList)cluster.getAttributes().get(KEY_CLUSTERED_VERTICES)));
        }
        colorizeClusters(clusterList);
        stop(20);
    }
    
/******************************************************************************/
/**
 * Moves a vertice from the cluster, it is holded, to another cluster. This
 * implies that the vertice is removed from the old cluster and added to the 
 * new. After this, the positions of the old and the new cluster are 
 * recalculated.
 * 
 * @param vertice Vertex that should be moved
 * @param cluster Cluster the vertex should be moved
 */
    protected void moveVerticeToCluster(VertexView vertice, VertexView cluster){
        //adding vertice to new cluster
        if( !cluster.getAttributes().containsKey(KEY_CLUSTERED_VERTICES) )
            cluster.getAttributes().put(KEY_CLUSTERED_VERTICES,new ArrayList());
        ArrayList clusteredVertices = (ArrayList) cluster.getAttributes().get(KEY_CLUSTERED_VERTICES);
        clusteredVertices.add(vertice);
        
        //removing vertice from old cluster
        if( vertice.getAttributes().containsKey(KEY_CLUSTER) ){
            VertexView oldCluster = (VertexView) vertice.getAttributes().get(KEY_CLUSTER);
            ArrayList list = (ArrayList)oldCluster.getAttributes().get(KEY_CLUSTERED_VERTICES);
            list.remove(vertice);
            computeClusterPosition(oldCluster);             
        }
        //register cluster in vertice
        vertice.getAttributes().put(KEY_CLUSTER,cluster);
        //reposition cluster
        computeClusterPosition(cluster);
    }
    
/******************************************************************************/
/**
 * Recalculates the position of a cluster. The position of a cluster is defined
 * by the barycenter of the clustered vertices.
 * 
 * @param cluster Cell, that has to be a cluster, should be repositioned.
 */
    protected void computeClusterPosition(VertexView cluster){
        ArrayList clusteredVertices = (ArrayList)cluster.getAttributes().get(KEY_CLUSTERED_VERTICES);
        Point2D.Double clusterPos = computeBarycenter(clusteredVertices);
        cluster.getAttributes().put(KEY_POSITION,clusterPos);
    }
    
/******************************************************************************/
/**
 * Moves all clusters from {@link #cellList} and {@link #applyCellList}, 
 * extracts their clustered vertices and adds them to {@link #cellList}. While
 * doing this, it repositions the clustered vertices with the move, the cluster
 * has made during the calculation.
 * 
 * @see #clusterGraph() 
 */
    protected void declusterGraph(){
        if( cellList.size() <= 1 )
            return;
        
        //first collecting all clusters from applyCellList
        ArrayList clusterList = new ArrayList();
        for( int i = 0; i < cellList.size(); i++ ){
            VertexView cell = ((VertexView)cellList.get(i));
            if( isCluster(cell) )
                clusterList.add(cell);            
        }
        
        if( clusterList.size() == 0 )
            return;
            
        //cleaning up the cell lists
        for( int i = 0; i < clusterList.size(); i++ ){
            cellList.remove(clusterList.get(i));
            applyCellList.remove(clusterList.get(i));
        }
        
        //repositioning and extracting vertices to cellList 
        for( int i = 0; i < clusterList.size(); i++ ){
            VertexView cluster = (VertexView)clusterList.get(i);
            Map attribs = cluster.getAttributes();
            Point2D.Double newClusterPos = getPosition(cluster);
            Point2D.Double oldClusterPos = (Point2D.Double) attribs.get(KEY_CLUSTER_INIT_POSITION);
            //calculating move, cluster has made during his existance
            Point2D.Double move = new Point2D.Double(newClusterPos.x - oldClusterPos.x,
                                                     newClusterPos.y - oldClusterPos.y);
            ArrayList vertexList = (ArrayList)attribs.get(KEY_CLUSTERED_VERTICES);
            //applying move to clustered vertices
            for( int j = 0; j < vertexList.size(); j++ ){
                VertexView cell = (VertexView) vertexList.get(j);
                Point2D.Double cellPos = getPosition(cell);
                Point2D.Double newCellPos = new Point2D.Double(cellPos.x + move.x,
                                                               cellPos.y + move.y);
                cell.getAttributes().put(KEY_POSITION,newCellPos);
                //refilling clustered vertices in cellList
                cellList.add(cell);
            }
        }
    }

/******************************************************************************/
/**
 * Returns <code><b>true</b></code> when a cell is a cluster, else 
 * <code<b>false</b></code>. A cell is a cluster when it has under it's 
 * attributes a attribute with the boolean value <code><b>true</b></code> under
 * the key {@link #KEY_IS_CLUSTER}.
 * 
 * @param cell cell, that should be researched wheather it is a cluster or not.
 * @return <code><b>true</b></code> if cell is a cluster, else 
 * <code><b>false</b></code>.
 */
    protected boolean isCluster(CellView cell){
        if( cell.getAttributes().containsKey(KEY_IS_CLUSTER)){
            if( isTrue((String)cell.getAttributes().get(KEY_IS_CLUSTER))){
                return true;
            }
            else {
                System.err.println("FATAL ERROR: CELL CANNOT CLEARLY BE IDENTIFIED AS A CLUSTER!!!");
                return false;
            }        
        }
        else return false;        
    }
    
/******************************************************************************/

    private void colorizeClusters(ArrayList clusterList){
        Color[] colorList = new Color[] { Color.black, Color.magenta, Color.yellow, Color.blue, Color.green, Color.gray, Color.cyan, Color.red, Color.darkGray, Color.lightGray, Color.orange, Color.pink };
        for( int i = 0; i < clusterList.size(); i++ )
            if( i < colorList.length ){
                ArrayList clusteredVertices = (ArrayList) ((CellView)clusterList.get(i)).getAttributes().get(KEY_CLUSTERED_VERTICES);
                showCellList(clusteredVertices,colorList[i]);
            }
    }

    
/******************************************************************************/

    private void showCellList(ArrayList list, Color color){
        
        Map viewMap = new Hashtable();
        
        for( int i = 0; i < list.size(); i++ ){
            
            CellView view = (CellView)list.get(i);
            Point2D.Double pos  = getPosition(i,list);
            Rectangle r = view.getBounds();

            r.x = (int) (pos.getX() - (double)r.getWidth() /2.0);
            r.y = (int) (pos.getY() - (double)r.getHeight()/2.0);
            
            Object cell = view.getCell();
  
            Map attributes = GraphConstants.createMap();

            GraphConstants.setBackground(attributes, color);
            GraphConstants.setBounds    (attributes, r);
            

            viewMap.put(cell, attributes);          
        }
        
        jgraph.getGraphLayoutCache().edit(viewMap,null,null,null);        
    }
    
/******************************************************************************/

    private synchronized void stop(double sec){
        try{
            wait((long)(sec * 1000.0));
        }
        catch( Exception e ){
            e.printStackTrace();
        }
    }
    
/******************************************************************************/

    private void stop(int sec){
        stop((double)sec);
    }
}
