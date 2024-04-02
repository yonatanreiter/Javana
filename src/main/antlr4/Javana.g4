grammar Javana;

@header {
package antlr4;
}

// Program and routines --------------------

program 
    : programHeader globalDefinitions* mainMethod globalDefinitions* 
    ;

programHeader 
    : 'Javana' identifer ':'
    ;

mainMethod 
    : '@main' '(' mainArg? ')' blockStatement 
    ;

mainArg 
    : identifer ':' stringArrType 
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
    : 'func' identifer '(' funcArgList? ')' '->' returnType #FunctionPrototype
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
    : 'record' identifer '{' (typeAssoc)* '}'
    ;

variableDecl 
    : 'decl' typeAssoc 
    ;

typeAssoc
    : nameList ':' type #TypeAssociation
    ;

variableDef
    : 'var' nameList '=' expression #VariableDefinition
    ;

constantDef
    : 'const' nameList '=' expression #ConstantDefinition
    ;

nameList 
    : identifer (',' identifer)* 
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
    : identifer identModifier? '=' expression 
    ;

identModifier
    : arrIdxSpecifier
    | '.' identifer
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
    : expression arrIdxSpecifier #ArrayIndexExpression
    | expression '.' 'length' #StringLengthExpression
    | expression '.' identifer #RecordFieldExpression
    | expression HIGHER_ARITH_OP expression #HigherArithmeticExpression
    | expression ARITH_OP expression #ArithmeticExpression
    | expression REL_OP expression #RelationalExpression
    | expression EQ_OP expression #EqualityExpression
    | expression COND_OP expression #ConditionalExpression
    | '!' expression #NotExpression
    | '-' expression #NegateExpression
    | '(' expression ')' #ParenthesizedExpression
    | readCharCall #ReadCharCallExpression
    | readLineCall #ReadLineCallExpression
    | functionCall #FunctionCallExpression
    | identifer #IdentifierExpression
    | literal #LiteralExpression
    | newArray #NewArrayExpression
    | newRecord #NewRecordExpression
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
    : identifer '(' exprList? ')' 
    ;

newArray 
    : '@' (scalarType | identifer) arrIdxSpecifier 
    ;

newRecord
    : '@' identifer '{' varInitList? '}'
    ;

varInitList
    : identifer '=' expression (',' identifer '=' expression)*
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
    : recordType #RecordCompositeType
    | integerArrType #IntegerArrayCompositeType
    | booleanArrType #BooleanArrayCompositeType
    | stringArrType #StringArrayCompositeType
    | recordArrType #RecordArrayCompositeType
    ;

integerType : INT_TYPE ;
booleanType : BOOL_TYPE ;
stringType  : STR_TYPE ;
recordType  : identifer ;

integerArrType : INT_ARR_TYPE ;
booleanArrType : BOOL_ARR_TYPE ;
stringArrType  : STR_ARR_TYPE ;
recordArrType  : REC_ARR_TYPE ;

// Misc Rules

identifer
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
