// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;

import java.util.stream.Collectors;

/**
 * @author: Sebastian Conybeare
 */
public class FADT extends FDeclarable {

    private final ImmutableList<FConstructor> constructors;
    private final FTypeVar type;

    public FADT(FTarget target, ImmutableList<FArgumentSignature> argSigs) {
        super(target);
        type = new FTypeVar(target);
        constructors = ImmutableList.copyOf(
            argSigs.stream()
            .map(argSig -> new FConstructorSignature(argSig, type))
            .map(conSig -> new FConstructor(conSig, target))
            .collect(Collectors.toList())
            );
        

    }

    public ImmutableList<FConstructor> getFConstructors() {
        return constructors;
    }

    public FTypeVar getTypeVar() {
        return type;
    }

    @Override
    public void declare() {
    }
    
}
