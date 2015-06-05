
/*
 * Designed by Ruben Rellan Alvarez  @ Carnegie Institution for Plant Sciences
 * Written by Guillaume Lobet  @ Universite de Liege, Belgium
 *
 * With this macro we merge and combine the 4 individual images
 * that are taken with the GLO-Roots imaging system. The format
 * of the file_name will be:
 *
 * ExpID-DAS-RhizotronID-FilterID-FL-Acquisition#.tif   FL: Front Low
 * ExpID-DAS-RhizotronID-FilterID-FU-Acquisition#.tif   FU: Front Up
 * ExpID-DAS-RhizotronID-FilterID-BL-Acquisition#.tif   BL: Back Low
 * ExpID-DAS-RhizotronID-FilterID-BU-Acquisition#.tif   BU: Back Up
 *
 * The macro create from this images a 2048x4096 px image,
 * conserve all the parts of the name and use -C- instead of FL, BU etc.
 *
 * It works on batch mode and process several images from a folder
 *
 */

// Tell ImageJ not to display the images
setBatchMode(true);

// Get the image directory
dir = getDirectory("Where are your images");
dir1 = getDirectory("Where do you want to save the converted images");

// Get the image list
list = getFileList(dir);
num = list.length;
count = 0;

print("Processing of "+num+"images started.");

// Loop over all the images
// Open 4 images at the same time (up/down-front/back)
for(k = 0 ;k < num ; k = k+4){

	bl = dir + list[k];  	// Back Low
	bu = dir + list[k+1]; 	// Back Up
	fl = dir + list[k+2];	// Front Low
	fu = dir + list[k+3]; 	// Front Up

	// Compute the new final name

	str = list[k+1];
	i1 = indexOf(str, "R");
	i2 = indexOf(str, "SR");
	i3 = indexOf(str, "-BU");
	i4 = lengthOf(str);
	//nameC = substring(str, 0, i1)+substring(str, i1+1, i2)+substring(str, i2+2, i3)+"-C";
	nameC = substring(str, 0, i3)+"-C";

	print(nameC);


	// ------------------------------------------------------
	// OPEN IMAGES
	// Open the different images and perform the individual
	// operations on them (rotate, translate)

	open(bl);
	tiBL = getTitle();
	run("Rotate 90 Degrees Left");

	open(bu);
	tiBU = getTitle();
	run("Rotate 90 Degrees Left");

	open(fl);
	tiFL = getTitle();
	run("Rotate 90 Degrees Left");
  	run("Flip Horizontally");
  	run("Rotate... ", "angle=0.4 grid=1 interpolation=Bilinear");
  	run("Translate...", "x=-47.8 y=2.00 interpolation=None");

	open(fu);
	tiFU = getTitle();
	run("Rotate 90 Degrees Left");
	run("Flip Horizontally");
	run("Rotate... ", "angle=-0.7 grid=1 interpolation=Bilinear");
	run("Translate...", "x=-24.4 y=2.00 interpolation=None");

	h = getHeight();
	w = getWidth();



	// ------------------------------------------------------
	// COMBINE UP AND DOWN IMAGES

	imageCalculator("Max create", tiBU, tiFU);
	tiU = "Up";
	rename(tiU);	// Rename the image to easily retrieve them later

	imageCalculator("Max create", tiBL, tiFL);
	tiL = "Low";
	rename(tiL);	// Rename the image to easily retrieve them later

	// Close the original images
	selectWindow(tiFU);
	close();
	selectWindow(tiBU);
	close();
	selectWindow(tiFL);
	close();
	selectWindow(tiBL);
	close();

	// ------------------------------------------------------
	// INTENSITY NORMALISATION

	// Up image
	selectWindow(tiU);
	makeRectangle(1948, 548, 50, 1500);
	getStatistics(area, mean);
	run("Select None");	// select the whole image
	run("Add...", "value="+(200 - mean));

	// Low image
	selectWindow(tiL);
	makeRectangle(1948, 548, 50, 1500);
	getStatistics(area, mean);
	run("Select None");	// select the whole image
	run("Add...", "value="+(200-mean));

	// ------------------------------------------------------
	// MERGE LOWER AND UPPER IMAGES
	// First we increase the size of the Up and Low image to the final size.
	// We create zero-value pixels either above or below the image to increase the size.
	// For the Low image, we also translate it.
	// Then we combine both to create the final image

	// Up image
	selectWindow(tiU);
	run("Canvas Size...", "width=2048 height=4096 position=Top-Center zero");

	// Low image
	selectWindow(tiL);
	run("Canvas Size...", "width=2048 height=4096 position=Bottom-Center zero");
	run("Translate...", "x=15 y=-44 interpolation=None");

	// Combine both
	imageCalculator("Max create", tiU, tiL);

	// ------------------------------------------------------
	// SAVE AND CLOSE THE FINAL IMAGES
	
	saveAs("Tiff", dir1+nameC);
	close();

	selectWindow(tiU);
	close();

	selectWindow(tiL);
	close();	
	

	count++;
}

print("Processing done. "+count+" complete images were created");
