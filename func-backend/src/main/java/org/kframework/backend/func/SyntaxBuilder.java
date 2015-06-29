package org.kframework.backend.func;

import java.util.List;

/**
 * This class is a temporary way to make the current
 * functional backend work with code adapted from the
 * OCaml backend
 * @author: Remy Goldschmidt
 */
public class SyntaxBuilder {
    private final StringBuilder sb;

    public SyntaxBuilder(StringBuilder s) {
        sb = s;
    }

    public SyntaxBuilder() {
        this(new StringBuilder());
    }

    public void append(String s) {
        sb.append(s);
    }

    public void appendf(String format, Object... args) {
        append(String.format(format, args));
    }

    public StringBuilder getStringBuilder() {
        return sb;
    }

    public String render() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }




    public void addKeyword(String keyword) {
        append(keyword);
    }

    public void addValue(String value) {
        append(value);
    }

    public void addSpace() {
        addKeyword(" ");
    }

    public void addApplication(String fnName, String... args) {
        beginApplication();
        addFunction(fnName);
        for(String a : args) {
            addArgument(a);
        }
        endApplication();
    }

    public void beginApplication() {
        beginParenthesis();
    }

    public void endApplication() {
        endParenthesis();
    }

    public void addFunction(String fnName) {
        addValue(fnName);
    }

    public void addArgument(String arg) {
        beginArgument();
        addValue(arg);
        endArgument();
    }

    public void beginArgument() {
        addSpace();
        beginParenthesis();
    }

    public void endArgument() {
        endParenthesis();
    }

    public void addConditionalIf() {
        addSpace();
        addKeyword("if");
        addSpace();
    }

    public void addConditionalThen() {
        addSpace();
        addKeyword("then");
        addSpace();
    }

    public void addConditionalElse() {
        addSpace();
        addKeyword("else");
        addSpace();
    }

    public void beginParenthesis() {
        addKeyword("(");
    }

    public void endParenthesis() {
        addKeyword(")");
    }



    public void addGlobalLet(String name, String value) {
        beginLetExpression();
        beginLetDefinitions();
        addLetEquation(name, value);
        endLetDefinitions();
        endLetExpression();
    }

    public void addLetEquation(String name, String value) {
        beginLetEquation();
        addLetEquationName(name);
        addLetEquationValue(value);
        endLetEquation();
    }

    public void addLetScope(String scope) {
        beginLetScope();
        addValue(scope);
        endLetScope();
    }

    public void addLetEquationName(String name) {
        beginLetEquationName();
        addValue(name);
        endLetEquationName();
    }

    public void addLetEquationValue(String value) {
        beginLetEquationValue();
        addValue(value);
        endLetEquationValue();
    }

    public void beginLetEquation() {
        // Begin let equation
    }

    public void endLetEquation() {
        // End let equation
    }

    public void beginLetEquationName() {
        // Begin let equation name
    }

    public void endLetEquationName() {
        addSpace();
        addKeyword("=");
        addSpace();
    }

    public void beginLetEquationValue() {
        // Begin let equation value
    }

    public void endLetEquationValue() {
        // End let equation value
    }

    public void addLetEquationSeparator() {
        addNewline();
        addSpace();
        addKeyword("and");
        addSpace();
    }

    public void beginLetDefinitions() {
        // Begin let definitions
    }

    public void endLetDefinitions() {
        // End let definitions
    }

    public void beginLetScope() {
        addSpace();
        addKeyword("in");
        addSpace();
    }

    public void endLetScope() {
        // End let scope
    }

    public void beginLetExpression() {
        addKeyword("let");
        addSpace();
    }

    public void endLetExpression() {
        // End let expression
    }



    public void addLetrecEquation(String name, String value) {
        beginLetrecEquation();
        addLetrecEquationName(name);
        addLetrecEquationValue(value);
        endLetrecEquation();
    }

    public void addLetrecEquationName(String name) {
        beginLetrecEquationName();
        addValue(name);
        endLetrecEquationName();
    }

    public void addLetrecEquationValue(String value) {
        beginLetrecEquationValue();
        addValue(value);
        endLetrecEquationValue();
    }

    public void beginLetrecEquation() {
        // Begin letrec equation
    }

    public void endLetrecEquation() {
        addNewline();
    }

    public void beginLetrecEquationName() {
        // Begin letrec equation name
    }

    public void endLetrecEquationName() {
        addSpace();
        addKeyword("=");
        addSpace();
    }

    public void beginLetrecEquationValue() {
        // Begin letrec equation value
    }

    public void endLetrecEquationValue() {
        // End letrec equation value
    }

    public void addLetrecEquationSeparator() {
        addNewline();
        addSpace();
        addKeyword("and");
        addSpace();
    }

    public void beginLetrecDefinitions() {
        // Begin letrec definitions
    }

    public void endLetrecDefinitions() {
        // End letrec definitions
    }

    public void beginLetrecScope() {
        addSpace();
        addKeyword("in");
        addSpace();
    }

    public void endLetrecScope() {
        // End letrec scope
    }

    public void beginLetrecExpression() {
        addKeyword("let rec");
        addSpace();
    }

    public void endLetrecExpression() {
        // End letrec expression
    }




    public void addMatch(String value, List<String> pats, List<String> vals) {
        beginMatchExpression(value);
        for(int i = Math.min(pats.size(), vals.size()) - 1; i >= 0; i--) {
            addMatchEquation(pats.get(i), vals.get(i));
        }
        endMatchExpression();
    }

    public void addMatchEquation(String pattern, String value) {
        beginMatchEquation();
        addMatchEquationPattern(pattern);
        addMatchEquationValue(value);
        endMatchEquation();
    }

    public void addMatchEquation(String equation) {
        beginMatchEquation();
        addValue(equation);
        endMatchEquation();
    }

    public void addMatchEquationPattern(String pattern) {
        beginMatchEquationPattern();
        addValue(pattern);
        endMatchEquationPattern();
    }

    public void addMatchEquationValue(String value) {
        beginMatchEquationValue();
        addValue(value);
        endMatchEquationValue();
    }

    public void beginMatchExpression(String varname) {
        beginParenthesis();
        addKeyword("match");
        addSpace();
        addValue(varname);
        addSpace();
        addKeyword("with");
        addSpace();
    }

    public void endMatchExpression() {
        endParenthesis();
        addNewline();
    }

    public void beginMatchEquation() {
        addKeyword("|");
        addSpace();
    }

    public void endMatchEquation() {
        addNewline();
    }

    public void beginMatchEquationPattern() {
        // Begin match equation pattern
    }

    public void endMatchEquationPattern() {
        addSpace();
        addKeyword("->");
        addSpace();
    }

    public void beginMatchEquationValue() {
        // Begin match equation value
    }

    public void endMatchEquationValue() {
        // End match equation value
    }




    public void beginMultilineComment() {
        addKeyword("(*");
    }

    public void endMultilineComment() {
        addKeyword("*)");
    }

    public void addNewline() {
        addKeyword("\n");
    }

    public void addImport(String i) {
        addKeyword("open");
        addSpace();
        addValue(i);
        addNewline();
    }




    public void beginTypeDefinition(String typename) {
        addKeyword("type");
        addSpace();
        addValue(typename);
        addSpace();
        addKeyword("=");
        addSpace();
        addNewline();
    }

    public void endTypeDefinition() {
        // End type definition
    }

    public void addConstructor(String con) {
        beginConstructor();
        append(con);
        endConstructor();
    }

    public void beginConstructor() {
        addKeyword("|");
        addSpace();
    }

    public void endConstructor() {
        addNewline();
    }

    public void beginConstructorName() {
        // Begin constructor name
    }

    public void endConstructorName() {
        // End constructor name
    }

    public void beginConstructorArgs() {
        addSpace();
        addKeyword("of");
        addSpace();
    }

    public void endConstructorArgs() {
        // End constructor args
    }

    public void addType(String typename) {
        addValue(typename);
    }

    public void addTypeProduct() {
        addSpace();
        addKeyword("*");
        addSpace();
    }
}