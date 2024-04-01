package edu.yu.compilers.frontend;

import antlr4.JavanaBaseVisitor;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;

/***
 * Check the semantics of the Javana program and populate the symbol table.
 */
public class Semantics extends JavanaBaseVisitor<Object> {

    public Semantics() {
    }

    public int getErrorCount() {
        return 0;
    }

    public SymTableEntry getProgramId() {
        return null;
    }

    public void printSymbolTableStack() {

    }

}
