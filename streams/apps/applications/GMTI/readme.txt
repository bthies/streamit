GMTI(Ground Moving Target Indicator) Application

  The software implementation of the GMTI app consists of seven stages of a processing chain that pass information in a linear fashion (the first passes its output to the second as an input, the second to the third, etc). In the streamit code, the first and last stages are omitted. The first stage does nothing, so its ommision does not change the program. The last stage, Target Parameter Estimation, is difficult to implement without dynamic rates (currently unsupported by streamit); moreover for the purposes of this project it is not crucial to the processing chain. The remaining five stages, in order, are: Adaptive Beamform, Pulse Compression, Doppler Filter, Stap, Target Detection. 

  The streamit code for this entire chain is intended to emulate the MATLAB code. Since the MATLAB code uses native libraries, which are not currently available in streamit, we had to construct a set of library filters. The majority of these filters are in the file matlabFunctions.str, and most of them deal with matrix manipulation. Other library files are: 

lqHouse.str - outputs L from the LQ decomposition of a matrix input.

WienerHopfSolution.str - outputs W from matrix inputs A and V, where 
 W = inv(A*A')*V. This NOT done by taking the inverse of A, since this might be numerically unstable - instead L is calculated from A, and W is calculated using forward and back substitution.

qr.str - outputs Q or R or both from the QR decomposition of a matrix input.

DFT.str - outputs the DFT or IDFT of a vector.

  The processing chain recieves as input a three-dimensional data cube, representing digitized pulse responses to a set of channel sensors during one cpi (coherent processing interval). A cpi is divided into pri's (pulse repetition intervals), during which a set of pulses is transmitted and recieved. Therefore, the data cube's dimensions are for these three variables are channel sensors, digitized values per pri, pri's per cpi, and the data cube corresponds to exactly one cpi. Running the streamit code with command '-i 1' will process 1 data cube, while running it with command '-i k' will process k data cubes (for k cpi's).

  The processing chain outputs a three-dimensional cube, with the dimensions corresponding to angle of elevation, position, and velocity of located targets. Values of 0 indicate no target was found at the indicated triple, non-zero values indicate a target was found. The target's SNR (signal to noise ratio) is given by this non-zero value.

  The GMTI uses a large number of constants. To facilitate this use, two data structures - GmtiParamType and AntParamTypes - store these values. The file DataStruct.str creates these data structures, and the main file, Tester.str, populates them with the appropriate values. Each stage of the processing chain recieves one or both of these structures as a parameter, while data cubes are recieved as a stream. The data structures are unchanged after populization, so they can be thought of as global constants. That is the reason why they are not passed along streams - only data that is manipulated (ie the data cubes) should be passed along a stream, while constants should be passed as parameters.

  Now we will look at the str files corresponding to each stage.


Adaptive Beamform: The top level pipeline for this stage is located in AdaptiveBeamform.str. It receives as input the initial data cube, and outputs T1, V1, Wabf, Data1 in that order. The first three are two-dimensional matrices, while Data1 is the processed data cube. V1 and T1 are generated from a void->complex pipeline in ABsteeringMatrix.str. This means their values are independent of the data cube, and only depend on the parameters in GmtiParam. They are used to calculate Wabf, and are also needed in later stages.


Pulse Compression: This stage is contained in Pulse.str. It receives a data cube Data1 and outputs a data cube Data2. It is an FIR filter applied to the columns of the inputted data cube. The weights of the filter are calculated from constants in GmtiParam (again, they are independent of the data), and filter convolution is done by taking the DFT of a data column and filter, multiplying the two vectors, then taking the IDFT of the result (this is convolution in the frequency domain). The convolution is done in this manner because for long filters it is more efficient than a straightforward time domain convolution.  


Doppler Filter: Located in DopplerFilter.str. It receives Data2 as input and outputs Data3. This stage uses a Chebychev window to taper data values. The window is stored in an array in matlabFunctions.str(it isn't generated).


STAP (Space-Time Adaptive Processing): Located in Stap.str. It receives Data3, T1, Wabf as input and outputs Astapset, Data4. This stage is very similar to Adaptive Beamform.


Target Detection: Located in Detection.str. Receives Data4 and outputs a Target Report, which is composed of zeros and SNRs as described above. Detection uses the file Cfar.str, which generates a list of preliminary targets. Each preliminary target point is checked against its neighbors - if the point is a local maximum, it is kept as a final target to be outputted, otherwise it is discarded.





