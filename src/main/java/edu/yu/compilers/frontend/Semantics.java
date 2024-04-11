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

import java.util.*;

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

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitConditionalExpression(JavanaParser.ConditionalExpressionContext ctx) {
        Object lhs = visit(ctx.expression(0)); // Visit left-hand side expression
        Object rhs = visit(ctx.expression(1)); // Visit right-hand side expression
        String operator = ctx.COND_OP().getText(); // Get the conditional operator

        // Check that both lhs and rhs are Boolean values
        if (!(lhs instanceof Boolean && rhs instanceof Boolean)) {
            error.flag(TYPE_MUST_BE_BOOLEAN, ctx.getStart().getLine(),ctx.getText());
            return null;
        }

        boolean lhsBool = (Boolean) lhs;
        boolean rhsBool = (Boolean) rhs;

        // Perform the logical operation based on the operator
        if (operator.equals("&&")) {
            return lhsBool && rhsBool;
        } else if (operator.equals("||")) {
            return lhsBool || rhsBool;
        } else {
            error.flag(INVALID_OPERATOR, ctx.getStart().getLine(),ctx.getText());
            return null;
        }
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

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitVariableDecl(JavanaParser.VariableDeclContext ctx) {
        visit(ctx.typeAssoc());

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
    public Object visitTypeAssoc(JavanaParser.TypeAssocContext ctx) {
        for(JavanaParser.IdentifierContext name : ctx.namelst.names) {
            SymTableEntry decl = symTableStack.enterLocal(name.getText(), VARIABLE);
            decl.setType(TypeChecker.returnType(ctx.children.get(2).getText()));
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

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitFunctionCallExpression(JavanaParser.FunctionCallExpressionContext ctx) {
        SymTableEntry function = symTableStack.lookup(ctx.functionCall().identifier().getText());
        List<SymTableEntry> params = function.getRoutineParameters();
        int argsPassed = ctx.functionCall().exprList() == null? 0 : ctx.functionCall().exprList().expression().size();

        if(params.size() != argsPassed){
            error.flag(ARGUMENT_COUNT_MISMATCH, ctx.getStart().getLine(), ctx.getText());
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
    public Object visitFuncPrototype(JavanaParser.FuncPrototypeContext ctx) {
        String funcName = ctx.name.getText();
        Typespec type = TypeChecker.returnType(ctx.returnType().children.get(0).getText());
        List<JavanaParser.FuncArgumentContext> parameters = ctx.funcArgList().args;

        SymTableEntry routineId = symTableStack.lookupLocal(funcName);

        if (routineId != null) {
            error.flag(REDECLARED_IDENTIFIER, ctx.getStart().getLine(), funcName);
            return null;
        }

        routineId = symTableStack.enterLocal(funcName, FUNCTION);
        routineId.setRoutineCode(SymTableEntry.Routine.DECLARED);
        routineId.setType(type);

        SymTableEntry parentId = symTableStack.getLocalSymTable().getOwner();
        parentId.appendSubroutine(routineId);

        routineId.setRoutineSymTable(symTableStack.push());

        SymTable symTable = symTableStack.getLocalSymTable();
        symTable.setOwner(routineId);

        if (parameters != null) {
            List<SymTableEntry> parameterIds = new ArrayList<>();
            for(JavanaParser.FuncArgumentContext arg : parameters){
                //pull out type assoc
                JavanaParser.TypeAssocContext typeAssoc = arg.typeAssoc();


                //deal with type assoc
                for(JavanaParser.IdentifierContext name : typeAssoc.namelst.names) {
                    SymTableEntry entry = routineId.getRoutineSymTable().enter(name.getText(), VALUE_PARAMETER);
                    entry.setType(TypeChecker.returnType(typeAssoc.children.get(2).getText()));
                    parameterIds.add(entry);
                }

                routineId.setRoutineParameters((ArrayList<SymTableEntry>) parameterIds);

                for (SymTableEntry paramId : parameterIds) {
                    paramId.setSlotNumber(symTable.nextSlotNumber());
                }

            }

        }

        return null;
    }

    @Override
    public Object visitConstantDefinition(JavanaParser.ConstantDefinitionContext ctx) {


        String constantName = ctx.nameList().identifier(0).getText().toLowerCase(); // Adjust based on actual method names
        SymTableEntry constantId = symTableStack.lookup(constantName);

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

        SymTableEntry variable = symTableStack.lookup(lhs);

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
    public Object visitRelationalExpression(JavanaParser.RelationalExpressionContext ctx) {
        Object lhs = visit(ctx.expression(0)); // Visit left-hand side expression
        Object rhs = visit(ctx.expression(1)); // Visit right-hand side expression
        String operator = ctx.REL_OP().getText(); // Get the relational operator

        if (!(lhs instanceof Integer && rhs instanceof Integer)) {
            error.flag(TYPE_MISMATCH, ctx.getStart().getLine(),ctx.getText());
            return null;
        }

        int lhsInt = (Integer) lhs;
        int rhsInt = (Integer) rhs;

        switch (operator) {
            case ">":
                return lhsInt > rhsInt;
            case "<":
                return lhsInt < rhsInt;
            case ">=":
                return lhsInt >= rhsInt;
            case "<=":
                return lhsInt <= rhsInt;
            default:
                error.flag(INVALID_OPERATOR, ctx.getStart().getLine(),ctx.getText());
                return null;
        }
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
        return ctx.getText();
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
            }
            if (operator.equals("-")) {
                return (int) lhs - (int) rhs;
            }
//            else {
//                return (int) lhs - (int) rhs;
//            }
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


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     *
     * @param ctx
     */
    @Override
    public Object visitPrintLineStatement(JavanaParser.PrintLineStatementContext ctx) {
        System.out.printf("Print: %s\n", visit(ctx.printArgument().children.get(0).getChild(1)));

        return null;
    }
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
    public Object visitEqualityExpression(JavanaParser.EqualityExpressionContext ctx) {
        Object lhs = visit(ctx.expression(0));
        Object rhs = visit(ctx.expression(1));
        String operator = ctx.EQ_OP().getText();

        // Ensure both operands are of the same type
        if (TypeChecker.returnType(lhs) != TypeChecker.returnType(rhs)) {
            error.flag(TYPE_MISMATCH, ctx.getStart().getLine(), ctx.getText());
            return null;
        }

        // If both are integers, check equality based on the operator
        if (lhs instanceof Integer && rhs instanceof Integer) {
            if (operator.equals("==")) {
                return lhs.equals(rhs); // or (int)lhs == (int)rhs
            } else {
                return !lhs.equals(rhs); // or (int)lhs != (int)rhs
            }
        }

        // If both are booleans, check equality based on the operator
        if (lhs instanceof Boolean && rhs instanceof Boolean) {
            if (operator.equals("==")) {
                return lhs.equals(rhs); // or (boolean)lhs == (boolean)rhs
            } else {
                return !lhs.equals(rhs); // or (boolean)lhs != (boolean)rhs
            }
        }

        // If both are strings, check equality using .equals()
        if (lhs instanceof String && rhs instanceof String) {
            if (operator.equals("==")) {
                return lhs.equals(rhs);
            } else {
                return !lhs.equals(rhs);
            }
        }

        // If types are not compatible or operator is not recognized, flag an error
        error.flag(INVALID_OPERATOR, ctx.getStart().getLine(), ctx.getText());
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
        SymTableEntry frame =  symTableStack.lookup(varName);

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