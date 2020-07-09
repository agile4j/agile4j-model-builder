package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildMultiPair
import com.agile4j.model.builder.build.BuildSinglePair
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildTargets
import com.agile4j.model.builder.build.injectModelBuilder
import com.agile4j.model.builder.build.injectRelation
import com.agile4j.model.builder.build.modelBuilder
import com.agile4j.model.builder.build.targetClazz
import com.agile4j.utils.util.CollectionUtil
import java.util.Collections.singleton
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-04
 */

infix fun <T : Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

infix fun <T : Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

infix fun <T : Any, I> BuildSinglePair<KClass<T>>.by(index: I): T? {
    val coll = this.modelBuilder buildMulti this.targetClazz by singleton(index)
    return if (CollectionUtil.isEmpty(coll)) null else coll.toList()[0]
}

infix fun <T : Any, I> BuildMultiPair<KClass<T>>.by(indies: Collection<I>) : Collection<T> {
    if (CollectionUtil.isEmpty(indies)) {
        return emptyList()
    }
    val targets = buildTargets(this, indies)
    injectModelBuilder(this, targets)
    injectRelation(targets)
    return targets
}
