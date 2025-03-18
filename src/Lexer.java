import java.util.LinkedList;
import java.util.HashMap;

public class Lexer {

    private final HashMap<String, Token.TokenTypes> keywordMap;
    private final HashMap<String, Token.TokenTypes> punctuationMap;
    private final LinkedList<Token> tokens;
    private final TextManager textManager;
    private int indentLevel;
//    private int indentLevelBaseline;

    public Lexer(String input) {
        tokens = new LinkedList<>();
        input += """
            \ninterface iterator
                hasNext() : boolean notDone
                getNext() : number next
            """;
        /*
        class Iterator
            number current
            number max
            construct(number n)
                max = n
                current = 0
            hasNext() : boolean notDone
                notDone = current == max
            getNext() : number next
                cur = cur + 1
                next = cur
         */
        textManager = new TextManager(input);

        keywordMap = new HashMap<>();
        keywordMap.put("accessor", Token.TokenTypes.ACCESSOR);
        keywordMap.put("mutator", Token.TokenTypes.MUTATOR);
        keywordMap.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywordMap.put("class", Token.TokenTypes.CLASS);
        keywordMap.put("interface", Token.TokenTypes.INTERFACE);
        keywordMap.put("loop", Token.TokenTypes.LOOP);
        keywordMap.put("if", Token.TokenTypes.IF);
        keywordMap.put("else", Token.TokenTypes.ELSE);
        keywordMap.put("true", Token.TokenTypes.TRUE);
        keywordMap.put("false", Token.TokenTypes.FALSE);
        keywordMap.put("new", Token.TokenTypes.NEW);
        keywordMap.put("private", Token.TokenTypes.PRIVATE);
        keywordMap.put("shared", Token.TokenTypes.SHARED);
        keywordMap.put("construct", Token.TokenTypes.CONSTRUCT);
        // keywordMap.put("and", Token.TokenTypes.AND);
        // keywordMap.put("or", Token.TokenTypes.OR);
        // keywordMap.put("not", Token.TokenTypes.NOT);

        punctuationMap = new HashMap<>();
        punctuationMap.put("=", Token.TokenTypes.ASSIGN);
        punctuationMap.put("(", Token.TokenTypes.LPAREN);
        punctuationMap.put(")", Token.TokenTypes.RPAREN);
        punctuationMap.put(":", Token.TokenTypes.COLON);
        punctuationMap.put(".", Token.TokenTypes.DOT);
        punctuationMap.put("+", Token.TokenTypes.PLUS);
        punctuationMap.put("-", Token.TokenTypes.MINUS);
        punctuationMap.put("*", Token.TokenTypes.TIMES);
        punctuationMap.put("/", Token.TokenTypes.DIVIDE);
        punctuationMap.put("%", Token.TokenTypes.MODULO);
        punctuationMap.put(",", Token.TokenTypes.COMMA);
        punctuationMap.put("==", Token.TokenTypes.EQUAL);
        punctuationMap.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuationMap.put("<", Token.TokenTypes.LESSTHAN);
        punctuationMap.put(">", Token.TokenTypes.GREATERTHAN);
        punctuationMap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuationMap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        punctuationMap.put("&&", Token.TokenTypes.AND);
        punctuationMap.put("||", Token.TokenTypes.OR);
        punctuationMap.put("!", Token.TokenTypes.NOT);
    }

    public LinkedList<Token> Lex() throws SyntaxErrorException {
        while (!textManager.isAtEnd()) {
            char c = textManager.getCharacter();
            Token t = null;

            if (c == ' ')
                continue;
            else if (c == '"')
                t = parseString();
            else if (c == '\'')
                t = parseCharacter();
            else if (Character.isLetter(c))
                t = parseWord(c);
            else if (Character.isDigit(c) || (c == '.' && Character.isDigit(textManager.peekCharacter())))
                t = parseNumber(c);
            else if (c == '{')
                parseComment();
            else if (c == '\n')
                parseIndentation(c);
            else
                t = parsePunctuation(c);
            if (t != null)
                tokens.add(t);
        }
        while (indentLevel > 0) {
            tokens.add(new Token(Token.TokenTypes.DEDENT, textManager.getLineNumber(), textManager.getColNumber()));
            indentLevel--;
        }
        return tokens;
    }

    private Token parseWord(char c) {
        String buffer = String.valueOf(c);
        int line = textManager.getLineNumber();
        int col = textManager.getColNumber();
        while (!textManager.isAtEnd()) {
            char p = textManager.peekCharacter();
            if (!Character.isLetter(p))
                break;
            c = textManager.getCharacter();
            buffer += c;
        }
        if (keywordMap.containsKey(buffer))
            return new Token(keywordMap.get(buffer), line, col);
        else
            return new Token(Token.TokenTypes.WORD, line, col, buffer);
    }

    private Token parseNumber(char c) throws SyntaxErrorException {
        String buffer = String.valueOf(c);
        int line = textManager.getLineNumber();
        int col = textManager.getColNumber();
        int decimals = 0;

        while (!textManager.isAtEnd()) {
            char p = textManager.peekCharacter();
            if (!Character.isDigit(p) && p != '.')
                break;
            if (p == '.') {
                decimals++;
                if (decimals >= 2)
                    throw new SyntaxErrorException("Number can only contain one decimal point", line, col);
            }
            c = textManager.getCharacter();
            buffer += c;
        }
        return new Token(Token.TokenTypes.NUMBER, line, col, buffer);
    }

    private Token parsePunctuation(char c) throws SyntaxErrorException {
        int line = textManager.getLineNumber();
        int col = textManager.getColNumber();

        String buffer = String.valueOf(c);
        Token token = null;
        if (punctuationMap.containsKey(buffer))
            token = new Token(punctuationMap.get(buffer), line, col);
        if (!textManager.isAtEnd()) {
            buffer += textManager.peekCharacter();
            if (punctuationMap.containsKey(buffer)) {
                token = new Token(punctuationMap.get(buffer), line, col);
                textManager.getCharacter();
            }
        }
        if (token == null)
            throw new SyntaxErrorException("Unknown punctuation '" + buffer + "'", line, col);
        return token;
    }

    private void parseIndentation(char c) throws SyntaxErrorException {
        tokens.add(new Token(Token.TokenTypes.NEWLINE, textManager.getLineNumber(), textManager.getColNumber()));
        int tabs = 0;
        int spaces = 0;
        while (!textManager.isAtEnd()) {
            if (textManager.matchMove(' ').isPresent())
                spaces++;
            else if (textManager.matchMove('\t').isPresent())
                tabs++;
            else if (textManager.matchMove('\n').isPresent()) {
                tabs = 0;
                spaces = 0;
            } else
                break;
        }
        int normalize = (tabs * 4) + spaces;
        if (normalize % 4 != 0)
            throw new SyntaxErrorException("Indentation is not a multiple of 4", textManager.getLineNumber(), 0);
        int newIndentLevel = normalize / 4;
        int diff = (newIndentLevel - indentLevel);
        indentLevel = newIndentLevel;
        if (diff > 0) {
            for (int i = 0; i < diff; i++)
                tokens.add(new Token(Token.TokenTypes.INDENT, textManager.getLineNumber(), 0));
        } else if (diff < 0) {
            for (int i = 0; i > diff; i--)
                tokens.add(new Token(Token.TokenTypes.DEDENT, textManager.getLineNumber(), 0));
        }
    }

//    private void parseIndentation(char c) throws SyntaxErrorException {
//        tokens.add(new Token(Token.TokenTypes.NEWLINE, textManager.getLineNumber(), textManager.getColNumber()));
//        int line = textManager.getLineNumber() + 1;
//        int tabs = 0;
//        int spaces = 0;
//        char p = '\0';
//        while (!textManager.isAtEnd()) {
//            p = textManager.peekCharacter();
//            if (p != ' ' && p != '\t' && p != '\n')
//                break;
//            c = textManager.getCharacter();
//            if (c == ' ')
//                spaces++;
//            else
//                tabs++;
//        }
//        if (p == '\n')
//            return;
//        int normalize = (tabs * 4) + spaces;
//        if (normalize % 4 != 0)
//            throw new SyntaxErrorException("Indentation is not a multiple of 4", line, 0);
//        int newIndentLevel = normalize / 4;
//        int diff = (newIndentLevel - indentLevel);
//        indentLevel = newIndentLevel;
//        if (diff > 0) {
//            for (int i = 0; i < diff; i++)
//                tokens.add(new Token(Token.TokenTypes.INDENT, line, 0));
//        } else if (diff < 0) {
//            for (int i = 0; i > diff; i--)
//                tokens.add(new Token(Token.TokenTypes.DEDENT, line, 0));
//        }
//    }

    private Token parseCharacter() throws SyntaxErrorException {
        char c = textManager.getCharacter();
        int line = textManager.getLineNumber();
        int col = textManager.getColNumber();
        if (textManager.getCharacter() != '\'')
            throw new SyntaxErrorException("Closing ' not found", line, col);
        return new Token(Token.TokenTypes.QUOTEDCHARACTER, line, col, String.valueOf(c));
    }

    private Token parseString() {
        String buffer = "";
        char c = textManager.getCharacter();
        int line = textManager.getLineNumber();
        int col = textManager.getColNumber();
        while (!textManager.isAtEnd() && c != '"') {
            buffer += c;
            c = textManager.getCharacter();
        }
        return new Token(Token.TokenTypes.QUOTEDSTRING, line, col, buffer);
    }

    private void parseComment() throws SyntaxErrorException {
        char c = textManager.getCharacter();
        while (c != '}') {
            if (textManager.isAtEnd())
                throw new SyntaxErrorException("Closing } not found", textManager.getLineNumber(), textManager.getColNumber());
            c = textManager.getCharacter();
        }
    }
}


// import java.util.LinkedList;
// import java.util.HashMap;

// public class Lexer {

//     private final HashMap<String, Token.TokenTypes> keywordMap;
//     private final HashMap<String, Token.TokenTypes> punctuationMap;
//     private final TextManager textManager;
//     private final LinkedList<Token> tokens;
//     private char currentChar;
//     private int indentLevel;

//     public Lexer(String input) {
//         tokens = new LinkedList<>();
//         textManager = new TextManager(input);
//         currentChar = textManager.getCharacter();
//         indentLevel = 0;

//         keywordMap = new HashMap<>();
//         keywordMap.put("accessor", Token.TokenTypes.ACCESSOR);
//         keywordMap.put("mutator", Token.TokenTypes.MUTATOR);
//         keywordMap.put("implements", Token.TokenTypes.IMPLEMENTS);
//         keywordMap.put("class", Token.TokenTypes.CLASS);
//         keywordMap.put("interface", Token.TokenTypes.INTERFACE);
//         keywordMap.put("loop", Token.TokenTypes.LOOP);
//         keywordMap.put("if", Token.TokenTypes.IF);
//         keywordMap.put("else", Token.TokenTypes.ELSE);
//         keywordMap.put("true", Token.TokenTypes.TRUE);
//         keywordMap.put("false", Token.TokenTypes.FALSE);
//         keywordMap.put("new", Token.TokenTypes.NEW);
//         keywordMap.put("private", Token.TokenTypes.PRIVATE);
//         keywordMap.put("shared", Token.TokenTypes.SHARED);
//         keywordMap.put("construct", Token.TokenTypes.CONSTRUCT);
//         // keywordMap.put("and", Token.TokenTypes.AND);
//         // keywordMap.put("or", Token.TokenTypes.OR);
//         // keywordMap.put("not", Token.TokenTypes.NOT);

//         punctuationMap = new HashMap<>();
//         punctuationMap.put("=", Token.TokenTypes.ASSIGN);
//         punctuationMap.put("(", Token.TokenTypes.LPAREN);
//         punctuationMap.put(")", Token.TokenTypes.RPAREN);
//         punctuationMap.put(":", Token.TokenTypes.COLON);
//         punctuationMap.put(".", Token.TokenTypes.DOT);
//         punctuationMap.put("+", Token.TokenTypes.PLUS);
//         punctuationMap.put("-", Token.TokenTypes.MINUS);
//         punctuationMap.put("*", Token.TokenTypes.TIMES);
//         punctuationMap.put("/", Token.TokenTypes.DIVIDE);
//         punctuationMap.put("%", Token.TokenTypes.MODULO);
//         punctuationMap.put(",", Token.TokenTypes.COMMA);
//         punctuationMap.put("==", Token.TokenTypes.EQUAL);
//         punctuationMap.put("!=", Token.TokenTypes.NOTEQUAL);
//         punctuationMap.put("<", Token.TokenTypes.LESSTHAN);
//         punctuationMap.put(">", Token.TokenTypes.GREATERTHAN);
//         punctuationMap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
//         punctuationMap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
//         punctuationMap.put("&&", Token.TokenTypes.AND);
//         punctuationMap.put("||", Token.TokenTypes.OR);
//         punctuationMap.put("!", Token.TokenTypes.NOT);
//     }

//     public LinkedList<Token> Lex() throws SyntaxErrorException {
//         while (!textManager.isAtEnd()) {
//             if (currentChar == '"')
//                 parseString();
//             else if (currentChar == '\'')
//                 parseCharacter();
//             else if (Character.isLetter(currentChar))
//                 parseWord();
//             else if (Character.isDigit(currentChar) || (currentChar == '.' && Character.isDigit(textManager.peekCharacter())))
//                 parseNumber();
//             else if (currentChar == '{')
//                 parseComment();
//             else if (currentChar == '\n')
//                 parseIndentation();
//             else if (isPuncuation())
//                 parsePunctuation();
//             else
//                 currentChar = textManager.getCharacter();
//         }
//         while (indentLevel > 0) {
//             tokens.add(new Token(Token.TokenTypes.DEDENT, textManager.getLineNumber(), textManager.getColNumber()));
//             indentLevel--;
//         }
//         return tokens;
//     }

//     private void parseWord() {
//         String buffer = "";
//         int line = textManager.getLineNumber();
//         int col = textManager.getColNumber();
//         while (!textManager.isAtEnd() && Character.isLetter(currentChar)) {
//             buffer += currentChar;
//             currentChar = textManager.getCharacter();
//         }
//         if (keywordMap.containsKey(buffer))
//             tokens.add(new Token(keywordMap.get(buffer), line, col));
//         else
//             tokens.add(new Token(Token.TokenTypes.WORD, line, col, buffer));
//     }

//     private void parseNumber() throws SyntaxErrorException {
//         String buffer = "";
//         int line = textManager.getLineNumber();
//         int col = textManager.getColNumber();
//         int decimals = 0;

//         while (!textManager.isAtEnd() && (Character.isDigit(currentChar) || currentChar == '.')) {
//             buffer += currentChar;
//             if (currentChar == '.') {
//                 decimals++;
//                 if (decimals > 1)
//                     throw new SyntaxErrorException("Number can only contain one decimal point", line, col);
//             }
//             currentChar = textManager.getCharacter();
//         }
//         tokens.add(new Token(Token.TokenTypes.NUMBER, line, col, buffer));
//     }

//     private void parsePunctuation() throws SyntaxErrorException {
//         int line = textManager.getLineNumber();
//         int col = textManager.getColNumber();

//         String buffer = String.valueOf(currentChar);
//         Token token = null;
//         if (punctuationMap.containsKey(buffer))
//             token = new Token(punctuationMap.get(buffer), line, col);
//         if (!textManager.isAtEnd()) {
//             buffer += textManager.peekCharacter();
//             if (punctuationMap.containsKey(buffer)) {
//                 token = new Token(punctuationMap.get(buffer), line, col);
//                 textManager.getCharacter();
//             }
//         }
//         if (token == null)
//             throw new SyntaxErrorException("Unknown punctuation '" + buffer + "'", line, col);
//         tokens.add(token);
//         currentChar = textManager.getCharacter();
//     }

//     private void parseIndentation() throws SyntaxErrorException {
//         tokens.add(new Token(Token.TokenTypes.NEWLINE, textManager.getLineNumber(), textManager.getColNumber()));
//         int line = textManager.getLineNumber() + 1;
//         int tabs = 0;
//         int spaces = 0;
//         while (!textManager.isAtEnd()) {
//             char p = textManager.peekCharacter();
//             if (p != ' ' && p != '\t' && p != '\n')
//                 break;
//             currentChar = textManager.getCharacter();
//             if (currentChar == ' ')
//                 spaces++;
//             else
//                 tabs++;
//         }
//         int normalize = (tabs * 4) + spaces;
//         if (normalize % 4 != 0)
//             throw new SyntaxErrorException("Indentation is not a multiple of 4", line, 0);
//         int newIndentLevel = normalize / 4;
//         int diff = (newIndentLevel - indentLevel);
//         indentLevel = newIndentLevel;
//         if (diff > 0) {
//             for (int i = 0; i < diff; i++)
//                 tokens.add(new Token(Token.TokenTypes.INDENT, line, 0));
//         } else if (diff < 0) {
//             for (int i = 0; i > diff; i--)
//                 tokens.add(new Token(Token.TokenTypes.DEDENT, line, 0));
//         }
//     }

//     private void parseCharacter() throws SyntaxErrorException {
//         currentChar = textManager.getCharacter();
//         int line = textManager.getLineNumber();
//         int col = textManager.getColNumber();
//         if (textManager.getCharacter() != '\'')
//             throw new SyntaxErrorException("Closing ' not found", line, col);
//         tokens.add(new Token(Token.TokenTypes.QUOTEDCHARACTER, line, col, String.valueOf(currentChar)));
//         currentChar = textManager.getCharacter();
//     }

//     private void parseString() {
//         String buffer = "";
//         currentChar = textManager.getCharacter();
//         int line = textManager.getLineNumber();
//         int col = textManager.getColNumber();
//         while (!textManager.isAtEnd() && currentChar != '"') {
//             buffer += currentChar;
//             currentChar = textManager.getCharacter();
//         }
//         tokens.add(new Token(Token.TokenTypes.QUOTEDSTRING, line, col, buffer));
//         currentChar = textManager.getCharacter();
//     }

//     private void parseComment() throws SyntaxErrorException {
//         while (currentChar != '}') {
//             if (textManager.isAtEnd())
//                 throw new SyntaxErrorException("Closing } not found", textManager.getLineNumber(), textManager.getColNumber());
//             currentChar = textManager.getCharacter();
//         }
//         currentChar = textManager.getCharacter();
//     }

//     private boolean isPuncuation() {
//         for (String key : punctuationMap.keySet())
//             if (key.startsWith(String.valueOf(currentChar)))
//                 return true;
//         return false;
//     }
// }
