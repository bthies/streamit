package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Hashtable;

import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.Edge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;

public class Touch implements Runnable {

	private GPGraph graph;
	private Hashtable deltas = new Hashtable();
	private Hashtable positions = new Hashtable();
	private Thread relaxer;
	private boolean allowedToRun = false;
	private boolean repaintNeeded = false;
	private double damper = 1.0;
	// A low damper value causes the graph to move slowly
	private double maxMotion = 0;
	// Keep an eye on the fastest moving node to see if the graph is stabilizing
	private double lastMaxMotion = 0;
	private double motionRatio = 0;
	// It'buttonSelect sort of a ratio, equal to lastMaxMotion/maxMotion-1
	private boolean damping = true;
	// When damping is true, the damper value decreases

	private double rigidity = 1.5;
	// Rigidity has the same effect as the damper, except that it'buttonSelect a constant
	// a low rigidity value causes things to go slowly.
	// a value that'buttonSelect too high will cause oscillation
	private double newRigidity = 1;

	// ............

	/** Constructor with a supplied TGPanel <tt>tgp</tt>.
	  */
	public Touch(GPGraph graph) {
		this.graph = graph;
		relaxer = null;
	}

	void setRigidity(double r) {
		newRigidity = r; //update rigidity at the end of the relax() thread
	}

	//relaxEdges is more like tense edges up.  All edges pull nodes closer together;
	private synchronized void relaxEdges() {
		Object[] edges = graph.getEdges(graph.getAll());
		//System.out.println("edges="+edges.length);
		for (int i = 0; i < edges.length; i++) {
			CellView from = graph.getSourceView(edges[i]);
			CellView to = graph.getTargetView(edges[i]);
			CellView fromV =
				graph.getGraphLayoutCache().getMapping(
					((Edge) edges[i]).getSource(),
					false);
			CellView toV =
				graph.getGraphLayoutCache().getMapping(
					((Edge) edges[i]).getTarget(),
					false);

			if (from != null && to != null) {
				Rectangle bf = fromV.getBounds();
				// GraphConstants.getBounds(fromV.getAttributes());
				Rectangle bt = toV.getBounds();
				//GraphConstants.getBounds(toV.getAttributes());
				//Point2D.Double bf = getPosition(from);
				//Point2D.Double bt = getPosition(to);
				double vx = bt.x - bf.x;
				double vy = bt.y - bf.y;
				double len = Math.sqrt(vx * vx + vy * vy);
				double dx = vx * rigidity; //rigidity makes edges tighter
				double dy = vy * rigidity;
				double length = getLength(edges[i]) * 100;
				dx /= length;
				dy /= length;
				moveView(to, -dx * len, -dy * len);
				moveView(from, dx * len, dy * len);

			}
		}
	}

	public double getLength(Object edge) {
		CellView view = graph.getGraphLayoutCache().getMapping(edge, false);
		return GPGraphTools.getLength(view);
	}

	private synchronized void avoidLabels() {
		Object[] vertices = graph.getVertices(graph.getAll());
		for (int i = 0; i < vertices.length; i++) {
			for (int j = i + 1; j < vertices.length; j++) {
				CellView from = graph.getGraphLayoutCache().getMapping(vertices[i], false);
				CellView to = graph.getGraphLayoutCache().getMapping(vertices[j], false);
				if (from != null && to != null) {
					Point2D.Double bf = getPosition(from);
					
					if (bf == null)
						continue;
						
					Point2D.Double bt = getPosition(to);
					if (bt == null)
						continue;

					double dx = 0;
					double dy = 0;
					double vx = bf.x - bt.x;
					double vy = bf.y - bt.y;
					double len = vx * vx + vy * vy; //so it'buttonSelect length squared
					if (len == 0) {
						dx = 0.1;
						dy = 0.1;
					} else if (len < 200 * 200) {
						dx = vx / len;
						dy = vy / len;
					}

					double repX = Math.max(from.getBounds().getWidth(), to.getBounds().getWidth());
					double repY = Math.max(from.getBounds().getHeight(), to.getBounds().getHeight());
					repX=repX*4;
					repY=repY*4;
					repX = repX*repX/100;
					repY = repY*repY/100;

					repX = Math.max(repX,repY);
					if (Math.random()>0.3){
						moveView(from, dx * repX, dy * repY);
						moveView(to, -dx * repY, -dy * repX);
					}
					else {
						moveView(from, dx * repX*3, dy * repY*3);
						moveView(to, -dx * repX, -dy * repY*3);
					}
//					else {
//						moveView(from, dx * repX*15*Math.random(), dy * repY);
//						moveView(to, -dx * repX*10*Math.random(), -dy * repY*10*Math.random());
//					}
				}
			}
		}
	}

	public void startDamper() {
		damping = true;
	}

	public void stopDamper() {
		damping = false;
		damper = 1.0; //A value of 1.0 means no damping
	}

	public void resetDamper() { //reset the damper, but don't keep damping.
		damping = true;
		damper = 1.0;
	}

	public void setDamper(double newValue) {
		damper = newValue;
	}

	public void damp() {
		if (damping) {
			if (motionRatio <= 0.001) {
				//This is important.  Only damp when the graph starts to move faster
				//When there is noise, you damp roughly half the time. (Which is a lot)
				//
				//If things are slowing down, then you can let them do so on their own,
				//without damping.

				//If max motion<0.2, damp away
				//If by the time the damper has ticked down to 0.9, maxMotion is still>1, damp away
				//We never want the damper to be negative though
				if ((maxMotion < 0.2 || (maxMotion > 1 && damper < 0.9))
					&& damper > 0.01)
					damper -= 0.01;
				//If we've slowed down significanly, damp more aggresively (then the line two below)
				else if (maxMotion < 0.4 && damper > 0.003)
					damper -= 0.003;
				//If max motion is pretty high, and we just started damping, then only damp slightly
				else if (damper > 0.0001)
					damper -= 0.0001;
			}
		}
		if (maxMotion < 0.001 && damping)
			damper = 0;
		//System.out.println("damper="+damper);
	}

	private synchronized void moveNodes() {
		lastMaxMotion = maxMotion;
		double maxMotionA = 0;
		Object[] vertices = graph.getVertices(graph.getAll());
		for (int i = 0; i < vertices.length; i++) {
			CellView view = graph.getGraphLayoutCache().getMapping(vertices[i], false);
			if (view != null) {
				Rectangle bounds =
					GraphConstants.getBounds(view.getAllAttributes());
				Point2D.Double delta = getDelta(view);
				Point2D.Double position = getPosition(view);
				//deltas.remove(view);
				double dx = delta.getX();
				double dy = delta.getY();
				dx *= damper;
				dy *= damper;
				delta.setLocation(dx / 2, dy / 2);
				double distMoved = Math.sqrt(dx * dx + dy * dy);
				if (GraphConstants.isMoveable(view.getAllAttributes())
					&& !graph.isCellSelected(vertices[i])
					&& (dx != 0 || dy != 0)) {
					//System.out.println("dx: " + dx + " dy: "+dy);
					position.x += Math.max(-5, Math.min(5, dx));
					position.y += Math.max(-5, Math.min(5, dy));
					bounds.x = Math.max(0, (int) position.x - bounds.width/2);
					bounds.y =
						Math.max(0, (int) position.y - bounds.height/2);
					repaintNeeded = true;
					// Invalidate cached edge shapes
					Object[] edges = DefaultGraphModel.getEdges(graph.getModel(), new Object[]{view.getCell()}).toArray();
					CellView[] v = graph.getGraphLayoutCache().getMapping(edges, false);
					for (int j=0; j<v.length; j++)
					 if (v[j] instanceof EdgeView)
					 	((EdgeView) v[j]).update();
				}
				maxMotionA = Math.max(distMoved, maxMotionA);

			}
		};
		maxMotion = maxMotionA;
		if (maxMotion > 0)
			motionRatio = lastMaxMotion / maxMotion - 1;
		else
			motionRatio = 0;
		damp();
	}

	private synchronized void relax() {
		for (int i = 0; i < 10; i++) {
			relaxEdges();
			avoidLabels();
			moveNodes();
		}
		if (rigidity != newRigidity)
			rigidity = newRigidity; //update rigidity
		if (repaintNeeded) {
			graph.repaint();
			repaintNeeded = false;
		}
	}

	public void run() {
		Thread me = Thread.currentThread();
		while (relaxer == me && allowedToRun) {
			relax();
			//System.out.println("D "+damper);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public void start() {
		relaxer = new Thread(this);
		allowedToRun = true;
		relaxer.start();
	}

	public boolean isRunning() {
		if (relaxer != null)
			return relaxer.isAlive();
		return false;
	}

	public void stop() {
		allowedToRun = false;
		relaxer = null;
	}

	public Point2D.Double getPosition(CellView view) {
		Point2D.Double p1 = (Point2D.Double) positions.get(view);

		Rectangle rect = GraphConstants.getBounds(view.getAllAttributes());
		
		// precondition test
		if (rect == null)
			return null;
			
		Point2D.Double p2 =
			new Point2D.Double(
				rect.x + rect.width / 2,
				rect.y + rect.height / 2);

		if (p1 != null) {
			if (Math.abs(p1.x - p2.x) > 5 || Math.abs(p1.y - p2.y) > 5) {
				//System.out.println("p1.x " +p1.x +" p2.x " +p2.x);
				p1.setLocation(p2.x, p2.y);
			}
			return p1;
		} else {
			positions.put(view, p2);
			return p2;
		}
	}

	public Point2D.Double getDelta(CellView view) {
		Point2D.Double p = (Point2D.Double) deltas.get(view);
		if (p == null) {
			p = new Point2D.Double(0, 0);
			deltas.put(view, p);
		}
		return p;
	}

	public void moveView(CellView view, double dx, double dy) {
		//System.out.println("set Delta for "+view+" to "+dx+", "+dy);
		Point2D.Double p = getDelta(view);
		p.setLocation(p.getX() + dx, p.getY() + dy);
	}

}