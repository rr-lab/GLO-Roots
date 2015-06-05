
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * This class is handling the different object (segment, nodes and apexes) create during the analysis.
 * @author guillaumelobet
 *
 */

public class RootSystem  implements TreeModel{
	
	boolean isReporterImage = false;
	boolean useAllSegments = false;
	boolean attachReporter = false;
	
	// Object lists
	ArrayList<Segment> segList;
	ArrayList<Reporter> repList, repListToAttach;
	
	// Selected objects
	Reporter selectedRep, hoverRep;
	Segment selectedSegment, hoverSegment;
	Node selectedNode;
	
	// Image informations
	ImagePlus imReporter, imSeg, imSoil;
	
	private String imName;
	public int time = 0;
	int attachReporterTo = 0;
	public double scale = 300;
	public double pixelSize = 1 / (scale);
	
	float maxDistFromApex = 4;
	
	// Display
	public GeneralPath nodesGP = new GeneralPath();
	static AlphaComposite ac1 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f);
	static Color nodesColor = Color.orange;
    private Vector<TreeModelListener> treeModelListeners = new Vector<TreeModelListener>();
    public Interface GLORWin = Interface.getInstance();

	RCImageCanvas ric;
	
	/**
	 * 
	 */
	public RootSystem(String im, boolean useAll, int ti, double sc, int attachTo, boolean attach){
		segList = new ArrayList<Segment>();
		repList = new ArrayList<Reporter>();
		imName = im;
		useAllSegments = useAll;
		time = ti;
		scale = sc;
		pixelSize = 1 / scale;
		attachReporterTo = attachTo;
		attachReporter = attach;
		
		GLORWin.setCurrentRootModel(this);		
		
	}
	
	
	/**
	 * 
	 * @param im
	 * @param flag
	 */
	public void setChannel(ImagePlus im, int flag){
		switch(flag){
			case Util.REPORTER: imReporter = im; break;
			case Util.STRUCTURE: imSeg = im; break;
			case Util.SOIL: imSoil = im; break;
		}
	}
	

	
	/**
	 * Refresh the graphics
	 */
	public void repaint() {
		ric.repaint();
	}	
	
	// Attach functions
	
	
	/**
	 * 
	 * @param seg
	 */
	public void attachSegment(Segment seg){
		segList.add(seg);
	}

	/**
	 * 
	 * @param ap
	 */
	public void attach(Reporter rep, int type){
		switch(type){
			case Util.REPORTER: repList.add(rep); break;
			case Util.STRUCTURE: repList.add(rep); break;
		}
	}	
	
	// Get functions
	
	 
	   /**
	    * Read rsml datafile structure
	    * @param f
	    */
	   public void readRSML(String f) {

		   // Choose the datafile
		   String fPath = f;
		        		   
		   org.w3c.dom.Document documentDOM = null;
		   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		   try {
			   DocumentBuilder builder = factory.newDocumentBuilder();
			   documentDOM = builder.parse(new File(fPath) );
		   }
		   catch (SAXException sxe) {
			   logReadError();
			   return;
		   }
		   catch (ParserConfigurationException pce) {
			   logReadError();
			   return;
		   }
		   catch (IOException ioe) {
			   logReadError();
			   return;
		   }

		   documentDOM.normalize();
		      
		   org.w3c.dom.Node nodeDOM = documentDOM.getFirstChild();
		      	   
		   if (!nodeDOM.getNodeName().equals("rsml")) {
			   logReadError();
			   return;
		   }	
		   
		   // Navigate the whole document
		   nodeDOM = nodeDOM.getFirstChild();
		   while (nodeDOM != null) {
			   
			   String nName = nodeDOM.getNodeName();
			   
			   // Get and process the metadata
			   if(nName.equals("metadata")){
				   org.w3c.dom.Node nodeMeta = nodeDOM.getFirstChild();			   
				   while (nodeMeta != null) {		   
					   	String metaName = nodeMeta.getNodeName();				   	
					   	// Get the image resolution
					   	if(metaName.equals("resolution")) scale = Float.valueOf(nodeMeta.getFirstChild().getNodeValue());
					   	if(metaName.equals("file-key")) imName = nodeMeta.getFirstChild().getNodeValue();
						nodeMeta = nodeMeta.getNextSibling();
				   }
			   }
			         
			   // Get the plant
			   if(nName.equals("scene")){
				   org.w3c.dom.Node nodeScene = nodeDOM.getFirstChild();
				   while (nodeScene != null) {		   
					   	String sceneName = nodeScene.getNodeName();
					   	
					   if(sceneName.equals("plant")){
						   org.w3c.dom.Node nodeRoot = nodeScene.getFirstChild();
						   while (nodeRoot != null) {
							   String rootName = nodeRoot.getNodeName();
					   
							   // Get the Roots
							   if(rootName.equals("root")){
								  Segment seg = new Segment(nodeRoot, this);
								  segList.add(seg);
								  if(seg.attachedRep != null) repList.add(seg.attachedRep);
							   }
							   
							   // Get the annotations
							   if(rootName.equals("annotations")){

								   org.w3c.dom.Node nodeAnnot = nodeRoot.getFirstChild();
								   while (nodeAnnot != null) {
									   String annotName = nodeAnnot.getNodeName();

									   // Get the annotations
									   if(annotName.equals("annotation")){
										  Reporter rep = new Reporter(nodeAnnot, this);
										  repList.add(rep);
									   }
									   nodeAnnot = nodeAnnot.getNextSibling();
								   }
							  }
							  nodeRoot = nodeRoot.getNextSibling();
						   }
					  }
					   nodeScene = nodeScene.getNextSibling();  
				   }	 
			   }
			   nodeDOM = nodeDOM.getNextSibling();  
		   }
	   	}
		    
	   
	   
	   	
	   
	/**
	 * Save function for the common XML structure
	 * @param fName
	 */
	public void saveToRSML(String fName){
	      FileWriter dataOut;

	      fName = fName.substring(0, fName.lastIndexOf('.'));
	      try {
	         dataOut = new FileWriter(fName+".rsml") ;
	         }
	      catch (IOException e) {
	         log("The datafile cannot be created or written to.");
	         log("Please check you have a WRITE access to the directory and ");
	         log("there is sufficient space on the disk.");
	         return;
	         }

	      try {
	          String nL = System.getProperty("line.separator");
	          dataOut.write("<?xml version='1.0' encoding='UTF-8'?>" + nL);
	          dataOut.write("<rsml xmlns:po='http://www.plantontology.org/xml-dtd/po.dtd'>" + nL);
	          dataOut.write("	<metadata>" + nL);
	          
	          // Image information
	          dataOut.write("		<version>1</version>" + nL);
	          dataOut.write("		<unit>cm</unit>" + nL);
	          dataOut.write("		<resolution>" + scale + "</resolution>" + nL);
	          dataOut.write("		<last-modified>today</last-modified>" + nL);
	          dataOut.write("		<software>gloria</software>" + nL);
	          dataOut.write("		<user>globet</user>" + nL);
	          dataOut.write("		<file-key>"+imName+"</file-key>" + nL);  
	          dataOut.write("		<time-in-serie>"+time+"</time-in-serie>" + nL);  
	     

	          dataOut.write("		<property-definitions>" + nL);
	          dataOut.write("			<property-definition>" + nL);
	          dataOut.write("		    	<label>direction</label>" + nL);
	          dataOut.write("		        <type>float</type>" + nL);    
	          dataOut.write("		        <unit>degree</unit>" + nL);
	          dataOut.write("			</property-definition>" + nL);
	          dataOut.write("		</property-definitions>" + nL);        
	          

	          dataOut.write("		<image>" + nL);
	          dataOut.write("			<captured></captured>" + nL);  
	          dataOut.write("			<label>" + imName + "</label>" + nL);            
	          dataOut.write("		</image>" + nL);
	          
	          
	          dataOut.write("	</metadata>" + nL);

	          // Define the scene  
	          dataOut.write("	<scene>" + nL);
	          dataOut.write("		<plant>" + nL);        
	         
	          // Print the segments
	          for (int i = 0; i < segList.size(); i ++) {
		           Segment seg = segList.get(i);
		           seg.saveRSML(dataOut);
	          }
	          
	          dataOut.write("			<annotations>" + nL);
	          // Print the un attached reporters
	          for (int i = 0; i < repList.size(); i ++) {
		           Reporter rep = repList.get(i);
		           rep.saveRSML(dataOut);
	          }
	          dataOut.write("			</annotations>" + nL);
	          
	          dataOut.write("		</plant>" + nL);
	          dataOut.write("	</scene>" + nL);
	          dataOut.write("</rsml>" + nL);
	          dataOut.close();
	          }
	       catch (IOException ioe) {
	          log("An I/O error occured while saving the datafile.");
	          log("The new datafile is thus most probably corrupted.");
	          log("It is recommended that you re-open the image and");
	          log("use a backup file before re-saving.");
	       }
		  
	  }
	   
	  	
	
	
	/**
	 * 
	 * @param g2D
	 * @param magnification
	 */
	public void paint(Graphics2D g2D) {

		   
		Rectangle rect = ric.getSrcRect();
		float magnification = (float) ric.getMagnification();

		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		AffineTransform g2dt = g2D.getTransform();
		g2D.transform(new AffineTransform(magnification, 0.0f, 0.0f, magnification,
	                                        magnification * (-rect.x + 0.5f), magnification * (-rect.y + 0.5f)));
	    
		// Display the apex image
		if(isReporterImage){
			for(int i = 0; i < repList.size(); i++){
				Reporter rep = repList.get(i); 
				rep.needRefresh = true;
				rep.paint(g2D, false);
			}
		}
		// Display the segment image
		else{
			if(attachReporter){
				if(repListToAttach != null){
					for(int i = 0; i < repListToAttach.size(); i++){
						Reporter rep = repListToAttach.get(i); 
						rep.needRefresh = true;
						rep.paint(g2D, true);
					}
				}
			}
			for(int i = 0; i < segList.size(); i++){
				Segment seg = segList.get(i); 
				seg.paint(g2D);
			}		
		}
		g2D.setTransform(g2dt);

	}	
	
	/**
	 * Automatically attach all the reporters in the images
	 */
	public void attachReporters(boolean toSeg){
			
		if(toSeg){
			IJ.log("Attach segment");
			if(repListToAttach != null){
				// Get all the apexes
				for(int i = 0; i < repListToAttach.size(); i++){
					Reporter rep = repListToAttach.get(i);
					Segment closestSegment = null;
					float distMin = 1e9f;
					// Get all the segments
					for(int j = 0; j < segList.size(); j++){
						Segment seg = segList.get(j);
		
						if(seg.attachedRep == null){ // cannot attached a segment that is already attached
	
							// if not, get its distance to the apex
							float dist = Util.norm(seg.firstNode.x - rep.xm, seg.firstNode.y - rep.ym);
							if(dist < distMin){
								distMin = dist;
								closestSegment = seg;
							}
							dist = Util.norm(seg.lastNode.x - rep.xm, seg.lastNode.y - rep.ym);
							if(dist < distMin){
								distMin = dist;
								closestSegment = seg;
							}
						}
					}
					
					if(distMin < maxDistFromApex * rep.feret){
						closestSegment.attachReporter(rep);
						rep.attachSegment(closestSegment);
					}
				}
			}
		}
		// Attach reporter to previous reporters (assign the same name)
		else{
			IJ.log("Attach reporters");
			if(repListToAttach != null){
				// Get all the apexes
				for(int i = 0; i < repList.size(); i++){
					Reporter rep = repList.get(i);
					Reporter closestReporter = null;
					float distMin = 1e9f;
					// Get all the segments
					for(int j = 0; j < repListToAttach.size(); j++){
						Reporter rep2 = repListToAttach.get(j);
						if(!rep2.attached){
							// if not, get its distance to the apex
							float dist = Util.norm(rep2.xm - rep.xm, rep2.ym - rep.ym);
							if(dist < distMin){
								distMin = dist;
								closestReporter = rep2;
							}
						}
					}			
					if(distMin < 50){
						if(closestReporter != null){
							rep.repID = closestReporter.repID;
							closestReporter.attached = true;
						}
					}
					
				}
			}
		}
	}
	
	
	/**
	 * Automatically attach all the reporters in the images
	 */
	public void attachReporters2(){
		if(repListToAttach != null){
			// Get all the apexes
			for(int i = 0; i < repListToAttach.size(); i++){
				Reporter rep = repListToAttach.get(i);
				
				Segment closestSegment = null;
				float distMin = 1e9f;
				// Get all the segments
				for(int j = 0; j < segList.size(); j++){
					Segment seg = segList.get(j);
	
					if(seg.attachedRep == null){ // cannot attached a segment that is already attached

						// if not, get its distance to the apex
						float dist = Util.norm(seg.firstNode.x - rep.xm, seg.firstNode.y - rep.ym);
						if(dist < distMin){
							distMin = dist;
							closestSegment = seg;
						}
						dist = Util.norm(seg.lastNode.x - rep.xm, seg.lastNode.y - rep.ym);
						if(dist < distMin){
							distMin = dist;
							closestSegment = seg;
						}
					}
				}
				
				if(distMin < maxDistFromApex * rep.feret/2){
					closestSegment.attachReporter(rep);
					rep.attachSegment(closestSegment);
				}
			}
		}
		
	}	
	
	/**
	 * Selected the apex or segment at position x/y
	 * @param x
	 * @param y
	 * @return
	 */
	public int select(float x, float y) {
		x -= 0.5f; 
		y -= 0.5f;

		// Reset the selected objects
		if(selectedSegment != null) selectedSegment.setSelected(false);
		selectedSegment = null;
		if(selectedNode != null) selectedNode.setSelected(false);
		selectedNode = null;		
		if(selectedRep != null) selectedRep.setSelected(false);
		selectedRep = null;

		
		if(isReporterImage){
			for (int i = 0; i < repList.size(); i++) {
				Reporter rep = repList.get(i);
				if (rep.contains(x, y)) {
					selectedRep = rep;    
					rep.setSelected(true);
	            	log(displayReporterInfo());
	            	return 1;
	         	}
			}
		}
		else{
			if(repListToAttach != null){
				for (int i = 0; i < repListToAttach.size(); i++) {
			         Reporter rep = repListToAttach.get(i);
			         if (rep.contains(x, y)) {
			        	 selectedRep = rep;    
			            rep.setSelected(true);
			            if(rep.attachedSegment != null){
			            	rep.attachedSegment.setSelected(true);
			            	selectedSegment = rep.attachedSegment;
			            }
			            log(displayReporterInfo());
			            return 1;
			         }
				}	
			}
			
			for (int i = 0; i < segList.size(); i++) {
		         Segment seg = segList.get(i);
		         if (seg.contains(x, y)) {
		        	selectedSegment = seg;    
		            seg.setSelected(true);
		            
		            if(seg.attachedRep != null){
		            	seg.attachedRep.setSelected(true);
		            	selectedRep = seg.attachedRep;
		            }
		            Node n = seg.firstNode;
		            while(n != null){
		            	if(n.contains(x, y)){
		            		selectedNode = n;
		            		n.setSelected(true);
		            	}
		            	n = n.child;
		            }
		            log(displaySegmentInfo());	            
		            return 1;
		         }
			}	
		}
		return 0;
	}		
	
	/**
	 * delete the apex or segment at position x/y
	 * @param x
	 * @param y
	 * @return
	 */
	public int delete(float x, float y) {
		x -= 0.5f; 
		y -= 0.5f;	
		
		for (int i = 0; i < repList.size(); i++) {
	         Reporter rep = repList.get(i);
	         if (rep.contains(x, y)) {
	        	 delete(rep);
	        	 return 1;
	         }
		}		
		
		for (int i = 0; i < segList.size(); i++) {
	         Segment seg = segList.get(i);
	         if (seg.contains(x, y)) {
	        	 delete(seg);
	            return 1;
	         }
		}		
		return 0;
	}	

	
	/**
	 * Delete a given apex
	 * @param ap
	 */
	public void delete(Reporter rep){
        if(rep.attachedSegment != null) rep.attachedSegment.attachedRep = null;
		repList.remove(rep);
	}
		
	
	
	/**
	 * Delete a given segment
	 * @param seg
	 */
	public void delete(Segment seg){
   	 	if(seg.attachedRep != null) seg.attachedRep.attachedSegment = null;
		segList.remove(seg);
	}
	

	
	/**
	 * Move the selected node to an x/y position
	 * @param x
	 * @param y
	 * @param flag
	 */
	public void moveSelectedReporter(int x, int y) {  
		selectedRep.move(x,y);
	}	
	
	/**
	 * Move the selected node to an x/y position
	 * @param x
	 * @param y
	 * @param flag
	 */
	public void moveSelectedNode(int x, int y) {  
		selectedNode.move(x,y);
		hover(x, y);
	}	
	
	/**
	 * Selected the apex at position x/y
	 * @param x
	 * @param y
	 * @return
	 */
	public int hover(float x, float y) {
		x -= 0.5f; 
		y -= 0.5f;
		
		// reset the hovered objects
		if(hoverRep != null) hoverRep.setHover(false);
		hoverRep = null;	
		if(hoverSegment != null) hoverSegment.setHover(false);
		hoverSegment = null;				
		
		if(repListToAttach != null){
			for (int i = 0; i < repListToAttach.size(); i++) {
		         Reporter rep = repListToAttach.get(i);
		         if (rep.contains(x, y)) {
		            hoverRep = rep;    
		            rep.setHover(true);
		            selectedSegment.setHover(true);
		            hoverSegment = selectedSegment;
		            return 1;
	         	}
			}
		}		
		
		return 0;
	}		
	
	
	/**
	 * 
	 * @param ric
	 */
	public void setImageCanvas(RCImageCanvas ric){
		this.ric = ric;
	}
	



	/*
	 * Auto generated method to make the root model an implementation of TreeModel
	 * @see javax.swing.tree.TreeModel#getRoot()
	 */
	
	/**
	 * 
	 */
	public Object getRoot() {
		return this;
	}

	/**
	 * 
	 */
	public Object getChild(Object parent, int index) {
		if(parent.equals(this)) {
			ArrayList<Segment> rA = new ArrayList<Segment>();
			for(int i = 0 ; i < segList.size(); i++){
				Segment r = (Segment) segList.get(i);
				rA.add(r);
			}		
			return rA.get(index);
		}
		else{
			if(parent instanceof Segment){
				Segment seg = (Segment) parent;
				if(seg.attachedRep != null) return seg.attachedRep;
				return null;
			}
			else return null;
		}

	}

	/**
	 * 
	 */
	public int getChildCount(Object parent) {
		if(parent.equals(this)){
			ArrayList<Segment> rA = new ArrayList<Segment>();
			for(int i = 0 ; i < segList.size(); i++){
				Segment r = (Segment) segList.get(i);
				rA.add(r);
			}
			return rA.size();
		}

		else{
			if(parent instanceof Segment){
				Segment seg = (Segment) parent;
				if(seg.attachedRep != null) return 1;
				return 0;
			}
			else return 0;
		}

	}

	/**
	 * 
	 */
	public boolean isLeaf(Object node) {
		if(node.equals(this)) return false;
		else {
			if(node instanceof Segment){
				Segment r = (Segment) node;
				return r.attachedRep == null;
			}
			else return true;
		}
	}

	/**
	 * 
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {
		
	}

	/**
	 * 
	 */
	public int getIndexOfChild(Object parent, Object child) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 
	 */
	public void addTreeModelListener(TreeModelListener l) { treeModelListeners.addElement(l);}

	/**
	 * 
	 */
	public void removeTreeModelListener(TreeModelListener l) {treeModelListeners.removeElement(l);}
	
	/**
	 * 
	 */
	public void windowActivated(WindowEvent e) {
		GLORWin.setCurrentRootModel(this);
	}	
	
	public String displayObjectInfo(Object obj){
		if(obj instanceof Segment) return displaySegmentInfo();
		else if (obj instanceof Reporter) return displayReporterInfo();
		else return "Please select object";
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String displaySegmentInfo(){
		return( "Segment name: "+selectedSegment.segName+"\n"+
				"Segment id: "+selectedSegment.segID+"\n"+
				"----------------------------------------\n"+
				"Length: "+selectedSegment.getLength()+"\n"+
				"Number of nodes: "+selectedSegment.getNNodes()+"\n");	}

	/**
	 * 
	 * @return
	 */
	public String displayReporterInfo(){
		return( "Reporter name: "+selectedRep.repName+"\n"+
				"Reporter id: "+selectedRep.repID+"\n"+
				"----------------------------------------\n"+
				"X: "+selectedRep.getXPos()+"\n"+
				"Y: "+selectedRep.getYPos()+"\n"+
				"Length: "+selectedRep.getFeret()+"\n"+
				"Intensity: "+selectedRep.getIntensity()+"\n"+	
				"Intensity ratio: "+selectedRep.getIntensityRatio()+"\n");	
	}

	
	/**
	 * 
	 * @return
	 */
	public String displayInfo(){
		return( "Image name: "+imName+"\n"+
				"----------------------------------------\n"+
				"Number of segment: "+segList.size()+"\n"+
				"----------------------------------------\n"+
				"Number of reporters: " + repList.size()+"\n"+
				"Number of previous reporters: "+this.repListToAttach.size()+"\n");
	}	
	
	
	public void selectObject(Object obj, boolean flag){
		if(obj instanceof Segment) selectSegment((Segment) obj, flag);
		if (obj instanceof Reporter) selectReporter((Reporter) obj, flag);
	}
	
	/**
	 * Select a segment in the list
	 * @param seg
	 * @param flag
	 */
	public void selectSegment(Segment seg, boolean flag){
		seg.setSelected(flag);
		if(flag = true) selectedSegment = seg;
	}

	
	
	/**
	 * Select and apex in the apex list
	 * @param ap
	 * @param flag
	 */
	public void selectReporter(Reporter rep, boolean flag){
		if(selectedRep != null){
			selectedRep.setSelected(false);
			selectedRep = null;
		}
		rep.setSelected(flag);
		if(flag = true) selectedRep = rep;
	}
	
	
	/**
	 * 
	 * @param flag
	 */
	public void setReporterImage(boolean flag){
		isReporterImage = flag;
	}

	
	/**
	 * Process the information contained in the soil channel
	 */
	public void processSoil(int type){
		ResultsTable rt = new ResultsTable();
		int box = 50;
		
		switch(type){
		case Util.STRUCTURE:
			IJ.log("Retrieving environmental values for each segment");
			for(int i = 0; i < segList.size(); i++){
				Segment seg = segList.get(i);
				rt.reset();
				imSoil.setRoi(new Rectangle(seg.lastNode.x - box, seg.lastNode.y - box, box*2, box*2));
				Analyzer meas = new Analyzer(imSoil,  Measurements.MEAN, rt);
				meas.measure();
				seg.environment = (float) rt.getValue("Mean", 0);
			}
			break;
		
		case Util.REPORTER: 
			IJ.log("Retrieving environmental values for each reporter");
			for(int i = 0; i < repList.size(); i++){
				Reporter rep = repList.get(i);
				rt.reset();
				imSoil.setRoi(new Rectangle(rep.xm - box, rep.ym - box, box*2, box*2));
				Analyzer meas = new Analyzer(imSoil,  Measurements.MEAN, rt);
				meas.measure();
				rep.environment = (float) rt.getValue("Mean", 0);
			}
			break;
		}
	}
	
	/**
	 * Export the segment information to a CSV file
	 * @param pw
	 */
	public void exportSegmentsToCSV(PrintWriter pw){
		
		for(int i = 0; i < segList.size(); i++){
			Segment seg = segList.get(i);
			if(!useAllSegments){
				if(seg.attachedRep != null){
					String stmt = "";
					stmt = stmt.concat(imName+",");
					stmt = stmt.concat(seg.getTime()+",");
					stmt = stmt.concat(seg.segID+",");
					stmt = stmt.concat(seg.getLength()+",");
					stmt = stmt.concat((seg.getDirection()* (180 / Math.PI))+",");
					stmt = stmt.concat(seg.firstNode.x+",");
					stmt = stmt.concat(seg.firstNode.y+",");
					stmt = stmt.concat(seg.lastNode.x+",");
					stmt = stmt.concat(seg.lastNode.y+",");
					stmt = stmt.concat(seg.environment+",");
					stmt = stmt.concat(seg.attachedRep.repID+",");
					stmt = stmt.concat(seg.attachedRep.getIntensity()+",");
					stmt = stmt.concat(seg.attachedRep.getIntensityRatio()+",");
					stmt = stmt.concat(seg.attachedRep.getFeret()+",");
					stmt = stmt.concat(seg.attachedRep.getXPos()+",");
					stmt = stmt.concat(seg.attachedRep.getYPos()+"");
					pw.println(stmt);
				}
			}
			else{
				String stmt = "";
				stmt = stmt.concat(imName+",");
				stmt = stmt.concat(seg.getTime()+",");
				stmt = stmt.concat(seg.segID+",");
				stmt = stmt.concat(seg.getLength()+",");
				stmt = stmt.concat(Math.abs((seg.getDirection()* (180 / Math.PI))-270)+",");
				stmt = stmt.concat(seg.firstNode.x+",");
				stmt = stmt.concat(seg.firstNode.y+",");			
				stmt = stmt.concat(seg.lastNode.x+",");			
				stmt = stmt.concat(seg.lastNode.y+",");			
				stmt = stmt.concat(seg.environment+",");
				if(seg.attachedRep != null){
					stmt = stmt.concat(seg.attachedRep.repID+",");
					stmt = stmt.concat(seg.attachedRep.getIntensity()+",");
					stmt = stmt.concat(seg.attachedRep.getIntensityRatio()+",");
					stmt = stmt.concat(seg.attachedRep.getFeret()+",");
					stmt = stmt.concat(seg.attachedRep.getXPos()+",");
					stmt = stmt.concat(seg.attachedRep.getYPos()+"");
				}
				else {
					stmt = stmt.concat("-1,");
					stmt = stmt.concat("-1,");
					stmt = stmt.concat("-1,");
					stmt = stmt.concat("-1,");
					stmt = stmt.concat("-1,");
					stmt = stmt.concat("-1");
				}
				pw.println(stmt);
			}
		}
	}
	
	/**
	 * Export the reporter information to an csv file
	 * @param pw
	 */
	public void exportReportersToCSV(PrintWriter pw){
		
		for(int i = 0; i < repList.size(); i++){
			Reporter rep = repList.get(i);
			String stmt = "";
			stmt = stmt.concat(imName+",");
			stmt = stmt.concat(rep.getTime()+",");
			stmt = stmt.concat(rep.repID+",");
			stmt = stmt.concat(rep.getIntensity()+",");
			stmt = stmt.concat(rep.getIntensityRatio()+",");
			stmt = stmt.concat(rep.getEnvironment()+",");
			stmt = stmt.concat(rep.getFeret()+",");
			stmt = stmt.concat(rep.getXPos()+",");
			stmt = stmt.concat(rep.getYPos()+"");
			pw.println(stmt);
		}
	}	
	
	
	
	
	/**
	 * Log function
	 * @param s the string to write to the log
	 */
	public void log(String s){
		log(s, false);
	}

	
	/**
 	* Log functions
 	* @param s the string to write to the log
 	* @param keep do we keep the previous line in the log
 	*/
	public void log(String s, boolean keep){
		if(keep) s = s.concat(GLORWin.infoPane.getText());
		GLORWin.infoPane.setText(s);
		
	}
	
	/**
	 * 
	 */
	private void logReadError() {
		log("An I/O error occured while attemping to read the datafile.");
		log("A new empty datafile will be created.");
		log("Backup versions of the datafile, if any, can be loaded");
		log("using the File -> Use backup datafile menu item.");
	}

}
