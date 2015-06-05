/**
 * @author guillaumelobet - Universite de Liege
 * The Node class is used to define the segments polyline coordinates.
 * Each node has a fixed diameter, but can be moved in the image to modify 
 * the polyline shape.
 * 
 */

import java.awt.AlphaComposite;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;


public class Node {
	
	// Node properties
	public int x, y;
	public float length, theta;
	private int time;
	public float diameter = 10;
	
	// Misc 
	boolean isSelected = false;
	Node parent, child;
	
	// Display
	public GeneralPath apexGP = new GeneralPath();
	static AlphaComposite ac1 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f);	   
	float[] bx = new float[6]; // border
	float[] by = new float[6]; // border
	boolean bCross01 = false, bCross23 = false;
	
	/**
	 * Default constructor
	 */
	public Node(){
		new Node(1, 1, 0);
	}
	
	/**
	 * Constructor
	 * @param x
	 * @param y
	 * @param intens
	 */
	public Node(int x, int y, int time){
		this.x = x;
		this.y = y;
		this.time = time;
		parent = null;
		child = null;
	}
	
	
	/**
	 * Constructor
	 * @param x
	 * @param y
	 * @param intens
	 */
	public Node(int x, int y, int time, Node p){
		this.x = x;
		this.y = y;
		this.time = time;
		parent = p;
		child = null;
		parent.child = this;
	}	
		
	
	
	/**
    * Build the node
    */
   public void buildNode() {
      if (diameter < 1.0f) diameter = 1.0f;

      // calculate length and theta where required
      if (parent != null) {
         float dx = x - parent.x;
         float dy = y - parent.y;
         parent.theta = Util.vectToTheta(dx, dy);
         parent.length = Util.norm(dx, dy);
         }
      if (child != null) {
         float dx = child.x - x;
         float dy = child.y - y;
         theta = Util.vectToTheta(dx, dy);
         length = Util.norm(dx, dy);
         }

      // calculate poles and borders
      if (parent != null){ 
    	  parent.calcBorders();  
    	  parent.calcPoles();
      }
      if (child != null){
    	  calcBorders();
    	  calcPoles();
      }
   }	
	
   /**
    * Determine a radius of a node oriented on the bissecting angle relative to parent and children
   	* This is only for display purpose and as such is called by Root.createGraphics() during paint()
    * It can be assumed that calcBorders() has already been called
    * see tech. note
    * @param dist
    */
   public void calcPoles(){  
	   calcPoles(bx, by, bCross01, bCross23, 1.0f);
      // Trick to detect borders intersections (see Tech Notes)
      if (parent != null && child != null) {
         bCross01 = (bx[1] * (y - by[0]) + by[1] * (bx[0] - x) + x * by[0] - y * bx[0]) >= 0.0f ? true : false;
         bCross23 = (bx[3] * (y - by[2]) + by[3] * (bx[2] - x) + x * by[2] - y * bx[2]) <= 0.0f ? true : false;
      }
      else {
         bCross01 = false;
         bCross23 = false;
         }
   }  
   
   /**
    * Read the node information from and RSML file
    * @param parentDOM the xml elemnt containg the x/y coordinates
    */
   public void readRSML(org.w3c.dom.Node parentDOM) {
	   
		  org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("x");
		  if (nn != null) x = Integer.valueOf(nn.getNodeValue()).intValue();
		  nn = parentDOM.getAttributes().getNamedItem("y");
		  if (nn != null) y = Integer.valueOf(nn.getNodeValue()).intValue();
	}
   
   
   /**
    * Determine a radius of a node oriented on the bissecting angle relative to parent and children
   	* This is only for display purpose and as such is called by Root.createGraphics() during paint()
    * It can be assumed that calcBorders() has already been called
    * see tech. note
    * @param bx
    * @param by
    * @param bCross01
    * @param bCross23
    * @param dist
    */
   public void calcPoles(float[] bx, float[] by, boolean bCross01, boolean bCross23, float dist) {
      float dx, dy, dxc = 0.0f, dyc = 0.0f, dxp = 0.0f, dyp = 0.0f, norm;
      if (parent == null && child == null) return;
      if (parent != null) {
         dxp = x - parent.x;
         dyp = y - parent.y;
         norm = (float) Math.sqrt(dxp * dxp + dyp * dyp);
         dxp /= norm;
         dyp /= norm;
         }
      if (child != null) {
         dxc = child.x - x;
         dyc = child.y - y;
         norm = (float) Math.sqrt(dxc * dxc + dyc * dyc);
         dxc /= norm;
         dyc /= norm;
         }
      if (parent == null) {
         dxp = dxc;
         dyp = dyc;
         }
      if (child == null) {
         dxc = dxp;
         dyc = dyp;
         }
      dx = dyp + dyc;
      dy = - (dxp + dxc);
      norm = (float) Math.sqrt(dx * dx + dy * dy);
      dx *= (diameter / (dist * norm));
      dy *= (diameter / (dist * norm));
      bx[4] = x - dx;
      by[4] = y - dy;
      bx[5] = x + dx;
      by[5] = y + dy;
      
   }   
   
   /**
    * Evaluate the position of border anchors between nodes this and this.child
    * @param dist
    * @param bx
    * @param by
    * @param cbx
    * @param cby
    */
   public void calcBorders() {
      
	   float[] cbx = child.bx;
	   float[] cby = child.by;
	   if (length == 0.0f) ; // Don't do the job
      float nr, cr, ncx, ncy, nax, nay, acnSin, acnCos;
      nr = diameter ;
      cr = child.diameter ;

      ncx = (child.x - x) / length; 
      ncy = (child.y - y) / length;
      acnSin = (cr - nr) / length;
      acnCos = (float) Math.sqrt(1 - acnSin * acnSin);

      nax = (-ncy) * acnCos - ncx * acnSin;
      nay = (-ncy) * acnSin + ncx * acnCos;
      bx[1] = x + nax * nr;
      by[1] = y + nay * nr;
      cbx[0] = child.x + nax * cr;
      cby[0] = child.y + nay * cr;

      nax = ncy * acnCos - (-ncx) * (-acnSin);
      nay = ncy * (-acnSin) + (-ncx) * acnCos;
      bx[3] = x + nax * nr;
      by[3] = y + nay * nr;
      cbx[2] = child.x + nax * cr;
      cby[2] = child.y + nay * cr;
      
      calcPoles();
      
   }   
   
	
	// Misc functions
	/**
	 * Move an apex to a x/y position
	 * @param x
	 * @param y
	 */
	public void move(int x, int y) {  
		this.x = x;
		this.y = y;
		
		
		// re-calculate poles and borders
		if (parent != null){ 
			parent.calcBorders();  
			parent.calcPoles();
		}
		if (child != null){
			calcBorders();
			calcPoles();
		}		
	}

	/**
	 * Does the node contains the x/y coordinates
	 * @param x
	 * @param y
	 * @return true if contains
	 */
	public boolean contains(float x, float y) {
		return (Point2D.distance(this.x, this.y, x, y) < (diameter / 2.0f));
	}	

	/**
	 * Save the node coordinates to an RSML datafile
	 * @param dataOut
	 * @throws IOException
	 */
	public void saveCoordinatesToRSML(FileWriter dataOut) throws IOException {
		String nL = System.getProperty("line.separator");
		dataOut.write("						<point x='" + x + "' y='" + y + "'/>" + nL);      
	}	
	
	/**
	 * Set the parent node
	 * @param p
	 */
	 public void setParent(Node p){
		 parent = p;
	 }
	 
	 /**
	  * Set the child node
	  * @param c
	  */
	 public void setChild(Node c){
		 child = c;
		 c.setParent(this);
	 }
	 
	 
	 /**
	  * Set the selected state of the node
	  * @param sel
	  */
	 public void setSelected(boolean sel){
		 isSelected = sel;
	 }
	 
	 /**
	  * 
	  * @return
	  */
	 public boolean isSelected(){
		 return isSelected;
	 }	
	
	 
	/**
	 * 
	 * @return
	 */
	public int getXPos(){
		return x;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getYPos(){
		return y;
	}

	/**
	 * 
	 * @return
	 */
	public int getTime(){
		return time;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDiameter(){
		return diameter;
	}	 
}
