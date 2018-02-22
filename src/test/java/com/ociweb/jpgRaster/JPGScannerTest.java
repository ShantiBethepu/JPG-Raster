package com.ociweb.jpgRaster;

import static org.junit.Assert.assertTrue;

import com.ociweb.pronghorn.pipe.Pipe;
import com.ociweb.pronghorn.stage.scheduling.GraphManager;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class JPGScannerTest {
    @Before
    public void initialize() {

    }

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
        int[] quantizationTable = {
                2, 1, 1, 2, 3, 5, 6, 7,
                1, 1, 2, 2, 3, 7, 7, 7,
                2, 2, 2, 3, 5, 7, 8, 7,
                2, 2, 3, 3, 6, 10, 10, 7,
                2, 3, 4, 7, 8, 13, 12, 9,
                3, 4, 7, 8, 10, 12, 14, 11,
                6, 8, 9, 10, 12, 15, 14, 12,
                9, 11, 11, 12, 13, 12, 12, 12
        };
        int[] quantizationTable2 = {
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
