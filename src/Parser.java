import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Parser {

    private Token[] tokens;
    private int current;
    private BufferedWriter writer;
    private String fileName;

    private Stack<String> units;
    private int indents;
    private String outputDir;

    // TODO Complete arrays
    private List<String> varDecs = Arrays.asList("static","field");
    private List<String> subroutineDecs = Arrays.asList("constructor","function","method");
    private List<String> types = Arrays.asList("int","char","boolean");
    private List<String> statementTypes = Arrays.asList("let","if","while","do","return");
    private List<String> operators = Arrays.asList("+","-","*","/","&","|","<",">","=");
    private List<String> unaryOperators = Arrays.asList("-","~");
    private List<String> keywordConstants = Arrays.asList("true","false","null","this");

    public Parser(String fileName, ArrayList<Token> tokens) {
        this.fileName = fileName;
        this.tokens = new Token[tokens.size()];
        this.tokens = tokens.toArray(this.tokens);
        current = 0;
        indents = 0;
        units = new Stack<>();
    }

    public void generateParseTree() {
        try {
            // Initiate class-wide writing variable
            writer = new BufferedWriter(new FileWriter(Main.XML_DIR+"/"+fileName+".xml"));
            compileClass();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void compileClass() {

        units.push("class");

        openTag("class");
        eat("keyword","class");
        eat("identifier");
        eat("symbol","{");
        while(varDecs.contains(currentValue())) { compileClassVarDec(); }
        while(subroutineDecs.contains(currentValue())) { compileSubroutineDec(); }
        eat("symbol","}");
        closeTag("class");

        units.pop();
    }

    private void compileClassVarDec() {

        units.push("class variable declaration");

        openTag("classVarDec");
        eat("keyword");
        compileType();
        eat("identifier");
        while(currentValue().equals(",")) {
            eat("symbol",",");
            eat("identifier"); }
        eat("symbol",";");
        closeTag("classVarDec");
        units.pop();
    }

    private void compileSubroutineDec() {
        units.push("subroutine declaration");
        openTag("subroutineDec");
        eat("keyword"); // type of subroutine
        if (currentValue().equals("void")) {
            eat("keyword");
        } else {
            compileType();
        }
        eat("identifier"); // subroutine name
        eat("symbol","(");
        compileParameterList();
        eat("symbol",")");
        compileSubroutineBody();
        closeTag("subroutineDec");
        units.pop();
    }

    private void compileParameterList() {

        units.push("parameter list");
        openTag("parameterList");

        if (!(currentValue().equals(")"))) {
            compileType();
            eat("identifier");
            while(currentValue().equals(",")) {
                eat("symbol",",");
                compileType();
                eat("identifier"); }
        }
        closeTag("parameterList");
        units.pop();
    }

    private void compileSubroutineBody() {
        units.push("subroutine body");

        openTag("subroutineBody");
        eat("symbol","{");
        while (currentValue().equals("var")) {
            compileVarDec();
        }
        compileStatements();
        eat("symbol","}");
        closeTag("subroutineBody");

        units.pop();
    }

    private void compileVarDec() {
        units.push("variable declaration");
        openTag("varDec");
        eat("keyword","var");
        compileType();
        eat("identifier");
        while(currentValue().equals(",")) {
            eat("symbol",",");
            eat("identifier"); }
        eat("symbol",";");
        closeTag("varDec");
        units.pop();
    }

    private void compileStatements() {
        units.push("statements");
        openTag("statements");
        while(statementTypes.contains(currentValue())) {
            compileStatement();
        }
        closeTag("statements");
        units.pop();
    }

    private void compileStatement() {
        switch (currentValue()) {
            case "let":
                compileLetStatement();
                break;
            case "if":
                compileIfStatement();
                break;
            case "while":
                compileWhileStatment();
                break;
            case "do":
                compileDoStatement();
                break;
            case "return":
                compileReturnStatement();
                break;
            default:
                throw new RuntimeException("Expected statement keyword, given " + currentType() + " " + currentValue());
        }
    }

    private void compileLetStatement() {
        units.push("let statement");

        openTag("letStatement");
        eat("keyword","let");
        eat("identifier");
        if (currentValue().equals("[")) {
            eat("symbol","[");
            compileExpression();
            eat("symbol","]");
        }
        eat("symbol","=");
        compileExpression();
        eat("symbol",";");
        closeTag("letStatement");

        units.pop();
    }

    private void compileIfStatement() {
        units.push("if statement");
        openTag("ifStatement");
        eat("keyword","if");
        eat("symbol","(");
        compileExpression();
        eat("symbol",")");
        eat("symbol","{");
        compileStatements();
        eat("symbol","}");
        if (currentValue().equals("else")) {
            eat("keyword","else");
            eat("symbol","{");
            compileStatements();
            eat("symbol","}");
        }
        closeTag("ifStatement");
        units.pop();
    }

    private void compileWhileStatment() {
        units.push("while statement");
        openTag("whileStatement");
        eat("keyword","while");
        eat("symbol","(");
        compileExpression();
        eat("symbol",")");
        eat("symbol","{");
        compileStatements();
        eat("symbol","}");
        closeTag("whileStatement");
        units.pop();
    }

    private void compileDoStatement() {
        units.push("doStatement");
        openTag("doStatement");
        eat("keyword","do");
        compileSubroutineCall();
        eat("symbol",";");
        closeTag("doStatement");
        units.pop();
    }

    private void compileReturnStatement() {
        units.push("return statement");
        openTag("returnStatement");
        eat("keyword","return");
        if (!currentValue().equals(";")) {
            compileExpression();
        }
        eat("symbol",";");
        closeTag("returnStatement");
        units.pop();
    }

    private void compileSubroutineCall() {
        units.push("subroutine call");
//        openTag("subroutineCall");
        eat("identifier");
        if (currentValue().equals(".")) {
            eat("symbol",".");
            eat("identifier");
        }
        eat("symbol","(");
        compileExpressionList();
        eat("symbol",")");
//        closeTag("subroutineCall");
        units.pop();
    }

    private void compileExpressionList() {

        units.push("expression list");
        openTag("expressionList");
        if (!(currentValue().equals(")"))) {
            compileExpression();
            while (currentValue().equals(",")) {
                eat("symbol",",");
                compileExpression();
            }
        }
        closeTag("expressionList");
        units.pop();
    }

    private void compileExpression() {
        units.push("expression");
        openTag("expression");
        compileTerm();
        while (operators.contains(currentValue())) {
            eat("symbol");
            compileTerm();
        }
        closeTag("expression");
        units.pop();
    }

    private void compileTerm() {
        units.push("term");
        openTag("term");

        if (unaryOperators.contains(currentValue())) { // Unary operator
            eat("symbol");
            compileTerm();
        } else if (currentType().equals("integerConstant")) { // Integer
            eat("integerConstant");
        } else if (currentType().equals("stringConstant")) { // String
            eat("stringConstant");
        } else if (keywordConstants.contains(currentValue())) { // Keyword Constant (true false null this)
            eat("keyword");
        } else if (currentValue().equals("(")) { // Expression in parens
            eat("symbol","(");
            compileExpression();
            eat("symbol",")");
        } else if (nextValue().equals("[")) { // Array call
            eat("identifier");
            eat("symbol","[");
            compileExpression();
            eat("symbol","]");
        } else if (nextValue().equals(".") || nextValue().equals("(")) { // Subroutine call
            compileSubroutineCall();
        } else { // Just a variable name, no other stuff
            eat("identifier");
        }

        units.pop();
        closeTag("term");
    }

    private void compileType() {
        units.push("type");
        if (types.contains(currentValue())) { eat("keyword"); }
        else { eat("identifier"); }
        units.pop();
    }

    private void eat(String aType) { eat(aType,null); }

    private void eat(String aType, String aValue) {
        if (current > tokens.length-1) {
            throw new RuntimeException("No more tokens, expected " + aType + " " + aValue + " while compiling "
                    + units.peek()+ " ("+fileName+" Token #" + current + ")");
        }
        if (!(aValue == null) && !(currentValue().equals(aValue))) {
            throw new RuntimeException("Expected " +aType+ " " + aValue + ", got " + currentType() + " " + currentValue()+ " while compiling "
                    + units.peek() + " ("+fileName+" Token #" + current + ")");
        }
        if (!currentType().equals(aType)) {
            throw new RuntimeException("Expected type " + aType + ", got type " + currentType()+ " (" + currentValue() + ") while compiling "
                    + units.peek()+ " ("+fileName+" Token #" + current + ")");
        } else {
            String val = currentValue();
            if (currentValue().equals("<")) {
                val = "&lt;";
            } else if (currentValue().equals(">")) {
                val = "&gt;";
            } else if (currentValue().equals("&")) {
                val = "&amp;";
            } else if (currentType().equals("stringConstant")) {
                val = val.substring(1,val.length()-1);
            }
            singleTag(currentType(),val);
            current++;
        }
    }

    private void openTag(String tagName) {
        try {
            for (int i=0;i<indents;i++) {
                writer.write("\t");
            }
            writer.write("<"+tagName+">\n");
            indents++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeTag(String tagName) {
        try {
            indents--;
            for (int i=0;i<indents;i++) {
                writer.write("\t");
            }
            writer.write("</"+tagName+">\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void singleTag(String tagName, String contents) {
        try {
            for (int i=0; i<indents;i++) {
                writer.write("\t");
            }
            writer.write("<"+tagName+">"+contents+"</"+tagName+">\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String currentType() {
        return tokens[current].getType();
    }

    private String currentValue() {
        return tokens[current].getValue();
    }

    private String nextType() {
        return tokens[current+1].getType();
    }

    private String nextValue() {
        return tokens[current+1].getValue();
    }

}
