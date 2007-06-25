/*
 * MATLAB Compiler: 4.4 (R2006a)
 * Date: Mon Jun 25 14:07:41 2007
 * Arguments: "-B" "macro_default" "-m" "-W" "main" "-T" "link:exe" "clocking"
 * "testfunc" "show" "ransacfitplane" "ransacfit" "ransac" "normalise2dpts"
 * "harris" "fundmatrix" "correlation" 
 */

#include "mclmcr.h"

#ifdef __cplusplus
extern "C" {
#endif
const unsigned char __MCC_clocking_session_key[] = {
        'B', '8', '6', 'C', 'B', 'E', 'D', '1', '1', '2', 'F', '2', '6', 'C',
        '7', '6', '3', '8', '3', '6', '9', 'E', '8', '8', '5', 'C', '3', '7',
        '9', '4', '5', '8', 'E', '6', '5', '6', '1', '9', 'E', '0', '5', '1',
        '6', 'F', 'A', 'A', '0', '5', '3', 'E', '5', 'D', '9', 'E', 'E', '6',
        '3', '8', '5', '4', '0', 'A', '9', '1', '2', 'E', 'E', 'A', '3', '3',
        '4', '4', 'E', '7', '9', '0', '8', 'E', '0', 'D', 'D', 'A', 'A', 'A',
        '0', 'B', '6', 'E', 'C', '1', 'E', '7', 'E', '0', '2', '1', '3', '4',
        '0', '3', 'E', 'F', '6', '8', '0', '1', 'B', '4', '6', '6', 'A', '4',
        '6', '2', 'A', '6', 'F', 'C', 'E', '6', 'D', '4', 'A', '3', '5', '5',
        '3', '1', '4', '0', 'D', '3', '3', '1', 'C', '4', '7', 'B', '5', '0',
        '5', 'C', '5', '9', '5', '3', '9', '2', '5', '5', '2', '6', 'A', '4',
        '2', 'D', 'D', '6', 'A', 'A', '1', '6', '2', '7', '3', 'C', 'B', '9',
        '0', '5', '6', 'E', '5', '1', '1', '8', '4', 'C', '7', '3', '3', 'C',
        '0', 'E', 'A', 'F', 'E', '7', '7', '8', '2', '6', 'E', '1', '2', 'B',
        '3', '9', 'E', 'C', '8', '1', '5', 'C', '1', 'E', '6', 'D', 'C', '0',
        '7', 'E', 'E', '0', '2', '6', 'A', '6', 'A', '6', 'E', '4', '4', '3',
        '9', '3', 'B', '7', 'E', '4', '3', 'B', 'A', '0', 'B', '6', '5', 'D',
        'E', '3', '7', '3', '4', '0', 'F', 'E', '9', 'D', '0', 'B', 'F', '5',
        '8', '7', 'D', '1', '\0'};

const unsigned char __MCC_clocking_public_key[] = {
        '3', '0', '8', '1', '9', 'D', '3', '0', '0', 'D', '0', '6', '0', '9',
        '2', 'A', '8', '6', '4', '8', '8', '6', 'F', '7', '0', 'D', '0', '1',
        '0', '1', '0', '1', '0', '5', '0', '0', '0', '3', '8', '1', '8', 'B',
        '0', '0', '3', '0', '8', '1', '8', '7', '0', '2', '8', '1', '8', '1',
        '0', '0', 'C', '4', '9', 'C', 'A', 'C', '3', '4', 'E', 'D', '1', '3',
        'A', '5', '2', '0', '6', '5', '8', 'F', '6', 'F', '8', 'E', '0', '1',
        '3', '8', 'C', '4', '3', '1', '5', 'B', '4', '3', '1', '5', '2', '7',
        '7', 'E', 'D', '3', 'F', '7', 'D', 'A', 'E', '5', '3', '0', '9', '9',
        'D', 'B', '0', '8', 'E', 'E', '5', '8', '9', 'F', '8', '0', '4', 'D',
        '4', 'B', '9', '8', '1', '3', '2', '6', 'A', '5', '2', 'C', 'C', 'E',
        '4', '3', '8', '2', 'E', '9', 'F', '2', 'B', '4', 'D', '0', '8', '5',
        'E', 'B', '9', '5', '0', 'C', '7', 'A', 'B', '1', '2', 'E', 'D', 'E',
        '2', 'D', '4', '1', '2', '9', '7', '8', '2', '0', 'E', '6', '3', '7',
        '7', 'A', '5', 'F', 'E', 'B', '5', '6', '8', '9', 'D', '4', 'E', '6',
        '0', '3', '2', 'F', '6', '0', 'C', '4', '3', '0', '7', '4', 'A', '0',
        '4', 'C', '2', '6', 'A', 'B', '7', '2', 'F', '5', '4', 'B', '5', '1',
        'B', 'B', '4', '6', '0', '5', '7', '8', '7', '8', '5', 'B', '1', '9',
        '9', '0', '1', '4', '3', '1', '4', 'A', '6', '5', 'F', '0', '9', '0',
        'B', '6', '1', 'F', 'C', '2', '0', '1', '6', '9', '4', '5', '3', 'B',
        '5', '8', 'F', 'C', '8', 'B', 'A', '4', '3', 'E', '6', '7', '7', '6',
        'E', 'B', '7', 'E', 'C', 'D', '3', '1', '7', '8', 'B', '5', '6', 'A',
        'B', '0', 'F', 'A', '0', '6', 'D', 'D', '6', '4', '9', '6', '7', 'C',
        'B', '1', '4', '9', 'E', '5', '0', '2', '0', '1', '1', '1', '\0'};

static const char * MCC_clocking_matlabpath_data[] = 
    { "clocking/", "toolbox/compiler/deploy/",
      "$TOOLBOXMATLABDIR/general/", "$TOOLBOXMATLABDIR/ops/",
      "$TOOLBOXMATLABDIR/lang/", "$TOOLBOXMATLABDIR/elmat/",
      "$TOOLBOXMATLABDIR/elfun/", "$TOOLBOXMATLABDIR/specfun/",
      "$TOOLBOXMATLABDIR/matfun/", "$TOOLBOXMATLABDIR/datafun/",
      "$TOOLBOXMATLABDIR/polyfun/", "$TOOLBOXMATLABDIR/funfun/",
      "$TOOLBOXMATLABDIR/sparfun/", "$TOOLBOXMATLABDIR/scribe/",
      "$TOOLBOXMATLABDIR/graph2d/", "$TOOLBOXMATLABDIR/graph3d/",
      "$TOOLBOXMATLABDIR/specgraph/", "$TOOLBOXMATLABDIR/graphics/",
      "$TOOLBOXMATLABDIR/uitools/", "$TOOLBOXMATLABDIR/strfun/",
      "$TOOLBOXMATLABDIR/imagesci/", "$TOOLBOXMATLABDIR/iofun/",
      "$TOOLBOXMATLABDIR/audiovideo/", "$TOOLBOXMATLABDIR/timefun/",
      "$TOOLBOXMATLABDIR/datatypes/", "$TOOLBOXMATLABDIR/verctrl/",
      "$TOOLBOXMATLABDIR/codetools/", "$TOOLBOXMATLABDIR/helptools/",
      "$TOOLBOXMATLABDIR/winfun/", "$TOOLBOXMATLABDIR/demos/",
      "$TOOLBOXMATLABDIR/timeseries/", "$TOOLBOXMATLABDIR/hds/",
      "toolbox/local/", "toolbox/compiler/", "toolbox/database/database/",
      "toolbox/images/images/", "toolbox/images/imuitools/",
      "toolbox/images/iptutils/", "toolbox/shared/imageslib/",
      "toolbox/images/medformats/", "toolbox/optim/" };

static const char * MCC_clocking_classpath_data[] = 
    { "java/jar/toolbox/database.jar", "java/jar/toolbox/images.jar" };

static const char * MCC_clocking_libpath_data[] = 
    { "" };

static const char * MCC_clocking_app_opts_data[] = 
    { "" };

static const char * MCC_clocking_run_opts_data[] = 
    { "" };

static const char * MCC_clocking_warning_state_data[] = 
    { "" };


mclComponentData __MCC_clocking_component_data = { 

    /* Public key data */
    __MCC_clocking_public_key,

    /* Component name */
    "clocking",

    /* Component Root */
    "",

    /* Application key data */
    __MCC_clocking_session_key,

    /* Component's MATLAB Path */
    MCC_clocking_matlabpath_data,

    /* Number of directories in the MATLAB Path */
    41,

    /* Component's Java class path */
    MCC_clocking_classpath_data,
    /* Number of directories in the Java class path */
    2,

    /* Component's load library path (for extra shared libraries) */
    MCC_clocking_libpath_data,
    /* Number of directories in the load library path */
    0,

    /* MCR instance-specific runtime options */
    MCC_clocking_app_opts_data,
    /* Number of MCR instance-specific runtime options */
    0,

    /* MCR global runtime options */
    MCC_clocking_run_opts_data,
    /* Number of MCR global runtime options */
    0,
    
    /* Component preferences directory */
    "clocking_4B6CCE996C7A8F448A832939ECD6D7AC",

    /* MCR warning status data */
    MCC_clocking_warning_state_data,
    /* Number of MCR warning status modifiers */
    0,

    /* Path to component - evaluated at runtime */
    NULL

};

#ifdef __cplusplus
}
#endif


