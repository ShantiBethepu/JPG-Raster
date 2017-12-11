package com.ociweb.jpgRaster;

import java.util.ArrayList;

public class JPG {
	public static class QuantizationTable {
		short tableID;
		short precision;
		// using ints instead of shorts because
		//  precision might be 16 instead of 8
		int[] table = new int[64];
	}
	
	public static class HuffmanTable {
		short tableID;
		ArrayList<ArrayList<Short>> symbols = new ArrayList<ArrayList<Short>>(16);
	}
	
	public static class ColorComponent {
		short componentID;
		short horizontalSamplingFactor;
		short verticalSamplingFactor;
		short quantizationTableID;
		short huffmanACTableID;
		short huffmanDCTableID;
	}
	
	public static class Header {
		// DQT
		ArrayList<QuantizationTable> quantizationTables = new ArrayList<QuantizationTable>(4);
		
		// DHT
		ArrayList<HuffmanTable> huffmanDCTables = new ArrayList<HuffmanTable>(2);
		ArrayList<HuffmanTable> huffmanACTables = new ArrayList<HuffmanTable>(2);
		
		// SOF
		String frameType;
		short precision;
		int height;
		int width;
		
		// SOS
		short startOfSelection;
		short endOfSelection;
		short successvieApproximation;

		ArrayList<ColorComponent> colorComponents = new ArrayList<ColorComponent>(3);
		ArrayList<Short> imageData = new ArrayList<Short>();
		
		Boolean valid = true;
	}
	
	public static class MCU {
		int yDc;
		int cbDc;
		int crDc;
		
		short[] yAc = new short[64];
		short[] cbAc = new short[64];
		short[] crAc = new short[64];
	}

	public static class RGB {
		public short r;
		public short g;
		public short b;
	}
}
