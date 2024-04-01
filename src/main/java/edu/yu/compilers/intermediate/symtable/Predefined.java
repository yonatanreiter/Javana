package edu.yu.compilers.intermediate.symtable;

import edu.yu.compilers.intermediate.symtable.SymTableEntry.Kind;
import edu.yu.compilers.intermediate.symtable.SymTableEntry.Routine;
import edu.yu.compilers.intermediate.type.Typespec;

import static edu.yu.compilers.intermediate.symtable.SymTableEntry.Kind.*;
import static edu.yu.compilers.intermediate.symtable.SymTableEntry.Routine.*;
import static edu.yu.compilers.intermediate.type.Typespec.Form.*;

import java.util.ArrayList;

public class Predefined {
    // Predefined types.
    public static Typespec integerType;
    public static Typespec realType;
    public static Typespec booleanType;
    public static Typespec charType;
    public static Typespec stringType;
    public static Typespec undefinedType;

    // Predefined identifiers.
    public static SymTableEntry integerId;
    public static SymTableEntry realId;
    public static SymTableEntry booleanId;
    public static SymTableEntry charId;
    public static SymTableEntry stringId;
    public static SymTableEntry falseId;
    public static SymTableEntry trueId;
    public static SymTableEntry printId;
    public static SymTableEntry printlnId;
    public static SymTableEntry readId;
    public static SymTableEntry readlnId;

    /**
     * Initialize a symbol table stack with predefined identifiers.
     *
     * @param symTableStack the symbol table stack to initialize.
     */
    public static void initialize(SymTableStack symTableStack) {
        initializeTypes(symTableStack);
        initializeConstants(symTableStack);
        initializeStandardRoutines(symTableStack);
    }

    /**
     * Initialize the predefined types.
     *
     * @param symTableStack the symbol table stack to initialize.
     */
    private static void initializeTypes(SymTableStack symTableStack) {
        // Type integer.
        integerId = symTableStack.enterLocal("integer", TYPE);
        integerType = new Typespec(SCALAR);
        integerType.setIdentifier(integerId);
        integerId.setType(integerType);

        // Type real.
        realId = symTableStack.enterLocal("real", TYPE);
        realType = new Typespec(SCALAR);
        realType.setIdentifier(realId);
        realId.setType(realType);

        // Type boolean.
        booleanId = symTableStack.enterLocal("boolean", TYPE);
        booleanType = new Typespec(ENUMERATION);
        booleanType.setIdentifier(booleanId);
        booleanId.setType(booleanType);

        // Type char.
        charId = symTableStack.enterLocal("char", TYPE);
        charType = new Typespec(SCALAR);
        charType.setIdentifier(charId);
        charId.setType(charType);

        // Type string.
        stringId = symTableStack.enterLocal("string", TYPE);
        stringType = new Typespec(SCALAR);
        stringType.setIdentifier(stringId);
        stringId.setType(stringType);

        // Undefined type.
        undefinedType = new Typespec(SCALAR);
    }

    /**
     * Initialize the predefined constant.
     *
     * @param symTabStack the symbol table stack to initialize.
     */
    private static void initializeConstants(SymTableStack symTabStack) {
        // Boolean enumeration constant false.
        falseId = symTabStack.enterLocal("false", ENUMERATION_CONSTANT);
        falseId.setType(booleanType);
        falseId.setValue(0);

        // Boolean enumeration constant true.
        trueId = symTabStack.enterLocal("true", ENUMERATION_CONSTANT);
        trueId.setType(booleanType);
        trueId.setValue(1);

        // Add false and true to the boolean enumeration type.
        ArrayList<SymTableEntry> constants = booleanType.getEnumerationConstants();
        constants.add(falseId);
        constants.add(trueId);
    }

    /**
     * Initialize the standard procedures and functions.
     *
     * @param symTableStack the symbol table stack to initialize.
     */
    private static void initializeStandardRoutines(SymTableStack symTableStack) {
        printId = enterStandard(symTableStack, FUNCTION, "print", PRINT);
        printlnId = enterStandard(symTableStack, FUNCTION, "println", PRINTLN);
        readId = enterStandard(symTableStack, FUNCTION, "read", READ);
        readlnId = enterStandard(symTableStack, FUNCTION, "readln", READLN);
    }

    /**
     * Enter a standard procedure or function into the symbol table stack.
     *
     * @param symTableStack the symbol table stack to initialize.
     * @param kind          FUNCTION
     * @param name          the procedure or function name.
     * @param routineCode   the routine code.
     */
    private static SymTableEntry enterStandard(SymTableStack symTableStack, Kind kind, String name,
            Routine routineCode) {
        SymTableEntry routineId = symTableStack.enterLocal(name, kind);
        routineId.setRoutineCode(routineCode);

        return routineId;
    }

}
