package parser;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {


    private final static String classDefinitionRegex = "(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*\\s*?(<.*>)?(\\s+extends\\s+[a-zA-Z][a-zA-Z0-9]*)?(\\s+implements\\s+[a-zA-Z][a-zA-Z0-9]*(,\\s*[a-zA-Z][a-zA-Z0-9]*)?)?\\s*{[\\S\\s]*}";
    private final static String COMMENTS = "(\\/\\/.*)|\\/\\*(?:.|[\\n\\r])*?\\*\\/";
    private final static String IMPORTS = "import\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;";

    public static void main(String[] args){
        new Parser().extractImports("import test.data;\n" +
                "\n" +
                "\n" +
                "import mama.dat.if.Lak;");
    }


    public String removeComments(String file){
        Pattern p = Pattern.compile(COMMENTS);
        Matcher m = p.matcher(file);
        return m.replaceAll("");
    }

    public void extractImports(String file){
        Pattern p = Pattern.compile(IMPORTS);
        Matcher m = p.matcher(file);
        while(m.find()){
            System.out.println(m.group());
        }
    }



}
