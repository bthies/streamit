package at.dms.kjc.spacetime.switchIR;

import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class BnezdIns extends SwitchProcessorIns {
    public BnezdIns(SwitchReg dest,SwitchReg src,String target) {
	op="bnezd";
	arg1=dest.toString();
	arg2=src.toString();
	arg3=target;
    }
}
