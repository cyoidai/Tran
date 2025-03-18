import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


public class Lexer1MyTests {

    @Test
    public void simpleTest() throws Exception {
        Lexer l = new Lexer("a");
        List<Token> res = l.Lex();
        Assertions.assertEquals(1, res.size());
        Assertions.assertEquals("a", res.getFirst().getValue());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.getFirst().getType());
    }

    @Test
    public void simpleTest1() throws Exception {
        Lexer l = new Lexer("a bc d");
        List<Token> res = l.Lex();
        Assertions.assertEquals(3, res.size());
        Assertions.assertEquals("a", res.get(0).getValue());
        Assertions.assertEquals("bc", res.get(1).getValue());
        Assertions.assertEquals("d", res.get(2).getValue());
        for (Token result : res)
            Assertions.assertEquals(Token.TokenTypes.WORD, result.getType());
    }

    @Test
    public void IndentTest() throws Exception {
        Lexer l = new Lexer("loop keepGoing\n    if n >= 15\r\n\t\tkeepGoing = false\n");
        List<Token> res = l.Lex();
        Assertions.assertEquals(16, res.size());
    }

    @Test
    public void mixedTest() throws Exception {
        Lexer l = new Lexer(".2-.3iloveicsithreeoneone3.14==22/7if!= <= :\n    (\n)\nfoo.bar");

        List<Token> res = l.Lex();
        Assertions.assertEquals(23, res.size());
        Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(0).getType());
        Assertions.assertEquals(".2", res.get(0).getValue());
        Assertions.assertEquals(Token.TokenTypes.MINUS, res.get(1).getType());
        Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(2).getType());
        Assertions.assertEquals(".3", res.get(2).getValue());

        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("iloveicsithreeoneone", res.get(3).getValue());

        Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
        Assertions.assertEquals("3.14", res.get(4).getValue());

        Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
        Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(6).getType());
        Assertions.assertEquals(Token.TokenTypes.DIVIDE, res.get(7).getType());
        Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(8).getType());
        Assertions.assertEquals(Token.TokenTypes.IF, res.get(9).getType());
        Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(10).getType());
        Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(11).getType());
        Assertions.assertEquals(Token.TokenTypes.COLON, res.get(12).getType());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(13).getType());
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(14).getType());
        Assertions.assertEquals(Token.TokenTypes.LPAREN, res.get(15).getType());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(16).getType());
        Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(17).getType());
        Assertions.assertEquals(Token.TokenTypes.RPAREN, res.get(18).getType());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(19).getType());

        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(20).getType());
        Assertions.assertEquals("foo",  res.get(20).getValue());
        Assertions.assertEquals(Token.TokenTypes.DOT, res.get(21).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(22).getType());
        Assertions.assertEquals("bar",  res.get(22).getValue());
    }
}
