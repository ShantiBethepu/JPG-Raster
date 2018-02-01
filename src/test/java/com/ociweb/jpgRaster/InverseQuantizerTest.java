package com.ociweb.jpgRaster;

import static org.junit.Assert.assertTrue;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class InverseQuantizerTest {
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
