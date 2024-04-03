package edu.yu.compilers.frontend;

import antlr4.JavanaBaseVisitor;
import antlr4.JavanaParser;
import edu.yu.compilers.backend.interpreter.StackFrame;
import edu.yu.compilers.intermediate.symtable.Predefined;
import edu.yu.compilers.intermediate.symtable.SymTable;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;
import edu.yu.compilers.intermediate.symtable.SymTableStack;
import edu.yu.compilers.intermediate.type.TypeChecker;
import edu.yu.compilers.intermediate.type.Typespec;
import edu.yu.compilers.intermediate.util.CrossReferencer;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Stack;

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
    public Object visitConstantDefinition(JavanaParser.ConstantDefinitionContext ctx) {


        String constantName = ctx.nameList().identifer(0).getText().toLowerCase(); // Adjust based on actual method names
        SymTableEntry constantId = symTableStack.lookupLocal(constantName);

        if (constantId == null) {

            Object constValue = visit(ctx.expression());

            constantId = symTableStack.enterLocal(constantName, CONSTANT);
            constantId.setValue(constValue);
            constantId.setType(TypeChecker.returnType(constantId));

        }
        else {
            error.flag(REDECLARED_IDENTIFIER, ctx);

//            idCtx.entry = constantId;
//            idCtx.type = Predefined.integerType;
        }

        constantId.appendLineNumber(ctx.getStart().getLine());
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitBlockStatement(JavanaParser.BlockStatementContext ctx) {
        for(ParseTree o : ctx.statement()){
            visit(o);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitNameDeclDefStatement(JavanaParser.NameDeclDefStatementContext ctx) {
       //just visit whatever is actually relevant here
        visit(ctx.children.get(0));

        return null;
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitVariableDefinition(JavanaParser.VariableDefinitionContext ctx) {
        String varName = ctx.nameList().identifer().get(0).getText();

        Object value = visit(ctx.expression());

        SymTable frame = symTableStack.get(symTableStack.getCurrentNestingLevel());
        frame.enter(varName, VARIABLE);
        frame.get(varName).setValue(value);

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitArithmeticExpression(JavanaParser.ArithmeticExpressionContext ctx) {
        if(ctx.children.get(0) instanceof JavanaParser.IdentifierExpressionContext){
            JavanaParser.IdentifierExpressionContext identifier = (JavanaParser.IdentifierExpressionContext) ctx.children.get(0);
            Object value = visit(identifier);

            if(value == null){
                error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),identifier.getText());
            }
        }

        if(ctx.children.get(2) instanceof JavanaParser.IdentifierExpressionContext){
            JavanaParser.IdentifierExpressionContext identifier = (JavanaParser.IdentifierExpressionContext) ctx.children.get(2);
            Object value = visit(identifier);

            if(value == null){
                error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),identifier.getText());
            }
        }
        return super.visitArithmeticExpression(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitIdentifierExpression(JavanaParser.IdentifierExpressionContext ctx) {
        String varName = ctx.getText();
        return symTableStack.get(symTableStack.getCurrentNestingLevel()).get(varName);

    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitLiteralExpression(JavanaParser.LiteralExpressionContext ctx) {
        return visit(ctx.literal());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitLiteral(JavanaParser.LiteralContext ctx) {
        return super.visitLiteral(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitGlobalDefinitions(JavanaParser.GlobalDefinitionsContext ctx) {
        for (ParseTree child : ctx.children) {
            if (child instanceof JavanaParser.VariableDeclContext) {
                // Handle global variable declaration
                visitVariableDecl((JavanaParser.VariableDeclContext) child);
            } else if (child instanceof JavanaParser.ConstantDefContext) {
                // Handle global constant definition
                visitConstantDefinition((JavanaParser.ConstantDefinitionContext) child);
            } else if (child instanceof JavanaParser.FuncDefinitionContext) {
                // Handle function definition
                visitFuncDefinition((JavanaParser.FuncDefinitionContext) child);
            } else if (child instanceof JavanaParser.RecordDeclContext) {
                // Handle record type declaration
                visitRecordDecl((JavanaParser.RecordDeclContext) child);
            }
            // Add more else-if branches as necessary for other kinds of global definitions
        }

        return null;
    }



}
