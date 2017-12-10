package com.ociweb.jpgRaster;

import com.ociweb.pronghorn.pipe.DataInputBlobReader;
import com.ociweb.pronghorn.pipe.FieldReferenceOffsetManager;
import com.ociweb.pronghorn.pipe.MessageSchema;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeReader;
import com.ociweb.pronghorn.pipe.PipeWriter;

public class BMPSchema extends MessageSchema<BMPSchema> {
	public final static FieldReferenceOffsetManager FROM = new FieldReferenceOffsetManager(
		    new int[]{0xc0400002,0xb8000000,0xc0200002},
		    (short)0,
		    new String[]{"RawMessage","RawData",null},
		    new long[]{1, 101, 0},
		    new String[]{"global",null,null},
		    "BMP.xml",
		    new long[]{2, 2, 0},
		    new int[]{2, 2, 0});


		protected BMPSchema() { 
		    super(FROM);
		}

		public static final BMPSchema instance = new BMPSchema();

		public static final int MSG_RAWMESSAGE_1 = 0x00000000; //Group/OpenTempl/2
		public static final int MSG_RAWMESSAGE_1_FIELD_RAWDATA_101 = 0x01c00001; //ByteVector/None/0


		public static void consume(Pipe<BMPSchema> input) {
		    while (PipeReader.tryReadFragment(input)) {
		        int msgIdx = PipeReader.getMsgIdx(input);
		        switch(msgIdx) {
		            case MSG_RAWMESSAGE_1:
		                consumeRawMessage(input);
		            break;
		            case -1:
		               //requestShutdown();
		            break;
		        }
		        PipeReader.releaseReadLock(input);
		    }
		}

		public static void consumeRawMessage(Pipe<BMPSchema> input) {
		    DataInputBlobReader<BMPSchema> fieldRawData = PipeReader.inputStream(input, MSG_RAWMESSAGE_1_FIELD_RAWDATA_101);
		}

		public static void publishRawMessage(Pipe<BMPSchema> output, byte[] fieldRawDataBacking, int fieldRawDataPosition, int fieldRawDataLength) {
		        PipeWriter.presumeWriteFragment(output, MSG_RAWMESSAGE_1);
		        PipeWriter.writeBytes(output,MSG_RAWMESSAGE_1_FIELD_RAWDATA_101, fieldRawDataBacking, fieldRawDataPosition, fieldRawDataLength);
		        PipeWriter.publishWrites(output);
		}
}