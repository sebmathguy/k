// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;
/**
 * @author Sebastian Conybeare
 */
public abstract class FADT extends FDeclarable {

    protected final FTarget target;

    protected FADT(FTarget target) {
        super(target);
        this.target = target;
    }

    public abstract ImmutableList<FConstructor> getFConstructors();

    public abstract FTypeVar getTypeVar();

    public boolean equals(FADT other) {
        return this.getTypeVar().equals(other.getTypeVar());
    }

}
