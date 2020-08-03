package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestCircularDependency: BaseTest() {

    @Test
    fun test() {
        val movieView = movieId1 mapSingle MovieView::class
        val authorView = movieView!!.authorView
        val authorMovieView = authorView!!.movieView
        val authorMovieAuthorView = authorMovieView!!.authorView
        Assert.assertEquals(movieView, authorMovieView)
        Assert.assertEquals(authorView, authorMovieAuthorView)
    }
}