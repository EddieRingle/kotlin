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

package org.jetbrains.android.inspections.klint

import com.android.builder.model.AndroidProject
import com.android.builder.model.Variant
import com.android.tools.idea.gradle.util.GradleUtil
import org.jetbrains.android.facet.AndroidFacet
import kotlin.reflect.memberFunctions
import kotlin.reflect.staticFunctions

class AndroidModelFacade(val facet: AndroidFacet) {
    private val model by lazy { loadModel() }

    fun isModelReady() = model != null

    fun getDependsOn(artifact: String): Boolean {
        val model = model ?: return false
        return GradleUtil::class.staticFunctions.firstOrNull {
            val type = it.parameters[0].type.toString()
            it.parameters.size == 2 && "IdeaAndroidProject" in type || "AndroidGradleModel" in type
        }?.call(model, artifact) as? Boolean ?: false
    }

    fun getAndroidProject(): AndroidProject? {
        val model = model ?: return null
        return model.javaClass.kotlin.memberFunctions
                .firstOrNull { it.name == "getDelegate" || it.name == "getAndroidProject" }
                ?.call() as? AndroidProject
    }

    fun getSelectedVariant(): Variant? {
        val model = model ?: return null
        return model.javaClass.kotlin.memberFunctions
                .firstOrNull { it.name == "getSelectedVariant" }
                ?.call() as? Variant
    }

    private fun loadModel(): Any? {
        try {
            val getAndroidProjectInfoFun = AndroidFacet::class.memberFunctions.singleOrNull {
                it.name == "getIdeaAndroidProject" || it.name == "getAndroidModel"
            }

            return getAndroidProjectInfoFun?.call(facet)
        }
        catch(e: Throwable) {
            return false
        }
    }

    fun computePackageName(): String? {
        return getSelectedVariant()?.mainArtifact?.applicationId
    }

}