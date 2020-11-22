package com.agile4j.model.builder.relation

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.exception.ExceptionHandler
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */

infix fun <AXT: Any> KClass<AXT>.handleExceptionBy(exceptionHandler: ExceptionHandler) {
    BuildContext.putAXTExceptionHandler(this, exceptionHandler)
}