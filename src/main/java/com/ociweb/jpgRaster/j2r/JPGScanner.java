package com.ociweb.jpgRaster.j2r;

import com.ociweb.jpgRaster.JPG.Header;
import com.ociweb.jpgRaster.JPGConstants;
import com.ociweb.jpgRaster.JPGSchema;
import com.ociweb.jpgRaster.JPG.ColorComponent;
import com.ociweb.jpgRaster.JPG.QuantizationTable;
import com.ociweb.jpgRaster.JPG.HuffmanTable;
import com.ociweb.jpgRaster.JPG.MCU;
import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.pipe.PipeWriter;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.stage.PronghornStage;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class JPGScanner extends PronghornStage {

	private ArrayList<String> inputFiles = new ArrayList<String>();
	private final Pipe<JPGSchema> output;
	boolean verbose;
	
	int mcuWidth = 0;
	int mcuHeight = 0;
	int numMCUs = 0;
	int numProcessed = 0;
	int aboutToSend = 0;
	
	Header header;
	MCU mcu1 = new MCU();
	MCU mcu2 = new MCU();
	MCU mcu3 = new MCU();
	MCU mcu4 = new MCU();
	ArrayList<MCU> mcus = null;
	
	public JPGScanner(GraphManager graphManager, Pipe<JPGSchema> output, boolean verbose) {
		super(graphManager, NONE, output);
		this.output = output;
		this.verbose = verbose;
	}
	
	/**
	 * Reads JPG File into an ArrayList of MCUs.
	 * 
	 * @param filename name of file to be read
	 * @param mcus ArrayList of MCUs to be populated during the decoding process
	 */
	public Header ReadJPG(String filename, ArrayList<MCU> mcus) throws IOException {
		Header header = new Header();
		header.filename = filename;
		DataInputStream f = new DataInputStream(new FileInputStream(filename));
		
		// JPG file must begin with 0xFFD8
		short last = (short)f.readUnsignedByte();
		short current = (short)f.readUnsignedByte();
		if (last != 0xFF || current != JPGConstants.SOI) {
			header.valid = false;
			f.close();
			return header;
		}
		if (verbose) 
			System.out.println("Start of Image");
		last = (short)f.readUnsignedByte();
		current = (short)f.readUnsignedByte();
		
		while (header.valid) {
			if (last != 0xFF) {
				System.err.println("Error - Expected a marker");
				header.valid = false;
				f.close();
				return header;
			}
			switch (current) {
			case JPGConstants.DQT:
				ReadQuantizationTable(f, header);
				break;
			case JPGConstants.SOF0:
				header.frameType = "Baseline";
				ReadStartOfFrame(f, header);
				break;
			case JPGConstants.SOF1:
				header.frameType = "Extended Sequential";
				ReadStartOfFrame(f, header);
				break;
			case JPGConstants.SOF2:
				header.frameType = "Progressive";
				ReadStartOfFrame(f, header);
				break;
			case JPGConstants.SOF3:
				header.frameType = "Lossless";
				ReadStartOfFrame(f, header);
				break;
			case JPGConstants.DHT:
				ReadHuffmanTable(f, header);
				break;
			case JPGConstants.SOS:
				ReadStartOfScan(f, header);
				// break out of while loop
				break;
			case JPGConstants.DRI:
				ReadRestartInterval(f, header);
				break;
			case JPGConstants.APP0:
			case JPGConstants.APP1:
			case JPGConstants.APP2:
			case JPGConstants.APP3:
			case JPGConstants.APP4:
			case JPGConstants.APP5:
			case JPGConstants.APP6:
			case JPGConstants.APP7:
			case JPGConstants.APP8:
			case JPGConstants.APP9:
			case JPGConstants.APP10:
			case JPGConstants.APP11:
			case JPGConstants.APP12:
			case JPGConstants.APP13:
			case JPGConstants.APP14:
			case JPGConstants.APP15:
				ReadAPPN(f, header);
				break;
			case JPGConstants.COM:
				ReadComment(f, header);
				break;
			case 0xFF: // skip
				current = (short)f.readUnsignedByte();
				break;
			case JPGConstants.JPG0:
			case JPGConstants.JPG1:
			case JPGConstants.JPG2:
			case JPGConstants.JPG3:
			case JPGConstants.JPG4:
			case JPGConstants.JPG5:
			case JPGConstants.JPG6:
			case JPGConstants.JPG7:
			case JPGConstants.JPG8:
			case JPGConstants.JPG9:
			case JPGConstants.JPG10:
			case JPGConstants.JPG11:
			case JPGConstants.JPG12:
			case JPGConstants.JPG13:
			case JPGConstants.DNL:
			case JPGConstants.DHP:
			case JPGConstants.EXP:
				// unsupported segments that can be skipped
				ReadComment(f, header);
				break;
			case JPGConstants.TEM:
				// unsupported segment with no size
				break;
			case JPGConstants.SOI:
				System.err.println("Error - This JPG contains an embedded JPG; THis is not supported");
				header.valid = false;
				f.close();
				return header;
			case JPGConstants.EOI:
				System.err.println("Error - EOI detected before SOS");
				header.valid = false;
				f.close();
				return header;
			case JPGConstants.DAC:
				System.err.println("Error - Arithmetic Table mode is not supported");
				header.valid = false;
				f.close();
				return header;
				// case JPGConstants.SOF4:
			case JPGConstants.SOF5:
			case JPGConstants.SOF6:
			case JPGConstants.SOF7:
				// case JPGConstants.SOF8:
			case JPGConstants.SOF9:
			case JPGConstants.SOF10:
			case JPGConstants.SOF11:
				// case JPGConstants.SOF12:
			case JPGConstants.SOF13:
			case JPGConstants.SOF14:
			case JPGConstants.SOF15:
				System.err.println("Error - This Start of Frame marker is not supported: " + String.format("0x%2x", current));
				header.valid = false;
				f.close();
				return header;
			case JPGConstants.RST0:
			case JPGConstants.RST1:
			case JPGConstants.RST2:
			case JPGConstants.RST3:
			case JPGConstants.RST4:
			case JPGConstants.RST5:
			case JPGConstants.RST6:
			case JPGConstants.RST7:
				System.err.println("Error - RSTN detected before SOS");
				header.valid = false;
				f.close();
				return header;
			default:
				System.err.println("Error - Unknown Marker: " + String.format("0x%2x", current));
				header.valid = false;
				f.close();
				return header;
			}
			if (current == JPGConstants.SOS) {
				// break out of while loop
				break;
			}

			last = (short)f.readUnsignedByte();
			current = (short)f.readUnsignedByte();
		}
		if (header.valid) {
			if (header.frameType.equals("Progressive")) {
				int numScans = 0;
				current = (short)f.readUnsignedByte();
				while (true) {
					last = current;
					current = (short)f.readUnsignedByte();
					if (last == 0xFF) {
						if      (current == JPGConstants.EOI) {
							if (verbose) 
								System.out.println("End of Image");
							break;
						}
						else if (current == 0x00) {
							header.imageData.add(last);
							// advance by a byte, to drop 0x00
							current = (short)f.readUnsignedByte();
						}
						else if (current == JPGConstants.DHT) {
							if (header.imageData.size() > 0) {
								if (!decodeScan(header, mcus, numScans)) {
									return header;
								}
								numScans += 1;
							}
							
							ReadHuffmanTable(f, header);
							current = (short)f.readUnsignedByte();
						}
						else if (current == JPGConstants.SOS) {
							if (header.imageData.size() > 0) {
								if (!decodeScan(header, mcus, numScans)) {
									return header;
								}
								numScans += 1;
							}
							
							ReadStartOfScan(f, header);
							current = (short)f.readUnsignedByte();
						}
						else if (current >= JPGConstants.RST0 && current <= JPGConstants.RST7) {
							ReadRSTN(f, header);
							current = (short)f.readUnsignedByte();
						}
						else if (current != 0xFF) {
							System.err.println("Error - Invalid marker during compressed data scan: " + String.format("0x%2x", current));
							header.valid = false;
							f.close();
							return header;
						}
					}
					else {
						header.imageData.add(last);
					}
				}
			}
			else { // if (header.frameType.equals("Baseline")) {
				current = (short)f.readUnsignedByte();
				while (true) {
					last = current;
					current = (short)f.readUnsignedByte();
					if (last == 0xFF) {
						if      (current == JPGConstants.EOI) {
							if (verbose) 
								System.out.println("End of Image");
							break;
						}
						else if (current == 0x00) {
							header.imageData.add(last);
							// advance by a byte, to drop 0x00
							current = (short)f.readUnsignedByte();
						}
						else if (current >= JPGConstants.RST0 && current <= JPGConstants.RST7) {
							ReadRSTN(f, header);
							current = (short)f.readUnsignedByte();
						}
						else if (current != 0xFF) {
							System.err.println("Error - Invalid marker during compressed data scan: " + String.format("0x%2x", current));
							header.valid = false;
							f.close();
							return header;
						}
					}
					else {
						header.imageData.add(last);
					}
				}
			}
		}
		f.close();
		
		if (header.numComponents != 1 && header.numComponents != 3) {
			System.err.println("Error - " + header.numComponents + " color components given (1 or 3 required)");
			header.valid = false;
			return header;
		}
		
		if (header.numComponents > 0 &&
			(header.colorComponents[0].horizontalSamplingFactor > 2 ||
			 header.colorComponents[0].verticalSamplingFactor > 2)) {
			System.err.println("Error - Unsupported Sampling Factor");
			header.valid = false;
		}
		if (header.numComponents > 1 &&
			(header.colorComponents[1].horizontalSamplingFactor != 1 ||
			 header.colorComponents[1].verticalSamplingFactor != 1)) {
			System.err.println("Error - Unsupported Sampling Factor");
			header.valid = false;
		}
		if (header.numComponents > 2 &&
			(header.colorComponents[2].horizontalSamplingFactor != 1 ||
			 header.colorComponents[2].verticalSamplingFactor != 1)) {
			System.err.println("Error - Unsupported Sampling Factor");
			header.valid = false;
		}
		
		return header;
	}
	
	// decode a whole scan, progressive images only
	private boolean decodeScan(Header header, ArrayList<MCU> mcus, int numScans) {
		// decode scan so far
		if (verbose) 
			System.out.println("Decoding a scan of size " + header.imageData.size());
		HuffmanDecoder.beginDecode(header);

		MCU mcu1 = null;
		MCU mcu2 = null;
		MCU mcu3 = null;
		MCU mcu4 = null;
		int horizontal = header.colorComponents[0].horizontalSamplingFactor;
		int vertical = header.colorComponents[0].verticalSamplingFactor;
		int numMCUs = ((header.width + 7) / 8) * ((header.height + 7) / 8);
		int numProcessed = 0;
		while (numProcessed < numMCUs) {
			if (mcus.size() < numMCUs) {
				if (horizontal == 1 && vertical == 1) {
					mcu1 = new MCU();
				}
				else if (horizontal == 2 && vertical == 1) {
					mcu1 = new MCU();
					mcu2 = new MCU();
				}
				else if (horizontal == 1 && vertical == 2) {
					mcu1 = new MCU();
					mcu2 = new MCU();
				}
				else if (horizontal == 2 && vertical == 2) {
					mcu1 = new MCU();
					mcu2 = new MCU();
					mcu3 = new MCU();
					mcu4 = new MCU();
				}
			}
			else {
				if (horizontal == 1 && vertical == 1) {
					mcu1 = mcus.get(numProcessed);
				}
				else if (horizontal == 2 && vertical == 1) {
					mcu1 = mcus.get(numProcessed);
					mcu2 = mcus.get(numProcessed + 1);
				}
				else if (horizontal == 1 && vertical == 2) {
					mcu1 = mcus.get(numProcessed);
					mcu2 = mcus.get(numProcessed + 1);
				}
				else if (horizontal == 2 && vertical == 2) {
					mcu1 = mcus.get(numProcessed);
					mcu2 = mcus.get(numProcessed + 1);
					mcu3 = mcus.get(numProcessed + 2);
					mcu4 = mcus.get(numProcessed + 3);
				}
			}
			if (!HuffmanDecoder.decodeHuffmanData(mcu1, mcu2, mcu3, mcu4)) {
				System.err.println("Error during scan " + numScans);
				return false;
			}
			mcus.add(mcu1);
			numProcessed += 1;
			if (header.colorComponents[0].horizontalSamplingFactor == 2) {
				mcus.add(mcu2);
				numProcessed += 1;
			}
			if (header.colorComponents[0].verticalSamplingFactor == 2) {
				mcus.add(mcu3);
				numProcessed += 1;
			}
			if (header.colorComponents[0].horizontalSamplingFactor == 2 &&
				header.colorComponents[0].verticalSamplingFactor == 2) {
				mcus.add(mcu4);
				numProcessed += 1;
			}
		}
		header.imageData.clear();
		return true;
	}
	
	/**
	 * Populates JPG header with Quantization Table data.
	 * 
	 * @param	f stream of byte data being read
	 * @param	header object representation of JPG header
	 */
	private void ReadQuantizationTable(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading Quantization Tables");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		length -= 2;
		while (length > 0) {
			short info = (short)f.readUnsignedByte();
			QuantizationTable table = new QuantizationTable();
			table.tableID = (short)(info & 0x0F);
			
			if (table.tableID > 3) {
				System.err.println("Error - Invalid Quantization table ID: " + table.tableID);
				header.valid = false;
				return;
			}
			
			if ((info & 0xF0) == 0) {
				table.precision = 1;
			}
			else {
				table.precision = 2;
			}
			if (table.precision == 2) {
				for (int i = 0; i < 64; ++i) {
					table.table[i] = f.readUnsignedByte() << 8 + f.readUnsignedByte();
				}
			}
			else {
				for (int i = 0; i < 64; ++i) {
					table.table[i] = f.readUnsignedByte();
				}
			}
			header.quantizationTables[table.tableID] = table;
			length -= 64 * table.precision + 1;
		}
		if (length != 0) {
			System.err.println("Error - DQT Invalid");
			header.valid = false;
		}
	}
	
	/**
	 * Populates JPG header with SOF data. Start of frame determines the image
	 * dimensions, color modes, and subsampling used to compress the image.
	 * 
	 * @param	f stream of byte data being read
	 * @param	header object representation of JPG header
	 */
	private void ReadStartOfFrame(DataInputStream f, Header header) throws IOException {
		if (header.numComponents != 0) {
			System.err.println("Error - Multiple SOFs detected");
			header.valid = false;
			return;
		}
		if (verbose) 
			System.out.println("Reading Start of Frame");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		header.precision = (short)f.readUnsignedByte();
		
		if (header.precision != 8) {
			System.err.println("Error - Invalid precision: " + header.precision);
			header.valid = false;
			return;
		}
		
		header.height = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		header.width = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		if (header.height == 0 || header.width == 0) {
			System.err.println("Error - Invalid dimensions");
			header.valid = false;
			return;
		}
		
		header.numComponents = (short)f.readUnsignedByte();
		if (header.numComponents == 4) {
			System.err.println("Error - CMYK color mode not supported");
			header.valid = false;
			return;
		}
		for (int i = 0; i < header.numComponents; ++i) {
			ColorComponent component = new ColorComponent();
			component.componentID = (short)f.readUnsignedByte();
			short samplingFactor = (short)f.readUnsignedByte();
			component.horizontalSamplingFactor = (short)((samplingFactor & 0xF0) >> 4);
			component.verticalSamplingFactor = (short)(samplingFactor & 0x0F);
			component.quantizationTableID = (short)f.readUnsignedByte();
			
			if (component.componentID == 0) {
				header.zeroBased = true;
			}
			if (header.zeroBased) {
				component.componentID += 1;
			}
			
			if (component.componentID == 4 || component.componentID == 5) {
				System.err.println("Error - YIQ color mode not supported");
				header.valid = false;
				return;
			}
			if (header.colorComponents[component.componentID - 1] != null) {
				System.err.println("Error - Duplicate color component ID");
				header.valid = false;
				return;
			}
			if (component.quantizationTableID > 3) {
				System.err.println("Error - Invalid Quantization table ID in frame components");
				header.valid = false;
				return;
			}
			
			header.colorComponents[component.componentID - 1] = component;
		}
		if (length - 8 - (header.numComponents * 3) != 0) {
			System.err.println("Error - SOF Invalid");
			header.valid = false;
		}
	}
	
	/**
	 * Populates JPG header with Huffman Table data.
	 * 
	 * @param	f stream of byte data being read
	 * @param	header object representation of JPG header
	 */
	private void ReadHuffmanTable(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading Huffman Tables");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		length -= 2;
		while (length > 0) {
			HuffmanTable table = new HuffmanTable();
			short info = (short)f.readUnsignedByte();
			table.tableID = (short)(info & 0x0F);
			boolean ACTable = (info & 0xF0) != 0;
			
			if (table.tableID > 3) {
				System.err.println("Error - Invalid Huffman table ID: " + table.tableID);
				header.valid = false;
				return;
			}
			
			int allSymbols = 0;
			short[] numSymbols = new short[16];
			for (int i = 0; i < 16; ++i) {
				numSymbols[i] = (short)f.readUnsignedByte();
				allSymbols += numSymbols[i];
			}
			
			if (allSymbols > 256) {
				System.err.println("Error - Invalid Huffman table");
				header.valid = false;
				return;
			}
			
			for (int i = 0; i < 16; ++i) {
				table.symbols.add(new ArrayList<Short>());
				for (int j = 0; j < numSymbols[i]; ++j) {
					table.symbols.get(i).add((short)f.readUnsignedByte());
				}
			}
			if (ACTable) {
				header.huffmanACTables[table.tableID] = table;
			}
			else {
				header.huffmanDCTables[table.tableID] = table;
			}
			length -= allSymbols + 17;
		}
		if (length != 0) {
			System.err.println("Error - DHT Invalid");
			header.valid = false;
		}
	}
	
	/**
	 * Populates JPG header with SOS data. Determines the color component IDs and
	 * Huffman table IDs used during compression.
	 * 
	 * @param	f stream of byte data being read
	 * @param	header object representation of JPG header
	 */
	private void ReadStartOfScan(DataInputStream f, Header header) throws IOException {
		if (header.numComponents == 0) {
			System.err.println("Error - SOS detected before SOF");
			header.valid = false;
			return;
		}
		if (verbose) 
			System.out.println("Reading Start of Scan");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		for (int i = 0; i < header.numComponents; ++i) {
			header.colorComponents[i].used = false;
		}
		
		int numComponents = f.readUnsignedByte();
		for (int i = 0; i < numComponents; ++i) {
			short componentID = (short)f.readUnsignedByte();
			short huffmanTableID = (short)f.readUnsignedByte();
			short huffmanACTableID = (short)(huffmanTableID & 0x0F);
			short huffmanDCTableID = (short)((huffmanTableID & 0xF0) >> 4);
			
			if (header.zeroBased) {
				componentID += 1;
			}
			
			if (huffmanACTableID > 3 || huffmanDCTableID > 3) {
				System.err.println("Error - Invalid Huffman table ID in scan components");
				header.valid = false;
				return;
			}

			if (componentID > header.numComponents) {
				System.err.println("Error - Invalid Color Component ID: " + componentID);
				header.valid = false;
				return;
			}
			header.colorComponents[componentID - 1].huffmanACTableID = huffmanACTableID;
			header.colorComponents[componentID - 1].huffmanDCTableID = huffmanDCTableID;
			header.colorComponents[componentID - 1].used = true;
		}
		header.startOfSelection = (short)f.readUnsignedByte();
		header.endOfSelection = (short)f.readUnsignedByte();
		short successiveApproximation = (short)f.readUnsignedByte();
		header.successiveApproximationLow = (short)(successiveApproximation & 0x0F);
		header.successiveApproximationHigh = (short)((successiveApproximation & 0xF0) >> 4);
		
		if (header.frameType.equals("Baseline") &&
			(header.startOfSelection != 0 ||
			header.endOfSelection != 63 ||
			header.successiveApproximationHigh != 0 ||
			header.successiveApproximationLow != 0)) {
			System.err.println("Error - Spectral selection or approximation is incompatible with Baseline");
			header.valid = false;
			return;
		}
		
		if (length - 6 - (numComponents * 2) != 0) {
			System.err.println("Error - SOS Invalid");
			header.valid = false;
		}
	}
	
	private void ReadRestartInterval(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading Restart Interval");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		header.restartInterval = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		if (length - 4 != 0) {
			System.err.println("Error - DRI Invalid");
			header.valid = false;
		}
	}
	
	private void ReadRSTN(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading RSTN");
		// RSTN has no length
	}
	
	private void ReadAPPN(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading APPN");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		// all of APPN markers can be ignored
		for (int i = 0; i < length - 2; ++i) {
			f.readUnsignedByte();
		}
	}
	
	private void ReadComment(DataInputStream f, Header header) throws IOException {
		if (verbose) 
			System.out.println("Reading Comment");
		int length = (f.readUnsignedByte() << 8) + f.readUnsignedByte();
		
		// all comment markers can be ignored
		for (int i = 0; i < length - 2; ++i) {
			f.readUnsignedByte();
		}
	}
	
	public void queueFile(String inFile) {
		inputFiles.add(inFile);
	}
	
	
	public void sendMCU(MCU emcu) {
		if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_MCUMESSAGE_4)) {
			DataOutputBlobWriter<JPGSchema> mcuWriter = PipeWriter.outputStream(output);
			DataOutputBlobWriter.openField(mcuWriter);
			for (int i = 0; i < 64; ++i) {
				mcuWriter.writeShort(emcu.y[i]);
			}
			DataOutputBlobWriter.closeHighLevelField(mcuWriter, JPGSchema.MSG_MCUMESSAGE_4_FIELD_Y_104);
			
			DataOutputBlobWriter.openField(mcuWriter);
			for (int i = 0; i < 64; ++i) {
				mcuWriter.writeShort(emcu.cb[i]);
			}
			DataOutputBlobWriter.closeHighLevelField(mcuWriter, JPGSchema.MSG_MCUMESSAGE_4_FIELD_CB_204);
			
			DataOutputBlobWriter.openField(mcuWriter);
			for (int i = 0; i < 64; ++i) {
				mcuWriter.writeShort(emcu.cr[i]);
			}
			DataOutputBlobWriter.closeHighLevelField(mcuWriter, JPGSchema.MSG_MCUMESSAGE_4_FIELD_CR_304);
			
			
			PipeWriter.publishWrites(output);
			
			numProcessed += 1;
			if (header.restartInterval > 0 && numProcessed % header.restartInterval == 0) {
				HuffmanDecoder.restart();
			}
		}
		else {
			System.err.println("JPG Scanner requesting shutdown");
			requestShutdown();
		}
	}

	@Override
	public void run() {
		while (PipeWriter.hasRoomForWrite(output) && numProcessed < numMCUs) {
			int horizontal = header.colorComponents[0].horizontalSamplingFactor;
			int vertical = header.colorComponents[0].verticalSamplingFactor;
			if (aboutToSend != 0) {
				if (aboutToSend == 2) {
					sendMCU(mcu2);
					if (horizontal == 2 && vertical == 2) {
						aboutToSend = 4;
					}
					else {
						aboutToSend = 0;
					}
				}
				else if (aboutToSend == 3) {
					sendMCU(mcu3);
					if (horizontal == 2 && vertical == 2) {
						aboutToSend = 2;
					}
					else {
						aboutToSend = 0;
					}
				}
				else { // if (aboutToSend == 4) {
					sendMCU(mcu4);
					aboutToSend = 0;
				}
			}
			else {
				if (header.frameType.equals("Progressive")) {
					if (horizontal == 1 && vertical == 1) {
						mcu1 = mcus.get(numProcessed);
					}
					else if (horizontal == 2 && vertical == 1) {
						mcu1 = mcus.get(numProcessed);
						mcu2 = mcus.get(numProcessed + 1);
					}
					else if (horizontal == 1 && vertical == 2) {
						mcu1 = mcus.get(numProcessed);
						mcu2 = mcus.get(numProcessed + 1);
					}
					else if (horizontal == 2 && vertical == 2) {
						mcu1 = mcus.get(numProcessed);
						mcu2 = mcus.get(numProcessed + 1);
						mcu3 = mcus.get(numProcessed + 2);
						mcu4 = mcus.get(numProcessed + 3);
					}
				}
				else {
					HuffmanDecoder.decodeHuffmanData(mcu1, mcu2, mcu3, mcu4);
				}
				// write mcu to pipe
				if (horizontal == 1 && vertical == 1) {
					sendMCU(mcu1);
					aboutToSend = 0;
				}
				else if (horizontal == 2 && vertical == 1) {
					sendMCU(mcu1);
					aboutToSend = 2;
					if (PipeWriter.hasRoomForWrite(output)) {
						sendMCU(mcu2);
						aboutToSend = 0;
					}
				}
				else if (horizontal == 1 && vertical == 2) {
					sendMCU(mcu1);
					aboutToSend = 3;
					if (PipeWriter.hasRoomForWrite(output)) {
						sendMCU(mcu3);
						aboutToSend = 0;
					}
				}
				else if (horizontal == 2 && vertical == 2) {
					sendMCU(mcu1);
					aboutToSend = 3;
					if (PipeWriter.hasRoomForWrite(output)) {
						sendMCU(mcu3);
						aboutToSend = 2;
						if (PipeWriter.hasRoomForWrite(output)) {
							sendMCU(mcu2);
							aboutToSend = 4;
							if (PipeWriter.hasRoomForWrite(output)) {
								sendMCU(mcu4);
								aboutToSend = 0;
							}
						}
					}
				}
			}
		}
		if (PipeWriter.hasRoomForFragmentOfSize(output, 400) && !inputFiles.isEmpty()) {
			String file = inputFiles.get(0);
			inputFiles.remove(0);
			System.out.println(file);
			try {
				mcus = new ArrayList<MCU>();
				header = ReadJPG(file, mcus);
				if (header == null || !header.valid) {
					System.err.println("Error - JPG file '" + file + "' invalid");
					return;
				}
				if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_HEADERMESSAGE_1)) {
					// write header to pipe
					if (verbose) 
						System.out.println("JPG Scanner writing header to pipe...");
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_HEIGHT_101, header.height);
					PipeWriter.writeInt(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_WIDTH_201, header.width);
					PipeWriter.writeASCII(output, JPGSchema.MSG_HEADERMESSAGE_1_FIELD_FILENAME_301, file);
					PipeWriter.publishWrites(output);
				}
				else {
					System.err.println("JPG Scanner requesting shutdown");
					requestShutdown();
				}
				// write color component data to pipe
				for (int i = 0; i < header.numComponents; ++i) {
					if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2)) {
						if (verbose) 
							System.out.println("JPG Scanner writing color component to pipe...");
						PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_COMPONENTID_102, header.colorComponents[i].componentID);
						PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_HORIZONTALSAMPLINGFACTOR_202, header.colorComponents[i].horizontalSamplingFactor);
						PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_VERTICALSAMPLINGFACTOR_302, header.colorComponents[i].verticalSamplingFactor);
						PipeWriter.writeInt(output, JPGSchema.MSG_COLORCOMPONENTMESSAGE_2_FIELD_QUANTIZATIONTABLEID_402, header.colorComponents[i].quantizationTableID);
						PipeWriter.publishWrites(output);
					}
					else {
						System.err.println("JPG Scanner requesting shutdown");
						requestShutdown();
					}
				}
				// write quantization tables to pipe
				for (int i = 0; i < header.quantizationTables.length; ++i) {
					if (header.quantizationTables[i] != null) {
						if (PipeWriter.tryWriteFragment(output, JPGSchema.MSG_QUANTIZATIONTABLEMESSAGE_3)) {
							if (verbose) 
								System.out.println("JPG Scanner writing quantization table to pipe...");
	
							DataOutputBlobWriter<JPGSchema> quantizationTableWriter = PipeWriter.outputStream(output);
							DataOutputBlobWriter.openField(quantizationTableWriter);
							quantizationTableWriter.writeInt(header.quantizationTables[i].tableID);
							DataOutputBlobWriter.closeHighLevelField(quantizationTableWriter, JPGSchema.MSG_QUANTIZATIONTABLEMESSAGE_3_FIELD_TABLEID_103);
							
							DataOutputBlobWriter.openField(quantizationTableWriter);
							quantizationTableWriter.writeInt(header.quantizationTables[i].precision);
							DataOutputBlobWriter.closeHighLevelField(quantizationTableWriter, JPGSchema.MSG_QUANTIZATIONTABLEMESSAGE_3_FIELD_PRECISION_203);
							
							DataOutputBlobWriter.openField(quantizationTableWriter);
							for (int j = 0; j < 64; ++j) {
								quantizationTableWriter.writeInt(header.quantizationTables[i].table[j]);
							}
							DataOutputBlobWriter.closeHighLevelField(quantizationTableWriter, JPGSchema.MSG_QUANTIZATIONTABLEMESSAGE_3_FIELD_TABLE_303);
							
							PipeWriter.publishWrites(output);
						}
						else {
							System.err.println("JPG Scanner requesting shutdown");
							requestShutdown();
						}
					}
				}
				if (header.frameType.equals("Baseline")) {
					HuffmanDecoder.beginDecode(header);
				}
				mcuWidth = (header.width + 7) / 8;
				mcuHeight = (header.height + 7) / 8;
				if (header.colorComponents[0].horizontalSamplingFactor == 2 &&
					((header.width - 1) / 8 + 1) % 2 == 1) {
					mcuWidth += 1;
				}
				if (header.colorComponents[0].verticalSamplingFactor == 2 &&
					((header.height - 1) / 8 + 1) % 2 == 1) {
					mcuHeight += 1;
				}
				numMCUs = mcuWidth * mcuHeight;
				numProcessed = 0;
			}
			catch (IOException e) {
				System.err.println("Error - Unknown error reading file '" + file + "'");
			}
			if (inputFiles.isEmpty()) {
				if (verbose) 
					System.out.println("All input files read.");
			}
		}
	}
}
