GMTI(Ground Moving Target Indicator) Application

  The software implementation of the GMTI app consists of seven stages of a processing chain that pass information in a linear fashion (the first passes its output to the second as an input, the second to the third, etc). In the streamit code, the first and last stages are omitted. The first stage does nothing, so its ommision does not change the program. The last stage, Target Parameter Estimation, is difficult to implement without dynamic rates (currently unsupported by streamit); moreover for the purposes of this project it is not crucial to the processing chain. The remaining five stages, in order, are: Adaptive Beamform, Pulse Compression, Doppler Filter, Stap, Target Detection. 

  The streamit code for this entire chain is intended to emulate the MATLAB code. Since the MATLAB code uses native libraries, which are not currently available in streamit, we had to construct a set of library filters. The majority of these filters are in the file matlabFunctions.str, and most of them deal with matrix manipulation. Other library files are: 

lqHouse.str - outputs L from the LQ decomposition of a matrix input.

WienerHopfSolution.str - outputs W from matrix inputs A and V, where 
 W = inv(A*A')*V. This NOT done by taking the inverse of A, since this might be numerically unstable - instead L is calculated from A, and W is calculated using forward and back substitution.

qr.str - outputs Q or R or both from the QR decomposition of a matrix input.

DFT.str - outputs the DFT or IDFT of a vector.

  The processing chain recieves as input a three-dimensional data cube, representing digitized pulse responses to set of channel sensors during one cpi (coherent processing interval). A cpi is divided into pri's (pulse repetition intervals), during which a set of pulses is transmitted and recieved. Therefore, the data cube's dimensions are for these three variables are channel sensors, digitized values per pri, pri's per cpi, and the data cube corresponds to exactly one cpi. Running the streamit code with command '-i 1' will process 1 data cube, while running it with command '-i k' will process k data cubes (for k cpi's).

  The processing chain outputs a three-dimensional cube, with the dimensions corresponding to angle of elevation, position, and velocity of located targets. Values of 0 indicate no target was found at the indicated triple, non-zero values indicate a target was found. The target's SNR (signal to noise ratio) is given by this non-zero value.

  Now we will look at the str files corresponding to each stage.


