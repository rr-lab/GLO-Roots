
/**
 * @author guillaumelobet
 * 
 * The segment stores and compute the information related to root segments. 
 * The segment is composed of multiple Nodes (minimum 2)
 */

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;


public class Segment {

	
	// Apexes (previous and current)
	public Reporter attachedRep;

	// Display variables
	public GeneralPath axisGP = new GeneralPath();
	public GeneralPath nodesGP = new GeneralPath();
	public GeneralPath bordersGP = new GeneralPath();
	
	
	// Nodes composing the root
	Node firstNode, lastNode;
	
	// Misc 
	boolean isSelected = false;
	boolean isHover = false;
	
	// Time coordinates
	private int time;
	
	public float environment = -1;

	String segID;
	String segName;
	
	/**
	 * Default constructor
	 */
	public Segment(){
		new Segment(0, 0, 0, 0, 0, 0);
	}
	
	/**
	 * Constructor sued when reading and RSML file
	 * @param node
	 * @param rs
	 */
	public Segment(org.w3c.dom.Node node, RootSystem rs){
		readRSML(node, rs);
	}
	
	/**
	 * 
	 */
	public Segment(int ti, int i){
		time = ti;
		segID = UUID.randomUUID().toString();
		segName = "seg_"+i;
	}
	/**
	 * Segment contructor
	 * @param xS
	 * @param xE
	 * @param yS
	 * @param yE
	 * @param ti
	 */
	public Segment(int xS, int yS, int xE, int yE, int ti, int i){
		
		if(yS < yE){
			firstNode = new Node(xS, yS, ti);
			lastNode = new Node(xE, yE, ti);
		}
		else{
			lastNode = new Node(xS, yS, ti);
			firstNode = new Node(xE, yE, ti);
		}
		firstNode.setChild(lastNode);
		lastNode.setParent(firstNode);
		firstNode.buildNode();
		lastNode.buildNode();
		time = ti;
		segID = UUID.randomUUID().toString();
		segName = "seg_"+i;
	}
	
	
	/**
	 * Read the root daa from the RSML file
	 * @param parentDOM
	 */
	public void readRSML(org.w3c.dom.Node parentDOM, RootSystem rs) {
		 
		   
		org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("label");
		if (nn != null) segName = nn.getNodeValue();
		 
	  nn = parentDOM.getAttributes().getNamedItem("ID");
	  if (nn != null){
		  segID = nn.getNodeValue();
	  }
	 

      org.w3c.dom.Node nodeDOM = parentDOM.getFirstChild();

	  nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
         String nName = nodeDOM.getNodeName();
         // Nodes that are neither name, rulerAtOrigin nor Node elemnts are not considered
         // Read the geometry
         if (nName.equals("geometry")) {
			   org.w3c.dom.Node nodeGeom = nodeDOM.getFirstChild();
			   while (nodeGeom != null) {
				   	String geomName = nodeGeom.getNodeName();
				   if (geomName.equals("polyline")) {
					   org.w3c.dom.Node nodePoint = nodeGeom.getFirstChild();
					   while (nodePoint != null) {
						   	String pointName = nodePoint.getNodeName();
						   if (pointName.equals("point")) {
							   		Node no = addNode(0, 0);
							   		no.readRSML(nodePoint);
							   		no.buildNode();
						   }
						   nodePoint = nodePoint.getNextSibling();
					   }
				   }
				   nodeGeom = nodeGeom.getNextSibling();
			   }
         }
         nodeDOM = nodeDOM.getNextSibling();
      } 
      
      // Add the annotations
      nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
          String nName = nodeDOM.getNodeName();
          if(nName.equals("annotations")){
			   org.w3c.dom.Node nodeAnnotations = nodeDOM.getFirstChild();
			   while(nodeAnnotations != null){
			      String fName = nodeAnnotations.getNodeName();
		          if(fName.equals("annotation")){
		              Reporter rep = new Reporter(nodeAnnotations, rs);
		              if (rep != null){
		            	  attachReporter(rep);
		              }
		          }
		          nodeAnnotations = nodeAnnotations.getNextSibling();
			   }
          }
          nodeDOM = nodeDOM.getNextSibling(); 
      }      
   }
	   
	  	
	
   /**
    * Save the data to a RSML file
    * @param dataOut
    * @throws IOException
    */
   public void saveRSML(FileWriter dataOut) throws IOException {
      Node n = firstNode;
      if (n == null) return;
      
      
      String nL = System.getProperty("line.separator");
      dataOut.write("			<root ID='" + segID + "' label='" + segName + "' po:accession='PO:0009005'>" + nL);

      // Root properties	      
      dataOut.write("				<properties>" + nL);      
      dataOut.write("					<direction>"+(float) (getDirection() * (180 / Math.PI))+"</direction>" + nL);           
      dataOut.write("				</properties>" + nL);	      
      
      // Root geometry
      dataOut.write("				<geometry>" + nL);      
      dataOut.write("					<polyline>" + nL);      
      while (n != null) {
         n.saveCoordinatesToRSML(dataOut);
         n = n.child;
         }
      dataOut.write("					</polyline>" + nL);      
      dataOut.write("				</geometry>" + nL); 
      
      // Root functions

      if(this.attachedRep != null){
	      dataOut.write("				<annotations>" + nL);      
	      attachedRep.saveRSML(dataOut);
	      dataOut.write("				</annotations>" + nL);      
      }

      
      dataOut.write("			</root>" + nL);
   }
	   
	   	
	
   /**
    * Does the segment contain the x/y position?
    * @param x
    * @param y
    * @return
    */
   public boolean contains(float x, float y) {
	  //createGraphics();
      return bordersGP.contains(x, y);
      }	
	
   /**
    * Paint the segment on the RCImage canvas
    * @param g2D
    */
	public void paint(Graphics2D g2D){
		createGraphics();
		
		Stroke bs1 = new BasicStroke(5.0f);
		Stroke bs2 = new BasicStroke(4.0f);

		g2D.setStroke(bs1);
		g2D.setColor(Util.PASSIVE);
		if(attachedRep != null) g2D.setColor(Util.ATTACHED);	
		if(isSelected) g2D.setColor(Util.SELECTED);	
		if(isHover) g2D.setColor(Util.ATTACHED);	
		
		g2D.setComposite(Util.AC1);
		g2D.draw(axisGP);
		
		g2D.setStroke(bs2);
		if(isSelected) g2D.draw(nodesGP);

	}
	
	
	
   /**
    * Create the segment graphics. For display purpose
    */
   public void createGraphics() {
      Arc2D arc = new Arc2D.Float(Arc2D.OPEN);
      
      // Axis
	  axisGP.reset();
	  Node n = firstNode;
	  if (n != null) {
	     axisGP.moveTo(n.x, n.y);
	     while ((n = n.child) != null) axisGP.lineTo(n.x, n.y);
	  }
  
	  // Nodes
	  nodesGP.reset();
	  n = firstNode;
	  while (n != null) {
	     float r = n.diameter / 2.0f;
	     arc.setArcByCenter(n.x, n.y, r, 0.0f, 360.0f, Arc2D.OPEN);
	     nodesGP.append(arc, false);
	     nodesGP.closePath();
	     n.calcPoles();
	     if(n == firstNode){
	    	 nodesGP.moveTo(n.bx[4], n.by[4]);
	    	 nodesGP.lineTo(n.bx[5], n.by[5]);
	     }
	     
	     n = n.child;
	  }
  
	  // Draw the segment borders
      bordersGP.reset();
      n = firstNode;
      if (n != null) {
         bordersGP.moveTo(n.bx[1], n.by[1]);
   
         while (n.child != null) {
            n = n.child;
            if (n.bCross01) bordersGP.lineTo(n.bx[4], n.by[4]);
            else {
               if (n.child == null) bordersGP.lineTo(n.bx[0], n.by[0]);
               else {
                  arc.setFrame(n.x - n.diameter / 2.0, n.y - n.diameter / 2.0, n.diameter, n.diameter);
                  arc.setAngles(n.bx[0], n.by[0], n.bx[1], n.by[1]);
                  bordersGP.append(arc, true);
               }
            }
         }
         
         arc.setFrame(n.x - n.diameter / 2.0, n.y - n.diameter / 2.0, n.diameter, n.diameter);
         arc.setAngles(n.bx[0], n.by[0], n.bx[2], n.by[2]);
         bordersGP.append(arc, true);
         
         while (n.parent != null) {
            n = n.parent;
            if (n.bCross23) bordersGP.lineTo(n.bx[5], n.by[5]);
            else {
               if (n.parent == null) bordersGP.lineTo(n.bx[3], n.by[3]);
               else {
                  arc.setFrame(n.x - n.diameter / 2.0, n.y - n.diameter / 2.0, n.diameter, n.diameter);
                  arc.setAngles(n.bx[3], n.by[3], n.bx[2], n.by[2]);
                  bordersGP.append(arc, true);
                  }
               }
            }
         
         arc.setFrame(n.x - n.diameter / 2.0, n.y - n.diameter / 2.0, n.diameter, n.diameter);
         arc.setAngles(n.bx[5], n.by[5], n.bx[1], n.by[1]);
         bordersGP.append(arc, true);
         bordersGP.closePath();
         }  
      }

	
	/**
	 * Add a node, right before the last existing node
	 * @param x
	 * @param y
	 */
	   public Node addNode(int x, int y){
			 if(firstNode == null){
				 Node n = new Node((int) x,(int) y, time);
				 firstNode = n;
				 return firstNode;
			 }
			 else if(lastNode == null){
				 Node n = new Node((int) x,(int) y, time, firstNode);
				 lastNode = n;
				 return lastNode;
			 }
			 else{
				 lastNode = new Node((int) x,(int) y, time, lastNode);
				 return lastNode;
			 }
	   }
	   
	 /**
	  * Insert a node in the segment, at the position x / y.
	  * @param x
	  * @param y
	  */
	 public void insertNode(float x, float y){
		 Node n = new Node((int) x,(int) y, time);
		 
		 firstNode.child.parent = n;
		 n.child = firstNode.child;
		 firstNode.child = n;
		 n.parent = firstNode;	
		 
		 n.buildNode();
	 }
	   
	 /**
	  * Returns the length of the segment
	  * @return
	  */
	 public float getLength(){
		 float length = 0;
		 Node n = firstNode;
		 while(n.child != null){
			 length += Util.norm(n.child.x - n.x, n.child.y - n.y);
			 n  = n.child;
		 }
		 return length;
	 }
	 
	 /**
	  * Returns the number of nodes in the segment
	  * @return
	  */
	 public int getNNodes(){
		 int i = 0;
		 Node n = firstNode;
		 while(n != null){
			 i++;
			 n  = n.child;
		 }
		 return i;
	 }
	 
	
	
	 /**
	  * Set the selected state of the segment
	  * @param sel
	  */
	 public void setSelected(boolean sel){
		 isSelected = sel;
	 }
	 
	 /**
	  * Is the segment selected
	  * @return
	  */
	 public boolean isSelected(){
		 return isSelected;
	 }

	 
	 /**
	  * 
	  * @param pa
	  */
	 public void attachReporter(Reporter rep){
		 attachedRep = rep;
		 rep.attachSegment(this);
	 }
	 
	 /**
	  * Detach reporter
	  */
	 public void detachReporter(){
		 attachedRep.attachedSegment = null;
		 attachedRep = null;
	 }
	 
	 
	/**
	 * Get the time in serie of the segment
	 * @return
	 */
	public int getTime(){
		return time;
	}
	
	/**
	 * Set the hover state of the root segment
	 * @param flag
	 */
	public void setHover(boolean flag){
		isHover = flag;
	}

	/**
	 * Returns the direction of the root segment
	 * @return
	 */
	public float getDirection() {
		return Util.vectToTheta(lastNode.x - firstNode.x, lastNode.y - firstNode.y);
	}
	
	
}
