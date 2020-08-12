package com.agile4j.model.builder.mock

import com.agile4j.utils.scope.ScopeKey

/**
 * @author liurenpeng
 * Created on 2020-07-31
 */
object MockScopes {
    val visitor: ScopeKey<Long> = ScopeKey.withDefaultValue(0)
    fun visitor() = visitor.get()
}