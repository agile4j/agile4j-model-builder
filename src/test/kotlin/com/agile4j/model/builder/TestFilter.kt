package com.agile4j.model.builder

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.accessTimes
import org.junit.Assert
import org.junit.Test

/**
 * 过滤器
 * @author liurenpeng
 * Created on 2020-11-20
 */
class TestFilter: BaseTest() {
    @Test
    fun testFilterBy() {
        val views = movieIds1A2 mapMulti MovieView::class filterBy {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)
    }

    @Test
    fun testBuildMulti() {
        var views = buildMulti(MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)

        views = buildMulti(MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)
    }

    @Test
    fun testBuildMapOfI() {
        var views = buildMapOfI(MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)

        views = buildMapOfI(MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
    }

    @Test
    fun testBuildMapOfA() {
        var views = buildMapOfA(MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)

        views = buildMapOfA(MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
    }

    @Test
    fun testBuildMultiWithExistModelBuilder() {
        val mb = ModelBuilder()
        var views = buildMultiWithExistModelBuilder(mb, MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)
        val accessTimes1 = accessTimes.get()
        println("accessTimes1:$accessTimes1")
        Assert.assertTrue(accessTimes1 > 0)

        views = buildMultiWithExistModelBuilder(mb, MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        printMultiMovieView(views)
        val accessTimes2 = accessTimes.get()
        println("accessTimes2:$accessTimes2")
        Assert.assertEquals(accessTimes1, accessTimes2)
    }

    @Test
    fun testBuildMapOfIWithExistModelBuilder() {
        val mb = ModelBuilder()
        var views = buildMapOfIWithExistModelBuilder(
            mb, MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
        val accessTimes1 = accessTimes.get()
        println("accessTimes1:$accessTimes1")
        Assert.assertTrue(accessTimes1 > 0)

        views = buildMapOfIWithExistModelBuilder(
            mb, MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
        val accessTimes2 = accessTimes.get()
        println("accessTimes2:$accessTimes2")
        Assert.assertEquals(accessTimes1, accessTimes2)
    }

    @Test
    fun testBuildMapOfAWithExistModelBuilder() {
        val mb = ModelBuilder()
        var views = buildMapOfAWithExistModelBuilder(
            mb, MovieView::class, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
        val accessTimes1 = accessTimes.get()
        println("accessTimes1:$accessTimes1")
        Assert.assertTrue(accessTimes1 > 0)

        views = buildMapOfAWithExistModelBuilder(
            mb, MovieView::class.java, movieIds1A2) {it.id > 1}
        Assert.assertEquals(views.size, 1)
        println(views)
        printMultiMovieView(views.values)
        val accessTimes2 = accessTimes.get()
        println("accessTimes2:$accessTimes2")
        Assert.assertEquals(accessTimes1, accessTimes2)
    }
}