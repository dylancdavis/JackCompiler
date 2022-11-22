import java.io.File;

public class Main {

    static final String FILE_NAME = "Main";

    static final String JACK_DIR = "JackFiles";
    static final String XML_DIR = "XMLOutput";
    static final String VM_DIR = "VMOutput";



    public static void main(String[] args) {

        File dir = new File(JACK_DIR);
        File[] files = dir.listFiles();

        if (files.length == 0) {
            throw new RuntimeException("No files ending in .jack in directory " + JACK_DIR);
        }

        for(File f: files) {
            String fileName = f.getName();
            String extension = fileName.substring(fileName.indexOf('.'));
            if (f.isFile() && extension.equals(".jack")) {
                Compiler compiler = new Compiler(fileName.substring(0,fileName.indexOf('.')));
                compiler.compile();

            }
        }
    }
}