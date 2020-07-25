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
 * 有2种写法：
 *
 * 写法1 kotlin语法友好：mapSingle & mapMulti
 * val movieView = movieId mapSingle MovieView::class
 * val movieViews = movieIds mapMulti MovieView::class
 * val movieView = movie mapSingle MovieView::class
 * val movieViews = movies mapMulti MovieView::class
 *
 * 写法2 java语法友好：buildSingle & buildMulti
 * val movieView = buildSingle(MovieView::class, movie)
 * val movieViews = buildMulti(MovieView::class, movies)
 * val movieView = buildSingle(MovieView::class, movieId)
 * val movieViews = buildMulti(MovieView::class, movieIds)
 *
 * abbreviations:
 * T        target
 * IXA      indexOrAccompany
 *
 * @author liurenpeng
 * Created on 2020-06-04
 */

infix fun <T: Any, IXA: Any> IXA.mapSingle(clazz: KClass<T>): T? =
    buildSingle(clazz, this)

infix fun <T: Any, IXA: Any> Collection<IXA>.mapMulti(clazz: KClass<T>): Collection<T> =
    buildMulti(clazz, this)

fun <T : Any, IXA: Any> buildSingle(clazz: KClass<T>, source: IXA): T? =
    ModelBuilder() buildSingle clazz by source

fun <T : Any, IXA: Any> buildMulti(clazz: KClass<T>, sources: Collection<IXA>) : Collection<T> =
    ModelBuilder() buildMulti clazz by sources

internal infix fun <T: Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

internal infix fun <T: Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

internal infix fun <T: Any, IXA: Any> BuildSinglePair<KClass<T>>.by(source: IXA): T? =
    (this.modelBuilder buildMulti this.targetClazz by singleton(source)).firstOrNull()

internal infix fun <T: Any, IXA: Any> BuildMultiPair<KClass<T>>.by(sources: Collection<IXA>) : Collection<T> =
    if (CollectionUtil.isEmpty(sources)) emptyList()
    else buildTargets(this.modelBuilder, this.targetClazz, sources)

