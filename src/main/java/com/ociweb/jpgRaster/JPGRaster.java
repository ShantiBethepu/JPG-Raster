package com.ociweb.jpgRaster;

import java.io.IOException;
import java.util.ArrayList;

import com.ociweb.jpgRaster.JPG.Header;
import com.ociweb.jpgRaster.JPG.MCU;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.stage.file.FileBlobReadStage;
import com.ociweb.pronghorn.stage.file.FileBlobWriteStage;
import com.ociweb.pronghorn.stage.route.ReplicatorStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import com.ociweb.pronghorn.stage.scheduling.StageScheduler;
import com.ociweb.pronghorn.stage.test.ConsoleJSONDumpStage;
import com.ociweb.pronghorn.stage.test.ConsoleSummaryStage;
import com.ociweb.pronghorn.stage.test.PipeCleanerStage;
import com.ociweb.pronghorn.util.MainArgs;

public class JPGRaster {

	public static void main(String[] args) {
		
		String inputFilePath = MainArgs.getOptArg("fileName", "-f", args, "./Simple.jpg");

		//GraphManager gm = new GraphManager();
		
		//populateGraph(gm, inputFilePath);
		
		//gm.enableTelemetry(8089);
		
		//StageScheduler.defaultScheduler(gm).startup();
		
		try {
			Header header = JPGScanner.ReadJPG(inputFilePath);
			ArrayList<MCU> mcus = HuffmanDecoder.decodeFourTables(header);
			RunLengthDecoder.decodeRLE(mcus);
			InverseQuantizer.dequantize(mcus, header);
			InverseDCT.inverseDCT(mcus);
			} catch (IOException e) {
			
		}
	}


	private static void populateGraph(GraphManager gm, String inputFilePath) {
				
		Pipe<RawDataSchema> pipe1  = RawDataSchema.instance.newPipe(10, 10_000); // 10 chunks each 10K in  size
		Pipe<RawDataSchema> pipe1A = RawDataSchema.instance.newPipe(20, 20_000); // 10 chunks each 10K in  size
		Pipe<RawDataSchema> pipe1B = RawDataSchema.instance.newPipe(20, 20_000); // 10 chunks each 10K in  size
		
		
		new FileBlobReadStage(gm, pipe1, inputFilePath); // This stage reads a file
		
		
		// This stage replicates the data to two pipes, great for debugging while passing on the real data.
		new ReplicatorStage<>(gm, pipe1, pipe1A, pipe1B); 
		
		new ConsoleJSONDumpStage<>(gm, pipe1A); // see all the data at the console.
		//new ConsoleSummaryStage<>(gm, pipe1A); // dumps just a count of messages periodically
		
		new PipeCleanerStage<>(gm, pipe1B); // dumps all data which came in 
		
		//new FileBlobWriteStage(gm, pipe1B, false, ".\targetFile.dat"); // write byte data to disk
		
		
	}
	

}
