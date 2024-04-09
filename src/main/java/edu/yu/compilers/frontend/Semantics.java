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

import java.util.HashSet;
import java.util.Set;
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
    Set<String> operators =  Set.of("+", "-", "*", "/", "%");

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
        for(ParseTree child : ctx.children){
            visit(child);
        }

        return null;
    }

    @Override
    public Object visitProgramHeader(JavanaParser.ProgramHeaderContext ctx) {
        String programName = ctx.identifier().getText();  // don't shift case

        programId = symTableStack.enterLocal(programName, PROGRAM);
        programId.setRoutineSymTable(symTableStack.push());

        symTableStack.setProgramId(programId);
        symTableStack.getLocalSymTable().setOwner(programId);

        //ctx.context = programId;
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
    public Object visitGlobalDefinitions(JavanaParser.GlobalDefinitionsContext ctx) {
        for (ParseTree child : ctx.children) {
            visit(child);
            // Add more else-if branches as necessary for other kinds of global definitions
        }

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


        String constantName = ctx.nameList().identifier(0).getText().toLowerCase(); // Adjust based on actual method names
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
    public Object visitAssignmentStatement(JavanaParser.AssignmentStatementContext ctx) {

        String lhs = ctx.variable().name.getText();

        SymTableEntry variable = symTableStack.lookupLocal(lhs);

        if(variable == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),lhs);
        }

        else{

            if(variable.getKind() == CONSTANT){
                error.flag(INCOMPATIBLE_ASSIGNMENT, ctx.getStart().getLine(), lhs);
            }

            Object value = visit(ctx.expression());



            variable = symTableStack.enterLocal(lhs, VARIABLE);
            variable.setValue(value);
            variable.setType(TypeChecker.returnType(variable));
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
        String varName = ctx.nameList().identifier().get(0).getText();

        SymTableEntry varId = symTableStack.lookupLocal(varName);

        if (varId == null) {

            Object varVal = visit(ctx.expression());

            if(varVal != null) {
                varId = symTableStack.enterLocal(varName, VARIABLE);

                varId.setValue(varVal);
                varId.setType(TypeChecker.returnType(varVal));
            }


        }
        else {
            error.flag(REDECLARED_IDENTIFIER, ctx.getStart().getLine(), varName);

            varId = symTableStack.enterLocal(varName, VARIABLE);
//            varId.setValue(0);
//            varId.setType(Predefined.undefinedType);
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
    public Object visitIdentifier(JavanaParser.IdentifierContext ctx) {
        Object value = visit(ctx);

        if(value == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),ctx.getText());
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
    public Object visitArithmeticExpression(JavanaParser.ArithmeticExpressionContext ctx) {
        Object lhs = visit(ctx.children.get(0));
        Object rhs = visit(ctx.children.get(2));
        String operator = ctx.children.get(1).getText();
        boolean oneIsInt = TypeChecker.returnType(lhs).getIdentifier().getName().equals("integer") || TypeChecker.returnType(rhs).getIdentifier().getName().equals("integer");


        if (TypeChecker.returnType(rhs) != TypeChecker.returnType(lhs)) {
            if (oneIsInt) {
                error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
            }
            return null;
        } else if (!oneIsInt) {
            error.flag(INVALID_OPERATOR, ctx.getStart().getLine(), ctx.getText());
        } else {
            if (operator.equals("+")) {
                return (int) lhs + (int) rhs;
            } else {
                return (int) lhs - (int) rhs;
            }
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
    public Object visitHigherArithmeticExpression(JavanaParser.HigherArithmeticExpressionContext ctx) {
        Object lhs = visit(ctx.children.get(0));
        Object rhs = visit(ctx.children.get(2));
        String operator = ctx.children.get(1).getText();
        boolean oneIsInt = TypeChecker.returnType(lhs).getIdentifier().getName().equals("integer") || TypeChecker.returnType(rhs).getIdentifier().getName().equals("integer");


        if (TypeChecker.returnType(rhs) != TypeChecker.returnType(lhs)) {
            if (oneIsInt) {
                error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
            }
            return null;
        } else if (!oneIsInt) {
            error.flag(INVALID_OPERATOR, ctx.getStart().getLine(), ctx.getText());
        } else {
            if (operator.equals("*")) {
                return (int) lhs * (int) rhs;
            } else if (operator.equals("/")) {
                return (int) lhs / (int) rhs;
            }
            else{
                return (int) lhs % (int) rhs;
            }
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
    public Object visitIdentifierExpression(JavanaParser.IdentifierExpressionContext ctx) {
        String varName = ctx.getText();
        SymTableEntry frame =  symTableStack.get(symTableStack.getCurrentNestingLevel()).get(varName);

        if(frame == null)return null;

        return frame.getValue();

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

        if(ctx.literal() instanceof JavanaParser.IntegerLiteralContext){
            return visitIntegerLiteral((JavanaParser.IntegerLiteralContext) ctx.literal());
        }
        else if(ctx.literal() instanceof JavanaParser.StringLiteralContext){
            return visitStringLiteral((JavanaParser.StringLiteralContext) ctx.literal());

        }
        else if(ctx.literal() instanceof JavanaParser.BooleanLiteralContext){
            return visitBooleanLiteral((JavanaParser.BooleanLiteralContext) ctx.literal());

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
    public Object visitIntegerLiteral(JavanaParser.IntegerLiteralContext ctx) {
        return Integer.parseInt(ctx.INTEGER().getText());
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
    public Object visitBooleanLiteral(JavanaParser.BooleanLiteralContext ctx) {
        return Boolean.parseBoolean(ctx.BOOL().getText());
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
    public Object visitStringLiteral(JavanaParser.StringLiteralContext ctx) {
        return ctx.STRING().getText();
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
    public Object visitNoneValue(JavanaParser.NoneValueContext ctx) {
        return super.visitNoneValue(ctx);
    }



}
