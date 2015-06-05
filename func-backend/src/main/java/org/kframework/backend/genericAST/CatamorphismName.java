// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.genericAST;

/**
 * @author: Sebastian Conybeare
 */
public class CatamorphismName extends Identifier {

    public CatamorphismName(Target target) {
        super(target.newCatamorphismName());
    }

}
