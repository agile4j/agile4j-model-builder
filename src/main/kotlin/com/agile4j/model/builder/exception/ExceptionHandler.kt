package com.agile4j.model.builder.exception

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
interface ExceptionHandler {
    fun <I: Any, A:Any, T:Any, EJP: Any, EJR: Any> handleExJoinException(
        exceptionDTO: ExJoinExceptionDTO<I, A, T, EJP>):  EJR?

    fun <I: Any, A:Any, T:Any, IJP: Any, IJR: Any> handleInJoinException(
        exceptionDTO: InJoinExceptionDTO<I, A, T, IJP>): IJR?
}