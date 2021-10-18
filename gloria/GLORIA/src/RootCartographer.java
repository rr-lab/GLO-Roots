/**
* @author Guillaume Lobet | Universite de Liege
* @date: 2014-11-16
* 
* The RootCartographer is the class managing the local analysis of dual reporter images.
* It is the one tacking care of creating the data structure (RootSystem) and add the object
* (Reporter and Segment) to it.
* 
*
**/

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;


public class RootCartographer {
	
	// Export parameters
	static File dirAll, dirOriginal;	
	static File[] images; 		
	static String  csvApex, csvSeg,imName, baseName, directory;
	
	int nTimeSerie = 0;
	int time = 0;
	int counter = 0, attachReporterTo;
	boolean isReporter, usePreviousRSML, useAllSegments, rsmlExists, segOnly, attachReporter, lowContrast, hasSoilImage, soilIsLoaded;
	ImagePlus segChannel = null;
	ImagePlus soilChannel = null;
	ImagePlus segChannelPrevious = null;
	ImagePlus reporterChannel = null;
	
	// Calibration
	Calibration calDefault;
	
	// Analysis parameters
	static double scalePix, scaleCm, scale;
	RootSystem rs;
	ArrayList<Reporter> previousRepList = null;
	Interface glori;
	RCImageWindow repImw, segImw;
	PrintWriter pwReporter, pwSeg;
	ContrastEnhancer ce = new ContrastEnhancer();

	/**
	 * Class builder
	 * @param f the folder containing the images
	 * @param file the name of the csv file where to save the data
	 * @param scaleP the scale in pixels
	 * @param scaleC the scale in centimeters
	 * @param usePrev if true, then use previous information from RSML files
	 * @param useAll if true use information about all segments, not only the ones attached
	 * @param seg the time serie contains only structrue (Segment) images
	 * @param nts length of the time-series
	 * @param gl the interface
	 */
	public RootCartographer(File f,
			String file,
			float scaleP,
			float scaleC,
			boolean usePrev,
			boolean useAll,
			boolean seg,
			boolean soil,
			int nts,
			boolean attach,
			int attachTo,
			boolean low,
			Interface gl){
		
		// Set up the different variables
		scalePix = scaleP;
		scaleCm = scaleC;
		dirAll = f;
		csvApex = file.substring(0, file.lastIndexOf("."))+"-reporter.csv";
		csvSeg = file.substring(0, file.lastIndexOf("."))+"-seg.csv";
		scale = scalePix / scaleCm;
		glori = gl;
		usePreviousRSML = usePrev;
		useAllSegments = useAll;
		nTimeSerie = nts;
		rsmlExists = false;
		segOnly = seg;
		attachReporter = attach;
		attachReporterTo = attachTo;
		lowContrast = low;
		hasSoilImage = soil;
		
		soilIsLoaded = false;
		
		// Calibration
		calDefault = new Calibration();
		calDefault.setUnit("px");
		calDefault.pixelHeight = 1;
		calDefault.pixelWidth = 1;
		
		// Analyze the plants
		time = 1;
		counter = 0;
		isReporter = !segOnly;
		
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
		
		// Initialize the csv files
		pwSeg = null; 
		try{pwSeg = new PrintWriter(new FileWriter(csvSeg));}
		catch(IOException e){IJ.log("Could not save file "+csvSeg);}
		
		pwReporter = null; 
		try{pwReporter = new PrintWriter(new FileWriter(csvApex));}
		catch(IOException e){IJ.log("Could not save file "+csvApex);}
		
		initializeCSV();
		
		// Run the analysis
		analyze();
		
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	private ImagePlus openImage(String path){
		// Set the reporter image
		ImagePlus im = IJ.openImage(path);
		im.setCalibration(calDefault);

		// Check the image bit depth
		ImageProcessor ip = im.getProcessor();
		if(ip.getBitDepth() != 8) ip = ip.convertToByte(true);
		im.setProcessor(ip);
				
		// If the the image is horizontal, rotate it
		if(im.getWidth() > im.getHeight()){
			im.setProcessor(im.getProcessor().rotateLeft());
		}
		
		// Crop the borders of the images, often black and making trouble for the threshold detection.
		Roi roi = new Roi(70, 70, im.getWidth()-140, im.getHeight()-140);
		im.setRoi(roi);
		return im.duplicate();
	}

	/**
	 * Analyze the next image in the time-series
	 * @return true is done with success
	 */
	public boolean analyze(){


		// If there is a root system existing, save it and reset it
		if(rs != null && isReporter){
			rsmlExists = false;
			FileInfo fileInfo = reporterChannel.getOriginalFileInfo();
			rs.saveToRSML(directory + "/" + fileInfo.fileName);
			rs = null;
		}
		
		// If only segment analysis, save the image and close it
		if(rs != null && segOnly){
			FileInfo fileInfo = segChannel.getOriginalFileInfo();
			rs.saveToRSML(directory + "/" + fileInfo.fileName);
			rs = null;
			
			// If there is an instance of imw, close it
			if(repImw != null) repImw.close();
			if(segImw != null) segImw.close();
			if(soilChannel != null) soilChannel.close();			
		}
		
		// If this is the end of the analysis
		if(counter == images.length){
			// If there is an instance of imw, close it
			if(repImw != null) repImw.close();
			if(!hasSoilImage) if(segImw != null) segImw.close();
			if(soilChannel != null) soilChannel.close();
			return false;
		}
		
		
		
		// if reach end of a time serie
		if(time > nTimeSerie){
			time = 1;
			segChannelPrevious = null;
			previousRepList = null;
		}

		
		baseName = images[counter].getName();
		IJ.log("------------------------------------");
		IJ.log("Analysis of "+baseName+" started... ");
		RCImageCanvas ric;
		
		if(hasSoilImage && !soilIsLoaded){
			soilChannel = openImage(images[counter].getAbsolutePath());
			counter ++;
			ImageProcessor soilProcessor = soilChannel.getProcessor();
	    	if(soilProcessor.getBitDepth() != 8) soilProcessor = soilProcessor.convertToByte(true);
	    	soilChannel.setProcessor(soilProcessor);
	    	soilIsLoaded = true;
		}
		
		// Process the reporters	
		if(isReporter){

			// If there is an instance of imw, close it
			if(repImw != null) repImw.close();
			if(segImw != null) segImw.close();
			
			// Create the root system
			rs = new RootSystem(images[counter].getName(), useAllSegments, time, scale, attachReporterTo, attachReporter);
			
			// If required use the rsml file (if it exists) to create the root system.
			if(usePreviousRSML){   
				String imName = images[counter].getName();
				String dataFName = images[counter].getParent() + File.separator + imName.substring(0, imName.lastIndexOf('.')) + ".rsml";
				if ((new File(dataFName)).exists()) {
					rs.log("LOADING RSML...", true);
					rs.readRSML(dataFName);    	 
					rsmlExists = true;
				}
			}
			
			// Set the different channels
			reporterChannel = openImage(images[counter].getAbsolutePath());
			directory = images[counter].getParent();
			
			segChannel = openImage(images[counter+1].getAbsolutePath());
			
			rs.setChannel(segChannel, Util.STRUCTURE);
			rs.setChannel(reporterChannel, Util.REPORTER);
			rs.setReporterImage(isReporter);

			if(previousRepList != null){
				IJ.log("prev rep 2 = "+previousRepList.size());
				rs.repListToAttach = previousRepList;
			}
		
			// Do the computations
			if(!usePreviousRSML || !rsmlExists){
				processReporter(reporterChannel, time);
				if(attachReporter){
					rs.attachReporters(false);
				}	
			}
			rs.GLORWin.setCurrentRootModel(rs);

			if(hasSoilImage){
				rs.setChannel(soilChannel, Util.SOIL);
				rs.processSoil(Util.REPORTER);
			}
			
			// Display the apex on the original image
			ce.equalize(reporterChannel);
			ric = new RCImageCanvas(reporterChannel, rs);
	        repImw = new RCImageWindow(reporterChannel, ric);
	        rs.setImageCanvas(ric);
	        ric.setImageWindow(repImw);
	        WindowManager.setWindow(repImw);	

	        // Save the data
			rs.exportReportersToCSV(pwReporter);
			
			// Set the reporter list to attach
			if(attachReporter){
				if(attachReporterTo == Util.ATTACH_PREVIOUS){
					if(previousRepList != null) rs.repListToAttach = previousRepList;
					previousRepList = rs.repList;						
				}
				if(attachReporterTo == Util.ATTACH_CURRENT){
					rs.repListToAttach = rs.repList;
				}
			}

		}
		
		// Process the segments
		else{
			
			// If the root system was not initialized
			if(rs == null){
				rs = new RootSystem(images[counter].getName(), useAllSegments, time, scale, attachReporterTo, attachReporter);
				// Set the different channels
				segChannel = openImage(images[counter].getAbsolutePath());
				
				rs.setChannel(segChannel, Util.STRUCTURE);
				rs.setChannel(reporterChannel, Util.REPORTER);
			}
			rs.setReporterImage(isReporter);


			// Do the computation
			if(!usePreviousRSML || !rsmlExists){
				processSegment(segChannel, time);
				if(attachReporter){
					rs.attachReporters(true);
				}	
			}
			rs.GLORWin.setCurrentRootModel(rs);
			
			if(hasSoilImage){
				if(rs.imSoil == null) rs.setChannel(soilChannel, Util.SOIL);
				rs.processSoil(Util.STRUCTURE);
			}
			
			// Display the apex on the original image
			ce.equalize(segChannel);
			ric = new RCImageCanvas(segChannel, rs);
	        segImw = new RCImageWindow(segChannel, ric);
	        rs.setImageCanvas(ric);
	        ric.setImageWindow(segImw);
	        WindowManager.setWindow(segImw);	
			
			rs.exportSegmentsToCSV(pwSeg);

			segChannelPrevious = segChannel;
			soilIsLoaded = false;

		}
				
		// Increment the different counters
		counter ++;
		if(!isReporter) time ++;		
		if(!segOnly) isReporter = !isReporter;
				
		return true;
	}

	
	/**
	 * Process the image to find the reporters' expression
	 * @param im the image to analyze
	 * @param time the time in series
	 */
	public void processReporter(ImagePlus im, int time){

		IJ.log("Process Reporter data...");
		
		// Process the apexes (threshold them out)
		ImagePlus imRep = im.duplicate();
		ImagePlus imOriginal = im.duplicate();
		ImageProcessor ipRep = imRep.getProcessor();
    	if(ipRep.getBitDepth() != 8) ipRep = ipRep.convertToByte(true);
   	
        // Equalize and normalize the histogram of the image
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.equalize(ipRep);
		ce.stretchHistogram(ipRep, 0.4);		

//		imRep.setProcessor(ipRep);
//        imRep.show();
//        if(1==1) return; 

        // Threshold the image based on its mean value
		if(!lowContrast) ipRep.threshold(200);
		else ipRep.threshold(240);
        
        
		// To find the segments, we use a broken skeleton
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(ipRep, true));
		bp.invert();		
		bp = Util.cleanImage(bp);
		bp.threshold(120);
		bp.invert(); // TO COMMENT			
        imRep.setProcessor(bp);
        

        
        // Get the apex in the image
        ResultsTable rt = new ResultsTable();
        ResultsTable rt2 = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.ADD_TO_MANAGER, 
				Measurements.FERET | Measurements.CENTER_OF_MASS | Measurements.AREA | Measurements.MEAN, rt, 10, 10e9, 0, 1);
		pa.analyze(imRep);
		
		// Get the mean size of the particles
		float meanArea = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			meanArea += rt.getValue("Area", i);
		}
		meanArea = meanArea / rt.getCounter();
		
		IJ.log("Reporter detected = "+rt.getCounter());
		
		RoiManager manager = RoiManager.getInstance();
		Roi[] roiA = manager.getRoisAsArray();		
		manager.removeAll();
		manager.close();
		
		// Create the reporters objects
		for(int i = 0; i < rt.getCounter(); i++){
				
			// Filter the particles based on their size
			if(rt.getValue("Area", i) > meanArea ){
				// Get the mean value inside the detected object			
				Analyzer meas = new Analyzer(imOriginal,  Measurements.MEAN, rt2);
				meas.measure();
								
				// Build the reporter object
				Reporter rep = new Reporter(
						(int) rt.getValue("XM", i), 		// x pos
						(int) rt.getValue("YM", i), 		// y pos
						time, 									// time
						(float) rt2.getValue("Mean", 0), 	// intensity
						(float) rt.getValue("Feret", i),	// diameter
						roiA[i],
						rs);			
					
				rs.attach(rep, Util.REPORTER);
			}
		}
		rt.reset(); rt2.reset();		
	}
	
	
	/**
	 * Process the segments in the image
	 * @param im the image to process
	 * @param time the time in series
	 * @return the processed image
	 */
	public void processSegment(ImagePlus im, int time){

		IJ.log("Process Segment data...");
		
		// Process the segments (threshold them out)
		im.setRoi(0,  0, im.getWidth(), im.getHeight());
		ImagePlus imSeg = im.duplicate();    
		ImageProcessor ipSeg = imSeg.getProcessor();   

		
        // Equalize and normalize the histogram of the image
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.equalize(ipSeg);
		ce.stretchHistogram(ipSeg, 0.4);		
		
        // Threshold the image based on its mean value
		if(!lowContrast){
			ImageStatistics istats = ipSeg.getStatistics();     
			ipSeg.threshold((int) istats.mean * 2);
		}
		else{
			ipSeg.threshold(200);
		}
		
		// To find the segments, we use a broken skeleton
		BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(ipSeg, true));
		bp.invert();		
		bp = Util.cleanImage(bp);
		bp.skeletonize();	  
		bp = Util.removeSkeletonConnections(bp);
		bp.threshold(120);
		bp.invert(); // TO COMMENT
        imSeg.setProcessor(bp);
        
//        imSeg.show(); if(1==1) return;
        
        // Get the segments in the image
        ResultsTable rt = new ResultsTable();
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET , 
				Measurements.RECT | Measurements.FERET | Measurements.AREA, rt, 10, 500, 0, 1);
		pa.analyze(imSeg);

		// Get the mean size of the particles
		float meanArea = 0;
		for(int i = 0; i < rt.getCounter(); i++){
			meanArea += rt.getValue("Area", i);
		}
		meanArea = meanArea / rt.getCounter();
		
		IJ.log("Segment detected = "+rt.getCounter());

		// Create the segment objects
		for(int i = 0; i < rt.getCounter(); i++){
			
			// filter the particules based on their size (relative tot the image)
			if(rt.getValue("Area", i) > meanArea/2){
				Segment seg ;
				if(rt.getValue("FeretAngle", i) < 90){
					seg = new Segment(
						(int) rt.getValue("BX", i), 		// x pos
						(int) (rt.getValue("BY", i) + rt.getValue("Height", i)), 		// y pos
						(int) (rt.getValue("BX", i) + rt.getValue("Width", i)), 		// x pos
						(int) rt.getValue("BY", i), 		// y pos
						time,
						rs.segList.size()+1);
				}
				else{
					seg = new Segment(
							(int) rt.getValue("BX", i), 		// x pos
							(int) rt.getValue("BY", i), 		// y pos
							(int) (rt.getValue("BX", i) + rt.getValue("Width", i)), 		// x pos
							(int) (rt.getValue("BY", i) + rt.getValue("Height", i)), 		// y pos
							time,
							rs.segList.size()+1);
				}
				rs.attachSegment(seg);
			}

		}		
		rt.reset();	
	}
	
	
	
	/**
	 * Initialize the CSV file header
	 */
	public void initializeCSV(){
		pwSeg.println("image,time_in_serie,segment_id,length,direction,start_x,start_y,end_x,end_y,environment,reporter_id,intensity,intensity_ratio,diameter,rep_x,rep_y");
		pwReporter.println("image,time_in_serie,rep_id,intensity,intensity_ratio,environment,feret,rep_x,rep_y");
	}	

	
}
