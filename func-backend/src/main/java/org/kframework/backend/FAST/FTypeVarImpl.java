// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author Sebastian Conybeare
 */
public class FTypeVarImpl extends FTypeVar {

    private final FTypeName name;

    public FTypeVarImpl(FTarget target) {
        super(target);
        name = new FTypeName(target);
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public String unparse() {
        return target.unparse(this);
    }
    
}
