package com.agile4j.model.builder

import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.initModelRelation
import org.junit.Before

/**
 * @author liurenpeng
 * Created on 2020-07-23
 */
open class BaseTest {

    @Before
    fun before() {
        initScope()
        initModelRelation()
    }

    protected fun gcAndSleep() {
        System.gc()
        Thread.sleep(100)
    }

    protected fun refreshWeakMapSize(target: Any?) {
        // get set 时都会进行refresh
        target?.buildInModelBuilder
    }

    protected fun printMovieView(movieView: MovieView?) {
        println("---movieView:$movieView")
        println()

        println("---author:${movieView?.author}")
        println("---authorView:${movieView?.authorView}")
        println("---authorView.movie:${movieView?.authorView?.movie}")
        println("---authorView.movieView:${movieView?.authorView?.movieView}")
        println("---checker:${movieView?.checker}")
        println("---checker:${movieView?.checker}")
    }
}