// public class TextManager {

//     private final String input;
//     private int position = 0;
//     private int line = 0;
//     private int column = 0;

//     public TextManager(String input) {
//         this.input = input.replace("\r\n", "\n");
//     }

//     public boolean isAtEnd() {
//         return position > input.length() - 1;
//     }

//     public char peekCharacter() throws IndexOutOfBoundsException {
//         return peekCharacter(1);
//     }

//     public char peekCharacter(int distance) throws IndexOutOfBoundsException {
//         return input.charAt(position + distance);
//     }

//     /**
//      * Returns the next character from the input string. Updates the current
//      * line and column numbers.
//      * @return the next character.
//      * @throws IndexOutOfBoundsException
//      */
//     public char getCharacter() throws IndexOutOfBoundsException {
//         position++;
//         column++;
//         char c = input.charAt(position);
//         if (position == 1) {
//             line = 1;
//             column = 1;
//         } else if (peekCharacter(-1) == '\n') {
//             line++;
//             column = 1;
//         }
//         return c;
//     }

//     /**
//      * Returns the line number of the character returned by
//      * {@code getCharacter()}. The first line is 1 and will return 0 if
//      * {@code getCharacter()} is yet to be called.
//      *
//      * @return the line number of the current character.
//      */
//     public int getLineNumber() {
//         return line;
//     }

//     /**
//      * Returns the column number of the character returned by
//      * {@code getCharacter()}. The first character in a line is 1 and will
//      * return 0 if {@code getCharacter()} is yet to be called
//      *
//      * @return the column number of the current character.
//      */
//     public int getColNumber() {
//         return column;
//     }

//     public static void main(String[] args) {
//         TextManager tm = new TextManager("lorem\nipsum\ndolor\nsit\namet\n");
//         System.out.println(tm.getLineNumber() + ":" + tm.getColNumber());
//         while (!tm.isAtEnd()) {
//             char c = tm.getCharacter();
//             System.out.println(tm.getLineNumber() + ":" + tm.getColNumber() + " '" + c + "'");
//         }
//     }
// }

import java.util.Optional;

public class TextManager {

    private final String input;
    private int position = -1;
    private int line = -1;
    private int column = -1;

    public TextManager(String input) {
        this.input = input.replace("\r\n", "\n");
    }

    public boolean isAtEnd() {
        return position >= input.length() - 1;
    }

    public char peekCharacter() throws IndexOutOfBoundsException {
        return peekCharacter(1);
    }

    public char peekCharacter(int distance) throws IndexOutOfBoundsException {
        return input.charAt(position + distance);
    }

    public Optional<Character> matchMove(char pattern) throws IndexOutOfBoundsException {
        char c = input.charAt(position + 1);
        if (c == pattern) {
            position++;
            column++;
            if (position == 0) {
                line = 0;
                column = 0;
            } else if (peekCharacter(-1) == '\n') {
                line++;
                column = 0;
            }
            return Optional.of(c);
        }
        return Optional.empty();
    }

    /**
     * Returns the next character from the input string. Updates the current
     * line and column numbers.
     * @return the next character.
     * @throws IndexOutOfBoundsException
     */
    public char getCharacter() throws IndexOutOfBoundsException {
        position++;
        column++;
        char c = input.charAt(position);
        if (position == 0) {
            line = 0;
            column = 0;
        } else if (peekCharacter(-1) == '\n') {
            line++;
            column = 0;
        }
        return c;
    }

    /**
     * Returns the line number of the character returned by
     * {@code getCharacter()}. The first line is 0 and will return -1 if
     * {@code getCharacter()} is yet to be called.
     *
     * @return the line number of the current character.
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * Returns the column number of the character returned by
     * {@code getCharacter()}. The first character in a line is 0 and will
     * return -1 if {@code getCharacter()} is yet to be called
     *
     * @return the column number of the current character.
     */
    public int getColNumber() {
        return column;
    }

    public static void main(String[] args) {
        TextManager tm = new TextManager("lorem\nipsum\ndolor\nsit\namet\n");
        System.out.println(tm.getLineNumber() + ":" + tm.getColNumber());
        while (!tm.isAtEnd()) {
            char c = tm.getCharacter();
            System.out.println(tm.getLineNumber() + ":" + tm.getColNumber() + " '" + c + "'");
        }
    }
}
