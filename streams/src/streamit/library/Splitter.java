package streamit;

// 2 input, 1 output
public class Splitter extends Operator {
    public static final Splitter ROUND_ROBIN_SPLITTER = new Splitter();
    public static final Splitter DUPLICATE_SPLITTER = new Splitter();
}

