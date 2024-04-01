grammar Javana;

@header {
package antlr4;
}

// Program and routines --------------------

program 
    : programHeader globalDefinitions* mainMethod globalDefinitions* 
    ;

programHeader 
    : 'Javana' identifier ':'
    ;

mainMethod 
    : '@main' '(' mainArg? ')' blockStatement 
    ;

mainArg 
    : identifier ':' stringArrType 
    ;

globalDefinitions 
    : nameDeclStatement 
    | nameDeclDefStatement
    ;

// Function Definitions and Declarations ---

funcDefinition 
    : funcPrototype blockStatement 
    ;

funcPrototype  
    : 'func' identifier '(' funcArgList? ')' '->' returnType 
    ;

funcArgList    
    : funcArgument (',' funcArgument)* 
    ;

funcArgument   
    : typeAssoc 
    ;

returnType
    : type
    | 'None'
    ;

// Name Definitions and Declarations -------

recordDecl
    : 'record' identifier '{' (typeAssoc)* '}'
    ;

variableDecl 
    : 'decl' typeAssoc 
    ;

typeAssoc
    : nameList ':' type
    ;

variableDef  
    : 'var' nameList '=' expression 
    ;

constantDef  
    : 'const' nameList '=' expression 
    ;

nameList 
    : identifier (',' identifier)* 
    ;
    

// Statements ------------------------------

statement 
    : blockStatement                  
    | nameDeclStatement
    | nameDeclDefStatement             
    | assignmentStatement 
    | ifStatement
    | forStatement
    | whileStatement
    | expressionStatement
    | returnStatement
    | printStatement
    | printLineStatement
    ;

blockStatement 
    : '{' (statement)* '}' 
    ;

nameDeclStatement
    : variableDecl
    | recordDecl
    ;

nameDeclDefStatement
    : variableDef
    | constantDef
    | funcDefinition
    ;
    
assignmentStatement        
    : identifier identModifier? '=' expression 
    ;

identModifier
    : arrIdxSpecifier
    | '.' identifier
    ;

arrIdxSpecifier
    : '[' expression ']'
    ;

ifStatement 
    : 'if' '(' expression ')' blockStatement ('else' blockStatement)? 
    ;

forStatement 
    : 'for' '(' variableDef? ';' expression ';' expression ')' blockStatement 
    ;

whileStatement 
    : 'while' '(' expression ')' blockStatement 
    ;

expressionStatement 
    : expression 
    ;

returnStatement 
    : 'return' expression? 
    ;

printStatement
    : 'print' printArgument 
    ;

printLineStatement
    : 'println' printArgument?
    ;

printArgument
    : expression
    | '(' exprList ')'
    ;

// Expressions -----------------------------

expression 
    : expression arrIdxSpecifier            
    | expression '.' 'length'
    | expression '.' identifier                   
    | expression HIGHER_ARITH_OP expression   
    | expression ARITH_OP expression          
    | expression REL_OP expression            
    | expression EQ_OP expression             
    | expression COND_OP expression           
    | '!' expression                          
    | '-' expression                          
    | '(' expression ')'                         
    | readCharCall
    | readLineCall
    | functionCall
    | identifier                                
    | literal                                    
    | newArray                                   
    | newRecord                                  
    ;

exprList
    : expression (',' expression)*
    ;

readCharCall
    : 'readch' '(' ')'
    ;

readLineCall
    : 'readln' '(' ')'
    ;

functionCall 
    : identifier '(' exprList? ')' 
    ;

newArray 
    : '@' (scalarType | identifier) arrIdxSpecifier 
    ;

newRecord
    : '@' identifier '{' varInitList? '}'
    ;

varInitList
    : identifier '=' expression (',' identifier '=' expression)*
    ;

literal 
    : INTEGER
    | BOOL        
    | STRING      
    | NULL_VALUE  
    ;

// Types -----------------------------------

type 
    : scalarType    
    | compositeType 
    ;

scalarType 
    : integerType
    | booleanType
    | stringType
    ;

compositeType 
    : recordType
    | integerArrType
    | booleanArrType
    | stringArrType
    | recordArrType
    ;

integerType : INT_TYPE ;
booleanType : BOOL_TYPE ;
stringType  : STR_TYPE ;
recordType  : identifier ;

integerArrType : INT_ARR_TYPE ;
booleanArrType : BOOL_ARR_TYPE ;
stringArrType  : STR_ARR_TYPE ;
recordArrType  : REC_ARR_TYPE ;

// Misc Rules

identifier
    : IDENT
    ;

// Lexer tokens

INT_ARR_TYPE  : INT_TYPE '[' ']' ;
BOOL_ARR_TYPE : BOOL_TYPE '[' ']' ;
STR_ARR_TYPE  : STR_TYPE '[' ']' ;
REC_ARR_TYPE  : IDENT '[' ']' ;

INT_TYPE  : 'int' ;
BOOL_TYPE : 'bool' ;
STR_TYPE  : 'string' ;

HIGHER_ARITH_OP : '*' | '/' | '%' ;
ARITH_OP        : '+' | '-' ;
REL_OP          : '<' | '>' | '<=' | '>=' ;
EQ_OP           : '==' | '!=' ;
COND_OP         : '&&' | '||' ;

IDENT  : [a-zA-Z_] [a-zA-Z_0-9]* ;
STRING : '"' ( '\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\\' ) | ~('\\' | '"') )* '"' ;
INTEGER : '-'? [0-9]+ ;
BOOL : 'true' | 'false' ;
NULL_VALUE : 'None';

NEWLINE : '\r'? '\n' -> skip ;
WS : [ \t]+ -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
