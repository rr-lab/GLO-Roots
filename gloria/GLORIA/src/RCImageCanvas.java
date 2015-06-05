
/**
 * @author Guillaume Lobet, Universite de Liege
 * 
 * The GLORImageCanvas create a layer on top of the image to display the root and apex
 * informations. It does not touch to the image in itself. 
 * This is the interaction layer wit the user.
 */

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


class RCImageCanvas extends ImageCanvas
                      implements ActionListener, KeyEventDispatcher {

	private static final long serialVersionUID = 1626426537236634465L;
	ImageWindow imw;
	RootSystem rs;
	private static Toolbar IJTool = Toolbar.getInstance();

	
	
	/**
	 * Constructor
	 * @param imp the image to analyze
	 * @param rs the root system
	 */
	public RCImageCanvas(ImagePlus imp, RootSystem rs) {
		super(imp);
		this.rs = rs;
		magnification = getMagnification();	    
		IJTool.addMouseListener(this);		
	}

	/**
	 * Set the image window of the canvas
	 * @param imw
	 */
	public void setImageWindow(ImageWindow imw) {
		this.imw = imw;
	}
	
	/**
	 * Display the graphics
	 */
   public void paint(Graphics g) { 
      super.paint(g);
      if (rs != null){
    	  rs.paint((Graphics2D) g);
      }
   }
	
   
	/**
	 * Events triggers when the mouse is pressed
	 * Popup menu are handled here and not in mouseClicked() to be consistent with the parent class behavior
	 */
   public void mousePressed(MouseEvent e) {
	  // mouseClicked() assumes this has been done
	  if(rs.select(offScreen2DX(e.getX()), offScreen2DY(e.getY())) == 1){
		  repaint();  
	  }
	  else{
		  rs.selectedRep = null;
		  rs.selectedSegment = null;
		  repaint();
	  }
   }   
   
   /**
    * Event trigger when the mouse is clicked
    */
   public void mouseClicked(MouseEvent e){
	   if(e.isControlDown()){
		   rs.delete(offScreen2DX(e.getX()), offScreen2DY(e.getY()));
		   repaint();
	   }
	   if(e.isAltDown()){
		   if(rs.selectedSegment.contains(offScreen2DX(e.getX()), offScreen2DY(e.getY()))){
			   rs.selectedSegment.insertNode(offScreen2DX(e.getX()), offScreen2DY(e.getY()));
			   repaint();
		   }
	   }
	   
   }
   
	/**
	 * Events when the moused is dragged. Mainly when a node is moved
	 */
   public void mouseDragged(MouseEvent e) {
      
	   if(rs.selectedRep != null){
    	  rs.moveSelectedReporter((int)offScreen2DX(e.getX()), (int) offScreen2DY(e.getY()));
    	  repaint();
       }
      
      if(rs.selectedNode != null){
    	  rs.moveSelectedNode((int)offScreen2DX(e.getX()), (int) offScreen2DY(e.getY()));
    	  repaint();
       }
     super.mouseDragged(e);
   }   
   
   /**
  	* Action triggered  with the release of the mouse
  	*/
    public void mouseReleased(MouseEvent e) {

 	   if(rs.selectedNode != null){
 		   if(rs.selectedNode == rs.selectedSegment.firstNode){
	 		   if(rs.hoverRep != null){
	 			   if(rs.selectedSegment.getTime() > rs.hoverRep.getTime()){
	 				   rs.selectedSegment.attachReporter(rs.hoverRep);
	 		 	 	   repaint();
	 			   }
	 		   }
 		   }
 	   }
 	   
 	   if(rs.selectedNode != null){
 		   if(rs.selectedNode == rs.selectedSegment.firstNode){
	 		   if(rs.hoverRep == null){
	 			   if(rs.selectedSegment.attachedRep != null){
	 			   		rs.selectedSegment.detachReporter();
	 			   		repaint();
	 			   }
	 		   	}
 		   }
 	   } 	   
 	   if(rs.hoverRep != null){
 		   rs.hoverRep.setHover(false);
 		   rs.hoverRep = null;
 	 	   repaint();
 	   }
 	   
 	   // If the selected apex was moved, update the mean value
 	   if(rs.selectedRep != null){
 		   rs.selectedRep.updateIntensity();
 	 	   repaint();
 	   }
 	   
       super.mouseReleased(e);
    }   
   
   
	/**
	 * 
	 * @param x
	 * @return
	 */
  public float offScreen2DX(int x) {
     return (float) (srcRect.x + (x / getMagnification()));
     }
  
	/**
	 * 
	 * @param y
	 * @return
	 */
  public float offScreen2DY(int y) {
     return (float) (srcRect.y + (y / getMagnification()));
     }

	public boolean dispatchKeyEvent(KeyEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}   
   



}
