package com.agile4j.model.builder

import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.model.builder.mock.MockScopes
import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.initModelRelation
import com.agile4j.model.builder.mock.warmUpModelRelation
import com.agile4j.model.builder.scope.Scope
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
    }

    protected fun gcAndSleepAndRefreshWeakMapSize(target: Any?) {
        gcAndSleep()
        refreshWeakMapSize(target)
    }

    protected fun printSingleMovieView(movieView : MovieView?) {
        println("---movieView:$movieView")
        println()

        println("---id:${movieView?.id}")
        println("---idInJoin:${movieView?.idInJoin}")
        println("---author:${movieView?.author}")
        println("---authorView:${movieView?.authorView}")
        println("---authorView.movie:${movieView?.authorView?.movie}")
        println("---authorView.movieView:${movieView?.authorView?.movieView}")
        println("---checker:${movieView?.checker}")
        println("---checker:${movieView?.checker}")
        println("---subscribers:${movieView?.subscribers}")
        println("---subscriberViews:${movieView?.subscriberViews}")
        println()
        printWeakMapSize(movieView)
        println()
        println()
    }

    protected fun printMultiMovieView(movieViews: Collection<MovieView>) {
        println("---movieViews:$movieViews.")
        println()

        for ((index, movieView) in movieViews.withIndex()) {
            println("---$index.shared:${movieView.shared}")
            println("---$index.viewed:${movieView.viewed}")
            println("---$index.count:${movieView.count}")
            println("---$index.interaction:${movieView.interaction}")
            println("---$index.author:${movieView.author}")
            println("---$index.checker:${movieView.checker}")
            println("---$index.authorView:${movieView.authorView}")
            println("---$index.videos:${movieView.videos}")
            println("---$index.videoDTOs:${movieView.videoDTOs}")
            println("---$index.byIVideos:${movieView.byIVideos}")
            println("---$index.byIVideoDTOs:${movieView.byIVideoDTOs}")
            println()
            movieView.videoDTOs?.forEach{dto -> println(dto.source)}
            println()
        }
        printWeakMapSize(movieViews.first())
        println()
        println()
    }

    private fun initScope() {
        Scope.beginScope()
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

    private fun printWeakMapSize(movieView : MovieView?) {
        gcAndSleep()
        refreshWeakMapSize(movieView)
        println("**********weakMapSize:${ModelBuilderDelegate.weakMapSize.get()}")
        println()
        println()
    }

    private fun getMovie(it: Long) = Movie(
        it, 2 * it - 1, 2 * it, listOf(2 * it + 1, 2 * it + 2))
}