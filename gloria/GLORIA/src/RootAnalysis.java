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
import ij.process.ImageStatistics;
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
	static String  csvGlobalFolder, csvLocalFolder, csvDirFolder, csvEFDFolder, imName, baseName;
	
	// Image paramaters
	static String experiment, stock, treatment, pot, plantID; 
	
	// Analysis parameters
	static long DAS;
	static int nROI, nTimeSerie, dirMethod, nDirBin, hBin, wBin, nEFD;
	static double scalePix, scaleCm, scale, rootMinSize;
	static boolean blackRoots, globalAnalysis, localAnalysis, efdAnalysis, isTimeSerie, 
		manualCorrection, lowContrast, directionalAnalysis, wideRhizotron;
	
	static float angle, length, diameter, feret, tortuosity, area, convexHull, rx, ry,
		depth, width, ax, ay, bx, by, efd, circ, ar, round, solidity, globalFeret, globalAngle, comY, comX;
			
	static PrintWriter pwGlobal, pwLocal, pwEFD, pwDir;	
	
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
			int nefd,
			int ndb,
			boolean mc,
			int wbin,
			int hbin,
			boolean ts,
			boolean lc,
			boolean rw){
		
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
		
		// Files to save the different data		
		csvGlobalFolder = file.substring(0, file.length()-4)+"-global.csv";
		csvLocalFolder = file.substring(0, file.length()-4)+"-local.csv";
		csvEFDFolder = file.substring(0, file.length()-4)+"-efd.csv";
		csvDirFolder = file.substring(0, file.length()-4)+"-dir.csv";
				
		scale = scaleCm / scalePix;
		
		// Analyze the plants
		analyze();
	}

	
	/**
	 * Perform the analysis of all the images
	 */
	public void analyze(){
		
		ImagePlus nextImage = null, previousImage = null;
		
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


		IJ.log("Root image analysis started: "+dirAll.getAbsolutePath().toString());
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
		if(directionalAnalysis){
			pwDir = Util.initializeCSV(csvDirFolder);
			printDirectionalCSVHeader();
		}		
		if(efdAnalysis){
			pwEFD = Util.initializeCSV(csvEFDFolder);
			printEFDCSVHeader();
		}		
		
			
		// Navigate the different images in the time serie
		for(int i = 0; i < images.length; i++){
			
			// Open the image
			nextImage = IJ.openImage(images[i].getAbsolutePath());
			IJ.log("------------------------");
			IJ.log("Analysis of image "+images[i]+ " started.");
			
			// If it is a time serie, load the previous image
			if(isTimeSerie){
				try{ if(i > 0){ previousImage = IJ.openImage(images[i-1].getAbsolutePath()); }}
				catch(Exception e){ IJ.log("Error in local analysis: "+e); }
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
			
			// Create the folder structure to store the images
			File dirSave; 
			dirSave = Util.createFolderStructure(dirAll.getAbsolutePath(), globalAnalysis, localAnalysis, directionalAnalysis);
			dirMask = new File(dirSave.getAbsolutePath()+"/global/");
			dirLocal = new File(dirSave.getAbsolutePath()+"/local/");
			dirDir = new File(dirSave.getAbsolutePath()+"/dir/");

			
			// Measure the image
			
			if(localAnalysis){
				IJ.log("------------------------");
				IJ.log("Starting local analysis");
				measureLocalImage(nextImage, previousImage);
			}
			
			if(globalAnalysis){
				IJ.log("------------------------");
				IJ.log("Starting global analysis");
				measureGlobalImage(nextImage);
			}
			if(directionalAnalysis){
				IJ.log("------------------------");
				IJ.log("Starting directional analysis");
				measureDirectionality(nextImage);
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
		int scalingFactor = 2;
		cal.setUnit("cm");
		cal.pixelHeight =( scalingFactor * scaleCm) / scalePix;
		cal.pixelWidth = ( scalingFactor * scaleCm) / scalePix;
		
		// Reset Scale
		calDefault.setUnit("px");
		calDefault.pixelHeight = 1;
		calDefault.pixelWidth = 1;
		
		// Initalisation of the image
		IJ.log("Loading images");
    	ImagePlus currentImage = current.duplicate();

    	// Keep a copy of the current image for the next run
    	nextImage = currentImage.duplicate();

    	
    	// Pre-process the image
    	IJ.log("Pre-processing the image");
    	ImageProcessor globalProcessor = currentImage.getProcessor();
    	
		// Crop the borders of the images, often black and making trouble for the threshold detection.
		if(!wideRhizotron) globalProcessor.setRoi(new Roi(170, 45, globalProcessor.getWidth()-(220+170), globalProcessor.getHeight()-(66+45)));
		else globalProcessor.setRoi(new Roi(40, 45, globalProcessor.getWidth()-(68+40), globalProcessor.getHeight()-(66+45)));
		globalProcessor = globalProcessor.crop();
    	
    	// Resize the image to speed up the analysis
    	globalProcessor = globalProcessor.resize(globalProcessor.getWidth()/scalingFactor, globalProcessor.getHeight()/scalingFactor);
		
    	// Convert to 8bit image if needed
    	if(globalProcessor.getBitDepth() != 8) globalProcessor = globalProcessor.convertToByte(true);
        
		// If the the image is horizontal, rotate it
		if(globalProcessor.getWidth() > globalProcessor.getHeight()){
			globalProcessor.rotateLeft();
		}
		

    	// If the root is white on black, then invert the image
    	if(!blackRoots) globalProcessor.invert(); 
        currentImage.setProcessor(globalProcessor);

//        
//        // Get the image start to detect the presence of low contrast images or a very large root system
//        IJ.log("Getting Image statistics");
//        ImageStatistics istats;
//        istats = currentImage.getStatistics();        
//        float diff = (((float)istats.pixelCount - (float)istats.maxCount) / (float)istats.pixelCount);
//        if(diff > 0.1){
//        	largeRoot = true;
//        	IJ.log("Large root detected");
//        }
        
        
		// Threshold the image
        IJ.log("Thresholding the image");
        
//        // Equalize and normalize the histogram of the image
//		ContrastEnhancer ce = new ContrastEnhancer();
//		ce.setNormalize(true);
//		ce.equalize(globalProcessor);
//		ce.stretchHistogram(globalProcessor, 0.4);		
//        
//        // Threshold the image based on its mean value
//		ImageStatistics istats = globalProcessor.getStatistics();        
//        globalProcessor.threshold((int) istats.mean / 2);
//		globalProcessor.invert();
//        
//		
//		// Clean the image
//		BinaryProcessor bp1 = new BinaryProcessor(new ByteProcessor(globalProcessor, true));
//		bp1.invert();		
//		if(!lowContrast) bp1 = Util.cleanImage(bp1);
//		bp1.threshold(120);
//		bp1.invert();
		
//        globalProcessor = 				
//		currentImage.setProcessor(globalProcessor);
		      		
//        // Clean the image by removing the small particules. Might be redundant with the previous operation...
//        IJ.log("Cleaning the image");
//		pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS, Measurements.AREA, rt, rootMinSize/scalingFactor, 10e9, 0, 1);
//		pa.analyze(currentImage);
//		
//		// Get the mask from the ParticuleAnalyser
//		ImagePlus globalMask = IJ.getImage(); 
//		globalMask.hide(); // Hide the mask, we do not want to display it.
//		ImageProcessor globalMaskProcessor = globalMask.getProcessor();

		ImageProcessor globalMaskProcessor = Util.thresholdImage(globalProcessor.duplicate(), lowContrast, true, rootMinSize/scalingFactor);
		globalMaskProcessor.invert();
		ImagePlus globalMask = new ImagePlus();
		globalMask.setProcessor(globalMaskProcessor);
		
		currentImage = globalMask.duplicate();

//		globalMask.show(); if(1==1) return null;
		
		// TOTAL SURFACE
		// Get the surface
		IJ.save(globalMask.duplicate(), dirMask.getAbsolutePath()+"/"+baseName+"_area.tif"); // Save the image used for the area computation
		
//		IJ.log("Getting the total surface of root system");
//		rt.reset();
//		Analyzer.setResultsTable(rt);
//		globalMask.setCalibration(cal);
//		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, rootMinSize/scalingFactor, 10e9, 0, 1);
//		pa.analyze(globalMask);
//
//		// Get the total surface of all the particles (the different parts of the root system)
//		area = 0;
//		for(int i = 0; i < rt.getCounter(); i++){
//			area += (float) rt.getValue("Area", i);			
//		}
		
		// CONVEX HULL
		// Compute the distance map. This step is made to re-costruct the root system based on its different
		// particles.
		IJ.log("Computing EDM mask");
		globalMaskProcessor.invert();
		edm.run(globalMaskProcessor);		
		
        // Find the best Threshold and apply it
		IJ.log("Computing best threshold");
		int tr = getBestThreshold(globalMaskProcessor.duplicate(), lowContrast || largeRoot);
		globalMaskProcessor.threshold(tr);
		
		// Erode the mask to reduce the effect of the extra size of the EDM threshold 
		IJ.log("Finding convex hull mask");
		globalMaskProcessor.invert();
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(globalMaskProcessor, true));		
		for(int i=0; i < tr/2; i++) bp.erode();	
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
		IJ.log("Find max object");
		int index = 0;
		double max = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			if(rt.getValue("Area", i) > max){
				max = rt.getValue("Area", i);
				index = i;
			};			
		}
		
		
		// Get the convex hull from the ROI manager (the largest object)
		RoiManager manager = RoiManager.getInstance();
		Roi[] roiA = manager.getRoisAsArray();
		Roi convexROI = roiA[index];
		
		if(manualCorrection){
			IJ.log("Correcting the ROI");
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
		
		IJ.log("Getting the total surface of root system");
		rt.reset();
		Analyzer.setResultsTable(rt);
		currentImage.getProcessor().fillOutside(convexROI);		
		currentImage.setCalibration(cal);
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, rootMinSize/scalingFactor, 10e9, 0, 1);
		pa.analyze(currentImage);

		// Get the total surface of all the particles (the different parts of the root system)
		area = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			area += (float) rt.getValue("Area", i);			
		}
		
		
		// Create a mask with the convexhull
		IJ.log("Creating convexhull mask");
		globalMaskProcessor = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON).getMask(); 			
		ImagePlus globalConvexHull = new ImagePlus();
		globalConvexHull.setProcessor(globalMaskProcessor);
		globalConvexHull.setCalibration(cal);
		globalMaskProcessor.autoThreshold();	
		
		// Compute the Fourrier Descriptors for the ROI.
		// The number of descriptors is set by the users (nEFD)
		// This part uses the EllipticFD plugin from Thomas Boudier and Ben Tupper (EFD)
		if(efdAnalysis){
			IJ.log("EFD analysis");
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
			IJ.save(edfOverlay, dirMask.getAbsolutePath()+"/"+baseName+"_edf_overlay.tif");		    
		} 
		
		// Remove the element from the ROI manager
		manager.removeAll(); 
		  		
		// Save the image with the convex hull ROI overlaid
		IJ.log("Create ROI overlay");
		Roi roiToOverlay = new PolygonRoi(convexROI.getConvexHull(), Roi.POLYGON); 
	    roiToOverlay.setStrokeColor(Color.blue);
	    roiToOverlay.setStrokeWidth(5);
	    Overlay overlay = new Overlay(roiToOverlay); 
	    currentImage.setOverlay(overlay); 
	    currentImage = currentImage.flatten(); 

	    // Get shape measurements from the convex hull
		IJ.log("Get measurments");
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
		
		
		// Save the images for post-processing check
		IJ.log("Save images");
		IJ.save(globalMask, dirMask.getAbsolutePath()+"/"+baseName+"_mask.tif");
		IJ.save(currentImage, dirMask.getAbsolutePath()+"/"+baseName+"_convexhull_overlay.tif");
		IJ.save(globalConvexHull, dirMask.getAbsolutePath()+"/"+baseName+"_convexhull.tif");
		
		// Send the data to the CSV file
		sendGlobalDataToCSV();

		// Close the images
		globalMask.flush(); globalMask.close();
		globalConvexHull.flush(); globalConvexHull.close();
		globalMask.close();
		
		return nextImage;
	}
	

	/**
	 * Process the global image to extract all the usefull information 
	 * @param currentImage the current image in the time serie
	 */
	private void measureDirectionality(ImagePlus current){
		
		int scalingFactor = 3;
		
		// Initalisation of the image
		IJ.log("Loading the image");
		ImagePlus currentImage;
    	currentImage = current.duplicate();
    	
    	// Pre-process the image
    	IJ.log("Pre-processing the image");
    	ImageProcessor globalProcessor = currentImage.getProcessor();
    	
		// Crop the borders of the images, often black and making trouble for the threshold detection.
		if(!wideRhizotron) globalProcessor.setRoi(new Roi(170, 45, globalProcessor.getWidth()-(220+170), globalProcessor.getHeight()-(66+45)));
		else globalProcessor.setRoi(new Roi(40, 45, globalProcessor.getWidth()-(68+40), globalProcessor.getHeight()-(66+45)));
		globalProcessor = globalProcessor.crop();    	
    	
    	// Resize the image to speed up the analysis
    	globalProcessor = globalProcessor.resize(globalProcessor.getWidth()/scalingFactor, globalProcessor.getHeight()/scalingFactor);
    	
		// Convert to 8bit image if needed
    	if(globalProcessor.getBitDepth() != 8) globalProcessor = globalProcessor.convertToByte(true);

        // If the root is white on black, then invert the image
    	if(!blackRoots) globalProcessor.invert();

        
		// Threshold the image
        IJ.log("Thresholding the image");
        
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
		currentImage.setProcessor(globalProcessor);		
   
        int w = currentImage.getWidth();
        int h = currentImage.getHeight();
        float hStep = h / hBin;
        float wStep = w / wBin;
        
        int counter = 0;
        Analyzer an;
        ResultsTable rt = new ResultsTable();

        IJ.log("Measure directionality");
        
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
						IJ.save(img2, dirDir.getAbsolutePath()+"/"+baseName+"_"+counter+"_orientationmap.tif");
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
	private void measureLocalImage(ImagePlus current, ImagePlus previous){
		
		// Initiate the different ImageJ tools
		ParticleAnalyzer pa;
		ResultsTable rt = new ResultsTable();
		Calibration cal = new Calibration();
    	ImageCalculator ic = new ImageCalculator();	
		
		int scale = 2;
		// Set the scale		
		cal.setUnit("cm");
		cal.pixelHeight =( scale * scaleCm) / scalePix;
		cal.pixelWidth = ( scale * scaleCm) / scalePix;
    	
		IJ.log("Loading images");
    	ImagePlus localImage = null;
    	ImageProcessor localProcessor = null;
    	if(previous == null){ localImage = current.duplicate(); }
    	else{
    		IJ.log("Substract previous image");
    		localImage = ic.run("Substract create", current.duplicate(), previous.duplicate()); 
    	}

    	localProcessor = localImage.getProcessor();
    	
		// Crop the borders of the images, often black and making trouble for the threshold detection.
		if(!wideRhizotron) localProcessor.setRoi(new Roi(170, 45, localProcessor.getWidth()-(220+170), localProcessor.getHeight()-(66+45)));
		else localProcessor.setRoi(new Roi(40, 45, localProcessor.getWidth()-(68+40), localProcessor.getHeight()-(66+45)));
		localProcessor = localProcessor.crop();
    	
    	localProcessor = localProcessor.resize(localProcessor.getWidth()/scale, localProcessor.getHeight()/scale);
    	if(localProcessor.getBitDepth() != 8) localProcessor = localProcessor.convertToByte(true);

    	localImage.setProcessor(localProcessor);
    	ImagePlus original = localImage.duplicate();	
        if(blackRoots) localProcessor.invert();
 
        // Get the image stats to detect the presence of low contrast images.
//        IJ.log("Get image statistics");
//        ImageStatistics istats;
//        istats = localImage.getStatistics();        
//        if((istats.max-istats.mode) < 10){lowContrast = true;}
        
        IJ.log("Threshold image");
     
//        // Equalize and normalize the histogram of the image
//		ContrastEnhancer ce = new ContrastEnhancer();
//		ce.setNormalize(true);
//		ce.equalize(localProcessor);
//		ce.stretchHistogram(localProcessor, 0.4);		
//        
//        // Threshold the image based on its mean value
//		istats = localProcessor.getStatistics();        
//		localProcessor.threshold((int) istats.mean / 2);
//		localProcessor.invert();
//        
//		// To find the segments, we use a broken skeleton
//		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(localProcessor, true));
//		bp.invert();		
//		bp = Util.cleanImage(bp);
//		
		
		localProcessor = Util.thresholdImage(localProcessor, lowContrast, false, (rootMinSize/scale));
//		localProcessor.invert();

		IJ.log("Cleaning skeleton");
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(localProcessor, true));
		bp.skeletonize();	
		bp.invert();
		bp = Util.removeSkeletonConnections2(bp);
		bp.threshold(120);
		bp.invert();
		localImage.setProcessor(bp);
//		localImage.show(); if(1==1) return ;
		localImage.setCalibration(cal);		
	
		// Get the surface
		//localImage.setCalibration(cal);
		Analyzer.setResultsTable(rt);
		rt.reset();
		pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.ADD_TO_MANAGER, 
//				Measurements.AREA | Measurements.FERET | Measurements.RECT, rt, 0 , 10e9, 0, 1);
				Measurements.AREA | Measurements.FERET | Measurements.RECT, rt, (rootMinSize/scale)/1 , 10e9, 0, 1);
		pa.analyze(localImage);
				
		Overlay overlay = new Overlay();
//		if(manualCorrection){
//			IJ.log("Manual correction");
//			ImagePlus ip = localImage.duplicate();
//			ip.show();
//			ContrastEnhancer ce1 = new ContrastEnhancer();
//			ce1.equalize(ip);
//			new WaitForUserDialog("Correct ROI", baseName+"\n Please correct the ROI by dragging the nodes.\n\n When done, click OK to validate").show();		
//			ip.hide(); ip.close(); ip.flush();
//		}		
		
		RoiManager roi = RoiManager.getInstance();
		Roi[] roiA = roi.getRoisAsArray();
		roi.removeAll();
		
		// Get the data of all the particles
		IJ.log("Get particule data");
		for(int i = 0; i < roiA.length; i++){
		    ImageProcessor ip = localImage.getProcessor(); 
		    ip.setRoi(roiA[i]); 
			length = (float) roiA[i].getFeretValues()[0];
			angle = (float) Math.abs(roiA[i].getFeretValues()[1] - 90);
			rx = (float) roiA[i].getBounds().x * (float) cal.pixelHeight;
			ry = (float) roiA[i].getBounds().y * (float) cal.pixelHeight;
			
			//tortuosity = length / feret;
			// Send the data to the csv file
			sendLocalDataToCSV(0);			
			
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
		
		IJ.log("Save images");
//	    original.getProcessor().invert();
	    localImage.setOverlay(overlay); 
	    localImage = localImage.flatten(); 
			    
		IJ.save(localImage, dirLocal.getAbsolutePath()+"/"+baseName+"_diff_bin.tif");
	}

	
	
	/**
	 * Find the best threshold value on an EDM image in order to have only one object remaining.
	 * The idea is to use the EDM image, and threshold at increasing values (starting to 0), until their is only one 
	 * object in the image. This is stopped at a max threshold value to prevent the algorithm to pick up dirts in the image
	 * @param p
	 * @return
	 */
	private int getBestThreshold(ImageProcessor p, boolean low){
		int fact = 2;
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
			proc = p.duplicate();
			proc.threshold(thrshld);
			temp.setProcessor(proc);
			// How many objects are in the image
			rt.reset();
			Analyzer.setResultsTable(rt);
			pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, Measurements.AREA, rt, 0, 10e9, 0, 1);
			pa.analyze(temp);
			if(rt.getCounter() == 1 || thrshld >= maxThrsld){ // If their is only one object in the image or we passed the max threshold.
				keepGoing = false;			
			}
			thrshld += 2; // increment the threshold
		}
		
		// Return the best value
		return thrshld;
	}
		
	
	
	/**
	 * Print Local CSV header
	 */
	private void printLocalCSVHeader(){	
		pwLocal.println("image, length, angle, x, y");			
		pwLocal.flush();
	}
	/**
	 * Send local data to an CSV file
	 */
	private void sendLocalDataToCSV(int id){	
		pwLocal.println(baseName +","+ length +","+ angle +","+ rx +","+ ry);
		pwLocal.flush();
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
		pwDir.println("image, x, y, angle, count");			
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
		pwGlobal.println("image, area, convexhull, depth, width, circularity, ar, round, solidity, feret, feret_angle, massX, massY");			
		pwGlobal.flush();
	}
	
	/**
	 * Send global data to an CSV file
	 */
	private void sendGlobalDataToCSV(){	
		pwGlobal.println(baseName +","+ area +","+ convexHull+","+ depth+","+ width+","+ circ+","+ ar+","+ round+","+ solidity+","+ globalFeret+","+ globalAngle+","+ comX +","+ comY);
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
		   IJ.log(coord.length+"");
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
