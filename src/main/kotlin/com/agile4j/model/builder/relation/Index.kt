package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.assertCanBeA
import com.agile4j.model.builder.build.BuildContext.assertCanBeI
import com.agile4j.model.builder.utils.nonNullReturnKClazz
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <A: Any, I> KClass<A>.indexBy(indexer: (A) -> I) {
    val iClazz = indexer.nonNullReturnKClazz
    assertCanBeA(this)
    assertCanBeI(iClazz)

    BuildContext.aToIHolder[this] = iClazz
    BuildContext.indexerHolder[this] = indexer
}