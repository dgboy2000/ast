package ast;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dannygoodman
 * Date: 6/6/14
 * Time: 6:44 PM
 * To change this template use File | Settings | File Templates.
 *
 * How to make a tree of the code? Each node is a node of the AST with type,
 * identifier if applicable, and children.
 *
 * How to embed in a vector space?
 * Want to share representations of the same tokens, and have the nodes represent
 * the semantics of everything under them
 *
 * How to handle multiple children instead of 1? Could do the numenta thing and combine
 *
 * Split visiting and processing?
 * Want to be able to visit in-order
 */
public class Processor extends JCTree.Visitor
{
    private int indentLevel = 0;
    private List<String> nestedClasses = new ArrayList<String>();
    private String getCurrentClass() { return nestedClasses.get(nestedClasses.size() - 1); }

    private void print(Object toPrint)
    {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i=0; i<indentLevel; ++i) indentBuilder.append("\t");
        System.out.print(toPrint.toString().replaceAll("\n", "\n"+indentBuilder.toString()));
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation)
    {
        visitTree(jcMethodInvocation.getMethodSelect());
        print("(");

        List<JCTree.JCExpression> arguments = jcMethodInvocation.getArguments();
        if (arguments.size() > 0)
        {
            visitTree(arguments.get(0));
            for (JCTree.JCExpression arg : arguments.subList(1, arguments.size()))
            {
                print(", ");
                visitTree(arg);
            }
        }

        // TODO: type arguments
        print(")");
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign)
    {
        visitTree(jcAssign.getVariable());
        print(" = ");
        visitTree(jcAssign.getExpression());
        print(";");
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary)
    {
        visitTree(jcBinary.getLeftOperand());
        print(" ");
        visitOperator(jcBinary.getKind());
        print(" ");
        visitTree(jcBinary.getRightOperand());
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock)
    {
        print("\n{");
        ++indentLevel;
        for (JCTree.JCStatement jcStatement : jcBlock.getStatements())
        {
            print("\n");
            visitTree(jcStatement);
        }
        --indentLevel;
        print("\n}");
    }

    @Override
    public void visitBreak(JCTree.JCBreak jcBreak)
    {
        // TODO: label
        print("break;");
    }

    @Override
    public void visitCase(JCTree.JCCase jcCase)
    {
        print("\n");
        if (jcCase.getExpression() == null)
        {
            print("default");
        }
        else
        {
            print("case ");
            visitTree(jcCase.getExpression());
        }
        print(":");

        ++indentLevel;
        for (JCTree.JCStatement statement : jcCase.getStatements())
        {
            print("\n");
            visitTree(statement);
        }
        --indentLevel;
    }

    @Override
    public void visitCatch(JCTree.JCCatch jcCatch)
    {
        print("\ncatch (");
        visitVarDef(jcCatch.getParameter());
        print(")");
        visitBlock(jcCatch.getBlock());
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl)
    {
        print("\n\n");
        visitModifiers(jcClassDecl.getModifiers());
        print("class "+jcClassDecl.getSimpleName());

        if (jcClassDecl.getExtendsClause() != null)
        {
            print(" extends ");
            visitTree(jcClassDecl.getExtendsClause());
        }

        // TODO: generics
        List<JCTree.JCExpression> interfaces = jcClassDecl.getImplementsClause();
        if (interfaces.size() > 0)
        {
            print(" implements ");
            visitTree(interfaces.get(0));
            for (JCTree.JCExpression jcExpression : interfaces.subList(1, interfaces.size()))
            {
                print(", ");
                visitTree(jcExpression);
            }
        }

        nestedClasses.add(jcClassDecl.getSimpleName().toString());
        visitClassMembers(jcClassDecl.getMembers());
        nestedClasses.remove(nestedClasses.size() - 1);
    }

    private void visitClassMembers(List<JCTree> members)
    {
        print("\n{");
        ++indentLevel;
        for (JCTree member : members)
        {
            print("\n");
            visitTree(member);
        }
        --indentLevel;
        print("\n}\n");
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement jcExpressionStatement)
    {
        visitTree(jcExpressionStatement.getExpression());
        print(";");
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop)
    {
        print("for (");
        visitVarDef(jcEnhancedForLoop.getVariable());
        print(" : ");
        visitTree(jcEnhancedForLoop.getExpression());
        print(") ");
        visitTree(jcEnhancedForLoop.getStatement());
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop)
    {
        print("for (");

        List<JCTree.JCStatement> initializers = jcForLoop.getInitializer();
        if (initializers.size() == 1)
        {
            visitTree(initializers.get(0));
        }
        else if (initializers.size() > 1) throw new IllegalArgumentException("Too many initializers: "+initializers.size());

        print(" ");
        visitTree(jcForLoop.getCondition());
        print("; ");

        List<JCTree.JCExpressionStatement> updates = jcForLoop.getUpdate();
        if (updates.size() == 1)
        {
            visitTree(updates.get(0).getExpression());
        }
        else if (initializers.size() > 1) throw new IllegalArgumentException("Too many initializers: "+initializers.size());

        print(") ");

        visitTree(jcForLoop.getStatement());
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent)
    {
        print(jcIdent.getName());
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf)
    {
        print("if ");
        visitTree(jcIf.getCondition());
        print(" ");

        visitTree(jcIf.getThenStatement());

        if (jcIf.getElseStatement() != null)
        {
            print("\nelse ");
            visitTree(jcIf.getElseStatement());
        }
    }

    @Override
    public void visitImport(JCTree.JCImport jcImport)
    {
        print("\n");
        if (jcImport.isStatic()) System.out.print("static ");
        print("import ");
        visitTree(jcImport.getQualifiedIdentifier());
        print(";");
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral jcLiteral)
    {
        print(jcLiteral.toString());
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl)
    {
        print("\n");
        visitModifiers(jcMethodDecl.getModifiers());

        if (jcMethodDecl.getReturnType() != null)
        {
            visitTree(jcMethodDecl.getReturnType());
            print(" ");
        }
        if (jcMethodDecl.getName().toString().equals("<init>")) print(getCurrentClass());
        else print(jcMethodDecl.getName());
        print("(");

        List<JCTree.JCVariableDecl> parameters = jcMethodDecl.getParameters();
        if (parameters.size() > 0)
        {
            visitVarDef(parameters.get(0));
            for (JCTree.JCVariableDecl param : parameters.subList(1, parameters.size()))
            {
                print(", ");
                visitVarDef(param);
            }
        }
        print(")");

        List<JCTree.JCExpression> possibleExceptions = jcMethodDecl.getThrows();
        if (possibleExceptions.size() > 0)
        {
            print(" throws ");
            visitTree(possibleExceptions.get(0));
            for (JCTree.JCExpression exception : possibleExceptions.subList(1, possibleExceptions.size()))
            {
                print(", ");
                visitTree(exception);
            }
        }

        // TODO: type parameters, default value
        visitBlock(jcMethodDecl.getBody());
    }

    @Override
    public void visitModifiers(JCTree.JCModifiers jcModifiers)
    {
        // TODO: annotations
        for (Modifier modifier : jcModifiers.getFlags())
        {
            print(modifier.toString() + " ");
        }
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass jcNewClass)
    {
        print("new "+jcNewClass.getIdentifier()+"(");
        List<JCTree.JCExpression> arguments = jcNewClass.getArguments();
        if (arguments.size() > 0)
        {
            visitTree(arguments.get(0));
            for (JCTree.JCExpression arg : arguments.subList(1, arguments.size()))
            {
                print(", ");
                visitTree(arg);
            }
        }
        print(")");

        if (jcNewClass.getClassBody() != null)
        {
            visitClassMembers(jcNewClass.getClassBody().getMembers());
        }

        // TODO: TypeArguments, EnclosingExpression
    }

    private void visitOperator(Tree.Kind kind)
    {
        switch (kind)
        {
            case PLUS: print("+"); break;
            case MINUS: print("-"); break;
            case MULTIPLY: print("*"); break;
            case DIVIDE: print("/"); break;
            case REMAINDER: print("%"); break;
            case LESS_THAN: print("<"); break;
            case GREATER_THAN: print(">"); break;
            case LESS_THAN_EQUAL: print("<="); break;
            case GREATER_THAN_EQUAL: print(">="); break;
            case EQUAL_TO: print("=="); break;
            case NOT_EQUAL_TO: print("!="); break;
            case AND: print("&&"); break;
//                    case XOR: print("+"); break;
            case OR: print("||"); break;
//                    case CONDITIONAL_AND: print("+"); break;
//                    case CONDITIONAL_OR: print("+"); break;
            case MULTIPLY_ASSIGNMENT: print("*="); break;
            case DIVIDE_ASSIGNMENT: print("/="); break;
            case REMAINDER_ASSIGNMENT: print("%="); break;
            case PLUS_ASSIGNMENT: print("+="); break;
            case MINUS_ASSIGNMENT: print("-="); break;
//                    case LEFT_SHIFT_ASSIGNMENT: print("+"); break;
//                    case RIGHT_SHIFT_ASSIGNMENT: print("+"); break;
//                    case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT: print("+"); break;
//                    case AND_ASSIGNMENT: print("+"); break;
//                    case XOR_ASSIGNMENT: print("+"); break;
//                    case OR_ASSIGNMENT: print("+"); break;
            case PREFIX_INCREMENT: print("++"); break;
            case PREFIX_DECREMENT: print("--"); break;
            default: throw new IllegalArgumentException("Unrecognized operator "+kind.toString());
        }
    }

    @Override
    public void visitParens(JCTree.JCParens jcParens)
    {
        print("(");
        visitTree(jcParens.getExpression());
        print(")");
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn)
    {
        print("\nreturn ");
        visitTree(jcReturn.getExpression());
        print(";");
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess)
    {
        visitTree(jcFieldAccess.selected);
        print("."+jcFieldAccess.getIdentifier());
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch jcSwitch)
    {
        print("switch ");
        visitTree(jcSwitch.getExpression());
        print("\n{");
        ++indentLevel;

        for (JCTree.JCCase jcCase : jcSwitch.getCases())
        {
            visitCase(jcCase);
        }

        --indentLevel;
        print("\n}");
    }

    @Override
    public void visitThrow(JCTree.JCThrow jcThrow)
    {
        print("throw ");
        visitTree(jcThrow.getExpression());
        print(";");
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit compilationUnit)
    {
        // TODO: package annotations
        print("package " + compilationUnit.getPackageName() + ";\n");
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
            case ASSIGNMENT: this.visitAssign((JCTree.JCAssign) jcTree); break;
            case ARRAY_TYPE: this.visitTypeArray((JCTree.JCArrayTypeTree) jcTree); break;
            case BLOCK: this.visitBlock((JCTree.JCBlock) jcTree); break;
            case BREAK: this.visitBreak((JCTree.JCBreak) jcTree); break;
            case CLASS: this.visitClassDef((JCTree.JCClassDecl) jcTree); break;
            case ENHANCED_FOR_LOOP: this.visitForeachLoop((JCTree.JCEnhancedForLoop) jcTree); break;
            case EMPTY_STATEMENT: break;
            case EXPRESSION_STATEMENT: this.visitExec((JCTree.JCExpressionStatement) jcTree); break;
            case FOR_LOOP: this.visitForLoop((JCTree.JCForLoop) jcTree); break;
            case IDENTIFIER: this.visitIdent((JCTree.JCIdent) jcTree); break;
            case IF: this.visitIf((JCTree.JCIf) jcTree); break;
            case INSTANCE_OF: this.visitTypeTest((JCTree.JCInstanceOf) jcTree); break;
            case MEMBER_SELECT: this.visitSelect((JCTree.JCFieldAccess) jcTree); break;
            case METHOD: this.visitMethodDef((JCTree.JCMethodDecl) jcTree); break;
            case METHOD_INVOCATION: this.visitApply((JCTree.JCMethodInvocation) jcTree); break;
            case MODIFIERS: this.visitModifiers((JCTree.JCModifiers) jcTree); break;
            case NEW_CLASS: this.visitNewClass((JCTree.JCNewClass) jcTree); break;
            case PARAMETERIZED_TYPE: this.visitTypeApply((JCTree.JCTypeApply) jcTree); break;
            case PARENTHESIZED: this.visitParens((JCTree.JCParens) jcTree); break;
            case PRIMITIVE_TYPE: this.visitTypeIdent((JCTree.JCPrimitiveTypeTree) jcTree); break;
            case RETURN: this.visitReturn((JCTree.JCReturn) jcTree); break;
            case SWITCH: this.visitSwitch((JCTree.JCSwitch) jcTree); break;
            case THROW: this.visitThrow((JCTree.JCThrow) jcTree); break;
            case TRY: this.visitTry((JCTree.JCTry) jcTree); break;
            case TYPE_CAST: this.visitTypeCast((JCTree.JCTypeCast) jcTree); break;
            case VARIABLE: this.visitVarDef((JCTree.JCVariableDecl) jcTree); break;

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

            case PREFIX_INCREMENT:
            case PREFIX_DECREMENT:
                this.visitUnary((JCTree.JCUnary) jcTree);
                break;

            default:
                throw new IllegalArgumentException("Unhandled kind " + jcTree.getKind().toString());
//                    case ANNOTATION:
//                        break;
//                    case ARRAY_ACCESS:
//                        break;
//                    case ASSERT:
//                        break;
//                    case BREAK:
//                        break;
//                    case CATCH:
//                        break;
//                    case COMPILATION_UNIT:
//                        break;
//                    case CONDITIONAL_EXPRESSION:
//                        break;
//                    case CONTINUE:
//                        break;
//                    case DO_WHILE_LOOP:
//                        break;
//                    case EXPRESSION_STATEMENT:
//                        break;
//                    case IMPORT:
//                        break;
//                    case LABELED_STATEMENT:
//                        break;
//                    case NEW_ARRAY:
//                        break;
//                    case EMPTY_STATEMENT:
//                        break;
//                    case SYNCHRONIZED:
//                        break;
//                    case PARAMETERIZED_TYPE:
//                        break;
//                    case TYPE_PARAMETER:
//                        break;
//                    case WHILE_LOOP:
//                        break;
//                    case POSTFIX_INCREMENT:
//                        break;
//                    case POSTFIX_DECREMENT:
//                        break;
//                    case UNARY_PLUS:
//                        break;
//                    case UNARY_MINUS:
//                        break;
//                    case BITWISE_COMPLEMENT:
//                        break;
//                    case LOGICAL_COMPLEMENT:
//                        break;
//                    case LEFT_SHIFT:
//                        break;
//                    case RIGHT_SHIFT:
//                        break;
//                    case UNSIGNED_RIGHT_SHIFT:
//                        break;
//                    case UNBOUNDED_WILDCARD:
//                        break;
//                    case EXTENDS_WILDCARD:
//                        break;
//                    case SUPER_WILDCARD:
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
        print("\ntry");
        visitBlock(jcTry.getBlock());

        for (JCTree.JCCatch jcCatch : jcTry.getCatches())
        {
            visitCatch(jcCatch);
        }

        if (jcTry.getFinallyBlock() != null)
        {
            print("\nfinally");
            visitBlock(jcTry.getFinallyBlock());
        }
    }

    @Override
    public void visitTypeApply(JCTree.JCTypeApply jcTypeApply)
    {
        visitTree(jcTypeApply.getType());
        print("<");
        List<JCTree.JCExpression> typeArguments = jcTypeApply.getTypeArguments();
        visitTree(typeArguments.get(0));
        for (JCTree.JCExpression jcExpression : typeArguments.subList(1, typeArguments.size()))
        {
            print(", ");
            visitTree(jcExpression);
        }
        print(">");
    }

    @Override
    public void visitTypeArray(JCTree.JCArrayTypeTree jcArrayTypeTree)
    {
        visitTree(jcArrayTypeTree.getType());
        print("[]");
    }

    @Override
    public void visitTypeCast(JCTree.JCTypeCast jcTypeCast)
    {
        print("(");
        visitTree(jcTypeCast.getType());
        print(") ");
        visitTree(jcTypeCast.getExpression());
    }

    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree jcPrimitiveTypeTree)
    {
        print(jcPrimitiveTypeTree.getPrimitiveTypeKind());
    }

    @Override
    public void visitTypeTest(JCTree.JCInstanceOf jcInstanceOf)
    {
        visitTree(jcInstanceOf.getExpression());
        print(" instanceof ");
        visitTree(jcInstanceOf.getType());
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary)
    {
        visitOperator(jcUnary.getKind());
        visitTree(jcUnary.getExpression());
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl)
    {
        visitModifiers(jcVariableDecl.getModifiers());
        visitTree(jcVariableDecl.vartype);
        print(" "+jcVariableDecl.getName());

        // TODO: is this right?
        if (jcVariableDecl.getInitializer() != null)
        {
            print(" = ");
            visitTree(jcVariableDecl.getInitializer());
            print(";");
        }
    }

}
