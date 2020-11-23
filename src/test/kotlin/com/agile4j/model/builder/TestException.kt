package com.agile4j.model.builder

import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.mock.MockScopes
import com.agile4j.model.builder.mock.MovieView
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

/**
 * 异常处理器
 * @author liurenpeng
 * Created on 2020-11-20
 */

class TestException: BaseTest() {

    @get:Rule
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun testException() {
        MockScopes.setThrowException(true)
        val view = movie1 mapSingle MovieView::class

        thrown.expect(ModelBuildException::class.java)
        thrown.expectMessage("for test")
        println(view?.exceptionTrailerView)
    }
}