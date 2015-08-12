// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;

/**
 * @author Sebastian Conybeare
 */
public class FSwitch extends FExp {

    private final FExp arg;
    private final ImmutableList<FPatternBinding> cases;

    public FSwitch(FTarget target, FExp arg, ImmutableList<FPatternBinding> cases) {
        super(target);
        this.arg = arg;
        this.cases = cases;
    }

    public FExp getArgument() {
        return arg;
    }

    public ImmutableList<FPatternBinding> getCases() {
        return cases;
    }

    public String unparse() {
        return target.unparse(this);
    }

}
