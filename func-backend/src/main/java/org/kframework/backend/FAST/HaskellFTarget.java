// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

/**
 * @author: Sebastian Conybeare
 */
public abstract class HaskellFTarget extends FTarget {
    private int constructorNameCount;
    private int typeNameCount;
    private int variableNameCount;
    private String declarations;
    
    public HaskellFTarget() {
        constructorNameCount = 0;
        typeNameCount = 0;
        variableNameCount = 0;
    }

    @Override
    public String unparse(FApp a) {
        return String.format("(%s) (%s)", a.getFunction().unparse(), a.getArgument().unparse());
    }

    @Override
    public String unparse(FConstructor c) {
        return c.getFConstructorName().toString();
    }

    @Override
    public String unparse(FIf i) {
        return String.format("if (%s) then (%s) else (%s)",
                             i.getCondition().unparse(),
                             i.getTrueBranch().unparse(),
                             i.getFalseBranch().unparse());
    }

    @Override
    public String unparse(FLitBool b) {
        return b.getValue() ? "True" : "False";
    }

    @Override
    public String unparse(FLitInt i) {
        return String.format("%i", i.getValue());
    }

    @Override
    public String unparse(FLitString s) {
        return String.format("\"%s\"", s.getValue());
    }

    @Override
    public String unparse(FVariable v) {
        return v.toString();
    }

    @Override
    public String newFConstructorName() {
        synchronized(this) {
            return String.format("Constructor%i", constructorNameCount++);
        }
    }

    @Override
    public String newFTypeName() {
        synchronized(this) {
            return String.format("Type%i", typeNameCount++);
        }
    }

    @Override
    public String newFVariable() {
        synchronized(this) {
            return String.format("var%i", variableNameCount++);
        }
    }

    @Override
    public void declare(FFunctionDefinition a) {
        String f = a.getFFunction().unparse();
        String dom = a.getDomain().unparse();
        String codom = a.getCodomain().unparse();
        String typeSignatureDecl = String.format("%s :: %s -> %s", f, dom, codom);
        FVariable x = new FVariable(this);
        FSwitch cases = new FSwitch(this, x, a.getCases());
        String funcDef = String.format("%s %s = %s",
                                       f, x.unparse(), cases.unparse());
        
        declarations = String.format("%s%s\n%s\n",
                                     declarations,
                                     typeSignatureDecl,
                                     funcDef);
    }

    @Override
    public String getDeclarations() {
        return declarations;
    }

}
