package com.agile4j.model.builder.exception

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
interface ExceptionHandler {
    fun <I: Any, A:Any, T:Any, JR: Any> handleException(
        exceptionContext: BaseExceptionContext<I, A, T, *, *>):  JR?
}