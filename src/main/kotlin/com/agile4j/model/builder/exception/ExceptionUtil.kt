package com.agile4j.model.builder.exception

import com.agile4j.model.builder.build.BuildContext.getAXTExceptionHandler
import com.agile4j.model.builder.build.BuildContext.globalExceptionHandler
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */

/**
 * 优先级：
 * 1. 定义在tClass上的ExceptionHandler
 * 2. 定义在aClass上的ExceptionHandler
 * 3. 定义在全局的ExceptionHandler
 */
internal fun getExceptionHandler(tClass: KClass<*>, aClass: KClass<*>): ExceptionHandler? =
    getAXTExceptionHandler(tClass) ?: getAXTExceptionHandler(aClass) ?: globalExceptionHandler