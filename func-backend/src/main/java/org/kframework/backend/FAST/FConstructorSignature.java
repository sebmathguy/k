// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;
/**
 * @author Sebastian Conybeare
 */
public class FConstructorSignature {

    private final FArgumentSignature argTypes;
    private FTypeVar retType;

    public FConstructorSignature(FArgumentSignature argumentTypes, FTypeVar returnType) {
        argTypes = argumentTypes;
        retType = returnType;
    }

    public FConstructorSignature(ImmutableList<FTypeVar> argumentTypes, FTypeVar returnType) {
        argTypes = new FArgumentSignature(argumentTypes);
        retType = returnType;
    }

    public FConstructorSignature(FTypeVar argumentType) {
        argTypes = new FArgumentSignature(argumentType);
    }

    public FArgumentSignature getFArgumentSignature() {
        return argTypes;
    }

    public ImmutableList<FTypeVar> getArgumentTypes() {
        return argTypes.getArgumentTypes();
    }

    public FTypeVar getReturnType() {
        return retType;
    }
    
}
