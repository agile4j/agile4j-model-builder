package com.agile4j.model.builder

import com.agile4j.model.builder.CurrentScopeKeys.visitor
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.getMovieById
import com.agile4j.model.builder.mock.getMovieByIds
import com.agile4j.model.builder.mock.initModelRelation
import com.agile4j.model.builder.scope.Scope.ScopeUtils.beginScope
import com.agile4j.model.builder.scope.ScopeKey


/**
 * 特性
 * 1. 支持批量构建
 * 2. 全部lazy式构建
 * 3. 语义性强的api
 * 4. 业务代码侵入性低
 * 5. 待构建类可嵌套
 * 6. * 支持异步化 回调
 * @author liurenpeng
 * Created on 2020-05-26
 */

fun main() {
    initScope()
    initModelRelation()

    /*val movieView = buildSingle(MovieView::class, 1L)
    println(movieView)

    val authorView = movieView?.authorView
    println(authorView)

    val movieView2 = authorView?.movieView
    println(movieView2)

    val authorView2 = movieView?.authorView
    println(authorView2)*/

    /*val movieView = buildSingle(MovieView::class, 1L)
    val mapper = ObjectMapper().registerKotlinModule()
    println("movieView:${mapper.writeValueAsString(movieView)}")*/


    testByIndex()
    //testByAccompany()
}

fun testByIndex() {
    buildByIndex(1L, listOf(1L, 2L))
    System.gc()
    Thread.sleep(1000)
    buildByIndex(3L, listOf(3L, 4L))
}

fun testByAccompany() {
    buildByAccompany(getMovieById(1L), getMovieByIds(setOf(1L, 2L)).values)
    buildByAccompany(getMovieById(3L),  getMovieByIds(setOf(3L, 4L)).values)
}

fun buildByIndex(movieId : Long, movieIds: Collection<Long>) {
    val movieView = ModelBuilder() buildSingle MovieView::class by movieId
    val movieViews = ModelBuilder() buildMulti MovieView::class by movieIds
    printMovieView(movieView, movieViews)
}

fun buildByAccompany(movie : Movie, movies: Collection<Movie>) {
    val movieView = ModelBuilder() buildSingle MovieView::class by movie
    val movieViews = ModelBuilder() buildMulti MovieView::class by movies
    printMovieView(movieView, movieViews)
}

fun printMovieView(movieView : MovieView?, movieViews: Collection<MovieView>) {

    println()
    println("---movieView:$movieView")
    println()

    println("---author:${movieView?.author}")
    /*println("---authorView:${movieView?.authorView}")
    println("---authorView.movie:${movieView?.authorView?.movie}")
    println("---authorView.movieView:${movieView?.authorView?.movieView}")*/
    println("---checker:${movieView?.checker}")
    println("---checker:${movieView?.checker}")
    println()

    println("---movieViews:$movieViews.")
    println()

    println("---0.shared:${movieViews.elementAt(0).shared}")
    println("---0.viewed:${movieViews.elementAt(0).viewed}")
    println("---0.count:${movieViews.elementAt(0).count}")
    println("---0.interaction:${movieViews.elementAt(0).interaction}")
    println("---0.videos:${movieViews.elementAt(0).videos}")
    println("---0.author:${movieViews.elementAt(0).author}")
    println("---0.checker:${movieViews.elementAt(0).checker}")
    println("---0.authorView:${movieViews.elementAt(0).authorView}")
    println("---0.videoDTOs:${movieViews.elementAt(0).videoDTOs}")
    println()

    println("---1.shared:${movieViews.elementAt(1).shared}")
    println("---1.viewed:${movieViews.elementAt(1).viewed}")
    println("---1.count:${movieViews.elementAt(1).count}")
    println("---1.interaction:${movieViews.elementAt(1).interaction}")
    println("---1.videos:${movieViews.elementAt(1).videos}")
    println("---1.author:${movieViews.elementAt(1).author}")
    println("---1.checker:${movieViews.elementAt(1).checker}")
    println("---1.authorView:${movieViews.elementAt(1).authorView}")
    println("---1.videoDTOs:${movieViews.elementAt(1).videoDTOs}")
    println()

    movieViews.elementAt(0).videoDTOs.forEach{dto -> println(dto.source)}
    println()
    movieViews.elementAt(1).videoDTOs.forEach{dto -> println(dto.source)}
    println()

    println("**********")
    System.gc()
    Thread.sleep(1000)
    movieView?.buildInModelBuilder
    println()
}

object CurrentScopeKeys{
    val visitor: ScopeKey<Long> = ScopeKey.withDefaultValue(0)
    fun visitor() = visitor.get()
}

fun initScope() {
    beginScope()
    visitor.set(1)
}

