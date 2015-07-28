package org.kframework.backend.ocaml;

import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.KompileOptions;
import org.kframework.main.GlobalOptions;
import org.kframework.utils.BinaryLoader;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Created by dwightguth on 5/27/15.
 */
public class OcamlBackend implements Consumer<CompiledDefinition> {

    private final KExceptionManager kem;
    private final FileUtil files;
    private final GlobalOptions globalOptions;
    private final KompileOptions kompileOptions;

    @Inject
    public OcamlBackend(KExceptionManager kem, FileUtil files, GlobalOptions globalOptions, KompileOptions kompileOptions) {
        this.kem = kem;
        this.files = files;
        this.globalOptions = globalOptions;
        this.kompileOptions = kompileOptions;
    }

    @Override
    public void accept(CompiledDefinition compiledDefinition) {
        DefinitionToOcaml def = new DefinitionToOcaml(kem, files, globalOptions, kompileOptions);
        def.initialize(compiledDefinition);
        String ocaml = def.constants();
        files.saveToKompiled("constants.ml", ocaml);
        ocaml = def.definition();
        files.saveToKompiled("def.ml", ocaml);
        new BinaryLoader(kem).saveOrDie(files.resolveKompiled("ocaml_converter.bin"), def);
        try {
            FileUtils.copyFile(files.resolveKBase("include/ocaml/prelude.ml"), files.resolveKompiled("prelude.ml"));
            Process ocamlopt = files.getProcessBuilder()
                    .command((DefinitionToOcaml.ocamlopt ? "ocamlopt.opt" : "ocamlc.opt"), "-c", "-g", "-I", "+gmp",
                            "-safe-string", "-w", "-26-11",
                            "constants.ml", "prelude.ml", "def.ml")
                    .directory(files.resolveKompiled("."))
                    .inheritIO()
                    .start();
            int exit = ocamlopt.waitFor();
            if (exit != 0) {
                throw KEMException.criticalError("ocamlopt returned nonzero exit code: " + exit + "\nExamine output to see errors.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw KEMException.criticalError("Ocaml process interrupted.", e);
        } catch (IOException e) {
            throw KEMException.criticalError("Error starting ocamlopt process: " + e.getMessage(), e);
        }
    }
}
