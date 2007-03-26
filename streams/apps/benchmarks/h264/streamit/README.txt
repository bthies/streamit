Notes taken by Bill and Shirley on 3/26/07:

USEFUL IN CURRENT FORM
----------------------

IntraPrediction -- toplevel file of what is complete
  - "complete intraprediction for 16x16 case"
    - missing 4x4 prediction (that's it)
    - and transforms may have to be modified for 4x4 case (use 
      different set of transforms).  All commented Transforms.str file.

  - now uses dummy input, but could substitute YUVFileReader 
    instead of IntStream and this should work.  The dummy input was 
    only to simplify testing and verification.

  - uses the following files:
    - PredictionModes16x16Luma.str
    - Transforms.str

  - currently limited to 16x16 but should be extended to include code
    from LumaPredictionModes4x4 to do 4x4 prediction as well.

  - width and height are currently hard-coded, so be careful plugging other inputs

Also, the test videos under ../testvideos have been tested to work
with the YUVFileReader.


COULD BE USEFUL LATER
---------------------

YUVFileReader -- contains functional YUVFileReader parser / to int-stream

LumaPredictionModes4x4.str is functional and tested but not yet
integrated into IntraPrediction
  - just have to figure out data re-arrangement to get data into 4x4s

MPEG4Encoder -- trying to work out high-level structure, but not yet complete


NOT USEFUL
----------

TESTTransforms.str -- just a debugging class, not useful as a benchmark

VideoProcessor.str -- something on evolution path, not a meaningful component now
  - uses structs instead of ints
