package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.*;
import java.util.HashMap;
import java.util.Random;
import at.dms.kjc.sir.*;

public class LevelMap {
    
    private static HashMap<FlatNode, Integer> levels; 
    public static int maxLevel = 0;
    private static String[] levelColors;
    
    static {    
        levels = new HashMap<FlatNode, Integer>();
    }
    
    public static void createLevelColors() {
        levelColors = new String[maxLevel+1];
        System.out.println("++++++++++++++++");
        Random random = new Random(15);
        for (int i = 0; i < levelColors.length; i++) {
            levelColors[i] = "\"#";
            for (int c = 0; c < 3; c++) {
               String cc = Integer.toHexString(random.nextInt(256));
               
               if (cc.length() < 2) {
                  cc = "0" + cc;  
               }
               
               levelColors[i] = levelColors[i] + cc; 
            }
            levelColors[i] = levelColors[i] + "\"";
            System.out.println(i + " = " + levelColors[i]);
        } 
        System.out.println("++++++++++++++++");
    }
    
    public static String getLevelColor(int l) {
        return levelColors[l];
    }
    
    public static String getLevelColor(FlatNode node) {
        //color spliltters and joiner white
        
        if (node.isJoiner() || node.isSplitter()) 
            return "grey";
        if (node.contents instanceof SIRFileReader || node.contents instanceof SIRFileWriter)
            return "white";
        
        return getLevelColor(getLevel(node));
    }
    
    public static int getLevel(FlatNode node) {
        assert levels.containsKey(node);
        return levels.get(node).intValue();
    }
    
    public static void buildMap(FlatNode current, int level) {
        if (level > maxLevel)
            maxLevel = level;
        
        //if we hit the filter for multiple times, then just take the max level
        if (levels.containsKey(current)) {
            if (levels.get(current).intValue() > level)
                return;
        }
                    
        levels.put(current, new Integer(level));
        for (int i = 0; i < current.ways; i++) {
            buildMap(current.getEdges()[i], level + 1);
        }
    }
}
