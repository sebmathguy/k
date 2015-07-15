package org.kframework.backend.FAST.it;

import org.kframework.backend.FAST.*;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static java.io.File.createTempFile;

public class HaskellIT {

    @Test
    public void IntegrationTest() throws Throwable {
        Runtime rt = Runtime.getRuntime();

        // First test basic system functionality
        Process proc = rt.exec("echo Hello, World!");
        InputStreamReader hellostream = new InputStreamReader(proc.getInputStream(), "UTF-8");
        Scanner s = new Scanner(hellostream).useDelimiter("\\A");
        String hello = s.hasNext() ? s.next() : " ";
        assertEquals("Hello, World!\n", hello);

        // Does GHC exist/work?
        File codeFile = createTempFile("Koutput", ".hs");
        File execFile = createTempFile("GHCoutput", null);
//        execFile.deleteOnExit();
//        codeFile.deleteOnExit();

        codeFile.setWritable(true);
        Writer writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(codeFile.getPath()), "UTF-8"));
        writer.write("main = print \"I am an executing haskell program!\"\n");
        writer.flush();
        codeFile.setWritable(false);

        execFile.setWritable(true);
        codeFile.setReadable(true);
        
        // String ghcCommand = String.format("ghc %s -o %s -outputdir /tmp",
        //                                   codeFile.getPath(),
        //                                   execFile.getPath());
        // Process GHC = rt.exec(ghcCommand);

        ProcessBuilder GHCb = new ProcessBuilder("ghc", codeFile.getPath(),
                                                 "-o", execFile.getPath(),
                                                 "-outputdir", "/tmp");
        GHCb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));
        // FIXME: these files will only work on unix-based systems

        Process GHC = GHCb.start();
        
        InputStreamReader GHCError = new InputStreamReader(GHC.getErrorStream(), "UTF-8");
        Scanner errScan = new Scanner(GHCError).useDelimiter("\\A");
        String err = errScan.hasNext() ? errScan.next() : "no error output from ghc";
        assertEquals("no error output from ghc", err);
        assertEquals(0, GHC.waitFor());
        execFile.setWritable(false);
        execFile.setExecutable(true);
        
        Process prog = rt.exec(execFile.getCanonicalPath());
        assertEquals(0, prog.waitFor());
        InputStreamReader progstream = new InputStreamReader(
            prog.getInputStream(), "UTF-8");
        Scanner progscanner = new Scanner(progstream).useDelimiter("\\A");
        String progout = progscanner.hasNext() ? progscanner.next() : " ";
        assertEquals("\"I am an executing haskell program!\"\n", progout);
        
    }
    
}
