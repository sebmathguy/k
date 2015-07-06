// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import com.google.common.collect.ImmutableList;
/**
 * @author Sebastian Conybeare
 */
public class FFunctionDefinition extends FDeclarable {

    private final FTypeVar domain;
    private final FTypeVar codomain;
    private final ImmutableList<FPatternBinding> cases;
    private final FVariable func;

    public FFunctionDefinition(FTarget  target,
                               FTypeVar domain,
                               FTypeVar codomain,
                               ImmutableList<FPatternBinding> cases) {
        super(target);
        this.domain = domain;
        this.codomain = codomain;
        this.cases = cases;
        func = new FVariable(target);
    }

    public FTypeVar getDomain() {
        return domain;
    }

    public FTypeVar getCodomain() {
        return codomain;
    }

    public ImmutableList<FPatternBinding> getCases() {
        return cases;
    }

    public FVariable getFFunction() {
        return func;
    }

    public void declare() {
        target.declare(this);
    }

}
