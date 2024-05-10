package edu.yu.compilers.backend.converter;

import antlr4.JavanaBaseVisitor;
import antlr4.JavanaParser;
import edu.yu.compilers.intermediate.symtable.SymTable;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;
import edu.yu.compilers.intermediate.type.Typespec;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import static edu.yu.compilers.intermediate.symtable.SymTableEntry.Kind.*;
import static edu.yu.compilers.intermediate.type.Typespec.Form.*;

/**
 * Convert Javana programs to Java.
 */
public class Converter extends JavanaBaseVisitor<Object> {

    // Map a Pascal datatype name to the Java datatype name.
    private static final Hashtable<String, String> typeNameTable;

    static {
        typeNameTable = new Hashtable<>();
        typeNameTable.put("int", "int");
        typeNameTable.put("bool", "boolean");
        typeNameTable.put("string", "String");
    }

    private CodeGenerator code;

    private String programName;

    private boolean programVariables = true;
    private boolean recordFields = false;
    private String currentRecordName = "";

    public String getProgramName() {
        return programName;
    }

    @Override
    public Object visitProgram(JavanaParser.ProgramContext ctx) {
        StringWriter sw = new StringWriter();
        code = new CodeGenerator(new PrintWriter(sw));

        code.emitLine("import java.util.Scanner;\n");

        //visit(ctx.programHeader());

        code.emitLine("public class " + ctx.programHeader().name.getText());
        code.emit("{");

        // Execution timer and runtime standard input.
        code.indent();

        // Level 1 declarations.
        JavanaParser.ProgramHeaderContext idCtx =
                ctx.programHeader();
        if(ctx.defs != null){
            for(ParseTree tree : ctx.defs){
                visit(tree);
                code.emitLine();
            }
        }

        code.emitLine("static Scanner scanner = new Scanner(System.in);");
        //emitUnnamedRecordDefinitions(ctx.);

        // Main.
        code.emitLine();
        code.emitLine("public static void main(String[] args)");
        code.emitLine("{");
        code.indent();

        // Allocate structured data.
        //emitAllocateStructuredVariables("", idCtx.entry.getRoutineSymTable());
        code.emitLine();

        // Main compound statement.
        visit(ctx.main);

        code.dedent();
        code.emitLine("}");

        code.dedent();
        code.emitLine("}");

        code.close();
        return sw.toString();

    }

    @Override
    public Object visitMainMethod(JavanaParser.MainMethodContext ctx) {
        visit(ctx.blockStatement());
        return null;
    }

    @Override
    public Object visitBlockStatement(JavanaParser.BlockStatementContext ctx) {
        for(ParseTree statement : ctx.statement()){
            code.emitStart();
            visit(statement);
        }

        return null;
    }

    @Override
    public Object visitStatement(JavanaParser.StatementContext ctx) {
        return visit(ctx.children.get(0));
    }

    @Override
    public Object visitNameDeclDefStatement(JavanaParser.NameDeclDefStatementContext ctx) {
        return visit(ctx.children.get(0));
    }

    @Override
    public Object visitVariableDefinition(JavanaParser.VariableDefinitionContext ctx) {
        //code.emitStart();
        for(JavanaParser.IdentifierContext id : ctx.nameList().names) {
            StringBuilder names = new StringBuilder(ctx.nameList().names.get(0).children.get(0).getText());

            code.emit(String.format("var ", names));
            visit(id);
            code.emit(String.format(" = ", names));


            if(ctx.expression() instanceof JavanaParser.NewRecordExpressionContext){
                currentRecordName = names.toString();
            }
            visit(ctx.expression());
            code.emit("; ");
        }

        return null;
    }

    @Override
    public Object visitArrayIndexExpression(JavanaParser.ArrayIndexExpressionContext ctx) {
        return super.visitArrayIndexExpression(ctx);
    }

    @Override
    public Object visitNewArray(JavanaParser.NewArrayContext ctx) {
        code.emit("new " + typeNameTable.get(ctx.scalarType().getText()));
        visit(ctx.arrIdxSpecifier());

        return null;
    }

    @Override
    public Object visitVarArrayIndexModfier(JavanaParser.VarArrayIndexModfierContext ctx) {
       visit(ctx.arrIdxSpecifier());
       return null;
    }

    @Override
    public Object visitArrIdxSpecifier(JavanaParser.ArrIdxSpecifierContext ctx) {
        code.emit("[");
        visit(ctx.expr);
        code.emit("]");

        return null;
    }

    @Override
    public Object visitForStatement(JavanaParser.ForStatementContext ctx) {
        code.emitStart();
        code.emit("for(");
        visit(ctx.init);
        visit(ctx.condition);
        code.emit("; ");
        visit(ctx.updateExpr.getChild(0));
        visit(ctx.updateExpr.getChild(1));
        code.emit("=");
        visit(ctx.updateExpr.getChild(2));
        code.emit(")");

        code.emitLine("{");
        code.indent();

        visit(ctx.body);
        code.dedent();
        code.emitLine("}");
        return null;
    }



    @Override
    public Object visitRelationalExpression(JavanaParser.RelationalExpressionContext ctx) {
        for(ParseTree tree : ctx.children){
            visit(tree);
            code.emit(" ");
        }

        return null;
    }

    @Override
    public Object visitAssignmentStatement(JavanaParser.AssignmentStatementContext ctx) {
        for(ParseTree tree : ctx.children){
            visit(tree);
            code.emit(" ");
        }

        code.emit(";");
        return null;
    }

    @Override
    public Object visitWhileStatement(JavanaParser.WhileStatementContext ctx) {
        code.emitStart();
        code.emit("while(");
        for(ParseTree tree : ctx.condition.children){
            visit(tree);
            code.emit(" ");
        }
        code.emit(")");

        code.emitLine("{");
        code.indent();

        visit(ctx.body);
        code.dedent();
        code.emitLine("}");



        return null;
    }

    @Override
    public Object visitRecordDecl(JavanaParser.RecordDeclContext ctx) {
        code.emitStart("static class ");
        visit(ctx.name);
        code.emitStart("{");
        code.indent();

        for(JavanaParser.TypeAssocContext typeCtx : ctx.typeAssoc()){

            for(JavanaParser.IdentifierContext id : typeCtx.namelst.names) {
                code.emitStart("public ");
                visit(typeCtx.t);
                code.emit(" ");
                visit(id);
                code.emit(";");
                code.emitLine();
            }

        }

        code.dedent();
        code.emitLine("}");


        return null;
    }

    @Override
    public Object visitReadLineCallExpression(JavanaParser.ReadLineCallExpressionContext ctx) {
        code.emit("scanner.nextLine()");
        return null;
    }

    @Override
    public Object visitStringToIntCall(JavanaParser.StringToIntCallContext ctx) {
        code.emit("Integer.parseInt");
        visit(ctx.expression());
        code.emit("");

        return null;
    }

    @Override
    public Object visitStringArrayCompositeType(JavanaParser.StringArrayCompositeTypeContext ctx) {
        code.emit("String[]");
        return null;
    }

    @Override
    public Object visitIntegerArrayCompositeType(JavanaParser.IntegerArrayCompositeTypeContext ctx) {
        code.emit("int[]");
        return null;
    }

    @Override
    public Object visitBooleanArrayCompositeType(JavanaParser.BooleanArrayCompositeTypeContext ctx) {
        code.emit("boolean[]");
        return null;
    }

    @Override
    public Object visitRecordArrayCompositeType(JavanaParser.RecordArrayCompositeTypeContext ctx) {
       // code.emit(ctx.recordArrType()."[]");
        return null;
    }

    @Override
    public Object visitNewRecordExpression(JavanaParser.NewRecordExpressionContext ctx) {
        visit(ctx.newRecord());

        return null;
    }

    @Override
    public Object visitNewRecord(JavanaParser.NewRecordContext ctx) {
        code.emit(" new ");
        visit(ctx.identifier());
        code.emit("();");

        code.emitLine();

        boolean isFirst = true;

        for(JavanaParser.FieldInitContext field : ctx.fieldInitList().fieldInit()){
            if (isFirst){
                isFirst = false;
            }
            else{
                code.emit(";");
            }
            code.emitStart(currentRecordName + ".");
            visit(field);

        }

        return null;
    }

    @Override
    public Object visitEqualityExpression(JavanaParser.EqualityExpressionContext ctx) {
        for(ParseTree tree : ctx.children){
            visit(tree);
        }

        return null;
    }

    @Override
    public Object visitFuncArgList(JavanaParser.FuncArgListContext ctx) {
        code.emit("(");
        boolean first = true;
        for(JavanaParser.FuncArgumentContext arg : ctx.args){

            if(first){
                first = false;
            }
            else {
                code.emit(", ");
            }

            visit(arg);

        }
        code.emit(")");
        return null;
    }

    @Override
    public Object visitFuncArgument(JavanaParser.FuncArgumentContext ctx) {
        visit(ctx.typeAssoc());
        return null;
    }

    @Override
    public Object visitTypeAssoc(JavanaParser.TypeAssocContext ctx) {
        visit(ctx.t);
        code.emit(" ");
        visit(ctx.namelst.identifier);

        return null;
    }

    @Override
    public Object visitBooleanType(JavanaParser.BooleanTypeContext ctx) {
        code.emit(typeNameTable.get(ctx.getText()));
        return null;
    }

    @Override
    public Object visitStringType(JavanaParser.StringTypeContext ctx) {
        code.emit(typeNameTable.get(ctx.getText()));
        return null;    }

    @Override
    public Object visitIntegerType(JavanaParser.IntegerTypeContext ctx) {
        code.emit(typeNameTable.get(ctx.getText()));
        return null;    }

    @Override
    public Object visitFuncPrototype(JavanaParser.FuncPrototypeContext ctx) {
        code.emitStart("public static ");
        visit(ctx.return_);
        code.emit(" ");
        visit(ctx.name);
        visit(ctx.funcArgList());

        return null;
    }

    @Override
    public Object visitReturnType(JavanaParser.ReturnTypeContext ctx) {
        String type = typeNameTable.get(ctx.getText());
        if(type == null){
            code.emit(ctx.getText());
        }
        else{
            code.emit(type);
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(JavanaParser.ReturnStatementContext ctx) {
        code.emit("return ");
        visit(ctx.expression());
        code.emit(";");

        return null;
    }

    @Override
    public Object visitFuncDefinition(JavanaParser.FuncDefinitionContext ctx) {
        visit(ctx.proto);
        code.emitLine("{");
        code.indent();
        visit(ctx.body);
        code.dedent();
        code.emitLine("}");

        return null;
    }

    @Override
    public Object visitFunctionCallExpression(JavanaParser.FunctionCallExpressionContext ctx) {
        visit(ctx.functionCall());

        return null;
    }

    @Override
    public Object visitFunctionCall(JavanaParser.FunctionCallContext ctx) {
        visit(ctx.name);
        code.emit("(");
        code.emit(ctx.args.getText());
        code.emit(")");
        return null;
    }

    @Override
    public Object visitIfStatement(JavanaParser.IfStatementContext ctx) {
        code.emitStart();
        code.emit("if(");
        for(ParseTree tree : ctx.condition.children){
            visit(tree);
            code.emit(" ");
        }
        code.emit(")");

        code.emitLine("{");
        code.indent();

        visit(ctx.thenStmt);
        code.dedent();
        code.emitLine("}");

        if(ctx.elseStmt != null){
            code.emitLine("else{");
            code.indent();

            visit(ctx.elseStmt);
            code.dedent();
            code.emitLine("}");
        }

        return null;
    }

    @Override
    public Object visitArithmeticExpression(JavanaParser.ArithmeticExpressionContext ctx) {
        for(ParseTree tree : ctx.children){
            visit(tree);
            code.emit(" ");
        }

        return null;
    }

    @Override
    public Object visitHigherArithmeticExpression(JavanaParser.HigherArithmeticExpressionContext ctx) {
        for(ParseTree tree : ctx.children){
            visit(tree);
            code.emit(" ");
        }

        return null;
    }

    @Override
    public Object visitIdentifierExpression(JavanaParser.IdentifierExpressionContext ctx) {
        code.emit(ctx.getText());
        return null;
    }

    @Override
    public Object visitLiteralExpression(JavanaParser.LiteralExpressionContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public Object visitIntegerLiteral(JavanaParser.IntegerLiteralContext ctx) {
        code.emit(ctx.getText());
        return null;
    }

    @Override
    public Object visitBooleanLiteral(JavanaParser.BooleanLiteralContext ctx) {
        code.emit(ctx.getText());
        return null;
    }

    @Override
    public Object visitStringLiteral(JavanaParser.StringLiteralContext ctx) {
        code.emit(ctx.getText());
        return null;
    }

    @Override
    public Object visitTerminal(TerminalNode node) {
        code.emit(node.getText());
        return null;
    }

    @Override
    public Object visitPrintStatement(JavanaParser.PrintStatementContext ctx) {
        code.emitStart(String.format("System.out.print%s;", ctx.printArgument().getText()));
        code.emitLine();
        return null;
    }

    @Override
    public Object visitPrintLineStatement(JavanaParser.PrintLineStatementContext ctx) {
        code.emitStart(String.format("System.out.println%s;", ctx.printArgument().getText()));
        code.emitLine();
        return null;    }

    /**
     * Emit a record type definition for an unnamed record.
     *
     * @param symTable the symbol table that can contain unnamed records.
     */
    private void emitUnnamedRecordDefinitions(SymTable symTable) {
        // Loop to look for names of unnamed record types.
        for (SymTableEntry id : symTable.sortedEntries()) {
            if ((id.getKind() == TYPE)
                    && (id.getType().getForm() == RECORD)
                    && (id.getName().startsWith(SymTable.UNNAMED_PREFIX))) {
                code.emitStart();
                if (programVariables) code.emit("public static ");
                code.emitEnd("class " + id.getName());
                code.emitLine("{");
                code.indent();
                emitRecordFields(id.getType().getRecordSymTable());
                code.dedent();
                code.emitLine("}");
                code.emitLine();
            }
        }
    }

    /**
     * Emit the record fields of a record.
     *
     * @param symTable the symbol table of the unnamed record.
     */
    private void emitRecordFields(SymTable symTable) {
        emitUnnamedRecordDefinitions(symTable);

        // Loop over the entries of the symbol table.
        for (SymTableEntry fieldId : symTable.sortedEntries()) {
            if (fieldId.getKind() == RECORD_FIELD) {
                code.emitStart(typeName(fieldId.getType()));
                code.emit(" " + fieldId.getName());
                code.emitEnd(";");
            }
        }
    }

    /**
     * Convert a Pascal type name to the equivalent Java type name.
     *
     * @param pascalType the datatype name.
     * @return the Java type name.
     */
    private String typeName(Typespec pascalType) {
        Typespec.Form form = pascalType.getForm();
        SymTableEntry typeId = pascalType.getIdentifier();
        String pascalTypeName = typeId != null ? typeId.getName() : null;

        if (form == ARRAY) {
            Typespec elemType = pascalType.getArrayBaseType();
            pascalTypeName = elemType.getIdentifier().getName();
            String javaTypeName = typeNameTable.get(pascalTypeName);
            return javaTypeName != null ? javaTypeName : pascalTypeName;
        } else if (form == SUBRANGE) {
            Typespec baseType = pascalType.baseType();
            pascalTypeName = baseType.getIdentifier().getName();
            return typeNameTable.get(pascalTypeName);
        } else if (form == ENUMERATION) {
            return pascalTypeName != null ? pascalTypeName : "int";
        } else if (form == RECORD) return pascalTypeName;
        else return typeNameTable.get(pascalTypeName);
    }

    /**
     * Emit code to allocate data for structured (array or record) variables.
     *
     * @param lhsPrefix the prefix for the target variable name.
     * @param symTable  the symbol table containing the variable names.
     */
    private void emitAllocateStructuredVariables(String lhsPrefix, SymTable symTable) {
        // Loop over all the symbol table's identifiers to emit
        // code to allocate array and record variables.
        for (SymTableEntry id : symTable.sortedEntries()) {
            if (id.getKind() == VARIABLE) {
                emitAllocateStructuredData(lhsPrefix, id);
            }
        }
    }

    private void emitAllocateStructuredData(String lhsPrefix,
                                            SymTableEntry variableId) {
        Typespec variableType = variableId.getType();
        Typespec.Form form = variableType.getForm();
        String variableName = variableId.getName();

        if (form == ARRAY) {
            code.emitStart(lhsPrefix + variableName + " = ");
            Typespec elemType = emitNewArray(variableType);
            code.emitEnd(";");

            if (elemType.isStructured()) {
                emitNewArrayElement(lhsPrefix, variableName, variableType);
            }
        } else if (form == RECORD) {
            code.emitStart(lhsPrefix + variableName + " = ");
            emitNewRecord(lhsPrefix, variableName, variableType);
        }
    }

    /**
     * Emit a string of bracketed dimension sizes for the array datatype.
     * followed by the "new" array allocation.
     *
     * @param type the array datatype.
     * @return the base datatype of the array.
     */
    private Typespec emitNewArray(Typespec type) {
        StringBuilder sizes = new StringBuilder();

        while (type.getForm() == ARRAY) {
            sizes.append("[").append(type.getArrayElementCount()).append("]");
            type = type.getArrayElementType();
        }

        type = type.baseType();
        String pascalTypeName = type.getIdentifier().getName();
        String javaTypeName = typeNameTable.get(pascalTypeName);

        if (javaTypeName == null) javaTypeName = pascalTypeName;
        code.emit("new " + javaTypeName + sizes);

        return type;
    }

    /**
     * Emit code to allocate an array element.
     *
     * @param lhsPrefix    the prefix for the target variable name.
     * @param variableName the name of the target variable.
     * @param elemType     the element's datatype.
     */
    private void emitNewArrayElement(String lhsPrefix, String variableName,
                                     Typespec elemType) {
        int dimensionCount = 0;

        StringBuilder variableNameBuilder = new StringBuilder(variableName);
        do {
            int elemCount = elemType.getArrayElementCount();
            ++dimensionCount;
            String subscript = "_i" + dimensionCount;
            variableNameBuilder.append("[").append(subscript).append("]");

            code.emitLine("for (int " + subscript + " = 0; " +
                    subscript + " < " + elemCount +
                    "; " + subscript + "++)");
            code.emitStart("{");
            code.indent();

            elemType = elemType.getArrayElementType();
        } while (elemType.getForm() == ARRAY);
        variableName = variableNameBuilder.toString();

        String typeName = elemType.getIdentifier().getName();
        code.emitStart(lhsPrefix + variableName + " = new " + typeName + "()");
        code.emitEnd(";");

        emitNewRecordFields(lhsPrefix + variableName + ".", elemType);

        while (--dimensionCount >= 0) {
            code.dedent();
            code.emitLine("}");
        }
    }

    /**
     * Emit code to allocate a new record.
     *
     * @param lhsPrefix    the prefix for the target variable name.
     * @param variableName the name of the target variable.
     * @param recordType   the record's datatype.
     */
    private void emitNewRecord(String lhsPrefix, String variableName,
                               Typespec recordType) {
        String typePath = recordType.getRecordTypePath();
        int index = typePath.indexOf('$');

        // Don't include the program name in the record type path.
        // Replace each $ with a period.
        typePath = typePath.substring(index + 1).replace('$', '.');
        code.emit("new " + typePath + "();");

        emitNewRecordFields(lhsPrefix + variableName + ".", recordType);
    }

    /**
     * Emit code to allocate a record's fields.
     *
     * @param lhsPrefix  the prefix for the target variable name.
     * @param recordType the record's datatype.
     */
    private void emitNewRecordFields(String lhsPrefix, Typespec recordType) {
        for (SymTableEntry fieldId : recordType.getRecordSymTable().sortedEntries()) {
            if (fieldId.getKind() == RECORD_FIELD) {
                Typespec type = fieldId.getType();

                if (type.isStructured()) {
                    emitAllocateStructuredData(lhsPrefix, fieldId);
                }
            }
        }
    }

}