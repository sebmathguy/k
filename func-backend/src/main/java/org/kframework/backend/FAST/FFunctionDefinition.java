// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author: Sebastian Conybeare
 */
public class FFunctionDefinition extends FDeclarable {

    private final TypeFExp domain;
    private final TypeFExp codomain;
    private final FMatch cases;
    private final FVariable func;

    public FFunctionDefinition(FTarget  target,
                               TypeFExp domain,
                               TypeFExp codomain,
                               FMatch   cases) {
        super(target);
        this.domain = domain;
        this.codomain = codomain;
        this.cases = cases;
        func = new FVariable(target);
    }

    public TypeFExp getDomain() {
        return domain;
    }

    public TypeFExp getCodomain() {
        return codomain;
    }

    public FMatch getCases() {
        return cases;
    }

    public FVariable getFFunction() {
        return func;
    }

    public String declare() {
        return target.declare(this);
    }

}
