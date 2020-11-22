package com.agile4j.model.builder.mock

import com.agile4j.utils.scope.ScopeKey

/**
 * @author liurenpeng
 * Created on 2020-07-31
 */
object MockScopes {
    private val visitor: ScopeKey<Long> = ScopeKey.withDefaultValue(0)
    fun setVisitor(visitorValue: Long) = visitor.set(visitorValue)
    fun visitor() = visitor.get() ?: 0

    private val fetchCount: ScopeKey<Boolean> = ScopeKey.withDefaultValue(true)
    fun setFetchCount(isFetchCount: Boolean) = fetchCount.set(isFetchCount)
    fun isFetchCount() = fetchCount.get() ?: true

    private val fetchVideos: ScopeKey<Boolean> = ScopeKey.withDefaultValue(true)
    fun setFetchVideos(isFetchVideos: Boolean) = fetchVideos.set(isFetchVideos)
    fun isFetchVideos() = fetchVideos.get() ?: true

    private val throwException: ScopeKey<Boolean> = ScopeKey.withDefaultValue(false)
    fun setThrowException(isThrowException: Boolean) = throwException.set(isThrowException)
    fun isThrowException() = throwException.get() ?: false
}