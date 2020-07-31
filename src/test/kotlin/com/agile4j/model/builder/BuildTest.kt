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








