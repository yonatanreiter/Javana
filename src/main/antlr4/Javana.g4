grammar Javana;

@header {
package antlr4;
}

// Program and routines --------------------

program 
    : hdr=programHeader defs+=globalDefinitions* main=mainMethod defs+=globalDefinitions*
    ;

programHeader
    : 'Javana' name=identifier ':'
    ;

mainMethod
    : '@main' '(' args=mainArg? ')' body=blockStatement
    ;

mainArg
    : name=identifier ':' stringArrType
    ;

globalDefinitions 
    : nameDeclStatement 
    | nameDeclDefStatement
    ;

// Function Definitions and Declarations ---

funcDefinition 
    : proto=funcPrototype body=blockStatement
    ;

funcPrototype
    : 'func' name=identifier '(' argList+=funcArgList? ')' '->' return=returnType
    ;

funcArgList
    : args+=funcArgument (',' args+=funcArgument)*
    ;

funcArgument
    : typeAssoc
    ;

returnType
    : type
    | None
    ;

// Name Definitions and Declarations -------

recordDecl
    : 'record' name=identifier '{' fields+=typeAssoc* '}'
    ;

variableDecl
    : 'decl' assoc=typeAssoc
    ;

typeAssoc
    : namelst=nameList ':' t=type
    ;

variableDef
    : 'var' namelst=nameList '=' expr=expression #VariableDefinition
    ;

constantDef
    : 'const' namelst=nameList '=' expr=expression #ConstantDefinition
    ;

nameList
    : names+=identifier (',' names+=identifier)*
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
    | continueStatement
    | breakStatement
    | printStatement
    | printLineStatement
    | printfCall
    ;

blockStatement 
    : '{' stmts+=statement* '}'
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
    : var=variable '=' expr=expression
    ;

variable
    : name=identifier modifiers+=varModifier*
    ;

varModifier
    : arrIdxSpecifier   # varArrayIndexModfier
    | '.' identifier    # varRecordFieldModifier
    ;

arrIdxSpecifier
    : '[' expr=expression ']'
    ;

ifStatement
    : 'if' '(' condition=expression ')' thenStmt=blockStatement ('else' elseStmt=blockStatement)?
    ;

forStatement
    : 'for' '(' init=variableDef? ';' condition=expression ';' updateExpr=expression ')' body=blockStatement
    ;

whileStatement
    : 'while' '(' condition=expression ')' body=blockStatement
    ;

expressionStatement
    : expr=expression
    ;

returnStatement
    : 'return' expr=expression?
    ;

continueStatement
    : 'continue'
    ;


breakStatement
    : 'break'
    ;

printStatement
    : 'print' arg=printArgument
    ;

printLineStatement
    : 'println' arg=printArgument?
    ;

printArgument
    : expression        # PrintSingleValue
    | '(' exprList ')'  # FormattedPrint
    ;

printfCall
    : 'printf' '(' formatString=expression argsList ')'
    ;

argsList
    : (',' expression)*  #printFArgsList
    ;


// Expressions -----------------------------

expression
    :  stringCharToValCall #CharToValExpression
    | concatenateStringsCall #ConcatenateStringsExpression
    | substringCall #SubstringExpression
    | arrayLength #ArrayLengthExpression
    | stringEquals #StringEqualsExpression
    | expression '.' 'charAt' '(' expression ')' #CharAtExpression
    | expression arrIdxSpecifier #ArrayIndexExpression
    | expression '.' 'length' #StringLengthExpression
    | expression '.' identifier #RecordFieldExpression
    | expression HIGHER_ARITH_OP expression #HigherArithmeticExpression
    | expression ARITH_OP expression #ArithmeticExpression
    | expression REL_OP expression #RelationalExpression
    | expression EQ_OP expression #EqualityExpression
    | expression COND_OP expression #ConditionalExpression
    | '!' expression #NotExpression
    | '(' expression ')' #ParenthesizedExpression
    | readCharCall #ReadCharCallExpression
    | readLineCall #ReadLineCallExpression
    | functionCall #FunctionCallExpression
    | stringToIntCall #StringToIntCallExpression
    | identifier #IdentifierExpression
    | literal #LiteralExpression
    | newArray #NewArrayExpression
    | newRecord #NewRecordExpression
    ;

exprList
    : exprs+=expression (',' exprs+=expression)*
    ;

readCharCall
    : 'readch' '(' ')'
    ;

readLineCall
    : 'readln' '(' ')'
    ;

stringToIntCall
    : 'stringToInt' '(' expression ')'
    ;

stringCharToValCall
    : 'stringCharToVal' '(' expression ')'
    ;


concatenateStringsCall
    : 'concat' '(' first=expression ',' second=expression ')'
    ;

substringCall
    : 'substring' '(' first=expression ',' second=expression ',' third=expression ')'
    ;

arrayLength
    : 'len' '(' paramArray=expression ')'
    ;

stringEquals
    : 'stringEquals' '(' first=expression ',' second=expression ')'
    ;

functionCall
    : name=identifier '(' args=exprList? ')'
    ;

newArray
    : '@' (scalarType | identifier) arrIdxSpecifier
    ;

newRecord
    : '@' identifier '{' init=fieldInitList? '}'
    ;

fieldInitList
    : init+=fieldInit (',' init+=fieldInit)*
    ;

fieldInit
    : field=identifier '=' expr=expression
    ;

literal 
    : INTEGER   # IntegerLiteral
    | BOOL      # BooleanLiteral
    | STRING    # StringLiteral
    | None      # NoneValue
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
recordType  : identifier ;

integerArrType : INT_ARR_TYPE ;
booleanArrType : BOOL_ARR_TYPE ;
stringArrType  : STR_ARR_TYPE ;
recordArrType  : REC_ARR_TYPE ;

// Misc Rules

identifier
    : IDENT
    ;

None
    : NULL_VALUE
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

BOOL : 'true' | 'false' ;
IDENT  : [a-zA-Z_] [a-zA-Z_0-9]* ;
STRING : '"' ( '\\' ( 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\\' ) | ~('\\' | '"') )* '"' ;
INTEGER : '-'? [0-9]+ ;
NULL_VALUE : 'None';

NEWLINE : '\r'? '\n' -> skip ;
WS : [ \t]+ -> skip ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
