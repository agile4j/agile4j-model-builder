package com.agile4j.model.builder.mock

import com.agile4j.model.builder.exception.ExJoinExceptionDTO
import com.agile4j.model.builder.exception.ExceptionHandler
import com.agile4j.model.builder.exception.InJoinExceptionDTO

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
class MockExceptionHandler : ExceptionHandler {
    override fun <I : Any, A : Any, T : Any, EJP : Any, EJR : Any> handleExJoinException(
        exceptionDTO: ExJoinExceptionDTO<I, A, T, EJP>): EJR? {
        println("exceptionDTO:$exceptionDTO")
        throw exceptionDTO.throwable
    }

    override fun <I : Any, A : Any, T : Any, IJP : Any, IJR : Any> handleInJoinException(
        exceptionDTO: InJoinExceptionDTO<I, A, T, IJP>): IJR? {
        println("exceptionDTO:$exceptionDTO")
        throw exceptionDTO.throwable
    }

    companion object {
        val globalHandler = MockExceptionHandler()
        val movieHandler = MockExceptionHandler()
        val movieViewHandler = MockExceptionHandler()
    }
}