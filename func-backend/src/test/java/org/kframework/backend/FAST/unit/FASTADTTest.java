package org.kframework.backend.FAST.unit;

import org.kframework.backend.FAST.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.assertThat;
// import org.hamcrest.core.StringContains;

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

        String subRegex = "[A-Z][\\w']*"; // Matches valid uppercase identifiers
        String natRegex = String.format(
            "\\s*data (%s) = (?!\\1 )(%s) \\| (?!\\1 |\\2 )%s \\1;\\s*",
            subRegex, subRegex, subRegex);

        assertThat(generatedOutput, RegexMatcher.matchesRegex(natRegex));
    }

}
