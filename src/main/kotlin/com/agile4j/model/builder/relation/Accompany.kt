package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.assertCanBeA
import com.agile4j.model.builder.build.BuildContext.assertCanBeT
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.getConstructor
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * T        target
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <T: Any, A: Any> KClass<T>.accompanyBy(aClazz: KClass<A>) {
    assertCanBeT(this)
    assertCanBeA(aClazz)

    assertTargetConstructor(this, aClazz)
    BuildContext.putTToA(this, aClazz)
}

private fun assertTargetConstructor(tClazz: KClass<*>, aClazz: KClass<*>) =
    getConstructor(tClazz, aClazz) ?: err(
        "no suitable constructor found for targetClass: $tClazz. accompanyClass:$aClazz")