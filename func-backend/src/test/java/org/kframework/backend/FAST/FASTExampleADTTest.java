package org.kframework.backend.FAST;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FASTExampleADTTest {
    @Test
    public void testUnit() {
        String correctOutput = "data Nat = Z | S Nat"; // FIXME(sebmathguy): probably wrong, given munging
        FTarget tgt = new HaskellFTarget();
        FADTProxy natp = new FADTProxy(tgt);
        FADT nat = (FADT) natp;
        FConstructorSignature conSigZ, conSigS;
        FArgumentSignature argSigZ = new FArgumentSignature();
        FArgumentSignature argSigS = new FArgumentSignature(ImmutableList.of(nat.getTypeVar()));
        natp.setDelegate(new FADTImpl(tgt, ImmutableList.of(argSigZ, argSigS)));
        String generatedOutput = tgt.getDeclarations();

        assertEquals(correctOutput, generatedOutput);
    }
}
