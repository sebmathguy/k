// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author: Sebastian Conybeare
 */
public class FTypeVar extends FASTNode {

    private final FTypeName name;

    public FTypeVar(FTarget target) {
        super(target);
        name = new FTypeName(target);
    }

    public String getName() {
        return name.toString();
    }

    public String unparse() {
        return target.unparse(this);
    }
    
}
