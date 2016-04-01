/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance

class AddVarianceModifierInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            private fun suggestedVariance(klass: KtClassOrObject, typeParameter: KtTypeParameter, context: BindingContext): Variance? {
                var usedAsIn = false
                var usedAsOut = false
                val parameterDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, typeParameter) ?: return null
                for (declaration in klass.declarations) {
                    val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration) ?: return null
                    when (declaration) {
                        is KtFunction -> {
                            val functionDescriptor = descriptor as? FunctionDescriptor ?: return null
                            if (functionDescriptor.returnType?.constructor?.declarationDescriptor == parameterDescriptor) {
                                usedAsOut = true
                            }
                            for (valueParameterDescriptor in functionDescriptor.valueParameters) {
                                if (valueParameterDescriptor.type.constructor.declarationDescriptor == parameterDescriptor) {
                                    usedAsIn = true
                                }
                            }
                        }
                        is KtProperty -> {
                            val propertyDescriptor = descriptor as? PropertyDescriptor ?: return null
                            if (propertyDescriptor.returnType?.constructor?.declarationDescriptor == parameterDescriptor) {
                                usedAsOut = true
                                if (propertyDescriptor.isVar) {
                                    usedAsIn = true
                                }
                            }
                        }
                        else -> return null
                    }
                    if (usedAsIn && usedAsOut) return null
                }
                return if (usedAsIn) Variance.IN_VARIANCE else if (usedAsOut) Variance.OUT_VARIANCE else null
            }

            override fun visitClassOrObject(klass: KtClassOrObject) {
                val context = klass.analyzeFully()
                for (typeParameter in klass.typeParameters) {
                    if (typeParameter.variance != Variance.INVARIANT) continue
                    val suggested = suggestedVariance(klass, typeParameter, context)
                    if (suggested != null && suggested != Variance.INVARIANT) {
                        holder.registerProblem(
                                typeParameter,
                                "Type parameter can have '$suggested' variance",
                                ProblemHighlightType.WEAK_WARNING,
                                AddVarianceFix(suggested)
                        )
                    }
                }
            }
        }
    }

    class AddVarianceFix(val variance: Variance) : LocalQuickFix {
        override fun getName() = "Add '$variance' variance"

        override fun getFamilyName() = "Add variance"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val typeParameter = descriptor.psiElement as? KtTypeParameter
                                ?: throw AssertionError("Add variance fix is used on ${descriptor.psiElement.text}")
            addModifier(typeParameter, if (variance == Variance.IN_VARIANCE) KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD)
        }

    }

}