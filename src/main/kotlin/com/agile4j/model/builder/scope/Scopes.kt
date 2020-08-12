package com.agile4j.model.builder.scope

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.utils.scope.ScopeKey

/**
 * @author liurenpeng
 * Created on 2020-07-28
 */
internal object Scopes {
    fun nullableModelBuilder() = modelBuilderScopeKey.get()
    fun setModelBuilder(modelBuilder: ModelBuilder?) = modelBuilderScopeKey.set(modelBuilder)
    private val modelBuilderScopeKey: ScopeKey<ModelBuilder?> = ScopeKey.withDefaultValue(null)
}