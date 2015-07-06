// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;

import java.util.stream.Collectors;

/**
 * @author Sebastian Conybeare
 */
public class FADTImpl extends FADT {

    private final ImmutableList<FConstructor> constructors;
    private final FTypeVar type;

    public FADTImpl(FTarget target, ImmutableList<FArgumentSignature> argSigs) {
        super(target);
        type = new FTypeVarImpl(target);
        constructors = ImmutableList.copyOf(
            argSigs.stream()
            .map(argSig -> new FConstructorSignature(argSig, type))
            .map(conSig -> new FConstructor(conSig, target))
            .collect(Collectors.toList())
            );
        target.declare(this);
    }

    @Override
    public ImmutableList<FConstructor> getFConstructors() {
        return constructors;
    }

    @Override
    public FTypeVar getTypeVar() {
        return type;
    }

}
