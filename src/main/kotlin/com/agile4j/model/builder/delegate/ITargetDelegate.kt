package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.ModelBuilder.Companion.copyBy
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.build.isTargetRelatedProperty
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.setModelBuilder
import com.agile4j.model.builder.scope.Scope.ScopeUtils.copyScope
import com.agile4j.model.builder.scope.Scope.ScopeUtils.currentScope
import com.agile4j.model.builder.scope.Scope.ScopeUtils.supplyWithExistScope
import com.agile4j.model.builder.scope.ScopeKey
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
internal interface ITargetDelegate<M>{

    operator fun getValue(outerTarget: Any, property: KProperty<*>): M? =
        if (isTargetRelatedProperty(property)) buildWithScope(outerTarget, property, this::buildTarget)
        else buildWithScope(outerTarget, property, this::buildAccompany)

    operator fun setValue(outerTarget: Any, property: KProperty<*>, value: M?): Nothing =
        err("model builder delegate field not support set")

    fun buildWithScope(
        outerTarget: Any,
        property: KProperty<*>,
        builder: (Any, KProperty<*>) -> M?
    ): M? = supplyWithExistScope(copyScope(currentScope())) {
        setModelBuilder(copyBy(outerTarget.buildInModelBuilder))
        return@supplyWithExistScope  builder(outerTarget, property)
    }

    fun buildTarget(outerTarget: Any, property: KProperty<*>): M?

    fun buildAccompany(outerTarget: Any, property: KProperty<*>): M?

    object ScopeKeys{
        fun nullableModelBuilder() = modelBuilderScopeKey.get()
        fun modelBuilder() = nullableModelBuilder() ?: err("modelBuilderScopeKey not init")
        fun setModelBuilder(modelBuilder: ModelBuilder) = modelBuilderScopeKey.set(modelBuilder)
        private val modelBuilderScopeKey: ScopeKey<ModelBuilder> = ScopeKey.withDefaultValue(null)
    }
}