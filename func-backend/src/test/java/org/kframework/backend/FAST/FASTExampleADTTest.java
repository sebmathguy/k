package org.kframework.backend.FAST;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

@RunWith(MockitoJUnitRunner.class)
public class FASTExampleADTTest {
    @Test
    public void testUnit() {
        FTarget tgt = new HaskellFTarget();
        FADTProxy nat = new FADTProxy(tgt);
        FTypeVarProxy natTV = new FTypeVarProxy(tgt);
        FArgumentSignature argSigZ = new FArgumentSignature();
        FArgumentSignature argSigS = new FArgumentSignature(ImmutableList.of(natTV));
        nat.setDelegate(new FADTImpl(tgt, ImmutableList.of(argSigZ, argSigS)));
        natTV.setDelegate(nat.getTypeVar());
        String generatedOutput = tgt.getDeclarations();

        FTarget tgt1 = new HaskellFTarget();
        FADTProxy nat1 = new FADTProxy(tgt1);
        FTypeVarProxy natTV1 = new FTypeVarProxy(tgt1);
        FArgumentSignature argSigZ1 = new FArgumentSignature();
        FArgumentSignature argSigS1 = new FArgumentSignature(ImmutableList.of(natTV1));
        nat1.setDelegate(new FADTImpl(tgt1, ImmutableList.of(argSigZ1, argSigS1)));
        natTV1.setDelegate(nat1.getTypeVar());
        String generatedOutput1 = tgt1.getDeclarations();

        assertEquals(generatedOutput, generatedOutput1);
        Pattern p = Pattern.compile("data Type0 = Constructor0 | Constructor1 Type0");
        Matcher m = p.matcher(generatedOutput);
        assertTrue(m.find());
        
        

//        System.out.println(generatedOutput);

        // String TCName = "[:upper:][:alnum:]*";
        // String TCNameParens = String.format("(%s|\(%s\))", TCName, TCName);
        
        // Pattern p = Pattern.compile(String.format("data\\s%s\\s=\\s%s\\s\\|\\s%s\\s%s",
        //                                           TCNameParens,
        //                                           TCNameParens,
        //                                           TCNameParens,
        //                                           TCNameParens));
        // Matcher m = p.matcher(generatedOutput);
        // String nat = m.group(1);
        // String Z = m.group(2);

        // assertEquals(correctOutput, generatedOutput);
    }

}
