import java.util.ArrayList;

public class Compiler {

    private String fileName;

    public Compiler(String fileName) {
        this.fileName = fileName;
    }

    public void compile() {

        analyzeSyntax(); // Call tokenizer and parser on file.jack --> file.xml
        CodeGenerator generator = new CodeGenerator(fileName);
        generator.generateVirtualCode(); // Call generator on file.xml --> file.vm

    }

    // TODO: Make private once code generator is complete
    public void analyzeSyntax() {

        Tokenizer tokenizer = new Tokenizer(fileName);
        ArrayList<Token> tokens = tokenizer.getTokens();

        Parser parser = new Parser(fileName, tokens);
        parser.generateParseTree(); // creates XML

    }
}
