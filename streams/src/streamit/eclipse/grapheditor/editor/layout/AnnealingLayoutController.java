package streamit.eclipse.grapheditor.editor.layout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author winkler
 * @version 1.0
 * Date of creation: 11.04.2003 - 12:40:48
 */
public class AnnealingLayoutController implements LayoutController {
    
    public final static String KEY_TITLE                    = "Simulated Annealing";
    
    public final static String KEY_CONFIG_NAME            = "CONFIG_NAME";
    public final static String KEY_INIT_TEMPERATURE       = "Start Temperature";
    public final static String KEY_MIN_TEMPERATURE        = "min. Temperature";
    public final static String KEY_MIN_DISTANCE           = "min. Distance";
    public final static String KEY_TEMP_SCALE_FACTOR      = "Temperature Scalefactor";
    public final static String KEY_COMPUTE_PERMUTATION    = "should compute per Permutation";
    public final static String KEY_IS_UPHILL_MOVE_ALLOWED = "are Uphill-Moves allowed";
    public final static String KEY_MAX_ROUNDS             = "max. Rounds";
    public final static String KEY_TRIES_PER_CELL         = "tries per cell";
    public final static String KEY_COST_FUNCTION_CONFIG   = "Costfunction Config";
    public final static String KEY_LAMBDA                 = "Lambda";
    public final static String KEY_BOUNDS                 = "Bounds of resulting graph";
    
    public final static String KEY_LAYOUT_UPDATE_INIT_TEMPERATURE       = "Layout Update Start Temperature";
    public final static String KEY_LAYOUT_UPDATE_MIN_TEMPERATURE        = "Layout Update min. Temperature";
    public final static String KEY_LAYOUT_UPDATE_MIN_DISTANCE           = "Layout Update min. Distance";
    public final static String KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR      = "Layout Update Temperature Scalefactor";
    public final static String KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION    = "Layout Update should compute per Permutation";
    public final static String KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED = "Layout Update are Uphill-Moves allowed";
    public final static String KEY_LAYOUT_UPDATE_MAX_ROUNDS             = "Layout Update max. Rounds";
    public final static String KEY_LAYOUT_UPDATE_TRIES_PER_CELL         = "Layout Update tries per cell";
    public final static String KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG   = "Layout Update Costfunction Config";
    public final static String KEY_LAYOUT_UPDATE_LAMBDA                 = "Layout Update Lambda";
    public final static String KEY_LAYOUT_UPDATE_BOUNDS                 = "Layout Update Bounds of resulting graph";
    public final static String KEY_LAYOUT_UPDATE_METHOD                 = "Layout Update Method";
    
    public final static String KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY  = "Layout Update Method Neighbors only";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETER       = "Layout Update Method Perimeter";
    
    public final static String KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH = "Layout Update Method Neighbors depth";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS= "Layout Update Method Perimeter radius";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE = "Layout Update Method Perimeter radius increase";
    
    public final static String KEY_LAYOUT_UPDATE_ENABLED                = "Layout Update enabled";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED     = "Layout Update clustering enabled";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR      = "Layout Update clustering factor";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE  = "Layout Update clustering move scaling factor";
    
    public final static int COUT_COSTFUNCTION = 6;
    
    public final static int COSTFUNCTION_EDGE_DISTANCE     =  1;
    public final static int COSTFUNCTION_EDGE_CROSSING     =  2;
    public final static int COSTFUNCTION_EDGE_LENGTH       =  4;
    public final static int COSTFUNCTION_BORDERLINE        =  8;
    public final static int COSTFUNCTION_NODE_DISTRIBUTION = 16;
    public final static int COSTFUNCTION_NODE_DISTANCE     = 32;
    
    
    protected Properties[] config;
    protected AnnealingLayoutConfigurationDialog dialog;
    private boolean isOptimizer = false;
    
	/**
	 * Constructor for SimulatedAnnealingController.
	 */
	public AnnealingLayoutController() {
        
        config = new Properties[2];
        
        config[0] = new Properties();
        config[0].put(KEY_CONFIG_NAME           ,"Default Values");
        config[0].put(KEY_INIT_TEMPERATURE      , "300.0");
        config[0].put(KEY_MIN_TEMPERATURE       ,   "2.0");
        config[0].put(KEY_MIN_DISTANCE          ,  "50.0");
        config[0].put(KEY_TEMP_SCALE_FACTOR     ,   "0.95");
        config[0].put(KEY_COMPUTE_PERMUTATION   ,"true");
        config[0].put(KEY_IS_UPHILL_MOVE_ALLOWED,"true");
        config[0].put(KEY_MAX_ROUNDS            ,  "10000");
        config[0].put(KEY_TRIES_PER_CELL        ,  "8");
        config[0].put(KEY_COST_FUNCTION_CONFIG  ,"111110");
        config[0].put(KEY_BOUNDS                ,   "0.0");
        
        ArrayList lambda1 = new ArrayList();
        lambda1.add(new Double(1000.0));
        lambda1.add(new Double(100000.0));
        lambda1.add(new Double(0.02));
        lambda1.add(new Double(2000.0));
        lambda1.add(new Double(150.0));
        lambda1.add(new Double(1000000.0));
        config[0].put(KEY_LAMBDA                ,lambda1);
        
        Rectangle bounds1 = new Rectangle(0,0,1000,700);
        config[0].put(KEY_BOUNDS                ,bounds1);


        config[0].put(KEY_LAYOUT_UPDATE_ENABLED               ,"true");
        config[0].put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      , "40.0");
        config[0].put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,   "2.0");
        config[0].put(KEY_LAYOUT_UPDATE_MIN_DISTANCE          ,  "50.0");
        config[0].put(KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR     ,   "0.95");
        config[0].put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        config[0].put(KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED,"true");
        config[0].put(KEY_LAYOUT_UPDATE_MAX_ROUNDS            ,  "10000");
        config[0].put(KEY_LAYOUT_UPDATE_TRIES_PER_CELL        ,  "8");
        config[0].put(KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG  ,"111110");
        config[0].put(KEY_LAYOUT_UPDATE_METHOD                ,KEY_LAYOUT_UPDATE_METHOD_PERIMETER);
        config[0].put(KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH,"1");
        config[0].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS,"100.0");
        config[0].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE,"20.0");
        
        ArrayList lambdaLU1 = new ArrayList();
        lambdaLU1.add(new Double(1000.0));
        lambdaLU1.add(new Double(100000.0));
        lambdaLU1.add(new Double(0.02));
        lambdaLU1.add(new Double(2000.0));
        lambdaLU1.add(new Double(150.0));
        lambdaLU1.add(new Double(1000000.0));
        config[0].put(KEY_LAYOUT_UPDATE_LAMBDA                ,lambdaLU1);
        
        Rectangle boundsLU1 = new Rectangle(0,0,1000,700);
        config[0].put(KEY_LAYOUT_UPDATE_BOUNDS                ,boundsLU1);
        config[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED,"true");
        config[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,"8.0");
        config[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,"0.1");
        
        
        
        config[1] = new Properties();
        config[1].put(KEY_CONFIG_NAME           ,"Heavy Values");
        config[1].put(KEY_INIT_TEMPERATURE      , "500.0");
        config[1].put(KEY_MIN_TEMPERATURE       ,   "0.5");
        config[1].put(KEY_MIN_DISTANCE          ,  "50.0");
        config[1].put(KEY_TEMP_SCALE_FACTOR     ,   "0.97");
        config[1].put(KEY_COMPUTE_PERMUTATION   ,"true");
        config[1].put(KEY_IS_UPHILL_MOVE_ALLOWED,"true");
        config[1].put(KEY_MAX_ROUNDS            ,  "10000");
        config[1].put(KEY_TRIES_PER_CELL        ,  "8");
        config[1].put(KEY_COST_FUNCTION_CONFIG  ,"111111");
        config[1].put(KEY_BOUNDS                ,   "0.0");
        
        ArrayList lambda2 = new ArrayList();
        lambda2.add(new Double(1000.0));
        lambda2.add(new Double(100000.0));
        lambda2.add(new Double(0.02));
        lambda2.add(new Double(2000.0));
        lambda2.add(new Double(150.0));
        lambda2.add(new Double(1000000.0));
        config[1].put(KEY_LAMBDA                ,lambda2);
        
        Rectangle bounds2 = new Rectangle(0,0,1000,700);
        config[1].put(KEY_BOUNDS                ,bounds2);


        config[1].put(KEY_LAYOUT_UPDATE_ENABLED               ,"true");
        config[1].put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      , "40.0");
        config[1].put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,   "2.0");
        config[1].put(KEY_LAYOUT_UPDATE_MIN_DISTANCE          ,  "50.0");
        config[1].put(KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR     ,   "0.97");
        config[1].put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        config[1].put(KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED,"true");
        config[1].put(KEY_LAYOUT_UPDATE_MAX_ROUNDS            ,  "10000");
        config[1].put(KEY_LAYOUT_UPDATE_TRIES_PER_CELL        ,  "8");
        config[1].put(KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG  ,"111111");
        config[1].put(KEY_LAYOUT_UPDATE_METHOD                ,KEY_LAYOUT_UPDATE_METHOD_PERIMETER);
        config[1].put(KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH,"2");
        config[1].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS,"200.0");
        config[1].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE,"40.0");
        
        ArrayList lambdaLU2 = new ArrayList();
        lambdaLU2.add(new Double(1000.0));
        lambdaLU2.add(new Double(100000.0));
        lambdaLU2.add(new Double(0.02));
        lambdaLU2.add(new Double(2000.0));
        lambdaLU2.add(new Double(150.0));
        lambdaLU2.add(new Double(1000000.0));
        config[1].put(KEY_LAYOUT_UPDATE_LAMBDA                ,lambdaLU2);
        
        Rectangle boundsLU2 = new Rectangle(0,0,1000,700);
        config[1].put(KEY_LAYOUT_UPDATE_BOUNDS                ,boundsLU2);
        config[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED,"true");
        config[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,"12.0");
        config[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,"0.2");
        
//        dialog = new AnnealingLayoutConfigurationDialog(null,config,false);        
	}
    
    public AnnealingLayoutController(boolean isOptimizingAlgorithm){
        this();
        isOptimizer = isOptimizingAlgorithm;
//        dialog = new AnnealingLayoutConfigurationDialog(null,config,isOptimizer);
    }
    
    public void setConfigurationDialog(AnnealingLayoutConfigurationDialog diag){
        dialog = diag;
    }

	/**
	 * @see org.jgraph.layout.LayoutController#isConfigurable()
	 */
	public boolean isConfigurable() {
		return true;
	}

	/**
	 * @see org.jgraph.layout.LayoutController#configure()
	 */
	public void configure() {
        if( dialog == null )
            dialog = new AnnealingLayoutConfigurationDialog(null,config,isOptimizer);
        dialog.validate();
        dialog.setVisible(true);
        dialog.pack();
	}

	/**
	 * @see org.jgraph.layout.LayoutController#getConfiguration()
	 */
	public Properties getConfiguration() {
        if( dialog == null )
            dialog = new AnnealingLayoutConfigurationDialog(null,config,isOptimizer);
        Properties config = new Properties();
        config.put("CAPTION"                ,KEY_TITLE);
        config.put(KEY_INIT_TEMPERATURE      ,String.valueOf(dialog.getInitTemperature()));
        config.put(KEY_MIN_TEMPERATURE       ,String.valueOf(dialog.getMinTemperature()));
        config.put(KEY_MIN_DISTANCE          ,String.valueOf(dialog.getMinDistance()));
        config.put(KEY_TEMP_SCALE_FACTOR     ,String.valueOf(dialog.getTemperatureScaleFactor()));
        config.put(KEY_COMPUTE_PERMUTATION   ,String.valueOf(dialog.getComputePermutation()));
        config.put(KEY_IS_UPHILL_MOVE_ALLOWED,String.valueOf(dialog.getUphillMovesAllowed()));
        config.put(KEY_MAX_ROUNDS            ,String.valueOf(dialog.getMaxRounds()));
        config.put(KEY_TRIES_PER_CELL        ,String.valueOf(dialog.getTriesPerCell()));
        config.put(KEY_COST_FUNCTION_CONFIG  ,String.valueOf(Integer.toBinaryString(dialog.getCostFunctionConfiguration())));
        config.put(KEY_LAMBDA                ,dialog.getLambda());
        config.put(KEY_BOUNDS                ,dialog.getResultBounds());
        
        config.put(KEY_LAYOUT_UPDATE_ENABLED               ,String.valueOf(dialog.getLayoutUpdateEnabled()));
        config.put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      ,String.valueOf(dialog.getLayoutUpdateInitTemperature()));
        config.put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,String.valueOf(dialog.getLayoutUpdateMinTemperature()));
        config.put(KEY_LAYOUT_UPDATE_MIN_DISTANCE          ,String.valueOf(dialog.getLayoutUpdateMinDistance()));
        config.put(KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR     ,String.valueOf(dialog.getLayoutUpdateTemperatureScaleFactor()));
        config.put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,String.valueOf(dialog.getLayoutUpdateComputePermutation()));
        config.put(KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED,String.valueOf(dialog.getLayoutUpdateUphillMovesAllowed()));
        config.put(KEY_LAYOUT_UPDATE_MAX_ROUNDS            ,String.valueOf(dialog.getLayoutUpdateMaxRounds()));
        config.put(KEY_LAYOUT_UPDATE_TRIES_PER_CELL        ,String.valueOf(dialog.getLayoutUpdateTriesPerCell()));
        config.put(KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG  ,String.valueOf(Integer.toBinaryString(dialog.getLayoutUpdateCostFunctionConfiguration())));
        config.put(KEY_LAYOUT_UPDATE_LAMBDA                ,dialog.getLayoutUpdateLambda());
        config.put(KEY_LAYOUT_UPDATE_BOUNDS                ,dialog.getLayoutUpdateResultBounds());
        config.put(KEY_LAYOUT_UPDATE_METHOD                ,dialog.getLayoutUpdateMethod());
        config.put(KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH,String.valueOf(dialog.getLayoutUpdateMethodNeighborsDepth()));
        config.put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS,String.valueOf(dialog.getLayoutUpdateMethodPerimeterRadius()));
        config.put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE,String.valueOf(dialog.getLayoutUpdateMethodPerimeterRadiusIncrease()));
        config.put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED,   String.valueOf(dialog.getLayoutUpdateClusteringEnabled()));
        config.put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR,    String.valueOf(dialog.getLayoutUpdateClusteringFactor()));
        config.put(KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE,String.valueOf(dialog.getLayoutUpdateClusteringMoveScaleFactor()));
        
        addAdditionalConfigurationData(config);
        
		return config;
	}
    
    protected void addAdditionalConfigurationData(Properties config){
    }
    
    
    public void setConfiguration(Properties config){
        dialog.setInitTemperature(
            Double.parseDouble(
                (String)config.get(KEY_INIT_TEMPERATURE)));
        dialog.setMinTemperature(
            Double.parseDouble(
                (String)config.get(KEY_MIN_TEMPERATURE)));
        dialog.setMinDistance(
            Double.parseDouble(
                (String)config.get(KEY_MIN_DISTANCE)));
        dialog.setTemperatureScaleFactor(
            Double.parseDouble(
                (String)config.get(KEY_TEMP_SCALE_FACTOR)));
        dialog.setComputePermutation(
            isTrue(
                (String)config.get(KEY_COMPUTE_PERMUTATION)));
        dialog.setUphillMovesAllowed(
            isTrue(
                (String)config.get(KEY_IS_UPHILL_MOVE_ALLOWED)));
        dialog.setMaxRounds(
            Integer.parseInt(
                (String)config.get(KEY_MAX_ROUNDS)));
        dialog.setTriesPerCell(
            Integer.parseInt(
                (String)config.get(KEY_TRIES_PER_CELL)));
        dialog.setCostFunctionConfiguration(
            Integer.parseInt(
                (String)config.get(KEY_COST_FUNCTION_CONFIG),2));
        dialog.setLambda((ArrayList)config.get(KEY_LAMBDA));
        dialog.setResultBounds((Rectangle)config.get(KEY_BOUNDS));
        

        dialog.setLayoutUpdateEnabled(isTrue((String)config.get(KEY_LAYOUT_UPDATE_ENABLED)));
        dialog.setLayoutUpdateInitTemperature(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE)));
        dialog.setLayoutUpdateMinTemperature(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE)));
        dialog.setLayoutUpdateMinDistance(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_MIN_DISTANCE)));
        dialog.setLayoutUpdateTemperatureScaleFactor(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_TEMP_SCALE_FACTOR)));
        dialog.setLayoutUpdateComputePermutation(
            isTrue(
                (String)config.get(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION)));
        dialog.setLayoutUpdateUphillMovesAllowed(
            isTrue(
                (String)config.get(KEY_LAYOUT_UPDATE_IS_UPHILL_MOVE_ALLOWED)));
        dialog.setLayoutUpdateMaxRounds(
            Integer.parseInt(
                (String)config.get(KEY_LAYOUT_UPDATE_MAX_ROUNDS)));
        dialog.setLayoutUpdateTriesPerCell(
            Integer.parseInt(
                (String)config.get(KEY_LAYOUT_UPDATE_TRIES_PER_CELL)));
        dialog.setLayoutUpdateCostFunctionConfiguration(
            Integer.parseInt(
                (String)config.get(KEY_LAYOUT_UPDATE_COST_FUNCTION_CONFIG),2));
        dialog.setLayoutUpdateLambda(
            (ArrayList)config.get(KEY_LAYOUT_UPDATE_LAMBDA));
        dialog.setLayoutUpdateResultBounds(
            (Rectangle)config.get(KEY_LAYOUT_UPDATE_BOUNDS));
        dialog.setLayoutUpdateMethod(
            (String)config.get(KEY_LAYOUT_UPDATE_METHOD));
        dialog.setLayoutUpdateMethodNeighborsDepth(
            Integer.parseInt(
                (String)config.get(KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_DEPTH)));
        dialog.setLayoutUpdateMethodPerimeterRadius(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS)));
        dialog.setLayoutUpdateMethodPerimeterRadiusIncrease(
            Double.parseDouble(
                (String)config.get(
                    KEY_LAYOUT_UPDATE_METHOD_PERIMETER_RADIUS_INCREASE)));        
        dialog.setLayoutUpdateClusteringEnabled(
            isTrue(
                (String)config.get(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED)));
        dialog.setLayoutUpdateClusteringFactor(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR)));
        dialog.setLayoutUpdateClusteringMoveScaleFactor(
            Double.parseDouble(
                (String)config.get(KEY_LAYOUT_UPDATE_CLUSTERING_MOVE_SCALE)));
    }
    
    private boolean isTrue(String condition){
        if( "TRUE".equals(condition.toUpperCase()) )
            return true;
        return false;
    }
    
	/**
	 * @see org.jgraph.layout.LayoutController#getLayoutAlgorithm()
	 */
	public LayoutAlgorithm getLayoutAlgorithm() {
		return new AnnealingLayoutAlgorithm();
	}
    
    public String toString(){
        return KEY_TITLE;
    }

}
