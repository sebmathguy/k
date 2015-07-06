// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author Sebastian Conybeare
 */
public class LiteralFPattern extends FPattern {

    private final FExp e;

    public LiteralFPattern(FTarget target, FExp e) {
        super(target);
        this.e = e;
    }

    public FExp getFExp() {
        return e;
    }

    @Override
    public String unparse() {
        return target.unparse(this);
    }
    
}
