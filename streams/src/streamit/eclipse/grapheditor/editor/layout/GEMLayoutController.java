package streamit.eclipse.grapheditor.editor.layout;


import java.util.Properties;
import javax.swing.JFrame;
import streamit.eclipse.grapheditor.editor.layout.LayoutAlgorithm;
import streamit.eclipse.grapheditor.editor.layout.LayoutController;
import streamit.eclipse.grapheditor.editor.layout.GEMLayoutAlgorithm;

/**
 * @author winkler
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

/******************************************************************************/    
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/    

public class GEMLayoutController implements LayoutController {
    
    public final static String KEY_TITLE                  = "GEM";
    
    public final static String KEY_CONFIG_NAME                 = "CONFIG_NAME";
    public final static String KEY_INIT_TEMPERATURE            = "init temperature";
    public final static String KEY_MIN_TEMPERATURE             = "min temperature";
    public final static String KEY_MAX_TEMPERATURE             = "max temperature";
    public final static String KEY_PREF_EDGE_LENGTH            = "preferred Edge length";
    public final static String KEY_GRAVITATION                 = "gravitation to barycenter";
    public final static String KEY_RANDOM_IMPULSE_RANGE        = "random impulse range";
    public final static String KEY_COMPUTE_PERMUTATION         = "compute permutation";
    public final static String KEY_END_CONDITION_AVERAGE       = "end condition average"; 
    public final static String KEY_AVOID_OVERLAPPING           = "avoid overlapping";
    public final static String KEY_OVERLAPPING_DETECTION_WIDTH = "overlapping detection width";   
    public final static String KEY_OVERLAPPING_PREF_DISTANCE   = "overlapping preferred distance";
    public final static String KEY_ALPHA_OSC                   = "alpha oscillation";
    public final static String KEY_ALPHA_ROT                   = "alpha rotation";
    public final static String KEY_SIGMA_OSC                   = "sigma oscillation";
    public final static String KEY_SIGMA_ROT                   = "sigma rotation";
    public final static String KEY_OPTIMIZE_ALGORITHM_ENABLED  = "optimization algorithm enabled";
    public final static String KEY_OPTIMIZE_ALGORITHM_CONFIG   = "optimization algorithm configuration";

    public final static String KEY_LAYOUT_UPDATE_INIT_TEMPERATURE            = "Layout Update init temperature";
    public final static String KEY_LAYOUT_UPDATE_MIN_TEMPERATURE             = "Layout Update min temperature";
    public final static String KEY_LAYOUT_UPDATE_MAX_TEMPERATURE             = "Layout Update max temperature";
    public final static String KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH            = "Layout Update preferred Edge length";
    public final static String KEY_LAYOUT_UPDATE_GRAVITATION                 = "Layout Update gravitation to barycenter";
    public final static String KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE        = "Layout Update random impulse range";
    public final static String KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION         = "Layout Update compute permutation";
    public final static String KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE       = "Layout Update end condition average";    
    public final static String KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING           = "Layout Update avoid overlapping";
    public final static String KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH = "Layout Update overlapping detection width";   
    public final static String KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE   = "Layout Update overlapping preferred distance";
    public final static String KEY_LAYOUT_UPDATE_ALPHA_ROT                   = "Layout Update alpha oscillation";
    public final static String KEY_LAYOUT_UPDATE_ALPHA_OSC                   = "Layout Update alpha rotation";
    public final static String KEY_LAYOUT_UPDATE_SIGMA_ROT                   = "Layout Update sigma oscillation";
    public final static String KEY_LAYOUT_UPDATE_SIGMA_OSC                   = "Layout Update sigma rotation";
    public final static String KEY_LAYOUT_UPDATE_ENABLED                     = "Layout Update enabled";
    public final static String KEY_LAYOUT_UPDATE_METHOD                      = "Layout Update method";
    public final static String KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED  = "Layout Update optimization algorithm enabled";
    public final static String KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG   = "Layout Update optimization algorithm configuration";
    
    
    public final static String KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY       = "Neighbors only";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETERS           = "Perimeter";
    public final static String KEY_LAYOUT_UPDATE_DEPTH                       = "Layout Update depth";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE  = "Layout Update method perimeter initial size";
    public final static String KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC   = "Layout Update method perimeter size increase value";
    
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED              = "clustering enabled";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE     = "cluster init temperature";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR = "clustering force scaling factor";
    public final static String KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR               = "cluster size factor";
    
    protected Properties[] configs;  
    protected Properties   configuration;  
    protected GEMLayoutConfigurationDialog dialog;
    protected AnnealingLayoutController optimizeController;
    protected AnnealingLayoutController lu_optimizeController;
    
/******************************************************************************/    
    
    public GEMLayoutController(){
        
        optimizeController = new AnnealingLayoutController(true);
        
        lu_optimizeController = new AnnealingLayoutController(true);
        
        configs = new Properties[2];
        for( int i = 0; i < configs.length; i++ )
            configs[i] = new Properties();
            
        Properties optimizeConfig = optimizeController.getConfiguration();
        optimizeConfig.put(AnnealingLayoutController.KEY_CONFIG_NAME,KEY_TITLE+" optimization values");
        optimizeConfig.put(AnnealingLayoutController.KEY_COST_FUNCTION_CONFIG,"110111");
        optimizeConfig.put(AnnealingLayoutController.KEY_INIT_TEMPERATURE,"40.0");
        optimizeConfig.put(AnnealingLayoutController.KEY_MIN_DISTANCE,"100.0");
        optimizeConfig.put(AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR,"0.85");
        optimizeController.setConfiguration(optimizeConfig);

        Properties lu_optimizeConfig = lu_optimizeController.getConfiguration();
        lu_optimizeConfig.put(AnnealingLayoutController.KEY_CONFIG_NAME,KEY_TITLE+" optimization values");
        lu_optimizeConfig.put(AnnealingLayoutController.KEY_COST_FUNCTION_CONFIG,"110111");
        lu_optimizeConfig.put(AnnealingLayoutController.KEY_INIT_TEMPERATURE,"40.0");
        lu_optimizeConfig.put(AnnealingLayoutController.KEY_MIN_DISTANCE,"100.0");
        lu_optimizeConfig.put(AnnealingLayoutController.KEY_TEMP_SCALE_FACTOR,"0.85");
        lu_optimizeController.setConfiguration(lu_optimizeConfig);
                
        configs[0].put(KEY_CONFIG_NAME           ,"Default Values");
        configs[0].put(KEY_INIT_TEMPERATURE      , "50.0"   );
        configs[0].put(KEY_MIN_TEMPERATURE       ,  "3.0"   );
        configs[0].put(KEY_MAX_TEMPERATURE       ,"256.0"   );
        configs[0].put(KEY_PREF_EDGE_LENGTH      ,"100.0"   );
        configs[0].put(KEY_GRAVITATION           ,  "0.0625");
        configs[0].put(KEY_RANDOM_IMPULSE_RANGE  , "32.0"   );
        configs[0].put(KEY_COMPUTE_PERMUTATION   , "true"   );
        configs[0].put(KEY_END_CONDITION_AVERAGE , "true"   );
        configs[0].put(KEY_AVOID_OVERLAPPING     , "false"   );
        configs[0].put(KEY_OVERLAPPING_DETECTION_WIDTH, "40.0");
        configs[0].put(KEY_OVERLAPPING_PREF_DISTANCE, "40.0");
        configs[0].put(KEY_ALPHA_OSC             ,String.valueOf(Math.PI/2.0));
        configs[0].put(KEY_ALPHA_ROT             ,String.valueOf(Math.PI/3.0));
        configs[0].put(KEY_SIGMA_OSC             ,String.valueOf(1.0/3.0)); //a higher value leads to a faster falling temperature
        configs[0].put(KEY_SIGMA_ROT             ,String.valueOf(1.0/2.0)); //as smaller this value is, the smaller the temperature alteration
        configs[0].put(KEY_OPTIMIZE_ALGORITHM_ENABLED,"false");
        configs[0].put(KEY_OPTIMIZE_ALGORITHM_CONFIG,optimizeConfig.clone());
        
        configs[0].put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      ,"10.0");
        configs[0].put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,"3.0");
        configs[0].put(KEY_LAYOUT_UPDATE_MAX_TEMPERATURE       ,"256.0");
        configs[0].put(KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH      ,"100.0");
        configs[0].put(KEY_LAYOUT_UPDATE_GRAVITATION           ,"0.0625");
        configs[0].put(KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE  ,"32.0");
        configs[0].put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        configs[0].put(KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE ,"true");
        configs[0].put(KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING     ,"false");
        configs[0].put(KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,"40.0");
        configs[0].put(KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE,  "40.0");
        configs[0].put(KEY_LAYOUT_UPDATE_ALPHA_OSC             ,String.valueOf(Math.toRadians(90.0)));
        configs[0].put(KEY_LAYOUT_UPDATE_ALPHA_ROT             ,String.valueOf(Math.toRadians(60.0)));
        configs[0].put(KEY_LAYOUT_UPDATE_SIGMA_OSC             ,String.valueOf(1.0/3.0));
        configs[0].put(KEY_LAYOUT_UPDATE_SIGMA_ROT             ,String.valueOf(1.0/2.0));
        configs[0].put(KEY_LAYOUT_UPDATE_ENABLED               ,"false");
        configs[0].put(KEY_LAYOUT_UPDATE_DEPTH                 ,"1");
        configs[0].put(KEY_LAYOUT_UPDATE_METHOD                ,KEY_LAYOUT_UPDATE_METHOD_NEIGHBORS_ONLY);
        configs[0].put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED,"false");
        configs[0].put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG,lu_optimizeConfig.clone());
        configs[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED              ,"false");
        configs[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE     ,"15.0");
        configs[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR ,"0.1");
        configs[0].put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR               ,"8.0");
        configs[0].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE      ,"100.0");
        configs[0].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC       ,"20.0");
        
        configs[1].put(KEY_CONFIG_NAME           ,"Long running Values");
        configs[1].put(KEY_INIT_TEMPERATURE      ,"250.0"   );
        configs[1].put(KEY_MIN_TEMPERATURE       ,  "0.5"   );
        configs[1].put(KEY_MAX_TEMPERATURE       ,"256.0"   );
        configs[1].put(KEY_PREF_EDGE_LENGTH      ,"100.0"   );
        configs[1].put(KEY_GRAVITATION           ,  "0.0625");
        configs[1].put(KEY_RANDOM_IMPULSE_RANGE  , "32.0"   );
        configs[1].put(KEY_COMPUTE_PERMUTATION   , "true"   );
        configs[1].put(KEY_END_CONDITION_AVERAGE , "false");
        configs[1].put(KEY_AVOID_OVERLAPPING     , "false");
        configs[1].put(KEY_OVERLAPPING_DETECTION_WIDTH,"40.0");
        configs[1].put(KEY_OVERLAPPING_PREF_DISTANCE,  "40.0");
        configs[1].put(KEY_ALPHA_OSC             ,String.valueOf(Math.toRadians(90.0)));
        configs[1].put(KEY_ALPHA_ROT             ,String.valueOf(Math.toRadians(60.0)));
        configs[1].put(KEY_SIGMA_OSC             ,String.valueOf(7.0/8.0));
        configs[1].put(KEY_SIGMA_ROT             ,String.valueOf(1.0/5.0));
        configs[1].put(KEY_OPTIMIZE_ALGORITHM_ENABLED,"false");
        configs[1].put(KEY_OPTIMIZE_ALGORITHM_CONFIG,optimizeConfig.clone());
        
        configs[1].put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE      ,"100.0");
        configs[1].put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE       ,"2.0");
        configs[1].put(KEY_LAYOUT_UPDATE_MAX_TEMPERATURE       ,"256.0");
        configs[1].put(KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH      ,"100.0");
        configs[1].put(KEY_LAYOUT_UPDATE_GRAVITATION           ,"0.0625");
        configs[1].put(KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE  ,"32.0");
        configs[1].put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION   ,"true");
        configs[1].put(KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE ,"false");
        configs[1].put(KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING     ,"false");
        configs[1].put(KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,"40.0");
        configs[1].put(KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE,  "40.0");
        configs[1].put(KEY_LAYOUT_UPDATE_ALPHA_OSC             ,String.valueOf(Math.toRadians(90.0)));
        configs[1].put(KEY_LAYOUT_UPDATE_ALPHA_ROT             ,String.valueOf(Math.toRadians(60.0)));
        configs[1].put(KEY_LAYOUT_UPDATE_SIGMA_OSC             ,String.valueOf(7.0/8.0));
        configs[1].put(KEY_LAYOUT_UPDATE_SIGMA_ROT             ,String.valueOf(1.0/5.0));
        configs[1].put(KEY_LAYOUT_UPDATE_ENABLED               ,"false");
        configs[1].put(KEY_LAYOUT_UPDATE_DEPTH                 ,"1");
        configs[1].put(KEY_LAYOUT_UPDATE_METHOD                ,KEY_LAYOUT_UPDATE_METHOD_PERIMETERS);
        configs[1].put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED,"false");
        configs[1].put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG,lu_optimizeConfig.clone());
        configs[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED              ,"true");
        configs[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE     ,"20.0");
        configs[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR ,"0.1");
        configs[1].put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR               ,"12.0");
        configs[1].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE      ,"150.0");
        configs[1].put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC       ,"30.0");
        
        
       
        configuration = (Properties) configs[0].clone();
        
        dialog = new GEMLayoutConfigurationDialog(new JFrame(),configs,optimizeController,lu_optimizeController);
        
    }

/******************************************************************************/  
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
        return KEY_TITLE;
	}

/******************************************************************************/    
	/**
	 * @see de.fzi.echidna.layout.LayoutController#isConfigurable()
	 */
	public boolean isConfigurable() {
		return true;
	}

/******************************************************************************/    
	/**
	 * @see de.fzi.echidna.layout.LayoutController#configure()
	 */
	public void configure() {
        pushConfig();
        dialog.setVisible(true);
        if( !dialog.isCanceled() )
            pullConfig();
	}

/******************************************************************************/    
	/**
	 * @see de.fzi.echidna.layout.LayoutController#getConfiguration()
	 */
	public Properties getConfiguration() {
        return configuration;
	}

/******************************************************************************/    
	/**
	 * @see de.fzi.echidna.layout.LayoutController#getLayoutAlgorithm()
	 */
	public LayoutAlgorithm getLayoutAlgorithm() {
        return new GEMLayoutAlgorithm((AnnealingLayoutAlgorithm)optimizeController.getLayoutAlgorithm());
	}

/******************************************************************************/    

    protected void pushConfig(){
        double initTemperature    = Double.parseDouble((String)configuration.get(KEY_INIT_TEMPERATURE));
        double minTemperature     = Double.parseDouble((String)configuration.get(KEY_MIN_TEMPERATURE));
        double maxTemperature     = Double.parseDouble((String)configuration.get(KEY_MAX_TEMPERATURE));
        double prefEdgeLength     = Double.parseDouble((String)configuration.get(KEY_PREF_EDGE_LENGTH));
        double gravitation        = Double.parseDouble((String)configuration.get(KEY_GRAVITATION));
        double randomImpulseRange = Double.parseDouble((String)configuration.get(KEY_RANDOM_IMPULSE_RANGE));
        double overlapDetectWidth = Double.parseDouble((String)configuration.get(KEY_OVERLAPPING_DETECTION_WIDTH));
        double overlapPrefDist    = Double.parseDouble((String)configuration.get(KEY_OVERLAPPING_PREF_DISTANCE));
        double alphaOsc           = Double.parseDouble((String)configuration.get(KEY_ALPHA_OSC));
        double alphaRot           = Double.parseDouble((String)configuration.get(KEY_ALPHA_ROT));
        double sigmaOsc           = Double.parseDouble((String)configuration.get(KEY_SIGMA_OSC));
        double sigmaRot           = Double.parseDouble((String)configuration.get(KEY_SIGMA_ROT));        
        
        boolean computePermut        = isTrue((String)configuration.get(KEY_COMPUTE_PERMUTATION));
        boolean endPerAverage        = isTrue((String)configuration.get(KEY_END_CONDITION_AVERAGE));
        boolean avoidOverlapping     = isTrue((String)configuration.get(KEY_AVOID_OVERLAPPING));
        boolean useOptimizeAlgorithm = isTrue((String)configuration.get(KEY_OPTIMIZE_ALGORITHM_ENABLED));
        
        Properties optimizationConfig = (Properties) configuration.get(KEY_OPTIMIZE_ALGORITHM_CONFIG);
                
        double lu_initTemperature    = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE));
        double lu_minTemperature     = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE));
        double lu_maxTemperature     = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_MAX_TEMPERATURE));
        double lu_prefEdgeLength     = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH));
        double lu_gravitation        = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_GRAVITATION));
        double lu_randomImpulseRange = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE));
        double lu_overlapDetectWidth = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH));
        double lu_overlapPrefDist    = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE));
        double lu_alphaOsc           = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_ALPHA_OSC));
        double lu_alphaRot           = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_ALPHA_ROT));
        double lu_sigmaOsc           = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_SIGMA_OSC));
        double lu_sigmaRot           = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_SIGMA_ROT));
        double lu_cluster_initTemp   = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE));
        double lu_cluster_forceScale = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR));
        double lu_cluster_factor     = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR));        
        
        int    lu_depth              = Integer.parseInt((String)configuration.get(KEY_LAYOUT_UPDATE_DEPTH));
        double lu_perimeter_initSize = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE));
        double lu_perimeter_sizeInc  = Double.parseDouble((String)configuration.get(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC));
        
        boolean lu_enabled              = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_ENABLED));
        boolean lu_cluster_enabled      = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED));
        
        boolean lu_computePermut        = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION));
        boolean lu_endPerAverage        = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE));
        boolean lu_avoidOverlapping     = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING));
        boolean lu_useOptimizeAlgorithm = isTrue((String)configuration.get(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED));
        
        Properties lu_optimizationConfig = (Properties) configuration.get(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG);
        
        String lu_method = (String)configuration.get(KEY_LAYOUT_UPDATE_METHOD);
        
        dialog.setInitTemperature(initTemperature);
        dialog.setMinTemperature(minTemperature);
        dialog.setMaxTemperature(maxTemperature);
        dialog.setPreferredEdgeLength(prefEdgeLength);
        dialog.setGravitation(gravitation);
        dialog.setRandomImpulseRange(randomImpulseRange);
        dialog.setOverlapDetectionWidth(overlapDetectWidth);
        dialog.setOverlapPreferredDistance(overlapPrefDist);
        dialog.setAlphaOsc(alphaOsc);
        dialog.setAlphaRot(alphaRot);
        dialog.setSigmaOsc(sigmaOsc);
        dialog.setSigmaRot(sigmaRot);
        dialog.setComputePermutation(computePermut);
        dialog.setEndPerAverage(endPerAverage);
        dialog.setAvoidOverlapping(avoidOverlapping);
        dialog.setOptimizationAlgorithmEnabled(useOptimizeAlgorithm);
        dialog.setOptimizationConfiguration(optimizationConfig);
        
        dialog.setLayoutUpdateInitTemperature(lu_initTemperature);
        dialog.setLayoutUpdateMinTemperature(lu_minTemperature);
        dialog.setLayoutUpdateMaxTemperature(lu_maxTemperature);
        dialog.setLayoutUpdatePreferredEdgeLength(lu_prefEdgeLength);
        dialog.setLayoutUpdateGravitation(lu_gravitation);
        dialog.setLayoutUpdateRandomImpulseRange(lu_randomImpulseRange);
        dialog.setLayoutUpdateOverlapDetectionWidth(lu_overlapDetectWidth);
        dialog.setLayoutUpdateOverlapPreferredDistance(lu_overlapPrefDist);
        dialog.setLayoutUpdateAlphaOsc(lu_alphaOsc);
        dialog.setLayoutUpdateAlphaRot(lu_alphaRot);
        dialog.setLayoutUpdateSigmaOsc(lu_sigmaOsc);
        dialog.setLayoutUpdateSigmaRot(lu_sigmaRot);
        dialog.setLayoutUpdateComputePermutation(lu_computePermut);
        dialog.setLayoutUpdateEndPerAverage(lu_endPerAverage);
        dialog.setLayoutUpdateAvoidOverlapping(lu_avoidOverlapping);
        dialog.setLayoutUpdateOptimizationAlgorithmEnabled(lu_useOptimizeAlgorithm);
        dialog.setLayoutUpdateOptimizationConfiguration(lu_optimizationConfig);
        
        dialog.setLayoutUpdateEnabled(lu_enabled);
        dialog.setLayoutUpdateDepth(lu_depth);
        dialog.setLayoutUpdateMethodPerimeterInitSize(lu_perimeter_initSize);
        dialog.setLayoutUpdateMethodPerimeterSizeInc(lu_perimeter_sizeInc);
        dialog.setLayoutUpdateClusteringEnabled(lu_cluster_enabled);
        dialog.setLayoutUpdateClusteringInitTemperature(lu_cluster_initTemp);
        dialog.setLayoutUpdateClusteringForceScalingFactor(lu_cluster_forceScale);
        dialog.setLayoutUpdateClusteringFactor(lu_cluster_factor);
        
        dialog.setLayoutUpdateMethod(lu_method);
        
    }

/******************************************************************************/    

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

    protected void pullConfig(){
        double initTemperature    = dialog.getInitTemperature();
        double minTemperature     = dialog.getMinTemperature();
        double maxTemperature     = dialog.getMaxTemperature();
        double prefEdgeLength     = dialog.getPreferredEdgeLength();
        double gravitation        = dialog.getGravitation();
        double randomImpulseRange = dialog.getRandomImpulseRange();
        double overlapDetectWidth = dialog.getOverlapDetectionWidth();
        double overlapPrefDist    = dialog.getOverlapPreferredDistance();
        double alphaOsc           = dialog.getAlphaOsc();
        double alphaRot           = dialog.getAlphaRot();
        double sigmaOsc           = dialog.getSigmaOsc();
        double sigmaRot           = dialog.getSigmaRot();
        boolean computePermut     = dialog.getComputePermutation();
        boolean endPerAverage     = dialog.getEndPerAverage();
        boolean avoidOverlapping  = dialog.getAvoidOverlapping();
        boolean useOptimizationAlg = dialog.getOptimizationAlgorithmEnabled();
        Properties optimizeConfig  = dialog.getOptimizationConfiguration();
        
        double lu_initTemperature    = dialog.getLayoutUpdateInitTemperature();
        double lu_minTemperature     = dialog.getLayoutUpdateMinTemperature();
        double lu_maxTemperature     = dialog.getLayoutUpdateMaxTemperature();
        double lu_prefEdgeLength     = dialog.getLayoutUpdatePreferredEdgeLength();
        double lu_gravitation        = dialog.getLayoutUpdateGravitation();
        double lu_randomImpulseRange = dialog.getLayoutUpdateRandomImpulseRange();
        double lu_overlapDetectWidth = dialog.getLayoutUpdateOverlapDetectionWidth();
        double lu_overlapPrefDist    = dialog.getLayoutUpdateOverlapPreferredDistance();
        double lu_alphaOsc           = dialog.getLayoutUpdateAlphaOsc();
        double lu_alphaRot           = dialog.getLayoutUpdateAlphaRot();
        double lu_sigmaOsc           = dialog.getLayoutUpdateSigmaOsc();
        double lu_sigmaRot           = dialog.getLayoutUpdateSigmaRot();
        boolean lu_computePermut     = dialog.getLayoutUpdateComputePermutation();
        boolean lu_endPerAverage     = dialog.getLayoutUpdateEndPerAverage();
        boolean lu_avoidOverlapping  = dialog.getLayoutUpdateAvoidOverlapping();
        boolean lu_useOptimizationAlg = dialog.getLayoutUpdateOptimizationAlgorithmEnabled();
        boolean lu_enabled           = dialog.getLayoutUpdateEnabled();
        String lu_method              = dialog.getLayoutUpdateMethod();
        int    lu_depth              = dialog.getLayoutUpdateDepth();
        double lu_perimeter_initSize = dialog.getLayoutUpdateMethodPerimeterInitSize();
        double lu_perimeter_sizeInc  = dialog.getLayoutUpdateMethodPerimeterSizeInc();
        boolean lu_cluster_enabled    = dialog.getLayoutUpdateClusteringEnabled();
        double lu_cluster_initTemp   = dialog.getLayoutUpdateClusteringInitTemperature();
        double lu_cluster_forceScale = dialog.getLayoutUpdateClusteringForceScalingFactor();
        double lu_cluster_factor     = dialog.getLayoutUpdateClusteringFactor();
        Properties lu_optimizeConfig  = dialog.getLayoutUpdateOptimizationConfiguration();
        
        
        
        configuration.put(KEY_INIT_TEMPERATURE           ,String.valueOf(initTemperature));
        configuration.put(KEY_MIN_TEMPERATURE            ,String.valueOf(minTemperature));
        configuration.put(KEY_MAX_TEMPERATURE            ,String.valueOf(maxTemperature));
        configuration.put(KEY_PREF_EDGE_LENGTH           ,String.valueOf(prefEdgeLength));
        configuration.put(KEY_GRAVITATION                ,String.valueOf(gravitation));
        configuration.put(KEY_RANDOM_IMPULSE_RANGE       ,String.valueOf(randomImpulseRange));
        configuration.put(KEY_OVERLAPPING_DETECTION_WIDTH,String.valueOf(overlapDetectWidth));
        configuration.put(KEY_OVERLAPPING_PREF_DISTANCE  ,String.valueOf(overlapPrefDist));
        configuration.put(KEY_COMPUTE_PERMUTATION        ,String.valueOf(computePermut));
        configuration.put(KEY_END_CONDITION_AVERAGE      ,String.valueOf(endPerAverage));
        configuration.put(KEY_AVOID_OVERLAPPING          ,String.valueOf(avoidOverlapping));
        configuration.put(KEY_ALPHA_OSC                  ,String.valueOf(alphaOsc));
        configuration.put(KEY_ALPHA_ROT                  ,String.valueOf(alphaRot));
        configuration.put(KEY_SIGMA_OSC                  ,String.valueOf(sigmaOsc));
        configuration.put(KEY_SIGMA_ROT                  ,String.valueOf(sigmaRot));
        configuration.put(KEY_OPTIMIZE_ALGORITHM_ENABLED ,String.valueOf(useOptimizationAlg));
        configuration.put(KEY_OPTIMIZE_ALGORITHM_CONFIG  ,optimizeConfig);
        
        
        configuration.put(KEY_LAYOUT_UPDATE_INIT_TEMPERATURE           ,String.valueOf(lu_initTemperature));
        configuration.put(KEY_LAYOUT_UPDATE_MIN_TEMPERATURE            ,String.valueOf(lu_minTemperature));
        configuration.put(KEY_LAYOUT_UPDATE_MAX_TEMPERATURE            ,String.valueOf(lu_maxTemperature));
        configuration.put(KEY_LAYOUT_UPDATE_PREF_EDGE_LENGTH           ,String.valueOf(lu_prefEdgeLength));
        configuration.put(KEY_LAYOUT_UPDATE_GRAVITATION                ,String.valueOf(lu_gravitation));
        configuration.put(KEY_LAYOUT_UPDATE_RANDOM_IMPULSE_RANGE       ,String.valueOf(lu_randomImpulseRange));
        configuration.put(KEY_LAYOUT_UPDATE_OVERLAPPING_DETECTION_WIDTH,String.valueOf(lu_overlapDetectWidth));
        configuration.put(KEY_LAYOUT_UPDATE_OVERLAPPING_PREF_DISTANCE  ,String.valueOf(lu_overlapPrefDist));
        configuration.put(KEY_LAYOUT_UPDATE_COMPUTE_PERMUTATION        ,String.valueOf(lu_computePermut));
        configuration.put(KEY_LAYOUT_UPDATE_END_CONDITION_AVERAGE      ,String.valueOf(lu_endPerAverage));
        configuration.put(KEY_LAYOUT_UPDATE_AVOID_OVERLAPPING          ,String.valueOf(lu_avoidOverlapping));
        configuration.put(KEY_LAYOUT_UPDATE_ALPHA_OSC                  ,String.valueOf(lu_alphaOsc));
        configuration.put(KEY_LAYOUT_UPDATE_ALPHA_ROT                  ,String.valueOf(lu_alphaRot));
        configuration.put(KEY_LAYOUT_UPDATE_SIGMA_OSC                  ,String.valueOf(lu_sigmaOsc));
        configuration.put(KEY_LAYOUT_UPDATE_SIGMA_ROT                  ,String.valueOf(lu_sigmaRot));
        configuration.put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_ENABLED ,String.valueOf(lu_useOptimizationAlg));
        configuration.put(KEY_LAYOUT_UPDATE_OPTIMIZE_ALGORITHM_CONFIG  ,lu_optimizeConfig);
        
        configuration.put(KEY_LAYOUT_UPDATE_ENABLED ,String.valueOf(lu_enabled));
        configuration.put(KEY_LAYOUT_UPDATE_METHOD                     ,lu_method);
        configuration.put(KEY_LAYOUT_UPDATE_DEPTH                      ,String.valueOf(lu_depth));
        configuration.put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_INIT_SIZE ,String.valueOf(lu_perimeter_initSize));
        configuration.put(KEY_LAYOUT_UPDATE_METHOD_PERIMETER_SIZE_INC  ,String.valueOf(lu_perimeter_sizeInc));
        configuration.put(KEY_LAYOUT_UPDATE_CLUSTERING_ENABLED         ,String.valueOf(lu_cluster_enabled));
        configuration.put(KEY_LAYOUT_UPDATE_CLUSTERING_INIT_TEMPERATURE,String.valueOf(lu_cluster_initTemp));
        configuration.put(KEY_LAYOUT_UPDATE_CLUSTERING_FORCE_SCALING_FACTOR,String.valueOf(lu_cluster_forceScale));
        configuration.put(KEY_LAYOUT_UPDATE_CLUSTERING_FACTOR          ,String.valueOf(lu_cluster_factor));
    }

/******************************************************************************/    
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/*############################################################################*/
/******************************************************************************/    
}
