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
 * val movieView = MovieView::class singleOf movieId
 * val movieViews = MovieView::class multiOf movieIds
 * val movieView = MovieView::class singleOf movie
 * val movieViews = MovieView::class multiOf movies
 *
 * 写法2 java语法友好：buildSingle & buildMulti
 * val movieView = buildSingle(MovieView::class, movie)
 * val movieViews = buildMulti(MovieView::class, movies)
 * val movieView = buildSingle(MovieView::class, movieId)
 * val movieViews = buildMulti(MovieView::class, movieIds)
 *
 * @author liurenpeng
 * Created on 2020-06-04
 */

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
infix fun <T: Any, IOA> KClass<T>.singleOf(source: IOA): T? =
    buildSingle(this, source)

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
infix fun <T: Any, IOA> KClass<T>.multiOf(source: Collection<IOA>): Collection<T> =
    buildMulti(this, source)

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
fun <T : Any, IOA> buildSingle(clazz: KClass<T>, source: IOA): T? =
    ModelBuilder() buildSingle clazz by source

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
fun <T : Any, IOA> buildMulti(clazz: KClass<T>, sources: Collection<IOA>) : Collection<T> =
    ModelBuilder() buildMulti clazz by sources

internal infix fun <T : Any> ModelBuilder.buildSingle(clazz: KClass<T>) =
    BuildSinglePair(this, clazz)

internal infix fun <T : Any> ModelBuilder.buildMulti(clazz: KClass<T>) =
    BuildMultiPair(this, clazz)

internal infix fun <T : Any, IOA> BuildSinglePair<KClass<T>>.by(source: IOA): T? =
    (this.modelBuilder buildMulti this.targetClazz by singleton(source)).firstOrNull()

internal infix fun <T : Any, IOA> BuildMultiPair<KClass<T>>.by(sources: Collection<IOA>) : Collection<T> =
    if (CollectionUtil.isEmpty(sources)) emptyList() else buildTargets(this, sources)

