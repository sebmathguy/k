// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author Sebastian Conybeare
 */
public abstract class FTypeVar extends FASTNode {

    protected final FTarget target;

    protected FTypeVar(FTarget target) {
        super(target);
        this.target = target;
    }

    public abstract String getName();

    public abstract String unparse();

    public boolean equals(FTypeVar other) {
        return this.getName().equals(other.getName());
    }

}
