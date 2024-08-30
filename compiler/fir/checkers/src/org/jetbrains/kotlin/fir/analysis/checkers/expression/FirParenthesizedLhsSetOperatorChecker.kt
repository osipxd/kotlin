/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes.PARENTHESIZED
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirParenthesizedLhsSetOperatorChecker : FirFunctionCallChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callee = expression.calleeReference

        if (callee.isSetOperator) {
            val lastArgument = expression.arguments.lastOrNull()

            // For `(a[0]) = ...` where `a: Array<A>`
            val source = expression.source?.takeIf { it.elementType == PARENTHESIZED }
            // For `(a[0]) += ""` where `a: Array<A>`
                ?: callee.source?.takeIf { lastArgument?.getAsGetFunctionCallIn(expression)?.explicitReceiver?.source?.elementType == PARENTHESIZED }
                // For `(a[0])++` where `a` has `get`,`set` and `inc` operators
                ?: expression.receiverCalleeSource?.takeIf { it.elementType == PARENTHESIZED && callee.isIncrementOrDecrement }
                ?: return

            reporter.reportOn(source, FirErrors.WRAPPED_LHS_IN_ASSIGNMENT, context)
        }
    }

    private val FirFunctionCall.receiverCalleeSource get() = (explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference?.source

    private fun FirExpression.getAsGetFunctionCallIn(expression: FirFunctionCall): FirFunctionCall? =
        (this as? FirFunctionCall)?.takeIf {
            // If the expression is desugared, the last argument is definitely a call like `plus()`, `minus()`, etc.
            expression.calleeReference.source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign
        }

    private val FirNamedReference.isIncrementOrDecrement: Boolean
        get() = source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement

    private val FirNamedReference.isSetOperator: Boolean
        get() = name == OperatorNameConventions.SET && source?.kind.let {
            it == KtFakeSourceElementKind.ArrayAccessNameReference
                    || it is KtFakeSourceElementKind.DesugaredIncrementOrDecrement
                    || it is KtFakeSourceElementKind.DesugaredAugmentedAssign
        }
}