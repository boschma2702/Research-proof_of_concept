package parser;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {


//    private final static String classDefinitionRegex = "(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*\\s*?(<.*>)?(\\s+extends\\s+[a-zA-Z][a-zA-Z0-9]*)?(\\s+implements\\s+[a-zA-Z][a-zA-Z0-9]*(,\\s*[a-zA-Z][a-zA-Z0-9]*)?)?(\\s)*\\{[\\S\\s]*\\}";
    //private final static String COMMENTS = "(\\/\\/.*)|\\/\\*(?:.|[\\n\\r])*?\\*\\/";
//    private final static String IMPORTS = "import\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;";
//    private final static String PACKAGE_IDENTIFIER = "[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)+(\\.\\*)?";
//    private final static String PACKAGE_DECLARATION = "package\\s+[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)*;";
//    private final static String PACKAGE_LOCATION = "[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)*(\\.\\*)?";

//    private final static String BODY = "\\{[\\s\\S]*\\}";
//    private final static String IDENTIFIER = "[a-zA-Z][a-zA-Z0-9]+";


    private final static Pattern COMMENTS = Pattern.compile("(\\/\\/.*)|\\/\\*(?:.|[\\n\\r])*?\\*\\/");
//    private final Pattern CLASS_DEFINITION = Pattern.compile("(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*\\s*?(<.*>)?(\\s+extends\\s+[a-zA-Z][a-zA-Z0-9]*)?(\\s+implements\\s+[a-zA-Z][a-zA-Z0-9]*(,\\s*[a-zA-Z][a-zA-Z0-9]*)?)?(\\s)*\\{[\\S\\s]*\\}");
    private final static Pattern CLASS_DEFINITION = Pattern.compile("(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*\\s*?(<.*>)?(\\s+extends\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*)?(\\s+implements\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(,\\s*[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)*)*)?");
    private final static Pattern PACKAGE_DEFINITION = Pattern.compile("package\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;");
    private final static Pattern IMPORT_DEFINITION = Pattern.compile("import\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;");

    private final static Pattern EXTENDS_DEFINITION = Pattern.compile("extends\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*");
    private final static Pattern IMPLEMENTS_DEFINITION = Pattern.compile("implements\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(,\\s*[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*)*");



    private final static Pattern BODY = Pattern.compile("\\{[\\s\\S]*\\}");
    private final static Pattern IDENTIFIER_NODOTS = Pattern.compile("[a-zA-Z][a-zA-Z0-9]+");
    private final static Pattern IDENTIFIER_MAYDOTS_MAYSTAR = Pattern.compile("[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)*(\\.\\*)?");
    private final static Pattern IDENTIFIER_MAYDOTS = Pattern.compile("[a-zA-Z][a-zA-Z0-9]+(\\.[a-zA-Z][a-zA-Z0-9]*)*");




    private Set<String> dependencies;
    /** dependencies either defined using path or can be derived from the imports **/
    private Set<String> knownDependencies;
    /** dependencies inside same package or used libraries **/
    private Set<String> unknownDependencies;
    /** fully specified classname. Example: com.example.Example **/
    private String className;
    /** location of the class **/
    private String packageName;



    public static void main(String[] args){
        new Parser("public class Test extends Tester implements Tobetested, ditooknog");
    }

    public Parser(String file){
        dependencies = new HashSet<>();
        knownDependencies = new HashSet<>();
        unknownDependencies = new HashSet<>();
        String noComments = removeComments(file);
        extractClassInheritance(noComments);
        System.out.println(this);
    }


    public String removeComments(String file){
        Matcher m = COMMENTS.matcher(file);
        return m.replaceAll("");
    }

    //TODO implement nested classes and multiple classes in a single file
    public void extractClassInheritance(String file){
        Matcher m = CLASS_DEFINITION.matcher(file);
        if(m.find()){
            String result = m.group();
            Matcher classNameMatcher = IDENTIFIER_NODOTS.matcher(result);

            for(int i=0; i<2 && classNameMatcher.find(); i++);

            className = classNameMatcher.group(0);

            //match on extends
            Matcher matcher = EXTENDS_DEFINITION.matcher(result);
            if(matcher.find()){
                Matcher maydots = IDENTIFIER_MAYDOTS.matcher(matcher.group());
                //skip the string extends
                maydots.find();
                if(maydots.find()){
                    dependencies.add(maydots.group());
                }else{
                    System.err.println("Could match on extend");
                }
            }

            //match on implements
            matcher = IMPLEMENTS_DEFINITION.matcher(result);
            if(matcher.find()){
                Matcher maydots = IDENTIFIER_MAYDOTS.matcher(matcher.group());
                //skip the string implements
                maydots.find();
                while(maydots.find()){
                    dependencies.add(maydots.group());
                }
            }
        }
    }

    public void extractPackageDeclaration(String file){
        Matcher m = PACKAGE_DEFINITION.matcher(file);
        if(m.find()){
            Matcher packageIdentifierMatcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(m.group());

            for(int i=0; i<2 && packageIdentifierMatcher.find(); i++);
            packageName = packageIdentifierMatcher.group();
        }else{
            System.err.println("No package defined");
        }
    }

    public void extractImports(String file){
        Matcher m = IMPORT_DEFINITION.matcher(file);
        List<String> entries = new ArrayList<>();
        while(m.find()){
            entries.add(m.group());
        }
        Set<String> imports = new HashSet<>();
        for(String entry : entries){
            Matcher matcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(entry);
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

    @Override
    public String toString() {
        return "Parser{" +
                "dependencies=" + dependencies +
                ", knownDependencies=" + knownDependencies +
                ", unknownDependencies=" + unknownDependencies +
                ", className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
