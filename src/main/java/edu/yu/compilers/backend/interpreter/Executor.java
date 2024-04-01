package edu.yu.compilers.backend.interpreter;

import antlr4.JavanaBaseVisitor;
import edu.yu.compilers.intermediate.symtable.SymTableEntry;

/**
 * Execute Javana programs.
 */
public class Executor extends JavanaBaseVisitor<Object> {

    public Executor(SymTableEntry programId) {
    }

}
