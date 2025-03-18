import AST.*;

import java.util.*;


public class Parser {

    private final TokenManager tokenManager;
    private final TranNode tranNode;

    public Parser(TranNode top, List<Token> tokens) {
        tokenManager = new TokenManager(tokens);
        tranNode = top;
    }

    // Tran = { Class | Interface }
    public void Tran() throws SyntaxErrorException {
        while (!tokenManager.done()) {
            Optional<Token> o;
            o = tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE);
            if (o.isPresent())
                tranNode.Interfaces.add(parseInterface());
            o = tokenManager.matchAndRemove(Token.TokenTypes.CLASS);
            if (o.isPresent())
                tranNode.Classes.add(parseClass());
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
        // if (tranNode.Classes.isEmpty())
        //     throw new SyntaxErrorException("At least one class is required", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    // Interface = "interface" Identifier NEWLINE INDENT {MethodHeader NEWLINE } DEDENT
    private InterfaceNode parseInterface() throws SyntaxErrorException {
        InterfaceNode node = new InterfaceNode();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Interface name expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.name = o.get().getValue();
        o = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        if (o.isEmpty())
            throw new SyntaxErrorException("New line expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        o = tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        if (o.isEmpty())
            throw new SyntaxErrorException("Indent expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        do {
            node.methods.add(parseMethodHeader());
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        } while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty());
        return node;
    }

    // Class = "class" Identifier [ "implements" Identifier { "," Identifier } ] NEWLINE INDENT {Constructor NEWLINE | MethodDeclaration NEWLINE | Member NEWLINE } DEDENT
    private ClassNode parseClass() throws SyntaxErrorException {
        ClassNode node = new ClassNode();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Class name expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.name = o.get().getValue();
        o = tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS);
        if (o.isPresent()) {
            do {
                o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                if (o.isPresent())
                    node.interfaces.add(o.get().getValue());
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
        }
        requireNewline();
        requireIndent();
//        do {
//            o = tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT);
//            if (o.isPresent())
//                node.constructors.add(parseConstructor());
//            else if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
//                parseMember();
//            else
//                parseMethodDeclaration();
//            requireNewline();
//        } while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty());
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isPresent())
                node.constructors.add(parseConstructor());
            else if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
                node.members.add(parseMember());
            else
                node.methods.add(parseMethodDeclaration());
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
        return node;
    }

    // MethodHeader = Identifier "(" VariableDeclarations ")" [ ":" VariableDeclaration { "," VariableDeclaration }]
    private MethodHeaderNode parseMethodHeader() throws SyntaxErrorException {
        MethodHeaderNode node = new MethodHeaderNode();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Method name expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.name = o.get().getValue();
        o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("LPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        o = tokenManager.peek();
        if (o.isPresent() && o.get().getType() != Token.TokenTypes.RPAREN)
            node.parameters = parseVariableDeclarations();
        o = tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("RPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        o = tokenManager.matchAndRemove(Token.TokenTypes.COLON);
        if (o.isPresent())
            node.returns = parseVariableDeclarations();
        return node;
    }

    // Variable declarations used in method parameters and return types
    // VariableDeclarations = [ VariableDeclaration ] | VariableDeclaration { "," VariableDeclaration }
    private ArrayList<VariableDeclarationNode> parseVariableDeclarations() throws SyntaxErrorException {
        ArrayList<VariableDeclarationNode> nodes = new ArrayList<>();
        Optional<Token> o;
        o = tokenManager.peek();
        if (o.isPresent() && o.get().getType() == Token.TokenTypes.RPAREN)
            return nodes;

        while (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
            nodes.add(parseVariableDeclaration());
            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }
//        while (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)) {
//            VariableDeclarationNode node = new VariableDeclarationNode();
//            o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            if (o.isEmpty())
//                throw new SyntaxErrorException("Expected variable type in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            node.type = o.get().getValue();
//            o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            if (o.isEmpty())
//                throw new SyntaxErrorException("Expected variable name in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            node.name = o.get().getValue();
//            nodes.add(node);
//            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
//        }
        return nodes;
    }

    // VariableDeclaration = Identifier Identifier
    private VariableDeclarationNode parseVariableDeclaration() throws SyntaxErrorException {
        VariableDeclarationNode node = new VariableDeclarationNode();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Expected variable type in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.type = o.get().getValue();
        o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Expected variable name in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.name = o.get().getValue();
        return node;
    }

    // Variable declaration used in normal code
    // {@code VariableDeclaration = Identifier Identifier}
//    private ArrayList<VariableDeclarationNode> parseVariableDeclaration() throws SyntaxErrorException {
//        ArrayList<VariableDeclarationNode> nodes = new ArrayList<>();
//        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//        if (o.isEmpty())
//            throw new SyntaxErrorException("Expected variable type in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//        String type = o.get().getValue();
//        do {
//            VariableDeclarationNode node = new VariableDeclarationNode();
//            node.type = type;
//            o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//            if (o.isEmpty())
//                throw new SyntaxErrorException("Expected variable name in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            node.name = o.get().getValue();
//            nodes.add(node);
//        } while(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
//        return nodes;
//    }

    // Constructor = "construct" "(" VariableDeclarations ")" NEWLINE MethodBody
    private ConstructorNode parseConstructor() throws SyntaxErrorException {
        ConstructorNode node = new ConstructorNode();
        Optional<Token> o;
        o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("LPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.parameters = parseVariableDeclarations();
        o = tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("RPAREN expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        requireNewline();
        parseMethodBody(node.locals, node.statements);
        return node;
    }

    // MethodDeclaration = ["private"] ["shared"] MethodHeader NEWLINE MethodBody
    private MethodDeclarationNode parseMethodDeclaration() throws SyntaxErrorException{
        MethodDeclarationNode node = new MethodDeclarationNode();
        Optional<Token> o;

        o = tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE);
        node.isPrivate = o.isPresent();
        o = tokenManager.matchAndRemove(Token.TokenTypes.SHARED);
        node.isShared = o.isPresent();

        MethodHeaderNode methodHeader = parseMethodHeader();
        node.name = methodHeader.name;
        node.parameters = methodHeader.parameters;
        node.returns = methodHeader.returns;

        requireNewline();

        parseMethodBody(node.locals, node.statements);

        return node;
    }

    // MethodBody = INDENT { VariableDeclaration NEWLINE } {Statement} DEDENT
    private void parseMethodBody(List<VariableDeclarationNode> locals, List<StatementNode> statements) throws SyntaxErrorException {
        requireIndent();
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.WORD))
                locals.add(parseVariableDeclaration());
            else
                statements.add(parseStatement());
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
    }

    // Member = VariableDeclaration ["accessor:" Statements] ["mutator:" Statements]
    private MemberNode parseMember() throws SyntaxErrorException {
        MemberNode node = new MemberNode();
        node.declaration = parseVariableDeclaration();
        Optional<Token> o;
//        o = tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR);
//        if (o.isPresent())
//            node.accessor = Optional.of(parseAccessor());
//        else
//            node.accessor = Optional.empty();
//        o = tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR);
//        if (o.isPresent())
//            node.mutator = Optional.of(parseMutator());
//        else
//            node.mutator = Optional.empty();
        if (tokenManager.matchPattern(Token.TokenTypes.NEWLINE, Token.TokenTypes.INDENT)) {
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
            while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
                if (tokenManager.matchAndRemove(Token.TokenTypes.ACCESSOR).isPresent())
                    node.accessor = Optional.of(parseAccessorMutator());
                else if (tokenManager.matchAndRemove(Token.TokenTypes.MUTATOR).isPresent())
                    node.mutator = Optional.of(parseAccessorMutator());
                else
                    throw new SyntaxErrorException("Expected an accessor or mutator in member declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
            }
        }
        return node;
    }

    private ArrayList<StatementNode> parseAccessorMutator() throws SyntaxErrorException {
        Optional<Token> o;
        o = tokenManager.matchAndRemove(Token.TokenTypes.COLON);
        if (o.isEmpty())
            throw new SyntaxErrorException("Colon expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        o = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        if (o.isPresent())
            return parseStatements();
        ArrayList<StatementNode> statement = new ArrayList<>();
        statement.add(parseStatement());
        return statement;
    }

    // Statements = INDENT {Statement NEWLINE } DEDENT
    private ArrayList<StatementNode> parseStatements() throws SyntaxErrorException {
        ArrayList<StatementNode> nodes = new ArrayList<>();
        requireIndent();
        while (tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()) {
            nodes.add(parseStatement());
            tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        }
        return nodes;
    }

    // Statement = If | Loop | MethodCall | Assignment
    private StatementNode parseStatement() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isPresent())
            return parseIf();
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isPresent())
            return parseLoop(false);
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN, Token.TokenTypes.LOOP))
            return parseLoop(true);
        if (matchMethodCall())
            return parseMethodCall();
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)) {
            return parseAssignment();
        }
        // MethodCallStatementNode
//        Optional<StatementNode> o = disambiguate();
//        if (o.isPresent()) {
//            return o.get();
//        }
//        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN))
//            return parseAssignment();
//        parseMethodCall();
        throw new SyntaxErrorException("Invalid statement", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    private boolean matchMethodCall() {
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
            return true;
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.DOT, Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
            return true;
        if (tokenManager.peekMatch(1, Token.TokenTypes.COMMA).isPresent())
            return true;
        int i = 0;
        while (tokenManager.peekMatch(i, Token.TokenTypes.NEWLINE).isEmpty()) {
            if (tokenManager.peekMatch(i, Token.TokenTypes.ASSIGN).isPresent()) {
                if (tokenManager.matchPattern(i + 1, Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
                    tokenManager.matchPattern(i + 1, Token.TokenTypes.WORD, Token.TokenTypes.DOT, Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
                    return true;
            }
            i++;
        }
        return false;

//        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
//            return true;
//        Optional<Token> comma = tokenManager.peekMatch(1, Token.TokenTypes.COMMA);
//        if (comma.isPresent())
//            return true;
//        Optional<Token> word = tokenManager.peekMatch(2, Token.TokenTypes.WORD);
//        Optional<Token> lparen = tokenManager.peekMatch(3, Token.TokenTypes.LPAREN);
//        if (word.isPresent() && lparen.isPresent())
//            return true;
//        word = tokenManager.peekMatch(2, Token.TokenTypes.WORD);
//        Optional<Token> dot = tokenManager.peekMatch(3, Token.TokenTypes.DOT);
//        Optional<Token> word1 = tokenManager.peekMatch(4, Token.TokenTypes.WORD);
//        lparen = tokenManager.peekMatch(5, Token.TokenTypes.LPAREN);
//        if (word.isPresent() && dot.isPresent() && word1.isPresent() && lparen.isPresent())
//            return true;
//        return false;
    }

    // MethodCall = [VariableReference { "," VariableReference } "=" MethodCallExpression
    // MethodCall = [VariableReference { "," VariableReference } "=" ] MethodCallExpression
    private MethodCallStatementNode parseMethodCall() throws SyntaxErrorException {
        MethodCallStatementNode node = new MethodCallStatementNode();
        boolean assignment = false;
        int i = 0;
        while (tokenManager.peekMatch(i, Token.TokenTypes.NEWLINE).isEmpty()) {
            if (tokenManager.peekMatch(i, Token.TokenTypes.ASSIGN).isPresent()) {
                assignment = true;
                break;
            }
            i++;
        }
        if (assignment) {
            do {
                node.returnValues.add(parseVariableReference());
            } while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent());
            Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
            if (o.isEmpty())
                throw new SyntaxErrorException("Assignment expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        MethodCallExpressionNode methodCallExp = parseMethodCallExp();
        node.objectName = methodCallExp.objectName;
        node.methodName = methodCallExp.methodName;
        node.parameters = methodCallExp.parameters;
        return node;
    }

    // MethodCallExpression = [Identifier "."] Identifier "(" [Expression {"," Expression }] ")"
    // private MethodCallExpressionNode parseMethodCallExp() throws SyntaxErrorException {
    private MethodCallExpressionNode parseMethodCallExp() throws SyntaxErrorException {
        MethodCallExpressionNode node = new MethodCallExpressionNode();
        Optional<Token> o;
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.DOT)) {
            o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (o.isEmpty())
                throw new SyntaxErrorException("Expected a reference to a clas or object", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            node.objectName = Optional.of(o.get().getValue());
            tokenManager.matchAndRemove(Token.TokenTypes.DOT);
        } else
            node.objectName = Optional.empty();
        o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Expected a reference to a method", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.methodName = o.get().getValue();
        requireLPAREN();
        while (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
            node.parameters.add(parseExpression());
            tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
        }
        return node;
    }

    // Assignment = VariableReference "=" Expression
    private AssignmentNode parseAssignment() throws SyntaxErrorException {
        AssignmentNode node = new AssignmentNode();
        node.target = parseVariableReference();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
        if (o.isEmpty())
            throw new SyntaxErrorException("Assignment expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.expression = parseExpression();
        return node;
    }

    // If = "if" BoolExp NEWLINE Statements ["else" NEWLINE (Statement | Statements)]
    private IfNode parseIf() throws SyntaxErrorException {
        IfNode node = new IfNode();
        node.condition = parseBoolExpTerm();
        requireNewline();
        node.statements = parseStatements();
        Optional<Token> o;
        o = tokenManager.matchAndRemove(Token.TokenTypes.ELSE);
        if (o.isPresent()) {
            requireNewline();
            ElseNode elseNode = new ElseNode();
            elseNode.statements = parseStatements();
            node.elseStatement = Optional.of(elseNode);
        } else
            node.elseStatement = Optional.empty();
        return node;
    }

    // Loop = [VariableReference "=" ] "loop" ( BoolExpTerm ) NEWLINE Statements
    private LoopNode parseLoop(boolean assignment) throws SyntaxErrorException {
        LoopNode node = new LoopNode();
        if (assignment) {
            node.assignment = Optional.of(parseVariableReference());
            tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN);
            tokenManager.matchAndRemove(Token.TokenTypes.LOOP);
        } else
            node.assignment = Optional.empty();
        node.expression = parseBoolExpTerm();
        requireNewline();
        node.statements = parseStatements();
        return node;
    }

    private VariableReferenceNode parseVariableReference() throws SyntaxErrorException {
        VariableReferenceNode node = new VariableReferenceNode();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (o.isEmpty())
            throw new SyntaxErrorException("Variable name expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.name = o.get().getValue();
        return node;
    }

    // VariableReference = Identifier
//    private Optional<VariableReferenceNode> parseVariableReference() throws SyntaxErrorException {
//        VariableReferenceNode node = new VariableReferenceNode();
//        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
//        if (o.isEmpty())
//            throw new SyntaxErrorException("Variable name expected in variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//        node.name = o.get().getValue();
//        o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
//        if (o.isPresent() && o.get().getType() == Token.TokenTypes.LPAREN)
//            return Optional.empty();
//        return Optional.of(node);
//    }

    // BoolExpTerm = BoolExpFactor {("and"|"or") BoolExpTerm} | "not" BoolExpTerm
    // BoolExpTerm = (BoolExpFactor {("and"|"or") BoolExpTerm}) | ("not" BoolExpTerm)
    private ExpressionNode parseBoolExpTerm() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.NOT).isPresent()) {
            NotOpNode node = new NotOpNode();
            node.left = parseBoolExpTerm();
            return node;
        }
        ExpressionNode factor = parseBoolExpFactor();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.AND, Token.TokenTypes.OR);
        while (o.isPresent()) {
            BooleanOpNode node = new BooleanOpNode();
            node.left = factor;
            if (o.get().getType() == Token.TokenTypes.AND)
                node.op = BooleanOpNode.BooleanOperations.and;
            else
                node.op = BooleanOpNode.BooleanOperations.or;
            node.right = parseBoolExpTerm();
            factor = node;
            o = tokenManager.matchAndRemove(Token.TokenTypes.AND, Token.TokenTypes.OR);
        }
        return factor;
//        ExpressionNode factor = parseBoolExpFactor();
//        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.AND, Token.TokenTypes.OR);
//        if (o.isEmpty())
//            return factor;
//        BooleanOpNode op = new BooleanOpNode();
//        op.left = factor;
//        while (o.isPresent()) {
//            if (o.get().getType() == Token.TokenTypes.AND)
//                op.op = BooleanOpNode.BooleanOperations.and;
//            else if (o.get().getType() == Token.TokenTypes.OR)
//                op.op = BooleanOpNode.BooleanOperations.or;
//            else
//                throw new SyntaxErrorException("Unknown operation in boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            op.right = parseBoolExpTerm();
//            o = tokenManager.matchAndRemove(Token.TokenTypes.AND, Token.TokenTypes.OR);
//        }
//        return op;

//        while (o.isPresent()) {
//            if (o.get().getType() == Token.TokenTypes.AND)
//                node.op = BooleanOpNode.BooleanOperations.and;
//            else if (o.get().getType() == Token.TokenTypes.OR)
//                node.op = BooleanOpNode.BooleanOperations.or;
//            else
//                throw new SyntaxErrorException("Unknown operation in boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            node.right = parseBoolExpTerm();
//            o = tokenManager.matchAndRemove(Token.TokenTypes.AND, Token.TokenTypes.OR);
//        }

//        Optional<Token> o = tokenManager.peek();
//        while (o.isPresent() && (o.get().getType() == Token.TokenTypes.AND ||
//                o.get().getType() == Token.TokenTypes.OR)) {
//            if (tokenManager.matchAndRemove(Token.TokenTypes.AND).isPresent())
//                node.op = BooleanOpNode.BooleanOperations.and;
//            else if (tokenManager.matchAndRemove(Token.TokenTypes.OR).isPresent())
//                node.op = BooleanOpNode.BooleanOperations.or;
//            else
//                throw new SyntaxErrorException("Unknown operation in boolean expression.", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
//            node.right = parseBoolExpTerm();
//            o = tokenManager.peek();
//        }
//        return node;
    }

    // BoolExpFactor = MethodCallExpression | (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | VariableReference
    private ExpressionNode parseBoolExpFactor() throws SyntaxErrorException {
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.DOT, Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
            tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
            return parseMethodCallExp();
//        int i = 0;
//        boolean comparison = false;
//        while (tokenManager.peekMatch(i, Token.TokenTypes.NEWLINE).isEmpty()) {
//            Optional<Token> o = tokenManager.peekMatch(i,
//                Token.TokenTypes.EQUAL, Token.TokenTypes.NOTEQUAL,
//                Token.TokenTypes.LESSTHAN, Token.TokenTypes.GREATERTHAN,
//                Token.TokenTypes.LESSTHANEQUAL, Token.TokenTypes.GREATERTHANEQUAL);
//            if (o.isPresent()) {
//                comparison = true;
//                break;
//            }
//            i++;
//        }
//        if (comparison) {
        ExpressionNode left = parseExpression();
        Optional<Token> o = tokenManager.matchAndRemove(
                Token.TokenTypes.EQUAL, Token.TokenTypes.NOTEQUAL,
                Token.TokenTypes.LESSTHAN, Token.TokenTypes.GREATERTHAN,
                Token.TokenTypes.LESSTHANEQUAL, Token.TokenTypes.GREATERTHANEQUAL
        );
        if (o.isEmpty())
            return left;
//        if (left instanceof VariableReferenceNode)
//            return left;
        CompareNode node = new CompareNode();
        node.left = left;
        if (o.get().getType() == Token.TokenTypes.EQUAL)
            node.op = CompareNode.CompareOperations.eq;
        else if (o.get().getType() == Token.TokenTypes.NOTEQUAL)
            node.op = CompareNode.CompareOperations.ne;
        else if (o.get().getType() == Token.TokenTypes.LESSTHAN)
            node.op = CompareNode.CompareOperations.lt;
        else if (o.get().getType() == Token.TokenTypes.GREATERTHAN)
            node.op = CompareNode.CompareOperations.gt;
        else if (o.get().getType() == Token.TokenTypes.LESSTHANEQUAL)
            node.op = CompareNode.CompareOperations.le;
        else if (o.get().getType() == Token.TokenTypes.GREATERTHANEQUAL)
            node.op = CompareNode.CompareOperations.ge;
        else
            throw new SyntaxErrorException("Unknown operation in boolean expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        node.right = parseExpression();
        return node;
//        return parseVariableReference();
    }

    // Expression = Term { ("+"|"-") Term }
    private ExpressionNode parseExpression() throws SyntaxErrorException {
        ExpressionNode term = parseTerm();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.PLUS, Token.TokenTypes.MINUS);
        while (o.isPresent()) {
            MathOpNode node = new MathOpNode();
            node.left = term;
            if (o.get().getType() == Token.TokenTypes.PLUS)
                node.op = MathOpNode.MathOperations.add;
            else // MINUS
                node.op = MathOpNode.MathOperations.subtract;
            node.right = parseTerm();
            term = node;
            o = tokenManager.matchAndRemove(Token.TokenTypes.PLUS, Token.TokenTypes.MINUS);
        }
        return term;
    }

    // Term = Factor { ("*"|"/"|"%") Factor }
    private ExpressionNode parseTerm() throws SyntaxErrorException {
        ExpressionNode factor = parseFactor();
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.TIMES, Token.TokenTypes.DIVIDE, Token.TokenTypes.MODULO);
        while (o.isPresent()) {
            MathOpNode node = new MathOpNode();
            node.left = factor;
            if (o.get().getType() == Token.TokenTypes.TIMES)
                node.op = MathOpNode.MathOperations.multiply;
            else if (o.get().getType() == Token.TokenTypes.DIVIDE)
                node.op = MathOpNode.MathOperations.divide;
            else // MODULO
                node.op = MathOpNode.MathOperations.modulo;
            node.right = parseFactor();
            factor = node;
            o = tokenManager.matchAndRemove(Token.TokenTypes.TIMES, Token.TokenTypes.DIVIDE, Token.TokenTypes.MODULO);
        }
        return factor;
    }

    // Factor = NumberLiteral | VariableReference | "true" | "false" | StringLiteral | CharacterLiteral | MethodCallExpression | "(" Expression ")" | "new" Identifier "(" [Expression {"," Expression }] ")"
    private ExpressionNode parseFactor() throws SyntaxErrorException {
        Optional<Token> o;
        o = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if (o.isPresent()) {
            NumericLiteralNode node = new NumericLiteralNode();
            node.value = Float.parseFloat(o.get().getValue());
            return node;
        }
        o = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
        if (o.isPresent()) {
            StringLiteralNode node = new StringLiteralNode();
            node.value = o.get().getValue();
            return node;
        }
        o = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
        if (o.isPresent()) {
            CharLiteralNode node = new CharLiteralNode();
            node.value = o.get().getValue().charAt(0);
            return node;
        }
        o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        if (o.isPresent()) {
            ExpressionNode node = parseExpression();
            tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
            return node;
        }
        o = tokenManager.matchAndRemove(Token.TokenTypes.NEW);
        if (o.isPresent()) {
            NewNode node = new NewNode();
            o = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (o.isEmpty())
                throw new SyntaxErrorException("Expected identifier following new", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            node.className = o.get().getValue();
            o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
            if (o.isEmpty())
                throw new SyntaxErrorException("Expected LPAREN", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            while (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                node.parameters.add(parseExpression());
                tokenManager.matchAndRemove(Token.TokenTypes.COMMA);
            }
            return node;
        }
        if (tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.DOT, Token.TokenTypes.WORD, Token.TokenTypes.LPAREN) ||
            tokenManager.matchPattern(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN))
            return parseMethodCallExp();
        o = tokenManager.matchAndRemove(Token.TokenTypes.TRUE);
        if (o.isPresent())
            return new BooleanLiteralNode(true);
        o = tokenManager.matchAndRemove(Token.TokenTypes.FALSE);
        if (o.isPresent())
            return new BooleanLiteralNode(false);
        o = tokenManager.peekMatch(0, Token.TokenTypes.WORD);
        if (o.isPresent())
            return parseVariableReference();
        throw new SyntaxErrorException("Unknown factor", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

//    private Optional<StatementNode> disambiguate() throws SyntaxErrorException {
//        Optional<MethodCallExpressionNode> methodCallExp = parseMethodCallExp();
//        if (methodCallExp.isPresent()) {
//            MethodCallStatementNode node = new MethodCallStatementNode();
//            return Optional.of(node);
//        }
//        return Optional.empty();
//    }

    private void requireNewline() throws SyntaxErrorException {
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE);
        if (o.isEmpty())
            throw new SyntaxErrorException("Newline expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    private void requireIndent() throws SyntaxErrorException {
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.INDENT);
        if (o.isEmpty())
            throw new SyntaxErrorException("Indent expected", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    private void requireLPAREN() throws SyntaxErrorException {
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.LPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("Expected '('", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }

    private void requireRPAREN() throws SyntaxErrorException {
        Optional<Token> o = tokenManager.matchAndRemove(Token.TokenTypes.RPAREN);
        if (o.isEmpty())
            throw new SyntaxErrorException("Expected ')'", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }
}
