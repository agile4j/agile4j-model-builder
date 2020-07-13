package com.agile4j.model.builder.scope

import com.agile4j.model.builder.scope.Scope.ScopeUtils.currentScope
import java.util.function.Supplier

/**
 * @author liurenpeng
 * Created on 2020-06-15
 */
class ScopeKey<T> private constructor(
    val defaultValue: T?,
    val initializer: Supplier<T>?
) {

    fun set(value: T) = currentScope()?.let { it[this] = value }

    fun get(): T? = currentScope()?.get(this) ?: defaultValue

    companion object ScopeKeyUtils {

        fun <T> withDefaultValue(defaultValue: T?) = ScopeKey(defaultValue, null)

        fun <T> withInitializer(initializer: Supplier<T>?) = ScopeKey(null, initializer)
    }
}