package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

//Move Instruction
public class MoveIns extends SwitchProcessorIns {
    public MoveIns(SwitchReg dest,SwitchSrc src) {
	op="move";
	arg1=dest.toString();
	arg2=src.toString();
    }
}
