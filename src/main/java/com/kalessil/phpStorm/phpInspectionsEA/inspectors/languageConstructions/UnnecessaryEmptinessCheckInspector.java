package com.kalessil.phpStorm.phpInspectionsEA.inspectors.languageConstructions;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import com.jetbrains.php.lang.psi.elements.PhpEmpty;
import com.jetbrains.php.lang.psi.elements.PhpIsset;
import com.jetbrains.php.lang.psi.elements.UnaryExpression;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.OpenapiTypesUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.PhpLanguageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/*
 * This file is part of the Php Inspections (EA Extended) package.
 *
 * (c) Vladimir Reznichenko <kalessil@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

public class UnnecessaryEmptinessCheckInspector extends BasePhpInspection {
    private static final String message = "...";

    @NotNull
    public String getShortName() {
        return "UnnecessaryEmptinessCheckInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            @Override
            public void visitPhpBinaryExpression(@NotNull BinaryExpression expression) {
                final IElementType operator = expression.getOperationType();
                if (operator == PhpTokenTypes.opAND || operator == PhpTokenTypes.opAND) {
                    /* false-positives: part of another condition */
                    final PsiElement parent = expression.getParent();
                    if (parent instanceof BinaryExpression && ((BinaryExpression) parent).getOperationType() == operator) {
                        return;
                    }

                    final HashMap<PsiElement, List<PsiElement>> grouping = this.group(this.extract(expression, operator));
                    /* expression => contexts map build, process expressions with 2+ contexts */
                }
            }

            @NotNull
            private HashMap<PsiElement, List<PsiElement>> group(@NotNull List<PsiElement> parts) {
                final HashMap<PsiElement, List<PsiElement>> result = new HashMap<>();
                for (final PsiElement condition : parts) {
                    PsiElement context          = null;
                    List<PsiElement> expression = new ArrayList<>();
                    /* populate expression to contexts mapping input */
                    if (condition instanceof PhpEmpty) {
                        final PhpEmpty empty         = (PhpEmpty) condition;
                        final PsiElement[] arguments = empty.getVariables();

                    } else if (condition instanceof PhpIsset) {
                        final PhpIsset isset         = (PhpIsset) condition;
                        final PsiElement[] arguments = isset.getVariables();

                    } else if (condition instanceof BinaryExpression) {
                        final BinaryExpression binary = (BinaryExpression) condition;
                        final IElementType operation  = binary.getOperationType();
                        if (OpenapiTypesUtil.tsCOMPARE_EQUALITY_OPS.contains(operation)) {
                            final PsiElement left  = binary.getLeftOperand();
                            final PsiElement right = binary.getRightOperand();
                            if (right != null && PhpLanguageUtil.isNull(left)) {
                                context = binary;
                                expression.add(right);
                            } else if (left != null && PhpLanguageUtil.isNull(right)) {
                                context = binary;
                                expression.add(left);
                            }
                        }
                    } else {
                        context = condition;
                        expression.add(condition);
                    }
                    /* perform expression to contexts mapping */
                    if (context != null && !expression.isEmpty()) {

                    }
                }
                parts.clear();
                return result;
            }

            @NotNull
            private List<PsiElement> extract(@NotNull BinaryExpression binary, @Nullable IElementType operator) {
                final List<PsiElement> result = new ArrayList<>();
                if (binary.getOperationType() == operator) {
                    Stream.of(binary.getLeftOperand(), binary.getRightOperand())
                        .filter(Objects::nonNull).map(ExpressionSemanticUtil::getExpressionTroughParenthesis)
                        .forEach(expression -> {
                            if (expression instanceof BinaryExpression) {
                                result.addAll(extract((BinaryExpression) expression, operator));
                            } else if (expression instanceof UnaryExpression) {
                                final UnaryExpression unary = (UnaryExpression) expression;
                                if (OpenapiTypesUtil.is(unary.getOperation(), PhpTokenTypes.opNOT)) {
                                    result.add(ExpressionSemanticUtil.getExpressionTroughParenthesis(unary.getValue()));
                                } else {
                                    result.add(unary);
                                }
                            } else {
                                result.add(expression);
                            }
                        });
                } else {
                    result.add(binary);
                }
                return result;
            }
        };
    }
}
