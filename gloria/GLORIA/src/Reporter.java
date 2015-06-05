import ij.IJ;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


public class Reporter {

	// The  reporter properties
	int xm, ym, xr, yr, xDev, yDev;
	public float feret;
	String repID;
	String repName;
	public float intensity, intensityRatio;
	Roi roi;
	int[] xRoi, yRoi;
	boolean needRefresh = true;
	boolean attached = false;
	public float environment = -1;

	
	// Time coordinates
	private int time;
	
	// Misc 
	boolean isSelected = false;
	boolean isHover = false;
	Segment attachedSegment = null;
	RootSystem rs;
	ResultsTable rt = new ResultsTable();
	
	// Display
	public GeneralPath reporterGP = new GeneralPath();
	public GeneralPath connectionGP = new GeneralPath();
	   
	// The intensity of the apex
	
	/**
	 * Default constructor
	 */
	public Reporter(){
		new Reporter(1, 1, 0, 0, 1, null, null);
		
	}
	
	public Reporter(org.w3c.dom.Node parentDOM, RootSystem rs){
		readRSML(parentDOM);
		this.rs = rs;
	}
	
	/**
	 * Constructor
	 * @param x
	 * @param y
	 * @param intens
	 */
	public Reporter(int x, int y, int ti, float intens, float feret, Roi r, RootSystem rs){
		this.xm = x;
		this.ym = y;
		this.rs = rs;
		time = ti;
		this.feret = feret;
		intensity = intens;
		roi = r;
		xr = roi.getBounds().x;
		yr = roi.getBounds().y;
		xDev = xm - xr;
		yDev = ym - yr;
		PolygonRoi pRoi = new PolygonRoi(roi.getConvexHull(), Roi.POLYGON);
		xRoi = pRoi.getXCoordinates();
		yRoi = pRoi.getYCoordinates();		
		repID = UUID.randomUUID().toString();
		repName = "reporter_"+rs.repList.size();
		rt = new ResultsTable();
		updateIntensityRatio();
	}
	
   	
	
	
	
	// Misc functions
	/**
	 * Move an apex to a x/y position
	 * @param x
	 * @param y
	 */
	public void move(int x, int y) {  
		xm = x;
		ym = y;
		yr = ym - yDev;
		xr = xm - xDev;
		updateIntensity();
		updateIntensityRatio();
		needRefresh = true;
	}

	/**
	 * 
	 */
	public void updateIntensity(){
		if(!needRefresh) return;
		rt.reset();
		rs.imReporter.setRoi(roi);
		Analyzer meas = new Analyzer(rs.imReporter,  Measurements.MEAN, rt);
		meas.measure();
		intensity = (float) rt.getValue("Mean", 0);
		needRefresh = false;
	}
	
	/**
	 * 
	 */
	public void updateIntensityRatio(){
		if(!needRefresh) return;
		if(rs.imSeg != null){
			updateIntensity();
			rt.reset();
			rs.imSeg.setRoi(roi);
			Analyzer meas = new Analyzer(rs.imSeg,  Measurements.MEAN, rt);
			meas.measure();
			intensityRatio = (float) rt.getValue("Mean", 0) / intensity;
		}
		else intensityRatio = -1;
		needRefresh = false;
	}
	
	   /**
	    * Save the data to a RSML file
	    * @param dataOut
	    * @throws IOException
	    */
	   public void saveRSML(FileWriter dataOut) throws IOException {	      
	      String nL = System.getProperty("line.separator");
	      dataOut.write("			<annotation type='reporter' id='"+repID+"' name='"+repName+"'>" + nL);
	      
	      // Reporter information
	      dataOut.write("				<center_of_mass x='"+xm+"' y='"+ym+"'/>" + nL);           
	      dataOut.write("				<bounding_rectangle x='"+xr+"' y='"+yr+"'/>" + nL);           
	      dataOut.write("				<intensity value='"+intensity+"' ratio='"+getIntensityRatio()+"'/>" + nL);           
	      dataOut.write("				<feret value='"+feret+"'/>" + nL);           
	      dataOut.write("				<geometry>" + nL);    
	      dataOut.write("					<polyline>" + nL);
	      for(int i = 0; i < xRoi.length; i++){
	    	  dataOut.write("						<point x='" + xRoi[i] + "' y='" + yRoi[i] + "'/>" + nL);
	      }
	      dataOut.write("					</polyline>" + nL);   	      
	      dataOut.write("				</geometry>" + nL);           

	      dataOut.write("			</annotation>" + nL);
	      }
	   
	/**
	 * Read the mark information from the RSML datafile
	 * @param parentDOMthe xml element containg the mark
	 */
	public void readRSML(org.w3c.dom.Node parentDOM) {  
			
			IJ.log("READ SEGMENT RSML...");
	      org.w3c.dom.Node nodeDOM = parentDOM.getFirstChild();
		  org.w3c.dom.Node nn;

		  nn = parentDOM.getAttributes().getNamedItem("id");
		  if (nn != null) repID = nn.getNodeValue();
		  nn = parentDOM.getAttributes().getNamedItem("name");
		  if (nn != null) repName = nn.getNodeValue();
		  
	      while (nodeDOM != null) {
	         String nName = nodeDOM.getNodeName();
	         if (nName.equalsIgnoreCase("center_of_mass")){
	        	 nn = nodeDOM.getAttributes().getNamedItem("x");
	        	 if (nn != null) xm = Integer.valueOf(nn.getNodeValue()).intValue();
	        	 nn = nodeDOM.getAttributes().getNamedItem("y");
	        	 if (nn != null) ym = Integer.valueOf(nn.getNodeValue()).intValue();
	         }
	         else if (nName.equalsIgnoreCase("bounding_rectangle")){
	        	 nn = nodeDOM.getAttributes().getNamedItem("x");
	        	 if (nn != null) xr = Integer.valueOf(nn.getNodeValue()).intValue();
	        	 nn = nodeDOM.getAttributes().getNamedItem("y");
	        	 if (nn != null) yr = Integer.valueOf(nn.getNodeValue()).intValue();
	         }
	         else if (nName.equalsIgnoreCase("intensity")){
	        	 nn = nodeDOM.getAttributes().getNamedItem("value");
	        	 if (nn != null) intensity = Float.valueOf(nn.getNodeValue()).floatValue() * 1.0f;
	        	 nn = nodeDOM.getAttributes().getNamedItem("ratio");
	        	 if (nn != null) intensityRatio = Float.valueOf(nn.getNodeValue()).floatValue();
	         }
	         else if (nName.equals("feret")) {
	        	 nn = nodeDOM.getAttributes().getNamedItem("value");
	        	 if (nn != null) feret = Float.valueOf(nn.getNodeValue()).floatValue() * 1.0f;
	            }
	         else if (nName.equals("geometry")) {
	        	 ArrayList<Integer> xR = new ArrayList<Integer>();
	        	 ArrayList<Integer> yR = new ArrayList<Integer>();
	        	 org.w3c.dom.Node nodeGeom = nodeDOM.getFirstChild();
	        	 while (nodeGeom != null) {
	        		 String geomName = nodeGeom.getNodeName();
	        		 if (geomName.equals("polyline")) {
	        			 org.w3c.dom.Node nodePoint = nodeGeom.getFirstChild();
	        			 while (nodePoint != null) {
	        				 String pointName = nodePoint.getNodeName();
	        				 if (pointName.equals("point")) {
	        					 nn = nodePoint.getAttributes().getNamedItem("x");
	        		        	 if (nn != null) xR.add(Integer.valueOf(nn.getNodeValue()).intValue());
	        		        	 nn = nodePoint.getAttributes().getNamedItem("y");
	        		        	 if (nn != null) yR.add(Integer.valueOf(nn.getNodeValue()).intValue());
	        				 }
	        				 nodePoint = nodePoint.getNextSibling();
	        			 }
	        		 }
	        		 nodeGeom = nodeGeom.getNextSibling();
	        	 }
	        	 IJ.log("--- length = "+xR.size());
	        	 xRoi = Util.listToArray(xR);
	        	 yRoi = Util.listToArray(yR);
	        	 roi = new PolygonRoi(xRoi, yRoi, yRoi.length, Roi.POLYGON);
	        	 xDev = xm - xr;
	        	 yDev = ym - yr;
	         }          
	         nodeDOM = nodeDOM.getNextSibling();
	      }
	}   
	
	
	
	/**
	 * Does the node contains the x/y coordinates
	 * @param x
	 * @param y
	 * @return true if contains
	 */
	public boolean contains(float x, float y) {
		return reporterGP.contains(x, y);
	}	

	/**
	 * Does the node contains the x/y coordinates
	 * @param x
	 * @param y
	 * @return true if contains
	 */
	public boolean contains(float x, float y, float radius) {
		createGraphics();
		return reporterGP.contains(x, y);
//		return (Point2D.distance(this.xm, this.ym, x, y) < ((feret / 2.0f)*radius));
	}	

	public void paint(Graphics2D g2D, boolean previous){
		createGraphics();
		
		Stroke bs1 = new BasicStroke(1.0f);
		Stroke bs2 = new BasicStroke(0.5f);

		g2D.setStroke(bs1);
		
		g2D.setColor(Util.PASSIVE);
		if(previous) g2D.setColor(Util.PREVIOUS);
		if(isHover) g2D.setColor(Util.ATTACHED);
		if(attachedSegment != null) g2D.setColor(Util.ATTACHED);
		if(isSelected) g2D.setColor(Util.SELECTED);
		
		g2D.setComposite(Util.AC1);
		g2D.draw(reporterGP);	
		
		
		g2D.setComposite(Util.AC2);
		g2D.fill(reporterGP);
		
		g2D.setComposite(Util.AC1);
		
		if(attachedSegment != null){
			g2D.setStroke(bs2);
			g2D.setColor(Util.ATTACHED);
			if(isSelected) g2D.setColor(Util.SELECTED);
			g2D.draw(connectionGP);	
		}
		
	}
	
	
	 public void createGraphics() {  

		 if(needRefresh){
			 reporterGP.reset();
			 connectionGP.reset();
			 reporterGP.moveTo(xRoi[0]+xr, yRoi[0]+yr);
			 for(int i = 1; i < xRoi.length; i++){
		 		reporterGP.lineTo(xRoi[i]+xr, yRoi[i]+yr);
			 }
			 reporterGP.lineTo(xRoi[0]+xr, yRoi[0]+yr);
		 	
			 if(attachedSegment != null){
		 		connectionGP.moveTo(xm, ym);
		 		connectionGP.lineTo(attachedSegment.firstNode.x, attachedSegment.firstNode.y);
			 }
		 }
		 needRefresh = false;
		 
	 }
	
	 
	 /**
	  * 
	  * @param sel
	  */
	 public void setSelected(boolean sel){
		 isSelected = sel;
	 }
	 
	 /**
	  * 
	  * @param sel
	  */
	 public void setHover(boolean hov){
		 isHover = hov;
	 }	 
	 
	 /**
	  * 
	  * @return
	  */
	 public boolean isSelected(){
		 return isSelected;
	 }
	
	// Get functions
	/**
	 * 
	 * @return
	 */
	public float getIntensity(){
		updateIntensity();
		return intensity;
	}
	
	// Get functions
	/**
	 * 
	 * @return
	 */
	public float getIntensityRatio(){
		if(needRefresh) updateIntensityRatio();
		return intensityRatio;
	}	
	
	/**
	 * 
	 * @return
	 */
	public float getEnvironment(){
		return environment;
	}		
	
	/**
	 * 
	 * @return
	 */
	public int getXPos(){
		return xm;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getYPos(){
		return ym;
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
	public float getFeret(){
		return feret;
	}
	
	public void attachSegment(Segment seg){
		attachedSegment = seg;
	}
	
	
}
