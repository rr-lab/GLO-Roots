/**
* @author Guillaume Lobet | Université de Liège
* @date: 2014-06-16
* 
* Global refers to the original images in the time series
* Local refers to the difference between two successive images in the time series
*
**/

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.plugin.ContrastEnhancer;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.EDM;
import ij.process.AutoThresholder.Method;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;

public class RootAnalysis {
	
	// Export parameters
	static File dirAll, dirOriginal, dirMask, dirLocal, dirDir;//, dirConvex;	
	static File[] images; 		
	static String  csvGlobalFolder, csvLocalFolder, csvROIFolder, csvDirFolder, csvShapeFolder, csvEFDFolder, tpsFolder, imName, baseName;
	
	// Image paramaters
	static String experiment, stock, treatment, pot, plantID; 
	
	// Analysis parameters
	static long DAS;
	static int nROI, nTimeSerie, dirMethod, nDirBin, hBin, wBin, nEFD, nCoord;
	static double scalePix, scaleCm, scale, rootMinSize;
	static boolean blackRoots, globalAnalysis, localAnalysis, efdAnalysis, isTimeSerie, shapeAnalysis, horizon, 
		manualCorrection, lowContrast, directionalAnalysis, wideRhizotron, saveTPS, verbatim=false, empty=false, saveImage=false;
	
	static float angle, length, diameter, feret, tortuosity, area, totlength, convexHull, rx, ry, sizeScale,
		depth, width, ax, ay, bx, by, efd, circ, ar, round, solidity, globalFeret, globalAngle, comY, comX;
			
	static PrintWriter pwGlobal, pwLocal, pwROI, pwEFD, pwDir, pwShape, pwTPS;	
	
	/**
	 * Constructor
	 * @param f = File containing the different images
	 * @param file = where to save csv file
	 * @param scaleP = scale, in pixels
	 * @param scaleC = scale, in cm
	 * @param dirmeth = what to do with the names
	 * @param date = starting date of the experiment
	 */
	public RootAnalysis(File f,
			String file,
			float scaleP,
			float scaleC,
			int dirmeth,
			boolean black,
			float minsize,
			boolean local,
			boolean global,
			boolean efd,
			boolean dir,
			boolean shape,
			boolean tps,
			int nefd,
			int ndb,
			boolean mc,
			int wbin,
			int hbin,
			int sbin, 
			boolean ts,
			boolean ho,
			boolean lc,
			boolean rw,
			boolean verb,
			boolean saveImg,
			float sizeSc){
		
		// Set up the different variables
		scalePix = scaleP;
		scaleCm = scaleC;
		dirAll = f;
		dirMethod = dirmeth;
		blackRoots = black;
		rootMinSize = minsize;
		globalAnalysis = global;
		localAnalysis = local;
		efdAnalysis = efd;
		shapeAnalysis = shape;
		saveTPS = tps;
		directionalAnalysis = dir;
		nEFD = nefd;
		manualCorrection = mc;
		lowContrast = false;
		nDirBin = ndb;
		hBin = hbin;
		wBin = wbin;
		isTimeSerie = ts;
		lowContrast = lc;
		wideRhizotron = rw;
		verbatim = verb;
		saveImage = saveImg;
		horizon = ho;
		
		sizeScale = sizeSc;
		rootMinSize = rootMinSize * 2;
		nCoord = sbin;
		
		// Files to save the different data		
		csvGlobalFolder = file.substring(0, file.length()-4)+"-global.csv";
		csvLocalFolder = file.substring(0, file.length()-4)+"-local.csv";
		csvROIFolder = file.substring(0, file.length()-4)+"-roi.csv";
		csvEFDFolder = file.substring(0, file.length()-4)+"-efd.csv";
		csvDirFolder = file.substring(0, file.length()-4)+"-dir.csv";
		csvShapeFolder = file.substring(0, file.length()-4)+"-shape.csv";
		tpsFolder = file.substring(0, file.length()-4)+"-shape.tps";
				
		scale = scaleCm / scalePix;
		
		// Analyze the plants
		analyze();
	}

	
	/**
	 * Perform the analysis of all the images
	 */
	public void analyze(){
		
		ImagePlus nextImage = null, previousImage = null, nextSubImage = null, previousSubImage = null;
		
        // Get all the images files in the directory
		images = dirAll.listFiles();
		for(int i = 0; i< images.length; i++) if(images[i].isHidden()) images[i].delete();
		images = dirAll.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".tiff") || 
		        		name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".jpeg") ||
		        		 name.toLowerCase().endsWith(".png");
		    }
		});


		if(verbatim) IJ.log("Root image analysis started: "+dirAll.getAbsolutePath().toString());
		long startD = System.currentTimeMillis(); // Counter for the processing time
		int counter = 0;
				
		// Initialize the CSV connection
		if(globalAnalysis){
			pwGlobal = Util.initializeCSV(csvGlobalFolder);
			printGlobalCSVHeader();
		}
		if(localAnalysis){
			pwLocal = Util.initializeCSV(csvLocalFolder);
			printLocalCSVHeader();
		}
		if(globalAnalysis){
			pwROI = Util.initializeCSV(csvROIFolder);
			printROICSVHeader();
		}
		if(directionalAnalysis){
			pwDir = Util.initializeCSV(csvDirFolder);
			printDirectionalCSVHeader();
		}		
		if(efdAnalysis){
			pwEFD = Util.initializeCSV(csvEFDFolder);
			printEFDCSVHeader();
		}
		
		if(shapeAnalysis){
			pwShape = Util.initializeCSV(csvShapeFolder);
			printShapeCSVHeader();
			if(saveTPS){
				pwTPS = Util.initializeCSV(tpsFolder);
			}
		}
		
		
		
			
		// Navigate the different images in the time serie
		for(int i = 0; i < images.length; i++){
			
			// Open the image
			nextImage = IJ.openImage(images[i].getAbsolutePath());
			if(verbatim) IJ.log("------------------------");
			
			// If it is a time serie, load the previous image
			if(isTimeSerie){
				try{ if(i > 0){ previousImage = IJ.openImage(images[i-1].getAbsolutePath()); }}
				catch(Exception e){ if(verbatim) IJ.log("Error in local analysis: "+e); }
			} 
			else{
				previousImage = null;
			}
			
	    	
			// Reset the ROI to the size of the image. 
			// This is done to prevent previously drawn ROI (ImageJ keep them in memory) to empede the analysis
			nextImage.setRoi(0, 0, nextImage.getWidth(), nextImage.getHeight());
			
			// Do the same for the previous image
	    	if(previousImage != null) previousImage.setRoi(0, 0, previousImage.getWidth(), previousImage.getHeight());	

			// Process the name to retrieve the experiment information
			baseName = images[i].getName();

			IJ.log("Analysis of image "+(i+1)+" on "+images.length+" started: "+this.baseName);

			// Create the folder structure to store the images
			if(saveImage){
				File dirSave; 
				dirSave = Util.createFolderStructure(dirAll.getAbsolutePath(), globalAnalysis, localAnalysis, directionalAnalysis);
				dirMask = new File(dirSave.getAbsolutePath()+"/01-global/");
				dirLocal = new File(dirSave.getAbsolutePath()+"/02-local/");
				dirDir = new File(dirSave.getAbsolutePath()+"/03-dir/");
			}

			
			// Flag to avoid analysis empty images
			empty = false;
			
			if(verbatim) IJ.log("------------------------");
			if(verbatim) IJ.log("Starting analysis of " + this.baseName);

			if(globalAnalysis && !empty){
				if(verbatim) IJ.log("------------------------");
				if(verbatim) IJ.log("Starting global analysis");
				measureGlobalImage(nextImage);
			}
			if(directionalAnalysis && !empty){
				if(verbatim) IJ.log("------------------------");
				if(verbatim) IJ.log("Starting directional analysis");
				measureDirectionality(nextImage);
			}
		
			
			// Reset the ROI to the total image
			nextImage.setRoi(0, 0, nextImage.getWidth(), nextImage.getHeight());
			if(previousImage != null) previousImage.setRoi(0, 0, previousImage.getWidth(), previousImage.getHeight());

		
			// Measure the image
			if(localAnalysis && !empty){
				if(verbatim) IJ.log("------------------------");
				if(verbatim) IJ.log("Starting local analysis");
				measureLocalImage(nextImage, previousImage, "total");
				
				if(horizon){
					int w = nextImage.getWidth();
					int h = nextImage.getHeight();
					int inc = h / 10;
					
					for(int k = 0; k < 10; k++){
						nextImage.setRoi(0, k * inc, w, inc);
						nextSubImage = nextImage.duplicate();
						if(previousImage != null){
							previousImage.setRoi(0, k * inc, w, inc);
							previousSubImage = previousImage.duplicate();	
						}
						measureLocalImage(nextSubImage, previousSubImage, "layer_"+k);
					}
				}
			}
			
		
			// Close the current image
			nextImage.flush(); nextImage.close(); 
			counter ++;
		}
		// Compute the time taken for the analysis
		long endD = System.currentTimeMillis();
		IJ.log("------------------------");
		IJ.log("------------------------");
		IJ.log(counter+" images analyzed in "+(endD - startD)/1000+" s");		
	}
	
	/**
	 * Process the global image to extract all the useful information 
	 * @param currentImage the current image in the time serie
	 */
	private ImagePlus measureGlobalImage(ImagePlus current){
		
		// Initiate the different ImageJ tools
		ParticleAnalyzer pa;
		EDM edm = new EDM();
		ResultsTable rt = new ResultsTable();
		Calibration cal = new Calibration();
		Calibration calDefault = new Calibration();
		boolean largeRoot = false;
		ImagePlus nextImage;
		
		// Set the scale		
		int scalingFactor = 1;
		cal.setUnit("cm");
		cal.pixelHeight =( scalingFactor * scaleCm) / scalePix;
		cal.pixelWidth = ( scalingFactor * scaleCm) / scalePix;
		
		// Reset Scale
		calDefault.setUnit("px");
		calDefault.pixelHeight = 1;
		calDefault.pixelWidth = 1;
		
		// Initalisation of the image
		if(verbatim) IJ.log("Loading images");
    	ImagePlus currentImage = current.duplicate();
    	ImagePlus skelImage = new ImagePlus();
    	
    	// Keep a copy of the current image for the next run
    	nextImage = currentImage.duplicate();

    	// Pre-process the image
    	if(verbatim) IJ.log("Pre-processing the image");
    	ImageProcessor globalProcessor = currentImage.getProcessor();

    	// Resize the image to speed up the analysis
    	globalProcessor = globalProcessor.resize(globalProcessor.getWidth()/scalingFactor, globalProcessor.getHeight()/scalingFactor);
		
    	// Convert to 8bit image if needed
    	if(globalProcessor.getBitDepth() != 8) globalProcessor = globalProcessor.convertToByte(true);
        
		// If the the image is horizontal, rotate it
		if(globalProcessor.getWidth() > globalProcessor.getHeight()){
			globalProcessor.rotateLeft();
		}
		

    	// If the root is white on black, then invert the image
    	if(blackRoots) globalProcessor.invert(); 
        currentImage.setProcessor(globalProcessor);
        
		// Threshold the image
        if(verbatim) IJ.log("Thresholding the image");

		ImageProcessor globalMaskProcessor = globalProcessor.duplicate();
//		globalMaskProcessor = Util.thresholdImage(globalMaskProcessor, lowContrast, blackRoots, rootMinSize);
//		globalMaskProcessor.invert();
		ImagePlus globalMask = new ImagePlus();
		globalMask.setProcessor(globalMaskProcessor);
		
		currentImage = globalMask.duplicate();	
		
		// CONVEX HULL
		// Compute the distance map. This step is made to re-construct the root system based on its different
		// particles.
		if(verbatim) IJ.log("Computing EDM mask");
		globalMaskProcessor.invert();
		edm.run(globalMaskProcessor);		
		
        // Find the best Threshold and apply it
		if(verbatim) IJ.log("Computing best threshold");
		int tr = getBestThreshold(globalMaskProcessor.duplicate(), lowContrast || largeRoot);
		globalMaskProcessor.threshold(tr);
		
		// Erode the mask to reduce the effect of the extra size of the EDM threshold 
		if(verbatim) IJ.log("Finding convex hull mask");
		globalMaskProcessor.invert();
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(globalMaskProcessor, true));		
		for(int i=0; i < ((2*tr)/3); i++) bp.erode();	
		globalMaskProcessor = bp.duplicate();
		globalMaskProcessor.setAutoThreshold(Method.Mean, false);
		globalMask.setProcessor(globalMaskProcessor);	
		
		
		// Add the object to the ROI manager
		globalMask.setCalibration(calDefault);		
		Analyzer.setResultsTable(rt);
		rt.reset();
		pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, 0, 10e9);
		pa.analyze(globalMask);
		
		// Find the largest object in the image (the root system) in case their are still multiple objects.
		if(verbatim) IJ.log("Find max object");
		int index = 0;
		double max = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			if(rt.getValue("Area", i) > max){
				max = rt.getValue("Area", i);
				index = i;
			}			
		}
		
		
		// Get the convex hull from the ROI manager (the largest object)
		RoiManager manager = RoiManager.getInstance();
		
		if(manager == null){
			if(verbatim) IJ.log(">>>>> NO ROOT DETECTED. GETTING OUT <<<<<");
			empty = true;
			return null;// Safe check
		}
				
		Roi[] roiA = manager.getRoisAsArray();
		Roi convexROI = roiA[index];
		
		if(manualCorrection){
			if(verbatim) IJ.log("Correcting the ROI");
			ImagePlus ip = currentImage.duplicate();
			ip.setCalibration(calDefault);
			ip.show(); // Show the image
			ContrastEnhancer ce = new ContrastEnhancer();
			ce.equalize(ip);
			ip.setRoi(new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON)); // Show the ROI
			new WaitForUserDialog("Correct ROI", baseName+"\n Please correct the ROI by dragging the nodes.\n\n When done, click OK to validate").show(); // Wait for the user to correct the ROI		
			convexROI = ip.getRoi(); // Get the convex hull from the object stored in the ROI Manager	
			ip.hide(); ip.close(); ip.flush();
		}
		
		
		// Save the ROI coordinates
      PolygonRoi polygon = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON); 
      Rectangle bounds = convexROI.getBounds(); 
      int n1 = polygon.getNCoordinates(); 
      int[] x1 = polygon.getXCoordinates(); 
      int[] y1 = polygon.getYCoordinates(); 
      for (int i = 0; i < n1; i++) sendROIDataToCSV("convex", (bounds.x + x1[i]) * (scaleCm/scalePix), 
    		  													(bounds.y + y1[i]) * (scaleCm/scalePix)); 

		if(verbatim) IJ.log("Getting the total surface of root system");
		rt.reset();
		Analyzer.setResultsTable(rt);
		currentImage.getProcessor().fillOutside(convexROI);		
		currentImage.setCalibration(cal);
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, rootMinSize/scalingFactor, 10e9, 0, 1);
		pa.analyze(currentImage);

		ImagePlus coordImage = currentImage.duplicate();
		
		// Get the total surface of all the particles (the different parts of the root system)
		area = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			area += (float) rt.getValue("Area", i);			
		}

		if(verbatim) IJ.log("Getting the total length of root system");
		BinaryProcessor bp1 = new BinaryProcessor(new ByteProcessor(currentImage.getProcessor(), true));
		for(int i=0; i < 3; i++) bp1.smooth();
		bp1.skeletonize();	
//		bp1.invert();
		bp1.threshold(120);
		skelImage.setProcessor(bp1);
		skelImage.setCalibration(cal);
		rt.reset();
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, rootMinSize/scalingFactor, 10e9, 0, 1);
		pa.analyze(skelImage);

		// Get the total surface of all the particles (the different parts of the root system)
		totlength = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			totlength += (float) rt.getValue("Area", i);			
		}
		
		
		// Create a mask with the convexhull
		if(verbatim) IJ.log("Creating convexhull mask");
		globalMaskProcessor = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON).getMask(); 			
		ImagePlus globalConvexHull = new ImagePlus();
		globalConvexHull.setProcessor(globalMaskProcessor);
		globalConvexHull.setCalibration(cal);
		globalMaskProcessor.autoThreshold();
		globalMaskProcessor.invert();
		
		// Compute the Fourrier Descriptors for the ROI.
		// The number of descriptors is set by the users (nEFD)
		// This part uses the EllipticFD plugin from Thomas Boudier and Ben Tupper (EFD)
		if(efdAnalysis){
			if(verbatim) IJ.log("EFD analysis");
			PolygonRoi roi = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON);
			Rectangle rect = roi.getBounds();
			int n = roi.getNCoordinates();
			double[] x = new double[n];
			double[] y = new double[n];
			int[] xp = roi.getXCoordinates();
			int[] yp = roi.getYCoordinates();  
			for (int i = 0; i < n; i++){
			    x[i] = (double) (rect.x + xp[i]);
			    y[i] = (double) (rect.y + yp[i]); 
			}  
			EllipticFD efd = new EllipticFD(x, y, nEFD);
			for (int i = 0; i < efd.nFD; i++) {
				sendEFDDataToCSV(i+1, efd.ax[i], efd.ay[i],efd.bx[i],efd.by[i],efd.efd[i]);
			}
			
			// Create the roi from the EFD to display as an overlay on the saved image
		    double[][] xy = efd.createPolygon();
		    int m = xy.length;
		    int[] xefd = new int[m];
		    int[] yefd = new int[m];
		    for (int i = 0; i < m; i++){
		    	xefd[i] = (int) Math.floor(xy[i][0]);
		    	yefd[i] = (int) Math.floor(xy[i][1]);
		    }
		    PolygonRoi roi2 = new PolygonRoi(xefd, yefd, n, Roi.FREEROI);
		    roi2.setStrokeColor(Color.YELLOW);
		    roi2.setStrokeWidth(5);
		    Overlay overlay = new Overlay(roi2); 
		    ImagePlus edfOverlay = currentImage.duplicate();
		    edfOverlay.setOverlay(overlay); 
		    edfOverlay = edfOverlay.flatten(); 
			if(saveImage) IJ.save(edfOverlay, dirMask.getAbsolutePath()+"/"+baseName+"_edf_overlay.tif");		    
		} 
		
		// Remove the element from the ROI manager
		manager.removeAll(); 
		  		
		// Save the image with the convex hull ROI overlaid
		if(verbatim) IJ.log("Create ROI overlay");
		Roi roiToOverlay = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON); 
	    roiToOverlay.setStrokeColor(Color.blue);
	    roiToOverlay.setStrokeWidth(5);
	    Overlay overlay = new Overlay(roiToOverlay); 
	    currentImage.setOverlay(overlay); 
	    currentImage = currentImage.flatten(); 

	    // Get shape measurements from the convex hull
		if(verbatim) IJ.log("Get measurments");
		globalConvexHull.setCalibration(cal);	
		globalConvexHull.getProcessor().invert();
		Analyzer.setResultsTable(rt);
		rt.reset();
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.CENTER_OF_MASS |
				Measurements.AREA | Measurements.RECT | Measurements.SHAPE_DESCRIPTORS | Measurements.FERET, 
				rt, 0, 10e9);
		pa.analyze(globalConvexHull);
		convexHull = (float) rt.getValue("Area", 0);
		depth = (float) rt.getValue("Height", 0);
		width = (float) rt.getValue("Width", 0);
		circ = (float) rt.getValue("Circ.", 0);
		ar = (float) rt.getValue("AR", 0);
		round = (float) rt.getValue("Round", 0);
		solidity = (float) rt.getValue("Solidity", 0);
		globalFeret = (float) rt.getValue("Feret", 0);
		globalAngle = (float) rt.getValue("FeretAngle", 0);
		comY = (float) rt.getValue("XM", 0);
		comY = (float) rt.getValue("YM", 0);
		
		// Get the shape of the root system
		if(shapeAnalysis){
			coordImage.setCalibration(calDefault);		
			getCoordinates(coordImage, (float) rt.getValue("BY", 0));
		}
		
		float invScale = (float) (scalePix / (scaleCm * scalingFactor));
				
		Roi shapeROI = new Roi((float) rt.getValue("BX", 0) * invScale , (float) rt.getValue("BY", 0) * invScale, 
				width * invScale, depth * invScale);
     	shapeROI.setStrokeColor(Color.blue);
	    shapeROI.setStrokeWidth(5);
	    overlay = new Overlay(shapeROI); 
	    globalMask.setOverlay(overlay); 
	    globalMask = globalMask.flatten(); 
		
		
		// Save the images for post-processing check
		if(saveImage){
		    if(verbatim) IJ.log("Save images");
			IJ.save(globalMask, dirMask.getAbsolutePath()+"/"+baseName+"_mask.tif");
			IJ.save(skelImage, dirMask.getAbsolutePath()+"/"+baseName+"_skell.tif");
			IJ.save(currentImage, dirMask.getAbsolutePath()+"/"+baseName+"_convexhull_overlay.tif");
			IJ.save(globalConvexHull, dirMask.getAbsolutePath()+"/"+baseName+"_convexhull.tif");
		}
		// Send the data to the CSV file
		sendGlobalDataToCSV();

		// Close the images
		globalMask.flush(); globalMask.close();
		globalConvexHull.flush(); globalConvexHull.close();
		globalMask.close();
		
		return nextImage;
	}
	
	
	/**
	 * Get a basic shape of the root system. The shape is the width of the root system at fixed depth interval. 
	 * @param im
	 */
	private void getCoordinates(ImagePlus im, float Ymid){
		
		boolean verbatim = true;
		
		if(verbatim) IJ.log("Get Coordinates");
		// Get bounding box
		im.getProcessor().autoThreshold();
        IJ.run(im, "Create Selection", "");
        
        
        Roi select;
        select = im.getRoi();
        if(select == null){
        	empty = true;
        	return;
        }       
        ImageProcessor Shape = im.getProcessor();
        Shape.setRoi(select.getBounds());
        Shape = Shape.crop();
        im.setProcessor(Shape);
        float w = im.getWidth();
        float h = im.getHeight();
        int m = 2 * nCoord;

        float[] xCoord = new float[nCoord * 2];
        float[] yCoord = new float[nCoord * 2];
        float[] diffCoord = new float[nCoord];
        float[] cumulCoord = new float[nCoord];
        
        //Calculate coordinates
        // Make rectangle (for each rectangle) 
        // Get bounding box of rectangle
		// Get coordinates of bounding box
		// Save coordinates
        
        for(int i = 0; i < nCoord; i++){
        	ImagePlus currentSelection = im.duplicate();
        	float factor = (float) i / (nCoord - 1);
        
        	float y = Ymid;
        	if(i == 0) y = Ymid;
        	else if(i == (nCoord - 1)) y = (float) (0.99 * h);
        	else y = (float) (factor * h);
        	
        	currentSelection.setRoi(new Roi(0, y, w, h/(nCoord*2)));
        	        	
        	ImageProcessor small = currentSelection.getProcessor();
        	small = small.crop();
        	small.setAutoThreshold("Li");
        	currentSelection.setProcessor(small);	
        
	        IJ.run(currentSelection, "Create Selection", "");  
	        ResultsTable rt = new ResultsTable();
	        Analyzer.setResultsTable(rt);
	        rt.reset();
	        Analyzer an = new Analyzer(currentSelection, Measurements.RECT, rt);
	        an.measure();
	         	        
	     	xCoord[i] = (float) rt.getValue("BX", 0);
	     	yCoord[i] = (float) y;
	     	     	
	        int o = m - i - 1;
	        xCoord[o] = (float) (rt.getValue("BX", 0) + rt.getValue("Width", 0));
	     	yCoord[o] = (float) y;  
	     	
	     	sendROIDataToCSV("shape", (select.getBounds().x + xCoord[i]) * (scaleCm/scalePix), (select.getBounds().x + yCoord[i]) * (scaleCm/scalePix));
	     	sendROIDataToCSV("shape", (select.getBounds().x + xCoord[o]) * (scaleCm/scalePix), (select.getBounds().x + yCoord[o]) * (scaleCm/scalePix));
	     	
	     	// Get the width and the cumutl width (inspired from Bucksch et al 2014, Plant Physiology)
	     	diffCoord[i] = Math.abs(xCoord[i] - xCoord[o]) / w;
	     	if(i == 0) cumulCoord[i] = diffCoord[i];
	     	else cumulCoord[i] = cumulCoord[i-1] + diffCoord[i];
        	
        }
        // Make shape
     	PolygonRoi shapeROI = new PolygonRoi(xCoord,yCoord, Roi.FREEROI);
     	shapeROI.setStrokeColor(Color.blue);
	    shapeROI.setStrokeWidth(5);
	    Overlay overlay = new Overlay(shapeROI); 
	    im.setOverlay(overlay); 
	    im = im.flatten(); 
  
	    // Save the images for post-processing check
	    if(saveImage) IJ.save(im, dirMask.getAbsolutePath()+"/"+baseName+"_shape.jpeg");   
	    
	    sendShapeDataToCSV(xCoord, yCoord, diffCoord, cumulCoord);
        if(saveTPS) sendShapeDataToTPS(xCoord, yCoord);
        im.close();
	}


	/**
	 * Process the global image to extract all the usefull information 
	 * @param currentImage the current image in the time serie
	 */
	private void measureDirectionality(ImagePlus current){
		
		int scalingFactor = 3;
		
		// Initalisation of the image
		if(verbatim) IJ.log("Loading the image");
		ImagePlus currentImage;
    	currentImage = current.duplicate();
    	
    	// Pre-process the image
    	if(verbatim) IJ.log("Pre-processing the image");
    	ImageProcessor globalProcessor = currentImage.getProcessor();
    	
		// Crop the borders of the images, often black and making trouble for the threshold detection.
		//if(!wideRhizotron) globalProcessor.setRoi(new Roi(170, 45, globalProcessor.getWidth()-(220+170), globalProcessor.getHeight()-(66+45)));
		//else globalProcessor.setRoi(new Roi(40, 45, globalProcessor.getWidth()-(68+40), globalProcessor.getHeight()-(66+45)));
		//globalProcessor = globalProcessor.crop();    	
    	
    	// Resize the image to speed up the analysis
    	globalProcessor = globalProcessor.resize(globalProcessor.getWidth()/scalingFactor, globalProcessor.getHeight()/scalingFactor);
    	
		// Convert to 8bit image if needed
    	if(globalProcessor.getBitDepth() != 8) globalProcessor = globalProcessor.convertToByte(true);

        // If the root is white on black, then invert the image
    	if(!blackRoots) globalProcessor.invert();

        
		// Threshold the image
        if(verbatim) IJ.log("Thresholding the image");
        
//        // Equalize and normalize the histogram of the image
//		ContrastEnhancer ce = new ContrastEnhancer();
//		ce.setNormalize(true);
//		ce.equalize(globalProcessor);
//		ce.stretchHistogram(globalProcessor, 0.4);		
//        
//        // Threshold the image based on its mean value
//		ImageStatistics istats = globalProcessor.getStatistics();        
//        globalProcessor.threshold((int) istats.mean / 2);
        
        globalProcessor = Util.thresholdImage(globalProcessor, lowContrast, true, rootMinSize/scalingFactor);
           
//		globalProcessor.invert();
		globalProcessor.smooth(); globalProcessor.smooth();
		
		if(verbatim) IJ.log("Cleaning skeleton");
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(globalProcessor, true));
		if(verbatim) IJ.log("Create skeleton");
		bp.skeletonize();	
		if(verbatim) IJ.log("Invert skeleton");
		bp.invert();
		if(verbatim) IJ.log("threshold skeleton");
		bp.threshold(120);
//		bp.invert();
		currentImage.setProcessor(bp);		
		
		
		//currentImage.setProcessor(globalProcessor);		
   
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();
        float hStep = h / hBin;
        float wStep = w / wBin;
        
        int counter = 0;
        Analyzer an;
        ResultsTable rt = new ResultsTable();

        if(verbatim) IJ.log("Measure directionality");
        
        for(int i=0; i < wBin; i++){
        	for(int j=0; j < hBin; j++){
        		
        		counter++;
        		
        		Roi rect = new Roi(i*wStep, j*hStep, wStep, hStep);
        		currentImage.setRoi(rect);
        		ImagePlus currentSelection = currentImage.duplicate();
        		
        		an = new Analyzer(currentSelection, Analyzer.AREA_FRACTION, rt);
        		an.measure();

        		if(rt.getValue("%Area",0) > 5){  		
		        		
		        		/**
		        		 * Modified from the Directionality plugin of Fiji 
		        		 * @author Jean-Yves Tinevez jeanyves.tinevez@gmail.com
		        		 * @version 2.0
		        		 */
		        		
		        		Directionality dnlty = new Directionality();
		        		currentSelection.setProcessor(currentSelection.getProcessor().rotateLeft());
		        		
		        		// Set fields and settings
		        		int binStart = -90;
		        		dnlty.setImagePlus(currentSelection);
		        		if(dirMethod == 0) dnlty.setMethod(Directionality.AnalysisMethod.FOURIER_COMPONENTS);
		        		else dnlty.setMethod(Directionality.AnalysisMethod.LOCAL_GRADIENT_ORIENTATION);
		        		dnlty.setBinNumber(nDirBin);
		        		dnlty.setBinStart(binStart);
		        		dnlty.setBuildOrientationMapFlag(true);
		        		
		        		// Do calculation
		        		dnlty.computeHistograms();
		        		ResultsTable rs = dnlty.displayResultsTable();

		        		for(int k = 0; k < rs.getCounter(); k++){
							this.sendDirectionalDataToCSV(
									(float)(i*wStep*scalingFactor*scale), 
									(float)(j*hStep*scalingFactor*scale), 
									(float) rs.getValueAsDouble(1, k),
									(float) rs.getValueAsDouble(0, k));
		        		}
		        		
						ImagePlus img2 = new ImagePlus("Orientation map", dnlty.getOrientationMap()).flatten();
						img2.setProcessor(img2.getProcessor().rotateRight());
						if(saveImage) IJ.save(img2, dirDir.getAbsolutePath()+"/"+baseName+"_"+counter+"_orientationmap.tif");
        		}
        		rt.reset();
        	}
        }
	}
	
	/**
	 * Process the local image to extract all the usefull information 
	 * @param current the current image in the time serie
	 * @param previous the previous image in the time serie
	 */
	private void measureLocalImage(ImagePlus current, ImagePlus previous, String type){
		
		// Initiate the different ImageJ tools
		ParticleAnalyzer pa;
		ResultsTable rt = new ResultsTable();
		Calibration cal = new Calibration();
    	ImageCalculator ic = new ImageCalculator();	
    	ImagePlus tempCurrent = current.duplicate();
    	ImagePlus tempPrevious = null;
    	if(previous != null) tempPrevious = previous.duplicate();
    	
        if(blackRoots){	
        	tempCurrent.getProcessor().invert();
        	if(previous != null){
        		tempPrevious.getProcessor().invert();
        	}
        }

    	
		float scale = 1/sizeScale;
		// Set the scale		
		cal.setUnit("cm");
		cal.pixelHeight =( scale * scaleCm) / scalePix;
		cal.pixelWidth = ( scale * scaleCm) / scalePix;
    	
		if(verbatim) IJ.log("Loading images");
    	ImagePlus localImage = null;
    	ImageProcessor localProcessor = null;
    	if(previous == null){ 
    		if(verbatim) IJ.log("NO previous images");
    		localImage = tempCurrent.duplicate(); 
    	}
    	else{
    		if(verbatim) IJ.log("Substract previous image");
    		for(int k = 0; k< 1; k++) tempPrevious.getProcessor().dilate();
    		localImage = ic.run("Substract create", tempCurrent.duplicate(), tempPrevious.duplicate());
    	}
    	
    	localProcessor = localImage.getProcessor();
//    	if(blackRoots) localProcessor.invert();
    	
    	localProcessor = localProcessor.resize((int)(localProcessor.getWidth()/scale), (int) (localProcessor.getHeight()/scale));
    	if(localProcessor.getBitDepth() != 8) localProcessor = localProcessor.convertToByte(true);

    	localImage.setProcessor(localProcessor);
    	ImagePlus original = localImage.duplicate();	
 
        if(verbatim) IJ.log("Threshold image");
//        localProcessor = Util.thresholdImage(localProcessor, lowContrast, blackRoots, rootMinSize);

		
		if(verbatim) IJ.log("Cleaning skeleton");
//		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(localProcessor, true));
//		for(int i=0; i < 3; i++) bp.smooth();
//		
//		if(verbatim) IJ.log("Create skeleton");
////		localImage.setProcessor(bp);
////		localImage.show();
//		bp.invert();	
//		bp.skeletonize();	
////		localImage.setProcessor(bp);
////		localImage.show();
////		if(0==0) return;
//		if(verbatim) IJ.log("Invert skeleton");
//
//		bp.invert();
//		if(verbatim) IJ.log("Remove skeleton");
//
//		bp = Util.removeSkeletonConnections2(bp);
//		if(verbatim) IJ.log("Threshold skeleton");
//
//		bp.threshold(120);
////		bp.invert();
//		localImage.setProcessor(bp);
//		localImage.setCalibration(cal);		
//	
//		localImage.show();
//		if(0==0) return;
		
		// 2017 version
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(localProcessor, true));
		for(int i=0; i < 3; i++) bp.smooth();
		bp.skeletonize();	
		bp.invert();
		bp = Util.removeSkeletonConnections2(bp);
		bp.threshold(120);
		bp.invert();
		localImage.setProcessor(bp);
		localImage.setCalibration(cal);		
		
		
		
//		
		// Get the surface
		Analyzer.setResultsTable(rt);
		rt.reset();
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.ADD_TO_MANAGER, 
				Measurements.AREA | Measurements.FERET | Measurements.RECT, rt, (rootMinSize/scale)/10 , 10e9, 0, 1);
		pa.analyze(localImage);
				
		Overlay overlay = new Overlay();		
		RoiManager roi = RoiManager.getInstance();

		
		// Check if the image is empty
		if(roi == null){
			if(verbatim) IJ.log(">>>>> NO ROOT DETECTED. GETTING OUT <<<<<");
			empty = true;
			return;
		}
		
		Roi[] roiA = roi.getRoisAsArray();
	
		roi.removeAll();
		roi.close();
				
		// Get the data of all the particles
		if(verbatim) IJ.log("Get particule data");
		for(int i = 0; i < roiA.length; i++){
		    ImageProcessor ip = localImage.getProcessor(); 
		    ip.setRoi(roiA[i]); 
			length = (float) roiA[i].getFeretValues()[0] * (float) cal.pixelHeight;;
			angle = (float) Math.abs(roiA[i].getFeretValues()[1]);
			System.out.println(angle);
			rx = (float) roiA[i].getBounds().x * (float) cal.pixelHeight;
			ry = (float) roiA[i].getBounds().y * (float) cal.pixelHeight;
			
			// Send the data to the csv file
			sendLocalDataToCSV(0, type);			
			
			Roi roiTemp;
			if((roiA[i].getFeretValues()[1] - 90) > 0){
				roiTemp = new Line(roiA[i].getBounds().x, roiA[i].getBounds().y, roiA[i].getBounds().x + roiA[i].getBounds().width, roiA[i].getBounds().y+roiA[i].getBounds().height);
			}
			else{
				roiTemp = new Line(roiA[i].getBounds().x + roiA[i].getBounds().width, roiA[i].getBounds().y, roiA[i].getBounds().x, roiA[i].getBounds().y+roiA[i].getBounds().height);
			}
			
			// Get the overlay from the ROI manage
//		    overlay.add(roiA[i]);
		    overlay.add(roiTemp);
			
		}
		
		if(verbatim) IJ.log("Save images");
//	    original.getProcessor().invert();
		overlay.setStrokeColor(Color.blue);
	    localImage.setOverlay(overlay); 
	    localImage = localImage.flatten(); 
			    
		if(saveImage) IJ.save(localImage, dirLocal.getAbsolutePath()+"/"+baseName+"_diff_bin_"+type+".tif");
	}

	
	
	/**
	 * Find the best threshold value on an EDM image in order to have only one object remaining.
	 * The idea is to use the EDM image, and threshold at increasing values (starting to 0), until their is only one 
	 * object in the image. This is stopped at a max threshold value to prevent the algorithm to pick up dirts in the image
	 * @param p
	 * @return
	 */
	private int getBestThreshold(ImageProcessor p, boolean low){
		int fact = 1;
		ImagePlus temp = new ImagePlus();
		p = p.resize(p.getWidth()/fact, p.getHeight()/fact);
		ImageProcessor proc;
		ResultsTable rt = new ResultsTable();
		Analyzer.setResultsTable(rt);
		ParticleAnalyzer pa;
		int maxThrsld = 30;
		if(low){
			maxThrsld = 100;
		}
		
		boolean keepGoing = true;
		int thrshld = 1;

		while(keepGoing){
			thrshld += 5; // increment the threshold
			proc = p.duplicate();
			proc.threshold(thrshld);
			proc.invert();
			temp.setProcessor(proc);
			// How many objects are in the image
			rt.reset();
			Analyzer.setResultsTable(rt);
			pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, 0, 10e9, 0, 1);
			pa.analyze(temp);			
			if(rt.getCounter() == 1 || thrshld >= maxThrsld){ // If their is only one object in the image or we passed the max threshold.
				keepGoing = false;			
			}
		}
		
		if(verbatim) IJ.log("Convexhull threshold is "+thrshld);
		
		// Return the best value
		return thrshld;
	}
		
	
	/**
	 * Print Local CSV header
	 */
	private void printROICSVHeader(){	
		pwROI.println("image, type, x, y");			
		pwROI.flush();
	}	
	
	/**
	 * Send local data to an CSV file
	 */
	private void sendROIDataToCSV(String type, double x, double y){	
		pwROI.println(baseName +","+ type +","+ x +","+ y);
		pwROI.flush();
	}
	/**
	 * Print Local CSV header
	 */
	private void printLocalCSVHeader(){	
		pwLocal.println("image, type, length, angle, x, y");			
		pwLocal.flush();
	}
	/**
	 * Send local data to an CSV file
	 */
	private void sendLocalDataToCSV(int id, String type){	
		pwLocal.println(baseName +","+ type +","+ length +","+ angle +","+ rx +","+ ry);
		pwLocal.flush();
	}
	
	/**
	 * Save Shape CSV header
	 */
	private void printShapeCSVHeader(){	
		String toPrint = "image, ";
		for(int i = 0; i < nCoord * 2; i++) toPrint = toPrint.concat("coord_x"+i+",coord_y"+i+",");
		for(int i = 0; i < nCoord; i++) toPrint = toPrint.concat("diff_x"+i+",cumul_x"+i+",");
		toPrint = toPrint.concat("index");
		pwShape.println(toPrint);
		pwShape.flush();
	}
	/**
	 * send Shape Data To CSV
	 */
	private void sendShapeDataToTPS(float[] coordX, float[] coordY){	
		pwTPS.println("ID="+baseName);
		pwTPS.println("LM="+(nCoord*2));
		for(int i = 0; i < coordX.length; i++) pwTPS.println(coordX[i]+" "+coordY[i]);
		pwTPS.flush();
	}
	
	/**
	 * Print Shape CSV header
	 */
	private void sendShapeDataToCSV(float[] coordX, float[] coordY, float[] diff, float[] cumul){	
		String toPrint = baseName;
		for(int i = 0; i < coordX.length; i++) toPrint = toPrint.concat(coordX[i]+","+coordY[i]+",");
		for(int i = 0; i < diff.length; i++) toPrint = toPrint.concat(diff[i]+","+cumul[i]+",");
		toPrint = toPrint.concat("1");
		pwShape.println(toPrint);
		pwShape.flush();
	}
	
	/**
	 * Print EFD CSV header
	 */
	private void printEFDCSVHeader(){	
		pwEFD.println("image, index, ax, ay, bx, by, efd");			
		pwEFD.flush();
	}
	/**
	 * Send EFD data to an CSV file
	 */
	private void sendEFDDataToCSV(int i, double ax, double ay, double bx, double by, double efd){	
		pwEFD.println(baseName +","+ i +","+ ax +","+ ay +","+ bx+","+ by+","+ efd);
		pwEFD.flush();
	}	
	
	/**
	 * Print EFD CSV header
	 */
	private void printDirectionalCSVHeader(){	
		pwDir.println("image,x,y,angle,count");			
		pwDir.flush();
	}
//	/**
//	 * Send EFD data to an CSV file
//	 */
//	private void sendDirectionalDataToCSV(double i, double angle){	
//		pwDir.println(baseName +","+ i +","+angle);
//		pwDir.flush();
//	}	
	
	/**
	 * Send EFD data to an CSV file
	 */
	private void sendDirectionalDataToCSV(float w, float h, double count, double angle){	
		pwDir.println(baseName +","+ w +","+ h +","+angle+","+count);
		pwDir.flush();
	}		
	
//	/**
//	 * Send EFD data to an CSV file
//	 */
//	private void sendDirectionalDataToCSV(float w, float h, float angle){	
//		pwDir.println(baseName +","+ w +","+ h +","+angle);
//		pwDir.flush();
//	}			
	/**
	 * Print Global CSV header
	 */
	private void printGlobalCSVHeader(){	
		pwGlobal.println("image,area,lenght,convexhull,depth,width,circularity,ar,round,solidity,feret,feret_angle,massX,massY");			
		pwGlobal.flush();
	}
	
	/**
	 * Send global data to an CSV file
	 */
	private void sendGlobalDataToCSV(){	
		pwGlobal.println(baseName +","+ area +","+ totlength +","+ convexHull+","+ depth+","+ width+","+ circ+","+ ar+","+ round+","+ solidity+","+ globalFeret+","+ globalAngle+","+ comX +","+ comY);
		pwGlobal.flush();
	}	
	
	
	   /**
	    * Compute the image skeleton and find its tips. 
	    * Theses tips will be used as starting point to create new roots
	    * Not used right now
	    */
	   public int[][] getSkeletonTips(ImagePlus imp){
		   
		   ImageProcessor ip = imp.getProcessor().duplicate();
		   ip.autoThreshold();
		   BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(ip, true));
		   bp.skeletonize();	   
		   bp.invert();
		   ImagePlus im1 = new ImagePlus(); im1.setProcessor(bp);
		   ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS, 0, new ResultsTable(), 100, 10e9, 0, 1);
		   pa.analyze(im1);
		   ImagePlus globalMask = IJ.getImage(); 
		   globalMask.hide();
		   bp = new BinaryProcessor(new ByteProcessor(globalMask.duplicate().getProcessor(), true));
		   
		   ArrayList<Integer> x = new ArrayList<Integer>();
		   ArrayList<Integer> y = new ArrayList<Integer>();
		   for(int w = 0; w < bp.getWidth(); w++){
			   for(int h = 0; h < bp.getHeight(); h++){			   
				   if(bp.get(w, h) > 125){
					   int n = nNeighbours(bp, w, h);
					   if(n == 1){
						   x.add(w);
						   y.add(h);
					   }
				   }
			   }   
		   }
		   
		   int[][] coord = new int[x.size()][2];
		   for(int i = 0; i < x.size(); i++){
			   coord[i][0] = x.get(i);
			   coord[i][1] = y.get(i);
		   }
		   if(verbatim) IJ.log(coord.length+"");
		   return coord;
		   
	   }
	   
	   /**
	    * Compute the number of black neigbours for a point
	    * @param bp
	    * @param w
	    * @param h
	    * @return
	    */
	   private int nNeighbours(ImageProcessor bp, int w, int h){
		   int n = 0;
		   for(int i = w-1; i <= w+1; i++){
			   for(int j = h-1; j <= h+1; j++){
				   if(bp.getPixel(i, j) > 125) n++;
				   if(n == 3) return n-1;
			   }
		   }
		   return n-1;
	   }	
	
	
	
   /**
    * Image filter	
    * @author guillaumelobet
    */
	public class ImageFilter extends javax.swing.filechooser.FileFilter{
		public boolean accept (File f) {
			if (f.isDirectory()) {
				return true;
			}

			String extension = getExtension(f);
			if (extension != null) {
				if (extension.equals("jpg") || extension.equals("png") ||
						extension.equals("tif") || extension.equals("tiff") || extension.equals("jpeg")) return true;
				else return false;
			}
			return false;
		}
	     
		public String getDescription () {
			return "Image files (*.jpg, *png, *tiff)";
		}
	      
		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i+1).toLowerCase();
			}
			return ext;
		}
	}	
	
}
