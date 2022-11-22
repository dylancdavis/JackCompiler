import java.io.*;

public class CodeGenerator {

    private String fileName;

    public CodeGenerator(String fileName) {
        this.fileName = fileName;
    }

    public void generateVirtualCode() {

        try {
            BufferedReader reader = new BufferedReader(new FileReader(Main.XML_DIR+"/"+fileName+".xml"));
            BufferedWriter writer = new BufferedWriter(new FileWriter(Main.VM_DIR+"/"+fileName+".vm"));
            System.out.println("Writing" + fileName + " to vm code.");

            writer.write("Testing text for file " + fileName);

            // TODO Generate code for each element encountered

            reader.close();
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
