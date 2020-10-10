package com.agile4j.model.builder

import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieView
import org.junit.Test


/**
 * 这里的test主要为了演示如何使用
 * @author liurenpeng
 * Created on 2020-05-26
 */
class BuildTest: BaseTest() {

    @Test
    fun testBuildSingleByIndex() {
        buildSingleByIndex(movieId1)
    }

    @Test
    fun testBuildMultiByIndex() {
        val m1 = System.currentTimeMillis()
        val t1= System.nanoTime()
        buildMultiByIndex(movieIds1A2)
        val m2 = System.currentTimeMillis()
        val t2= System.nanoTime()
        buildMultiByIndex(movieIds1A2)
        val m3 = System.currentTimeMillis()
        val t3= System.nanoTime()
        println("``````````t1:$t1")
        println("``````````t2:$t2  di:\t${t2 - t1}")
        println("``````````t3:$t3  di:\t${t3 - t2}")
        println()
        println("``````````m1:$m1")
        println("``````````m2:$m2  di:\t${m2 - m1}")
        println("``````````m3:$m3  di:\t${m3 - m2}")
    }

    @Test
    fun testBuildByIndex() {
        buildSingleByIndex(movieId1)
        buildMultiByIndex(movieIds1A2)
        buildSingleByIndex(movieId3)
        buildMultiByIndex(movieIds3A4)
    }

    @Test
    fun testBuildByAccompanyTwice() {
        buildSingleByAccompany(movie1)
        buildMultiByAccompany(movies1A2)
        buildSingleByAccompany(movie3)
        buildMultiByAccompany(movies3A4)
    }

    private fun buildSingleByIndex(movieId: Long) {
        val movieView = movieId mapSingle MovieView::class
        printSingleMovieView(movieView)
    }

    private fun buildMultiByIndex(movieIds: Collection<Long>) {
        val movieViews = movieIds mapMulti MovieView::class
        printMultiMovieView(movieViews)
    }

    private fun buildSingleByAccompany(movie: Movie) {
        val movieView = movie mapSingle MovieView::class
        printSingleMovieView(movieView)
    }

    private fun buildMultiByAccompany(movies: Collection<Movie>) {
        val movieViews = movies mapMulti MovieView::class
        printMultiMovieView(movieViews)
    }

}








