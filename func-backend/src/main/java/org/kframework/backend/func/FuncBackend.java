// Copyright (c) 2014-2015 K Team. All Rights Reserved.
package org.kframework.backend.func;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.kframework.backend.Backends;
import org.kframework.backend.BasicBackend;
import org.kframework.backend.FirstStep;
import org.kframework.backend.LastStep;
import org.kframework.compile.transformers.AddHeatingConditions;
import org.kframework.compile.transformers.ContextsToHeating;
import org.kframework.compile.transformers.StrictnessToContexts;
import org.kframework.compile.utils.CompilerSteps;
import org.kframework.kil.Definition;
import org.kframework.kil.loader.Context;
import org.kframework.kompile.KompileOptions;
import org.kframework.utils.OS;
import org.kframework.utils.Stopwatch;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.file.FileUtil;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FuncBackend extends BasicBackend {
    @Inject
    public FuncBackend(Stopwatch sw, Context context, KompileOptions options) {
        super(sw, context, options);
    }

    @Override
    public void run(Definition definition) {
        System.out.println("nothing implemented yet");
        System.out.println(definition);
        System.out.println("nothing implemented yet");
    }

    @Override
    public String getDefaultStep() {
        return "LastStep";
    }

    @Override
    public CompilerSteps<Definition> getCompilationSteps() {
        CompilerSteps<Definition> steps = new CompilerSteps<Definition>(context);
        steps.add(new FirstStep(this, context));
        steps.add(new StrictnessToContexts(context));
        steps.add(new ContextsToHeating(context));
        steps.add(new AddHeatingConditions(context));
        steps.add(new LastStep(this, context));
        return steps;
    }

    @Override
    public String autoincludedFile() {
        return Backends.AUTOINCLUDE_JAVA;
    }

    @Override
    public boolean generatesDefinition() {
        return false;
    }
}