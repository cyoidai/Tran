
import AST.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;


public class ParserMyTests {

    private TranNode lexAndParse(String input) throws Exception {
        Lexer lexer = new Lexer(input);
        LinkedList<Token> tokens = lexer.Lex();
        TranNode root = new TranNode();
        Parser parser = new Parser(root, tokens);
        parser.Tran();
        return root;
    }

    @Test
    public void classes() throws Exception {
        TranNode root = lexAndParse("""
class Program
    number a
    number b
    number c
    construct(number x, number y, number z)
        a = x
        y = b
        z = c
        """);

        assertEquals(1,         root.Classes.size());
        assertEquals("Program", root.Classes.getFirst().name);
        assertEquals(3,         root.Classes.getFirst().members.size());
        assertEquals("a",       root.Classes.getFirst().members.get(0).declaration.name);
        assertEquals("b",       root.Classes.getFirst().members.get(1).declaration.name);
        assertEquals("c",       root.Classes.getFirst().members.get(2).declaration.name);
        assertEquals(1,         root.Classes.getFirst().constructors.size());
        assertEquals(3,         root.Classes.getFirst().constructors.getFirst().parameters.size());
        assertEquals("x",       root.Classes.getFirst().constructors.getFirst().parameters.get(0).name);
        assertEquals("y",       root.Classes.getFirst().constructors.getFirst().parameters.get(1).name);
        assertEquals("z",       root.Classes.getFirst().constructors.getFirst().parameters.get(2).name);
        assertEquals(0,         root.Classes.getFirst().methods.size());
    }

    @Test
    public void interfaces() throws Exception {
        TranNode root = lexAndParse("""
interface List
    get(number index) : number n
    add(number n)
    length() : number l
        """);

        assertEquals(1,        root.Interfaces.size());
        assertEquals("List",   root.Interfaces.getFirst().name);
        assertEquals(3,        root.Interfaces.getFirst().methods.size());

        assertEquals("get",    root.Interfaces.getFirst().methods.getFirst().name);
        assertEquals(1,        root.Interfaces.getFirst().methods.getFirst().parameters.size());
        assertEquals("number", root.Interfaces.getFirst().methods.getFirst().parameters.getFirst().type);
        assertEquals("index",  root.Interfaces.getFirst().methods.getFirst().parameters.getFirst().name);
        assertEquals(1,        root.Interfaces.getFirst().methods.getFirst().returns.size());
        assertEquals("number", root.Interfaces.getFirst().methods.getFirst().returns.getFirst().type);
        assertEquals("n",      root.Interfaces.getFirst().methods.getFirst().returns.getFirst().name);

        assertEquals("add",    root.Interfaces.getFirst().methods.get(1).name);
        assertEquals("number", root.Interfaces.getFirst().methods.get(1).parameters.getFirst().type);
        assertEquals("n",      root.Interfaces.getFirst().methods.get(1).parameters.getFirst().name);
        assertEquals(0,        root.Interfaces.getFirst().methods.get(1).returns.size());

        assertEquals("length", root.Interfaces.getFirst().methods.get(2).name);
        assertEquals(0,        root.Interfaces.getFirst().methods.get(2).parameters.size());
        assertEquals(1,        root.Interfaces.getFirst().methods.get(2).returns.size());
        assertEquals("number", root.Interfaces.getFirst().methods.get(2).returns.getFirst().type);
        assertEquals("l",      root.Interfaces.getFirst().methods.get(2).returns.getFirst().name);
    }

    @Test
    public void inheritance() throws Exception {
        TranNode root = lexAndParse("""
interface List
    get(number index) : number n
    add(number n)
    length() : number l
class ABadList implements List
    number first
    number second
    number third
    get(number index) : number n
        n = first
    add(number n)
        third = n
    length() : number l
        l = first
        """);
    }

    @Test
    public void accessor_mutator() throws Exception {
        TranNode root = lexAndParse("""
class Program
    number x
        accessor: value = x.clone()
        mutator: x = value
    number y
        accessor:
            value = y.clone()
        mutator:
            y = value
    number z
        accessor: value = z.clone()
        """);

        MemberNode member = root.Classes.getFirst().members.getFirst();
        assertTrue(member.accessor.isPresent());
        assertTrue(member.mutator.isPresent());
    }

//    @Test
//    void test1() throws Exception {
//        TranNode root = lexAndParse("""
//interface test
//    dosomething(string a, number b) : string c, number d
//    dosomethingelse(number x) : number y
//class Tran implements test
//    number lorem
//    number ipsum
//    construct(number somenum)
//        number num
//    string dolor
//    dosomething(string a, number b) : string c, number d
//        string x
//    dosomethingelse(number x) : number y
//        number idunno
//        loop x
//            number somenumber
//"""
//        );
//
//        assertEquals("Tran", root.Classes.getFirst().name);
//        assertEquals(3, root.Classes.getFirst().members.size());
//    }

    @Test
    void loop() throws Exception {
        TranNode root = lexAndParse("""
class Program
    start()
        loop keepGoing
            x = y
        """);

        MethodDeclarationNode start = root.Classes.getFirst().methods.getFirst();
    }

    @Test
    void boolExp() throws Exception {
        TranNode root = lexAndParse("""
class Program
    max(number a, number b) : number c
        if a > b
            c = a
        else
            c = b
        """);

        IfNode ifNode = (IfNode)root.Classes.getFirst().methods.getFirst().statements.getFirst();
        CompareNode condition = (CompareNode)ifNode.condition;
        assertEquals("a", condition.left.toString());
        assertEquals(CompareNode.CompareOperations.gt, condition.op);
        assertEquals("b", condition.right.toString());
    }

    @Test
    void boolExp1() throws Exception {
        TranNode root = lexAndParse("""
class Program
    max(number a, number b, number c) : number d
        if a > b && b > c
            d = a
        else
            if c > b && b > c
                d = c
            else
                d = b
        """);

        IfNode ifnode = (IfNode)root.Classes.getFirst().methods.getFirst().statements.getFirst();
        IfNode ifnode1 = (IfNode)ifnode.elseStatement.get().statements.getFirst();
    }

    @Test
    void boolExp2() throws Exception {
        TranNode root = lexAndParse("""
class Program
    start()
        number a
        number b
        number c
        a=b
        b=a
        if a == b
            c = a
        else
            if a > b
                c = b
            else
                c = c
        """);

        assertEquals(1, root.Classes.size());
    }

    @Test
    void bool() throws Exception {
        TranNode root = lexAndParse("""
class Program
    myMethod() : boolean c
        a = true
        b = false
        if a || b
            c = false
        if a && b
            c = true
        """);

        List<StatementNode> statements = root.Classes.getFirst().methods.getFirst().statements;

        assertEquals("a", ((AssignmentNode)statements.get(0)).target.name);
        assertTrue(((BooleanLiteralNode)((AssignmentNode)statements.get(0)).expression).value);

        assertEquals("b", ((AssignmentNode)statements.get(1)).target.name);
        assertFalse(((BooleanLiteralNode)((AssignmentNode)statements.get(1)).expression).value);

        IfNode if1 = (IfNode)statements.get(2);
        assertEquals("a", ((VariableReferenceNode)((BooleanOpNode)if1.condition).left).name);
        assertEquals(BooleanOpNode.BooleanOperations.or, ((BooleanOpNode)if1.condition).op);
        assertEquals("b", ((VariableReferenceNode)((BooleanOpNode)if1.condition).right).name);

        IfNode if2 = (IfNode)statements.get(3);
        assertEquals("a", ((VariableReferenceNode)((BooleanOpNode)if2.condition).left).name);
        assertEquals(BooleanOpNode.BooleanOperations.and, ((BooleanOpNode)if2.condition).op);
        assertEquals("b", ((VariableReferenceNode)((BooleanOpNode)if2.condition).right).name);
    }

    @Test
    void newKeyword() throws Exception {
        TranNode root = lexAndParse("""
class Person
    string firstName
    string lastName
    number age
class Program
    start()
        Person john
        Person jane
        john = new Person("John", "Doe", 24)
        jane = new Person("Jane", "Doe", 25)
        """);

        VariableDeclarationNode declareJohn = root.Classes.get(1).methods.getFirst().locals.get(0);
        VariableDeclarationNode declareJane = root.Classes.get(1).methods.getFirst().locals.get(1);

        assertEquals("Person", declareJohn.type);
        assertEquals("john", declareJohn.name);
        assertEquals("Person", declareJane.type);
        assertEquals("jane", declareJane.name);

        AssignmentNode john = (AssignmentNode)root.Classes.get(1).methods.getFirst().statements.get(0);
        AssignmentNode jane = (AssignmentNode)root.Classes.get(1).methods.getFirst().statements.get(1);

        assertEquals("Person", ((NewNode)john.expression).className);
        assertEquals("John",   ((StringLiteralNode)(((NewNode)john.expression).parameters.get(0))).value);
        assertEquals("Doe",    ((StringLiteralNode)(((NewNode)john.expression).parameters.get(1))).value);
        assertEquals(24,       ((NumericLiteralNode)(((NewNode)john.expression).parameters.get(2))).value);

        assertEquals("Person", ((NewNode)jane.expression).className);
        assertEquals("Jane",   ((StringLiteralNode)(((NewNode)jane.expression).parameters.get(0))).value);
        assertEquals("Doe",    ((StringLiteralNode)(((NewNode)jane.expression).parameters.get(1))).value);
        assertEquals(25,       ((NumericLiteralNode)(((NewNode)jane.expression).parameters.get(2))).value);
    }
}
