import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;


public class Lexer2MyTests {

    @Test
    public void quotedStringTest() throws Exception {
        Lexer l = new Lexer("\"lorem\"\n\tipsum dolor 's''i''t'\namet");
        LinkedList<Token> res = l.Lex();
        Assertions.assertEquals(11, res.size());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(0).getType());
        Assertions.assertEquals("lorem", res.get(0).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
        Assertions.assertEquals(Token.TokenTypes.INDENT, res.get(2).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
        Assertions.assertEquals("ipsum", res.get(3).getValue());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(4).getType());
        Assertions.assertEquals("dolor", res.get(4).getValue());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, res.get(5).getType());
        Assertions.assertEquals("s", res.get(5).getValue());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, res.get(6).getType());
        Assertions.assertEquals("i", res.get(6).getValue());
        Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, res.get(7).getType());
        Assertions.assertEquals("t", res.get(7).getValue());
        Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(8).getType());
        Assertions.assertEquals(Token.TokenTypes.DEDENT, res.get(9).getType());
        Assertions.assertEquals(Token.TokenTypes.WORD, res.get(10).getType());
        Assertions.assertEquals("amet", res.get(10).getValue());
    }

    @Test
    public void commentTest() {
        Lexer l = new Lexer(
            "\"lorem ipsum\"" +
            "{ this is a very\n" +
            "  very very very\n" +
            "  long multiline comment }" +
            "\"dolor sit amet\""
        );
        try {
            LinkedList<Token> res = l.Lex();
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(0).getType());
            Assertions.assertEquals("lorem ipsum", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("dolor sit amet", res.get(1).getValue());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void commentErrorTest() {
        Lexer l = new Lexer("code here { this is not a valid tran comment");
        try {
            Assertions.assertThrows(SyntaxErrorException.class, l::Lex);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
}
