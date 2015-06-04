package org.kframework.backend.func;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.kframework.attributes.Location;
import org.kframework.attributes.Source;
import org.kframework.builtin.BooleanUtils;
import org.kframework.builtin.Sorts;
import org.kframework.definition.Module;
import org.kframework.definition.Production;
import org.kframework.definition.Rule;
import org.kframework.kil.Attribute;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.KompileOptions;
import org.kframework.kore.InjectedKLabel;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KLabel;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KSequence;
import org.kframework.kore.KToken;
import org.kframework.kore.KVariable;
import org.kframework.kore.Sort;
import org.kframework.kore.ToKast;
import org.kframework.kore.compile.RewriteToTop;
import org.kframework.kore.compile.VisitKORE;
import org.kframework.main.GlobalOptions;
import org.kframework.utils.StringUtil;
import org.kframework.utils.algorithms.SCCTarjan;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.kframework.Collections.*;
import static org.kframework.kore.KORE.*;
import static scala.compat.java8.JFunction.*;
import static org.kframework.backend.func.OcamlIncludes.*;

/*
 * @author: Remy Goldschmidt
 */

public class DefinitionToFunc {
    public static final boolean annotateOutput = true;

    private final KExceptionManager kem;
    private final FileUtil files;
    private final GlobalOptions globalOptions;
    private final KompileOptions kompileOptions;

    private FuncPreprocessors preproc;
    private Module mainModule;

    private Set<KLabel> functionSet;
    private SetMultimap<KLabel, Rule> functionRules;
    private List<List<KLabel>> functionOrder;

    public DefinitionToFunc(KExceptionManager kem,
                            FileUtil files,
                            GlobalOptions globalOptions,
                            KompileOptions kompileOptions) {
        this.kem = kem;
        this.files = files;
        this.globalOptions = globalOptions;
        this.kompileOptions = kompileOptions;
    }

    private FuncAST runtimeCodeToFunc(K k, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("open Def");
        addNewline(sb);
        sb.append("open K");
        addNewline(sb);
        sb.append("open Big_int");
        addNewline(sb);
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "_");
        beginLetEquationValue(sb);
        sb.append("print_string(print_k(try(run(");
        Visitor convVisitor = oldConvert(sb, true, HashMultimap.create(), false);
        convVisitor.apply(preproc.korePreprocess(k));
        sb.append(") (");
        sb.append(depth);
        sb.append(")) with Stuck c' -> c'))");
        endLetEquationValue(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
        return new FuncAST(sb.toString());
    }

    private FuncAST langDefToFunc(CompiledDefinition def) {
        return new FuncAST(oldConvert());
    }

    public String convert(CompiledDefinition def) {
        preproc = new FuncPreprocessors(def, kem, files, globalOptions, kompileOptions);
        mainModule = preproc.modulePreprocess();
        return langDefToFunc(def).render();
    }

    public String convert(K k, int depth) {
        return runtimeCodeToFunc(k, depth).render();
    }

    private void addNewline(StringBuilder sb) {
        sb.append("\n");
    }
    
    private void addPrelude(StringBuilder sb) {
        sb.append(prelude);
    }

    private void addMidlude(StringBuilder sb) {
        sb.append(midlude);
    }

    private void addPostlude(StringBuilder sb) {
        sb.append(postlude);
    }
    
    private void beginTypeDefinition(StringBuilder sb, String typename) {
        sb.append("type ");
        sb.append(typename);
        sb.append(" = ");
    }

    private void endTypeDefinition(StringBuilder sb) {
        // End type definition
    }

    private void addConstructor(StringBuilder sb, String con) {
        beginConstructor(sb);
        sb.append(con);
        endConstructor(sb);
    }

    private void addConstructorSum(StringBuilder sb) {
        sb.append("|");
    }

    private void beginConstructor(StringBuilder sb) {
        sb.append("|");
    }

    private void endConstructor(StringBuilder sb) {
        // End constructor
    }

    private void beginConstructorName(StringBuilder sb) {
        // Begin constructor name
    }
    
    private void endConstructorName(StringBuilder sb) {
        // End constructor name
    }

    private void beginConstructorArgs(StringBuilder sb) {
        sb.append(" of ");
    }
    
    private void endConstructorArgs(StringBuilder sb) {
        // End constructor args
    }

    private void addType(StringBuilder sb, String typename) {
        sb.append(typename);
    }
    
    private void addTypeProduct(StringBuilder sb) {
        sb.append(" * ");
    }
    
    private void addSortType(StringBuilder sb) {
        beginTypeDefinition(sb, "sort");
        addNewline(sb);

        for (Sort s : iterable(mainModule.definedSorts())) {
            beginConstructor(sb);
            sb.append(encodeStringToIdentifier(s));
            endConstructor(sb);
            addNewline(sb);
        }
        if (!mainModule.definedSorts().contains(Sorts.String())) {
            addConstructor(sb, "SortString");
            addNewline(sb);
        }
    }

    private void addSortOrderFunc(StringBuilder sb) {
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "order_sort(s: sort)");
        beginLetEquationValue(sb);
        beginMatchExpression(sb, "s");
        addNewline(sb);

        int i = 0;

        for (Sort s : iterable(mainModule.definedSorts())) {
            beginMatchEquation(sb);
            beginMatchEquationPattern(sb);
            sb.append(encodeStringToIdentifier(s));
            endMatchEquationPattern(sb);
            addMatchEquationValue(sb, Integer.toString(i++));
            addNewline(sb);
        }
        endMatchExpression(sb);
        endLetEquationValue(sb);
        endLetDefinitions(sb);
        endLetEquation(sb);
    }

    private void addKLabelType(StringBuilder sb) {
        beginTypeDefinition(sb, "klabel");
        for (KLabel label : iterable(mainModule.definedKLabels())) {
            beginConstructor(sb);
            sb.append(encodeStringToIdentifier(label));
            endConstructor(sb);
            addNewline(sb);
        }
        endTypeDefinition(sb);
    }

    private void addKLabelOrderFunc(StringBuilder sb) {
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "order_klabel(l: klabel)");
        beginLetEquationValue(sb);
        
        beginMatchExpression(sb, "l");
        addNewline(sb);

        int i = 0;

        for (KLabel label : iterable(mainModule.definedKLabels())) {
            beginMatchEquation(sb);
            beginMatchEquationPattern(sb);
            sb.append(encodeStringToIdentifier(label));
            endMatchEquationPattern(sb);
            addMatchEquationValue(sb, Integer.toString(i++));
            addNewline(sb);
            endMatchEquation(sb);
        }
        endMatchExpression(sb);
        
        endLetEquationValue(sb);
        endLetEquation(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
    }

    private void addPrintSortString(StringBuilder sb) {
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "print_sort_string(c: sort) : string");
        beginLetEquationValue(sb);
        
        beginMatchExpression(sb, "c");
        addNewline(sb);

        for (Sort s : iterable(mainModule.definedSorts())) {
            beginMatchEquation(sb);
            beginMatchEquationPattern(sb);
            sb.append(encodeStringToIdentifier(s));
            endMatchEquationPattern(sb);
            addMatchEquationValue(sb, StringUtil.enquoteCString(StringUtil.enquoteKString(s.name())));
            addNewline(sb);
            endMatchEquation(sb);
        }
        
        endMatchExpression(sb);
        
        endLetEquationValue(sb);
        endLetEquation(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
    }
    
    private void addPrintSort(StringBuilder sb) {
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "print_sort(c: sort) : string");
        beginLetEquationValue(sb);
        
        beginMatchExpression(sb, "c");
        addNewline(sb);

        for (Sort s : iterable(mainModule.definedSorts())) {
            beginMatchEquation(sb);
            beginMatchEquationPattern(sb);
            sb.append(encodeStringToIdentifier(s));
            endMatchEquationPattern(sb);
            addMatchEquationValue(sb, StringUtil.enquoteCString(s.name()));
            addNewline(sb);
            endMatchEquation(sb);
        }
        
        endMatchExpression(sb);
        
        endLetEquationValue(sb);
        endLetEquation(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
    }

    private void addPrintKLabel(StringBuilder sb) {
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "print_klabel(c: klabel) : string");
        beginLetEquationValue(sb);
        
        beginMatchExpression(sb, "c");
        addNewline(sb);

        for (KLabel label : iterable(mainModule.definedKLabels())) {
            beginMatchEquation(sb);
            beginMatchEquationPattern(sb);
            sb.append(encodeStringToIdentifier(label));
            endMatchEquationPattern(sb);
            addMatchEquationValue(sb, StringUtil.enquoteCString(ToKast.apply(label)));
            addNewline(sb);
            endMatchEquation(sb);
        }
        
        endMatchExpression(sb);
        
        endLetEquationValue(sb);
        endLetEquation(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
    }
        

    private void addMatchEquation(StringBuilder sb, String pattern, String value) {
        beginMatchEquation(sb);
        addMatchEquationPattern(sb, pattern);
        addMatchEquationValue(sb, value);
        endMatchEquation(sb);
    }

    private void addMatchEquationPattern(StringBuilder sb, String pattern) {
        beginMatchEquationPattern(sb);
        sb.append(pattern);
        endMatchEquationPattern(sb);
    }

    private void addMatchEquationValue(StringBuilder sb, String value) {
        beginMatchEquationValue(sb);
        sb.append(value);
        endMatchEquationValue(sb);
    }

    private void beginMatchExpression(StringBuilder sb, String varname) {
        sb.append("match ");
        sb.append(varname);
        sb.append(" with ");
    }

    private void endMatchExpression(StringBuilder sb) {
        // End match expression
    }

    private void beginMatchEquation(StringBuilder sb) {
        sb.append("|");
    }

    private void endMatchEquation(StringBuilder sb) {
        // End match equation
    }
    
    private void beginMatchEquationPattern(StringBuilder sb) {
        // Begin match equation pattern
    }
    
    private void endMatchEquationPattern(StringBuilder sb) {
        sb.append(" -> ");
    }
    
    private void beginMatchEquationValue(StringBuilder sb) {
        // Begin match equation value
    }

    private void endMatchEquationValue(StringBuilder sb) {
        // End match equation value
    }


    
    private void addLetEquation(StringBuilder sb, String name, String value) {
        beginLetEquation(sb);
        addLetEquationName(sb, name);
        addLetEquationValue(sb, value);
        endLetEquation(sb);
    }

    private void addLetEquationName(StringBuilder sb, String name) {
        beginLetEquationName(sb);
        sb.append(name);
        endLetEquationName(sb);
    }

    private void addLetEquationValue(StringBuilder sb, String value) {
        beginLetEquationValue(sb);
        sb.append(value);
        endLetEquationValue(sb);
    }

    private void beginLetEquation(StringBuilder sb) {
        // Begin let equation
    }

    private void endLetEquation(StringBuilder sb) {
        // End let equation
    }

    private void beginLetEquationName(StringBuilder sb) {
        // Begin let equation name
    }
    
    private void endLetEquationName(StringBuilder sb) {
        sb.append(" = ");
    }

    private void beginLetEquationValue(StringBuilder sb) {
        // Begin let equation value
    }

    private void endLetEquationValue(StringBuilder sb) {
        // End let equation value
    }

    private void addLetEquationSeparator(StringBuilder sb) {
        sb.append(" and ");
    }

    private void beginLetDefinitions(StringBuilder sb) {
        // Begin let definitions
    }

    private void endLetDefinitions(StringBuilder sb) {
        // End let definitions
    }

    private void beginLetScope(StringBuilder sb) {
        sb.append(" in ");
    }
    
    private void endLetScope(StringBuilder sb) {
        // End let scope
    }

    private void beginLetExpression(StringBuilder sb) {
        sb.append("let ");
    }

    private void endLetExpression(StringBuilder sb) {
        // End let expression
    }

    

    private void addLetrecEquation(StringBuilder sb, String name, String value) {
        beginLetrecEquation(sb);
        addLetrecEquationName(sb, name);
        addLetrecEquationValue(sb, value);
        endLetrecEquation(sb);
    }

    private void addLetrecEquationName(StringBuilder sb, String name) {
        beginLetrecEquationName(sb);
        sb.append(name);
        endLetrecEquationName(sb);
    }

    private void addLetrecEquationValue(StringBuilder sb, String value) {
        beginLetrecEquationValue(sb);
        sb.append(value);
        endLetrecEquationValue(sb);
    }

    private void beginLetrecEquation(StringBuilder sb) {
        // Begin letrec equation
    }

    private void endLetrecEquation(StringBuilder sb) {
        // End letrec equation
    }

    private void beginLetrecEquationName(StringBuilder sb) {
        // Begin letrec equation name
    }
    
    private void endLetrecEquationName(StringBuilder sb) {
        sb.append(" = ");
    }

    private void beginLetrecEquationValue(StringBuilder sb) {
        // Begin letrec equation value
    }

    private void endLetrecEquationValue(StringBuilder sb) {
        // End letrec equation value
    }

    private void addLetrecEquationSeparator(StringBuilder sb) {
        sb.append(" and ");
    }

    private void beginLetrecDefinitions(StringBuilder sb) {
        // Begin letrec definitions
    }

    private void endLetrecDefinitions(StringBuilder sb) {
        // End letrec definitions
    }

    private void beginLetrecScope(StringBuilder sb) {
        sb.append(" in ");
    }
    
    private void endLetrecScope(StringBuilder sb) {
        // End letrec scope
    }

    private void beginLetrecExpression(StringBuilder sb) {
        sb.append("let rec ");
    }

    private void endLetrecExpression(StringBuilder sb) {
        // End letrec expression
    }


    

    

    private void initializeFunctionRules() {
        functionRules = HashMultimap.create();

        for(Rule r : iterable(mainModule.rules())) {
            K left = RewriteToTop.toLeft(r.body());
            if(left instanceof KSequence) {
                KSequence kseq = (KSequence) left;
                if(kseq.items().size() == 1 && kseq.items().get(0) instanceof KApply) {
                    KApply kapp = (KApply) kseq.items().get(0);
                    if(mainModule.attributesFor().apply(kapp.klabel()).contains(Attribute.FUNCTION_KEY)) {
                        functionRules.put(kapp.klabel(), r);
                    }
                }
            }
        }
    }

    private void initializeFunctionSet() {
        functionSet = new HashSet<>(functionRules.keySet());

        for(Production p : iterable(mainModule.productions())) {
            if(p.att().contains(Attribute.FUNCTION_KEY)) {
                functionSet.add(p.klabel().get());
            }
        }
    }

    private void initializeFunctionOrder() {
        BiMap<KLabel, Integer> mapping = HashBiMap.create();
        int counter = 0;
        for(KLabel lbl : functionSet) {
            mapping.put(lbl, counter++);
        }
        List<Integer>[] predecessors = new List[functionSet.size()];
        for(int i = 0; i < predecessors.length; i++) {
            predecessors[i] = new ArrayList<>();
        }

        class GetPredecessors extends VisitKORE {
            private final KLabel current;

            public GetPredecessors(KLabel current) {
                this.current = current;
            }

            @Override
            public Void apply(KApply k) {
                if (functionSet.contains(k.klabel())) {
                    predecessors[mapping.get(current)].add(mapping.get(k.klabel()));
                }
                return super.apply(k);
            }
        }

        for (Map.Entry<KLabel, Rule> entry : functionRules.entries()) {
            GetPredecessors visitor = new GetPredecessors(entry.getKey());
            visitor.apply(entry.getValue().body());
            visitor.apply(entry.getValue().requires());
        }

        List<List<Integer>> components = new SCCTarjan().scc(predecessors);

        functionOrder = components
                        .stream()
                        .map(l -> l.stream()
                                   .map(i -> mapping.inverse().get(i))
                                   .collect(Collectors.toList()))
                        .collect(Collectors.toList());
    }

    private int sortFunctionRules(Rule a1, Rule a2) {
        return Boolean.compare(a1.att().contains("owise"), a2.att().contains("owise"));
    }

    private void addRules(StringBuilder sb) {
        int i = 0;
        for (List<KLabel> component : functionOrder) {
            boolean inLetrec = false;
            for (KLabel functionLabel : component) {
                if(inLetrec) {
                    addLetrecEquationSeparator(sb);
                } else {
                    beginLetrecExpression(sb);
                    beginLetrecDefinitions(sb);
                }
                beginLetrecEquation(sb);                
                beginLetrecEquationName(sb);
                String functionName = encodeStringToFunction(functionLabel.name());
                sb.append(functionName);
                sb.append(" (c: k list) (guards: Guard.t) : k");
                endLetrecEquationName(sb);
                beginLetrecEquationValue(sb);
                beginLetExpression(sb);
                beginLetDefinitions(sb);
                beginLetEquation(sb);
                addLetEquationName(sb, "lbl");
                beginLetEquationValue(sb);
                sb.append(encodeStringToIdentifier(functionLabel));
                endLetEquationValue(sb);
                endLetEquation(sb);
                endLetDefinitions(sb);
                beginLetScope(sb);
                beginMatchExpression(sb, "c");
                addNewline(sb);
                String hook = mainModule.attributesFor().apply(functionLabel).<String>getOptional(Attribute.HOOK_KEY).orElse("");
                if (hooks.containsKey(hook)) {
                    beginMatchEquation(sb);
                    sb.append(hooks.get(hook));
                    endMatchEquation(sb);
                    addNewline(sb);
                }
                if (predicateRules.containsKey(functionLabel.name())) {
                    beginMatchEquation(sb);
                    sb.append(predicateRules.get(functionLabel.name()));
                    endMatchEquation(sb);
                    addNewline(sb);
                }

                i = 0;
                for (Rule r : functionRules.get(functionLabel).stream().sorted(this::sortFunctionRules).collect(Collectors.toList())) {
                    oldConvert(r, sb, true, i++, functionName);
                }
                addMatchEquation(sb, "_", "raise (Stuck [KApply(lbl, c)])");
                endMatchExpression(sb);
                endLetScope(sb);
                endLetExpression(sb);
                endLetrecEquationValue(sb);
                endLetrecEquation(sb);
                addNewline(sb);
                inLetrec = true;
            }
            endLetrecDefinitions(sb);
            endLetrecExpression(sb);
        }

        boolean hasLookups = false;
        Map<Boolean, List<Rule>> sortedRules = stream(mainModule.rules()).collect(Collectors.groupingBy(this::hasLookups));

        beginLetrecExpression(sb);
        beginLetrecDefinitions(sb);
        beginLetrecEquation(sb);
        addLetrecEquationName(sb, "lookups_step (c: k) (guards: Guard.t) : k");
        beginLetrecEquationValue(sb);
        beginMatchExpression(sb, "c");
        addNewline(sb);
        i = 0;
        for (Rule r : sortedRules.get(true)) {
            if (!functionRules.values().contains(r)) {
                oldConvert(r, sb, false, i++, "lookups_step");
            }
        }
        addMatchEquation(sb, "_", "raise (Stuck c)");
        endLetrecEquationValue(sb);
        endLetrecDefinitions(sb);
        endLetrecExpression(sb);
        
        addNewline(sb);
        
        beginLetExpression(sb);
        beginLetDefinitions(sb);
        beginLetEquation(sb);
        addLetEquationName(sb, "step (c: k) : k");
        beginLetEquationValue(sb);
        beginMatchExpression(sb, "c");
        addNewline(sb);
        for (Rule r : sortedRules.get(false)) {
            if (!functionRules.values().contains(r)) {
                oldConvert(r, sb, false, i++, "step");
            }
        }
        addMatchEquation(sb, "_", "lookups_step c Guard.empty");
        addNewline(sb);
        endLetEquationValue(sb);
        endLetDefinitions(sb);
        endLetExpression(sb);
    }

    private String oldConvert() {
        StringBuilder sb = new StringBuilder();

        addSortType(sb);
        addSortOrderFunc(sb);
        addKLabelType(sb);
        addKLabelOrderFunc(sb);
        addPrelude(sb);
        addPrintSortString(sb);
        addPrintKLabel(sb);
        addMidlude(sb);
        initializeFunctionRules();
        initializeFunctionSet();
        initializeFunctionOrder();
        addRules(sb);
        addPostlude(sb);

        return sb.toString();
    }

    private boolean hasLookups(Rule r) {
        class Holder { boolean b; }
        Holder h = new Holder();
        new VisitKORE() {
            @Override
            public Void apply(KApply k) {
                h.b |= isLookupKLabel(k);
                return super.apply(k);
            }
        }.apply(r.requires());
        return h.b;
    }

    private void beginMultilineComment(StringBuilder sb) {
        sb.append("(*");
    }

    private void endMultilineComment(StringBuilder sb) {
        sb.append("*)");
    }
    
    private void outputAnnotate(Rule r, StringBuilder sb) {
        beginMultilineComment(sb);
        sb.append(" rule ");
        sb.append(ToKast.apply(r.body()));
        sb.append(" requires ");
        sb.append(ToKast.apply(r.requires()));
        sb.append(" ensures ");
        sb.append(ToKast.apply(r.ensures()));
        sb.append(" ");
        sb.append(r.att().toString());
        endMultilineComment(sb);
        addNewline(sb);
    }

    private void unhandledOldConvert(Rule r, StringBuilder sb, boolean function, int ruleNum, String functionName) throws KEMException {
        if(annotateOutput) { outputAnnotate(r, sb); }

        sb.append("| ");

        K left     = RewriteToTop.toLeft(r.body());
        K right    = RewriteToTop.toRight(r.body());
        K requires = r.requires();

        SetMultimap<KVariable, String> vars = HashMultimap.create();
        Visitor visitor = oldConvert(sb, false, vars, false);

        if(function) {
            KApply kapp = (KApply) ((KSequence) left).items().get(0);
            visitor.apply(kapp.klist().items(), true);
        } else {
            visitor.apply(left);
        }

        String result = oldConvert(vars);

        if(hasLookups(r)) {
            sb.append(" when not (Guard.mem (GuardElt.Guard ").append(ruleNum).append(") guards)");
        }

        String suffix = "";

        if(!requires.equals(KSequence(BooleanUtils.TRUE)) || !("true".equals(result))) {
            suffix = oldConvertLookups(sb, requires, vars, functionName, ruleNum);
            sb.append(" when ");
            oldConvert(sb, true, vars, true).apply(requires);
            sb.append(" && (");
            sb.append(result);
            sb.append(")");
        }

        sb.append(" -> ");
        oldConvert(sb, true, vars, false).apply(right);
        sb.append(suffix);
        addNewline(sb);
    }

    private void oldConvert(Rule r, StringBuilder sb, boolean function, int ruleNum, String functionName) {
        try {
            unhandledOldConvert(r, sb, function, ruleNum, functionName);
        } catch (KEMException e) {
            e.exception.addTraceFrame("while compiling rule at " + r.att().getOptional(Source.class).map(Object::toString).orElse("<none>") + ":" + r.att().getOptional(Location.class).map(Object::toString).orElse("<none>"));
            throw e;
        }
    }

    private static class Holder { int i; }

    private String oldConvertLookups(StringBuilder sb, K requires, SetMultimap<KVariable, String> vars, String functionName, int ruleNum) {
        Deque<String> suffix = new ArrayDeque<>();
        Holder h = new Holder();
        h.i = 0;
        new VisitKORE() {
            @Override
            public Void apply(KApply k) {
                if (k.klabel().name().equals("#match")) {
                    if (k.klist().items().size() != 2) {
                        throw KEMException.internalError("Unexpected arity of lookup: " + k.klist().size(), k);
                    }
                    sb.append(" -> (match ");
                    oldConvert(sb, true, vars, false).apply(k.klist().items().get(1));
                    sb.append(" with ");
                    addNewline(sb);
                    oldConvert(sb, false, vars, false).apply(k.klist().items().get(0));
                    suffix.add("| _ -> (" + functionName + " c (Guard.add (GuardElt.Guard " + ruleNum + ") guards)))");
                    h.i++;
                } else if (k.klabel().name().equals("#setChoice")) {
                    if (k.klist().items().size() != 2) {
                        throw KEMException.internalError("Unexpected arity of choice: " + k.klist().size(), k);
                    }
                    sb.append(" -> (match ");
                    oldConvert(sb, true, vars, false).apply(k.klist().items().get(1));
                    sb.append(" with ");
                    addNewline(sb);
                    sb.append("| [Set s] -> let choice = (KSet.fold (fun e result -> if result = [Bottom] then (match e with ");
                    oldConvert(sb, false, vars, false).apply(k.klist().items().get(0));
                    suffix.add("| _ -> (" + functionName + " c (Guard.add (GuardElt.Guard " + ruleNum + ") guards)))");
                    suffix.add("| _ -> [Bottom]) else result) s [Bottom]) in if choice = [Bottom] then (" + functionName + " c (Guard.add (GuardElt.Guard " + ruleNum + ") guards)) else choice");
                    h.i++;
                } else if (k.klabel().name().equals("#mapChoice")) {
                    if (k.klist().items().size() != 2) {
                        throw KEMException.internalError("Unexpected arity of choice: " + k.klist().size(), k);
                    }
                    sb.append(" -> (match ");
                    oldConvert(sb, true, vars, false).apply(k.klist().items().get(1));
                    sb.append(" with ");
                    addNewline(sb);
                    sb.append("| [Map m] -> let choice = (KMap.fold (fun k v result -> if result = [Bottom] then (match k with ");
                    oldConvert(sb, false, vars, false).apply(k.klist().items().get(0));
                    suffix.add("| _ -> (" + functionName + " c (Guard.add (GuardElt.Guard " + ruleNum + ") guards)))");
                    suffix.add("| _ -> [Bottom]) else result) m [Bottom]) in if choice = [Bottom] then (" + functionName + " c (Guard.add (GuardElt.Guard " + ruleNum + ") guards)) else choice");
                    h.i++;
                }
                return super.apply(k);
            }
        }.apply(requires);

        StringBuilder sb2 = new StringBuilder();
        while(!suffix.isEmpty()) {
            sb2.append(suffix.pollLast());
        }
        return sb2.toString();
    }

    private static String oldConvert(SetMultimap<KVariable, String> vars) {
        StringBuilder sb = new StringBuilder();
        for (Collection<String> nonLinearVars : vars.asMap().values()) {
            if (nonLinearVars.size() < 2) {
                continue;
            }
            Iterator<String> iter = nonLinearVars.iterator();
            String last = iter.next();
            while (iter.hasNext()) {
                //handle nonlinear variables in pattern
                String next = iter.next();
                sb.append("(eq ");
                sb.append(last);
                sb.append(" ");
                sb.append(next);
                sb.append(")");
                last = next;
                sb.append(" && ");
            }
        }
        sb.append("true");
        return sb.toString();
    }

    private void applyVarRhs(KVariable v, StringBuilder sb, SetMultimap<KVariable, String> vars) {
        sb.append(vars.get(v).iterator().next());
    }

    private void applyVarLhs(KVariable k, StringBuilder sb, SetMultimap<KVariable, String> vars) {
        String varName = encodeStringToVariable(k.name());
        vars.put(k, varName);
        Sort s = Sort(k.att().<String>getOptional(Attribute.SORT_KEY).orElse(""));
        if (mainModule.sortAttributesFor().contains(s)) {
            String hook = mainModule.sortAttributesFor().apply(s).<String>getOptional("hook").orElse("");
            if (sortHooks.containsKey(hook)) {
                sb.append("(");
                sb.append(s.name()).append(" _");
                sb.append(" as ").append(varName).append(")");
                return;
            }
        }
        sb.append(varName);
    }

    private Visitor oldConvert(StringBuilder sb, boolean rhs, SetMultimap<KVariable, String> vars, boolean useNativeBooleanExp) {
        return new Visitor(sb, rhs, vars, useNativeBooleanExp);
    }

    private class Visitor extends VisitKORE {
        private final StringBuilder sb;
        private final boolean rhs;
        private final SetMultimap<KVariable, String> vars;
        private final boolean useNativeBooleanExp;

        public Visitor(StringBuilder sb, boolean rhs, SetMultimap<KVariable, String> vars, boolean useNativeBooleanExp) {
            this.sb = sb;
            this.rhs = rhs;
            this.vars = vars;
            this.useNativeBooleanExp = useNativeBooleanExp;
            this.inBooleanExp = useNativeBooleanExp;
        }

        private boolean inBooleanExp;

        @Override
        public Void apply(KApply k) {
            if (isLookupKLabel(k)) {
                apply(BooleanUtils.TRUE);
            } else if (k.klabel().name().equals("#KToken")) {
                //magic down-ness
                sb.append("KToken (");
                Sort sort = Sort(((KToken) ((KSequence) k.klist().items().get(0)).items().get(0)).s());
                apply(sort);
                sb.append(", ");
                apply(((KSequence) k.klist().items().get(1)).items().get(0));
                sb.append(")");
            } else if (functionSet.contains(k.klabel())) {
                applyFunction(k);
            } else {
                applyKLabel(k);
            }
            return null;
        }

        public void applyKLabel(KApply k) {
            sb.append("KApply (");
            apply(k.klabel());
            sb.append(", ");
            apply(k.klist().items(), true);
            sb.append(")");
        }

        public void applyFunction(KApply k) {
            boolean stack = inBooleanExp;
            String hook = mainModule.attributesFor().apply(k.klabel()).<String>getOptional(Attribute.HOOK_KEY).orElse("");
            // use native &&, ||, not where possible
            if (useNativeBooleanExp && ("#BOOL:_andBool_".equals(hook) || "#BOOL:_andThenBool_".equals(hook))) {
                assert k.klist().items().size() == 2;
                if (!stack) {
                    sb.append("[Bool ");
                }
                inBooleanExp = true;
                sb.append("(");
                apply(k.klist().items().get(0));
                sb.append(") && (");
                apply(k.klist().items().get(1));
                sb.append(")");
                if (!stack) {
                    sb.append("]");
                }
            } else if (useNativeBooleanExp && ("#BOOL:_orBool_".equals(hook) || "#BOOL:_orElseBool_".equals(hook))) {
                assert k.klist().items().size() == 2;
                if (!stack) {
                    sb.append("[Bool ");
                }
                inBooleanExp = true;
                sb.append("(");
                apply(k.klist().items().get(0));
                sb.append(") || (");
                apply(k.klist().items().get(1));
                sb.append(")");
                if (!stack) {
                    sb.append("]");
                }
            } else if (useNativeBooleanExp && "#BOOL:notBool_".equals(hook)) {
                assert k.klist().items().size() == 1;
                if (!stack) {
                    sb.append("[Bool ");
                }
                inBooleanExp = true;
                sb.append("(not ");
                apply(k.klist().items().get(0));
                sb.append(")");
                if (!stack) {
                    sb.append("]");
                }
            } else if (mainModule.collectionFor().contains(k.klabel()) && !rhs) {
                applyKLabel(k);
                sb.append(" :: []");
            } else {
                if (mainModule.attributesFor().apply(k.klabel()).contains(Attribute.PREDICATE_KEY)) {
                    Sort s = Sort(mainModule.attributesFor().apply(k.klabel()).<String>get(Attribute.PREDICATE_KEY).get());
                    if (mainModule.sortAttributesFor().contains(s)) {
                        String hook2 = mainModule.sortAttributesFor().apply(s).<String>getOptional("hook").orElse("");
                        if (sortHooks.containsKey(hook2) && k.klist().items().size() == 1) {
                            KSequence item = (KSequence) k.klist().items().get(0);
                            if (item.items().size() == 1 &&
                                    vars.containsKey(item.items().get(0))) {
                                Optional<String> varSort = item.items().get(0).att().<String>getOptional(Attribute.SORT_KEY);
                                if (varSort.isPresent() && varSort.get().equals(s.name())) {
                                    // this has been subsumed by a structural check on the builtin data type
                                    apply(BooleanUtils.TRUE);
                                    return;
                                }
                            }
                        }
                    }
                    if (s.equals(Sorts.KItem()) && k.klist().items().size() == 1) {
                        if (k.klist().items().get(0) instanceof KSequence) {
                            KSequence item = (KSequence) k.klist().items().get(0);
                            if (item.items().size() == 1) {
                                apply(BooleanUtils.TRUE);
                                return;
                            }
                        }
                    }
                }
                if (stack) {
                    sb.append("(isTrue ");
                }
                inBooleanExp = false;
                sb.append("(");
                sb.append(encodeStringToFunction(k.klabel().name()));
                sb.append("(");
                apply(k.klist().items(), true);
                sb.append(") Guard.empty)");
                if (stack) {
                    sb.append(")");
                }
            }
            inBooleanExp = stack;
        }

        @Override
        public Void apply(KRewrite k) {
            throw new AssertionError("unexpected rewrite");
        }

        @Override
        public Void apply(KToken k) {
            if (useNativeBooleanExp && inBooleanExp && k.sort().equals(Sorts.Bool())) {
                sb.append(k.s());
                return null;
            }
            if (mainModule.sortAttributesFor().contains(k.sort())) {
                String hook = mainModule.sortAttributesFor().apply(k.sort()).<String>getOptional("hook").orElse("");
                if (sortHooks.containsKey(hook)) {
                    sb.append(sortHooks.get(hook).apply(k.s()));
                    return null;
                }
            }
            sb.append("KToken (");
            apply(k.sort());
            sb.append(", ");
            sb.append(StringUtil.enquoteCString(k.s()));
            sb.append(")");
            return null;
        }

        @Override
        public Void apply(KVariable k) {
            if (rhs) {
                applyVarRhs(k, sb, vars);
            } else {
                applyVarLhs(k, sb, vars);
            }
            return null;
        }

        @Override
        public Void apply(KSequence k) {
            if (useNativeBooleanExp && k.items().size() == 1 && inBooleanExp) {
                apply(k.items().get(0));
                return null;
            }
            sb.append("(");
            if (!rhs) {
                for (int i = 0; i < k.items().size() - 1; i++) {
                    if (isList(k.items().get(i), false)) {
                        throw KEMException.criticalError("Cannot compile KSequence with K variable not at tail.", k.items().get(i));
                    }
                }
            }
            apply(k.items(), false);
            sb.append(")");
            return null;
        }

        public String getSortOfVar(K k) {
            return k.att().<String>getOptional(Attribute.SORT_KEY).orElse("K");
        }

        @Override
        public Void apply(InjectedKLabel k) {
            sb.append("InjectedKLabel (");
            apply(k.klabel());
            sb.append(")");
            return null;
        }

        private void apply(List<K> items, boolean klist) {
            for(int i = 0; i < items.size(); i++) {
                K item = items.get(i);
                apply(item);
                if (i == items.size() - 1) {
                    if (!isList(item, klist)) {
                        sb.append(" :: []");
                    }
                } else {
                    if (isList(item, klist)) {
                        sb.append(" @ ");
                    } else {
                        sb.append(" :: ");
                    }
                }
            }
            if(items.isEmpty()) {
                sb.append("[]");
            }
        }

        private boolean isList(K item, boolean klist) {
            return !klist && ((item instanceof KVariable && getSortOfVar(item).equals("K")) || item instanceof KSequence
                    || (item instanceof KApply && functionSet.contains(((KApply) item).klabel())));
        }

        private void apply(Sort sort) {
            sb.append(encodeStringToIdentifier(sort));
        }

        public void apply(KLabel klabel) {
            if (klabel instanceof KVariable) {
                apply((KVariable) klabel);
            } else {
                sb.append(encodeStringToIdentifier(klabel));
            }
        }
    }

    private boolean isLookupKLabel(KApply k) {
        return k.klabel().name().equals("#match") || k.klabel().name().equals("#mapChoice") || k.klabel().name().equals("#setChoice");
    }
}
