/*
 * Created on Dec 1, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad;

/**
 * @author jcarlos
 *
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**File format for the default graph model.
 * The file format writes a XML
 * file with the graph as content.
 *
 * @author luzar
 * @version 1.0
 */
public class DefaultGraphModelFileFormatStreamIt implements GraphModelFileFormat {

	public static final String EMPTY = new String("Empty");

	public static final String PARENT = new String("Parent");

	protected static Map cells = new Hashtable();

	protected static Map attrs = new Hashtable();

	protected static Map objs = new Hashtable();

	protected static List delayedAttributes = new LinkedList();

	protected static List connectionSetIDs = new LinkedList();

	protected Map cellMap = new Hashtable();

	protected AttributeCollection attrCol = new AttributeCollection();

	protected Map userObjectMap = new Hashtable();

	/** file filter for this file format
	 */
	FileFilter fileFilter;

	/** accessory component. A checkbox for the
	 *  zipped output or unzipped output
	 *
	 */
	JComponent compZipSelect;

	/** a const value for the key at the properties hashtable
	 */
	public static final String COMPRESS_WITH_ZIP = "CompressWithZip";

	/**
	 * Constructor for DefaultGraphModelFileFormatXML.
	 */
	public DefaultGraphModelFileFormatStreamIt() {
		fileFilter = new FileFilter() {
			/**
			 * @see javax.swing.filechooser.FileFilter#accept(File)
			 */
			public boolean accept(File f) {
				if (f == null)
					return false;
				if (f.getName() == null)
					return false;
				if (f.getName().endsWith(".str"))
					return true;
				if (f.isDirectory())
					return true;

				return false;
			}

			/**
			 * @see javax.swing.filechooser.FileFilter#getDescription()
			 */
			public String getDescription() {
				return Translator.getString("FileDesc.StreamItFileExtenstion"); /*Finished: Original="JGraphpad Diagram (*.pad_xml)"*/
			}
		};
		compZipSelect = new JCheckBox(Translator.getString("zipCompress"));
	}

	/** returns <tt>pad_xml</tt>
	 */
	public String getFileExtension(){
		return "str";
	}


	/** Returns a file filter for the <tt>pad_xml</tt> extension.
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getFileFilter()
	 */
	public FileFilter getFileFilter() {
		return fileFilter;
	}

	/** Returns null
	 * @see org.jgraph.pad.GraphModelFileFormat#getReadAccessory()
	 */
	public JComponent getReadAccessory() {
		return null;
	}

	/** Returns the compZipSelect object.
	 *
	 * @see #compZipSelect
	 * @see org.jgraph.pad.GraphModelFileFormat#getWriteAccessory()
	 */
	public JComponent getWriteAccessory() {
		return null; //compZipSelect;
	}

	/**
	 * Writes the graph as XML file
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#write(String, Hashtable, GPGraph, GraphModel)
	 */
	public void write(
		URL file,
		Hashtable properties,
		GPGraph gpGraph,
		GraphModel graphModel)
		throws Exception {

		// don't try / catch this command
		// sothat we get error messages at the 
		// frontend.
		// e.g. I you have not permissions to
		// write a file you should get an error message
		
		//try {
			OutputStream out = null;
			out = new FileOutputStream(file.getFile());
// Ignore
//		if (properties != null &&
//			properties.get(COMPRESS_WITH_ZIP) != null &&
//			((Boolean)properties.get(COMPRESS_WITH_ZIP)).booleanValue() )
//			f = new GZIPOutputStream(f);
			out = new BufferedOutputStream(out);
			out.write(toString(gpGraph).getBytes());
			out.flush();
			out.close();
			
		//} catch (Exception e) {
		//	e.printStackTrace();
		//}

	}

	/**
	 * Puts the value from the checkbox into the properties hashtable
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getWriteProperties(JComponent)
	 */
	public Hashtable getWriteProperties(JComponent accessory) {
		Hashtable properties = new Hashtable();
		if (!(accessory instanceof JCheckBox)){
			return properties;
		}
		properties.put(COMPRESS_WITH_ZIP, new Boolean(((JCheckBox)accessory).isSelected() ));
		return properties;
	}

	/**Reads the File form the XML input stream.
	 * Tempts to load from a zipped input stream.
	 * If this procedure fails the method tempts
	 * to load without the zipped option.
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#read(String, Hashtable, GPGraph)
	 */
	public GraphModel read(
		URL file,
		Hashtable properties,
		GPGraph gpGraph)
		throws Exception {

		// Check file contents for new file format
		boolean newFileFormat = false;
		try {
			InputStream f = file.openStream();
			byte[] ident = new byte[4];
			int length = f.read(ident);
			if (length == 4 && new String(ident).toLowerCase().equals("<str"))
			newFileFormat = true;
		} catch (Exception e) {
		// ignore
		}

		// Read old file format...
		if (!newFileFormat) {
		try {
			InputStream f = file.openStream();
			XMLDecoder dec =
			new XMLDecoder(new BufferedInputStream(f));
			gpGraph.setArchivedState(dec.readObject());
		} catch (Exception ex) {
			try {
			// Try to load as compressed old file:
			GZIPInputStream zipf = new GZIPInputStream(file.openStream());
			XMLDecoder dec =
				new XMLDecoder(new BufferedInputStream(zipf));
			gpGraph.setArchivedState(dec.readObject());
			} catch (Exception e) {
			// Read as new file
			try {
				read(file.openStream(), gpGraph);
			} catch (Exception ex2) {
				if (file.toString().toLowerCase().startsWith("http") ||
				file.toString().toLowerCase().startsWith("ftp") ) {
				// do nothing create an empty graph
				} else {
				throw ex2;
				}
			}
			}
		}
		} else {
		// Read as new file
		// Copied from above: Use only this in future versions
		try {
			read(file.openStream(), gpGraph);
		} catch (Exception ex2) {
			if (file.toString().toLowerCase().startsWith("http") ||
			file.toString().toLowerCase().startsWith("ftp") ) {
			// do nothing create an empty graph
			} else {
			throw ex2;
			}
		}
		}

		return gpGraph.getModel();
	}

	/**Returns null
	 *
	 * @see org.jgraph.pad.GraphModelFileFormat#getReadProperties(JComponent)
	 */
	public Hashtable getReadProperties(JComponent accessory) {
		return null;
	}

	//
	// Read
	//

	public static void read(InputStream in, GPGraph graph) throws Exception {
			GraphModel model = graph.getModel();
		// Create a DocumentBuilderFactory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// Create a DocumentBuilder
		DocumentBuilder db = dbf.newDocumentBuilder();
		// Parse the input file to get a Document object
		Document doc = db.parse(in);
		// Get the first child (the jgx-element)
		Node modelNode = null;
		Node objsNode = null;
		Node attrsNode = null;
		Node viewNode = null;

		for (int i = 0;
			i < doc.getDocumentElement().getChildNodes().getLength();
			i++) {
			Node node = doc.getDocumentElement().getChildNodes().item(i);
			if (node.getNodeName().toLowerCase().equals("model")) {
				modelNode = node;
			} else if (node.getNodeName().toLowerCase().equals("user")) {
				objsNode = node;
			} else if (node.getNodeName().toLowerCase().equals("attrs")) {
				attrsNode = node;
			} else if (node.getNodeName().toLowerCase().equals("view")) {
				viewNode = node;
			}
		}
		objs = decodeUserObjects(objsNode);
		attrs = parseAttrs(attrsNode);
		attrs = augmentAttrs(attrs);
		Map settings = decodeMap(viewNode, false, false);
		ConnectionSet cs = new ConnectionSet();
		Hashtable cells = new Hashtable();
		DefaultGraphCell[] insert = parseChildren(modelNode, cells, cs);

		// Create ConnectionSet
		Iterator it = connectionSetIDs.iterator();
		while (it.hasNext()) {
			ConnectionID cid = (ConnectionID) it.next();
			Object cell = cid.getCell();
			String tid = cid.getTargetID();
			if (tid != null) {
				Object port = cells.get(tid);
				if (port != null) {
					cs.connect(cell, port, cid.isSource());
				}
			}
		}

		// Create AttributeMap
		Map nested = new Hashtable();
		it = delayedAttributes.iterator();
		while (it.hasNext()) {
			DelayedAttributeID att = (DelayedAttributeID) it.next();
			Map attr = (Map) attrs.get(att.getMapID());
			if (attr != null) {
				attr = GraphConstants.cloneMap(attr);
				if (att.getBounds() != null)
					GraphConstants.setBounds(attr, att.getBounds());
				if (att.getPoints() != null)
					GraphConstants.setPoints(attr, att.getPoints());
				nested.put(att.getCell(), attr);
			}
		}
		
		// Apply settings to graph
		applySettings(settings, graph);

		// Insert the cells (View stores attributes)
		model.insert(insert, nested, cs, null, null);
	}

		public static void applySettings(Map s, GPGraph graph) {
		Object tmp;

		tmp = s.get("editable");
		if (tmp != null)
		graph.setEditable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("bendable");
		if (tmp != null)
		graph.setBendable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("cloneable");
		if (tmp != null)
		graph.setCloneable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("connectable");
		if (tmp != null)
		graph.setConnectable(new Boolean(tmp.toString()).booleanValue());
	    
		tmp = s.get("disconnectable");
		if (tmp != null)
		graph.setDisconnectable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("disconnectOnMove");
		if (tmp != null)
		graph.setDisconnectOnMove(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("doubleBuffered");
		if (tmp != null)
		graph.setDoubleBuffered(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("dragEnabled");
		if (tmp != null)
		graph.setDragEnabled(new Boolean(tmp.toString()).booleanValue());	

		tmp = s.get("dropEnabled");
		if (tmp != null)
		graph.setDropEnabled(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("moveable");
		if (tmp != null)
		graph.setMoveable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("sizeable");
		if (tmp != null)
		graph.setSizeable(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("selectNewCells");
		if (tmp != null)
		graph.setSelectNewCells(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("gridVisible");
		if (tmp != null)
		graph.setGridVisible(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("gridEnabled");
		if (tmp != null)
		graph.setGridEnabled(new Boolean(tmp.toString()).booleanValue());

		tmp = s.get("gridSize");
		if (tmp != null)
		graph.setGridSize(Integer.parseInt(tmp.toString()));

		tmp = s.get("gridMode");
		if (tmp != null)
		graph.setGridMode(Integer.parseInt(tmp.toString()));

		tmp = s.get("scale");
		if (tmp != null)
		graph.setScale(Double.parseDouble(tmp.toString()));

		tmp = s.get("antiAlias");
		if (tmp != null)
		graph.setAntiAliased(new Boolean(tmp.toString()).booleanValue());

	}

	public static Map augmentAttrs(Map attrs) {
		Map newAttrs = new Hashtable();
		Iterator it = attrs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Object key = entry.getKey();
			Map map = (Map) entry.getValue();
			Stack s = new Stack();
			s.add(map);
			Object parentID = map.get(PARENT);
			Object hook = null;
			while (parentID != null) {
				hook = attrs.get(parentID);
				s.add(hook);
				parentID = ((Map) hook).get(PARENT);
			}
			Map newMap = new Hashtable();
			while (!s.isEmpty()) {
				newMap.putAll((Map) s.pop());
			}
			newMap.remove(PARENT);
			// Remove Empty values
			Iterator it2 = newMap.entrySet().iterator();
			while (it2.hasNext()) {
				entry = (Map.Entry) it2.next();
				if (entry.getValue() == EMPTY)
					it2.remove();
			}
			newAttrs.put(key, newMap);
		}
		return newAttrs;
	}

	public static DefaultGraphCell parseCell(
		Node node,
		Hashtable cells,
		ConnectionSet cs) {
		DefaultGraphCell cell = null;
		if (node.getNodeName().toLowerCase().equals("a")) {
			Node key = node.getAttributes().getNamedItem("id");
			Node type = node.getAttributes().getNamedItem("class");
			if (key != null && type != null) {
				Node value = node.getAttributes().getNamedItem("val");
				Object userObject = "";
				if (value != null)
					userObject = objs.get(value.getNodeValue());
				cell = createCell(type.getNodeValue(), userObject);

				if (cell != null) {
					cells.put(key.getNodeValue(), cell);

					DefaultGraphCell[] children =
						parseChildren(node, cells, cs);
					for (int i = 0; i < children.length; i++)
						cell.add(children[i]);

					Node source = node.getAttributes().getNamedItem("src");
					Node target = node.getAttributes().getNamedItem("tgt");
					if (source != null) {
						ConnectionID cid =
							new ConnectionID(cell, source.getNodeValue(), true);
						connectionSetIDs.add(cid);
					}
					if (target != null) {
						ConnectionID cid =
							new ConnectionID(
								cell,
								target.getNodeValue(),
								false);
						connectionSetIDs.add(cid);
					}

					Node boundsNode = node.getAttributes().getNamedItem("rect");
					Rectangle bounds = null;
					if (boundsNode != null) {
						Object rectangle = decodeValue(Rectangle.class, boundsNode.getNodeValue());
						if (rectangle instanceof Rectangle)
						bounds = (Rectangle) rectangle;
					}
					
					Node pointsNode = node.getAttributes().getNamedItem("pts");
					List points = null;
					if (pointsNode != null) {
						Object pointList = decodeValue(List.class, pointsNode.getNodeValue());
						if (pointList instanceof List)
						points = (List) pointList;
					}

					Node attr = node.getAttributes().getNamedItem("attr");
					String mapID = null;
					if (attr != null)
						mapID = attr.getNodeValue();

					if (mapID != null)
						delayedAttributes.add(
							new DelayedAttributeID(
								cell,
								bounds,
								points,
								mapID));
				}
			}
		}
		return cell;
	}

	public static DefaultGraphCell[] parseChildren(
		Node node,
		Hashtable cells,
		ConnectionSet cs) {
		List list = new LinkedList();
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node child = node.getChildNodes().item(i);
			DefaultGraphCell cell = parseCell(child, cells, cs);
			if (cell != null)
				list.add(cell);
		}
		DefaultGraphCell[] dgc = new DefaultGraphCell[list.size()];
		list.toArray(dgc);
		return dgc;
	}

	public static Map parseAttrs(Node node) {
		Hashtable map = new Hashtable();
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node child = node.getChildNodes().item(i);
			if (child.getNodeName().toLowerCase().equals("map")) {
				Node key = child.getAttributes().getNamedItem("id");
				Node pid = child.getAttributes().getNamedItem("pid");
				Map attrs = decodeMap(child, true, false);
				if (key != null && attrs.size() > 0) {
					if (pid != null)
						attrs.put(
							PARENT,
							pid.getNodeValue());
					map.put(key.getNodeValue(), attrs);
				}
			}
		}
		return map;
	}

	/**
	 * Returns an attributeMap for the specified position and color.
	 */
	public static Map createDefaultAttributes() {
		// Create an AttributeMap
		Map map = GraphConstants.createMap();
		// Set a Black Line Border (the Border-Attribute must be Null!)
		GraphConstants.setBorderColor(map, Color.black);
		// Return the Map
		return map;
	}

	public static class DelayedAttributeID {

		protected Object cell;

		protected Rectangle bounds;

		protected List points;

		protected String mapID;

		public DelayedAttributeID(
			Object cell,
			Rectangle bounds,
			List points,
			String mapID) {
			this.cell = cell;
			this.bounds = bounds;
			this.points = points;
			this.mapID = mapID;
		}

		/**
		 * @return
		 */
		public Object getCell() {
			return cell;
		}

		/**
		 * @return
		 */
		public Rectangle getBounds() {
			return bounds;
		}

		/**
		 * @return
		 */
		public String getMapID() {
			return mapID;
		}

		/**
		 * @return
		 */
		public List getPoints() {
			return points;
		}

		/**
		 * @param rectangle
		 */
		public void setBounds(Rectangle rectangle) {
			bounds = rectangle;
		}

		/**
		 * @param object
		 */
		public void setCell(Object object) {
			cell = object;
		}

		/**
		 * @param string
		 */
		public void setMapID(String string) {
			mapID = string;
		}

		/**
		 * @param list
		 */
		public void setPoints(List list) {
			points = list;
		}

	}

	public static class ConnectionID {

		protected Object cell;

		protected String targetID;

		protected boolean source;

		public ConnectionID(Object cell, String targetID, boolean source) {
			this.cell = cell;
			this.targetID = targetID;
			this.source = source;
		}

		/**
		 * @return
		 */
		public Object getCell() {
			return cell;
		}

		/**
		 * @return
		 */
		public boolean isSource() {
			return source;
		}

		/**
		 * @return
		 */
		public String getTargetID() {
			return targetID;
		}

		/**
		 * @param object
		 */
		public void setCell(Object object) {
			cell = object;
		}

		/**
		 * @param b
		 */
		public void setSource(boolean b) {
			source = b;
		}

		/**
		 * @param string
		 */
		public void setTargetID(String string) {
			targetID = string;
		}

	}


	//
	// Write
	//


	public String toString(GPGraph graph) {
		userObjectMap.clear();
		cellMap.clear();
		attrs.clear();

		String xml = "<str-1.0>\n";

		GraphModel model = graph.getModel();
		xml += "<model>\n";
		xml += outputModel(model, "\t", null);
		xml += "</model>\n";

		xml += "<attrs>\n";
		xml += outputAttributes("\t");
		xml += "</attrs>\n";

		xml += "<user>\n";
		xml += encodeUserObjects("\t", userObjectMap);
		xml += "</user>\n";

		xml += "<view>\n";
		xml += outputView(graph, "\t");
		xml += "</view>\n";

		// Close main tags
		xml += "</str-1.0>\n";
		return xml;
	}

		public String outputView(GPGraph graph, String indent) {
		String xml = indent + "<a key=\"editable\" val=\""+graph.isEditable()+"\"/>\n"
			+ indent + "<a key=\"bendable\" val=\""+graph.isBendable()+"\"/>\n"
			+ indent + "<a key=\"cloneable\" val=\""+graph.isCloneable()+"\"/>\n"
			+ indent + "<a key=\"connectable\" val=\""+graph.isConnectable()+"\"/>\n"
			+ indent + "<a key=\"disconnectable\" val=\""+graph.isDisconnectable()+"\"/>\n"
			+ indent + "<a key=\"disconnectOnMove\" val=\""+graph.isDisconnectOnMove()+"\"/>\n"
			+ indent + "<a key=\"doubleBuffered\" val=\""+graph.isDoubleBuffered()+"\"/>\n"
			+ indent + "<a key=\"dragEnabled\" val=\""+graph.isDragEnabled()+"\"/>\n"
			+ indent + "<a key=\"dropEnabled\" val=\""+graph.isDropEnabled()+"\"/>\n"
			+ indent + "<a key=\"moveable\" val=\""+graph.isMoveable()+"\"/>\n"
			+ indent + "<a key=\"sizeable\" val=\""+graph.isSizeable()+"\"/>\n"
			+ indent + "<a key=\"selectNewCells\" val=\""+graph.isSelectNewCells()+"\"/>\n"
			+ indent + "<a key=\"gridVisible\" val=\""+graph.isGridVisible()+"\"/>\n"
			+ indent + "<a key=\"gridEnabled\" val=\""+graph.isGridEnabled()+"\"/>\n"
			+ indent + "<a key=\"gridSize\" val=\""+graph.getGridSize()+"\"/>\n"
			+ indent + "<a key=\"gridMode\" val=\""+graph.getGridMode()+"\"/>\n"
			+ indent + "<a key=\"scale\" val=\""+graph.getScale()+"\"/>\n"
			+ indent + "<a key=\"antiAlias\" val=\""+graph.isAntiAliased()+"\"/>\n";
		return xml;
		}

	public String outputModel(GraphModel model, String indent, Object parent) {
		String xml = new String("");
		int max =
			(parent != null)
				? model.getChildCount(parent)
				: model.getRootCount();
		for (int i = 0; i < max; i++) {
			Object cell =
				(parent != null)
					? model.getChild(parent, i)
					: model.getRootAt(i);
			if (cell != null)
				xml += outputCell(indent, model, cell);
		}
		return xml;
	}

	public String outputCell(String indent, GraphModel model, Object cell) {
		Map map = new Hashtable(model.getAttributes(cell));
		Rectangle r = (Rectangle) map.remove(GraphConstants.BOUNDS);
		Object value = map.remove(GraphConstants.VALUE);
		if (GraphConstants.getFont(map).equals(GraphConstants.defaultFont))
			map.remove(GraphConstants.FONT);
		Object source = model.getSource(cell);
		Object target = model.getTarget(cell);
		if (GraphConstants.getRouting(map) != null)
			map.remove(GraphConstants.POINTS);
		String sourceID = "";
		String targetID = "";
		if (source != null)
			sourceID = " src=\"" + getID(source) + "\"";
		if (source != null)
			targetID = " tgt=\"" + getID(target) + "\"";
		String bounds = "";
		String valueS = "";
		if (r != null && !model.isEdge(cell) && !model.isPort(cell) && !r.equals(DefaultGraphCell.defaultBounds))
			bounds = " rect=\"" + encodeValue(r) + "\"";
		List p = GraphConstants.getPoints(map);
		map.remove(GraphConstants.POINTS);
		String points = "";
		if (p != null && !p.equals(DefaultEdge.defaultPoints)) {
			String tmp = encodeValue(p);
			if (tmp.length() > 0)
				points = " pts=\"" + tmp + "\"";
		}
		if (value != null)
			valueS = " val=\"" + getUserObjectID(value) + "\"";
		String attrID = "";
		if (map.size() > 0)
			attrID = " attr=\"" + attrCol.addMap(map) + "\"";
		String xml =
			new String(
				indent
					+ "<a class=\""
					+ getType(cell)
					+ "\" id=\""
					+ getID(cell)
					+ "\""
					+ valueS
					+ sourceID
					+ targetID
					+ bounds
					+ points
					+ attrID);
		if (model.getChildCount(cell) > 0)
			xml += ">\n"
				+ outputModel(model, indent + "\t", cell)
				+ indent
				+ "</a>\n";
		else
			xml += "/>\n";
		return xml;
	}

	public int getUserObjectID(Object object) {
		Integer index = (Integer) userObjectMap.get(object);
		if (index != null)
			return index.intValue();
		index = new Integer(userObjectMap.size() + 1);
		userObjectMap.put(object, index);
		return index.intValue();
	}

	public int getID(Object object) {
		Integer index = (Integer) cellMap.get(object);
		if (index != null)
			return index.intValue();
		index = new Integer(cellMap.size() + 1);
		cellMap.put(object, index);
		return index.intValue();
	}

	public String outputAttributes(String indent) {
		Set set = new HashSet();
		set.add(PARENT);
		set.add(GraphConstants.BOUNDS);
		set.add(GraphConstants.POINTS);

		String xml = new String();
		for (int i = 0; i < attrCol.maps.size(); i++) {
			Map map = (Map) attrCol.maps.get(i);
			Object hook = map.get(PARENT);
			String hookS = "";
			if (hook != null)
				hookS = " pid=\"" + attrCol.maps.indexOf(hook) + "\"";
			xml += indent + "<map id=\"" + i + "\"" + hookS + ">\n";
			xml += encodeMap(indent + "\t", map, false, set, false);
			xml += indent + "</map>\n";
		}
		return xml;
	}

	public class AttributeCollection {

		public List maps = new LinkedList();

		public int addMap(Map attr) {
			Iterator it = maps.iterator();
			Map storeMap = new Hashtable(attr);
			Map hook = storeMap;
			while (it.hasNext()) {
				Map ref = (Map) it.next();
				Map diff = diffMap(ref, attr);
				if (diff.size() < storeMap.size()) {
					hook = ref;
					storeMap = diff;
				}
			}
			if (storeMap.size() == 0 && hook != storeMap)
				return maps.indexOf(hook);
			if (hook != storeMap)
				storeMap.put(PARENT, hook);
			maps.add(storeMap);
			return maps.indexOf(storeMap);
		}

		public void clear() {
			maps.clear();
		}

		/**
		 * Returns a new map that contains all (key, value)-pairs
		 * of <code>newState</code> where either key is not used
		 * or value is different for key in <code>oldState</code>.
		 * In other words, this method removes the common entries
		 * from oldState and newState, and returns the "difference"
		 * between the two.
		 * 
		 * This method never returns null.
		 */
		public Map diffMap(Map oldState, Map newState) {
			// Augment oldState
			Stack s = new Stack();
			s.add(oldState);
			Object hook = oldState.get(PARENT);
			while (hook instanceof Map) {
				s.add(hook);
				hook = ((Map) hook).get(PARENT);
			}
			oldState = new Hashtable();
			while (!s.isEmpty()) {
				oldState.putAll((Map) s.pop());
			}
			Map diff = new Hashtable();
			Iterator it = newState.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				Object key = entry.getKey();
				Object oldValue = oldState.remove(key);
				if (key != PARENT) {
					Object newValue = entry.getValue();
					if (oldValue == null || !oldValue.equals(newValue))
						diff.put(key, newValue);
				}
			}
			it = oldState.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				if (!oldState.get(key).equals(""))
					diff.put(key, "");
			}
			diff.remove(PARENT);
			return diff;
		}

	}


	//
	// Codec
	//

	public static String[] knownKeys =
		new String[] {
			GraphConstants.ABSOLUTE,
			GraphConstants.AUTOSIZE,
			GraphConstants.BACKGROUND,
			GraphConstants.BEGINFILL,
			GraphConstants.BEGINSIZE,
			GraphConstants.BENDABLE,
			GraphConstants.BORDER,
			GraphConstants.BORDERCOLOR,
			GraphConstants.BOUNDS,
			GraphConstants.CONNECTABLE,
			GraphConstants.DASHPATTERN,
			GraphConstants.DISCONNECTABLE,
			GraphConstants.EDITABLE,
			GraphConstants.ENDFILL,
			GraphConstants.ENDSIZE,
			GraphConstants.FONT,
			GraphConstants.FOREGROUND,
			GraphConstants.HORIZONTAL_ALIGNMENT,
			GraphConstants.VERTICAL_ALIGNMENT,
			GraphConstants.ICON,
			GraphConstants.LABELPOSITION,
			GraphConstants.LINEBEGIN,
			GraphConstants.LINECOLOR,
			GraphConstants.LINEEND,
			GraphConstants.LINESTYLE,
			GraphConstants.LINEWIDTH,
			GraphConstants.MOVEABLE,
			GraphConstants.OFFSET,
			GraphConstants.OPAQUE,
			GraphConstants.POINTS,
			GraphConstants.ROUTING,
			GraphConstants.SIZE,
			GraphConstants.SIZEABLE,
			GraphConstants.VALUE };

	public static Class[] keyTypes =
		new Class[] {
			Boolean.class,
			Boolean.class,
			Color.class,
			Boolean.class,
			Integer.class,
			Boolean.class,
			Border.class,
			Color.class,
			Rectangle.class,
			Boolean.class,
			float[].class,
			Boolean.class,
			Boolean.class,
			Boolean.class,
			Integer.class,
			Font.class,
			Color.class,
			Integer.class,
			Integer.class,
			Icon.class,
			Point.class,
			Integer.class,
			Color.class,
			Integer.class,
			Integer.class,
			Float.class,
			Boolean.class,
			Point.class,
			Boolean.class,
			List.class,
			Edge.Routing.class,
			Dimension.class,
			Boolean.class,
			Object.class };

	public static String encodeMap(
		String indent,
		Map attributes,
		boolean invert,
		Set excludeAttributes,
		boolean URLencodeValues) {
		String xml = new String("");
		Iterator it = attributes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Object key = entry.getKey();
			if (excludeAttributes == null
				|| !excludeAttributes.contains(key)) {
				Object value = entry.getValue();
				if (invert) {
					Object tmp = key;
					key = value;
					value = tmp;
				}
				if (URLencodeValues) {
					try {
					key = URLEncoder.encode(key.toString(), "UTF-8");
					value = URLEncoder.encode(encodeValue(value), "UTF-8");
					} catch (Exception e) {
					System.err.println(e.getMessage());
					}
				}
				xml += indent
					+ "<a key=\""
					+ encodeKey(key.toString())
					+ "\" val=\""
					+ encodeValue(value)
					+ "\"/>\n";
			}
		}
		return xml;
	}

	public static String encodeUserObjects(
		String indent,
		Map userObjects) {
		String xml = new String("");
		Iterator it = userObjects.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Object key = entry.getValue();
			Object value = entry.getKey();
			if (value instanceof GPUserObject) {
				xml += indent
				+ "<a key=\""
				+ encodeKey(key.toString())
				+ "\"";
				Map map = ((GPUserObject) value).getProperties();
				xml += ">\n" + encodeMap(indent+"\t", map, false, null, true)
					+ indent + "</a>\n";
			} else {
				try {
				value = URLEncoder.encode(encodeValue(value), "UTF-8");
				} catch (Exception e) {
				System.err.println(e.getMessage());
				}
				xml += indent
				+ "<a key=\""
				+ encodeKey(key.toString())
				+ "\" val=\""
				+ value
				+ "\"/>\n";
			}
		}
		return xml;
	}

	public static String encodeKey(String key) {
//		for (int i = 0; i < knownKeys.length; i++)
//			if (key.equals(knownKeys[i]))
//				return Integer.toString(i);
		return key;
	}

	public static String encodeValue(Object value) {
		String ret = "";
		if (value instanceof Rectangle) {
			Rectangle r = (Rectangle) value;
			ret =  r.x + "," + r.y + "," + r.width + "," + r.height;
		} else if (value instanceof List) { // TODO: non-points
			List list = (List) value;
			String s = "";
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i) instanceof Point) {
					Point pt = (Point) list.get(i);
					s = s + pt.x + "," + pt.y + ",";
				}
			}
			ret =  (s.length() > 0) ? s.substring(0, s.length() - 1) : s;
		} else if (value instanceof Font) {
			Font font = (Font) value;
			ret =  font.getName()
				+ ","
				+ font.getSize()
				+ ","
				+ font.getStyle();
		} else if (value instanceof Color) {
			Color color = (Color) value;
			ret =  Integer.toString(color.getRed())
				+ ","
				+ Integer.toString(color.getGreen())
				+ ","
				+ Integer.toString(color.getBlue());
		} else if (value instanceof Point) {
			Point point = (Point) value;
			ret =  point.x + "," + point.y;
		} else if (value instanceof float[]) {
			float[] f = (float[]) value;
			String s = "";
			for (int i = 0; i < f.length; i++) {
				s = s + Float.toString(f[i]) + ",";
			}
			ret =  s.substring(0, s.length() - 1);
		} else if (value instanceof Border) {
			if (value instanceof LineBorder) {
				LineBorder lb = (LineBorder) value;
				ret =  "L,"
					+ lb.getLineColor().getRGB()
					+ ","
					+ lb.getThickness();
			} else if (value instanceof BevelBorder) {
				BevelBorder bb = (BevelBorder) value;
				ret =  "B," + bb.getBevelType();
			}
		} else if (value instanceof ImageIconBean) {
			ImageIconBean icon = (ImageIconBean) value;
			ret =  icon.getFileName();
		} else if (value instanceof Edge.Routing) {
			if (value instanceof DefaultEdge.DefaultRouting)
			ret =  "simple";
		} else if (value != null)
			ret =  value.toString();
		return ret;
	}

	public static Map decodeMap(Node node, boolean useKnownKeys, boolean URLdecodeValues) {
		Hashtable map = new Hashtable();
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node child = node.getChildNodes().item(i);
			if (child.getNodeName().toLowerCase().equals("a")) {
				Node key = child.getAttributes().getNamedItem("key");
				Node value = child.getAttributes().getNamedItem("val");
				if (key != null && value != null) {
					String keyVal = key.getNodeValue().toString();
					Object valueS = value.getNodeValue().toString();
					if (useKnownKeys) {
						int index = -1;
						for (int j=0; j<knownKeys.length; j++)
						if (keyVal.equals(knownKeys[j]))
							index = j;
						if (index != -1)
						valueS =
							decodeValue(keyTypes[index], valueS.toString());
					} else if (URLdecodeValues) {
					
						try {
						keyVal = URLDecoder.decode(keyVal.toString(), "UTF-8");
						valueS = URLDecoder.decode(valueS.toString(), "UTF-8");
						} catch (Exception e) {
						System.err.println(e.getMessage());
						}
					}
					if (valueS != null)
						map.put(keyVal, valueS);
				}
			}
		}
		return map;
	}

	public static Map decodeUserObjects(Node node) {
		Hashtable map = new Hashtable();
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node child = node.getChildNodes().item(i);
			if (child.getNodeName().toLowerCase().equals("a")) {
				Node key = child.getAttributes().getNamedItem("key");
				Node value = child.getAttributes().getNamedItem("val");
				if (key != null) {
					String keyVal = key.getNodeValue().toString();
					if (value != null) {
						Object valueS = value.getNodeValue().toString();
						if (valueS != null) {
						try {
							valueS = URLDecoder.decode(valueS.toString(), "UTF-8");
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
						map.put(keyVal, valueS);
						}
					} else {
						Map properties = decodeMap(child, false, true);
						if (properties != null)
						map.put(keyVal, properties);
					}
				}
			}
		}
		return map;
	}

	public static String[] tokenize(String s, String token) {
		StringTokenizer tokenizer = new StringTokenizer(s, token);
		String[] tok = new String[tokenizer.countTokens()];
		int i = 0;
		while (tokenizer.hasMoreElements()) {
			tok[i++] = tokenizer.nextToken();
		}
		return tok;
	}

	public static Object decodeValue(Class key, String value) {
		if (key != String.class
			&& key != Object.class
			&& (value == null || value.equals("")))
			return EMPTY;
		if (key == Rectangle.class) {
			String[] tok = tokenize(value, ",");
			if (tok.length == 4) {
				int x = Integer.parseInt(tok[0]);
				int y = Integer.parseInt(tok[1]);
				int w = Integer.parseInt(tok[2]);
				int h = Integer.parseInt(tok[3]);
				return new Rectangle(x, y, w, h);
			}
		} else if (key == List.class) { // FIX: Do not assume Points!
			List list = new LinkedList();
			String[] tok = tokenize(value, ",");
			for (int i = 0; i < tok.length; i = i + 2) {
				int x = Integer.parseInt(tok[i]);
				int y = Integer.parseInt(tok[i + 1]);
				list.add(new Point(x, y));
			}
			return list;
		} else if (key == Font.class) {
			String[] tok = tokenize(value, ",");
			if (tok.length == 3) {
				String name = tok[0];
				int size = Integer.parseInt(tok[1]);
				int style = Integer.parseInt(tok[2]);
				return new Font(name, style, size);
			}
		} else if (key == Color.class) {
			String[] tok = tokenize(value, ",");
			if (tok.length == 3) {
				int r = Integer.parseInt(tok[0]);
				int g = Integer.parseInt(tok[1]);
				int b = Integer.parseInt(tok[2]);
				return new Color(r, g, b);
			}
			return new Color(Integer.parseInt(value));
		} else if (key == Point.class) {
			String[] tok = tokenize(value, ",");
			if (tok.length == 2) {
				int x = Integer.parseInt(tok[0]);
				int y = Integer.parseInt(tok[1]);
				return new Point(x, y);
			}
		} else if (key == float[].class) {
			String[] tok = tokenize(value, ",");
			float[] f = new float[tok.length];
			for (int i = 0; i < tok.length; i++)
				f[i] = Float.parseFloat(tok[i]);
			return f;
		} else if (key == Integer.class) {
			return new Integer(value);
		} else if (key == Border.class) {
			String[] tok = tokenize(value, ",");
			if (tok[0].equals("L")) { // LineBorder
				Color c = new Color(Integer.parseInt(tok[1]));
				int thickness = Integer.parseInt(tok[2]);
				return BorderFactory.createLineBorder(c, thickness);
			} else if (tok[0].equals("B")) { // BevelBorder
				int type = Integer.parseInt(tok[1]);
				return BorderFactory.createBevelBorder(type);
			}
			return BorderFactory.createLineBorder(Color.black, 1);
		} else if (key == Boolean.class) {
			return new Boolean(value);
		} else if (key == Float.class) {
			return new Float(value);
		} else if (key == Icon.class) {
			try {
			return new ImageIconBean(new URL(value));
			} catch (Exception e) {
			System.err.println("Invalid URL: "+value);
			return new ImageIconBean(value);
			}
		} else if (key == Edge.Routing.class) {
			if (value.equals("simple"))
			return GraphConstants.ROUTING_SIMPLE;
		}
		return value;
	}

	//
	// Cell Factory
	//

	public static DefaultGraphCell createCell(String type, Object userObject) {
		if (userObject instanceof Map)
		userObject = new GPUserObject((Map) userObject);
		if (type.equals("rect"))
		return new DefaultGraphCell(userObject);
		else if (type.equals("text"))
		return new TextCell(userObject);
		else if (type.equals("ellipse"))
		return new EllipseCell(userObject);
		else if (type.equals("image"))
		return new ImageCell(userObject);
		else if (type.equals("port"))
		return new DefaultPort(userObject);
		else if (type.equals("edge"))
		return new DefaultEdge(userObject);
		return null;
	}

	public static String getType(Object cell) {
		if (cell instanceof DefaultPort)
			return "port";
		else if (cell instanceof TextCell)
			return "text";
		else if (cell instanceof EllipseCell)
			return "ellipse";
		else if (cell instanceof ImageCell)
			return "image";
		else if (cell instanceof DefaultEdge)
			return "edge";
		return "rect";
	}

}
