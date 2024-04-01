package edu.yu.compilers.frontend;

import org.antlr.v4.runtime.ParserRuleContext;

public class SemanticErrorHandler
{
    public enum Code
    {
        UNDECLARED_IDENTIFIER      ("Undeclared identifier"),
        REDECLARED_IDENTIFIER      ("Redeclared identifier"),
        INVALID_OPERATOR           ("Invalid operator"),
        INVALID_TYPE               ("Invalid type"),
        TYPE_MISMATCH              ("Mismatched datatype"),
        TYPE_MUST_BE_INTEGER       ("Datatype must be integer"),
        TYPE_MUST_BE_STRING        ("Datatype must be integer or real"),
        TYPE_MUST_BE_BOOLEAN       ("Datatype must be boolean"),
        INCOMPATIBLE_ASSIGNMENT    ("Incompatible assignment"),
        INCOMPATIBLE_COMPARISON    ("Incompatible comparison"),
        NAME_MUST_BE_FUNCTION      ("Must be a function name"),
        ARGUMENT_COUNT_MISMATCH    ("Invalid number of arguments"),
        INVALID_RETURN_TYPE        ("Invalid function return type"),
        INDEX_OUT_OF_BOUNDS        ("Index out of bounds");
        
        private final String message;
        
        Code(String message) { this.message = message; }
    }
    
    private int count = 0;
    
    /**
     * Get the count of semantic errors.
     * @return the count.
     */
    public int getCount() { return count; }

    /**
     * Flag a semantic error.
     * @param code the error code.
     * @param lineNumber the line number of the offending line.
     * @param text the text near the error.
     */
    public void flag(Code code, int lineNumber, String text)
    {
        if (count == 0)
        {
            System.out.println("\n===== SEMANTIC ERRORS =====\n");
            System.out.printf("%-4s %-40s %s\n", "Line", "Message", "Found near");
            System.out.printf("%-4s %-40s %s\n", "----", "-------", "----------");
        }
        
        count++;
        
        System.out.printf("%03d  %-40s \"%s\"\n", 
                          lineNumber, code.message, text);
    }
    
    /**
     * Flag a semantic error.
     * @param code the error code.
     * @param ctx the context containing the error.
     */
    public void flag(Code code, ParserRuleContext ctx)
    {
        flag(code, ctx.getStart().getLine(), ctx.getText());
    }
}
