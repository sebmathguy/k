// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author Sebastian Conybeare
 */
public class FTypeVarProxy extends FTypeVar {

    private FTypeVar delegate;

    public FTypeVarProxy(FTarget target) {
        super(target);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String unparse() {
        return delegate.unparse();
    }

    public void setDelegate(FTypeVar delegate) {
        this.delegate = delegate;
    }
    
}
