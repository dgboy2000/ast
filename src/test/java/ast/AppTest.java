package ast;

import com.google.common.io.Files;

import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;


import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.*;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.*;
import japa.parser.ast.visitor.VoidVisitor;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.tools.*;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    static final String thisFilename = System.getProperty("user.dir") + "/src/test/java/ast/AppTest.java";
    static final String thatFilename = System.getProperty("user.dir") + "/src/main/java/ast/Processor.java";
    static final String otherFilename = System.getProperty("user.dir") + "/src/main/java/ast/PostOrderVisitor.java";


    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testAst()
    {
        char[] source = "int a=0; int b=a+1;".toCharArray();
    }

    public void testParser() throws IOException {
//        visit(thisFilename);
//        visit(thatFilename);
        visit(otherFilename);
    }

    private void visit(String filename) throws IOException
    {
        Context context = new Context();

        // http://docs.oracle.com/javase/7/docs/api/javax/tools/StandardJavaFileManager.html
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, null);
        context.put(JavaFileManager.class, fm);

        Parser.Factory parserFactory = Parser.Factory.instance(context);
        Scanner.Factory scannerFactory = Scanner.Factory.instance(context);

        String thisFileContents = Files.toString(new File(filename), Charset.defaultCharset());
        Scanner scanner = scannerFactory.newScanner(thisFileContents);
        Parser parser = parserFactory.newParser(scanner, true, true);
        JCTree.JCCompilationUnit compilationUnit = parser.compilationUnit();

        JCTree.Visitor visitor;
//        visitor = new PostOrderVisitor();
        visitor = new ProcessorTest.MinifiedProcessor();
        visitor.visitTopLevel(compilationUnit);
    }

    public void testJavaParse() throws IOException, ParseException {
        // creates an input stream for the file to be parsed
        InputStream stream = new FileInputStream(thisFilename);

        CompilationUnit cu;
        try {
            // parse the file
            cu = JavaParser.parse(stream);
        } finally {
            stream.close();
        }

        VoidVisitor visitor = new VoidVisitor<Object>() {
            @Override
            public void visit(CompilationUnit compilationUnit, Object o) {
                System.out.println("Visiting CompilationUnit");
                this.visit(compilationUnit.getPackage(), o);

                for (ImportDeclaration importDeclaration : compilationUnit.getImports())
                {
                    this.visit(importDeclaration, o);
                }

                for (TypeDeclaration typeDeclaration : compilationUnit.getTypes())
                {
                    this.visit(typeDeclaration, o);
                }
            }

            private void visit(TypeDeclaration typeDeclaration, Object o)
            {
                if (typeDeclaration instanceof ClassOrInterfaceDeclaration) visit((ClassOrInterfaceDeclaration) typeDeclaration, o);
                else if (typeDeclaration instanceof EnumDeclaration) visit((EnumDeclaration) typeDeclaration, o);
                else throw new IllegalArgumentException("Unhandled TypeDeclaration of type "+typeDeclaration.getClass().getName());
            }

            @Override
            public void visit(PackageDeclaration packageDeclaration, Object o) {
                System.out.println("package "+packageDeclaration.getName()+";");
            }

            @Override
            public void visit(ImportDeclaration importDeclaration, Object o) {
                if (importDeclaration.isStatic()) System.out.print("static ");
                System.out.print("import "+importDeclaration.getName());
                if (importDeclaration.isAsterisk()) System.out.print(".*");
                System.out.println(";");
            }

            @Override
            public void visit(TypeParameter typeParameter, Object o) {
                System.out.println("Visiting TypeParameter");
            }

            @Override
            public void visit(LineComment lineComment, Object o) {
                System.out.println("Visiting LineComment");
            }

            @Override
            public void visit(BlockComment blockComment, Object o) {
                System.out.println("Visiting BlockComment");
            }

            @Override
            public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Object o) {
                classOrInterfaceDeclaration.getModifiers();
                for (ClassOrInterfaceType classOrInterfaceType : classOrInterfaceDeclaration.getExtends())
                {
                    visit(classOrInterfaceType, o);
                }
                System.out.println("Visiting ClassOrInterfaceDeclaration");
            }

            @Override
            public void visit(EnumDeclaration enumDeclaration, Object o) {
                System.out.println("Visiting EnumDeclaration");
            }

            @Override
            public void visit(EmptyTypeDeclaration emptyTypeDeclaration, Object o) {
                System.out.println("Visiting EmptyTypeDeclaration");
            }

            @Override
            public void visit(EnumConstantDeclaration enumConstantDeclaration, Object o) {
                System.out.println("Visiting EnumConstantDeclaration");
            }

            @Override
            public void visit(AnnotationDeclaration annotationDeclaration, Object o) {
                System.out.println("Visiting AnnotationDeclaration");
            }

            @Override
            public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(FieldDeclaration fieldDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(VariableDeclarator variableDeclarator, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(VariableDeclaratorId variableDeclaratorId, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ConstructorDeclaration constructorDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(MethodDeclaration methodDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(Parameter parameter, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(EmptyMemberDeclaration emptyMemberDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(InitializerDeclaration initializerDeclaration, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(JavadocComment javadocComment, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ClassOrInterfaceType classOrInterfaceType, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(PrimitiveType primitiveType, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ReferenceType referenceType, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(VoidType voidType, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(WildcardType wildcardType, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ArrayAccessExpr arrayAccessExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ArrayCreationExpr arrayCreationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ArrayInitializerExpr arrayInitializerExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(AssignExpr assignExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(BinaryExpr binaryExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(CastExpr castExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ClassExpr classExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ConditionalExpr conditionalExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(EnclosedExpr enclosedExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(FieldAccessExpr fieldAccessExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(InstanceOfExpr instanceOfExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(StringLiteralExpr stringLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(IntegerLiteralExpr integerLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(LongLiteralExpr longLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(IntegerLiteralMinValueExpr integerLiteralMinValueExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(LongLiteralMinValueExpr longLiteralMinValueExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(CharLiteralExpr charLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(DoubleLiteralExpr doubleLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(BooleanLiteralExpr booleanLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(NullLiteralExpr nullLiteralExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(MethodCallExpr methodCallExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(NameExpr nameExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ObjectCreationExpr objectCreationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(QualifiedNameExpr qualifiedNameExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ThisExpr thisExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(SuperExpr superExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(UnaryExpr unaryExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(VariableDeclarationExpr variableDeclarationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(MarkerAnnotationExpr markerAnnotationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(NormalAnnotationExpr normalAnnotationExpr, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(MemberValuePair memberValuePair, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ExplicitConstructorInvocationStmt explicitConstructorInvocationStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(TypeDeclarationStmt typeDeclarationStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(AssertStmt assertStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(BlockStmt blockStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(LabeledStmt labeledStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(EmptyStmt emptyStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ExpressionStmt expressionStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(SwitchStmt switchStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(SwitchEntryStmt switchEntryStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(BreakStmt breakStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ReturnStmt returnStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(IfStmt ifStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(WhileStmt whileStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ContinueStmt continueStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(DoStmt doStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ForeachStmt foreachStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ForStmt forStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(ThrowStmt throwStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(SynchronizedStmt synchronizedStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(TryStmt tryStmt, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void visit(CatchClause catchClause, Object o) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        cu.accept(visitor, "ignore this arg");


        // prints the resulting compilation unit to default system output
        System.out.println(cu.toString());
    }
}
