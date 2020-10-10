package com.agile4j.model.builder

import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.mock.MockScopes
import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.accessTimes
import com.agile4j.model.builder.mock.initModelRelation
import com.agile4j.model.builder.mock.warmUpModelRelation
import org.junit.Before

/**
 * @author liurenpeng
 * Created on 2020-07-23
 */
open class BaseTest {

    protected val movieId1 = 1L
    protected val movieIds1A2 = listOf(1L, 2L)
    protected val movieId3 = 3L
    protected val movieIds3A4 = listOf(3L, 4L)
    protected val movie1 = getMovie(1L)
    protected val movies1A2 = listOf(getMovie(1L), getMovie(2L))
    protected val movie3 = getMovie(3L)
    protected val movies3A4 = listOf(getMovie(3L), getMovie(4L))

    @Before
    fun before() {
        initScope()
        initModelRelation()
        warmUpModelRelation()
        gcAndSleep()
        accessTimes.set(0)
    }

    protected fun gcAndSleepAndRefreshWeakMapSize(target: Any?) {
        gcAndSleep()
        if (target is Collection<*>) refreshWeakMapSize(target.first()) else refreshWeakMapSize(target)
    }

    protected fun printSingleMovieView(movieView : MovieView?) =
        printSingleMovieView(movieView, true)

    protected fun printSingleMovieView(movieView : MovieView?, withGC: Boolean) {
        //println("mb sig0-----${System.nanoTime()}")
        println("---movieView:$movieView")
        println()

        //println("mb sig1-----${System.nanoTime()}")
        println("---id:${movieView?.id}")
        //println("mb sig2-----${System.nanoTime()}")
        println("---idInJoin:${movieView?.idInJoin}")
        //println("mb sig3-----${System.nanoTime()}")
        println("---subscriberIdsInJoin:${movieView?.subscriberIdsInJoin}")
        //println("mb sig4-----${System.nanoTime()}")
        println("---checkerView:${movieView?.checkerView}")
        //println("mb sig5-----${System.nanoTime()}")
        println("---visitorViews:${movieView?.visitorViews}")
        //println("mb sig6-----${System.nanoTime()}")
        println("---author:${movieView?.author}")
        //println("mb sig7-----${System.nanoTime()}")
        println("---subscribers:${movieView?.subscribers}")
        //println("mb sig8-----${System.nanoTime()}")
        println("---authorView:${movieView?.authorView}")
        //println("mb sig9-----${System.nanoTime()}")
        println("---subscriberViews:${movieView?.subscriberViews}")
        //println("mb sig10----${System.nanoTime()}")
        println("---shared:${movieView?.shared}")
        //println("mb sig11----${System.nanoTime()}")
        println("---count:${movieView?.count}")
        //println("mb sig12----${System.nanoTime()}")
        println("---interaction:${movieView?.interaction}")
        //println("mb sig13----${System.nanoTime()}")
        println("---videos:${movieView?.videos}")
        //println("mb sig14----${System.nanoTime()}")
        println("---trailerView:${movieView?.trailerView}")
        //println("mb sig15----${System.nanoTime()}")
        println("---videoDTOs:${movieView?.videoDTOs}")
        //println("mb sig16----${System.nanoTime()}")
        println("---trailer:${movieView?.trailer}")
        //println("mb sig17----${System.nanoTime()}")
        println("---byIVideos:${movieView?.byIVideos}")
        //println("mb sig18----${System.nanoTime()}")
        println("---byITrailerView:${movieView?.byITrailerView}")
        //println("mb sig19----${System.nanoTime()}")
        println()
        println("---authorView.movie:${movieView?.authorView?.movie}")
        //println("mb sig20----${System.nanoTime()}")
        println("---authorView.movieView:${movieView?.authorView?.movieView}")
        println()
        printWeakMapSize(movieView, withGC)
        println()
    }

    protected fun printMultiMovieView(movieViews: Collection<MovieView>)
            = printMultiMovieView(movieViews, true)

    protected fun printMultiMovieView(movieViews: Collection<MovieView>, withGC: Boolean) {
        //println("mb mul0-----${System.nanoTime()}")
        println("---movieViews:$movieViews.")
        println()

        for ((index, movieView) in movieViews.withIndex()) {
            //println("mb mul1-----${System.nanoTime()}")
            /*println("---$index.shared:${movieView.shared}")
            //println("mb mul2-----${System.nanoTime()}")
            println("---$index.count:${movieView.count}")
            //println("mb mul3-----${System.nanoTime()}")
            println("---$index.interaction:${movieView.interaction}")
            //println("mb mul4-----${System.nanoTime()}")
            println("---$index.author:${movieView.author}")
            //println("mb mul5-----${System.nanoTime()}")
            println("---$index.checkerView:${movieView.checkerView}")
            //println("mb mul6-----${System.nanoTime()}")
            println("---$index.visitorViews:${movieView.visitorViews}")
            //println("mb mul7-----${System.nanoTime()}")
            println("---$index.authorView:${movieView.authorView}")
            //println("mb mul8-----${System.nanoTime()}")
            println("---$index.videos:${movieView.videos}")
            //println("mb mul9-----${System.nanoTime()}")
            println("---$index.videoDTOs:${movieView.videoDTOs}")
            //println("mb mul10----${System.nanoTime()}")
            println("---$index.byIVideos:${movieView.byIVideos}")*/
            //println("mb mul11----${System.nanoTime()}")
            println("---$index.byIVideoDTOs:${movieView.byIVideoDTOs}")
            println()
            movieView.videoDTOs?.forEach{dto ->
                //println("mb mul12----${System.nanoTime()}")
                println(dto.source)
            }
            println()
        }
        printWeakMapSize(movieViews.first(), withGC)
        println()
    }

    private fun initScope() {
        MockScopes.visitor.set(1)
    }

    private fun gcAndSleep() {
        System.gc()
        Thread.sleep(100)
    }

    private fun refreshWeakMapSize(target: Any?) {
        // get set 时都会进行refresh
        target?.buildInModelBuilder
    }

    private fun printWeakMapSize(movieView : MovieView?, withGC: Boolean) {
        if (withGC) gcAndSleep()
        refreshWeakMapSize(movieView)
        println("**********weakMapSize:${ModelBuilderDelegate.weakMapSize.get()}")
        println()
        println()
    }

    private fun getMovie(it: Long) = Movie(
        it, 2 * it - 1,  listOf(2 * it + 1, 2 * it + 2))
}