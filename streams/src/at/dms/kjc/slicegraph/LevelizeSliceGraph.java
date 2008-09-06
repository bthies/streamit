package at.dms.kjc.slicegraph;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

public class LevelizeSliceGraph {
    private Slice[] topSlices;
    private HashMap<Slice, Integer> levelMap;
    private Slice[][] levels;
    
    public LevelizeSliceGraph(Slice[] topSlices) {
        this.topSlices = topSlices;
        levelMap = new HashMap<Slice, Integer>();
        calcLevels();
    }
    
    public Slice[][] getLevels() {
        return levels;
    }
    
    private void calcLevels() {
        LinkedList<LinkedList<Slice>> levelsList = new LinkedList<LinkedList<Slice>>();
        HashSet<Slice> visited = new HashSet<Slice>();
        LinkedList<Slice> queue = new LinkedList<Slice>();
        
        //add the top slices and set their level
        for (int i = 0; i < topSlices.length; i++) {
            queue.add(topSlices[i]);
            levelMap.put(topSlices[i], 0);
        }
        
        while (!queue.isEmpty()) {
            Slice slice = queue.removeFirst();
            if (!visited.contains(slice)) {
                visited.add(slice);
                for (Edge destEdge : slice.getTail().getDestSet(SchedulingPhase.STEADY)) {
                    Slice current = destEdge.getDest().getParent();
                    if (!visited.contains(current)) {
                        // only add if all sources has been visited
                        boolean addMe = true;
                        int maxParentLevel = 0;
                        for (Edge sourceEdge : current.getHead().getSourceSet(SchedulingPhase.STEADY)) {
                            if (!visited.contains(sourceEdge.getSrc().getParent())) {
                                addMe = false;
                                break;
                            }
                            //remember the max parent level
                            if (levelMap.get(sourceEdge.getSrc().getParent()).intValue() > maxParentLevel)
                                maxParentLevel = levelMap.get(sourceEdge.getSrc().getParent()).intValue();
                        }   
                        if (addMe) {
                            levelMap.put(current, maxParentLevel + 1);
                            queue.add(current);
                        }
                    }
                }
                //add the slice to the appropriate level
                int sliceLevel = levelMap.get(slice);
                if (levelsList.size() <= sliceLevel) {
                    int levelsToAdd = sliceLevel - levelsList.size() + 1;
                    for (int i = 0; i < levelsToAdd; i++);
                        levelsList.add(new LinkedList<Slice>());
                }
                levelsList.get(sliceLevel).add(slice);
            }
        }
        
        //set the multi-dim array for the levels from the linkedlist of lists 
        levels = new Slice[levelsList.size()][];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = new Slice[levelsList.get(i).size()];
            for (int j = 0; j < levels[i].length; j++) 
                levels[i][j] = levelsList.get(i).get(j);
        }
    }
    
}
