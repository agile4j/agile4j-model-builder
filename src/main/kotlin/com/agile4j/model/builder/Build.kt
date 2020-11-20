package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildMultiPair
import com.agile4j.model.builder.build.BuildSinglePair
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildIToAByIs
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.build.buildTargetMapOfA
import com.agile4j.model.builder.build.buildTargetMapOfI
import com.agile4j.model.builder.build.buildTargets
import com.agile4j.model.builder.build.cacheAndGetUnNullIToA
import com.agile4j.model.builder.build.filterTargetValueMap
import com.agile4j.model.builder.build.filterTargets
import com.agile4j.model.builder.build.modelBuilder
import com.agile4j.model.builder.build.targetClazz
import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.utils.util.CollectionUtil
import java.util.Collections.singleton
import kotlin.reflect.KClass

/**
 * 1. API：
 *
 * 1). mapSingle & mapMulti
 * val movieView: MovieView = movieId mapSingle MovieView::class
 * val movieView: MovieView = movie mapSingle MovieView::class
 * val movieViews: Collection<MovieView> = movieIds mapMulti MovieView::class
 * val movieViews: Collection<MovieView> = movies mapMulti MovieView::class
 *
 * 2). buildSingle & buildMulti
 * val movieView: MovieView = buildSingle(MovieView::class, movieId)
 * val movieView: MovieView = buildSingle(MovieView::class, movie)
 * val movieViews: Collection<MovieView> = buildMulti(MovieView::class, movieIds)
 * val movieViews: Collection<MovieView> = buildMulti(MovieView::class, movies)
 *
 * 3). buildMapOfI & buildMapOfA
 * val idToViewMap: Map<Long, MovieView> = buildMapOfI(MovieView::class, movieIds)
 * val idToViewMap: Map<Long, MovieView> = buildMapOfI(MovieView::class, movies)
 * val movieToViewMap: Map<Movie, MovieView> = buildMapOfA(MovieView::class, movieIds)
 * val movieToViewMap: Map<Movie, MovieView> = buildMapOfA(MovieView::class, movies)
 *
 * 2. build结果的顺序与sources一致，且滤掉了其中值为null的target
 *
 * 3. abbreviations:
 * T        target
 * IXA      index or accompany
 *
 * @author liurenpeng
 * Created on 2020-06-04
 */

infix fun <T: Any, IXA: Any> IXA?.mapSingle(clazz: Class<T>): T? =
    buildSingle(clazz, this)

infix fun <T: Any, IXA: Any> IXA?.mapSingle(clazz: KClass<T>): T? =
    buildSingle(clazz, this)

infix fun <T: Any, IXA: Any> Collection<IXA?>.mapMulti(clazz: Class<T>): List<T> =
    buildMulti(clazz, this)

infix fun <T: Any, IXA: Any> Collection<IXA?>.mapMulti(clazz: KClass<T>): List<T> =
    buildMulti(clazz, this)



fun <T : Any, IXA: Any> buildSingle(clazz: Class<T>, source: IXA?): T? =
    ModelBuilder() buildSingle clazz.kotlin by source

fun <T : Any, IXA: Any> buildSingle(clazz: KClass<T>, source: IXA?): T? =
    ModelBuilder() buildSingle clazz by source

fun <T : Any, IXA: Any> buildMulti(clazz: Class<T>, sources: Collection<IXA?>) : List<T> =
    ModelBuilder() buildMulti clazz.kotlin by sources

fun <T : Any, IXA: Any> buildMulti(clazz: KClass<T>, sources: Collection<IXA?>) : List<T> =
    ModelBuilder() buildMulti clazz by sources

fun <T : Any, IXA: Any> buildMulti(
    clazz: Class<T>, sources: Collection<IXA?>, filter: (T) -> Boolean): List<T> =
    ModelBuilder() buildMulti clazz.kotlin by sources filterBy filter

fun <T : Any, IXA: Any> buildMulti(
    clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : List<T> =
    ModelBuilder() buildMulti clazz by sources filterBy filter


fun <T : Any, IXA: Any> buildMapOfI(clazz: KClass<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfI(ModelBuilder(), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfI(clazz: Class<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfI(ModelBuilder(), clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfA(clazz: KClass<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfA(ModelBuilder(), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfA(clazz: Class<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfA(ModelBuilder(), clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfI(
    clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfI(ModelBuilder(), clazz, sources), filter)

fun <T : Any, IXA: Any> buildMapOfI(
    clazz: Class<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfI(ModelBuilder(), clazz.kotlin, sources), filter)

fun <T : Any, IXA: Any> buildMapOfA(
    clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfA(ModelBuilder(), clazz, sources), filter)

fun <T : Any, IXA: Any> buildMapOfA(
    clazz: Class<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfA(ModelBuilder(), clazz.kotlin, sources), filter)



fun <T : Any, IXA: Any> buildSingleWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, source: IXA?): T? =
    ModelBuilder.copyBy(modelBuilder) buildSingle clazz.kotlin by source

fun <T : Any, IXA: Any> buildSingleWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, source: IXA?): T? =
    ModelBuilder.copyBy(modelBuilder) buildSingle clazz by source

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : List<T> =
    ModelBuilder.copyBy(modelBuilder) buildMulti clazz.kotlin by sources

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : List<T> =
    ModelBuilder.copyBy(modelBuilder) buildMulti clazz by sources

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : List<T> =
    ModelBuilder.copyBy(modelBuilder) buildMulti clazz.kotlin by sources filterBy filter

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : List<T> =
    ModelBuilder.copyBy(modelBuilder) buildMulti clazz by sources filterBy filter



fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfI(ModelBuilder.copyBy(modelBuilder), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfI(ModelBuilder.copyBy(modelBuilder), clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfA(ModelBuilder.copyBy(modelBuilder), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : LinkedHashMap<Any, T> =
    buildTargetMapOfA(ModelBuilder.copyBy(modelBuilder), clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>,
    sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfI(ModelBuilder.copyBy(modelBuilder), clazz, sources), filter)

fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>,
    sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfI(ModelBuilder.copyBy(modelBuilder), clazz.kotlin, sources), filter)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>,
    sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfA(ModelBuilder.copyBy(modelBuilder), clazz, sources), filter)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>,
    sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T> =
    filterTargetValueMap(buildTargetMapOfA(ModelBuilder.copyBy(modelBuilder), clazz.kotlin, sources), filter)



infix fun <T: Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

infix fun <T: Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

infix fun <T: Any, IXA: Any> BuildSinglePair<KClass<T>>.by(source: IXA?): T? =
    (this.modelBuilder buildMulti this.targetClazz by singleton(source)).firstOrNull()

infix fun <T: Any, IXA: Any> BuildMultiPair<KClass<T>>.by(sources: Collection<IXA?>) : List<T> =
    buildTargets(this.modelBuilder, this.targetClazz, sources)



/**
 * filter并非单纯过滤，还会把[ModelBuilder]currXXXCache中的I、A、T移除
 * 定义为internal接口，以免被错误使用，例如：
 *
 * var movieViews = movieIds mapMulti MovieView::class
 * movieViews filterBy filter
 * movieViews.XXX // 此时的movieViews，已经"脏了"，里面可能包含"空心"(currXXXCache中的I、A、T已被移除)target
 *
 * 正确的使用姿势是：
 * movieViews = movieViews filterBy filter
 *
 * 或直接一行构建：
 * var movieViews = movieIds mapMulti MovieView::class filterBy filter
 *
 * 开放接口没法保证使用方能正确使用，因此收敛为internalAPI
 *
 * @exception [ModelBuildException] Collection<T>必须是由ModelBuilder构建而来,否则抛出该异常
 */
internal infix fun <T: Any> Collection<T>.filterBy(filter: (T) -> Boolean): List<T> {
    return filterTargets(this, filter)
}



fun <T: Any> extractModelBuilder(target: T): ModelBuilder? = target.buildInModelBuilder

fun <A: Any, I: Any> buildIndexToAccompanyWithExistModelBuilder(
    modelBuilder: ModelBuilder,
    accompanyClass: Class<A>,
    indices: Collection<I>
): LinkedHashMap<I, A> = buildIndexToAccompanyWithExistModelBuilder(
    modelBuilder, accompanyClass.kotlin, indices)

@Suppress("UNCHECKED_CAST")
fun <A: Any, I: Any> buildIndexToAccompanyWithExistModelBuilder(
    originModelBuilder: ModelBuilder,
    accompanyClass: KClass<A>,
    indices: Collection<I>
): LinkedHashMap<I, A> {
    val iToA = LinkedHashMap<I, A>()
    if (CollectionUtil.isEmpty(indices)) return iToA
    val indexClass = indices.first()::class
    if (BuildContext.getIClazzByA(accompanyClass) != indexClass)
        err("unmatched indexClass($indexClass) to accompanyClass($accompanyClass)")

    val modelBuilder = ModelBuilder.copyBy(originModelBuilder)
    val nullableIToA = buildIToAByIs(accompanyClass, indices, modelBuilder)
    val unNullIToA = cacheAndGetUnNullIToA(nullableIToA, accompanyClass, modelBuilder)
    indices.forEach { i -> unNullIToA[i]?.let { iToA[i] = it as A } }
    return iToA
}