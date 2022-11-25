import java.util.ArrayList;

public class Compiler {

    private final String fileName;

    public Compiler(String fileName) {
        this.fileName = fileName;
    }

    public void compile() {

        analyzeSyntax(); // Call tokenizer and parser on file.jack --> file.xml

    }

    // TODO: Make private once code generator is complete
    public void analyzeSyntax() {

        Tokenizer tokenizer = new Tokenizer(fileName);
        ArrayList<Token> tokens = tokenizer.getTokens();

        Parser parser = new Parser(fileName, tokens);
        parser.generateCode(); // creates XML

    }
}
