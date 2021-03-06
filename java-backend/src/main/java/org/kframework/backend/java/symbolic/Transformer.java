// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.builtins.*;
import org.kframework.backend.java.kil.*;
import org.kframework.kil.ASTNode;


/**
 * Interface for a visitor pattern which constructs a new AST node.
 *
 * @author AndreiS
 */
public interface Transformer {

    public String getName();

    public ASTNode transform(BitVector bitVector);
    public ASTNode transform(BoolToken boolToken);
    public ASTNode transform(BuiltinList builtinList);
    public ASTNode transform(BuiltinMap builtinMap);
    public ASTNode transform(BuiltinSet builtinSet);
    public ASTNode transform(CellCollection cellCollection);
    public ASTNode transform(Collection collection);
    public ASTNode transform(ConjunctiveFormula conjunctiveFormula);
    public ASTNode transform(ConstrainedTerm constrainedTerm);
    public ASTNode transform(DisjunctiveFormula disjunctiveFormula);
    public ASTNode transform(FloatToken floatToken);
    public ASTNode transform(Hole hole);
    public ASTNode transform(IntToken intToken);
    public ASTNode transform(KLabelConstant kLabelConstant);
    public ASTNode transform(KLabelFreezer kLabelFreezer);
    public ASTNode transform(KLabelInjection kLabelInjection);
    public ASTNode transform(KItemProjection kItemProjection);
    public ASTNode transform(KItem kItem);
    public ASTNode transform(KCollection kCollection);
    public ASTNode transform(KLabel kLabel);
    public ASTNode transform(KList kList);
    public ASTNode transform(KSequence kSequence);
    public ASTNode transform(MetaVariable metaVariable);
    public ASTNode transform(Rule rule);
    public ASTNode transform(StringToken stringToken);
    public ASTNode transform(Term node);
    public ASTNode transform(Token token);
    public ASTNode transform(UninterpretedToken uninterpretedToken);
    public ASTNode transform(Variable variable);
    public ASTNode transform(InjectedKLabel injectedKLabel);
}
