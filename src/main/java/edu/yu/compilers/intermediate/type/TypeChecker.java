package edu.yu.compilers.intermediate.type;

import antlr4.JavanaParser;
import edu.yu.compilers.intermediate.symtable.Predefined;

/***
 * This class is responsible for checking the types of expressions and statements.
 * Implement your type system here.
 */
public class TypeChecker {

    public static Typespec returnType(Object o){

        if (o == null) {
            return null;
        }

        // Example: Handling literals directly.
        if (o instanceof Integer) {
            return Predefined.integerType;
        } else if (o instanceof Float) {
            return Predefined.realType;
        } else if (o instanceof Boolean) {
            return Predefined.booleanType;
        } else if (o instanceof String) {
            return Predefined.stringType;
        }




        return null;

    }


}
