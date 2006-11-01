package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterContent;
import at.dms.util.Utils;

/**
 * This class will fiss a linear filter in a pipeline of smaller filters, where
 * each filter has a subset of the original weights.  This only works
 * on linear filters where A in Ax + b is one dimensional.
 **/
public class LinearFission {
    /** 
     * Fiss linear FilterContent <pre>content</pre> into a pipeline of <pre>num</pre> filters and return 
     * the new FilterContents, right now, <pre>num</pre> must evenly divide the number
     * weights of the linear filter <pre>content</pre>.
     **/
    public static FilterContent[] fiss(FilterContent content, int num) {
        assert content.isLinear() : "Trying to Linear fiss a nonlinear filter";
        //the A array
        double[] A_array = content.getArray();
        //the B constant
        double B_constant = content.getConstant();

        assert A_array.length % num == 0 : 
            "Trying to Linear fiss a linear filter into unequal parts";
    
        //the number of weights per fissed filter
        //(int)Math.ceil(((double)array.length)/num);
        final int weightsPerTile =  A_array.length / num;

        // final int realNum = (int)Math.ceil(((double)array.length)/weightsPerTile);
    
        //record the number of filters we are going to create
        content.setTotal(num);

        FilterContent[] out = new FilterContent[num];
    
        for(int i = 0; i < num; i++)
            out[i] = new FilterContent(content);
    
        //
        double[] temp = new double[weightsPerTile];
    
        if (num > 1) {
            //create the first filter in the pipeline
            System.arraycopy(A_array, 0, temp, 0, weightsPerTile);
            out[0].setArray(temp);
            out[0].setBegin(true);
            out[0].setEnd(false);
            out[0].setPos(num - 1);

            int idx = weightsPerTile;
            for(int i = 1;i < num-1; i++, idx+=weightsPerTile) {
                temp = new double[weightsPerTile];
                System.arraycopy(A_array,idx,temp,0,weightsPerTile);
                out[i].setArray(temp);
                out[i].setPos(num-i-1);

                out[i].setBegin(false);
                out[i].setEnd(false);
            }
            temp=new double[weightsPerTile];
            System.arraycopy(A_array,idx,temp,0,A_array.length-idx);
            out[num-1].setArray(temp);
            out[num-1].setBegin(false);
            out[num-1].setEnd(true);
            out[num-1].setPos(0);

        } 
        else {  //here we are just making a copy
            System.arraycopy(A_array, 0, temp, 0, A_array.length);
            out[0].setArray(temp);
            out[0].setBegin(true);
            out[0].setEnd(true);
            out[0].setPos(0);
        }
        return out;
    }
}
