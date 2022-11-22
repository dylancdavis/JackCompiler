import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {

    private String fileName;

    private List<String> keywords = Arrays.asList(
                                "class","constructor","function","method","field","static",
                                "var","int","char","boolean","void","true","false","null","this",
                                "let","do","if","else","while","return");

    private List<Character> operators = Arrays.asList('{','}','(',')',')','[',']','.',',',';','+','-','*','/','&','|','<','>','=','~');

    public Tokenizer(String fileName) {
        this.fileName = fileName;
    }

    public ArrayList<Token> getTokens() {

        ArrayList<Token> ret = new ArrayList<>();
        ArrayList<String> lines = splitIntoLines();

        for (String line : lines) {
            ret.addAll(handleLine(line));
        }

        return ret;

    }

    private ArrayList<String> splitIntoLines() {

        boolean inComment = false;


        ArrayList<String> lines = new ArrayList<>();
        // Read file, and assign lines to an arraylist
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Main.JACK_DIR +"/"+fileName+".jack"));
            String line;

            while ((line = reader.readLine()) != null) {

                line=line.split("//")[0].trim(); // trim any comments and whitespace

                if (inComment & !line.startsWith("*")) {
                    inComment = false;
                }

                if (line.startsWith("/*")) {
                    inComment = true;
                }

                if (line.length() != 0 && !inComment) {
                    lines.add(line); // Add any nonempty lines
                    }
                }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines; // Return all applicable lines
    }

    private ArrayList<Token> handleLine(String line) {

        ArrayList<Token> ret = new ArrayList<>();

        int start = 0;
        boolean inString = false;
        for (int current=0;current<line.length();current++) {

            // Case: already in a string
            if (inString) {
                // Within a string already, so just search for the end quote.
                if (line.charAt(current) == '"') {
                    // Once found, add the string constant.
                    inString = false; // no longer in string
                    ret.add(new Token("stringConstant",line.substring(start,current+1).trim()));
                    start = current+1; // start at the next character
                }

            // Case: entering a string
            } else if (line.charAt(current) == '"') {
                inString = true;
                start = current; // Should be done automatically by spaces and operators. but we'll see.

            // Case: reached an operator
            } else if (operators.contains(line.charAt(current))) {
                if (!(start == current)) {
                    // Case with a preceding string
                    String token = line.substring(start,current);
                    Token temp = tokenizeText(token);
                    if (temp != null) {
                        ret.add(temp);
                    }
                }
                // Add the symbol token.
                ret.add(new Token("symbol",Character.toString(line.charAt(current))));
                start = current+1; // Move the start forward one

            // Case: reached a space character
            } else if (line.charAt(current) == ' ') {
                if (!(start == current)) {
                    // Preceding string before space
                    String token = line.substring(start,current);
                    Token temp = tokenizeText(token);
                    if (temp != null) {
                        ret.add(temp);
                    }
                }
                start = current+1;
            }

        }
        if (start != line.length()) {
            String token = line.substring(start);
            Token temp = tokenizeText(token);
            if (temp != null) {
                ret.add(temp);
            }
        }
        return ret;
    }

    private Token tokenizeText(String token) {
        if (token.isBlank()) {
            return null;
        }
        if (keywords.contains(token)) {
            return new Token("keyword",token);
        } else if (Character.isDigit(token.charAt(0))) {
            return new Token("integerConstant",token);
        } else {
            return new Token("identifier",token);
        }
    }
}
