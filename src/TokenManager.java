import java.util.List;
import java.util.Optional;

public class TokenManager {

    private final List<Token> tokens;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
        return tokens.isEmpty();
    }

    /**
     * Removes and returns the next token from the list of tokens only if the
     * next token's type matches any token type in {@code types}.
     * @param types The token type to match.
     * @return The next token.
     */
    public Optional<Token> matchAndRemove(Token.TokenTypes... types) {
        if (tokens.isEmpty())
            return Optional.empty();
        Token token = tokens.getFirst();
        for (Token.TokenTypes t : types) {
            if (token.getType() == t)
                return Optional.of(tokens.removeFirst());
        }
        return Optional.empty();
    }

    /**
     * Returns a token without removing it from the list of tokens.
     * @param i Offset value.
     * @return A token.
     */
    public Optional<Token> peek(int i) {
        if (i < 0 || i >= tokens.size())
            return Optional.empty();
        return Optional.of(tokens.get(i));
    }

    /**
     * Equivalent to {@code peek(0)}.
     * @return The next token.
     */
    public Optional<Token> peek() {
        return peek(0);
    }

    public Optional<Token> peekMatch(int i, Token.TokenTypes t) {
        Optional<Token> o = peek(i);
        if (o.isPresent() && o.get().getType() == t)
            return o;
        return Optional.empty();
    }

    /**
     * Peeks the {@code i}-th token, if any type of {@code types} matches the
     * token, it is returned.
     * @param i     Offset value.
     * @param types Token types to match,
     * @return A token.
     */
    public Optional<Token> peekMatch(int i, Token.TokenTypes... types) {
        Optional<Token> o = peek(i);
        for (Token.TokenTypes t : types)
            if (o.isPresent() && o.get().getType() == t)
                return o;
        return Optional.empty();
    }

    /**
     * Returns true or false depending on whether the next tokens match a given
     * token type pattern. Use {@code null} for representing any token.
     * @param offset Offset from start.
     * @param types  Token type pattern.
     * @return True or false if tokens match the pattern.
     */
    public boolean matchPattern(int offset, Token.TokenTypes... types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == null)
                continue;
            Optional<Token> o = peek(offset + i);
            if (o.isEmpty() || o.get().getType() != types[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Equivalent to {@code matchPattern(0, ...)}
     */
    public boolean matchPattern(Token.TokenTypes... types) {
        return matchPattern(0, types);
    }

    /**
     * @deprecated
     * Use matchPattern instead
     */
    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        Optional<Token> o1 = peek(0);
        Optional<Token> o2 = peek(1);
        return (
            (o1.isPresent() && o2.isPresent()) &&
            (o1.get().getType() == first && o2.get().getType() == second)
        );
    }

    public int getCurrentLine() {
        return tokens.getFirst().getLineNumber();
    }

    public int getCurrentColumnNumber() {
        return tokens.getFirst().getColumnNumber();
    }
}
