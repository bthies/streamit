package streamit.eclipse.grapheditor.editor.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
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
 * <h1>Simulated Annealing Layout Algorithm</h1><p>
 * Implemented from the paper: "Drawing Graphs Nicely Using Simulated Annealing"
 * from Ron Davidson and David Harel. ACM Transactions on Graphics, Vol. 15, 
 * No. 4, October 1996, Pages 301-331.
 * @author winkler
 * @version 1.0
 * Date of creation: 11.04.2003 - 12:39:58
 */
public class AnnealingLayoutAlgorithm implements LayoutAlgorithm, GraphModelListener {

    public final static String KEY_CAPTION   = "Annealing Layoutalgorithm Attributes";    
    public final static String KEY_POSITION  = "Position";
    public final static String KEY_RELATIVES = "Relatives";
    
    public final static String CF_KEY_EDGE_DISTANCE_RELEVANT_EDGES = "costfunction edge distance key for relevant edges";
    
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
     * Key for loading configuration values. Indicates to load values for a
     * normal run.
     */
    protected final static int CONFIG_KEY_RUN           = 0;
    /**
     * Key for loading configuration values. Indicates to load values for a
     * layout update.
     */
    protected final static int CONFIG_KEY_LAYOUT_UPDATE = 1;

    /**
     * actual temperature
     */
    private double    temperature;
    /**
     * starting temperature
     */
    private double    initTemperature;
    /**
     * when {@link #temperature} reaches this value, the algorithm finishes its
     * calculation.
     */
    private double    minTemperature;
    /**
     * value for costfunctions {@link #getNodeDistribution()} and
     * {@link #getEdgeDistribution()}. Determines, how long the edges have to
     * be.
     */
    private double    minDistance;
    /**
     * {@link #temperature} will be multiplied with this value every round
     */
    private double    tempScaleFactor;
    /**
     * maximum number of rounds, if algorithm doesn't stop earlier, by 
     * temperature decreasement.
     */
    private int        maxRounds;
    /**
     * normalizing and priority factors for the costfunctions
     */
    protected double[]  lambdaList;
    /**
     * the drawing area, the graph should be layouted in.
     */
    private Rectangle bounds;
    /**
     * determines, if the cells of the graph are computed every time in the
     * same order or a random order, calculated every round.
     */
    private boolean   computePermutation;
    /**
     * determines, if the only allowed moves for cells of the graph are moves, 
     * that cost less. 
     */
    private boolean   uphillMovesAllowed;
    /**
     * Indicates, if the algorithm should also run on Updates in the graph.
     */
    private boolean   isLayoutUpdateEnabled;
    
    /**
     * Indicates what costfunctions to use for calculating the costs of the 
     * graph. The bits of this Integer switches the functions. Possible Values
     * are <br>
     * <blockquote><blockquote>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_NODE_DISTRIBUTION 
     * COSTFUNCTION_NODE_DISTRIBUTION}<br>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_NODE_DISTANCE
     * COSTFUNCTION_NODE_DISTANCE}<br>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_BORDERLINE
     * COSTFUNCTION_BORDERLINE}<br>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_EDGE_DISTANCE
     * COSTFUNCTION_EDGE_DISTANCE}<br>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_EDGE_CROSSING
     * COSTFUNCTION_EDGE_CROSSING}<br>
     * {@link AnnealingLayoutAlgorithm#COSTFUNCTION_EDGE_DISTRIBUTION
     * COSTFUNCTION_EDGE_DISTRIBUTION}
     * </blockquote></blockquote>
     */
    private int       costFunctionConfig;
    
    /**
     * counts the rounds 
     */
    private int       round;
    /**
     * determines, in how many segments the circle around cells is divided,
     * to find a new position for the cell.
     */
    private int       triesPerCell;
    
    /**
     * the list of all cells of the graph
     */
    protected ArrayList cellList;
    /**
     * the list of all edges of the graph
     */
    protected ArrayList edgeList;
    /**
     * the list of all cells, a new layout should be calculated for
     */
    protected ArrayList applyCellList;
    
    /**
     * the JGraph
     */
    private JGraph    jgraph;
    
    /**
     * if this algorithm is a optimizing algorithm, this value indicates the
     * offset for the progress dialog, else it is 0
     */
    private int       initProgressValue = 0;
    
    /**
     * holds the configuration of the algorithm, gained by the controller
     */
    protected Properties presetConfig;
    
    /**
     * for debugging purpose. 
     */
    private long      time = 0;

    /**
     * the progress dialog
     */
    private ProgressDialog dlgProgress =
        new ProgressDialog((Frame) null, "Progress:", false);
        
    /**
     * for debugging purpose
     */
    private boolean isDebugging = false;
    /**
     * indicates if the algorihm is performing a calculation. this prevents from
     * entering the method {@link #graphChanged(GraphModelEvent) 
     * graphChanged(...)} more than once at a time.
     */
    private boolean isRunning   = false;
        
        
    /**
     * the number of edges, neighbors of inserted cells are away,
     * to be also layouted again.
     */   
    private int       luRecursionDepth;
    /**
     * if a cell has a lower distance to a inserted cell, after the cell gained 
     * its initial position, it will be layouted too
     */
    private double    luPerimeterRadius;
    /**
     * if more than one cell is inserted and the initial position of other 
     * inserted cells is inside {@link #luPerimeterRadius} around a initial
     * position of a inserted cell, than {@link #luPerimeterRadius} will be
     * increased by this value.
     */
    private double    luPerimeterRadiusInc;
    /**
     * determines how the neighborhood is handled, when a layout update
     * should be performed. Possible values are:<p>
     * <blockquote><blockquote>
     * {@link AnnealingLayoutController#KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY
     * KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY}<br>
     * {@link AnnealingLayoutController#KEY_LAYOUT_UPDATE_METHOD_PERIMETER
     * KEY_LAYOUT_UPDATE_METHOD_PERIMETER}<br>
     * </blockquote></blockquote>
     */
    private String    luMethod;
    
    /**
     * prevents from dividing with zero and from creating to high costs
     */
    private double equalsNull = 0.05;
    
    /**
     * Switches clustering for the layout update process on/off
     */
    private boolean         isClusteringEnabled;
    
    /**
     * Scales movement of clusters. It is recommendet to take
     * a value between 1.0 and 0.0. This garanties, that clusters move slower
     * than other cells. That rises the chance of getting a good looking layout
     * after the calculation.
     */
    private double          clusterMoveScaleFactor;
    
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

/******************************************************************************/
	/**
	 * Constructor for SimulatedAnnealingAlgorithm.
	 */
	public AnnealingLayoutAlgorithm() {
	}

/******************************************************************************/
	/**
     * Runs the Algorithm
	 * @see org.jgraph.layout.LayoutAlgorithm#perform(JGraph, boolean, Properties)
	 */
	public void perform(
		JGraph graph,
		boolean applyToAll,
		Properties configuration) {
            
        isRunning = true;
            
//        System.out.println("now running Simulated Annealing");
            
        /*----------------AQUIRATION OF RUNTIME CONSTANTS----------------*/
        jgraph = graph;
        presetConfig = configuration;
            
        cellList      = new ArrayList();
        edgeList      = new ArrayList();
        applyCellList = new ArrayList();
                    
        loadConfiguration(CONFIG_KEY_RUN);
        getNodes(jgraph,applyToAll);
        
        if( applyCellList.size() == 0 )
            return;
        
        if( isLayoutUpdateEnabled )
            jgraph.getModel().addGraphModelListener(this);
        /*------------------------AQUIRATION DONE------------------------*/
        
        initProgressValue = 0;
        dlgProgress.setValue(initProgressValue);
        dlgProgress.setVisible(true);
        
        /*------------------------ALGORITHM START------------------------*/
        init(true);
                
        boolean isCancled = run();
        /*-------------------------ALGORITHM END-------------------------*/
        
        
        //if this algorithm isn't a optimization add-on of another algorithm
        if( !isCancled ){
            moveGraphToNW();//moves the graph to the upper left corner
            applyChanges(); // making temporary positions to real positions
            removeTemporaryData(); // remove temporary positions
        }
        
        dlgProgress.setVisible(false);
        
        isRunning = false;
	}
    
/******************************************************************************/
/**
 * Runs the Algorithm as a optimization Algorithm of another Algorithm
 * @param applyList List of all Cells, a new Layout should be found for.
 * @param allCellList List of all Cells of the Graph
 * @param allEdgeList List of all Edges of the Graph
 * @param dialog Progress Dialog of the Algorithm, this Algorithm is a 
 * Optimizer for.
 * @return when Cancel is pressed during the optimization, the method returns
 * <code><b>true</b></code>, else, for a successfull run, 
 * <code><b>false</b></code>.
 */
    public boolean performOptimization(ArrayList applyList, ArrayList allCellList, ArrayList allEdgeList, Properties config, ProgressDialog dialog){
        cellList  = allCellList;
        applyCellList = applyList;
        edgeList = allEdgeList;
        
        presetConfig = config;
        
        dlgProgress = dialog;
        initProgressValue = dialog.getValue();

        loadConfiguration(CONFIG_KEY_RUN);
        
        init(false);
        boolean isCancled = run();
        
        return isCancled;
    }

/******************************************************************************/
/**
 * Loads the initial Values from the configuration.
 * 
 * @param configSwitch Determines which configurationvalues have to be loaded
 * Possible values are {@link #CONFIG_KEY_RUN} and 
 * {@link #CONFIG_KEY_LAYOUT_UPDATE}
 */
    private void loadConfiguration(int configSwitch){
        
        isLayoutUpdateEnabled = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_ENABLED));

        //load config for normal runs        
        if( configSwitch == CONFIG_KEY_RUN ){
            initTemperature  = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_INIT_TEMPERATURE));
            minTemperature   = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_MIN_TEMPERATURE));
            minDistance      = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_MIN_DISTANCE));
            tempScaleFactor  = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR));
            maxRounds        = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_MAX_ROUNDS));
            triesPerCell     = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_TRIES_PER_CELL));
            ArrayList lambda = (ArrayList) presetConfig.get(AnnealingLayoutController.KEY_LAMBDA);
        
            lambdaList = new double[AnnealingLayoutController.COUT_COSTFUNCTION];
        
            for( int i = 0; i < lambdaList.length; i++ )
                lambdaList[i] = ((Double)lambda.get(i)).doubleValue();
        
            bounds = (Rectangle) presetConfig.get(AnnealingLayoutController.KEY_BOUNDS);
        
            costFunctionConfig = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_COST_FUNCTION_CONFIG),2);
        
            computePermutation = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_COMPUTE_PERMUTATION));
            uphillMovesAllowed = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_IS_UPHILL_MOVE_ALLOWED));
        }

        //load config for layout updates
        else if( configSwitch == CONFIG_KEY_LAYOUT_UPDATE ){
            initTemperature  = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_INIT_TEMPERATURE));
            minTemperature   = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_TEMPERATURE));
            minDistance      = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MIN_DISTANCE));
            tempScaleFactor  = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR));
            maxRounds        = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_MAX_ROUNDS));
            triesPerCell     = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_TRIES_PER_CELL));
            ArrayList lambda = (ArrayList) presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_LAMBDA);
        
            lambdaList = new double[AnnealingLayoutController.COUT_COSTFUNCTION];
        
            for( int i = 0; i < lambdaList.length; i++ )
                lambdaList[i] = ((Double)lambda.get(i)).doubleValue();
        
            bounds = (Rectangle) presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_BOUNDS);
        
            costFunctionConfig = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG),2);
        
            computePermutation = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION));
            uphillMovesAllowed = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED));
            luRecursionDepth = Integer.parseInt((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH));
            luPerimeterRadius = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS));
            luPerimeterRadiusInc = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE));
            luMethod             = (String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD);
            
            isClusteringEnabled = isTrue((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED));
            clusteringFactor = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR));            
            clusterMoveScaleFactor = Double.parseDouble((String)presetConfig.get(AnnealingLayoutController.KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE));
        }
        loadAdditionalConfiguration(configSwitch);
    }

/******************************************************************************/
/**
 * Method of classes extending this class, that want to load their initial
 * values from the configuration.
 * 
 * @param configSwitch Determines which configurationvalues have to be loaded
 * Possible values are {@link #CONFIG_KEY_RUN} and 
 * {@link #CONFIG_KEY_LAYOUT_UPDATE}
 * @see #loadConfiguration(int)
 */
    protected void loadAdditionalConfiguration(int configSwitch){
    }

/******************************************************************************/
/**
 * Helper-method. Transforms a String into a Boolean Value. The String has to
 * Contain the characters "true" or "false". upper case writings of some letters
 * doesn't matter. if the String doesn't contain "true" or "false" the method
 * returns false (easier to handle than throwing an exception).
 */
    private boolean isTrue(String boolValue){
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
 * Extracts all cells, all edges and all cells, the algorithm should run for,
 * from JGraph. After calling this Method {@link #cellList}, 
 * {@link #applyCellList} and {@link #edgeList} is filled.
 * 
 * @param jgraph A instanz from JGraph, the Cells will be extract from.
 * @param applyToAll Determines, if the Algorithm should run for all Cells or
 * only for the selected. In the first case, the contense of {@link #cellList}
 * is the same as of {@link #applyCellList}
 */
    private void getNodes(JGraph jgraph, boolean applyToAll){
        
        Object[] cells = jgraph.getRoots();
        Object[] selectedCells = jgraph.getSelectionCells();
        
        CellView[] view = jgraph.getGraphLayoutCache().getMapping(cells,false);
        CellView[] selectedView = jgraph.getGraphLayoutCache().getMapping(selectedCells,false);
        
        for (int i = 0; i < view.length; i++)
            if (view[i] instanceof VertexView){
                if( !cellList.contains(view[i]) )
                    cellList.add(view[i]);
                if( applyToAll )
                    if( !applyCellList.contains(view) )
                        applyCellList.add(view[i]);
            }
            else if( view[i] instanceof EdgeView && view[i] != null ){
                if( !edgeList.contains(view[i]) )
                    edgeList.add(view[i]);
            }
        if( !applyToAll )
            for( int i = 0; i < selectedView.length; i++ )
                if( view[i] instanceof VertexView )
                    if( !applyCellList.contains(selectedView[i]) )
                        applyCellList.add(selectedView[i]);
    }
    
/******************************************************************************/
/**
 * Makes the changed Positions of the Cells of the graph visible. This is like
 * a "commit". Before this Method runs, nothing has change is the visible 
 * representation of the graph. After this method, the Layout for the Cells
 * of the graph is applied.
 */
    private void applyChanges(){
        
        Map viewMap = new Hashtable();
        
        for( int i = 0; i < applyCellList.size(); i++ ){
            
            CellView view = (CellView)applyCellList.get(i);
            Point2D.Double pos  = getPosition(view);
            Rectangle r = view.getBounds();

            r.x = (int) (pos.getX() - ((double)r.getWidth() /2.0));
            r.y = (int) (pos.getY() - ((double)r.getHeight()/2.0));
            
            Object cell = ((CellView) applyCellList.get(i)).getCell();
  
            Map attributes = GraphConstants.createMap();

            GraphConstants.setBounds(attributes, r);

            viewMap.put(cell, attributes);          
        }
        
        jgraph.getGraphLayoutCache().edit(viewMap,null,null,null);        
        
    }

/******************************************************************************/
/**
 * Removes the temporary Data from the Cells of the graph. During the run of the
 * Algorithm there has been plenty of Data stored in the Cells. These are
 * removed here, if the Algorithm is canceled or finished.
 */
    private void removeTemporaryData(){
        for( int i = 0; i < applyCellList.size(); i++ )
            ((CellView)applyCellList.get(i)).getAttributes().clear();        
    }

/******************************************************************************/
/**
 * Initialises the Algorithm. This is the step right before running the 
 * Algorithm. Letting this method set the initial Positions for all cells is
 * only necessary when the Algorithm makes a normal run. Otherwise the initial
 * Positions are allready set, by  
 * {@link #arrangeLayoutUpdateInsertPlacement(CellView[]) 
 * arrangeLayoutUpdateInsertPlacement(...)}.
 * @param setInitPositions Determines, if the initial Positions of the cells 
 * should be set or not. Initial Positions are calculated by random.
 */
    private void init(boolean setInitPositions){
        if( setInitPositions ){
            
            for( int i = 0; i < applyCellList.size(); i++ ) 
                if( !((CellView)applyCellList.get(i)).getAttributes().containsKey(KEY_POSITION) )                   
                    setPosition(i, 
                              (Math.random()*bounds.getWidth()) +bounds.getX(),
                              (Math.random()*bounds.getHeight())+bounds.getY());
                              
            for( int i = 0; i < cellList.size(); i++ )
                if( !((CellView)cellList.get(i)).getAttributes().containsKey(KEY_POSITION) )
                    setPosition((CellView)cellList.get(i),
                              (Math.random()*bounds.getWidth()) +bounds.getX(),
                              (Math.random()*bounds.getHeight())+bounds.getY());
        }
            
        
        temperature = initTemperature;
        maxRounds   = Math.min(100 * applyCellList.size(),
                               getMaxRoundsByTemperature(temperature));
        round = 0;
    }

/******************************************************************************/
/**
 * Runs the Algorithm until {@link #temperature} is lower than 
 * {@link #minTemperature} or cancel on the progressdialog is pressed.
 * 
 * @return When the Algorithm is Canceled, the Method breaks and returns 
 * <code><b>true</b></code>.
 */
    private boolean run(){
        boolean isCancled = false;
        while( round <= maxRounds && !isCancled)
            isCancled = performRound();
        return isCancled;
    }

/******************************************************************************/
/**
 * Performs one round, so thats the main part of the Algorithm.
 * Different to the original Implementation of the Algorithm, this Algorithm
 * doesn't work with aproximativ 30 random Placements per Cell to find the best
 * Position. This Algorithm works with a user defined number of segments. The
 * Circle, the Cells will be placed on, is calculated like the original
 * Implementation tells. But it is splited into a user defined number of
 * segments. Then per cell a random offset is calculated and starting from
 * that offset every segment is checked out, whether there is a better position
 * for the cell. This can be done in a random order of the cells or always in
 * the same order. Temperature is decreased after all cells are checked out
 * for a new position, like in the original. While the original Implementation
 * allows always uphill moves, this Algorithm allows the user to decide to work
 * with or without them.
 * @return If the Algorithm is canceled while performing this method, the
 * method returns <code><b>true</b></code>. 
 */
    private boolean performRound(){
        
        Point2D.Double[] config = getConfig();
        
        double startEnergy      = getGlobalCosts(lambdaList);
        double globalEnergy     = startEnergy;
        double newGlobalEnergy  = globalEnergy * 1.1; //somewhat higher than globalEnergy

        //sequencial order cells are computed (every round the same order)
        int[] sequence = new int[applyCellList.size()];
        if( !computePermutation )
            for( int i = 0; i < applyCellList.size(); i++ )
                sequence[i] = i;
                
                
                
        for( int i = 0; i < applyCellList.size(); i++ ){        
            
            
            if( computePermutation )//random order
                sequence = createPermutation(applyCellList.size());
           
            //random offset
            double offset = Math.random() * 2.0 * Math.PI;
            
            for( int j = 0; j < triesPerCell; j++ ){
                
                double angle = (double)j * ((2.0 * Math.PI)/(double)triesPerCell);
                angle += offset;

                Point2D.Double move = null;                                
                //calculating new move 
                if( isCluster((CellView)applyCellList.get(i)) ){
                    move = new Point2D.Double(  
                        clusterMoveScaleFactor * temperature * Math.cos(angle),
                        clusterMoveScaleFactor * temperature * Math.sin(angle));
                }
                else {
                    move = new Point2D.Double( temperature * Math.cos(angle),
                                               temperature * Math.sin(angle));
                    
                }
//                Point2D.Double randomMove = getRandomVector(temperature);

                //applying new move
                setPosition(sequence[i],config[sequence[i]].x + move.x,
                                        config[sequence[i]].y + move.y);
            
                //calculating the costs for the actual layout
                newGlobalEnergy = getGlobalCosts(lambdaList);
            
                //taking move if costs < previos cost or uphill move possible
                if( newGlobalEnergy < globalEnergy ||
                    (getBolzmanBreak(globalEnergy,newGlobalEnergy) && 
                     uphillMovesAllowed) ){
                
//                    if( isDebugging )
//                        System.out.println("taking new energy : "+globalEnergy+" -> "+newGlobalEnergy+" <<<<<<<<<<<<<<<<<<<<<<<<<");
                
                    globalEnergy = newGlobalEnergy;
                    
                    config[sequence[i]] = new Point2D.Double(
                                                config[sequence[i]].x + move.x,
                                                config[sequence[i]].y + move.y);
                                       
//                    if( isDebugging )         
//                        showApplyCellList();
                
                    break;
                }
                else {
//                    if( isDebugging )
//                        System.out.println("energy = "+globalEnergy+"   new Global Energy = "+newGlobalEnergy+"   temperature = "+temperature);
                    setPosition(sequence[i],
                                config[sequence[i]].x,
                                config[sequence[i]].y);
                }
                   
                //progressdialog update
                dlgProgress.setValue(initProgressValue+(int)(((double)((round*applyCellList.size()*triesPerCell)+(i*triesPerCell)+j)/(double)(maxRounds*applyCellList.size()*triesPerCell))*(100.0-initProgressValue)));
                if( dlgProgress.isCanceled() )
                    break;
            }
            
            //if this rounds runs very good and energy is 5% of starting value
            //then break this round and start next round
            if( globalEnergy == startEnergy * 0.05 )
                break;
            //if cancel is pressed
            if( dlgProgress.isCanceled() )
                break;
        }
        //temperature will be decreased
        temperature *= tempScaleFactor;
        
        round++;//rounds are counted
        
        return dlgProgress.isCanceled();
    }

/******************************************************************************/
/**
 * Extracts the Positions of all cells into a array of Positions.
 * @return Array that represents the Positions of the Cells in 
 * {@link #applyCellList}.
 */
    private Point2D.Double[] getConfig(){
        Point2D.Double[] config = new Point2D.Double[applyCellList.size()];
        for( int i = 0; i < applyCellList.size(); i++ ){
            Point2D.Double pos = getPosition((CellView)applyCellList.get(i));
            config[i] = new Point2D.Double(pos.x,pos.y);
        }
        return config;
    }

/******************************************************************************/
/**
 * Calculates the costs of the actual graph by using costfunctions.
 * @param lambda Normalizing and priority values for the costfunctions
 * @return costs for the actual graph
 * @see #costFunctionConfig
 * @see #getBorderline(double)
 * @see #getEdgeCrossing(double)
 * @see #getEdgeDistance(double)
 * @see #getEdgeLength(double)
 * @see #getNodeDistance(double)
 * @see #getNodeDistribution(double)
 */
    private double getGlobalCosts(double[] lambda){
        
        //assert lambda.length != AnnealingLayoutController.COUT_COSTFUNCTION;
        
//        long startTime = System.currentTimeMillis();
        
        double energy = 0.0;
        
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_NODE_DISTANCE) != 0 ){
            energy += getNodeDistance(lambda[5]);
        }
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_NODE_DISTRIBUTION) != 0 ){
            energy += getNodeDistribution(lambda[0]);
        }
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_BORDERLINE) != 0 ){
            energy += getBorderline(lambda[1]);
        }
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_EDGE_LENGTH) != 0 ){  
            energy += getEdgeLength(lambda[2]);
        }
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_EDGE_CROSSING) != 0 ){
            energy += getEdgeCrossing(1.0,lambda[3]);
        }
        if( (costFunctionConfig & AnnealingLayoutController.COSTFUNCTION_EDGE_DISTANCE) != 0 ){
            energy += getEdgeDistance(lambda[4]);
        }
        
        energy += getAdditionalCosts(costFunctionConfig,lambda);
        
//        time += System.currentTimeMillis()-startTime;    
                    
        return energy;
    }
        
/******************************************************************************/
/**
 * Method for classes that extends this Algorithm. Calls the Costfunctions of
 * the extending class.
 * @return costs generated with the additional costfunctions
 * @see #getGlobalCosts(double[])
 */
    protected double getAdditionalCosts(int cfConfig, double[] lambda){
        return 0.0;
    }

/******************************************************************************/
/**
 * Creates a permutation of the Numbers from 0 to a determined value.
 * @param length Number of Numbers and maximal distance to 0 for the Numbers
 * filling the permutation
 * @return Permutation of the Numbers between 0 and <code>length</code>
 */

    public int[] createPermutation(int length){
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
 * Calculates a break condition for {@link #performRound()} if uphill moves
 * are allowed. This is computed by a formular from Bolzman:<p>
 * <blockquote><blockquote><code>
 * random < e^(oldEnergy-newEnergy)
 * </code></blockquote></blockquote>
 * @param oldEnergy The Energy before the Energy has increased, so it's the 
 * lower one, of the two values.
 * @param newEnergy The Energy after the Energy has increased, so it's the
 * higher one, of the two values
 * @return sometimes <code><b>true</b></code> when the random number is
 * smaler than <code>e^(oldEnergy-newEnergy)</code>
 */
    private boolean getBolzmanBreak(double oldEnergy, double newEnergy){
        return Math.random() < Math.pow(Math.E,(oldEnergy-newEnergy)/temperature);
    }
    
/******************************************************************************/
/**
 * Calculates the maximal number of rounds, by flattening the actual 
 * {@link #temperature} with the temperature scaling factor 
 * {@link #tempScaleFactor}
 * 
 * @param actualTemperature The Temperature of the actual Graph
 * @return The number of Rounds that have to be performed until 
 * {@link #temperature} falls under {@link #minTemperature}.
 */
    private int getMaxRoundsByTemperature(double actualTemperature){
        return (int)Math.ceil( Math.log(minTemperature/actualTemperature) /
                                Math.log(tempScaleFactor));
    }
    
/******************************************************************************/
/**
 * Costfunction. One criterion for drawing a "nice" graph is to spread the cells
 * out evenly on the drawing space. The distances between the cells need not to 
 * be perfectly uniform, but the graph sould be occupy a reasonable part of
 * the drawing space, and, if possible, the cells shouldn't be overcrowded.
 * This function calculates the sum, over all pairs of cells, of a function
 * that is inverse-proportional to the distance between the cells.
 * 
 * @param lambda A normalizing factor that defines the relativ importance of
 * this criterion compared to others. Increasing lambda relative to the other
 * normalizing factors causes the Algorithm to prefer pictures with smaller 
 * distances between cells.
 * @return costs of this criterion
 */
    private double getNodeDistribution(double lambda){
        double energy = 0.0;
        
        for( int i = 0 ; i < applyCellList.size(); i++ )
            for( int j = 0; j < cellList.size(); j++ ){

                if( applyCellList.get(i) != cellList.get(j) ){
                    double distance = MathExtensions.getEuclideanDistance(
                                    getPosition((CellView)applyCellList.get(i)),
                                    getPosition((CellView)cellList.get(j)));
                                    
                    //prevents from dividing with Zero
                    if( Math.abs(distance) < equalsNull )
                        distance = equalsNull;
            
                    energy += lambda/(distance*distance);
                }
            }
//        System.out.println("NodeDistribution : "+energy);
        return energy;
    }
    
/******************************************************************************/
/**
 * Costfunction. As in physics, truly minimizing the potential energy might
 * result in spreading out the elements indefinitely. To avoid this, and to 
 * reflect the physical limitations of the output device, add this costfunction
 * to the energy function to deal with the borderlines of the drawing space.
 * 
 * @param Increasing lambda relativ to the other lamdas pushes the cells 
 * towards the center, while decreasing it results in using more of the
 * drawing space near the borderlines. 
 * @return costs of this criterion
 */
    private double getBorderline(double lambda){
        double energy = 0.0;
        for( int i = 0; i < applyCellList.size(); i++ ){
            Point2D.Double pos = getPosition((CellView)applyCellList.get(i));
            double t = pos.y-bounds.y;
            double l = pos.x-bounds.x;
            double b = bounds.y+bounds.height-pos.y;
            double r = bounds.x+bounds.width -pos.x;
        
            energy += lambda * ( (1.0/(t*t)) + (1.0/(l*l)) + (1.0/(b*b)) + (1.0/(r*r)) );        
        }
//        System.out.println("Borderline       : "+energy);
        return energy;
    }
    
/******************************************************************************/
/**
 * Costfunction. This criterion tries to shorten the edges to a necessary 
 * minimum without causing the entire graph to become to tightly packed.
 * This function penalizes long edges.
 * 
 * @param lambda An appropriate normalizing factor. Increasing lamda relative
 * to the lambdas of other costfunctions will result in shorter Edges. 
 * Decreasing brings up very different length of the edges.
 * @return costs of this criterion
 */    
    private double getEdgeLength(double lambda ){
        double energy = 0.0;

        Line2D.Double[] lineList = getEdgeLines(edgeList);
        
        for( int i = 0; i < lineList.length; i++ ){
            
            Point2D p1 = lineList[i].getP1();
            Point2D p2 = lineList[i].getP2();
            
            double edgeLength = p1.distance(p2);
            
            energy += lambda * edgeLength * edgeLength;
        }        
//        System.out.println("EdgeLength       : "+energy);        
        return energy;
    }
             
/******************************************************************************/
/**
 * Costfunction. A constant penalty value is added for every two edges that 
 * cross.
 * @param lambda Normalizing factor. Increasing lambda means attributing more
 * importance to the elimination of edge crossings, and results in pictures
 * with fewer crossings on average. However, this may be at the expense of other
 * aesthetics.
 * @return costs of this criterion.
 */
    private double getEdgeCrossing(double f, double lambda){
        int n = 0; // counts edgecrossings around vertex[i]
        
        Line2D.Double[] lineList = getEdgeLines(edgeList);
        
        for( int i = 0; i < lineList.length; i++ )
            for( int j = i; j < lineList.length; j++ )
                if( j != i )
                    if( lineList[i].intersectsLine(lineList[j]) ){
                        if( ((lineList[i].getP1().getX() != lineList[j].getP1().getX()) && (lineList[i].getP1().getY() != lineList[j].getP1().getY())) &&
                            ((lineList[i].getP1().getX() != lineList[j].getP2().getX()) && (lineList[i].getP1().getY() != lineList[j].getP2().getY())) &&
                            ((lineList[i].getP2().getX() != lineList[j].getP1().getX()) && (lineList[i].getP2().getY() != lineList[j].getP1().getY())) &&
                            ((lineList[i].getP2().getX() != lineList[j].getP2().getX()) && (lineList[i].getP2().getY() != lineList[j].getP2().getY())) ){
                            n++;
                        }
                    }
//        System.out.println("EdgeCrossings : "+n);
        return lambda * f * (double) n;
    }
    
/******************************************************************************/
/**
 * Costfunction. This method calculates the distance between Cells and Edges.
 * A small distance brings up higher costs while great distances generates lower
 * costs. Costs for the distance between Cells and Edges are always computed
 * by the method. If the distance is smaller than {@link #minDistance} 
 * additional costs are added. This method is suggested for finetuning and other
 * short running calculations. Its the slowest of all costfunctions implemented 
 * here.
 * 
 * @param lamda A normalizing factor for this function. Drawings with a
 * relativ increase lambda will have greater distances between nodes and
 * edges, by the expense of other aesthetics.
 */
    private double getEdgeDistance(double lambda){
        double energy = 0.0;
        
        for( int i = 0; i < applyCellList.size(); i++ ){
            
            double h = 0.0;
            CellView view = (CellView) applyCellList.get(i);
            
            ArrayList relevantEdges = null;
            if( view.getAttributes().containsKey(CF_KEY_EDGE_DISTANCE_RELEVANT_EDGES) ){
                relevantEdges = (ArrayList) view.getAttributes().get(CF_KEY_EDGE_DISTANCE_RELEVANT_EDGES);
            }
            else {
                relevantEdges = getRelevantEdges(view);
                view.getAttributes().put(CF_KEY_EDGE_DISTANCE_RELEVANT_EDGES,relevantEdges);
            }
            
            Line2D.Double[] lineList = getEdgeLines(getRelevantEdges(view));
        
            for( int j = 0; j < lineList.length; j++ ){
                
                double distance = lineList[j].ptSegDist(getPosition(view));
                
                //prevents from dividing with Zero
                if( Math.abs(distance) < equalsNull )
                    distance = equalsNull;
                                        
                if( distance != 0.0 )
                    h += lambda / ( distance * distance );
                  
                    if( distance < minDistance )
                        h += lambda / ( minDistance * minDistance );
            }
            
            energy += h;
        }
        
//        System.out.println("EdgeDistance     : "+energy);
        
        return energy;
    }
    
/******************************************************************************/
/**
 * Costfunction. This is a extension to the original Algorithm. This method
 * evaluates the distances between cells. When the distance is lower than 
 * {@link #minDistance} or the cells are overlapping the costs from this 
 * function increase.
 * 
 * @param lambda Normalizing value for this function. Increasing this value
 * brings up less overlapping pairs of cells, by the expense of other 
 * aesthetics.
 * @return costs of this criterion. 
 */
    private double getNodeDistance(double lambda){
        double energy = 0.0;
        double radiusInc = 30.0;
        int overlapCount = 0;
        for( int i = 0; i < applyCellList.size(); i++ ){
                    
            Point2D.Double pos  = (Point2D.Double)((CellView)applyCellList.get(i)).getAttributes().get(KEY_POSITION);
            Rectangle vertex    = ((CellView)applyCellList.get(i)).getBounds();
            
            for( int j = 0; j < cellList.size(); j++ ){
                
                if( applyCellList.get(i) != cellList.get(j) ){
                    Point2D.Double uPos = (Point2D.Double)((CellView)cellList.get(j)).getAttributes().get(KEY_POSITION);
                    Rectangle uVertex =   ((CellView)cellList.get(j)).getBounds();
                    
                    double minDist = Math.max((2.0 * radiusInc) + 
                        (Math.max(vertex.getWidth(),vertex.getHeight())/2.0) + 
                        (Math.max(uVertex.getWidth(),uVertex.getHeight())/2.0),
                        minDistance);
                        
                    double distance = Math.abs(pos.distance(uPos));
                    
                    //prevents from dividing with Zero
                    if( Math.abs(distance) < equalsNull )
                        distance = equalsNull;
                    
                
                    if( distance < minDist ){
                        energy += lambda / (distance * distance);
                        overlapCount++;
                    }
                }
            }                
        }
        return energy;            
    }

/******************************************************************************/
/**
 * Transforms the Edges stored in a given List of edges into an array of lines.
 * This is usefull, to get the Positions of the Edges.
 * @param edges List containing only EdgeViews
 * @return Array of Lines representing the edges of the graph.
 */
    private Line2D.Double[] getEdgeLines(ArrayList edges){
        Line2D.Double[] lines = new Line2D.Double[edges.size()];
        for( int i = 0; i < edges.size(); i++ ){
            
            EdgeView edge = (EdgeView) edges.get(i);
            
            GraphModel model  = edge.getModel();
            CellMapper mapper = edge.getMapper();
            
            CellView source = edge.getSource().getParentView();
            CellView target = edge.getTarget().getParentView();
            
            lines[i] = new Line2D.Double(getPosition(source),
                                         getPosition(target));
        }
        return lines;
    }

/******************************************************************************/
/**
 * Returns all Edges that are connected with cells, member of 
 * {@link #applyCellList}, except the edges connected the the given cell.
 * @param except Edges connected to this cell are not of interest
 * @return List of all interesting Edges
 */
    private ArrayList getRelevantEdges(CellView except){
        ArrayList relevantEdgeList = new ArrayList();
        for( int i = 0; i < edgeList.size(); i++ ){
            CellView view = ((EdgeView)edgeList.get(i)).getSource().getParentView();
            if( view != except &&
                applyCellList.contains(view) ){
                relevantEdgeList.add(edgeList.get(i));
            }
            else {
                view = ((EdgeView)edgeList.get(i)).getTarget().getParentView();
                if( view != except &&
                    applyCellList.contains(view) ){
                    relevantEdgeList.add(edgeList.get(i));
                }
            }
        }
        return relevantEdgeList;
    }

/******************************************************************************/
/**
 * Computes a random Vector with a random direction and a given length.
 */
    public Point2D.Double getRandomVector(double maxLength){
        double alpha  = Math.random()*Math.PI*2;
        double length = Math.random()*maxLength;
        return new Point2D.Double(length*Math.cos(alpha),
                                   length*Math.sin(alpha));
    }
    
/******************************************************************************/
/**
 * Sets the position of a CellView to the given Position
 * 
 * @param view The CellView, the position should be set
 * @param pos New Position
 * @see #setAttribute(CellView,String,Object)
 */
    private void setPosition(CellView view, Point2D.Double pos){
        setAttribute(view,KEY_POSITION,pos);
    }

/******************************************************************************/
/**
 * Sets the position of a CellView member of {@link #applyCellList} to the given
 * position.
 * 
 * @param index ID of the CellView in {@link #applyCellList}
 * @param x X-Coordinate of the new position
 * @param y Y-Coordinate of the new position
 * @see #setPosition(CellView,double,double)
 */
    private void setPosition(int index, double x, double y){
        setPosition((CellView)applyCellList.get(index),x,y);
    }

/******************************************************************************/
/**
 * Sets the position of a CellView to the given Position
 * 
 * @param view The CellView, the position should be set
 * @param x X-Coordinate of the new position
 * @param y Y-Coordinate of the new position
 * @see #setPosition(CellView,Point2D.Double)
 */
    private void setPosition(CellView view, double x, double y){
        setPosition(view,new Point2D.Double(x,y));
    }

/******************************************************************************/
/**
 * Returns the Position of a CellView
 * 
 * @param view CellView, the position is requested
 * @return Position of the CellView
 * @see #getAttribute(CellView,String)
 */
    private Point2D.Double getPosition(CellView view){
        return (Point2D.Double) getAttribute(view,KEY_POSITION);        
    }

/******************************************************************************/
/**
 * Sets an attribute in a CellView
 * 
 * @param view CellView, the attribute should be set
 * @param key The attribute will be stored in the CellView under that key.
 * @param obj Object representing the attribute, that should be stored.
 */
    private void setAttribute(CellView view,String key, Object obj){
        if( view.getAttributes() == null )
            view.setAttributes(new Hashtable());
        Map attributes = view.getAttributes();
        attributes.put(key,obj);
    }

/******************************************************************************/
/**
 * Returns an attribute from a CellView
 * 
 * @param view CellView, that stores the attribute
 * @param key The attribute is stored in the CellView with this key
 * @return Object stored with the given key in the given CellView
 */
    private Object getAttribute(CellView view, String key){
        return view.getAttributes().get(key);
    }

/******************************************************************************/
/**
 * After the calculation of the new Layout for a graph, the cells of the graph
 * are positioned somewhere on the drawing space. They even might have negative
 * coordinates. To prevent from this, this method is called, everytime before
 * {@link #applyChanges()} is called. This method moves the whole graph to the
 * upper left corner. No cell will have negative x- or y-coordinates. 
 */
    private void moveGraphToNW(){
        Point2D.Double firstPos = getPosition((CellView)cellList.get(0));
        double minX = firstPos.x;
        double minY = firstPos.y;
        double maxX = minX;
        double maxY = minY;
        
        for( int i = 0; i < cellList.size(); i++ ){
            CellView view = (CellView) cellList.get(i);
            Point2D.Double viewPos = getPosition((CellView)cellList.get(i));
            Rectangle viewBounds = new Rectangle(view.getBounds());
            if( viewPos.getX() < minX ){
                minX = viewPos.getX();
            }
            else if( viewPos.getX()+viewBounds.getWidth() > maxX ){
                maxX = viewPos.getX()+viewBounds.getWidth();
            }
            if( viewPos.getY() < minY ){
                minY = viewPos.getY();
            }
            else if( viewPos.getY()+viewBounds.getHeight() > maxY ){
                maxY = viewPos.getY()+viewBounds.getHeight();
            }
        }
        
        minX -= 50;
        minY -= 50;
        
        for( int i = 0; i < cellList.size(); i++ ){
            CellView view = (CellView) cellList.get(i);
            
            Point2D.Double pos = getPosition(view);
            
            setPosition(view,new Point2D.Double(pos.x-minX,
                                                pos.y-minY));                                                        
        }   
                    
    }

/******************************************************************************/
/**
 * Retrieves the Cells that are directly connected to the given Cell and
 * member of the given list.
 * @param list Only relatives from this List are allowed
 * @param view Relatives from this view are requested
 * @return Relatives from view that are in the list
 * @see #getRelatives(CellView)
 */
    protected ArrayList getRelativesFrom(ArrayList list, CellView view){
        ArrayList relatives = getRelatives(view);
        ArrayList result    = new ArrayList();
        for( int i = 0; i < relatives.size(); i++ )
            if( list.contains(relatives.get(i)) )
                result.add(relatives.get(i));
        return result;
    }
        
/******************************************************************************/
/**
 * Retrieves all Cells that have an edge with the given Cell.
 * @param view Cell, the relatives are requested from
 * @return Relatives of view
 */
    protected ArrayList getRelatives(CellView view){    
        
        if( view.getAttributes().containsKey(KEY_RELATIVES) )
            return (ArrayList) view.getAttributes().get(KEY_RELATIVES);
            
        ArrayList relatives = new ArrayList();
        ArrayList portsCells = new ArrayList();
        
        VertexView vertexView = (VertexView)view;
        
        if( isCluster(view) ){
            
            ArrayList clusteredVertices = (ArrayList) vertexView.getAttributes().get(KEY_CLUSTERED_VERTICES);
            for( int i = 0; i < clusteredVertices.size(); i++ ){
                ArrayList clusterRelatives = getRelatives((CellView)clusteredVertices.get(i));
                for( int j = 0; j < clusterRelatives.size(); j++ )
                    if( !relatives.contains(clusterRelatives.get(j)) &&
                        !clusteredVertices.contains(clusterRelatives.get(j)) ){
                        relatives.add(clusterRelatives.get(j));
                    }
            }
            
        }
        else {
        
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
                                               model.getParent(nextPort), false);
                    relatives.add(nextVertex);
                }
            }
        }
        
        view.getAttributes().put(KEY_RELATIVES,relatives);
        return relatives;                
    }

/******************************************************************************/
/**
 * When Cells are inserted and a update of the layout is desired, this method
 * defines the initial positions for all cells, the already layouted cells and
 * the inserted. The already layouted cells get their previos calculated 
 * position, gained from their bounds. The inserted Cells are positioned
 * recursivly. The inserted Cells, that have at least one relative in 
 * {@link #cellList} are placed in the barycenter of the relatives. After this,
 * the inserted Cells, with a new position are added to {@link #cellList}.
 * This is done, until all inserted Cells are in {@link #cellList}.
 * 
 * @param viewList List of the inserted Cells
 * @see #arrangeLayoutUpdateInsertedCellsPlacement(ArrayList)
 */
    private void arrangeLayoutUpdateInsertPlacement(CellView[] viewList){
        
        //preinitialisation - init positions for all known vertexViews
        for( int i = 0; i < cellList.size(); i++ ){
            CellView view = (CellView) cellList.get(i);
            if( !view.getAttributes().containsKey(KEY_POSITION) ){
                Point2D.Double pos = new Point2D.Double(
                                                 view.getBounds().getCenterX(),
                                                 view.getBounds().getCenterY());
                view.getAttributes().put(KEY_POSITION,pos);
            }
        }
        
        ArrayList placableCells = new ArrayList();
        for( int i = 0; i < viewList.length; i++ )
           placableCells.add(viewList[i]);
        
        arrangeLayoutUpdateInsertedCellsPlacement(placableCells);
/*        
        //puts the view in the barycenter of the relatives, if there are any
        for( int i = 0; i < viewList.length; i++ )
        
            if( viewList[i] instanceof VertexView ){
                
                ArrayList relatives = getRelativesFrom(cellList,viewList[i]);
                
                if( relatives.size() != 0 ){
                
                    double sumX = 0.0;
                    double sumY = 0.0;
                    for( int j = 0; j < relatives.size(); j++ ){
                        Point2D.Double pos = (Point2D.Double)
                                                 ((CellView)relatives.get(j)).
                                                        getAttributes().
                                                            get(KEY_POSITION);
                        sumX += pos.x;
                        sumY += pos.y;
                    }
                    Point2D.Double randomVector = new Point2D.Double(Math.cos(Math.random()*2.0*Math.PI)*10.0,
                                                                     Math.sin(Math.random()*2.0*Math.PI)*10.0);
                    viewList[i].getAttributes().put(KEY_POSITION,
                                           new Point2D.Double(
                                               (sumX/(double)relatives.size())+randomVector.x,
                                               (sumY/(double)relatives.size())+randomVector.y));
                }
                else {
                    viewList[i].getAttributes().put(KEY_POSITION,
                                                    new Point2D.Double(
                                                        0.0,
                                                        0.0));
                }
            }*/
    }

/******************************************************************************/
/**
 * Recursive method for finding the initial position for inserted cells. The
 * inserted cells are checked, whether there is at leased one of the relatives
 * in {@link #cellList}. If there is any, the cells are positioned in the
 * barycenter of the relatives. If there is only one relative, this means, the
 * inserted CellViews are positioned exactly on the position of the relative.
 * Cells with no relative in {@link #cellList} are stored in a list. After all
 * Cells are visited and checked, all positioned cells are added to 
 * {@link #cellList}. Then, while the list with the non positioned Cells is
 * not empty, the method is called recursivly again. This is done, until all
 * inserted cells are positioned or no relatives could be found for all left
 * Cells (that causes that the left cells are positioned in the upper left 
 * corner).
 * 
 * @param placableCells A List of CellViews, that have to be placed in the 
 * barycenter of their relatives
 * @see #arrangeLayoutUpdateInsertPlacement(CellView[])
 * @see #graphChanged(GraphModelEvent)  
 */
    private void arrangeLayoutUpdateInsertedCellsPlacement(ArrayList placableCells){
        ArrayList notPlacedCells = new ArrayList();
        for( int i = 0; i < placableCells.size(); i++ ){
        
            CellView view = (CellView) placableCells.get(i);
            
            if( view instanceof VertexView ){
                
                ArrayList relatives = getRelativesFrom(cellList,view);
                
                if( relatives.size() != 0 ){
                
                    double sumX = 0.0;
                    double sumY = 0.0;
                    for( int j = 0; j < relatives.size(); j++ ){
                        Point2D.Double pos = (Point2D.Double)
                                                 ((CellView)relatives.get(j)).
                                                        getAttributes().
                                                            get(KEY_POSITION);
                        sumX += pos.x;
                        sumY += pos.y;
                    }
                    Point2D.Double randomVector = new Point2D.Double(Math.cos(Math.random()*2.0*Math.PI)*10.0,
                                                                     Math.sin(Math.random()*2.0*Math.PI)*10.0);
                    view.getAttributes().put(KEY_POSITION,
                           new Point2D.Double(
                               (sumX/(double)relatives.size())+randomVector.x,
                               (sumY/(double)relatives.size())+randomVector.y));
                    
                }
                else {
                    notPlacedCells.add(view);
                }
            }
        }
        
        for( int i = 0; i < placableCells.size(); i++ ){
            if( placableCells.get(i) != null )
                if( ((CellView) placableCells.get(i)).getAttributes() != null )
                    if( ((CellView) placableCells.get(i)).getAttributes().containsKey(KEY_POSITION) )
                        cellList.add(placableCells.get(i));
        }
        
        
        if( notPlacedCells.size() != placableCells.size() ){
            
            arrangeLayoutUpdateInsertedCellsPlacement(notPlacedCells);
        }
        else {
            for( int i = 0; i < notPlacedCells.size(); i++ ){
                CellView view = (CellView) notPlacedCells.get(i);
                if( !view.getAttributes().containsKey(KEY_POSITION) )
                    view.getAttributes().put(KEY_POSITION,
                                             new Point2D.Double(0.0,0.0));                
            
            }
        }
        for( int i = 0; i < cellList.size(); i++ )
            if( ((CellView)cellList.get(i)).getAttributes().get(KEY_POSITION) == null )
                System.err.println("WHATCH OUT!!! NODE "+i+" == NULL");
            
    }

/******************************************************************************/
/**
 * Decides in a layout update process, what cells are member of 
 * {@link #applyCellList}. This depends on the configuration of the layout 
 * update method. First, regardless which layout update method was chosen, all
 * inserted cells, gained as parameter, are added. Then, when the perimeter
 * method is chosen, the cells are counted, which position is in the basic
 * perimeter radius around an inserted cell. That number multiplied with the
 * perimeter radius increase are added to the basic perimeter radius. Every
 * Cell, that was not inserted but is positioned in that radius, is added to
 * {@link #applyCellList}. After that, if perimeter method or neighbor method
 * is choosen, the relatives up to {@link #luRecursionDepth} away of the 
 * inserted cells are added to {@link #applyCellList}.
 * 
 * @param viewList Array of the inserted CellView's (includes EdgeView)
 * @see #graphChanged(GraphModelEvent) 
 */
    private void getLayoutUpdateCells(CellView[] viewList){
        //adds all inserted views
        for( int i = 0; i < viewList.length; i++ ){
            if( viewList[i] instanceof VertexView ){
                if( !applyCellList.contains(viewList[i]) )
                    applyCellList.add(viewList[i]);
                if( !cellList.contains(viewList[i]) )
                    cellList.add(viewList[i]);
            }
            else if( viewList[i] instanceof EdgeView &&
                viewList[i] != null ){
                if( !edgeList.contains(viewList[i]) ){
                    edgeList.add(viewList[i]);
                    System.out.println("edge added");
                }
            }
        }
        //now all vertices (old and new) are in cellList & all edges in edgeList
        
        //adds all known cells in a perimeter
        if( AnnealingLayoutController.KEY_LAYOUT_UPDATE_METHOD_PERIMETER.
            equals(luMethod)){
            
            //precalculation of perimeters
            ArrayList perimeterList = new ArrayList();
            for( int i = 0; i < applyCellList.size(); i++ ){
                VertexView vertex = (VertexView) applyCellList.get(i);
                Point2D.Double pos = (Point2D.Double) vertex.
                                              getAttributes().get(KEY_POSITION);
                int intersectionCount = 0;
                for( int j = 0; j < applyCellList.size(); j++ ){
                    if( i != j ){
                        VertexView uVertex = (VertexView) applyCellList.get(j);
                        Point2D.Double uPos = (Point2D.Double) uVertex.
                                              getAttributes().get(KEY_POSITION);
                        if( pos.distance(uPos) < luPerimeterRadius )
                            intersectionCount++;//counting inserted cells in perimeter
                    }
                    
                }
                perimeterList.add(new Ellipse2D.Double(
                          pos.x - (luPerimeterRadius + ((double)intersectionCount * luPerimeterRadiusInc)),
                          pos.y - (luPerimeterRadius + ((double)intersectionCount * luPerimeterRadiusInc)), 
                          2.0 * (luPerimeterRadius + ((double)intersectionCount * luPerimeterRadiusInc)), 
                          2.0 * (luPerimeterRadius + ((double)intersectionCount * luPerimeterRadiusInc))));
            }
            //adding all members of cellList within a perimeter to applyCellList
            for( int i = 0; i < cellList.size(); i++ ){
                VertexView vertex = (VertexView) cellList.get(i);
                Point2D.Double pos = (Point2D.Double) vertex.getAttributes().get(KEY_POSITION);
                for( int j = 0; j < perimeterList.size(); j++ ){
                    Ellipse2D.Double perimeter = (Ellipse2D.Double) 
                                                           perimeterList.get(j);
                    Point2D.Double center = new Point2D.Double(
                                                        perimeter.getCenterX(),
                                                        perimeter.getCenterY());
                    double radius = perimeter.getCenterX() - perimeter.getX();
                    
                    if( center.distance(pos) < radius )
                        if( !applyCellList.contains(vertex) )
                            applyCellList.add(vertex);                        
                }
            }
        }
        if( luRecursionDepth > 0 ){
            
            int vertexCount = 0;
            for( int i = 0; i < viewList.length; i++ )
                if( viewList[i] instanceof VertexView )
                    vertexCount++;
                    
            VertexView[] vertexList = new VertexView[vertexCount];
            vertexCount = 0;
            for( int i = 0; i < viewList.length; i++ )
                if( viewList[i] instanceof VertexView )
                    vertexList[vertexCount++] = (VertexView) viewList[i];
            
            addRelativesToList(vertexList,luRecursionDepth);
        }
    }

/******************************************************************************/
/**
 * Recursive method, to add relatives to {@link #applyCellList}, that are
 * maximal a given pathlength away of the views in the given Array.
 * 
 * @param vertexList Array of the VertexView's, which relatives should be
 * added to {@link #applyCellList}, if they are whithin a given pathlength
 * away of the VertexViews
 * @param depth Pathlength, relatives could be away of the VertexViews
 * @see #getLayoutUpdateCells(CellView)
 * @see #graphChanged(GraphModelEvent)
 */
    private void addRelativesToList(VertexView[] vertexList, int depth){
        
        if( vertexList == null )     return;
        if( vertexList.length == 0 ) return;
        if( depth == 0 )             return;
        
        for( int i = 0; i  < vertexList.length; i++ ){
                
            ArrayList relatives = getRelatives(vertexList[i]);
            VertexView[] relativeList = new VertexView[relatives.size()];
            
            for( int j = 0; j < relatives.size(); j++ ){
                if( !applyCellList.contains(relatives.get(j)) ){
                    applyCellList.add(relatives.get(j));
//                    showCell((CellView)relatives.get(j),new Color(0,180,180));
                }
                if( !cellList.contains(relatives.get(j)) )
                    cellList.add(relatives.get(j));
                relativeList[j] = (VertexView) relatives.get(j);
            }
                
            addRelativesToList(relativeList,depth-1);
        }
    }

/******************************************************************************/
/**
 * When a event reaches this method, it will be scanned, if there are
 * Cells removed or Cells inserted. When there are Cells removed from the graph,
 * they have to be removed from {@link #cellList}, {@link #edgeList} and from
 * {@link #applyCellList}. If there are Cells added, the layout update process
 * starts. This triggers the algorithm to try to find a suitable layout for
 * the inserted cells, by layouting them and some of the cells, available in
 * {@link #cellList}. The algorithm tries to stimulate the cells from 
 * {@link #cellList} to make place for the layout of the inserted Cells. 
 */
    public void graphChanged(GraphModelEvent e){
        
        if( !isRunning ){
            isRunning = true;
        
            Object[] vertexIns = e.getChange().getInserted();
            Object[] vertexRem = e.getChange().getRemoved();
        
        
            //Insert - Action
            if( vertexIns != null && vertexRem == null ){
                
                if( vertexIns.length == 0 ){
                    isRunning = false;
                    return;
                }
                                                       
                CellView[] viewList = jgraph.getGraphLayoutCache().getMapping(
                                                               vertexIns,false);
                if( viewList.length == 0 ){
                    isRunning = false;
                    return;
                }
                                
                applyCellList.clear();
                
                loadConfiguration(CONFIG_KEY_LAYOUT_UPDATE);
                
                
                //enables a workaround if a known bug is still present
                boolean bugPresent = false;
                for( int i = 0; i < viewList.length; i++ )
                    if( viewList[i] == null ){
                        bugPresent = true;
                        break;                                
                    }
                
                if( bugPresent )
                    getAllEdges();
            
                arrangeLayoutUpdateInsertPlacement(viewList);
                getLayoutUpdateCells(viewList);
            
                if( applyCellList.size() == 0 ){
                    isRunning = false;
                    return;
                }
            
                round = 0;
                initProgressValue = 0;
                dlgProgress.setValue(initProgressValue);
                dlgProgress.setVisible(true);
                
                if( isClusteringEnabled )
                    clusterGraph();


                //algorithm start                
                
                init(false);
                boolean isCanceled = run();
                
                //algorithm end

                
                if( isClusteringEnabled )
                    declusterGraph();
                    
                if( !isCanceled )
                    applyChanges();
                removeTemporaryData();
                
                dlgProgress.setVisible(false);                
            }
            //Remove - Action
            else if( vertexIns == null && vertexRem != null ){

                isRunning = true;
            
                CellView[] viewList = jgraph.getGraphLayoutCache().getMapping(
                                                               vertexRem,false);

            
                for( int i = 0; i < viewList.length; i++ )
            
                    if( viewList[i] instanceof VertexView ){
                    
                        if( applyCellList.contains(viewList[i]) )
                            applyCellList.remove(viewList[i]);
                        if( cellList.contains(viewList[i]) )
                            cellList.remove(viewList[i]);
                    }
                    else if( viewList[i] instanceof EdgeView ){
// as long as graphChanged get no inserted Edges, this lines should stay
// commented out.                    
//                        if( edgeList.contains(viewList[i]) )
//                            edgeList.remove(viewList[i]);
                    }    
            }
            isRunning = false;
        }
    }

/******************************************************************************/
/**
 * Debugging Method. Shows the given CellView with the given Color
 * 
 * @param view CellView, that should be displayed
 * @param color New background color of the CellView
 */
    private void showCell(CellView view, Color color){
        
        Map viewMap = new Hashtable();
        
        Point2D.Double pos  = getPosition(view);
        Rectangle r = view.getBounds();

        r.x = (int) (pos.getX() - (double)r.getWidth() /2.0);
        r.y = (int) (pos.getY() - (double)r.getHeight()/2.0);
            
        Object cell = view.getCell();
  
        Map attributes = GraphConstants.createMap();

        GraphConstants.setBackground(attributes, color);
        GraphConstants.setBounds    (attributes, r);
            

        viewMap.put(cell, attributes);          
        
        jgraph.getGraphLayoutCache().edit(viewMap,null,null,null);        
    }

/******************************************************************************/
    
    private void colorizeClusters(ArrayList clusterList){
        Color[] colorList = new Color[]{Color.black, Color.magenta, Color.yellow, Color.blue, Color.green, Color.gray, Color.cyan, Color.red, Color.darkGray, Color.lightGray, Color.orange, Color.pink};
        for( int i = 0; i < clusterList.size(); i++ ){
            if( i < colorList.length ){
                ArrayList clusteredVertices = (ArrayList)((VertexView)clusterList.get(i)).getAttributes().get(KEY_CLUSTERED_VERTICES);
                showCellList(clusteredVertices,colorList[i]);
            }
            else break;
        }
    }

/******************************************************************************/
/**
 * Debugging Method. Shows all Cells in {@link #applyCellList} with their
 * temporary positions.
 */
    private void showCellList(ArrayList list, Color color){
        
        Map viewMap = new Hashtable();
        
        for( int i = 0; i < list.size(); i++ ){
            
            CellView view = (CellView)list.get(i);
            Point2D.Double pos  = getPosition(view);
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
/**
 * Workaround for a BUG. When 
 * {@link #graphChanged(GraphModelEvent) graphChanged(...)} is called, the 
 * method gets via myGraphModelEvent.getChanged().getInserted() an array of
 * objects. This array consists of the key's to the views inserted into the 
 * graph. When this views are gained, the BUG appears. The array gained from
 * the GraphLayoutCache contains only VertexView's. Instead of the EdgeViews 
 * there is NULL in the array. This method is callen if this BUG appears, in the
 * hope, to get the inserted edges.  
 */
    private void getAllEdges(){
        Object[] cells = jgraph.getRoots();
        
        CellView[] views = jgraph.getGraphLayoutCache().getMapping(cells,false);
        
        for( int i = 0; i < views.length; i++ ){
            if( views[i] instanceof VertexView ){
                VertexView vertexView = (VertexView) views[i];
                GraphModel model = vertexView.getModel();
                CellMapper mapper = vertexView.getMapper() ;
                Object vertexCell = vertexView.getCell() ;
                ArrayList portsCells = new ArrayList();
            
                for (int j = 0; j < model.getChildCount(vertexCell); j++){
                    Object portCell = model.getChild(vertexCell, j);
                    portsCells.add(portCell);
                }
                for( int j = 0; j < portsCells.size(); j++ ){

                    Object portCell = portsCells.get(j);

                    Iterator edges = model.edges(portCell);
                    
                    while (edges.hasNext() ){            
                
                        Object edge = edges.next() ;
                    
                        Object e = mapper.getMapping(edge,false);
                        if( !edgeList.contains(e) &&
                            e != null){
                            edgeList.add(e);
                        }
                    }
                }
            }
            else if( views[i] instanceof EdgeView ){
                if( !edgeList.contains(views[i]) &&
                    views[i] != null ){
                    edgeList.add(views[i]);
                }
            }
        }
    }
    
/******************************************************************************/
/**
 * Debugging Method. Stops the running layouting process and allows a short look
 * on the actual calculations, especialy by calling 
 * {@link #showCell(CellView,Color)} or {@link #showApplyCellList()} before.
 * 
 * @param ms Time, the algorithm should wait in milliseconds.
 */
    private synchronized void stop(long ms){
        try{
            wait(ms);
        }
        catch( Exception e ){
            e.printStackTrace();
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
        int maxClusters = Math.max((int)((double)(cellList.size() - applyCellList.size()) / clusteringFactor ),2);
    
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
                clusterPos = getPosition((VertexView)clusterList.get(j));
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
/*        colorizeClusters(clusterList);
        stop(20000);*/
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
            
            Point2D.Double pos = getPosition(view);
            sumX += pos.x;
            sumY += pos.y;
        }
        return new Point2D.Double(sumX/((double)list.size()),
                                   sumY/((double)list.size()));
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
            
            Point2D.Double vertexPos = getPosition((VertexView)verticeList.get(0));
            Dimension vertexSize = ((CellView)verticeList.get(0)).getBounds().getSize();
            
            double minX = vertexPos.getX();
            double minY = vertexPos.getX();
            double maxX = vertexPos.getX()+vertexSize.getWidth();
            double maxY = vertexPos.getX()+vertexSize.getHeight();
            
            for( int i = 1; i < verticeList.size(); i++ ){
                
                vertexPos  = getPosition((VertexView)verticeList.get(i));
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

    private Rectangle getBoundingBox(){
        return getBoundingBox(cellList);
    }

/******************************************************************************/

}
