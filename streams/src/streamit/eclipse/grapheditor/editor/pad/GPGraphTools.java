/*
 * @(#)GPGraphTools.java	1.2 11/11/02
 *
 * Copyright (C) 2001 Gaudenz Alder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphModel;

public class GPGraphTools {

	public CostFunction createDefaultCostFunction() {
		return new DefaultCostFunction();
	}

	//
	// Component Counting
	//

	public int getComponentCount(GPGraph graph) {
		UnionFind uf = new UnionFind();
		Object[] all = graph.getAll();
		// Vertices
		Object[] v = graph.getVertices(all);
		for (int i = 0; i < v.length; i++)
			uf.find(v[i]);
		// Edges
		Object[] e = graph.getEdges(all);
		for (int i = 0; i < e.length; i++) {
			Object source = graph.getSourceVertex(e[i]);
			Object target = graph.getTargetVertex(e[i]);
			uf.union(uf.find(source), uf.find(target));
		}
		return uf.getSetCount();
	}

	/** Returns the ShortestPath. Implemented with the
	* 	Dijkstra Algorithm
	*
	*/

	public Object[] getShortestPath(
		GPGraph graph,
		Object from,
		Object to,
		CostFunction cf) {
		if (cf == null)
			cf = createDefaultCostFunction();
		GraphModel model = graph.getModel();
		PriorityQueue q = new PriorityQueue();
		Hashtable pred = new Hashtable();
		q.setPrio(from, 0);
		// Main Loop
		Object[] all = graph.getAll();
		int jmax = graph.getVertices(all).length;
		for (int j = 0; j < jmax; j++) {
			double prio = q.getPrio();
			Object obj = q.pop();
			if (obj == to)
				break;
			Object[] tmp = new Object[] { obj };
			Object[] e = DefaultGraphModel.getEdges(model, tmp).toArray();
			if (e != null) {
				for (int i = 0; i < e.length; i++) {
					Object neighbour = graph.getNeighbour(e[i], obj);
					double newPrio = prio + cf.getCost(graph, e[i]);
					if (neighbour != null
						&& neighbour != obj
						&& newPrio < q.getPrio(neighbour)) {
						pred.put(neighbour, e[i]);
						q.setPrio(neighbour, newPrio);
					}
				}
			}
			if (q.isEmpty())
				break;
		}
		// Return Path-Array
		ArrayList list = new ArrayList();
		Object obj = to;
		while (obj != null) {
			list.add(obj);
			Object edge = pred.get(obj);
			if (edge != null) {
				list.add(edge);
				obj = graph.getNeighbour(edge, obj);
			} else
				obj = null;
		}
		return list.toArray();
	}

	/** Returns the shortest spanning tree.
	 *  Implemented with the Kruskal Algorithm
	*/
	public Object[] getSpanningTree(GPGraph graph, CostFunction cf) {
		if (cf == null)
			cf = createDefaultCostFunction();
		Object[] all = graph.getAll();
		SortedSet edges = sort(graph, graph.getEdges(all), cf);
		UnionFind uf = new UnionFind();
		HashSet result = new HashSet();
		while (!edges.isEmpty()) {
			Object edge = edges.first();
			edges.remove(edge);
			Object setA, setB;
			setA = uf.find(graph.getSourceVertex(edge));
			setB = uf.find(graph.getTargetVertex(edge));
			if (setA == null || setB == null || setA != setB) {
				uf.union(setA, setB);
				result.add(edge);
			}
		}
		// Create set of vertices
		HashSet v = new HashSet();
		Iterator it = result.iterator();
		while (it.hasNext()) {
			Object edge = it.next();
			Object source = graph.getSourceVertex(edge);
			Object target = graph.getTargetVertex(edge);
			if (source != null)
				v.add(source);
			if (target != null)
				v.add(target);
		}
		Object[] cells = new Object[result.size() + v.size()];
		System.arraycopy(result.toArray(), 0, cells, 0, result.size());
		System.arraycopy(v.toArray(), 0, cells, result.size(), v.size());
		return cells;
	}

	//
	// Sorting With CostFunction
	//

	public SortedSet sort(
		final JGraph graph,
		Object[] cells,
		final CostFunction cf) {
		TreeSet set = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				Double d1 = new Double(cf.getCost(graph, o1));
				Double d2 = new Double(cf.getCost(graph, o2));
				return d1.compareTo(d2);
			}
		});
		for (int i = 0; i < cells.length; i++)
			set.add(cells[i]);
		return set;
	}

	//
	// Cost Function
	//

	public interface CostFunction {

		public double getCost(JGraph graph, Object cell);

	}

	public class DefaultCostFunction implements CostFunction {

		public double getCost(JGraph graph, Object cell) {
			CellView view = graph.getGraphLayoutCache().getMapping(cell, false);
			return getLength(view);
		}
	}

	public static double getLength(CellView view) {
		double cost = 1;
		if (view instanceof EdgeView) {
			EdgeView edge = (EdgeView) view;
			Point last = null, current = null;
			for (int i = 0; i < edge.getPointCount(); i++) {
				current = edge.getPoint(i);
				if (last != null)
					cost += last.distance(current);
				last = current;
			}
		}
		return cost;
	}

	//
	// Union-Find
	//

	public class UnionFind {

		protected Hashtable sets = new Hashtable(), cells = new Hashtable();

		/* Return the number of distinct sets. */
		public int getSetCount() {
			return sets.size();
		}

		/* Return an object identifying the set that contains the given cell. */
		public Object find(Object cell) {
			Object set = null;
			if (cell != null) {
				set = cells.get(cell);
				if (set == null) {
					set = cell;
					cells.put(cell, set);
					HashSet contents = new HashSet();
					contents.add(cell);
					sets.put(set, contents);
				}
			}
			return set;
		}

		/* Union the given sets such that all elements belong to the same set. */
		public Object union(Object set1, Object set2) {
			if (set1 != null && set2 != null && set1 != set2) {
				HashSet tmp1 = (HashSet) sets.get(set1);
				HashSet tmp2 = (HashSet) sets.get(set2);
				if (tmp1 != null && tmp2 != null) {
					if (tmp1.size() < tmp2.size()) {
						Object tmp = tmp1;
						tmp1 = tmp2;
						tmp2 = (HashSet) tmp;
						tmp = set1;
						set1 = set2;
						set2 = tmp;
					}
					tmp1.addAll(tmp2);
					sets.remove(set2);
					Iterator it = tmp2.iterator();
					while (it.hasNext())
						cells.put(it.next(), set1);
				}
			}
			return set1;
		}

	}

	//
	// Priority Queue
	//

	public class PriorityQueue {

		protected Hashtable prio = new Hashtable();

		protected HashSet data = new HashSet();

		protected double minPrio = Double.MAX_VALUE;

		protected Object minElt = null;

		public boolean isEmpty() {
			return data.isEmpty();
		}

		// Removes element but holds prio
		public Object pop() {
			Object tmp = minElt;
			data.remove(tmp);
			update();
			return tmp;
		}

		/* Return the priority of the top element. */
		public double getPrio() {
			return minPrio;
		}

		/* Return the priority of the given element. */
		public double getPrio(Object obj) {
			if (obj != null) {
				Double d = (Double) prio.get(obj);
				if (d != null)
					return d.doubleValue();
			}
			return Double.MAX_VALUE;
		}

		protected void update() {
			Iterator it = data.iterator();
			minElt = null;
			minPrio = Double.MAX_VALUE;
			while (it.hasNext()) {
				Object tmp = it.next();
				double prio = getPrio(tmp);
				if (prio < minPrio) {
					minPrio = prio;
					minElt = tmp;
				}
			}
		}

		/* Set the priority of the given element and add to queue if necessary. */
		public void setPrio(Object obj, double prio) {
			Double d = new Double(prio);
			this.prio.put(obj, d);
			data.add(obj);
			if (prio < minPrio) {
				minPrio = prio;
				minElt = obj;
			}
		}

	}

}