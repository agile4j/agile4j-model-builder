package com.agile4j.model.builder.scope

import java.util.concurrent.ConcurrentHashMap

/**
 * @author liurenpeng
 * Created on 2020-06-15
 */
class Scope {

    private val map = ConcurrentHashMap<ScopeKey<*>, Any>()

    operator fun <T> set(key: ScopeKey<T>, value: T?) =
        if (value != null) map.put(key, value) else map.remove(key)

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: ScopeKey<T>): T? {
        var value = map[key] as T?
        if (value == null && key.initializer != null) {
            value = key.initializer.get()
            value?.let {map.put(key, value)}
        }
        return value?:key.defaultValue
    }

    companion object ScopeUtils {
        private val scopeThreadLocal = ThreadLocal<Scope>()

        fun copyScope(scope: Scope?) : Scope {
            val result = Scope()
            result.map.putAll(scope?.map?: emptyMap())
            return result
        }

        fun currentScope() : Scope? = scopeThreadLocal.get()

        fun beginScope() {
            if (scopeThreadLocal.get() == null) scopeThreadLocal.set(Scope())
        }

        fun endScope() = scopeThreadLocal.remove()

        fun runWithExistScope(scope: Scope?, runner: () -> Unit) =
            supplyWithExistScope(scope) {runner.invoke()}

        fun <R> supplyWithExistScope(scope: Scope?, supplier: () -> R) : R {
            val oldScope = scopeThreadLocal.get()
            scopeThreadLocal.set(scope)
            try {
                return supplier.invoke()
            } finally {
                if (oldScope != null) {
                    scopeThreadLocal.set(oldScope)
                } else {
                    scopeThreadLocal.remove()
                }
            }
        }

        fun runWithNewScope(runner: () -> Unit) =
            runWithExistScope(Scope()) {runner.invoke()}

        fun <R> supplyWithNewScope(supplier: () -> R) : R =
            supplyWithExistScope(Scope()) {supplier.invoke()}
    }
}