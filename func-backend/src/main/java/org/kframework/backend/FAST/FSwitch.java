// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author: Sebastian Conybeare
 */
public class FSwitch extends FExp {

    private final FExp arg;
    private final FMatch cases;

    public FSwitch(FTarget target, FExp arg, FMatch cases) {
        super(target);
        this.arg = arg;
        this.cases = cases;
    }

    public FExp getArgument() {
        return arg;
    }

    public FMatch getCases() {
        return cases;
    }

    @Override
    public String unparse() {
        return target.unparse(this);
    }
    
}
