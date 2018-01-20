package com.ociweb.jpgRaster;

import java.nio.ByteBuffer;

import com.ociweb.jpgRaster.JPG.ColorComponent;
import com.ociweb.jpgRaster.JPG.Header;
import com.ociweb.jpgRaster.JPG.MCU;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeReader;
import com.ociweb.pronghorn.pipe.PipeWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

public class InverseDCT extends PronghornStage {

	private final Pipe<JPGSchema> input;
	private final Pipe<JPGSchema> output;
	
	protected InverseDCT(GraphManager graphManager, Pipe<JPGSchema> input, Pipe<JPGSchema> output) {
		super(graphManager, input, output);
		this.input = input;
		this.output = output;
	}
	
	private static double[] idctMap = new double[64];
	
	// prepare idctMap
	static {
		for (int u = 0; u < 8; ++u) {
			double c = 1.0f;
			if (u == 0) {
				c = 1 / Math.sqrt(2.0);
			}
			for (int x = 0; x < 8; ++x) {
				idctMap[u * 8 + x] = c * Math.cos((2.0 * x + 1.0) * u * Math.PI / 16.0);
			}
		}
	}
	
	private static short[] MCUInverseDCT(short[] mcu) {
		/*System.out.print("Before Inverse DCT:");
		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				if (j % 8 == 0) {
					System.out.println();
				}
				System.out.print(mcu[i * 8 + j] + " ");
			}
		}
		System.out.println();*/
		
		short[] result = new short[64];
		
		for (int y = 0; y < 8; ++y) {
			for (int x = 0; x < 8; ++x) {
				double sum = 0.0;
				for (int i = 0; i < 8; ++i) {
					for (int j = 0; j < 8; ++j) {
						sum += mcu[i * 8 + j] *
							   idctMap[j * 8 + x] *
							   idctMap[i * 8 + y];
					}
				}
				sum /= 4.0;
				result[y * 8 + x] = (short)sum;
			}
		}
		
		/*System.out.print("After Inverse DCT:");
		for (int i = 0; i < 8; ++i) {
			for (int j = 0; j < 8; ++j) {
				if (j % 8 == 0) {
					System.out.println();
				}
				System.out.print(result[i * 8 + j] + " ");
			}
		}
		System.out.println();*/
		
		return result;
	}
	
	public static void inverseDCT(MCU mcu) {
		mcu.y =  MCUInverseDCT(mcu.y);
		mcu.cb = MCUInverseDCT(mcu.cb);
		mcu.cr = MCUInverseDCT(mcu.cr);
		return;
	}

	@Override
	public void run() {
		while (PipeWriter.hasRoomForWrite(output) && PipeReader.tryReadFragment(input)) {
			
			int msgIdx = PipeReader.getMsgIdx(input);
			
			if (msgIdx == JPGSchema.MSG_HEADERMESSAGE_1) {
				// read header from pipe
				Header header = new Header();
				header.height = PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_HEIGHT_101);
				header.width = PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_WIDTH_201);
				String filename = PipeReader.readASCII(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_FILENAME_301, new StringBuilder()).toString();
				header.frameType = PipeReader.readASCII(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_FRAMETYPE_401, new StringBuilder()).toString();
				header.precision = (short) PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_PRECISION_501);
				header.startOfSelection = (short) PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_STARTOFSELECTION_601);
				header.endOfSelection = (short) PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_ENDOFSELECTION_701);
				header.successiveApproximation = (short) PipeReader.readInt(input, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_SUCCESSIVEAPPROXIMATION_801);

				// write header to pipe
				if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_HEADERMESSAGE_1)) {
					System.out.println("Writing header to pipe...");
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_HEIGHT_101, header.height);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_WIDTH_201, header.width);
					PipeWriter.writeASCII(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_FILENAME_301, filename);
					PipeWriter.writeASCII(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_FRAMETYPE_401, header.frameType);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_PRECISION_501, header.precision);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_STARTOFSELECTION_601, header.startOfSelection);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_ENDOFSELECTION_701, header.endOfSelection);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_SUCCESSIVEAPPROXIMATION_801, header.successiveApproximation);
				}
				else {
					requestShutdown();
				}
				PipeWriter.publishWrites(output);
			}
			else if (msgIdx == JPGSchema.MSG_COLORCOMPONENTMESSAGE_2) {
				// read color component data from pipe
				ColorComponent component = new ColorComponent();
				component.componentID = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_COMPONENTID_102);
				component.horizontalSamplingFactor = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HORIZONTALSAMPLINGFACTOR_202);
				component.verticalSamplingFactor = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_VERTICALSAMPLINGFACTOR_302);
				component.quantizationTableID = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_QUANTIZATIONTABLEID_402);
				component.huffmanACTableID = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HUFFMANACTABLEID_502);
				component.huffmanDCTableID = (short) PipeReader.readInt(input, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HUFFMANDCTABLEID_602);
				
				// write color component data to pipe
				System.out.println("Attempting to write color component to pipe...");
				if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2)) {
					System.out.println("Writing color component to pipe...");
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_COMPONENTID_102, component.componentID);
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HORIZONTALSAMPLINGFACTOR_202, component.horizontalSamplingFactor);
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_VERTICALSAMPLINGFACTOR_302, component.verticalSamplingFactor);
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_QUANTIZATIONTABLEID_402, component.quantizationTableID);
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HUFFMANACTABLEID_502, component.huffmanACTableID);
					PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HUFFMANDCTABLEID_602, component.huffmanDCTableID);
				}
				else {
					requestShutdown();
				}
				PipeWriter.publishWrites(output);
			}
			else if (msgIdx == JPGSchema.MSG_MCUMESSAGE_6) {
				MCU mcu = new MCU();
				ByteBuffer yBuffer = ByteBuffer.allocate(64 * 2);
				ByteBuffer cbBuffer = ByteBuffer.allocate(64 * 2);
				ByteBuffer crBuffer = ByteBuffer.allocate(64 * 2);
				PipeReader.readBytes(input, JPGSchema.MSG_MCUMESSAGE_6_FIELD_Y_106, yBuffer);
				PipeReader.readBytes(input, JPGSchema.MSG_MCUMESSAGE_6_FIELD_CB_206, cbBuffer);
				PipeReader.readBytes(input, JPGSchema.MSG_MCUMESSAGE_6_FIELD_CR_306, crBuffer);
				inverseDCT(mcu);
				
				ByteBuffer yBuffer2 = ByteBuffer.allocate(64 * 2);
				ByteBuffer cbBuffer2 = ByteBuffer.allocate(64 * 2);
				ByteBuffer crBuffer2 = ByteBuffer.allocate(64 * 2);
				for (int i = 0; i < 64; ++i) {
					yBuffer2.putShort(mcu.y[i]);
					cbBuffer2.putShort(mcu.cb[i]);
					crBuffer2.putShort(mcu.cr[i]);
				}
				yBuffer2.position(0);
				cbBuffer2.position(0);
				crBuffer2.position(0);
				PipeWriter.writeBytes(output, JPGSchema.MSG_MCUMESSAGE_6_FIELD_Y_106, yBuffer2);
				PipeWriter.writeBytes(output, JPGSchema.MSG_MCUMESSAGE_6_FIELD_CB_206, cbBuffer2);
				PipeWriter.writeBytes(output, JPGSchema.MSG_MCUMESSAGE_6_FIELD_CR_306, crBuffer2);
				PipeWriter.publishWrites(output);
			}
			else {
				requestShutdown();
			}
		}
	}
	
	/*public static void main(String[] args) {
		short[] mcu = new short[] {
				-252, -36,  -5, -6, 15, -4, 6, 0,
				  55,  84, -14,  7,  0,  0, 0, 0,
				  20,   0, -18, -9,  0,  0, 0, 0,
				 -24,  32,   0,  0,  0,  0, 0, 0,
				 22,  -22,   0,  0,  0,  0, 0, 0,
				  0,    0,   0,  0,  0,  0, 0, 0,
				  0,    0,   0,  0,  0,  0, 0, 0,
				  0,    0,   0,  0,  0,  0, 0, 0
		};
		short[] result = MCUInverseDCT(mcu);
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				System.out.print(String.format("%04d ", result[i * 8 + j]));
			}
			System.out.println();
		}
	}*/
}
