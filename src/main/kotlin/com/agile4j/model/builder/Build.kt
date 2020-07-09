package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildMultiPair
import com.agile4j.model.builder.build.BuildSinglePair
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildTargets
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

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
infix fun <T : Any, IOA> BuildSinglePair<KClass<T>>.by(source: IOA): T? =
    (this.modelBuilder buildMulti this.targetClazz by singleton(source)).firstOrNull()

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
infix fun <T : Any, IOA> BuildMultiPair<KClass<T>>.by(sources: Collection<IOA>) : Collection<T> =
    if (CollectionUtil.isEmpty(sources)) emptyList() else buildTargets(this, sources)

