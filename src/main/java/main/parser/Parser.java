package main.parser;


import main.exceptions.NotAClassException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

//    private final static Pattern COMMENTS = Pattern.compile("(\\/\\/.*)|(\\/\\*(?:.|[\\n\\r])*?\\*\\/)");
    private final static Pattern COMMENTS = Pattern.compile("(\\/\\/.*)|(\\/\\*[\\s\\S]*?\\*\\/)");
    private final static Pattern CLASS_DEFINITION = Pattern.compile("(class|enum|interface)\\s+[a-zA-Z][a-zA-Z0-9]*(\\s*<([a-zA-Z][a-zA-Z0-9]*)(,\\s*([a-zA-Z][a-zA-Z0-9]*))*>)?");
    private final static Pattern CLASS_GENERIC_DEFINITION = Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)(,\\s*([a-zA-Z][a-zA-Z0-9]*))*>");

    private final static Pattern PACKAGE_DEFINITION = Pattern.compile("package\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*;");
    private final static Pattern IMPORT_DEFINITION = Pattern.compile("import\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(\\.\\*)?;");

    private final static Pattern FIELD_DECLARATION = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*((\\[\\])|(<.*?>))?\\s+[a-zA-Z][a-zA-Z0-9]*\\s*(;|=)");

    private final static Pattern STRING_DECLARATION = Pattern.compile("([\"'])(?:(?=(\\\\?))\\2([\\S\\s]))*?\\1");

    private final static Pattern EXTENDS_DEFINITION = Pattern.compile("extends\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(<.*>)?");
    private final static Pattern IMPLEMENTS_DEFINITION = Pattern.compile("implements\\s+[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(,\\s*[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*)*(<.*>)?");

    private final static Pattern EXTENDS_OR_IMPLEMENTS_DEFINITION = Pattern.compile("(extends|implements)\\s+([a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*\\s*(,\\s*)?)+");
    private final static Pattern HOOK_BRACKETS = Pattern.compile("[<>]");


    private final static Pattern BODY = Pattern.compile("\\{[\\s\\S]*\\}");
    private final static Pattern IDENTIFIER_NODOTS = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");
    private final static Pattern IDENTIFIER_MAYDOTS_MAYSTAR = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*(\\.\\*)?");
    private final static Pattern IDENTIFIER_MAYDOTS = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*");


    private final static Set<String> primitives = new HashSet<>(Arrays.asList("float", "double", "int", "boolean", "char", "long", "shot", "byte"));


    private Set<String> dependencies;
    /** dependencies either defined using path or can be derived from the imports **/
    private Set<String> knownDependencies;
    /** dependencies inside same package or used libraries **/
    private Set<String> unknownExternalDependencies;
    /**  specified classname. Example: Example **/
    private String className;
    /** location of the class **/
    private String packageName;

    private Set<String> genericTypes;
    private Set<String> imports;
    private Set<String> starImports;


    public static void main(String[] args) throws NotAClassException {

    }

    public Parser(String file) throws NotAClassException {

            dependencies = new HashSet<>();
            knownDependencies = new HashSet<>();
//        unknownDependencies = new HashSet<>();
            genericTypes = new HashSet<>();
            imports = new HashSet<>();
            unknownExternalDependencies = new HashSet<>();
            starImports = new HashSet<>();
        try {
            String noStringContents = removeStringContents(file);
            String noCommentsNoStringContents = removeComments(noStringContents);

            extractPackageDeclaration(noCommentsNoStringContents);
            extractImports(noCommentsNoStringContents);
            extractClassInheritance(noCommentsNoStringContents);

            Matcher bodyMatcher = BODY.matcher(noCommentsNoStringContents);
            if (bodyMatcher.find()) {
                extractDeclarations(bodyMatcher.group());
            }
            dependencies.removeAll(genericTypes);
            identifyKnownDependencies();
        }catch (StackOverflowError e){
            System.out.println(file);
            throw new IllegalStateException("Stack overflow");
        }
//        System.out.println(this);
    }

    public static String getName(String file) throws NotAClassException {
        String className = "";
        String packageName = "";
        Matcher m = CLASS_DEFINITION.matcher(file);
        if(m.find()) {
            String result = m.group();
            Matcher classNameMatcher = IDENTIFIER_NODOTS.matcher(result);

            for (int i = 0; i < 2 && classNameMatcher.find(); i++) ;

            className = classNameMatcher.group(0);

        }
        if(className == null){
            throw new NotAClassException();
        }

        Matcher m2 = PACKAGE_DEFINITION.matcher(file);
        if(m2.find()) {
            Matcher packageIdentifierMatcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(m2.group());

            for (int i = 0; i < 2 && packageIdentifierMatcher.find(); i++) ;
            packageName = packageIdentifierMatcher.group();
        }
        return packageName + "." + className;
    }

    private void identifyKnownDependencies() {
        for(String s : dependencies){
            if(s.contains(".")){
                knownDependencies.add(s);
            }
        }
        for(String s : imports){
            String[] array = s.split("\\.");
            if(dependencies.contains(array[array.length-1])){
                knownDependencies.add(s);
                dependencies.remove(array[array.length-1]);
            }
        }
    }


    public String removeComments(String file){
        Matcher m = COMMENTS.matcher(file);
        return m.replaceAll("");
    }

    public String removeStringContents(String file){
        Matcher m = STRING_DECLARATION.matcher(file);
        return m.replaceAll("\"\"");
    }

    //TODO implement nested classes and multiple classes in a single file
    public void extractClassInheritance(String file) throws NotAClassException {
        Matcher m = CLASS_DEFINITION.matcher(file);
        if(m.find()){
            String result = m.group();
            Matcher classNameMatcher = IDENTIFIER_NODOTS.matcher(result);

            for(int i=0; i<2 && classNameMatcher.find(); i++);

            className = classNameMatcher.group(0);


            Matcher genericMatcher = CLASS_GENERIC_DEFINITION.matcher(result);
            if(genericMatcher.find()){
                String generics = genericMatcher.group();
                Matcher genericIdentifiers = IDENTIFIER_NODOTS.matcher(generics);
                while (genericIdentifiers.find()){
                    genericTypes.add(genericIdentifiers.group());
                }
            }

            //retrieve inheritance
            //replace all < > with whitespace
            Matcher matcher = HOOK_BRACKETS.matcher(file);
            String noHookBrackets = matcher.replaceAll(" ");

            Matcher inheritanceMatcher = EXTENDS_OR_IMPLEMENTS_DEFINITION.matcher(noHookBrackets);
            if(inheritanceMatcher.find()){
                String extendsOrImplements = inheritanceMatcher.group();
                Matcher identifiers = IDENTIFIER_MAYDOTS.matcher(extendsOrImplements);
                while(identifiers.find()){
                    String r = identifiers.group();
                    if(!(r.equals("extends")||r.equals("implements"))){
                        dependencies.add(r);
                    }
                }
            }
        }
        if(className == null){
            throw new NotAClassException();
        }
    }

    public void extractPackageDeclaration(String file){
        Matcher m = PACKAGE_DEFINITION.matcher(file);
        if(m.find()){
            Matcher packageIdentifierMatcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(m.group());

            for(int i=0; i<2 && packageIdentifierMatcher.find(); i++);
            packageName = packageIdentifierMatcher.group();
        }else{
//            System.err.println("No package defined");
        }
    }

    public void extractImports(String file){
        Matcher m = IMPORT_DEFINITION.matcher(file);
        List<String> entries = new ArrayList<>();
        while(m.find()){
            entries.add(m.group());
        }

        for(String entry : entries){
            Matcher matcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(entry);
            matcher.find();
            if(matcher.find()){
                String imp = matcher.group();
                if(imp.contains("*")){
                    starImports.add(imp.substring(0, imp.length()-1));
                }else{
                    imports.add(imp);
                }
            }
        }
    }

    public void extractDeclarations(String file){
        Matcher fieldsDecMatcher = FIELD_DECLARATION.matcher(file);
        while (fieldsDecMatcher.find()){
            String declaration = fieldsDecMatcher.group();
            //all but last identifiers are dependencies
            Matcher identifiers = IDENTIFIER_MAYDOTS.matcher(declaration);
            String prev = "";
            if(identifiers.find()){
                prev = identifiers.group();
            }
            while (identifiers.find()){
//                System.err.println(prev);
                if(!primitives.contains(prev)&&!prev.equals("return")&&!prev.equals("break")) {
                    dependencies.add(prev);
                }
                prev = identifiers.group();
            }
        }
    }

    public String getName(){
        return packageName+"."+className;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getKnownDependencies() {
        return knownDependencies;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getGenericTypes() {
        return genericTypes;
    }

    public Set<String> getImports() {
        return imports;
    }

    public void addUnknownExternalDependencies(String dependency){
        unknownExternalDependencies.add(dependency);
    }

    public Set<String> getUnknownExternalDependencies() {
        return unknownExternalDependencies;
    }

    public Set<String> getStarImports() {
        return starImports;
    }

    @Override
    public String toString() {
        return "Parser{" +
                "className='" + className + '\'' +
                ", dependencies=" + dependencies +
                ", knownDependencies=" + knownDependencies +
                ", unknownExternalDependencies=" + unknownExternalDependencies +
                ", packageName='" + packageName + '\'' +
                ", genericTypes=" + genericTypes +
                ", imports=" + imports +
                ", starImports=" + starImports +
                '}';
    }
}
