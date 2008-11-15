These applications were used for the Graphics Hardware 2005 paper:

A Reconfigurable Architecture for Load-Balanced Rendering
Jiawen Chen, Michael I. Gordon, William Thies, Matthias Zwicker, Kari Pulli, and Fredo Durand
Proceedings of the SIGGRAPH / Eurographics Workshop on Graphics Hardware

Each one represents a graphics rendering pipeline that is specialized
to devote varying amounts of resources to each processing stage
depending on what is utilized in a given scene.

As opposed to most StreamIt apps, the widths of splitjoins in these
apps is custom-tailored to the degree of parallelism expected on the
machine.  One reason this is necessary is because there are dynamic
rates (rasterization) in these apps, so work estimation was done by
hand.

Correspondence of directories to apps in paper:

Case 1 - phong_shading
Case 2 - shadow_volumes
Case 3 - image_processing
Case 4 - particle_system
Reference pipeline (fixed allocation of resources) - reference_pipeline

REGARDING STATE IN THESE BENCHMARKS
-----------------------------------

Most of the benchmarks are stateless except for the RasterOps filter.
However, the compiler detects lots of false state, because many local
variables (that are structures) were moved to filter fields in order
to reduce the overhead of initializing them to zero on every
invocation of work.

ORIGINAL SOURCE
---------------

If something is missing or wrong in CVS, see Jiawen's working
directories at /home/streamit/graphics-apps on CAG.
