package com.ociweb.jpgRaster;

import java.util.concurrent.TimeUnit;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.test.ConsoleJSONDumpStage;
import com.ociweb.pronghorn.util.MainArgs;

public class JPGRaster {

	public static void main(String[] args) {
		String defaultFiles = "test_jpgs/huff_simple0 test_jpgs/robot test_jpgs/cat test_jpgs/car test_jpgs/squirrel test_jpgs/nathan test_jpgs/earth test_jpgs/dice test_jpgs/pyramids test_jpgs/static test_jpgs/turtle";
		String inputFilePaths = MainArgs.getOptArg("fileName", "-f", args, defaultFiles);
		String[] inputFiles = inputFilePaths.split(" ");
		
		/*for (int i = 0; i < inputFiles.length; ++i) {
			String file = inputFiles[i];
			if (file.length() > 4 && file.substring(file.length() - 4).equals(".jpg")) {
				file = file.substring(0, file.length() - 4);
			}
			try {
				System.out.println("Reading '" + file + "' JPG file...");
				Header header = JPGScanner.ReadJPG(file + ".jpg");
				if (header.valid) {
					System.out.println("Performing Huffman Decoding...");
					ArrayList<MCU> mcus = HuffmanDecoder.decodeHuffmanData(header);
					if (mcus != null) {
						System.out.println("Performing Inverse Quantization...");
						InverseQuantizer.dequantize(mcus, header);
						System.out.println("Performing Inverse DCT...");
						InverseDCT.inverseDCT(mcus);
						System.out.println("Performing YCbCr to RGB Conversion...");
						byte[][] rgb = YCbCrToRGB.convertYCbCrToRGB(mcus, header.height, header.width);
						System.out.println("Writing BMP file...");
						BMPDumper.Dump(rgb, header.height, header.width, file + ".bmp");
						System.out.println("Done.");
					}
				}
			} catch (IOException e) {
				System.err.println("Error - Unknown error");
			}
		}*/
		
		GraphManager gm = new GraphManager();
		
		populateGraph(gm, inputFiles);
				
		gm.enableTelemetry(8089);
				
		StageScheduler.defaultScheduler(gm).startup();
	}


	private static void populateGraph(GraphManager gm, String[] inputFiles) {
				
		/*		
		new PipeCleanerStage<>(gm, pipe1B); // dumps all data which came in 
		
		new FileBlobWriteStage(gm, pipe1B, false, ".\targetFile.dat"); // write byte data to disk
		*/
		
		// pipe1 should be the same size as the others, but it mysteriously fills up faster (shouldn't be the case)
		Pipe<JPGSchema> pipe1 = JPGSchema.instance.newPipe(500, 200);
		Pipe<JPGSchema> pipe2 = JPGSchema.instance.newPipe(500, 200);
		Pipe<JPGSchema> pipe3 = JPGSchema.instance.newPipe(500, 200);
		Pipe<JPGSchema> pipe4 = JPGSchema.instance.newPipe(500, 200);
		
		JPGScanner scanner = new JPGScanner(gm, pipe1);
		InverseQuantizer inverseQuantizer = new InverseQuantizer(gm, pipe1, pipe2);
		InverseDCT inverseDCT = new InverseDCT(gm, pipe2, pipe3);
		YCbCrToRGB yCbCr = new YCbCrToRGB(gm, pipe3, pipe4);
		BMPDumper bmpDumper = new BMPDumper(gm, pipe4, System.nanoTime(), scanner, inverseQuantizer, inverseDCT, yCbCr);
		
//		new ConsoleJSONDumpStage<JPGSchema>(gm, pipe4);
		
		for (int i = 0; i < inputFiles.length; ++i) {
			String file = inputFiles[i];
			if (file.length() > 4 && file.substring(file.length() - 4).equals(".jpg")) {
				file = file.substring(0, file.length() - 4);
			}
			scanner.queueFile(file);
		}
	}

}
