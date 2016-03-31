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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlFile
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.indices.MavenArtifactSearchDialog
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.IntentionActionPriority
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import java.util.*

class KotlinMavenUnresolvedReferenceQuickFixProvider : UnresolvedReferenceQuickFixProvider<KtSimpleNameReference>() {
    override fun getReferenceClass(): Class<KtSimpleNameReference>  = KtSimpleNameReference::class.java

    override fun registerFixes(ref: KtSimpleNameReference, registrar: QuickFixActionRegistrar) {
        val module = ModuleUtilCore.findModuleForPsiElement(ref.expression) ?: return
        if (!MavenProjectsManager.getInstance(module.project).isMavenizedModule(module)) {
            return
        }

        val name = ref.expression.getReferencedName()
        registrar.register(AddMavenDependencyQuickFix(name, ref.expression.createSmartPointer()))
    }
}

class AddMavenDependencyQuickFix(val className: String, val smartPsiElementPointer: SmartPsiElementPointer<KtSimpleNameExpression>) : IntentionAction, LowPriorityAction {
    override fun getText() = "Add dependency..."
    override fun getFamilyName() = "Kotlin"
    override fun startInWriteAction() = false
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) =
            smartPsiElementPointer.element.let { it != null && it.isValid } && file != null && MavenDomUtil.findContainingProject(file) != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) {
            return
        }

        val virtualFile = file.originalFile.virtualFile ?: return
        val mavenProject = MavenDomUtil.findContainingProject(file) ?: return
        val xmlFile = PsiManager.getInstance(project).findFile(mavenProject.file) as? XmlFile ?: return

        val ids = MavenArtifactSearchDialog.searchForClass(project, className)
        if (ids.isEmpty()) return

        runWriteAction {
            val isTestSource = ProjectRootManager.getInstance(project).fileIndex.isInTestSourceContent(virtualFile)
            val scope = if (isTestSource) MavenArtifactScope.TEST else null

            val pom = PomFile(xmlFile)

            ids.forEach {
                pom.addDependency(it, scope)
            }
        }
    }
}

object PlatformUnresolvedProvider : KotlinIntentionActionFactoryWithDelegate<KtNameReferenceExpression, String>() {
    override fun getElementOfInterest(diagnostic: Diagnostic) = QuickFixUtil.getParentElementOfType(diagnostic, KtNameReferenceExpression::class.java)
    override fun extractFixData(element: KtNameReferenceExpression, diagnostic: Diagnostic) = element.getReferencedName()

    override fun createFixes(originalElementPointer: SmartPsiElementPointer<KtNameReferenceExpression>, diagnostic: Diagnostic, quickFixDataFactory: () -> String?): List<QuickFixWithDelegateFactory> {
        val result = ArrayList<QuickFixWithDelegateFactory>()

        originalElementPointer.element?.references?.filterIsInstance<KtSimpleNameReference>()?.firstOrNull()?.let { reference ->
            UnresolvedReferenceQuickFixProvider.registerReferenceFixes(reference, object: QuickFixActionRegistrar {
                override fun register(action: IntentionAction) {
                    result.add(QuickFixWithDelegateFactory(IntentionActionPriority.LOW) { action } )
                }

                override fun register(fixRange: TextRange, action: IntentionAction, key: HighlightDisplayKey?) {
                    register(action)
                }

                override fun unregister(condition: Condition<IntentionAction>) {
                }
            })
        }

        return result
    }

}
