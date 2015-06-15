// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;
/**
 * @author: Sebastian Conybeare
 */
public class FADTProxy extends FADT {

    private FADT delegate;

    public FADTProxy(FTarget target) {
        super(target);
    }

    public ImmutableList<FConstructor> getFConstructors() {
        return delegate.getFConstructors();
    }

    public FTypeVar getTypeVar() {
        return delegate.getTypeVar();
    }

    public void declare() {
        delegate.declare();
    }

    public void setDelegate(FADT delegate) {
        this.delegate = delegate;
    }
    
}
