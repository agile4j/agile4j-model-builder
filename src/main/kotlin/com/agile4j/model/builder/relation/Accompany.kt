package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.assertCanBeA
import com.agile4j.model.builder.build.BuildContext.assertCanBeT
import com.agile4j.model.builder.exception.ModelBuildException
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

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

    this.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == aClazz.createType() }
        .findFirst()
        .orElseThrow { ModelBuildException(
            "no suitable constructor found for targetClass: $this. accompanyClass:$aClazz") }

    BuildContext.putTToA(this, aClazz)
}