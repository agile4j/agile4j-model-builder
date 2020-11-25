package com.agile4j.model.builder.mock

import com.agile4j.model.builder.exception.BaseExceptionContext
import com.agile4j.model.builder.exception.ExceptionHandler
import com.agile4j.model.builder.exception.ModelBuildException

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
class MockExceptionHandler : ExceptionHandler {
    override fun <I : Any, A : Any, T : Any, JR : Any> handleException(
        exceptionContext: BaseExceptionContext<I, A, T, *, *>): JR? {
        println("exceptionContext:$exceptionContext")
        throw ModelBuildException("for test", exceptionContext.throwable)
    }

    companion object {
        val globalHandler = MockExceptionHandler()
        val movieHandler = MockExceptionHandler()
        val movieViewHandler = MockExceptionHandler()
    }
}