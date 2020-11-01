package com.agile4j.model.builder

import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieView
import org.junit.Assert
import org.junit.Test


/**
 * 这里的test主要为了演示如何使用
 * @author liurenpeng
 * Created on 2020-05-26
 */
class BuildTest: BaseTest() {

    @Test
    fun testMapSingle() {
        var target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        println("target:$target")

        target = movieId1 mapSingle MovieView::class.java
        Assert.assertNotNull(target)
        println("target:$target")
    }

    @Test
    fun testMapMulti() {
        var targets = movieIds1A2 mapMulti MovieView::class
        Assert.assertEquals(2, targets.size)
        println("targets:$targets")

        targets = movieIds1A2 mapMulti MovieView::class.java
        Assert.assertEquals(2, targets.size)
        println("targets:$targets")
    }

    @Test
    fun testBuildMapOfI() {
        var map = buildMapOfI(MovieView::class, movieIds1A2)
        Assert.assertEquals(2, map.size)
        println("map:$map")

        map = buildMapOfI(MovieView::class.java, movieIds1A2)
        Assert.assertEquals(2, map.size)
        println("map:$map")
    }

    @Test
    fun testBuildMapOfA() {
        var map = buildMapOfA(MovieView::class, movieIds1A2)
        Assert.assertEquals(2, map.size)
        println("map:$map")

        map = buildMapOfA(MovieView::class.java, movieIds1A2)
        Assert.assertEquals(2, map.size)
        println("map:$map")
    }

    @Test
    fun testExtractModelBuilder() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)
        println("mb:$mb")
    }

    @Test
    fun testBuildSingleWithExistModelBuilder() {
        var target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)

        target = buildSingleWithExistModelBuilder(mb!!, MovieView::class, movieId3)
        Assert.assertNotNull(target)

        target = buildSingleWithExistModelBuilder(mb, MovieView::class.java, movieId3)
        Assert.assertNotNull(target)
    }

    @Test
    fun testBuildMultiWithExistModelBuilder() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)

        var targets = buildMultiWithExistModelBuilder(mb!!, MovieView::class, movieIds1A2)
        Assert.assertEquals(2, targets.size)

        targets = buildMultiWithExistModelBuilder(mb, MovieView::class.java, movieIds3A4)
        Assert.assertEquals(2, targets.size)
    }

    @Test
    fun testBuildMapOfIWithExistModelBuilder() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)

        var iToTMap = buildMapOfIWithExistModelBuilder(mb!!, MovieView::class, movieIds1A2)
        Assert.assertEquals(2, iToTMap.size)
        println("iToTMap:$iToTMap")

        iToTMap = buildMapOfIWithExistModelBuilder(mb, MovieView::class.java, movieIds3A4)
        Assert.assertEquals(2, iToTMap.size)
        println("iToTMap:$iToTMap")
    }

    @Test
    fun testBuildMapOfAWithExistModelBuilder() {
        val target = movieId1 mapSingle MovieView::class
        Assert.assertNotNull(target)
        val mb = extractModelBuilder(target!!)
        Assert.assertNotNull(mb)

        var aToTMap = buildMapOfAWithExistModelBuilder(mb!!, MovieView::class, movieIds1A2)
        Assert.assertEquals(2, aToTMap.size)
        println("aToTMap:$aToTMap")

        aToTMap = buildMapOfAWithExistModelBuilder(mb, MovieView::class.java, movieIds3A4)
        Assert.assertEquals(2, aToTMap.size)
        println("aToTMap:$aToTMap")
    }

    @Test
    fun testBuildSingleByIndex() {
        buildSingleByIndex(movieId1)
    }

    @Test
    fun testBuildMultiByIndex() {
        buildMultiByIndex(movieIds1A2)
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








