// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;

/**
 * @author Sebastian Conybeare
 */
public class FArgumentSignature {

    private final ImmutableList<FTypeVar> argTypes;

    public FArgumentSignature(ImmutableList<FTypeVar> argumentTypes) {
        argTypes = argumentTypes;
    }

    public FArgumentSignature() {
        argTypes = ImmutableList.of();
    }

    public FArgumentSignature(FTypeVar argumentType) {
        argTypes = ImmutableList.of(argumentType);
    }

    public ImmutableList<FTypeVar> getArgumentTypes() {
        return argTypes;
    }

}
