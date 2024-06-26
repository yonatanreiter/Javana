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

    private SymTableStack symTableStack;
    private SymTableStack tempTableStack  = new SymTableStack();
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
        visit(ctx.hdr);
        for(ParseTree child : ctx.defs){
            visit(child);
        }


        for(int i = 0; i <= symTableStack.getCurrentNestingLevel(); i++){
            try{
                tempTableStack.set(i,symTableStack.get(i));
            }catch (Exception e){
                tempTableStack.push(symTableStack.get(i));
            }
        }


        visit(ctx.main);

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

            if(symTableStack.lookupLocal(name.getText()) != null){
                error.flag(REDECLARED_IDENTIFIER, ctx.start.getLine(), ctx.getText());
                continue;
            }

            SymTableEntry decl = symTableStack.enterLocal(name.getText(), VARIABLE);
            String toSearch = ctx.children.get(2).getText();
            boolean isArr = false;

            if(((JavanaParser.TypeContext) ctx.children.get(2)).children.get(0) instanceof JavanaParser.RecordArrayCompositeTypeContext){
               toSearch = toSearch.substring(0, toSearch.length()-2);
               isArr = true;
            }
            Typespec type = TypeChecker.returnType(toSearch);

            if(type == null){
                SymTableEntry entry = symTableStack.lookup(toSearch);
                if(entry == null){
                    error.flag(UNDECLARED_IDENTIFIER, ctx.start.getLine(), ctx.getText());
                    continue;
                }
                if(!isArr) {
                    type = entry.getType();
                }
                else{
                    Typespec typespec = new Typespec(Typespec.Form.ARRAY);
                    typespec.setArrayElementType(entry.getType());
                    type = typespec;
                }
            }
            decl.setType(type);

        }

        return null;
    }

    @Override
    public Object visitMainMethod(JavanaParser.MainMethodContext ctx){
        JavanaParser.BlockStatementContext blockCtx = ctx.blockStatement();
        SymTable mainTable = symTableStack.push();
        mainTable.setOwner(programId);

        if(ctx.args != null){
            visit(ctx.mainArg());
        }

        visit(blockCtx);
        symTableStack.pop();
        return null;
    }

    @Override
    public Object visitMainArg(JavanaParser.MainArgContext ctx) {
        SymTableEntry entry = symTableStack.enterLocal(ctx.name.getText(), PROGRAM_PARAMETER);
        Typespec type = new Typespec(Typespec.Form.ARRAY);
        type.setArrayElementType(Predefined.stringType);
        type.setArrayIndexType(Predefined.integerType);
        entry.setType(type);

        return null;
    }

    @Override
    public Object visitRecordDecl(JavanaParser.RecordDeclContext ctx) {
        // Create an unnamed record type.
        String recordTypeName = ctx.name.getText();
        createRecordType(ctx, recordTypeName);

        return null;
    }



    private SymTableEntry createRecordType(JavanaParser.RecordDeclContext ctx, String recordTypeName) {
        List<JavanaParser.TypeAssocContext> typeAssocs = ctx.typeAssoc();
        Typespec recordType = new Typespec(Typespec.Form.RECORD);

        SymTableEntry recordTypeId = symTableStack.enterLocal(recordTypeName, TYPE);
        recordTypeId.setType(recordType);
        recordType.setIdentifier(recordTypeId);

        String recordTypePath = createRecordTypePath(recordType);
        recordType.setRecordTypePath(recordTypePath);

        SymTable recordSymTable = createRecordSymTable(typeAssocs, recordTypeId);
        recordType.setRecordSymTable(recordSymTable);

        return recordTypeId;
    }

    private SymTable createRecordSymTable(List<JavanaParser.TypeAssocContext> typeAssocs, SymTableEntry ownerId) {
        SymTable recordSymTable = symTableStack.push();

        recordSymTable.setOwner(ownerId);

        for(JavanaParser.TypeAssocContext assoc : typeAssocs){
            visit(assoc);
        }

        recordSymTable.resetVariables(RECORD_FIELD);
        symTableStack.pop();

        return recordSymTable;
    }

    private String createRecordTypePath(Typespec recordType) {
        SymTableEntry recordId = recordType.getIdentifier();
        SymTableEntry parentId = recordId.getSymTable().getOwner();
        String path = recordId.getName();

        while ((parentId.getKind() == TYPE) && (parentId.getType().getForm() == Typespec.Form.RECORD)) {
            path = parentId.getName() + "$" + path;
            parentId = parentId.getSymTable().getOwner();
        }

        path = parentId.getName() + "$" + path;
        return path;
    }

    @Override
    public Object visitRecordFieldExpression(JavanaParser.RecordFieldExpressionContext ctx) {
        SymTableEntry record = symTableStack.lookup(ctx.expression().getText());
        if(record == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.start.getLine(), ctx.getText());
        }

        record  = record.getType().getRecordSymTable().lookup(ctx.identifier().getText());

        if(record == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.start.getLine(), ctx.getText());
        }

        return record;
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
    public Object visitFuncDefinition(JavanaParser.FuncDefinitionContext ctx) {
        SymTableEntry entry = (SymTableEntry) visitFuncPrototype(ctx.funcPrototype());
        entry.setExecutable(ctx.blockStatement());

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

        if(function == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.start.getLine(), ctx.getText());
            return null;
        }

        if (function.getKind() != FUNCTION) {
            error.flag(NAME_MUST_BE_FUNCTION, ctx);
            return null;
        }

        List<SymTableEntry> params = function.getRoutineParameters();
        int argsPassed = ctx.functionCall().exprList() == null? 0 : ctx.functionCall().exprList().expression().size();

        if(params.size() != argsPassed){
            error.flag(ARGUMENT_COUNT_MISMATCH, ctx.getStart().getLine(), ctx.getText());
        }

//        SymTableStack hodl = symTableStack;
//        symTableStack = tempTableStack;
//        symTableStack.push(function.getRoutineSymTable());

        Object result = visit(ctx.functionCall());

//        symTableStack.pop();
//        symTableStack = hodl;

        return function.getType();
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
    public Object visitFunctionCall(JavanaParser.FunctionCallContext ctx) {
        JavanaParser.ExprListContext exprList = ctx.exprList();
        String name = ctx.name.getText();

        //funcTable.setOwner();
        SymTableEntry functionId = symTableStack.lookup(name);
        boolean badName = false;
        

        if (functionId == null) {
            error.flag(UNDECLARED_IDENTIFIER, ctx);
            badName = true;
        }

        // Bad function name. Do a simple arguments check and then leave.
        if (badName) {
            for (JavanaParser.ExpressionContext exprCtx : exprList.expression()) {
                visit(exprCtx);
            }
        }

        // Good function name.
        else {
            ArrayList<SymTableEntry> parameters = functionId.getRoutineParameters();
            checkCallArguments(exprList, parameters);

            if(ctx.getStart().getLine() == 83){
                int dd = 2;
            }

            updateTempStack();
            SymTableStack hodl = symTableStack;
            symTableStack = tempTableStack;
            symTableStack.push(functionId.getRoutineSymTable());

            visit((ParseTree) functionId.getExecutable());

            symTableStack.pop();
            symTableStack = hodl;
            updateRealStack();

        }


        return null;
    }

    private void updateTempStack(){
        tempTableStack.set(0, symTableStack.get(0));
        tempTableStack.set(1, symTableStack.get(1));
    }

    private void updateRealStack(){
        symTableStack.set(0, tempTableStack.get(0));
        symTableStack.set(1, tempTableStack.get(1));
    }

    private void checkCallArguments(JavanaParser.ExprListContext listCtx, ArrayList<SymTableEntry> parameters) {
        SymTable top = symTableStack.get(symTableStack.getCurrentNestingLevel());
        int paramsCount = parameters.size();
        int argsCount = listCtx != null ? listCtx.expression().size() : 0;

//        if (paramsCount != argsCount) {
//            error.flag(ARGUMENT_COUNT_MISMATCH, listCtx);
//            return;
//        }

        // Check each argument against the corresponding parameter.
        for (int i = 0; i < paramsCount; i++) {
            JavanaParser.ExpressionContext exprCtx = listCtx.expression(i);
            Object returnType = visit(exprCtx);

            SymTableEntry paramId = parameters.get(i);
            Typespec paramType = paramId.getType();
            Typespec argType = TypeChecker.returnType(returnType);

            // For a VAR parameter, the argument must be a variable
            // with the same datatype.
            if (paramId.getKind() == REFERENCE_PARAMETER) {
//                if (expressionIsVariable(exprCtx)) {
//                    if (paramType != argType) {
//                        error.flag(TYPE_MISMATCH, exprCtx);
//                    }
//                } else {
//                   // error.flag(ARGUMENT_MUST_BE_VARIABLE, exprCtx);
//                }
            }

            // For a value parameter, the argument type must be
            // assignment compatible with the parameter type.
            else if (!TypeChecker.areAssignmentCompatible(paramType, argType)) {
                error.flag(TYPE_MISMATCH, exprCtx);
            }
            else{
                paramId.setValue(returnType);
            }
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
    public Object visitFuncPrototype(JavanaParser.FuncPrototypeContext ctx) {
        String funcName = ctx.name.getText();
        Typespec type = TypeChecker.returnType(ctx.returnType().children.get(0).getText());
        List<JavanaParser.FuncArgumentContext> parameters = null;
        if(ctx.funcArgList != null) parameters = ctx.funcArgList().args;

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

            }

            for (SymTableEntry paramId : parameterIds) {
                paramId.setSlotNumber(symTable.nextSlotNumber());
            }

        }
        symTableStack.pop();

        return routineId;
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
    public Object visitExpressionStatement(JavanaParser.ExpressionStatementContext ctx) {
        visit(ctx.expression());
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
    public Object visitArrayIndexExpression(JavanaParser.ArrayIndexExpressionContext ctx) {
        String lhs = ctx.expression().getText();


        SymTableEntry variable = symTableStack.lookup(lhs);

        if(variable == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),lhs);
        }

        else{

                if(!variable.getType().getForm().toString().equals("array")){
                    error.flag(TYPE_MUST_BE_ARRAY, ctx.getStart().getLine(), ctx.getText());
                }
                else{

                    //Grab whatever is inside the brackets
                    Object inside = visit(ctx.arrIdxSpecifier().children.get(1));
                    if(TypeChecker.returnType(inside) != Predefined.integerType){
                        error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
                    }

                    return variable.getType().getArrayElementType();

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
    public Object visitIfStatement(JavanaParser.IfStatementContext ctx) {
        symTableStack.push();
        Object expressionResult = visit(ctx.expression());

        if(TypeChecker.returnType(expressionResult) != Predefined.booleanType){
            error.flag(TYPE_MUST_BE_BOOLEAN, ctx.getStart().getLine(),ctx.expression().getText());
            return null;
        }

        //just semantic checking so visit both

        symTableStack.push();
        visit(ctx.thenStmt);
        symTableStack.pop();

        if(ctx.elseStmt != null) {
            symTableStack.push();
            visit(ctx.elseStmt);
            symTableStack.pop();
        }

        symTableStack.pop();
        return null;
    }

    @Override
    public Object visitForStatement(JavanaParser.ForStatementContext ctx) {
        symTableStack.push();

        if(ctx.start.getLine() == 14){
            int i = 2;
        }


        visit(ctx.init);
        Object obj = visit(((JavanaParser.VariableDefinitionContext) ctx.init).expr);

        if(obj != null && TypeChecker.returnType(obj) != Predefined.integerType){
            error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.init.getText());

        }


        obj = visit(ctx.condition);
        if(obj != null && TypeChecker.returnType(obj) != Predefined.booleanType){
            error.flag(TYPE_MUST_BE_BOOLEAN, ctx.getStart().getLine(), ctx.condition.getText());

        }
        visit(ctx.updateExpr);
        visit(ctx.body);
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
    public Object visitWhileStatement(JavanaParser.WhileStatementContext ctx) {
        symTableStack.push();
        Object expressionResult = visit(ctx.expression());

        if(TypeChecker.returnType(expressionResult) != Predefined.booleanType){
            error.flag(TYPE_MUST_BE_BOOLEAN, ctx.getStart().getLine(),ctx.expression().getText());
            return null;
        }


        visit(ctx.blockStatement());

        expressionResult = visit(ctx.expression());



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
    public Object visitReturnStatement(JavanaParser.ReturnStatementContext ctx) {
        if(ctx.expression() != null) {
            return visit(ctx.expression());
        }else return null;
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
        boolean isArrayBracket = false;

        if(ctx.variable().children.size() > 1){
            if(ctx.variable().children.get(1) instanceof JavanaParser.VarArrayIndexModfierContext){
                isArrayBracket = true;
            }
            else if(ctx.variable().children.size() > 2 && ctx.variable().children.get(2) instanceof JavanaParser.VarArrayIndexModfierContext){
                isArrayBracket = true;
            }
        }

        SymTableEntry variable = symTableStack.lookup(lhs);
        if(ctx.variable().children.size() > 1 && ctx.variable().children.get(1) instanceof JavanaParser.VarRecordFieldModifierContext){
            String recordFieldId = ((JavanaParser.VarRecordFieldModifierContext) ((JavanaParser.VariableContext) ctx.children.get(0)).children.get(1)).identifier().getText();
            Object val = variable.getValue();
            if(val == null){
                variable.setValue(variable.getType());
                val = variable.getValue();
            }
            variable =  ((Typespec) val).getRecordSymTable().get(recordFieldId);

        }

        if(variable == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),lhs);
        }

        else{

            if(variable.getKind() == CONSTANT){
                error.flag(INCOMPATIBLE_ASSIGNMENT, ctx.getStart().getLine(), lhs);
            }

            Object value = visit(ctx.expression());

            if(isArrayBracket){

                if(!variable.getType().getForm().toString().equals("array")){
                    error.flag(TYPE_MUST_BE_ARRAY, ctx.getStart().getLine(), ctx.getText());
                }
                else{

                    if(TypeChecker.returnType(value) != variable.getType().getArrayElementType()) {

                        if(((Typespec) value).getForm() == variable.getType().getArrayElementType().getForm()){
                            dealWithRecordInArray(variable, value, ctx);
                        }
                        else {

                            error.flag(INCOMPATIBLE_ASSIGNMENT, ctx.getStart().getLine(), ctx.getText());
                        }
                    }

                    else{
                        //Grab whatever is inside the brackets
                        Object inside = visit(ctx.variable().varModifier.children.get(0).getChild(1));
                        if(TypeChecker.returnType(inside) != Predefined.integerType){
                            error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
                        }
                    }
                }
            }
            else if(!TypeChecker.areAssignmentCompatible(TypeChecker.returnType(value), variable.getType())){
                error.flag(TYPE_MISMATCH, ctx.getStart().getLine(), ctx.getText());

            }else {
                //variable = symTableStack.enterLocal(lhs, VARIABLE);
                variable.setValue(value);
            }
        }




        return null;
    }

    private void dealWithRecordInArray(SymTableEntry variable, Object value, JavanaParser.AssignmentStatementContext ctx) {
        Typespec valueType = (Typespec) value;

        if(!(variable.getType().getForm() == variable.getType().getForm())
        || !(Objects.equals(variable.getType().getArrayElementType().getIdentifier().getName(), ((Typespec) value).getIdentifier().getName()))){

            error.flag(TYPE_MISMATCH, ctx.getStart().getLine(), ctx.getText());
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
    public Object visitNewArrayExpression(JavanaParser.NewArrayExpressionContext ctx) {
        return visit(ctx.newArray());
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
    public Object visitNewArray(JavanaParser.NewArrayContext ctx) {
        Typespec type = null;
        if(ctx.scalarType() != null){
            type = TypeChecker.returnType(ctx.scalarType().children.get(0).getText());
        }
        else{
            SymTableEntry entry = symTableStack.lookup(ctx.identifier().children.get(0).getText());
            if(entry == null){
                error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(), ctx.getText());
                return null;
            }

            type = entry.getType();
        }
        Object elementCount = visit(ctx.arrIdxSpecifier().expression());

        if(TypeChecker.returnType(elementCount) != Predefined.integerType){
            error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
            return null;
        }

        Typespec spec = new Typespec(Typespec.Form.ARRAY);
        spec.setArrayElementCount((int)elementCount);
        spec.setArrayElementType(type);
        spec.setArrayIndexType(Predefined.integerType);
        return spec;
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
    public Object visitVariable(JavanaParser.VariableContext ctx) {
        String lhs = ctx.name.getText();
        boolean isArrayBracket = false;

        if(ctx.children.size() > 1){
            if(ctx.children.get(1) instanceof JavanaParser.VarArrayIndexModfierContext){
                isArrayBracket = true;
            }
        }

        SymTableEntry variable = symTableStack.lookup(lhs);

        if(variable == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(),lhs);
        }

        else{


            if(isArrayBracket){

                if(!variable.getType().getForm().toString().equals("array")){
                    error.flag(TYPE_MUST_BE_ARRAY, ctx.getStart().getLine(), ctx.getText());
                }
                else{

                        //Grab whatever is inside the brackets
                        Object inside = visit(ctx.varModifier.children.get(0).getChild(1));
                        if(TypeChecker.returnType(inside) != Predefined.integerType){
                            error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
                        }

                }
            }

        }

        return null;
    }

    @Override
    public Object visitParenthesizedExpression(JavanaParser.ParenthesizedExpressionContext ctx) {
        return visit(ctx.expression());
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

        for(ParseTree tree : ctx.nameList().names) {
            String varName = tree.getChild(0).getText();

            SymTableEntry varId = symTableStack.lookupLocal(varName);

            if (varId == null) {


                Object varVal = visit(ctx.expression());

                if (varVal != null) {
                    varId = symTableStack.enterLocal(varName, VARIABLE);

                    varId.setValue(varVal);
                    if (varVal instanceof Typespec) {
                        varId.setType((Typespec) varVal);

                        //if its a record
                        if (((Typespec) varVal).baseType().getForm().name().equals("RECORD")) {
                            //go through the fields
                            for (JavanaParser.FieldInitContext field : ((JavanaParser.NewRecordContext) ctx.expression().children.get(0)).init.fieldInit()) {

                                //collect the name of the record
                                String recordType = ctx.expression().children.get(0).getChild(1).getText();
                                SymTableEntry recordEntry = symTableStack.lookup(recordType);

                                //if record doesn't exist
                                if (recordEntry == null) {
                                    error.flag(UNDECLARED_IDENTIFIER, field.getStart().getLine(), varName);
                                    return null;
                                }

                                //get the type of the field and whats being assigned to it
                                Typespec valueType = TypeChecker.returnType(visit(field.expression()));
                                Typespec varType = recordEntry.getType().getRecordSymTable().lookup((String) visit(field.identifier())).getType();
                                if (!valueType.equals(varType)) {

                                    if (varType.baseType().getForm().equals(Typespec.Form.ARRAY)) {
                                        if (!(
                                                varType.baseType().getForm() == valueType.baseType().getForm()
                                                        && varType.getArrayElementType().getIdentifier().equals(valueType.getArrayElementType().getIdentifier()))
                                        ) {
                                            error.flag(ILLEGAL_ASSIGNMENT, field.getStart().getLine(), field.getText());
                                        }
                                    } else if (!(varType.getForm() == valueType.getForm() && varType.getIdentifier() == valueType.getIdentifier())) {
                                        error.flag(ILLEGAL_ASSIGNMENT, field.getStart().getLine(), field.getText());
                                    }


                                }

                            }
                        }

                    } else {
                        varId.setType(TypeChecker.returnType(varVal));

                    }
                }


            } else {
                error.flag(REDECLARED_IDENTIFIER, ctx.getStart().getLine(), varName);

                varId = symTableStack.enterLocal(varName, VARIABLE);
//            varId.setValue(0);
//            varId.setType(Predefined.undefinedType);
            }
        }




        return null;
    }

    @Override
    public Object visitStringToIntCallExpression(JavanaParser.StringToIntCallExpressionContext ctx) {
        return 1;
    }

    @Override
    public Object visitVarRecordFieldModifier(JavanaParser.VarRecordFieldModifierContext ctx) {
        return super.visitVarRecordFieldModifier(ctx);
    }

    @Override
    public Object visitNewRecordExpression(JavanaParser.NewRecordExpressionContext ctx) {
        SymTable table = symTableStack.push();

        String recordId = ((JavanaParser.NewRecordContext) ctx.children.get(0)).identifier().getText();
        if(symTableStack.lookup(recordId) == null){
            error.flag(REDECLARED_IDENTIFIER, ctx.getStart().getLine(), ctx.getText());
            return null;
        }

        Typespec type = new Typespec(Typespec.Form.RECORD);
        JavanaParser.FieldInitListContext list = ((JavanaParser.NewRecordContext) ctx.children.get(0)).fieldInitList();
        for(JavanaParser.FieldInitContext field  : list.fieldInit()){
            String id = (String) visit(field.identifier());
            Object obj = visit(field.expression());
            if(table.lookup(id) != null){
                error.flag(REDECLARED_IDENTIFIER, field.getStart().getLine(), field.getText());
            }
            else{
                table.enter(id, RECORD_FIELD);
                table.get(id).setType(TypeChecker.returnType(obj));
                table.get(id).setValue(obj);
            }

        }

        type.setIdentifier(symTableStack.lookup(recordId));
        type.setRecordSymTable(table);

        symTableStack.pop();
        return type;
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



        if(lhs == null || rhs == null){
            SymTableEntry l = symTableStack.lookup(ctx.children.get(0).getText());
            SymTableEntry r = symTableStack.lookup(ctx.children.get(2).getText());

            if(l != null && r != null){
                return 0;
            }
        }

        boolean oneIsInt = TypeChecker.returnType(lhs).getIdentifier().getName().equals("integer") || TypeChecker.returnType(rhs).getIdentifier().getName().equals("integer");


        //in case one is a typespec
        if(TypeChecker.oneIsTypeSpecOneIsActualType(lhs, rhs)){
            return lhs;
        }

        if (TypeChecker.returnType(rhs) != TypeChecker.returnType(lhs)) {
            if (oneIsInt) {
                error.flag(TYPE_MUST_BE_INTEGER, ctx.getStart().getLine(), ctx.getText());
            }
            else {
                error.flag(INVALID_OPERATOR, ctx.getStart().getLine(), ctx.getText());
            }
            return null;
        } else {
            if (operator.equals("+")) {
                try{
                    return (int) lhs + (int) rhs;
                }catch (Exception e){
                    System.out.println("Error at " +  ctx.getStart().getLine());
                    throw e;
                }
            } else {
                try{
                    return (int) lhs - (int) rhs;
                }catch (Exception e){
                    System.out.println("Error at " +  ctx.getStart().getLine());
                    throw e;
                }
            }
        }
    }

    @Override
    public Object visitConcatenateStringsExpression(JavanaParser.ConcatenateStringsExpressionContext ctx) {
        return "a";
    }

    @Override
    public Object visitConcatenateStringsCall(JavanaParser.ConcatenateStringsCallContext ctx) {
        return "a";
    }

    @Override
    public Object visitStringCharToValCall(JavanaParser.StringCharToValCallContext ctx) {
        return 0;
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

        //in case one is a typespec
        if(TypeChecker.oneIsTypeSpecOneIsActualType(lhs, rhs)){
            return lhs;
        }


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
    public Object visitNotExpression(JavanaParser.NotExpressionContext ctx) {
        Object a  = visit(ctx.expression());
        return a;
    }

    @Override
    public Object visitStringLengthExpression(JavanaParser.StringLengthExpressionContext ctx) {
        return 0;
    }


    @Override
    public Object visitReadCharCall(JavanaParser.ReadCharCallContext ctx) {
        return "a";
    }

    @Override
    public Object visitReadLineCall(JavanaParser.ReadLineCallContext ctx) {
        return "a";
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

        //in case one is a typespec
        if(TypeChecker.oneIsTypeSpecOneIsActualType(lhs, rhs)){
            return Predefined.booleanType;
        }


        if (!(lhs instanceof Integer && rhs instanceof Integer)) {

            error.flag(INCOMPATIBLE_COMPARISON, ctx.getStart().getLine(),ctx.getText());

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
    public Object visitPrintLineStatement(JavanaParser.PrintLineStatementContext ctx) {
        visit(ctx.printArgument());
       // System.out.printf("Print: %s\n", visit(ctx.printArgument().children.get(0).getChild(1)));

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
    public Object visitPrintStatement(JavanaParser.PrintStatementContext ctx) {
        visit(ctx.printArgument());
        //System.out.printf("Print: %s", visit(ctx.printArgument().children.get(0).getChild(1)));

        return null;
    }

    @Override
    public Object visitHigherArithmeticExpression(JavanaParser.HigherArithmeticExpressionContext ctx) {
        Object lhs = visit(ctx.children.get(0));
        Object rhs = visit(ctx.children.get(2));
        String operator = ctx.children.get(1).getText();

        if(lhs == null || rhs == null){
            SymTableEntry l = symTableStack.lookup(ctx.children.get(0).getText());
            SymTableEntry r = symTableStack.lookup(ctx.children.get(2).getText());

            if(l != null && r != null){
                return 0;
            }
        }

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

    @Override
    public Object visitStringEquals(JavanaParser.StringEqualsContext ctx) {
        return Predefined.booleanType;
    }

    @Override
    public Object visitArrayLengthExpression(JavanaParser.ArrayLengthExpressionContext ctx) {
        return visit(ctx.arrayLength());
    }

    @Override
    public Object visitArrayLength(JavanaParser.ArrayLengthContext ctx) {
        return Predefined.integerType;
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


        if(lhs ==  null || rhs == null){
                return Predefined.booleanType;
        }

        // Ensure both operands are of the same type
        if (TypeChecker.returnType(lhs) != TypeChecker.returnType(rhs)) {
            error.flag(INCOMPATIBLE_COMPARISON, ctx.getStart().getLine(), ctx.getText());
            return Predefined.booleanType;
        }
        else{
            return Predefined.booleanType;
        }

//        // If both are integers, check equality based on the operator
//        if (lhs instanceof Integer && rhs instanceof Integer) {
//            if (operator.equals("==")) {
//                return lhs.equals(rhs); // or (int)lhs == (int)rhs
//            } else {
//                return !lhs.equals(rhs); // or (int)lhs != (int)rhs
//            }
//        }
//
//        // If both are booleans, check equality based on the operator
//        if (lhs instanceof Boolean && rhs instanceof Boolean) {
//            if (operator.equals("==")) {
//                return lhs.equals(rhs); // or (boolean)lhs == (boolean)rhs
//            } else {
//                return !lhs.equals(rhs); // or (boolean)lhs != (boolean)rhs
//            }
//        }
//
//        // If both are strings, check equality using .equals()
//        if (lhs instanceof String && rhs instanceof String) {
//            if (operator.equals("==")) {
//                return lhs.equals(rhs);
//            } else {
//                return !lhs.equals(rhs);
//            }
//        }

        // If types are not compatible or operator is not recognized, flag an error
        //error.flag(INVALID_OPERATOR, ctx.getStart().getLine(), ctx.getText());
       // return null;
    }

    @Override
    public Object visitSubstringCall(JavanaParser.SubstringCallContext ctx) {
        return "a";
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

        if(varName.equals("secretWord")){
            int a = 1;
        }

        if(frame == null){
            error.flag(UNDECLARED_IDENTIFIER, ctx.getStart().getLine(), ctx.getText());

            return null;
        }

        try {
            Object end = frame.getValue();
            if(end == null){
                return frame.getType();
            }
            return end;
        }catch (NullPointerException e){
            try {
                Object end = frame.getType();
                return end;
            }catch (NullPointerException f){
                return null;
            }
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