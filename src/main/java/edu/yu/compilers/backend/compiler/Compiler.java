package edu.yu.compilers.backend.compiler;

import antlr4.JavanaBaseVisitor;
import antlr4.JavanaParser;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Compile Pascal to Jasmin assembly language.
 */
public class Compiler extends JavanaBaseVisitor<Object> {
    private String programName;     // the program name
    private final CodeGenerator code;            // base code generator
    private final Optional<Path> outputPath;
    private ProgramGenerator programCode;     // program code generator
    private StatementGenerator statementCode;   // statement code generator
    private ExpressionGenerator expressionCode;  // expression code generator

    /**
     * Constructor for the base compiler.
     *
     * @param programName the program name.
     */
    public Compiler(String programName) throws IOException  {
        this(programName, null);
    }

    /**
     * Constructor for the base compiler.
     *
     * @param programName the program name.
     */
    public Compiler(String programName, Path outputPath) throws IOException {
        this.programName = programName;
        this.outputPath = Optional.ofNullable(outputPath);
        code = new CodeGenerator(programName, this);
    }

    /**
     * Constructor for child compilers of procedures and functions.
     *
     * @param parent the parent compiler.
     */
    public Compiler(Compiler parent) {
        this.outputPath = Optional.empty();
        this.code = parent.code;
        this.programCode = parent.programCode;
        this.programName = parent.programName;
    }

    /**
     * Constructor for child compilers of records.
     *
     * @param recordId the symbol table entry of the name of the record to compile.
     */
    protected Compiler(SymTableEntry recordId, Optional<Path> outputPath) throws IOException {
        this.outputPath = outputPath;
        String recordTypePath = recordId.getType().getRecordTypePath();
        code = new CodeGenerator(recordTypePath, this);
        createNewGenerators(code);

        programCode.emitRecord(recordId, recordTypePath);
    }

    /**
     * Create new child code generators.
     *
     * @param parentGenerator the parent code generator.
     */
    private void createNewGenerators(CodeGenerator parentGenerator) {
        programCode = new ProgramGenerator(parentGenerator, this);
        statementCode = new StatementGenerator(programCode, this);
        expressionCode = new ExpressionGenerator(programCode, this);
    }

    /**
     * Get the name of the object (Jasmin) file.
     *
     * @return the name.
     */
    public String getObjectFileName() {
        return code.getObjectFileName();
    }

    public Optional<Path> getOutputPath() {
        return outputPath;
    }

    @Override
    public Object visitProgram(JavanaParser.ProgramContext ctx) {
        createNewGenerators(code);
        programCode.emitProgram(ctx);
        return null;
    }

    @Override
    public Object visitFuncDefinition(JavanaParser.FuncDefinitionContext ctx) {
        createNewGenerators(programCode);
        programCode.emitRoutine(ctx);
        return null;
    }

}
