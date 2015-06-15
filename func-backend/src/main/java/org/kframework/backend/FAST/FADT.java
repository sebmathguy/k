// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;

import java.util.stream.Collectors;

/**
 * @author: Sebastian Conybeare
 */
public class FADT extends TypeFExp {

    private final ImmutableList<FConstructor> constructors;
    private final FTypeName name;

    public FADT(FTarget target, ImmutableList<FArgumentSignature> argSigs) {

        constructors = argSigs.stream()
            .map(argSig -> new FConstructorSignature(argSig, this))
            .map(conSig -> new FConstructor(conSig, target))
            .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                  ImmutableList::copyOf));
        
        name = new FTypeName(target);

    }

    public ImmutableList<FConstructor> getFConstructors() {
        return constructors;
    }

    public FTypeName getName() {
        return name;
    }
    
}
