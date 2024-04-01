package edu.yu.compilers.backend.compiler;

import antlr4.JavanaParser;

public class ExpressionGenerator extends CodeGenerator
{
    /**
     * Constructor.
     * @param parent the parent executor.
     * @param compiler the compiler to use.
     */
    public ExpressionGenerator(CodeGenerator parent, Compiler compiler)
    {
        super(parent, compiler);
    }
    
    /**
     * Emit code for an expression.
     * @param ctx the ExpressionContext.
     */
    public void emitExpression(JavanaParser.ExpressionContext ctx)
    {
    }
}
