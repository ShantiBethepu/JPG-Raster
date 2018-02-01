package com.ociweb.jpgRaster;

import static org.junit.Assert.assertTrue;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import org.junit.Test;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class JPGScannerTest {

    @Test
    public void queueFileTest() throws NoSuchFieldException, IllegalAccessException {
        GraphManager graphManager = new GraphManager();
        Pipe<JPGSchema> outputPipe = JPGSchema.instance.newPipe(10, 100_000);
        JPGScanner jpgScanner = new JPGScanner(
                graphManager, outputPipe
        );
        Field field = JPGScanner.class.getDeclaredField("inputFiles");
        field.setAccessible(true);

        String filename = "Test Filename";


        ArrayList<String> inputFiles = (ArrayList<String>) field.get(jpgScanner);
        assertTrue(inputFiles.size() == 0);
        jpgScanner.queueFile(filename);
        inputFiles = (ArrayList<String>) field.get(jpgScanner);
        assertTrue(inputFiles.size() == 1);
        assertTrue(inputFiles.get(0).equals(filename));
    }

    @Test
    public void ReadQuantizationTableTest() {

    }

    @Test
    public void ReadStartOfFrameTest() {

    }

    @Test
    public void ReadHuffmanTableTest() {

    }

    @Test
    public void ReadStartOfScanTest() {

    }

    @Test
    public void ReadRestartIntervalTest() {

    }

    @Test
    public void ReadRSTNTest() throws NoSuchMethodException {
        GraphManager graphManager = new GraphManager();
        Pipe<JPGSchema> outputPipe = JPGSchema.instance.newPipe(10, 100_000);
        JPGScanner jpgScanner = new JPGScanner(
                graphManager, outputPipe
        );
        Method method = JPGScanner.class.getDeclaredMethod(
                "ReadRSTN", DataInputStream.class, JPG.Header.class
        );
        method.setAccessible(true);
        // Does nothing right now
    }

    @Test
    public void ReadAPPNTest() {

    }

    @Test
    public void ReadCommentTest() {

    }

    @Test
    public void ReadJPEGTest() {

    }
}
