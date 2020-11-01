package com.agile4j.model.builder

import com.agile4j.model.builder.build.BuildMultiPair
import com.agile4j.model.builder.build.BuildSinglePair
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.build.buildTargetMapOfA
import com.agile4j.model.builder.build.buildTargetMapOfI
import com.agile4j.model.builder.build.buildTargets
import com.agile4j.model.builder.build.modelBuilder
import com.agile4j.model.builder.build.targetClazz
import java.util.Collections.singleton
import kotlin.reflect.KClass

/**
 * 1. 有多种API：
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
 * 2. 所有API build的结果顺序性都与sources一致，且滤掉了其中值为null的target
 * 返回类型为Collection的API，对应实现类型为List
 * 返回类型为Map的API，对应实现类型为LinkedHashMap
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

infix fun <T: Any, IXA: Any> Collection<IXA?>.mapMulti(clazz: Class<T>): Collection<T> =
    buildMulti(clazz, this)

infix fun <T: Any, IXA: Any> IXA?.mapSingle(clazz: KClass<T>): T? =
    buildSingle(clazz, this)

infix fun <T: Any, IXA: Any> Collection<IXA?>.mapMulti(clazz: KClass<T>): Collection<T> =
    buildMulti(clazz, this)



fun <T : Any, IXA: Any> buildSingle(clazz: Class<T>, source: IXA?): T? =
    ModelBuilder() buildSingle clazz.kotlin by source

fun <T : Any, IXA: Any> buildMulti(clazz: Class<T>, sources: Collection<IXA?>) : Collection<T> =
    ModelBuilder() buildMulti clazz.kotlin by sources

fun <T : Any, IXA: Any> buildSingle(clazz: KClass<T>, source: IXA?): T? =
    ModelBuilder() buildSingle clazz by source

fun <T : Any, IXA: Any> buildMulti(clazz: KClass<T>, sources: Collection<IXA?>) : Collection<T> =
    ModelBuilder() buildMulti clazz by sources



fun <T : Any, IXA: Any> buildMapOfI(clazz: KClass<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfI(ModelBuilder(), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfI(clazz: Class<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfI(ModelBuilder(), clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfA(clazz: KClass<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfA(ModelBuilder(), clazz, sources)

fun <T : Any, IXA: Any> buildMapOfA(clazz: Class<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfA(ModelBuilder(), clazz.kotlin, sources)



fun <T : Any, IXA: Any> buildSingleWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, source: IXA?): T? =
    modelBuilder buildSingle clazz.kotlin by source

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : Collection<T> =
    modelBuilder buildMulti clazz.kotlin by sources

fun <T : Any, IXA: Any> buildSingleWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, source: IXA?): T? =
    modelBuilder buildSingle clazz by source

fun <T : Any, IXA: Any> buildMultiWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : Collection<T> =
    modelBuilder buildMulti clazz by sources



fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfI(modelBuilder, clazz, sources)

fun <T : Any, IXA: Any> buildMapOfIWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfI(modelBuilder, clazz.kotlin, sources)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: KClass<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfA(modelBuilder, clazz, sources)

fun <T : Any, IXA: Any> buildMapOfAWithExistModelBuilder(
    modelBuilder: ModelBuilder, clazz: Class<T>, sources: Collection<IXA?>) : Map<Any, T> =
    buildTargetMapOfA(modelBuilder, clazz.kotlin, sources)



infix fun <T: Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

infix fun <T: Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

infix fun <T: Any, IXA: Any> BuildSinglePair<KClass<T>>.by(source: IXA?): T? =
    (this.modelBuilder buildMulti this.targetClazz by singleton(source)).firstOrNull()

infix fun <T: Any, IXA: Any> BuildMultiPair<KClass<T>>.by(sources: Collection<IXA?>) : Collection<T> =
    buildTargets(this.modelBuilder, this.targetClazz, sources)



fun <T: Any> extractModelBuilder(target: T): ModelBuilder? = target.buildInModelBuilder