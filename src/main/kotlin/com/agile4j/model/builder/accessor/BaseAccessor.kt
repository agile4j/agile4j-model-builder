package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.delegate.ITargetDelegate

/**
 * @author liurenpeng
 * Created on 2020-07-21
 */
abstract class BaseAccessor<K, V> {
    protected val modelBuilder = ITargetDelegate.ScopeKeys.modelBuilderScopeKey.get()
        ?: throw ModelBuildException("modelBuilderScopeKey not init")

    abstract fun get(accompanies: Collection<K>): Map<K, V>
}