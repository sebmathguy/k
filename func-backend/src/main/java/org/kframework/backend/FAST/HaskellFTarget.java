// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.backend.FAST;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author: Sebastian Conybeare
 */
public class HaskellFTarget extends FTarget {
    private int constructorNameCount = 0;
    private int typeNameCount = 0;
    private int variableNameCount = 0;

    private final Object constructorLock = new Object();
    private final Object typeLock = new Object();
    private final Object variableLock = new Object();

    private final Object declarationLock = new Object();

    private String typeDeclarations = "";
    private String functionDeclarations = "";
    private boolean declarationsValid = false;

    private final HashSet<FADT> newTypes = new HashSet();
    private final HashSet<FFunctionDefinition> newFunctions = new HashSet();
    
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
    public String unparse(VarFPattern p) {
        return p.getFVariable().unparse();
    }

    @Override
    public String unparse(ConstructorFPattern p) {
        return String.format("(%s) (%s)",
                             p.getFConstructor().unparse(),
                             p.getArgs().stream()
                             .map(x -> String.format("(%s)", x.unparse()))
                             .collect(Collectors.joining(" "))
            );
    }

    @Override
    public String unparse(WildcardFPattern p) {
        return "_";
    }

    @Override
    public String unparse(FSwitch s) {
        return "switch statements not implemented"; //TODO switch statements
    }

    @Override
    public String unparse(FTypeVar t) {
        return t.getName();
    }

    @Override
    public String newFConstructorName() {
        synchronized(constructorLock) {
            return String.format("Constructor%i", constructorNameCount++);
        }
    }

    @Override
    public String newFTypeName() {
        synchronized(typeLock) {
            return String.format("Type%i", typeNameCount++);
        }
    }

    @Override
    public String newFVariable() {
        synchronized(variableLock) {
            return String.format("var%i", variableNameCount++);
        }
    }

    private String serializeDeclaration(FFunctionDefinition a) {
        String f = a.getFFunction().unparse();
        String dom = a.getDomain().unparse();
        String codom = a.getCodomain().unparse();
        String typeSignatureDecl = String.format("%s :: %s -> %s", f, dom, codom);
//        ImmutableList<FPatternBinding> cases = new FSwitch(this, x, a.getCases());
        String funcDef = a.getCases().stream()
            .map(binding -> String.format("%s (%s) = %s", f,
                                          binding.getLHS().unparse(),
                                          binding.getRHS().unparse()))
            .collect(Collectors.joining("\n"));
        String newDeclaration = String.format("%s\n%s\n",
                                              typeSignatureDecl,
                                              funcDef);
        return newDeclaration;
    }

    @Override
    public void declare(FFunctionDefinition f) {
        synchronized(declarationLock) {
            declarationsValid = false;
            newFunctions.add(f);
        }
    }

    @Override
    public void declare(FADT a) {
        synchronized(declarationLock) {
            declarationsValid = false;
            newTypes.add(a);
        }
    }

    private String getFunctionDeclarations() {
        synchronized(declarationLock) {
            if(declarationsValid) {
                return functionDeclarations;
            } else {
                // TODO add type definitions
                String newFunctionDeclarations = newFunctions.stream()
                    .map(f -> serializeDeclaration(f))
                    .collect(Collectors.joining("\n"));
                String allFunctionDeclarations = String.format("%s\n%s", functionDeclarations, newFunctionDeclarations);
                declarationsValid = true;
                functionDeclarations = allFunctionDeclarations;
                return functionDeclarations;
            }
        }
    }

    private String getTypeDeclarations() {
        return "";
    }

    public String getDeclarations() {
        return String.format("%s\n%s", getTypeDeclarations(), getFunctionDeclarations());
    }
}
