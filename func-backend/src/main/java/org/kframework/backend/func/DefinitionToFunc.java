// Copyright (c) 2015 K Team. All Rights Reserved.
package org.kframework.backend.func;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;

import org.kframework.attributes.Location;
import org.kframework.attributes.Source;
import org.kframework.builtin.BooleanUtils;
import org.kframework.builtin.Sorts;
import org.kframework.definition.Rule;
import org.kframework.kil.Attribute;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.KompileOptions;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KLabel;
import org.kframework.kore.KSequence;
import org.kframework.kore.KVariable;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KToken;
import org.kframework.kore.InjectedKLabel;
import org.kframework.kore.Sort;
import org.kframework.kore.ToKast;
import org.kframework.kore.compile.RewriteToTop;
import org.kframework.kore.compile.VisitKORE;
import org.kframework.kore.AbstractKORETransformer;
import org.kframework.main.GlobalOptions;
import org.kframework.utils.StringUtil;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.kframework.kore.KORE.*;
import static org.kframework.backend.func.FuncUtil.*;
import static org.kframework.backend.func.OCamlIncludes.*;

/**
 * Main class for converting KORE to functional code
 *
 * @author Remy Goldschmidt
 */
public class DefinitionToFunc {
    /** Flag that determines whether or not we annotate output OCaml with rules */
    public static final boolean annotateOutput = false;

    private final KExceptionManager kem;
    private final FileUtil files;
    private final GlobalOptions globalOptions;
    private final KompileOptions kompileOptions;

    private final XMLBuilder xml = new XMLBuilder();

    private PreprocessedKORE preproc;


    private static final SyntaxBuilder setChoiceSB1, mapChoiceSB1;
    private static final SyntaxBuilder wildcardSB, bottomSB, choiceSB, resultSB;
    private static final SyntaxBuilder equalityTestSB;

    static {
        wildcardSB = newsbv("_");
        bottomSB = newsbv("[Bottom]");
        choiceSB = newsbv("choice");
        resultSB = newsbv("result");
        equalityTestSB = newsb().addEqualityTest(choiceSB, bottomSB);
        setChoiceSB1 = choiceSB1("e", "KSet.fold", "[Set s]",
                                 "e", "result");
        mapChoiceSB1 = choiceSB1("k", "KMap.fold", "[Map m]",
                                 "k", "v", "result");
    }

    /**
     * Constructor for DefinitionToFunc
     */
    public DefinitionToFunc(KExceptionManager kem,
                            FileUtil files,
                            GlobalOptions globalOptions,
                            KompileOptions kompileOptions) {
        this.kem = kem;
        this.files = files;
        this.globalOptions = globalOptions;
        this.kompileOptions = kompileOptions;
        xml.beginXML("body");
    }

    /**
     * Convert a {@link CompiledDefinition} to an OCaml string
     */
    public String convert(CompiledDefinition def) {

        preproc = new PreprocessedKORE(def, kem, files, globalOptions, kompileOptions);
        SyntaxBuilder sb = langDefToFunc(preproc);

        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");

//        String outXML = sb.pretty().stream().collect(joining());
//        outprintfln("%s",
//                    new XMLBuilder()
//                    .beginXML("body")
//                    .append(outXML)
//                    .endXML("body")
//                    .renderSExpr(files));

        xml.endXML("body");
        outprintfln("%s", xml.renderSExpr(files));

        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");

        outprintfln("%s", sb.trackPrint());
        outprintfln(";; Number of parens: %d", sb.getNumParens());
        outprintfln(";; Number of lines:  %d", sb.getNumLines());

        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");
        outprintfln(";; DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG");

        return sb.toString();
    }

    /**
     * Convert KORE to an OCaml string that runs against the
     * code generated from the {@link CompiledDefinition}, up
     * to a certain evaluation depth.
     */
    public String convert(K k, int depth) {
        return runtimeCodeToFunc(k, depth).toString();
    }

    private SyntaxBuilder runtimeCodeToFunc(K k, int depth) {
        SyntaxBuilder sb = new SyntaxBuilder();
        FuncVisitor convVisitor = oldConvert(preproc,
                                             true,
                                             HashMultimap.create(),
                                             false);
        sb.addImport("Def");
        sb.addImport("K");
        sb.beginLetDeclaration();
        sb.beginLetDefinitions();
        String runFmt = "print_string(print_k(try(run(%s) (%s)) with Stuck c' -> c'))";
        sb.addLetEquation(newsb("_"),
                          newsbf(runFmt,
                                 convVisitor.apply(preproc.runtimeProcess(k)),
                                 depth));
        sb.endLetDefinitions();
        sb.endLetDeclaration();
        outprintfln(";; DBG: runtime # of parens: %d", sb.getNumParens());
        return sb;
    }

    private SyntaxBuilder langDefToFunc(PreprocessedKORE ppk) {
        return mainConvert(ppk);
    }

    private Function<String, String> wrapPrint(String pfx) {
        return x -> pfx + encodeStringToAlphanumeric(x);
    }

    private SyntaxBuilder addSimpleFunc(Collection<String> pats,
                                        Collection<String> vals,
                                        String args,
                                        String outType,
                                        String funcName,
                                        String matchVal) {
        List<String> pl = pats.stream().collect(toList());
        List<String> vl = vals.stream().collect(toList());
        return
            newsb()
            .addGlobalLet(newsbf("%s(%s) : %s",
                                 funcName,
                                 args,
                                 outType),
                          newsb().addMatch(newsb().addValue(matchVal),
                                           pl,
                                           vl));

    }

    private SyntaxBuilder addSimpleFunc(Collection<String> pats,
                                        Collection<String> vals,
                                        String inType,
                                        String outType,
                                        String funcName) {
        String varName = String.valueOf(inType.charAt(0));
        String arg = String.format("%s: %s", varName, inType);
        return addSimpleFunc(pats, vals, arg, outType, funcName, varName);
    }

    private <T> SyntaxBuilder addOrderFunc(Collection<T> elems,
                                           Function<T, String> print,
                                           String pfx,
                                           String tyName) {
        String fnName = String.format("order_%s", tyName);

        List<String> pats = elems.stream()
                                 .map(print)
                                 .map(wrapPrint(pfx))
                                 .collect(toList());
        List<String> vals = rangeInclusive(pats.size()).stream()
                                                       .map(x -> Integer.toString(x))
                                                       .collect(toList());

        return addSimpleFunc(pats, vals, tyName, "int", fnName);
    }

    private <T> SyntaxBuilder addPrintFunc(Collection<T> elems,
                                           Function<T, String> patPrint,
                                           Function<T, String> valPrint,
                                           String pfx,
                                           String tyName) {
        String fnName = String.format("print_%s", tyName);

        List<String> pats = elems.stream()
                                 .map(patPrint)
                                 .map(wrapPrint(pfx))
                                 .collect(toList());

        List<String> vals = elems.stream()
                                 .map(valPrint.andThen(StringUtil::enquoteCString))
                                 .collect(toList());

        return addSimpleFunc(pats, vals, tyName, "string", fnName);
    }

    private SyntaxBuilder addType(Collection<String> cons, String tyName) {
        SyntaxBuilder sb = newsb();
        sb.beginTypeDefinition(tyName);
        for(String c : cons) {
            sb.addConstructor(newsb().addConstructorName(c));
        }
        sb.endTypeDefinition();
        return sb;
    }

    private <T> SyntaxBuilder addEnumType(Collection<T> toEnum,
                                          Function<T, String> print,
                                          String pfx,
                                          String tyName) {
        List<String> cons = toEnum.stream()
                                  .map(print)
                                  .map(wrapPrint(pfx))
                                  .collect(toList());
        return addType(cons, tyName);
    }


    private SyntaxBuilder addSortType(PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();
        sb.beginTypeDefinition("sort");
        for(Sort s : ppk.definedSorts) {
            sb.addConstructor(newsb()
                              .addConstructorName(encodeStringToIdentifier(s)));
        }
        if(! ppk.definedSorts.contains(Sorts.String())) {
            sb.addConstructor(newsb()
                              .addConstructorName("SortString"));
        }
        sb.endTypeDefinition();
        return sb;
    }

    private SyntaxBuilder addKLabelType(PreprocessedKORE ppk) {
        return addEnumType(ppk.definedKLabels,
                           x -> x.name(),
                           "Lbl",
                           "klabel");
    }

    private SyntaxBuilder addSortOrderFunc(PreprocessedKORE ppk) {
        return addOrderFunc(ppk.definedSorts,
                            x -> x.name(),
                            "Sort",
                            "sort");
    }

    private SyntaxBuilder addKLabelOrderFunc(PreprocessedKORE ppk) {
        return addOrderFunc(ppk.definedKLabels,
                            x -> x.name(),
                            "Lbl",
                            "klabel");
    }

    private SyntaxBuilder addPrintSort(PreprocessedKORE ppk) {
        return addPrintFunc(ppk.definedSorts,
                            x -> x.name(),
                            x -> x.name(),
                            "Sort",
                            "sort");
    }

    private SyntaxBuilder addPrintKLabel(PreprocessedKORE ppk) {
        return addPrintFunc(ppk.definedKLabels,
                            x -> x.name(),
                            x -> ToKast.apply(x),
                            "Lbl",
                            "klabel");
    }

    private SyntaxBuilder addFunctionMatch(String functionName,
                                           KLabel functionLabel,
                                           PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();
        String hook = ppk.attrLabels
                         .get(Attribute.HOOK_KEY)
                         .getOrDefault(functionLabel, "");
        String fn = functionLabel.name();
        boolean isHook = OCamlIncludes.hooks.containsKey(hook);
        boolean isPred = OCamlIncludes.predicateRules.containsKey(fn);
        Collection<Rule> rules = ppk.functionRulesOrdered
                                    .getOrDefault(functionLabel, newArrayList());

        if(!isHook && !hook.isEmpty()) {
            kem.registerCompilerWarning("missing entry for hook " + hook);
        }

        sb.beginMatchExpression(newsbv("c"));

        if(isHook) {
            sb.append(OCamlIncludes.hooks.get(hook));
        }

        if(isPred) {
            sb.append(OCamlIncludes.predicateRules.get(fn));
        }

        int i = 0;
        for(Rule r : rules) {
            sb.append(oldConvert(ppk, r, true, i++, functionName));
        }

        sb.addMatchEquation(wildcardSB, raiseStuck(newsbv("[KApply(lbl, c)]")));
        sb.endMatchExpression();

        return sb;
    }

    private SyntaxBuilder addFunctionEquation(KLabel functionLabel,
                                              PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        String functionName = encodeStringToFunction(functionLabel.name());

        sb.beginLetrecEquation();
        sb.addLetrecEquationName(newsb()
                                 .addValue(functionName)
                                 .addSpace()
                                 .addValue("(c: k list)")
                                 .addSpace()
                                 .addValue("(guards: Guard.t)")
                                 .addSpace()
                                 .addKeyword(":")
                                 .addSpace()
                                 .addValue("k"));
        sb.beginLetrecEquationValue();
        sb.beginLetExpression();
        sb.beginLetDefinitions();
        sb.addLetEquation(newsb("lbl"),
                          newsb(encodeStringToIdentifier(functionLabel)));
        sb.endLetDefinitions();

        sb.beginLetScope();
        sb.append(addFunctionMatch(functionName, functionLabel, ppk));
        sb.endLetScope();

        sb.endLetExpression();
        sb.endLetrecEquationValue();
        sb.endLetrecEquation();

        return sb;
    }

    private SyntaxBuilder addFreshFunction(PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        sb.beginLetrecEquation();
        sb.addLetrecEquationName(newsb()
                                 .addValue("freshFunction")
                                 .addSpace()
                                 .addValue("(sort: string)")
                                 .addSpace()
                                 .addValue("(counter: Z.t)")
                                 .addSpace()
                                 .addKeyword(":")
                                 .addSpace()
                                 .addValue("k"));
        sb.beginLetrecEquationValue();

        sb.beginMatchExpression(newsb("sort"));
        for(Sort sort : ppk.freshFunctionFor.keySet()) {
            KLabel freshFunction = ppk.freshFunctionFor.get(sort);
            sb.addMatchEquation(newsbf("\"%s\"",
                                       sort.name()),
                                newsbf("(%s ([Int counter] :: []) Guard.empty)",
                                       encodeStringToFunction(freshFunction.name())));
        }
        sb.endMatchExpression();

        sb.endLetrecEquationValue();
        sb.endLetrecEquation();

        return sb;
    }

    private SyntaxBuilder addEval(Set<KLabel> labels,
                                  PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        sb.beginLetrecEquation();
        sb.addLetrecEquationName(newsb("eval (c: kitem) : k"));
        sb.beginLetrecEquationValue();

        sb.beginMatchExpression(newsb("c"));

        sb.beginMatchEquation();
        // BUG SPOT? Can patterns have arbitrary parens in them?
        sb.addMatchEquationPattern(newsb()
                                   .addApplication("KApply",
                                                   newsb("(lbl, kl)")));
        sb.beginMatchEquationValue();
        sb.beginMatchExpression(newsb("lbl"));
        for(KLabel label : labels) {
            SyntaxBuilder valSB =
                newsb().addApplication(encodeStringToFunction(label.name()),
                                       newsb("kl"),
                                       newsb("Guard.empty"));
            sb.addMatchEquation(newsb(encodeStringToIdentifier(label)),
                                valSB);
        }
        sb.endMatchExpression();
        sb.endMatchEquationValue();
        sb.endMatchEquation();

        sb.addMatchEquation(wildcardSB, newsbv("[c]"));

        sb.endMatchExpression();

        sb.endLetrecEquationValue();
        sb.endLetrecEquation();

        return sb;
    }

    private SyntaxBuilder addFunctions(PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        Set<KLabel> functions = ppk.functionSet;
        Set<KLabel> anywheres = ppk.anywhereSet;

        Set<KLabel> funcAndAny = Sets.union(functions, anywheres);

        for(List<KLabel> component : ppk.functionOrder) {
            sb.beginLetrecDeclaration();
            sb.beginLetrecDefinitions();
            for(KLabel functionLabel : component) {
                sb.append(addFunctionEquation(functionLabel, ppk));
            }
            sb.endLetrecDefinitions();
            sb.endLetrecDeclaration();
        }

        sb.beginLetrecDeclaration();
        sb.beginLetrecDefinitions();
        sb.append(addFreshFunction(ppk));
        sb.addLetrecEquationSeparator();
        sb.append(addEval(funcAndAny, ppk));
        sb.endLetrecDefinitions();
        sb.endLetrecDeclaration();

        return sb;
    }

    private SyntaxBuilder makeStuck(SyntaxBuilder body) {
        return newsb().addApplication("Stuck", body);
    }

    private SyntaxBuilder raiseStuck(SyntaxBuilder body) {
        return newsb().addApplication("raise", makeStuck(body));
    }

    private SyntaxBuilder addSteps(PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        sb.beginLetrecDeclaration();
        sb.beginLetrecDefinitions();
        sb.beginLetrecEquation();
        sb.addLetrecEquationName(newsb("lookups_step (c: k) (guards: Guard.t) : k"));
        sb.beginLetrecEquationValue();
        sb.beginMatchExpression(newsb("c"));
        int i = 0;
        for(Rule r : ppk.indexedRules.keySet()) {
            Set<String> cap = ppk.indexedRules.get(r);
            if(cap.contains("lookup") && !cap.contains("function")) {
                sb.append(oldConvert(ppk, r, false, i++, "lookups_step"));
            }
        }

        // BUG SPOT? Can raise be parenthesized?
        sb.addMatchEquation(wildcardSB, raiseStuck(newsb("c")));
        sb.endMatchExpression();
        sb.endLetrecEquationValue();
        sb.endLetrecEquation();
        sb.endLetrecDefinitions();
        sb.endLetrecDeclaration();


        sb.beginLetDeclaration();
        sb.beginLetDefinitions();
        sb.beginLetEquation();
        sb.addLetEquationName(newsb("step (c: k) : k"));
        sb.beginLetEquationValue();
        sb.beginMatchExpression(newsb("c"));
        for(Rule r : ppk.indexedRules.keySet()) {
            Set<String> cap = ppk.indexedRules.get(r);
            if(!cap.contains("lookup") && !cap.contains("function")) {
                sb.append(oldConvert(ppk, r, false, i++, "step"));
            }
        }
        sb.addMatchEquation(newsb("_"),
                            newsb("lookups_step c Guard.empty"));
        sb.endMatchExpression();
        sb.endLetEquationValue();
        sb.endLetEquation();
        sb.endLetDefinitions();
        sb.endLetDeclaration();

        return sb;
    }

    private SyntaxBuilder mainConvert(PreprocessedKORE ppk) {
        SyntaxBuilder sb = newsb();

        sb.append(addSortType(ppk));
        sb.append(addSortOrderFunc(ppk));
        sb.append(addKLabelType(ppk));
        sb.append(addKLabelOrderFunc(ppk));
        sb.append(OCamlIncludes.preludeSB);
        sb.append(addPrintSort(ppk));
        sb.append(addPrintKLabel(ppk));
        sb.append(OCamlIncludes.midludeSB);
        sb.append(addFunctions(ppk));
        sb.append(addSteps(ppk));
        sb.append(OCamlIncludes.postludeSB);

        return sb;
    }

    private SyntaxBuilder outputAnnotate(Rule r) {
        SyntaxBuilder sb = newsb();

        sb.beginMultilineComment();
        sb.appendf("rule %s requires %s ensures %s %s",
                   ToKast.apply(r.body()),
                   ToKast.apply(r.requires()),
                   ToKast.apply(r.ensures()),
                   r.att().toString());
        sb.endMultilineComment();
        sb.addNewline();

        return sb;
    }

    private SyntaxBuilder unhandledOldConvert(PreprocessedKORE ppk,
                                              Rule r,
                                              boolean isFunction,
                                              int ruleNum,
                                              String functionName) throws KEMException {
        SyntaxBuilder sb = newsb();

        if(annotateOutput) { sb.append(outputAnnotate(r)); }

        K left     = RewriteToTop.toLeft(r.body());
        K right    = RewriteToTop.toRight(r.body());
        K requires = r.requires();

        Set<String> indices = ppk.indexedRules.get(r);
        SetMultimap<KVariable, String> vars = HashMultimap.create();
        FuncVisitor visitor = oldConvert(ppk, false, vars, false);

        sb.beginMatchEquation();
        sb.beginMatchEquationPattern();

        sb.append(handleLeft(isFunction, left, visitor));

        sb.append(handleLookup(indices, ruleNum));

        SBPair side = handleSideCondition(ppk, vars, functionName, ruleNum, requires);

        sb.append(side.getFst());
        sb.endMatchEquationPattern();
        sb.beginMatchEquationValue();
        sb.append(oldConvert(ppk, true, vars, false).apply(right));
        sb.endMatchEquationValue();
        sb.endMatchEquation();
        sb.append(side.getSnd());

        return sb;
    }

    private SyntaxBuilder handleLeft(boolean isFunction, K left, FuncVisitor visitor) {
        if(isFunction) {
            return handleFunction(left, visitor);
        } else {
            return handleNonFunction(left, visitor);
        }
    }

    private SyntaxBuilder handleFunction(K left, FuncVisitor visitor) {
        KApply kapp = (KApply) ((KSequence) left).items().get(0);
        return visitor.apply(kapp.klist().items(), true);
    }

    private SyntaxBuilder handleNonFunction(K left, FuncVisitor visitor) {
        return visitor.apply(left);
    }

    private SyntaxBuilder handleLookup(Set<String> indices, int ruleNum) {
        if(indices.contains("lookup")) {
            return newsb()
                .addSpace()
                .addKeyword("when")
                .addSpace()
                .addKeyword("not")
                .addSpace()
                .beginApplication()
                .addFunction("Guard.mem")
                .beginArgument()
                .addApplication("GuardElt.Guard", newsbf("%d", ruleNum))
                .endArgument()
                .addArgument(newsb("guards"))
                .endApplication();

        } else {
            return newsb();
        }
    }

    private SBPair handleSideCondition(PreprocessedKORE ppk,
                                       SetMultimap<KVariable, String> vars,
                                       String functionName,
                                       int ruleNum,
                                       K requires) {
         SBPair convLookups = oldConvertLookups(ppk, requires, vars,
                                               functionName, ruleNum);

         SyntaxBuilder result = oldConvert(vars);

         if(hasSideCondition(requires, result.toString())) {
             SyntaxBuilder fstSB =
                 convLookups
                 .getFst()
                 .addSpace()
                 .addKeyword("when")
                 .addSpace()
                 .append(oldConvert(ppk, true, vars, true).apply(requires))
                 .addSpace()
                 .addKeyword("&&")
                 .addSpace()
                 .beginParenthesis()
                 .append(result)
                 .endParenthesis();
             SyntaxBuilder sndSB = convLookups.getSnd();
             return newSBPair(fstSB, sndSB);
         } else {
             return newSBPair(newsb(), newsb());
         }
    }

    private boolean hasSideCondition(K requires, String result) {
        return !(KSequence(BooleanUtils.TRUE).equals(requires))
            || !("true".equals(result));
    }

    private SyntaxBuilder oldConvert(PreprocessedKORE ppk,
                                     Rule r,
                                     boolean function,
                                     int ruleNum,
                                     String functionName) {
        try {
            return unhandledOldConvert(ppk, r, function, ruleNum, functionName);
        } catch (KEMException e) {
            String src = r.att()
                          .getOptional(Source.class)
                          .map(Object::toString)
                          .orElse("<none>");
            String loc = r.att()
                          .getOptional(Location.class)
                          .map(Object::toString)
                          .orElse("<none>");
            e.exception
             .addTraceFrame(String.format("while compiling rule at %s: %s",
                                          src, loc));
            throw e;
        }
    }

    private void checkApplyArity(KApply k,
                                 int arity,
                                 String funcName) throws KEMException {
        if(k.klist().size() != arity) {
            throw kemInternalErrorF(k,
                                    "Unexpected arity of %s: %s",
                                    funcName,
                                    k.klist().size());
        }
    }

    private static final SyntaxBuilder choiceSB1(String chChoiceVar,
                                                 String chFold,
                                                 String chPat,
                                                 String... chArgs) {
        return newsb()

            .beginMatchEquation()
            .addMatchEquationPattern(newsbv(chPat))
            .beginMatchEquationValue()
            .beginLetExpression()
            .beginLetDefinitions()
            .beginLetEquation()
            .addLetEquationName(choiceSB)
            .beginLetEquationValue()
            .beginApplication()
            .addFunction(chFold)
            .beginArgument()
            .beginLambda(chArgs)
            .beginConditional()
            .addConditionalIf()
            .append(equalityTestSB)
            .addConditionalThen()
            .beginMatchExpression(newsbv(chChoiceVar));
    }


    private static final SyntaxBuilder choiceSB2(String chVar,
                                                 int ruleNum,
                                                 String functionName) {
        String guardCon = "GuardElt.Guard";
        String guardAdd = "Guard.add";
        SyntaxBuilder rnsb = newsbv(Integer.toString(ruleNum));

        SyntaxBuilder guardSB =
            newsb().addApplication(guardAdd,
                                   newsb().addApplication(guardCon, rnsb),
                                   newsbv("guards"));
        SyntaxBuilder condSB =
            newsb().addConditional(equalityTestSB,
                                   newsb().addApplication(functionName,
                                                          newsbv("c"),
                                                          guardSB),
                                   choiceSB);

        return newsb()
            .addMatchEquation(wildcardSB, bottomSB)
            .endMatchExpression()
            .addConditionalElse()
            .append(resultSB)
            .endConditional()
            .endLambda()
            .endArgument()
            .addArgument(newsbv(chVar))
            .addArgument(bottomSB)
            .endApplication()
            .endLetEquationValue()
            .endLetEquation()
            .endLetDefinitions()
            .addLetScope(condSB)
            .endLetExpression()
            .endMatchEquationValue()
            .endMatchEquation();
    }

    // TODO(remy): this needs refactoring very badly
    private SBPair oldConvertLookups(PreprocessedKORE ppk,
                                     K requires,
                                     SetMultimap<KVariable, String> vars,
                                     String functionName,
                                     int ruleNum) {
        Deque<SyntaxBuilder> suffStack = new ArrayDeque<>();

        SyntaxBuilder res = new SyntaxBuilder();
        SyntaxBuilder setChoiceSB2 = choiceSB2("s", ruleNum, functionName);
        SyntaxBuilder mapChoiceSB2 = choiceSB2("m", ruleNum, functionName);
        String formatSB3 = "(%s c (Guard.add (GuardElt.Guard %d) guards))";
        SyntaxBuilder sb3 =
            newsb().addMatchEquation(wildcardSB, newsbf(formatSB3,
                                                        functionName,
                                                        ruleNum));

        new AbstractKORETransformer<Void>() {
            private SyntaxBuilder sb1;
            private SyntaxBuilder sb2;
            private String functionStr;
            private int arity;

            @Override
            public Void apply(KApply k) {
                List<K> kitems = k.klist().items();
                String klabel = k.klabel().name();

                boolean choiceOrMatch = true;

                switch(klabel) {
                case "#match":
                    functionStr = "lookup";
                    sb1 = newsb();
                    sb2 = newsb();
                    arity = 2;
                    break;
                case "#setChoice":
                    functionStr = "set choice";
                    sb1 = setChoiceSB1;
                    sb2 = setChoiceSB2;
                    arity = 2;
                    break;
                case "#mapChoice":
                    functionStr = "map choice";
                    sb1 = mapChoiceSB1;
                    sb2 = mapChoiceSB2;
                    arity = 2;
                    break;
                default:
                    choiceOrMatch = false;
                    break;
                }

                xml.beginXML("apply", choiceOrMatch ? xmlAttr("type", functionStr)
                                                    : emptyXMLAttrs());

                if(choiceOrMatch) {
                    // prettyStackTrace();

                    checkApplyArity(k, arity, functionStr);

                    K kLabel1 = kitems.get(0);
                    K kLabel2 = kitems.get(1);

                    xml.beginXML("effect", xmlAttr("name", "vars"));
                    xml.addXML("beg", vars.toString());

                    SyntaxBuilder luMatchValue
                        = oldConvert(ppk, true,  vars, false).apply(kLabel2);
                    SyntaxBuilder luLevelUp     = sb1;
                    SyntaxBuilder luPattern
                        = oldConvert(ppk, false, vars, false).apply(kLabel1);
                    SyntaxBuilder luWildcardEqn = sb3;
                    SyntaxBuilder luLevelDown   = sb2;

                    xml.addXML("end", vars.toString());
                    xml.endXML("effect");

                    xml.addXML("effect", xmlAttr("name", "res"));
                    res.endMatchEquationPattern();
                    res.beginMatchEquationValue();
                    res.beginMatchExpression(luMatchValue);
                    res.append(luLevelUp);
                    res.beginMatchEquation();
                    res.beginMatchEquationPattern();
                    res.append(luPattern);

                    xml.addXML("effect", xmlAttr("name", "suffStack"));
                    suffStack.add(luWildcardEqn);
                    suffStack.add(luLevelDown);
                }

                k.klist().items().stream().forEach(this::apply);
                xml.endXML("apply");

                return null;
            }

            @Override
            public Void apply(KRewrite k) {
                throw kemCriticalErrorF("Unexpected rewrite in requires clause:\n%s\n" +
                                        " in rule #%d, accounting for function \"%s\"",
                                        k, ruleNum, functionName);
            }

            @Override
            public Void apply(KSequence k) {
                xml.beginXML("seq");
                k.items().stream().forEach(this::apply);
                xml.endXML("seq");
                return null;
            }

            @Override
            public Void apply(KToken k) {
                xml.addXML("ktoken", xmlAttr("val", k));
                return null;
            }

            @Override
            public Void apply(KVariable k) {
                xml.addXML("kvar", xmlAttr("val", k));
                return null;
            }


            @Override
            public Void apply(InjectedKLabel k) {
                xml.addXML("inject", xmlAttr("val", k));
                return null;
            }

        }.apply(requires);

        SyntaxBuilder suffSB = new SyntaxBuilder();
        while(!suffStack.isEmpty()) { suffSB.append(suffStack.pollLast()); }

        return newSBPair(res, suffSB);
    }

    private static void prettyStackTrace() {
        Pattern funcPat = Pattern.compile("^org[.]kframework.*$");
        outprintfln(";; DBG: ----------------------------");
        outprintfln(";; DBG: apply executed:");
        StackTraceElement[] traceArray = Thread.currentThread().getStackTrace();
        List<StackTraceElement> trace = newArrayListWithCapacity(traceArray.length);

        for(StackTraceElement ste : traceArray) { trace.add(ste); }

        trace = trace
            .stream()
            .filter(x -> funcPat.matcher(x.toString()).matches())
            .collect(toList());

        int skip = 1;
        for(StackTraceElement ste : trace) {
            if(skip > 0) {
                skip--;
            } else {
                outprintfln(";; DBG: trace: %20s %6s %30s",
                            ste.getMethodName(),
                            "(" + Integer.toString(ste.getLineNumber()) + ")",
                            "(" + ste.getFileName() + ")");
            }
        }
    }

    private static SBPair newSBPair(SyntaxBuilder a, SyntaxBuilder b) {
        return new SBPair(a, b);
    }

    private static class SBPair {
        private final SyntaxBuilder fst, snd;

        public SBPair(SyntaxBuilder fst, SyntaxBuilder snd) {
            this.fst = fst;
            this.snd = snd;
        }


        public SyntaxBuilder getFst() {
            return fst;
        }

        public SyntaxBuilder getSnd() {
            return snd;
        }
    }

    private static SyntaxBuilder oldConvert(SetMultimap<KVariable, String> vars) {
        SyntaxBuilder sb = new SyntaxBuilder();
        for(Collection<String> nonLinearVars : vars.asMap().values()) {
            if(nonLinearVars.size() < 2) { continue; }
            Iterator<String> iter = nonLinearVars.iterator();
            String last = iter.next();
            while (iter.hasNext()) {
                //handle nonlinear variables in pattern
                String next = iter.next();
                // BUG SPOT? Parenthesizes last and next
                sb.addApplication("eq", newsb(last), newsb(next));
                last = next;
                sb.addSpace();
                sb.addKeyword("&&");
                sb.addSpace();
            }
        }
        sb.addValue("true");
        return sb;
    }

    private FuncVisitor oldConvert(PreprocessedKORE ppk,
                                   boolean rhs,
                                   SetMultimap<KVariable, String> vars,
                                   boolean useNativeBooleanExp) {
        return new FuncVisitor(ppk, rhs, vars, useNativeBooleanExp);
    }
}
