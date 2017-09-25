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

    private final static Pattern STRING_DECLARATION = Pattern.compile("([\"])(?:(?=(\\\\?))\\2([\\S\\s]))*?\\1");

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
    /**
     * dependencies either defined using path or can be derived from the imports
     **/
    private Set<String> knownDependencies;
    /**
     * dependencies inside same package or used libraries
     **/
    private Set<String> unknownExternalDependencies;
    /**
     * specified classname. Example: Example
     **/
    private String className;
    /**
     * location of the class
     **/
    private String packageName;

    private Set<String> genericTypes;
    private Set<String> imports;
    private Set<String> starImports;

    /**
     * contains the contents of the representing file
     **/
    private String contents;


    public static void main(String[] args) throws NotAClassException {
        Parser p = new Parser("package accounts;\n" +
                "\n" +
                "import java.util.HashSet;\n" +
                "import java.util.Set;\n" +
                "\n" +
                "import javax.persistence.CascadeType;\n" +
                "import javax.persistence.Column;\n" +
                "import javax.persistence.Entity;\n" +
                "import javax.persistence.JoinColumn;\n" +
                "import javax.persistence.FetchType;\n" +
                "import javax.persistence.Id;\n" +
                "import javax.persistence.JoinTable;\n" +
                "import javax.persistence.ManyToMany;\n" +
                "import javax.persistence.Table;\n" +
                "import javax.persistence.Transient;\n" +
                "\n" +
                "import database.DataManager;\n" +
                "\n" +
                "/**\n" +
                " * A bank customer's main account, to which multiple <code>BankAccounts</code> may be tied.\n" +
                " * @author Andrei Cojocaru\n" +
                " */\n" +
                "@Entity\n" +
                "@Table(name = \"customeraccounts\")\n" +
                "public class CustomerAccount implements database.DBObject {\n" +
                "\tprivate String name;\n" +
                "\tprivate String surname;\n" +
                "\tprivate String BSN;\n" +
                "\tprivate String streetAddress;\n" +
                "\tprivate String phoneNumber;\n" +
                "\tprivate String email;\n" +
                "\tprivate String birthdate;\n" +
                "\tprivate Set<BankAccount> bankAccounts = new HashSet<BankAccount>();\n" +
                "\tpublic static final String CLASSNAME = \"accounts.CustomerAccount\";\n" +
                "\tpublic static final String PRIMARYKEYNAME = \"BSN\";\n" +
                "\t\n" +
                "\t/**\n" +
                "\t * Create a new <code>CustomerAccount</code> with the given customer information.\n" +
                "\t * @param name The customer's name\n" +
                "\t * @param surname The customer's surname\n" +
                "\t * @param BSN The customer's BSN\n" +
                "\t * @param streetAddress The customer's street address\n" +
                "\t * @param phoneNumber The customer's phone number\n" +
                "\t * @param email The customer's email\n" +
                "\t * @param birthdate The customer's date of birth\n" +
                "\t * @param addToDB Whether or not to add the newly-created customer account to the database\n" +
                "\t */\n" +
                "\tpublic CustomerAccount(String name, String surname, String BSN, String streetAddress, String phoneNumber, \n" +
                "\t\t\tString email, String birthdate) {\n" +
                "\t\tthis.setName(name);\n" +
                "\t\tthis.setSurname(surname);\n" +
                "\t\tthis.setBSN(BSN);\n" +
                "\t\tthis.setStreetAddress(streetAddress);\n" +
                "\t\tthis.setPhoneNumber(phoneNumber);\n" +
                "\t\tthis.setEmail(email);\n" +
                "\t\tthis.setBirthdate(birthdate);\n" +
                "\t}\n" +
                "\t\n" +
                "\tpublic CustomerAccount() {\n" +
                "\t\t\n" +
                "\t}\n" +
                "\t\n" +
                "\t/**\n" +
                "\t * Open a new <code>BankAccount</code> in this holder's name.\n" +
                "\t * Adds this and the respective association to the database.\n" +
                "\t */\n" +
                "\tpublic void openBankAccount() {\n" +
                "\t\tBankAccount newAccount = new BankAccount(getBSN());\n" +
                "\t\taddBankAccount(newAccount);\n" +
                "\t}\n" +
                "\t\n" +
                "\t/**\n" +
                "\t * Adds the <code>CustomerAccount</code> as an owner to a pre-existing\n" +
                "\t * <code>BankAccount</code>. Also adds the appropriate association to the\n" +
                "\t * database.\n" +
                "\t * @param account The <code>BankAccount</code> to be owned by the customer.\n" +
                "\t */\n" +
                "\tpublic void addBankAccount(BankAccount account) {\n" +
                "\t\tbankAccounts.add(account);\n" +
                "\t\taccount.addOwner(this);\n" +
                "\t}\n" +
                "\t\n" +
                "\t/**\n" +
                "\t * Removes the <code>CustomerAccount</code>'s ownership of a given\n" +
                "\t * <code>BankAccount</code>.\n" +
                "\t * @param account The <code>BankAccount</code> to remove ownership of\n" +
                "\t */\n" +
                "\tpublic void removeBankAccount(BankAccount account) {\n" +
                "\t\tbankAccounts.remove(account);\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"name\")\n" +
                "\tpublic String getName() {\n" +
                "\t\treturn name;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setName(String name) {\n" +
                "\t\tthis.name = name;\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"surname\")\n" +
                "\tpublic String getSurname() {\n" +
                "\t\treturn surname;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setSurname(String surname) {\n" +
                "\t\tthis.surname = surname;\n" +
                "\t}\n" +
                "\n" +
                "\t@Id\n" +
                "\t@Column(name = \"customer_BSN\")\n" +
                "\tpublic String getBSN() {\n" +
                "\t\treturn BSN;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setBSN(String bSN) {\n" +
                "\t\tBSN = bSN;\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"street_address\")\n" +
                "\tpublic String getStreetAddress() {\n" +
                "\t\treturn streetAddress;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setStreetAddress(String streetAddress) {\n" +
                "\t\tthis.streetAddress = streetAddress;\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"phone_number\")\n" +
                "\tpublic String getPhoneNumber() {\n" +
                "\t\treturn phoneNumber;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setPhoneNumber(String phoneNumber) {\n" +
                "\t\tthis.phoneNumber = phoneNumber;\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"email\")\n" +
                "\tpublic String getEmail() {\n" +
                "\t\treturn email;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setEmail(String email) {\n" +
                "\t\tthis.email = email;\n" +
                "\t}\n" +
                "\n" +
                "\t@Column(name = \"birth_date\")\n" +
                "\tpublic String getBirthdate() {\n" +
                "\t\treturn birthdate;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setBirthdate(String birthdate) {\n" +
                "\t\tthis.birthdate = birthdate;\n" +
                "\t}\n" +
                "\t\n" +
                "\t@Transient\n" +
                "\tpublic String getPrimaryKeyName() {\n" +
                "\t\treturn PRIMARYKEYNAME;\n" +
                "\t}\n" +
                "\t\n" +
                "\t@Transient\n" +
                "\tpublic String getPrimaryKeyVal() {\n" +
                "\t\treturn BSN;\n" +
                "\t}\n" +
                "\t\n" +
                "\t@Transient\n" +
                "\tpublic String getClassName() {\n" +
                "\t\treturn CLASSNAME;\n" +
                "\t}\n" +
                "\n" +
                "\t@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)\n" +
                "\t@JoinTable(name = \"customerbankaccounts\", joinColumns = {\n" +
                "\t\t\t@JoinColumn(name = \"customer_BSN\", nullable = false, updatable = false)}, inverseJoinColumns = {\n" +
                "\t\t\t\t\t@JoinColumn(name = \"IBAN\", nullable = false, updatable = false)})\n" +
                "\tpublic Set<BankAccount> getBankAccounts() {\n" +
                "\t\treturn bankAccounts;\n" +
                "\t}\n" +
                "\n" +
                "\tpublic void setBankAccounts(Set<BankAccount> bankAccounts) {\n" +
                "\t\tthis.bankAccounts = bankAccounts;\n" +
                "\t}\n" +
                "\t\n" +
                "\tpublic void saveToDB() {\n" +
                "\t\tDataManager.save(this);\n" +
                "\t}\n" +
                "\t\n" +
                "\tpublic void deleteFromDB() {\n" +
                "\t\tfor (BankAccount key : getBankAccounts()) {\n" +
                "\t\t\tkey.deleteFromDB();\n" +
                "\t\t}\n" +
                "\t\tDataManager.removeEntryFromDB(this);\n" +
                "\t}\n" +
                "}\n");
        System.out.println(p);
    }

    public Parser(String file) throws NotAClassException {
        contents = file;
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
        } catch (StackOverflowError e) {
            System.out.println(file);
            throw new IllegalStateException("Stack overflow");
        }
//        System.out.println(this);
    }

    public static String getName(String file) throws NotAClassException {
        String className = "";
        String packageName = "";
        Matcher m = CLASS_DEFINITION.matcher(file);
        if (m.find()) {
            String result = m.group();
            Matcher classNameMatcher = IDENTIFIER_NODOTS.matcher(result);

            for (int i = 0; i < 2 && classNameMatcher.find(); i++) ;

            className = classNameMatcher.group(0);

        }
        if (className == null) {
            throw new NotAClassException();
        }

        Matcher m2 = PACKAGE_DEFINITION.matcher(file);
        if (m2.find()) {
            Matcher packageIdentifierMatcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(m2.group());

            for (int i = 0; i < 2 && packageIdentifierMatcher.find(); i++) ;
            packageName = packageIdentifierMatcher.group();
        }
        return packageName + "." + className;
    }

    private void identifyKnownDependencies() {
        for (String s : dependencies) {
            if (s.contains(".")) {
                knownDependencies.add(s);
            }
        }
        for (String s : imports) {
            String[] array = s.split("\\.");
            if (dependencies.contains(array[array.length - 1])) {
                knownDependencies.add(s);
                dependencies.remove(array[array.length - 1]);
            }
        }
    }


    public String removeComments(String file) {
        Matcher m = COMMENTS.matcher(file);
        return m.replaceAll("");
    }

    public String removeStringContents(String file) {
        Matcher m = STRING_DECLARATION.matcher(file);
        return m.replaceAll("\"\"");
    }

    //TODO implement nested classes and multiple classes in a single file
    public void extractClassInheritance(String file) throws NotAClassException {
        Matcher m = CLASS_DEFINITION.matcher(file);
        if (m.find()) {
            String result = m.group();
            Matcher classNameMatcher = IDENTIFIER_NODOTS.matcher(result);

            for (int i = 0; i < 2 && classNameMatcher.find(); i++) ;

            className = classNameMatcher.group(0);


            Matcher genericMatcher = CLASS_GENERIC_DEFINITION.matcher(result);
            if (genericMatcher.find()) {
                String generics = genericMatcher.group();
                Matcher genericIdentifiers = IDENTIFIER_NODOTS.matcher(generics);
                while (genericIdentifiers.find()) {
                    genericTypes.add(genericIdentifiers.group());
                }
            }

            //retrieve inheritance
            //replace all < > with whitespace
            Matcher matcher = HOOK_BRACKETS.matcher(file);
            String noHookBrackets = matcher.replaceAll(" ");

            Matcher inheritanceMatcher = EXTENDS_OR_IMPLEMENTS_DEFINITION.matcher(noHookBrackets);
            if (inheritanceMatcher.find()) {
                String extendsOrImplements = inheritanceMatcher.group();
                Matcher identifiers = IDENTIFIER_MAYDOTS.matcher(extendsOrImplements);
                while (identifiers.find()) {
                    String r = identifiers.group();
                    if (!(r.equals("extends") || r.equals("implements"))) {
                        dependencies.add(r);
                    }
                }
            }
        }
        if (className == null) {
            throw new NotAClassException();
        }
    }

    public void extractPackageDeclaration(String file) {
        Matcher m = PACKAGE_DEFINITION.matcher(file);
        if (m.find()) {
            Matcher packageIdentifierMatcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(m.group());

            for (int i = 0; i < 2 && packageIdentifierMatcher.find(); i++) ;
            packageName = packageIdentifierMatcher.group();
        } else {
//            System.err.println("No package defined");
        }
    }

    public void extractImports(String file) {
        Matcher m = IMPORT_DEFINITION.matcher(file);
        List<String> entries = new ArrayList<>();
        while (m.find()) {
            entries.add(m.group());
        }

        for (String entry : entries) {
            Matcher matcher = IDENTIFIER_MAYDOTS_MAYSTAR.matcher(entry);
            matcher.find();
            if (matcher.find()) {
                String imp = matcher.group();
                if (imp.contains("*")) {
                    starImports.add(imp.substring(0, imp.length() - 1));
                } else {
                    imports.add(imp);
                }
            }
        }
    }

    public void extractDeclarations(String file) {
        Matcher fieldsDecMatcher = FIELD_DECLARATION.matcher(file);
        while (fieldsDecMatcher.find()) {
            String declaration = fieldsDecMatcher.group();
            //all but last identifiers are dependencies
            Matcher identifiers = IDENTIFIER_MAYDOTS.matcher(declaration);
            String prev = "";
            if (identifiers.find()) {
                prev = identifiers.group();
            }
            while (identifiers.find()) {
//                System.err.println(prev);
                if (!primitives.contains(prev) && !prev.equals("return") && !prev.equals("break")) {
                    dependencies.add(prev);
                }
                prev = identifiers.group();
            }
        }
    }

    public String getName() {
        return packageName + "." + className;
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

    public void addUnknownExternalDependencies(String dependency) {
        unknownExternalDependencies.add(dependency);
    }

    public Set<String> getUnknownExternalDependencies() {
        return unknownExternalDependencies;
    }

    public Set<String> getStarImports() {
        return starImports;
    }

    public String getContents() {
        return contents;
    }

    public int getNumberOflines() {
        String noComments = removeComments(contents);
        try {
            Parser p = new Parser(noComments);
        } catch (NotAClassException e) {
            System.out.println("COULD NOT PARSE CLASS");
            System.out.println(noComments);
        }
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(noComments);
        int lines = 1;
        while (m.find()) {
            lines++;
        }
        return lines;
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
