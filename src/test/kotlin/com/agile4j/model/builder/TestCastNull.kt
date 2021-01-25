package com.agile4j.model.builder

import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-08-03
 */

class TestCastNull: BaseTest() {

    @Test
    fun testIToEjicToEjacToEjtc() {
        val movieView1 = movieId1 mapSingle MovieView::class
        Assert.assertTrue(movieView1?.byIVideoDTOsForTestEmpty!!.isEmpty())
    }

    @Test
    fun testIToEjicToEjacToEjtcCached() {
        val movieView1 = movieId1 mapSingle MovieView::class
        movieView1?.byIVideoDTOsForTestEmpty
        Assert.assertTrue(movieView1?.byIVideoDTOsForTestEmpty!!.isEmpty())
    }

    @Test
    fun testIToEjicToEjac() {
        val movieView1 = movieId1 mapSingle MovieView::class
        Assert.assertTrue(movieView1?.byIVideosForTestEmpty!!.isEmpty())
    }

    @Test
    fun testIToEjicToEjacCached() {
        val movieView1 = movieId1 mapSingle MovieView::class
        movieView1?.byIVideosForTestEmpty
        Assert.assertTrue(movieView1?.byIVideosForTestEmpty!!.isEmpty())
    }

    @Test
    fun testIToEjacToEjtc() {
        val movieView1 = movieId1 mapSingle MovieView::class
        Assert.assertTrue(movieView1?.videoDTOsForTestEmpty!!.isEmpty())
    }

    @Test
    fun testIToEjacToEjtcCached() {
        val movieView1 = movieId1 mapSingle MovieView::class
        movieView1?.videoDTOsForTestEmpty
        Assert.assertTrue(movieView1?.videoDTOsForTestEmpty!!.isEmpty())
    }
}