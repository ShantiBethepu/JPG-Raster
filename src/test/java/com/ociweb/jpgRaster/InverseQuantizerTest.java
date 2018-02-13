package com.ociweb.jpgRaster;

import static org.junit.Assert.assertTrue;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class InverseQuantizerTest {
    JPG.QuantizationTable qTable0;
    JPG.QuantizationTable qTable1;

    @Before
    public void setupQuantizationTable() {
        // These tables are obtained from running djpeg on jpeg_test.jpg
        // from the test_jpeg folder.
        qTable0 = new JPG.QuantizationTable();
        qTable0.tableID = 0;
        qTable0.precision = 0;
        qTable0.table = new int[]{
                2, 1, 1, 2, 3, 5, 6, 7,
                1, 1, 2, 2, 3, 7, 7, 7,
                2, 2, 2, 3, 5, 7, 8, 7,
                2, 2, 3, 3, 6, 10, 10, 7,
                2, 3, 4, 7, 8, 13, 12, 9,
                3, 4, 7, 8, 10, 12, 14, 11,
                6, 8, 9, 10, 12, 15, 14, 12,
                9, 11, 11, 12, 13, 12, 12, 12
        };

        qTable1 = new JPG.QuantizationTable();
        qTable1.tableID = 1;
        qTable1.precision = 0;
        qTable1.table = new int[]{
                2, 2, 3, 6, 12, 12, 12, 12,
                2, 3, 3, 8, 12, 12, 12, 12,
                3, 3, 7, 12, 12, 12, 12, 12,
                6, 8, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12,
                12, 12, 12, 12, 12, 12, 12, 12
        };
    }

    @Test
    public void dequantizeMCUTest() throws NoSuchMethodException {
        GraphManager graphManager = new GraphManager();
        Pipe<JPGSchema> inputPipe = JPGSchema.instance.newPipe(10, 100_000);
        Pipe<JPGSchema> outputPipe = JPGSchema.instance.newPipe(10, 100_000);

        InverseQuantizer inverseQuantizer = new InverseQuantizer(
                graphManager, inputPipe, outputPipe
        );
        Method method = InverseQuantizer.class.getDeclaredMethod(
                "dequantizeMCU",
                short[].class,
                JPG.QuantizationTable.class
        );
        method.setAccessible(true);
    }

    @Test
    public void dequantizeTest() {
//        JPG.MCU mcu;
//        JPG.Header header;
//        // Initialize MCU
//        mcu = new JPG.MCU();
//        for (int i = 0; i < 64; i++) {
//            mcu.y[i] = 0;
//            mcu.cb[i] = 0;
//            mcu.cr[i] = 0;
//        }
//        // Initialize Header
//        header = new JPG.Header();
//        header.colorComponents.set(0, JPG.ColorComponent);
//        InverseQuantizer.dequantize(mcu, header);
    }
}
