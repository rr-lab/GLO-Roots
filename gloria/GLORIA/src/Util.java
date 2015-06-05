
/**
* @author Guillaume Lobet | Universite de Liege
* @date:  2014-06-16 
* Util class, containing various utility fonctions
**/

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class Util {

	final static Color PASSIVE = Color.red;
	final static Color ATTACHED = Color.MAGENTA;
	final static Color SELECTED = Color.green;
	final static Color PREVIOUS = Color.yellow;
	
	static final int ATTACH_PREVIOUS = 0;
	static final int ATTACH_CURRENT = 1;
	
	public final static int REPORTER = 1;
	public final static int STRUCTURE = 2;
	public final static int SOIL = 3;
	
	final static AlphaComposite AC1 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
	final static AlphaComposite AC2 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

	
	/**
	 * Compute the image to delete small 'orphan' particle. 
	 * These particles are defined has having less than 5 neighbors (out of 8) 
	 * @param the original binary image
	 * @return the new image
	 */
	public static BinaryProcessor cleanImage(BinaryProcessor bp){
		for(int w = 0; w < bp.getWidth(); w++){
			for(int h = 0; h < bp.getHeight(); h++){			   
				if(bp.get(w, h) < 125){
					int n = nNeighbours(bp, w, h);
					if(n < 5){ // If the pixel has less than 5 neighbours, delete it and its neighbours
						for(int i = w-1; i <= w+1; i++){
							for(int j = h-1; j <= h+1; j++){
								bp.setColor(255);
								bp.drawPixel(i,j);	
							}
						}
					}
				}
			}   
		}
		return bp;
	}
	
	/**
	 * Compute the image skeleton and delete the connection, to create small segments in the image. 
	 * @param the original binary image
	 * @return the new image
	 */
	public static BinaryProcessor removeSkeletonConnections(BinaryProcessor bp){
		for(int w = 0; w < bp.getWidth(); w++){
			for(int h = 0; h < bp.getHeight(); h++){			   
				if(bp.get(w, h) < 125){
					int n = nNeighbours(bp, w, h);
					if(n > 3){ // If the pixel has more than 3 neighbours, delete it and its neighbours
								bp.drawPixel(w,h);					
					}
				}
			}   
		}
		return bp;
	}
	   
	
	/**
	 * Compute the image skeleton and delete the connection, to create small segments in the image. 
	 * @param the original binary image
	 * @return the new image
	 */
	public static BinaryProcessor removeSkeletonConnections2(BinaryProcessor bp){
		for(int w = 0; w < bp.getWidth(); w++){
			for(int h = 0; h < bp.getHeight(); h++){			   
				if(bp.get(w, h) < 125){
					int n = nNeighbours(bp, w, h);
					if(n > 3){ // If the pixel has more than 3 neighbours, delete it and its neighbours
						for(int i = w-1; i <= w+1; i++){
							for(int j = h-1; j <= h+1; j++){
								bp.setColor(255);
//								bp.drawPixel(w,h);
								bp.drawPixel(i,j);
							}
						}					
					}
				}
			}   
		}
		return bp;
	}	
	
	
	/**
	 * Compute the number of black neighbors for a point
	 * @param bp the processor
	 * @param x coordinate of the pixel
	 * @param y coordinate of the pixel
	 * @return the number of black neighbors
	 */
	public static int nNeighbours(ImageProcessor bp, int x, int y){
		int n = 0;
		for(int i = x-1; i <= x+1; i++){
			for(int j = y-1; j <= y+1; j++){
				if(bp.getPixel(i, j) < 125) n++;
			}
		}
		return n;
	}	
	
	
	/**
	 * Return the sum of an array
	 * @param vect
	 * @return
	 */
	public static float sum(Vector<Float> vect){
		float sum = 0;
		for(int i = 0; i < vect.size(); i ++) sum = sum + vect.get(i);
		return 	sum;
	}
	
	/**
	 * Return the sum of an array
	 * @param vect
	 * @return
	 */
	public static float sum(float[] vect){
		float sum = 0;
		for(int i = 0; i < vect.length; i ++) sum = sum + vect[i];
		return 	sum;
	}
	
	/**
	 * Return the mean values of an array
	 * @param vect
	 * @return
	 */
	public static float avg(Vector<Float> vect){
		return 	sum(vect)/vect.size();
	}
	
	/**
	 * Return the mean values of an array
	 * @param vect
	 * @return
	 */
	public static float avg(float[] vect){
		return 	sum(vect)/vect.length;
	}

	/**
	 * return the maximum of an array
	 * @param vect
	 * @return
	 */
	public static float max(Vector<Float> vect){
		float max = 0;
		for(int i = 0; i < vect.size(); i++) if(vect.get(i) > max) max = vect.get(i);
		return max;
	}
	
	/**
	 * return the minimum of an array
	 * @param vect
	 * @return
	 */
	public static float min(Vector<Float> vect){
		float min = 10e9f;
		for(int i = 0; i < vect.size(); i++) if(vect.get(i) < min) min = vect.get(i);
		return min;
	}
	
	
	/**
	 * return the standart deviation of an array
	 * @param vect
	 * @return
	 */
	public static float std(Vector<Float> vect){
		float sum = 0;
		float avg = avg(vect);
		for(int i = 0; i < vect.size(); i++) sum = sum + ((vect.get(i) - avg)*(vect.get(i) - avg));
		return (float) Math.sqrt(sum/vect.size());
	}
	
	/**
	 * return the standart deviation of an array
	 * @param vect
	 * @return
	 */
	public static float std(float[] vect){
		float sum = 0;
		float avg = avg(vect);
		for(int i = 0; i < vect.length; i++) sum = sum + ((vect[i] - avg)*(vect[i] - avg));
		return (float) Math.sqrt(sum/vect.length);
	}
	

    
    /**
     * Clean the ROI by removing coordinates that are too close from each other
     * @param roi
     * @return
     */
    	public static PolygonRoi cleanROI(Roi roi, int threshold){
    		
    		// Remove the extra points in the ROI
    		PolygonRoi pRoi = new PolygonRoi(roi.getConvexHull(), Roi.POLYGON);
    		Rectangle rect = pRoi.getBounds();		
    		List<Integer> xList = new ArrayList<Integer>(); 
    		List<Integer> yList = new ArrayList<Integer>();
    		int[] xRoi = pRoi.getXCoordinates();
    		int[] yRoi = pRoi.getYCoordinates();
    		for(int l = 0; l < xRoi.length; l++){
    			xList.add(xRoi[l]);
    			yList.add(yRoi[l]);
    		}
    		for(int l = xRoi.length-1; l > 1; l--){
    			double distRoi = Math.sqrt(Math.pow((xRoi[l] - xRoi[l-1]), 2) + Math.pow((yRoi[l] - yRoi[l-1]), 2));
    			if(distRoi < threshold){
    				xList.remove(l-1);
    				yList.remove(l-1);
    			}
    		}
    		int[] xRoiNew = new int[xList.size()];
    		int[] yRoiNew = new int[yList.size()];
    		for(int l = 0; l < yList.size(); l++){
    			xRoiNew[l] = xList.get(l) + rect.x;
    			yRoiNew[l] = yList.get(l) + rect.y;
    		}
    		return new PolygonRoi(xRoiNew, yRoiNew, yRoiNew.length, Roi.POLYGON);
    	}    
    
    	/**
    	 * 
    	 * @param li
    	 * @return
    	 */
    	public static int[] listToArray(ArrayList<Integer> li){
    		int[] ar = new int[li.size()];
    		for(int l = 0; l < li.size(); l++){
    			ar[l] = li.get(l);
    		}
    		return ar;
    	}
    	
    	
	

	/**
	 * Initialize the CSV connection
	 */
	public static PrintWriter initializeCSV(String folder){	
		
		// Create the connection
		PrintWriter pw = null;
		try{ pw = new PrintWriter(new FileWriter(folder)); }
		catch(IOException e){
			IJ.log("Could not save file "+folder);
			return null;
		}
		return pw;
	}
	

	

	/**
	 * Remove r element from an array to minimize its variance
	 * @param vect
	 * @param r
	 * @return
	 */
	public static Vector<Float> minimizeSTD(float[] vect, int r){
		
		IJ.log("total = "+vect.length+" / remove = "+r);
		
		// Create the array containing the element to keep
		Vector<Float> bestV = new Vector<Float>();
		for(int i = 0; i < vect.length; i++) bestV.add(vect[i]);		
		
		// Remove r element to the vector to minize the variance
		for(int i = 0; i < r; i ++){		
			Vector<Float> v = new Vector<Float>(bestV);
			float stdMin = Util.sum(v);		
			for(int j = 0; j < v.size(); j++){	
				Vector<Float> v1 = new Vector<Float>(v);
				v1.remove(j);
				if(Util.std(v1) < stdMin){
					stdMin = Util.std(v1);
					bestV = new Vector<Float>(v1);
				}
			}
		}
		return bestV;
	}
	
	/**
	 * Get the variance, min, max and mean for any number of elements to keep
	 * @param vect
	 * @return the couple [n element, variance]
	 */
	public static int[][] getSTDDecrease(float[] vect){
		
		int[][] var = new int[vect.length][4];
		
		// Create the array containing the element to keep
		Vector<Float> bestV = new Vector<Float>();
		for(int i = 0; i < vect.length; i++) bestV.add(vect[i]);		
		
		// Remove r element to the vector to minize the variance
		for(int i = 0; i < vect.length; i ++){		
			Vector<Float> v = new Vector<Float>(bestV);
			float stdMin = Util.sum(v);		
			for(int j = 0; j < v.size(); j++){	
				Vector<Float> v1 = new Vector<Float>(v);
				v1.remove(j);
				if(Util.std(v1) < stdMin){
					stdMin = Util.std(v1);
					bestV = new Vector<Float>(v1);
					var[i][0] = (int) ((stdMin / Util.avg(v1))*100);
					var[i][1] = (int) Util.min(bestV);
					var[i][2] = (int) Util.max(bestV);
					var[i][3] = (int) Util.avg(bestV);
				}
			}
		}
		return var;
	}
	
   /**
    * 
    * @param dirX
    * @param dirY
    * @return
    */
   public static float vectToTheta (float dirX, float dirY) {
      float norm = (float) Math.sqrt(dirX * dirX + dirY * dirY);
      return (float) (dirY <= 0 ? Math.acos(dirX / norm) 
              : 2.0 * Math.PI - Math.acos(dirX / norm));      

      }	
   /**
    * 
    * @param dx
    * @param dy
    * @return
    */
   public static float norm(float dx, float dy) {
      return (float) Math.sqrt(dx * dx + dy * dy); 
      }    
   
	
	/**
	 * Create the image folder structure
	 * The structure is:
	 * EXPERIENCE_NAME | GENOTYPE_NAME | TREATMENT_NAME | PLANT_ID
	 */
	public static File createFolderStructure(String folder){
				
		File d0 = new File(folder);
		File dirOriginal = new File(d0.getAbsolutePath()+"/originals/");
		File dirMask = new File(d0.getAbsolutePath()+"/masks/");
		
		if(!dirOriginal.exists()) try{dirOriginal.mkdir();} catch(Exception e){IJ.log("Could not create folder "+dirOriginal);}
		if(!dirMask.exists()) try{dirMask.mkdir();} catch(Exception e){IJ.log("Could not create folder "+dirMask);}
		
		return d0;
	}	
	
	
	/**
	 * Create the image folder structure
	 */
	public static File createFolderStructure(String folder, boolean global, boolean local, boolean direct){
				
		File d0 = new File(folder);

		if(global){
			File dirMask = new File(d0.getAbsolutePath()+"/global/");
			if(!dirMask.exists()) try{dirMask.mkdir();} catch(Exception e){IJ.log("Could not create folder "+dirMask);}
		}		
		if(local){
			File dirDiff = new File(d0.getAbsolutePath()+"/local/");
			if(!dirDiff.exists()) try{dirDiff.mkdir();} catch(Exception e){IJ.log("Could not create folder "+dirDiff);}
		}
		if(direct){
			File dirDiff = new File(d0.getAbsolutePath()+"/dir/");
			if(!dirDiff.exists()) try{dirDiff.mkdir();} catch(Exception e){IJ.log("Could not create folder "+dirDiff);}
		}		
		
		return d0;
	}	
   
	/**
	 * Get an array of String from a String with the form "1,2,3,4"
	 * @param s
	 * @return
	 */
	public static ArrayList<String> getArrayFromString(String s, String sep, boolean last){
		ArrayList<String> al = new ArrayList<String>();
		
		int index = s.indexOf(sep);
		while(index >= 0){
			al.add(s.substring(0, index));
			s = s.substring(index+1, s.length());
			index = s.indexOf(sep);
		} 
		if(last) al.add(s);
		
		return al;
	}    
	
	/**
	 * Get an array of String from a String with the form "1,2,3,4"
	 * @param s
	 * @return
	 */
	public static String getStringFromArray(ArrayList<String> s, String sep){		
		String st = "";
		for(int i = 0; i < s.size(); i++){
			st = st.concat(s.get(i));
			st = st.concat(sep);
		}
		return st;
	}   	
	
	
	/**
	 * 
	 * @param ip
	 * @return
	 */
	public static ImageProcessor thresholdImage(ImageProcessor ip, boolean lowContrast, boolean black, double min){
		
        IJ.log("Thresholding");

		// Equalize and normalize the histogram of the image
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.equalize(ip);
		ce.stretchHistogram(ip, 0.4);		
	        
		// Threshold the image based on its mean value
		ImageStatistics istats = ip.getStatistics();     
		if(black) ip.threshold((int) istats.mean / 2);
		else  ip.threshold((int) istats.mean * 2);
			
		// Clean the image
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(ip, true));	
		if(lowContrast) bp = Util.cleanImage(bp);
		bp.threshold(120);

		bp.invert();
		ImagePlus im = new ImagePlus();
		im.setProcessor(bp);
			
		// Clean the image by removing the small particles. Might be redundant with the previous operation...
		IJ.log("Cleaning the image");
		ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS, Measurements.AREA, rt, min, 10e9, 0, 1);
		pa.analyze(im);
			
		// Get the mask from the ParticuleAnalyser
		ImagePlus globalMask = IJ.getImage(); 
		globalMask.hide(); // Hide the mask, we do not want to display it.
		return globalMask.getProcessor();
	}
	
}
