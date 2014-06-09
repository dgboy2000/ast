package ast;

import com.google.common.collect.Lists;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dannygoodman
 * Date: 6/7/14
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class PostOrderVisitor extends JCTree.Visitor
{

    public static String minify(String javaCode)
    {
        Context context = new Context();

        // http://docs.oracle.com/javase/7/docs/api/javax/tools/StandardJavaFileManager.html
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(diagnostics, null, null);
        context.put(JavaFileManager.class, fm);

        Parser.Factory parserFactory = Parser.Factory.instance(context);
        Scanner.Factory scannerFactory = Scanner.Factory.instance(context);

        Scanner scanner = scannerFactory.newScanner(javaCode);
        Parser parser = parserFactory.newParser(scanner, true, true);
        JCTree.JCCompilationUnit compilationUnit = parser.compilationUnit();

        PostOrderVisitor visitor = new PostOrderVisitor();
        visitor.visitTopLevel(compilationUnit);
        return visitor.processor.toString();
    }

    public static class Processor
    {
        private List<Name> nestedClasses = new ArrayList<Name>();
        public Name getCurrentClass() { return nestedClasses.get(nestedClasses.size() - 1); }
        private boolean lastIsKeyword = false;
        private StringBuilder stringBuilder = new StringBuilder();

        private void printSym(Object toPrint)
        {
            stringBuilder.append(toPrint.toString());
            lastIsKeyword = false;
        }
        private void printWord(Object toPrint)
        {
            if (lastIsKeyword) stringBuilder.append(" ");
            stringBuilder.append(toPrint.toString());
            lastIsKeyword = true;
        }

        public void processCloseBlock() { printSym("}"); }
        public void processCloseClass() { this.nestedClasses.remove(this.nestedClasses.size() - 1); }
        public void processCloseParenthesis() { printSym(")"); }
        public void processKeyword(String keyword) { printWord(keyword); }
        public void processLiteral(String literal) { printWord(literal); }
        public void processName(Name name) { printWord(name); }
        public void processOpenBlock() { printSym("{"); }
        public void processOpenClass(Name className) { this.nestedClasses.add(className); }
        public void processOpenParenthesis() { printSym("("); }
        public void processSymbol(String symbol) { printSym(symbol); }

        public String toString() { return stringBuilder.toString(); }
    }

    private Processor processor = new Processor();

    private boolean isPrefix(Tree.Kind operatorType)
    {
        switch(operatorType)
        {
            case BITWISE_COMPLEMENT:
            case UNARY_MINUS:
            case UNARY_PLUS:
            case LOGICAL_COMPLEMENT:
            case PREFIX_DECREMENT:
            case PREFIX_INCREMENT:
                return true;
            case POSTFIX_DECREMENT:
            case POSTFIX_INCREMENT:
                return false;
            default:
                throw new IllegalArgumentException("Unhandled operator of type: "+operatorType.toString());
        }
    }

    private boolean needsSemicolon(JCTree.JCStatement statement) {
        switch (statement.getKind())
        {
            case ASSERT:
            case BREAK:
            case CONTINUE:
            case EXPRESSION_STATEMENT:
            case RETURN:
            case THROW:
            case VARIABLE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void visitAnnotation(JCTree.JCAnnotation jcAnnotation)
    {
        processor.processSymbol("@");
        visitTree(jcAnnotation.getAnnotationType());
        List<JCTree.JCExpression> arguments = jcAnnotation.getArguments();
        if (arguments != null && arguments.size() > 0)
        {
            processor.processOpenParenthesis();
            visitWithCommas(arguments);
            processor.processCloseParenthesis();
        }
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation)
    {
        visitTree(jcMethodInvocation.getMethodSelect());
        processor.processOpenParenthesis();

        List<JCTree.JCExpression> arguments = jcMethodInvocation.getArguments();
        if (arguments != null)
        {
            visitWithCommas(arguments);
        }

        // TODO: type arguments
        processor.processCloseParenthesis();
    }

    @Override
    public void visitAssert(JCTree.JCAssert jcAssert)
    {
        // TODO: detail
        processor.processKeyword("assert");
        visitTree(jcAssert.getCondition());
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign)
    {
        visitTree(jcAssign.getVariable());
        processor.processSymbol("=");
        visitTree(jcAssign.getExpression());
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp)
    {
        visitTree(jcAssignOp.getVariable());
        visitOperator(jcAssignOp.getKind());
        visitTree(jcAssignOp.getExpression());
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary)
    {
        visitTree(jcBinary.getLeftOperand());
        visitOperator(jcBinary.getKind());
        visitTree(jcBinary.getRightOperand());
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock)
    {
        processor.processOpenBlock();
        if (jcBlock.getStatements() != null)
        {
            visitStatements(jcBlock.getStatements());
        }
        processor.processCloseBlock();
    }

    @Override
    public void visitBreak(JCTree.JCBreak jcBreak)
    {
        // TODO: label
        if (jcBreak.getLabel() != null) throw new IllegalArgumentException("can't handle label");
        processor.processKeyword("break");
    }

    @Override
    public void visitCase(JCTree.JCCase jcCase)
    {
        if (jcCase.getExpression() == null)
        {
            processor.processKeyword("default");
        }
        else
        {
            processor.processKeyword("case");
            visitTree(jcCase.getExpression());
        }
        processor.processSymbol(":");

        visitStatements(jcCase.getStatements());
    }

    @Override
    public void visitCatch(JCTree.JCCatch jcCatch)
    {
        processor.processKeyword("catch");
        processor.processOpenParenthesis();
        visitVarDef(jcCatch.getParameter());
        processor.processCloseParenthesis();
        visitBlock(jcCatch.getBlock());
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl)
    {
        JCTree.JCModifiers modifiers = jcClassDecl.getModifiers();
        visitModifiers(modifiers);

        boolean isInterface = (modifiers.flags & 512) == 512;
        if (isInterface) processor.processKeyword("interface");
//        else if ((modifiers.flags & 16384) == 16384) processor.processKeyword("enum");
        else processor.processKeyword("class");

        processor.processName(jcClassDecl.getSimpleName());
        List<JCTree.JCTypeParameter> typeParameters = jcClassDecl.getTypeParameters();
        if (typeParameters != null && typeParameters.size() > 0)
        {
            processor.processSymbol("<");
            visitWithCommas(jcClassDecl.getTypeParameters());
            processor.processSymbol(">");
        }

        if (jcClassDecl.getExtendsClause() != null)
        {
            processor.processKeyword("extends");
            visitTree(jcClassDecl.getExtendsClause());
        }

        List<JCTree.JCExpression> interfaces = jcClassDecl.getImplementsClause();
        if (interfaces.size() > 0)
        {
            if (isInterface) processor.processKeyword("extends");
            else processor.processKeyword("implements");
            visitWithCommas(interfaces);
        }

        processor.processOpenClass(jcClassDecl.getSimpleName());
        visitClassMembers(jcClassDecl.getMembers());
        processor.processCloseClass();
    }

    private void visitClassMembers(List<JCTree> members)
    {
        processor.processOpenBlock();
        for (JCTree member : members)
        {
            visitTree(member);
            if (member.getKind() == Tree.Kind.VARIABLE ||
                    (member.getKind() == Tree.Kind.METHOD && ((JCTree.JCMethodDecl) member).getBody() == null))
            {
                processor.processSymbol(";");
            }
        }
        processor.processCloseBlock();
    }

    @Override
    public void visitConditional(JCTree.JCConditional jcConditional)
    {
        visitTree(jcConditional.getCondition());
        processor.processSymbol("?");
        visitTree(jcConditional.getTrueExpression());
        processor.processSymbol(":");
        visitTree(jcConditional.getFalseExpression());
    }

    @Override
    public void visitContinue(JCTree.JCContinue jcContinue)
    {
        if (jcContinue.getLabel() != null) throw new IllegalArgumentException("Can't handle continue labels");
        // TODO: labels

        processor.processKeyword("continue");
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop jcDoWhileLoop)
    {
        processor.processKeyword("do");
        visitTree(jcDoWhileLoop.getStatement());
        if (needsSemicolon(jcDoWhileLoop.getStatement())) processor.processSymbol(";");
        processor.processKeyword("while");
        visitTree(jcDoWhileLoop.getCondition());
        processor.processSymbol(";");
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement jcExpressionStatement)
    {
        visitTree(jcExpressionStatement.getExpression());
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop)
    {
        processor.processKeyword("for");
        processor.processOpenParenthesis();
        visitVarDef(jcEnhancedForLoop.getVariable());
        processor.processSymbol(":");
        visitTree(jcEnhancedForLoop.getExpression());
        processor.processCloseParenthesis();
        visitTree(jcEnhancedForLoop.getStatement());
        if (needsSemicolon(jcEnhancedForLoop.getStatement())) processor.processSymbol(";");
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop)
    {
        processor.processKeyword("for");
        processor.processOpenParenthesis();

        List<JCTree.JCStatement> initializers = jcForLoop.getInitializer();
        if (initializers.size() == 1)
        {
            visitTree(initializers.get(0));
        }
        else if (initializers.size() > 1) throw new IllegalArgumentException("Too many initializers: "+initializers.size());
        processor.processSymbol(";");

        visitTree(jcForLoop.getCondition());
        processor.processSymbol(";");

        List<JCTree.JCExpressionStatement> updates = jcForLoop.getUpdate();
        if (updates.size() == 1)
        {
            visitTree(updates.get(0).getExpression());
        }
        else if (initializers.size() > 1) throw new IllegalArgumentException("Too many initializers: "+initializers.size());

        processor.processCloseParenthesis();

        visitTree(jcForLoop.getStatement());
        if (needsSemicolon(jcForLoop.getStatement())) processor.processSymbol(";");
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent)
    {
        processor.processName(jcIdent.getName());
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf)
    {
        processor.processKeyword("if");
        visitTree(jcIf.getCondition());

        JCTree.JCStatement thenStatement = jcIf.getThenStatement();
        visitTree(thenStatement);
        if (needsSemicolon(thenStatement)) processor.processSymbol(";");

        if (jcIf.getElseStatement() != null)
        {
            JCTree.JCStatement elseStatement = jcIf.getElseStatement();
            processor.processKeyword("else");
            visitTree(elseStatement);
            if (needsSemicolon(elseStatement)) processor.processSymbol(";");
        }
    }

    @Override
    public void visitImport(JCTree.JCImport jcImport)
    {
        processor.processKeyword("import");
        if (jcImport.isStatic())
        {
            processor.processKeyword("static");
        }
        visitTree(jcImport.getQualifiedIdentifier());
        processor.processSymbol(";");
    }

    @Override
    public void visitIndexed(JCTree.JCArrayAccess jcArrayAccess)
    {
        visitTree(jcArrayAccess.getExpression());
        processor.processSymbol("[");
        visitTree(jcArrayAccess.getIndex());
        processor.processSymbol("]");
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral jcLiteral)
    {
        processor.processLiteral(jcLiteral.toString());
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl)
    {
        visitModifiers(jcMethodDecl.getModifiers());

        if (jcMethodDecl.getReturnType() != null)
        {
            visitTree(jcMethodDecl.getReturnType());
        }
        if (jcMethodDecl.getName().toString().equals("<init>")) processor.processName(processor.getCurrentClass());
        else processor.processName(jcMethodDecl.getName());

        processor.processOpenParenthesis();
        visitWithCommas(jcMethodDecl.getParameters());
        processor.processCloseParenthesis();

        List<JCTree.JCExpression> possibleExceptions = jcMethodDecl.getThrows();
        if (possibleExceptions.size() > 0)
        {
            processor.processKeyword("throws");
            visitWithCommas(possibleExceptions);
        }

        // TODO: type parameters, default value
        if (jcMethodDecl.getBody() != null)
        {
            visitBlock(jcMethodDecl.getBody());
        }
    }

    @Override
    public void visitModifiers(JCTree.JCModifiers jcModifiers)
    {
        for (JCTree.JCAnnotation annotation : jcModifiers.getAnnotations())
        {
            visitAnnotation(annotation);
        }

        for (Modifier modifier : jcModifiers.getFlags())
        {
            processor.processKeyword(modifier.toString());
        }
    }

    @Override
    public void visitNewArray(JCTree.JCNewArray jcNewArray)
    {
        List<JCTree.JCExpression> dimensions = jcNewArray.getDimensions();

        if (jcNewArray.getType() != null)
        {
            processor.processKeyword("new");
            JCTree.JCExpression arrayType = jcNewArray.getType();
            if (arrayType instanceof JCTree.JCArrayTypeTree) // This is because Java multidimensional syntax is backwards from what a parse tree would expect
            {
                visitTree(((JCTree.JCArrayTypeTree) arrayType).getType());
                for (JCTree.JCExpression dimension : dimensions)
                {
                    processor.processSymbol("[");
                    visitTree(dimension);
                    processor.processSymbol("]");
                }
                processor.processSymbol("[]");
            }
            else
            {
                visitTree(jcNewArray.getType());
                if (dimensions.size() == 0)
                {
                    processor.processSymbol("[]");
                }
                for (JCTree.JCExpression dimension : dimensions)
                {
                    processor.processSymbol("[");
                    visitTree(dimension);
                    processor.processSymbol("]");
                }
            }
        }

        if (jcNewArray.getInitializers() != null)
        {
            List<JCTree.JCExpression> initializers = jcNewArray.getInitializers();
            processor.processSymbol("{");
            visitWithCommas(initializers);
            processor.processSymbol("}");
            return;
        }
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass jcNewClass)
    {
        if (jcNewClass.getEnclosingExpression() != null)
        {
            visitTree(jcNewClass.getEnclosingExpression());
            processor.processSymbol(".");
        }

        processor.processKeyword("new");
        visitTree(jcNewClass.getIdentifier());
        processor.processOpenParenthesis();
        List<JCTree.JCExpression> arguments = jcNewClass.getArguments();
        if (arguments != null)
        {
            visitWithCommas(arguments);
        }
        processor.processCloseParenthesis();

        if (jcNewClass.getClassBody() != null)
        {
            visitClassMembers(jcNewClass.getClassBody().getMembers());
        }

        if (jcNewClass.getTypeArguments().size() > 0)
            throw new IllegalArgumentException("not implemented");
    }

    private void visitOperator(Tree.Kind kind)
    {
        switch (kind)
        {
            case BITWISE_COMPLEMENT: processor.processSymbol("~"); break;
            case PLUS: case UNARY_PLUS: processor.processSymbol("+"); break;
            case MINUS: case UNARY_MINUS: processor.processSymbol("-"); break;
            case MULTIPLY: processor.processSymbol("*"); break;
            case DIVIDE: processor.processSymbol("/"); break;
            case REMAINDER: processor.processSymbol("%"); break;
            case LESS_THAN: processor.processSymbol("<"); break;
            case GREATER_THAN: processor.processSymbol(">"); break;
            case LESS_THAN_EQUAL: processor.processSymbol("<="); break;
            case GREATER_THAN_EQUAL: processor.processSymbol(">="); break;
            case EQUAL_TO: processor.processSymbol("=="); break;
            case NOT_EQUAL_TO: processor.processSymbol("!="); break;
            case AND: processor.processSymbol("&&"); break;
            case XOR: processor.processSymbol("^"); break;
            case OR: processor.processSymbol("||"); break;
            case LOGICAL_COMPLEMENT: processor.processSymbol("!"); break;
            case CONDITIONAL_AND: processor.processSymbol("&&"); break;
            case CONDITIONAL_OR: processor.processSymbol("||"); break;
            case MULTIPLY_ASSIGNMENT: processor.processSymbol("*="); break;
            case DIVIDE_ASSIGNMENT: processor.processSymbol("/="); break;
            case REMAINDER_ASSIGNMENT: processor.processSymbol("%="); break;
            case PLUS_ASSIGNMENT: processor.processSymbol("+="); break;
            case MINUS_ASSIGNMENT: processor.processSymbol("-="); break;
            case LEFT_SHIFT: processor.processSymbol("<<"); break;
            case RIGHT_SHIFT: processor.processSymbol(">>"); break;
            case UNSIGNED_RIGHT_SHIFT: processor.processSymbol(">>>"); break;
//                    case LEFT_SHIFT_ASSIGNMENT: processor.processSymbol("+"); break;
//                    case RIGHT_SHIFT_ASSIGNMENT: processor.processSymbol("+"); break;
//                    case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT: processor.processSymbol("+"); break;
            case AND_ASSIGNMENT: processor.processSymbol("&="); break;
            case XOR_ASSIGNMENT: processor.processSymbol("^="); break;
            case OR_ASSIGNMENT: processor.processSymbol("|="); break;
            case PREFIX_INCREMENT: case POSTFIX_INCREMENT: processor.processSymbol("++"); break;
            case PREFIX_DECREMENT: case POSTFIX_DECREMENT: processor.processSymbol("--"); break;
            case UNBOUNDED_WILDCARD: case EXTENDS_WILDCARD: case SUPER_WILDCARD: processor.processSymbol("?"); break;
            default: throw new IllegalArgumentException("Unrecognized operator "+kind.toString());
        }
    }

    @Override
    public void visitParens(JCTree.JCParens jcParens)
    {
        processor.processOpenParenthesis();
        visitTree(jcParens.getExpression());
        processor.processCloseParenthesis();
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn)
    {
        processor.processKeyword("return");
        visitTree(jcReturn.getExpression());
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess)
    {
        visitTree(jcFieldAccess.selected);
        processor.processSymbol(".");
        processor.processName(jcFieldAccess.getIdentifier());
    }

    private void visitStatements(List<JCTree.JCStatement> statements) {
        for (JCTree.JCStatement statement : statements)
        {
            visitTree(statement);
            if (needsSemicolon(statement)) processor.processSymbol(";");
        }
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch jcSwitch)
    {
        processor.processKeyword("switch");
        visitTree(jcSwitch.getExpression());
        processor.processOpenBlock();

        for (JCTree.JCCase jcCase : jcSwitch.getCases())
        {
            visitCase(jcCase);
        }

        processor.processCloseBlock();
    }

    @Override
    public void visitSynchronized(JCTree.JCSynchronized jcSynchronized)
    {
        processor.processKeyword("synchronized");
        visitTree(jcSynchronized.getExpression());
        visitBlock(jcSynchronized.getBlock());
    }

    @Override
    public void visitThrow(JCTree.JCThrow jcThrow)
    {
        processor.processKeyword("throw");
        visitTree(jcThrow.getExpression());
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit compilationUnit)
    {
        // TODO: package annotations
        processor.processKeyword("package");
        visitTree(compilationUnit.getPackageName());
        processor.processSymbol(";");

        for (JCTree.JCImport jcImport : compilationUnit.getImports())
        {
            visitImport(jcImport);
        }

        for (JCTree jcTree : compilationUnit.getTypeDecls())
        {
            visitTree(jcTree);
        }
    }

    @Override
    public void visitTree(JCTree jcTree)
    {
        if (jcTree == null) return;
        switch (jcTree.getKind()) {
            case ANNOTATION: this.visitAnnotation((JCTree.JCAnnotation) jcTree); break;
            case ARRAY_ACCESS: this.visitIndexed((JCTree.JCArrayAccess) jcTree); break;
            case ARRAY_TYPE: this.visitTypeArray((JCTree.JCArrayTypeTree) jcTree); break;
            case ASSERT: this.visitAssert((JCTree.JCAssert) jcTree); break;
            case ASSIGNMENT: this.visitAssign((JCTree.JCAssign) jcTree); break;
            case BLOCK: this.visitBlock((JCTree.JCBlock) jcTree); break;
            case BREAK: this.visitBreak((JCTree.JCBreak) jcTree); break;
            case CLASS: this.visitClassDef((JCTree.JCClassDecl) jcTree); break;
            case CONDITIONAL_EXPRESSION: this.visitConditional((JCTree.JCConditional) jcTree); break;
            case CONTINUE: this.visitContinue((JCTree.JCContinue) jcTree); break;
            case DO_WHILE_LOOP: this.visitDoLoop((JCTree.JCDoWhileLoop) jcTree); break;
            case ENHANCED_FOR_LOOP: this.visitForeachLoop((JCTree.JCEnhancedForLoop) jcTree); break;
            case EMPTY_STATEMENT: break;
            case EXPRESSION_STATEMENT: this.visitExec((JCTree.JCExpressionStatement) jcTree); break;
            case FOR_LOOP: this.visitForLoop((JCTree.JCForLoop) jcTree); break;
            case IDENTIFIER: this.visitIdent((JCTree.JCIdent) jcTree); break;
            case IF: this.visitIf((JCTree.JCIf) jcTree); break;
            case IMPORT: this.visitImport((JCTree.JCImport) jcTree); break;
            case INSTANCE_OF: this.visitTypeTest((JCTree.JCInstanceOf) jcTree); break;
            case MEMBER_SELECT: this.visitSelect((JCTree.JCFieldAccess) jcTree); break;
            case METHOD: this.visitMethodDef((JCTree.JCMethodDecl) jcTree); break;
            case METHOD_INVOCATION: this.visitApply((JCTree.JCMethodInvocation) jcTree); break;
            case MODIFIERS: this.visitModifiers((JCTree.JCModifiers) jcTree); break;
            case NEW_ARRAY: this.visitNewArray((JCTree.JCNewArray) jcTree); break;
            case NEW_CLASS: this.visitNewClass((JCTree.JCNewClass) jcTree); break;
            case PARAMETERIZED_TYPE: this.visitTypeApply((JCTree.JCTypeApply) jcTree); break;
            case PARENTHESIZED: this.visitParens((JCTree.JCParens) jcTree); break;
            case PRIMITIVE_TYPE: this.visitTypeIdent((JCTree.JCPrimitiveTypeTree) jcTree); break;
            case RETURN: this.visitReturn((JCTree.JCReturn) jcTree); break;
            case SWITCH: this.visitSwitch((JCTree.JCSwitch) jcTree); break;
            case SYNCHRONIZED: this.visitSynchronized((JCTree.JCSynchronized) jcTree); break;
            case THROW: this.visitThrow((JCTree.JCThrow) jcTree); break;
            case TRY: this.visitTry((JCTree.JCTry) jcTree); break;
            case TYPE_CAST: this.visitTypeCast((JCTree.JCTypeCast) jcTree); break;
            case TYPE_PARAMETER: this.visitTypeParameter((JCTree.JCTypeParameter) jcTree); break;
            case VARIABLE: this.visitVarDef((JCTree.JCVariableDecl) jcTree); break;
            case WHILE_LOOP: this.visitWhileLoop((JCTree.JCWhileLoop) jcTree); break;


            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case REMAINDER:
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN_EQUAL:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case AND:
            case XOR:
            case OR:
            case CONDITIONAL_AND:
            case CONDITIONAL_OR:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
                this.visitBinary((JCTree.JCBinary) jcTree);
                break;

            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                this.visitLiteral((JCTree.JCLiteral) jcTree);
                break;

            case BITWISE_COMPLEMENT:
            case UNARY_MINUS:
            case UNARY_PLUS:
            case LOGICAL_COMPLEMENT:
            case PREFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
                this.visitUnary((JCTree.JCUnary) jcTree);
                break;

            case MULTIPLY_ASSIGNMENT:
            case DIVIDE_ASSIGNMENT:
            case REMAINDER_ASSIGNMENT:
            case PLUS_ASSIGNMENT:
            case MINUS_ASSIGNMENT:
            case LEFT_SHIFT_ASSIGNMENT:
            case RIGHT_SHIFT_ASSIGNMENT:
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
            case AND_ASSIGNMENT:
            case XOR_ASSIGNMENT:
            case OR_ASSIGNMENT:
                this.visitAssignop((JCTree.JCAssignOp) jcTree);
                break;

            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
                this.visitWildcard((JCTree.JCWildcard) jcTree);
                break;


            default:
                throw new IllegalArgumentException("Unhandled kind " + jcTree.getKind().toString());
//                    case ANNOTATION:
//                        break;
//                    case EXPRESSION_STATEMENT:
//                        break;]
//                    case LABELED_STATEMENT:
//                        break;
//                    case SYNCHRONIZED:
//                        break;
//                    case ERRONEOUS:
//                        break;
//                    case OTHER:
//                        break;
        }
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry)
    {
        processor.processKeyword("try");
        visitBlock(jcTry.getBlock());

        for (JCTree.JCCatch jcCatch : jcTry.getCatches())
        {
            visitCatch(jcCatch);
        }

        if (jcTry.getFinallyBlock() != null)
        {
            processor.processKeyword("finally");
            visitBlock(jcTry.getFinallyBlock());
        }
    }

    @Override
    public void visitTypeApply(JCTree.JCTypeApply jcTypeApply)
    {
        visitTree(jcTypeApply.getType());
        processor.processSymbol("<");
        List<JCTree.JCExpression> typeArguments = jcTypeApply.getTypeArguments();
        visitWithCommas(typeArguments);
        processor.processSymbol(">");
    }

    @Override
    public void visitTypeArray(JCTree.JCArrayTypeTree jcArrayTypeTree)
    {
        visitTree(jcArrayTypeTree.getType());
        processor.processSymbol("[]");
    }

    @Override
    public void visitTypeCast(JCTree.JCTypeCast jcTypeCast)
    {
        processor.processOpenParenthesis();
        visitTree(jcTypeCast.getType());
        processor.processCloseParenthesis();
        visitTree(jcTypeCast.getExpression());
    }

    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree jcPrimitiveTypeTree)
    {
        processor.processKeyword(jcPrimitiveTypeTree.getPrimitiveTypeKind().toString().toLowerCase());
    }

    @Override
    public void visitTypeParameter(JCTree.JCTypeParameter jcTypeParameter)
    {
        processor.processName(jcTypeParameter.getName());
        List<JCTree.JCExpression> bounds = jcTypeParameter.getBounds();
        if (bounds != null && bounds.size() > 0)
        {
            if (bounds.size() > 1) throw new IllegalArgumentException("can't handle multiple bounds");
            processor.processKeyword("extends");
            visitTree(bounds.get(0));
        }
    }

    @Override
    public void visitTypeTest(JCTree.JCInstanceOf jcInstanceOf)
    {
        visitTree(jcInstanceOf.getExpression());
        processor.processKeyword("instanceof");
        visitTree(jcInstanceOf.getType());
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary)
    {
        if (isPrefix(jcUnary.getKind()))
        {
            visitOperator(jcUnary.getKind());
            visitTree(jcUnary.getExpression());
        }
        else
        {
            visitTree(jcUnary.getExpression());
            visitOperator(jcUnary.getKind());
        }
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl)
    {
        visitModifiers(jcVariableDecl.getModifiers());
        visitTree(jcVariableDecl.vartype);
        processor.processName(jcVariableDecl.getName());

        if (jcVariableDecl.getInitializer() != null)
        {
            processor.processSymbol("=");
            visitTree(jcVariableDecl.getInitializer());
        }
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop jcWhileLoop)
    {
        processor.processKeyword("while");
        visitTree(jcWhileLoop.getCondition());
        visitTree(jcWhileLoop.getStatement());
        if (needsSemicolon(jcWhileLoop.getStatement())) processor.processSymbol(";");
    }

    @Override
    public void visitWildcard(JCTree.JCWildcard jcWildcard)
    {
        visitOperator(jcWildcard.getKind());

        if (jcWildcard.getBound() != null)
        {
            if (jcWildcard.getKind() == Tree.Kind.EXTENDS_WILDCARD)
            {
                processor.processKeyword("extends");
            }
            else if (jcWildcard.getKind() == Tree.Kind.SUPER_WILDCARD)
            {
                processor.processKeyword("super");
            }
            else
            {
                throw new IllegalArgumentException("can't handle bound");
            }
            visitTree(jcWildcard.getBound());
        }
    }

    private void visitWithCommas(List<? extends JCTree> jcTrees)
    {
        boolean isFirst = true;
        for (JCTree jcTree : jcTrees)
        {
            if (!isFirst) processor.processSymbol(",");
            visitTree(jcTree);
            isFirst = false;
        }
    }

}
