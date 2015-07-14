package org.kframework.backend.FAST;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.core.StringContains;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.runners.MockitoJUnitRunner;

// import java.util.regex.Pattern;
// import java.util.regex.Matcher;

@RunWith(MockitoJUnitRunner.class)
public class FASTADTTest {
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

        StringContains substm = new StringContains(
            "data Type0 = Constructor0 | Constructor1 Type0");
        assertThat(generatedOutput, substm);
        
        // Pattern p = Pattern.compile("data Type0 = Constructor0 \\| Constructor1 Type0");
        // Matcher m = p.matcher(generatedOutput);
        // if(! m.find()) {
        //     assertEquals("", generatedOutput);
        // }
    }

}
