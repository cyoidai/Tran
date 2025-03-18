import AST.*;
import Interpreter.ConsoleWrite;
import Interpreter.Interpreter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class InterpreterMyTests {

    @Test
    public void constructors() throws Exception {
        List<String> c = run("""
            class DateTime
                construct(number timestamp)
                    console.write("DateTime object created using a numeric timestamp")
                construct(string yyyymmdd, string hhmmss)
                    console.write("DateTime object created using a date and a time")
            
            class DateTimeDelta
                construct(number years, number months, number days, number h, number m, number s, number ms)
                    console.write("TimeDateDelta object created using manually entered values")
                construct(DateTime start, DateTime end)
                    console.write("TimeDateDelta object created using two DateTime objects")
                construct(number start, number end)
                    console.write("TimeDateDelta object created using two numerical timestamps")
            
            class Program
                shared start()
                    DateTime dt
                    dt = new DateTime(1735689600)
                    dt = new DateTime("2025-01-01", "00:00:00")
                    DateTimeDelta td
                    td = new DateTimeDelta(1, 2, 3, 4, 5, 6, 7)
                    td = new DateTimeDelta(dt, dt)
                    td = new DateTimeDelta(1732925424, 1732925589)
            """);
        assertEquals(5, c.size());
        assertEquals("DateTime object created using a numeric timestamp", c.get(0));
        assertEquals("DateTime object created using a date and a time", c.get(1));
        assertEquals("TimeDateDelta object created using manually entered values", c.get(2));
        assertEquals("TimeDateDelta object created using two DateTime objects", c.get(3));
        assertEquals("TimeDateDelta object created using two numerical timestamps", c.get(4));
    }

    @Test
    public void sharedMethod() throws Exception {
        List<String> c = run("""
            class Math
                shared pi() : number a
                    a = 3.14
            class Program
                shared start()
                    console.write(Math.pi())
            """);
        assertEquals(1, c.size());
        assertEquals("3.14", c.getFirst());
    }

    @Test
    public void privateMethod() throws Exception {
        List<String> c = run("""
            class Student
                number id
                string name
                construct(number i, string n)
                    id = i
                    name = n
                getID() : number i
                    i = id
                equals(Student s) : boolean res
                    if id == s.getID()
                        res = true
                    else
                        res = false
            class Program
                shared start()
                    Student mark
                    Student jennifer
                    mark = new Student(1, "Mark")
                    jennifer = new Student(2, "Jennifer")
                    if !mark.equals(jennifer)
                        console.write("mark is not jennifer")
                    else
                        console.write("mark is jennifer, that can't be!")
            """);
        assertEquals(1, c.size());
        assertEquals("mark is not jennifer", c.getFirst());
    }

    @Test
    public void orderOfOperations() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    number a
                    number b
                    number c
                    a = 8 - 8 / 4 * 2 + 7
                    b = 29 - 2 * 5
                    c = 4 * 5 + a
                    console.write(a)
                    console.write(b)
                    console.write(c)
            """);
        assertEquals(3, c.size());
        assertEquals("11.0", c.get(0));
        assertEquals("19.0", c.get(1));
        assertEquals("31.0", c.get(2));
    }

    @Test
    public void ifStatement() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    number a
                    a = 10
                    if a < 10
                        a = a + 5
                    else
                        a = a - 5
                        if a < 6
                            a = 5
                        else
                            a = 4
                    console.write(a)
            """);
        assertEquals(1, c.size());
        assertEquals("5.0", c.getFirst());
    }

    @Test
    public void loopConditional() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    boolean keepGoing
                    number x
                    x = 1
                    loop x < 4
                        console.write(x)
                        x = x + 1
            """);
        assertEquals(3, c.size());
        assertEquals("1.0", c.get(0));
        assertEquals("2.0", c.get(1));
        assertEquals("3.0", c.get(2));
    }

    @Test void loopConditionalAssignment() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    number x
                    boolean condition
                    x = 1
                    condition = loop x < 4
                        console.write(condition)
                        console.write(x)
                        x = x + 1
                    console.write(condition)
            """);
        assertEquals(7, c.size());
        assertEquals("true", c.get(0));
        assertEquals("1.0",  c.get(1));
        assertEquals("true", c.get(2));
        assertEquals("2.0",  c.get(3));
        assertEquals("true", c.get(4));
        assertEquals("3.0",  c.get(5));
        assertEquals("false", c.get(6));
    }

    @Test
    public void loopIterator() throws Exception {
        List<String> c = run("""
            class Program
                shared multiply(number x, number y) : number z
                    loop x.times()
                        z = z + y
                shared start()
                    if 2 * 2 == Program.multiply(2, 2)
                        console.write("2 * 2 = 4")
                    else
                        console.write("2 * 2 != 4")
            """);
        Assertions.assertEquals(1,c.size());
        Assertions.assertEquals("2 * 2 = 4",c.getFirst());
    }

    @Test
    public void loopIteratorAssignment() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    number x
                    number next
                    x = 5
                    next = loop x.times()
                        console.write(next)
                    console.write(next)
            """);
        assertEquals(6, c.size());
        assertEquals("0.0", c.get(0));
        assertEquals("1.0", c.get(1));
        assertEquals("2.0", c.get(2));
        assertEquals("3.0", c.get(3));
        assertEquals("4.0", c.get(4));
        assertEquals("5.0", c.get(5));
    }

    @Test
    public void stringConcat() throws Exception {
        List<String> c = run("""
            class Program
                shared start()
                    string x
                    string y
                    string z
                    x = "lorem"
                    y = "ipsum"
                    z = x + y
                    console.write(z)
                    console.write(x + " " + y)
            """);
        assertEquals(2, c.size());
        assertEquals("loremipsum", c.getFirst());
        assertEquals("lorem ipsum", c.getLast());
    }

    private static List<String> getConsole(TranNode tn) {
        for (ClassNode c : tn.Classes)
            if (c.name.equals("console")) {
                for (MethodDeclarationNode m : c.methods)  {
                    if (m.name.equals("write")) {
                        return ((ConsoleWrite)m).console;
                    }
                }
            }
        throw new RuntimeException("Unable to find console");
    }

    private static List<String> run(String program) throws Exception {
        Lexer l = new Lexer(program);
        LinkedList<Token> tokens = l.Lex();
        TranNode root = new TranNode();
        new Parser(root, tokens).Tran();
        var i = new Interpreter(root);
        i.start();
        return getConsole(root);
    }
}
