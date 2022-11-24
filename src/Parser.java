import java.io.*;
import java.util.*;

public class Parser {

    private Token[] tokens;
    private int current;
    private BufferedWriter XMLWriter;
    private BufferedWriter CodeWriter;
    private String fileName;

    private Stack<String> units;
    private int indents;
    private String outputDir;

    private List<String> varDecs = Arrays.asList("static","field");
    private List<String> subroutineDecs = Arrays.asList("constructor","function","method");
    private List<String> types = Arrays.asList("int","char","boolean");
    private List<String> statementTypes = Arrays.asList("let","if","while","do","return");
    private List<String> operators = Arrays.asList("+","-","*","/","&","|","<",">","=");
    private List<String> unaryOperators = Arrays.asList("-","~");
    private List<String> keywordConstants = Arrays.asList("true","false","null","this");

    private HashMap<String,JackVariable> classVariables;
    private HashMap<String,JackVariable> subroutineVariables;

    private int staticPos;
    private int fieldPos;
    private int argumentPos;
    private int localPos;

    private int numArgs;

    private String className;


    public Parser(String fileName, ArrayList<Token> tokens) {
        this.fileName = fileName;
        this.tokens = new Token[tokens.size()];
        this.tokens = tokens.toArray(this.tokens);
        current = 0;
        indents = 0;

        units = new Stack<>();
        classVariables = new HashMap<>();
        subroutineVariables = new HashMap<>();

        // Position assignments
        staticPos = 0;
        fieldPos = 0;
        argumentPos = 0;
        localPos = 0;
    }

    public void generateCode() {
        try {
            // Initiate class-wide writing variable
            XMLWriter = new BufferedWriter(new FileWriter(Main.XML_DIR+"/"+fileName+".xml"));
            CodeWriter = new BufferedWriter(new FileWriter(Main.VM_DIR+"/"+fileName+".vm"));
            compileClass();
            XMLWriter.close();
            CodeWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void compileClass() {

        units.push("class");

        openTag("class");
        eat("keyword","class");
        className = currentValue();
        eat("identifier");
        eat("symbol","{");
        while(varDecs.contains(currentValue())) { compileClassVarDec(); }
        while(subroutineDecs.contains(currentValue())) { compileSubroutineDec(); }
        eat("symbol","}");
        closeTag("class");

        units.pop();
    }

    private void compileClassVarDec() {

        String name;
        String kind;
        String type;

        units.push("class variable declaration");

        openTag("classVarDec");

        // Deal with keyword (variable kind)
        kind = currentValue();
        eat("keyword");

        type = currentValue();
        compileType();

        name = currentValue();
        eat("identifier");

        classVariables.put(name,new JackVariable(kind,type,getPos(kind)));

        while(currentValue().equals(",")) {
            eat("symbol",",");
            name = currentValue();
            eat("identifier");
            classVariables.put(name,new JackVariable(kind,type,getPos(kind)));}
        eat("symbol",";");
        closeTag("classVarDec");
        units.pop();
    }

    private void compileSubroutineDec() {
        units.push("subroutine declaration");
        openTag("subroutineDec");

        String subroutineType = currentValue();

        eat("keyword"); // type of subroutine
        boolean isVoid = false;
        if (currentValue().equals("void")) {
            isVoid = true;
            eat("keyword");
        } else {
            compileType();
        }
        String subroutineName = currentValue();
        eat("identifier"); // subroutine name
        eat("symbol","(");

        if (subroutineType.equals("method")) {
            // If method, assume argument 0 is this.
            addLine("push argument 0");
            addLine("pop pointer 0");
        }

        compileParameterList();
        eat("symbol",")");
        compileSubroutineBody(subroutineName); // also handles adding function declaration
        closeTag("subroutineDec");
        units.pop();



        // TODO: Check that clearing is proper
        // TODO: And also check that resetting positions of arguments and pos is valid
        subroutineVariables.clear();
        argumentPos = 0;
        localPos = 0;

        // TODO: Resolve isVoid functionality.
        // Calls to void methods should pop to temp after returning.

    }

    private void compileParameterList() {

        units.push("parameter list");
        openTag("parameterList");

        String varName;
        String varType;

        if (!(currentValue().equals(")"))) {
            varType = currentValue();
            compileType(); // eats type
            varName = currentValue();
            eat("identifier");
            // Within a subroutine, we assign names to each argument
            // The assumption being that they are already mapped
            subroutineVariables.put(varName, new JackVariable("argument", varType, getPos("argument")));
            while(currentValue().equals(",")) {
                eat("symbol",",");
                varType = currentValue();
                compileType();
                varName = currentValue();
                eat("identifier");
                subroutineVariables.put(varName, new JackVariable("argument", varType, getPos("argument")));
            }
        }
        closeTag("parameterList");
        units.pop();
    }

    private void compileSubroutineBody(String subroutineName) {
        units.push("subroutine body");

        int localVars = 0;

        openTag("subroutineBody");
        eat("symbol","{");
        while (currentValue().equals("var")) {
            localVars++;
            compileVarDec();
        }

        // TODO: Figure out how to calculate local variable number
        addLine("function " + className + "." + subroutineName + " " + localVars );
        compileStatements();
        eat("symbol","}");
        closeTag("subroutineBody");

        units.pop();
    }

    private void compileVarDec() {
        units.push("variable declaration");
        openTag("varDec");
        eat("keyword","var");
        String type = currentValue();
        compileType();
        String name = currentValue();
        eat("identifier");
        subroutineVariables.put(name,new JackVariable("var",type,getPos("var")));
        while(currentValue().equals(",")) {
            eat("symbol",",");
            name = currentValue();
            eat("identifier");
            subroutineVariables.put(name,new JackVariable("var",type,getPos("var")));}
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
        String varName = currentValue();
        boolean assignToArray = false;

        eat("identifier");
        if (currentValue().equals("[")) {
            assignToArray = true;
            addLine("push " + getVariableData(varName)); // push base address
            eat("symbol","[");
            compileExpression(); //evaluate to an index
            eat("symbol","]");
            addLine("add"); // correct address stays on the stack
        }
        eat("symbol","=");
        compileExpression();
        // After we compile the expression, we expect its result to be on the stack
        // So we pop it to the variable according to its segment
        if (assignToArray) {
            // If it's an array, we instead need to store to the array's location
            addLine("pop temp 0"); // so store our result in a temp
            addLine("pop pointer 1"); // move our stored address to THIS so this points to the array index
            addLine("push temp 0"); // reload our stored value
            addLine("pop that 0"); // and store it to the array
        } else {
            // Otherwise just store the value to wherever it is
            addLine("pop " + getVariableData(varName));
        }

        eat("symbol",";");
        closeTag("letStatement");

        units.pop();
    }

    private void compileIfStatement() {
        // TODO deal with if statement
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
        // TODO deal with while statement
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
        // TODO deal with do statement
        units.push("doStatement");
        openTag("doStatement");
        eat("keyword","do");
        compileSubroutineCall();
        eat("symbol",";");
        closeTag("doStatement");
        units.pop();
    }

    private void compileReturnStatement() {
        // TODO deal with return statement
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
        // There are three subroutine call types
        // Constructors, Methods, and Functions.
        // Note that methods don't need a dot if they're internal


        units.push("subroutine call");

        String classOrObject = currentValue();
        String subroutineName;
        int numArgs = 0;

        eat("identifier");
        if (currentValue().equals(".")) {
            // Process
            eat("symbol",".");
            // function or method is the second half
            subroutineName = currentValue();
            eat("identifier");

            if (variableExists(classOrObject)) {
                // Case: object exists as variable
                // push that object as an argument
                addLine("push " + getVariableData(classOrObject));
                numArgs++;
            } else {
                // Case: object DNE, must be a class
                // do not push!
            }
        } else {
            // Internal method - rename accordingly
            subroutineName = classOrObject; // method name is actually the first part
            classOrObject = className; // and its class is the class name
            // Then push the "THIS" value onto the stack, to be the first argument
            addLine("push pointer 0");
            numArgs++;
        }
        eat("symbol","(");

        // Expression list compilation will push all relevant arguments onto the stack
        // Could just do some strange global variable for each call, no?
        // TODO: Determine number of arguments needed
        // TODO: Resolve argument addition order
        compileExpressionList();

        // Finally, call the function with its number of args
        addLine("call " + classOrObject + "." + subroutineName + " " + numArgs);


        eat("symbol",")");
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
        // Should be good.
        units.push("term");
        openTag("term");

        if (unaryOperators.contains(currentValue())) { // Unary operator
            String op = currentValue();
            eat("symbol");
            compileTerm(); // will put the result on top of stack
            if (op.equals("-")) {
                addLine("neg"); // call neg
            } else if (op.equals("~")) {
                addLine("not"); // call not
            } else {
                throw new RuntimeException("Unrecognized unary operator: " + op);
            }

        } else if (currentType().equals("integerConstant")) { // Integer
            // For integers just push a constant
            addLine("push constant " + currentValue());
            eat("integerConstant");


        } else if (currentType().equals("stringConstant")) { // String
            // First let's get the String into our java format. substring to trim extra quotes.
            String stringContent = currentValue().substring(1,currentValue().length()-1);
            // Let's call the OS to make a new String object
            addLine("push constant " + stringContent.length());
            addLine("call String.new 1"); // call with length
            // Constructor pushes String's base address on the stack
            for (int i=0;i<stringContent.length();i++) {
                // Pushes unicode # of character onto stack
                addLine("push constant " + Character.getNumericValue(stringContent.charAt(i)));
                // Call appendChar function with two arguments: String as arg0 and unicode as arg1
                addLine("call String.appendChar 2");
            }
            eat("stringConstant");
            // I think strings are actually good to go.

        } else if (keywordConstants.contains(currentValue())) { // Keyword Constant (true false null this)
            String keywordConstant = currentValue();
            eat("keyword");
            switch (keywordConstant) {
                case "true":
                    addLine("push constant 1");
                    addLine("neg"); // negative one
                    break;
                case "false":
                case "null":
                    addLine("push constant 0"); // both false and null are 0
                    break;
                case "this":
                    addLine("push pointer 0"); // just push the address of the current object
                    break;
                default:
                    throw new RuntimeException("Not keyword constant: " + keywordConstant);
            }

        } else if (currentValue().equals("(")) { // Expression in parens
            eat("symbol","(");
            // If in parens just do nothing. recursion will handle it
            compileExpression();
            eat("symbol",")");

        } else if (nextValue().equals("[")) { // Array call
            // Array calls should be handled now; they resolve to the correct location
            String varName = currentValue(); // array name, reference to a base address
            addLine("push " + getVariableData(varName)); // adds base address to stack
            eat("identifier");
            eat("symbol","[");
            compileExpression(); // inner expression should become an index, which we push on stack
            eat("symbol","]");
            addLine("add"); // add for an address. next we need to push the actual value...
            addLine("pop pointer 1");
            addLine("push that 0");

        } else if (nextValue().equals(".") || nextValue().equals("(")) { // Subroutine call
            // Method handles everything and pushes what we need on the stack
            compileSubroutineCall();

        } else { // Just a variable name, no other stuff
            String varName = currentValue();
            eat("identifier");
            addLine("push " + getVariableData(varName));
        }

        units.pop();
        closeTag("term");
    }

    private void compileType() {
        // Just checks for type, shouldn't need compilation.
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
                XMLWriter.write("\t");
            }
            XMLWriter.write("<"+tagName+">\n");
            indents++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeTag(String tagName) {
        try {
            indents--;
            for (int i=0;i<indents;i++) {
                XMLWriter.write("\t");
            }
            XMLWriter.write("</"+tagName+">\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void singleTag(String tagName, String contents) {
        try {
            for (int i=0; i<indents;i++) {
                XMLWriter.write("\t");
            }
            XMLWriter.write("<"+tagName+">"+contents+"</"+tagName+">\n");
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

    private int getPos(String kind) {
        switch(kind) {
            case "static":
                return getStaticPos();
            case "field":
                return getFieldPos();
            case "argument":
                return getArgumentPos();
            case "local":
                return getLocalPos();
            default:
                throw new RuntimeException("Unrecognized variable kind " + kind + " in " + units.peek());
        }
    }

    public int getStaticPos() {
        int ret = staticPos;
        staticPos++;
        return ret;
    }

    public int getFieldPos() {
        int ret = fieldPos;
        fieldPos++;
        return ret;
    }

    public int getArgumentPos() {
        int ret = argumentPos;
        argumentPos++;
        return ret;
    }

    public int getLocalPos() {
        int ret = localPos;
        localPos++;
        return ret;
    }

    public String getVariableData(String name) {
        if (subroutineVariables.containsKey(name)) {
            JackVariable var = subroutineVariables.get(name);
            if (var.getKind().equals("field")) { // For field variables we access ith position in current object
                return "this " + var.getPos();
            }
            return var.getKind() + " " + var.getPos();
        } else if (classVariables.containsKey(name)) {
            JackVariable var = classVariables.get(name);
            return var.getKind() + " " + var.getPos();
        } else throw new RuntimeException("Variable " + name + "not found in symbol tables (has it been initialized?)");
    }

    public boolean variableExists(String name) {
        return (subroutineVariables.containsKey(name) || classVariables.containsKey(name));
    }

    public String getVMCommand(String operator) {
        switch (operator) {
            case ("+"):
                return "add";
            default:
                throw new RuntimeException("Unrecognized operator " + operator);
        }
    }

    private void addLine(String line) {
        try {
            CodeWriter.write(line+"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
