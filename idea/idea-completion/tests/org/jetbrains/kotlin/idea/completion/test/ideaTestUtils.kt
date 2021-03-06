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

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import java.io.File

fun CodeInsightTestFixture.configureWithExtraFileAbs(path: String, vararg extraNameParts: String) {
    configureWithExtraFile(path, *extraNameParts, relativePaths = false)
}

fun CodeInsightTestFixture.configureWithExtraFile(path: String, vararg extraNameParts: String = arrayOf(".Data"), relativePaths: Boolean = false) {
    fun String.toFile(): File = if (relativePaths) File(testDataPath, this) else File(this)

    val noExtensionPath = FileUtil.getNameWithoutExtension(path)
    val extensions = arrayOf("kt", "java")
    val extraPaths: List<String> = extraNameParts
            .flatMap { extensions.map { ext -> "$noExtensionPath$it.$ext" } }
            .filter { it.toFile().exists() }

    configureByFiles(*(listOf(path) + extraPaths).toTypedArray())
}
