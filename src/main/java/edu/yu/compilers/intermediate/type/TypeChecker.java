package edu.yu.compilers.intermediate.type;

import antlr4.JavanaParser;
import edu.yu.compilers.intermediate.symtable.Predefined;

import static edu.yu.compilers.intermediate.type.Typespec.Form.*;

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
        } else if (o instanceof Boolean) {
            return Predefined.booleanType;
        } else if (o instanceof String) {
            return Predefined.stringType;
        }
        else if(o instanceof Typespec){
            return (Typespec) o;
        }




        return null;

    }

    public static Typespec returnType(String o) {

        if (o == null) {
            return null;
        }

        // Example: Handling literals directly.
        if (o.equals("int")) {
            return Predefined.integerType;
        } else if (o.equals("bool")) {
            return Predefined.booleanType;
        } else if (o.equals("string")) {
            return Predefined.stringType;
        }
        else if(o.equals("int[]")){
            Typespec spec = new Typespec(ARRAY);
            spec.setArrayElementType(Predefined.integerType);
            return spec;
        }
        else if(o.equals("bool[]")){
            Typespec spec = new Typespec(ARRAY);
            spec.setArrayElementType(Predefined.booleanType);
            return spec;
        }
        else if(o.equals("string[]")){
            Typespec spec = new Typespec(ARRAY);
            spec.setArrayElementType(Predefined.stringType);
            return spec;
        }


        return null;
    }

    public static boolean areComparisonCompatible(Typespec type1, Typespec type2) {
        if ((type1 == null) || (type2 == null)) return false;

        type1 = type1.baseType();
        type2 = type2.baseType();
        Typespec.Form form = type1.getForm();

        boolean compatible = false;

        // Two identical scalar or enumeration types.
        if ((type1 == type2) && ((form == SCALAR) || (form == ENUMERATION))) {
            compatible = true;
        }

        // One integer and one real.
        else if (isAtLeastOneReal(type1, type2)) compatible = true;

        return compatible;
    }


    /**
     * Check if a type specification is integer.
     *
     * @param type the type specification to check.
     * @return true if integer, else false.
     */
    public static boolean isInteger(Typespec type) {
        return (type != null) && (type.baseType() == Predefined.integerType);
    }

    /**
     * Check if both type specifications are integer.
     *
     * @param type1 the first type specification to check.
     * @param type2 the second type specification to check.
     * @return true if both are integer, else false.
     */
    public static boolean areBothInteger(Typespec type1, Typespec type2) {
        return isInteger(type1) && isInteger(type2);
    }

    /**
     * Check if a type specification is real.
     *
     * @param type the type specification to check.
     * @return true if real, else false.
     */
    public static boolean isReal(Typespec type) {
        return (type != null) && (type.baseType() == Predefined.realType);
    }

    /**
     * Check if a type specification is integer or real.
     *
     * @param type the type specification to check.
     * @return true if integer or real, else false.
     */
    public static boolean isIntegerOrReal(Typespec type) {
        return isInteger(type) || isReal(type);
    }

    /**
     * Check if at least one of two type specifications is real.
     *
     * @param type1 the first type specification to check.
     * @param type2 the second type specification to check.
     * @return true if at least one is real, else false.
     */
    public static boolean isAtLeastOneReal(Typespec type1, Typespec type2) {
        return (isReal(type1) && isReal(type2)) || (isReal(type1) && isInteger(type2)) || (isInteger(type1) && isReal(type2));
    }

    /**
     * Check if a type specification is boolean.
     *
     * @param type the type specification to check.
     * @return true if boolean, else false.
     */
    public static boolean isBoolean(Typespec type) {
        return (type != null) && (type.baseType() == Predefined.booleanType);
    }

    /**
     * Check if both type specifications are boolean.
     *
     * @param type1 the first type specification to check.
     * @param type2 the second type specification to check.
     * @return true if both are boolean, else false.
     */
    public static boolean areBothBoolean(Typespec type1, Typespec type2) {
        return isBoolean(type1) && isBoolean(type2);
    }

    /**
     * Check if a type specification is char.
     *
     * @param type the type specification to check.
     * @return true if the type is not char
     */
    public static boolean isChar(Typespec type) {
        return (type != null) && (type.baseType() == Predefined.charType);
    }

    /**
     * Check if a type specification is string.
     *
     * @param type the type specification to check.
     * @return true if integer, else false.
     */
    public static boolean isString(Typespec type) {
        return (type != null) && (type.baseType() == Predefined.stringType);
    }

    /**
     * Check if both type specifications are string.
     *
     * @param type1 the first type specification to check.
     * @param type2 the second type specification to check.
     * @return true if both are integer, else false.
     */
    public static boolean areBothString(Typespec type1, Typespec type2) {
        return isString(type1) && isString(type2);
    }

    public static boolean areAssignmentCompatible(Typespec targetType, Typespec valueType) {
        if ((targetType == null) || (valueType == null)) return false;

        targetType = targetType.baseType();
        valueType = valueType.baseType();

        boolean compatible = false;

        // Identical types.
        if (targetType == valueType) compatible = true;

            // real := integer
        else if (isReal(targetType) && isInteger(valueType)) compatible = true;

        return compatible;
    }


    public static boolean oneIsTypeSpecOneIsActualType(Object lhs, Object rhs) {
        if(lhs instanceof Typespec && !(rhs instanceof Typespec)){
            return returnType(rhs).equals(lhs);
        }
        else if(rhs instanceof Typespec && !(lhs instanceof Typespec)){
            return returnType(lhs).equals(rhs);
        }

        return false;
    }
}