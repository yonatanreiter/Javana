package edu.yu.compilers.frontend;

import antlr4.JavanaBaseVisitor;
import antlr4.JavanaParser;
import edu.yu.compilers.intermediate.symtable.Predefined;
import edu.yu.compilers.intermediate.symtable.SymTable;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;
import edu.yu.compilers.intermediate.symtable.SymTableStack;
import edu.yu.compilers.intermediate.type.Typespec;
import edu.yu.compilers.intermediate.util.CrossReferencer;

import static edu.yu.compilers.frontend.SemanticErrorHandler.Code.*;
import static edu.yu.compilers.intermediate.symtable.SymTableEntry.Kind.*;

/***
 * Check the semantics of the Javana program and populate the symbol table.
 */
public class Semantics extends JavanaBaseVisitor<Object> {

    private final SymTableStack symTableStack;
    private final SemanticErrorHandler error;
    private SymTableEntry programId;

    public Semantics() {

        this.symTableStack = new SymTableStack();
        Predefined.initialize(symTableStack);
        this.error = new SemanticErrorHandler();
    }

    /**
     * Return the default value for a data type.
     *
     * @param type the data type.
     * @return the default value.
     */
    public static Object defaultValue(Typespec type) {
        type = type.baseType();

        if (type == Predefined.integerType) return 0;
        else if (type == Predefined.realType) return 0.0f;
        else if (type == Predefined.booleanType) return Boolean.FALSE;
        else if (type == Predefined.charType) return '#';
        else /* string */                        return "#";
    }

    public int getErrorCount() {
        return error.getCount();
    }

    public SymTableEntry getProgramId() {
        return programId;
    }

    public void printSymbolTableStack() {
        CrossReferencer crossReferencer = new CrossReferencer();
        crossReferencer.print(symTableStack);
    }

    @Override
    public Object visitProgram(JavanaParser.ProgramContext ctx) {
        visit(ctx.programHeader());
        visit(ctx.mainMethod());

        return null;
    }

    @Override
    public Object visitProgramHeader(JavanaParser.ProgramHeaderContext ctx) {
        String programName = ctx.identifer().getText();  // don't shift case

        programId = symTableStack.enterLocal(programName, PROGRAM);
        programId.setRoutineSymTable(symTableStack.push());

        symTableStack.setProgramId(programId);
        symTableStack.getLocalSymTable().setOwner(programId);

        //ctx.context = programId;
        return null;
    }

    @Override
    public Object visitMainMethod(JavanaParser.MainMethodContext ctx){
        JavanaParser.BlockStatementContext blockCtx = ctx.blockStatement();

        symTableStack.push(new SymTable(symTableStack.getCurrentNestingLevel()));
        visit(blockCtx);
        symTableStack.pop();
        return null;
    }


    @Override
    public Object visitConstantDef(JavanaParser.ConstantDefContext ctx) {

        String constantName = ctx.nameList().identifer(0).getText().toLowerCase(); // Adjust based on actual method names
        SymTableEntry constantId = symTableStack.lookupLocal(constantName);

        if (constantId == null) {

            Object constValue = visit(ctx.expression());

            constantId = symTableStack.enterLocal(constantName, CONSTANT);
            constantId.setValue(constValue);
            //constantId.setType(ctx.);

        } else {
            error.flag(REDECLARED_IDENTIFIER, ctx);

//            idCtx.entry = constantId;
//            idCtx.type = Predefined.integerType;
        }

        constantId.appendLineNumber(ctx.getStart().getLine());
        return null;
    }

}