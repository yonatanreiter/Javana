package edu.yu.compilers.backend.compiler;

public class StatementGenerator extends CodeGenerator {
    /**
     * Constructor.
     *
     * @param parent   the parent generator.
     * @param compiler the compiler to use.
     */
    public StatementGenerator(CodeGenerator parent, Compiler compiler) {
        super(parent, compiler);
    }
}