package parser;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {


    private final static String classDefinitionRegex = "(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*\\s*?(<.*>)?(\\s+extends\\s+[a-zA-Z][a-zA-Z0-9]*)?(\\s+implements\\s+[a-zA-Z][a-zA-Z0-9]*(,\\s*[a-zA-Z][a-zA-Z0-9]*)?)?(\\s)*\\{[\\S\\s]*\\}";
    private final static String COMMENTS = "(\\/\\/.*)|\\/\\*(?:.|[\\n\\r])*?\\*\\/";
    private final static String IMPORTS = "import\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;";
    private final static String PACKAGE_IDENTIFIER = "[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)+(\\.\\*)?";
    private final static String BODY = "\\{[\\s\\S]*\\}";
    private final static String IDENTIFIER = "[a-zA-Z][a-zA-Z0-9]+";

    /** dependencies either defined using path or can be derived from the imports **/
    private Set<String> knownDependencies;
    /** dependencies inside same package or used libraries **/
    private Set<String> unknownDependencies;
    /** fully specified classname. Example: com.example.Example **/
    private String className;



    public static void main(String[] args){
        new Parser().extractClassInheritance("public class Parser {test}");
    }


    public String removeComments(String file){
        Pattern p = Pattern.compile(COMMENTS);
        Matcher m = p.matcher(file);
        return m.replaceAll("");
    }

    //TODO implement nested classes and multiple classes in a single file
    public void extractClassInheritance(String file){
        Pattern p = Pattern.compile(classDefinitionRegex);
        Matcher m = p.matcher(file);
        if(m.find()){
            String result = m.group();
            Pattern classNamePattern = Pattern.compile(IDENTIFIER);
            System.out.println(result);
            Matcher classNameMatcher = classNamePattern.matcher(result);

            for(int i=0; i<2 && classNameMatcher.find(); i++){}

            className = classNameMatcher.group(0);
            System.out.println(className);


            //System.out.println(m.group());
        }
    }

    public void extractImports(String file){
        Pattern p = Pattern.compile(IMPORTS);
        Matcher m = p.matcher(file);
        List<String> entries = new ArrayList<>();
        while(m.find()){
            entries.add(m.group());
        }
        Pattern ids = Pattern.compile(PACKAGE_IDENTIFIER);
        Set<String> imports = new HashSet<>();
        for(String entry : entries){
            Matcher matcher = ids.matcher(entry);
            if(matcher.find()){
                imports.add(matcher.group());
            }
        }
        System.out.println(imports);
    }

    public void extractDeclarations(){
        //TODO remove string declaratoin \" ... \"
        //TODO match id id; or id id =
        //TODO don't forget special types arrays and generics
    }



}
