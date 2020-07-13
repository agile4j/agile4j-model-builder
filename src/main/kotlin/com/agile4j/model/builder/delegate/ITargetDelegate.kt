package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.ModelBuilder.Companion.copyBy
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.build.isTargetClass
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.scope.Scope.ScopeUtils.copyScope
import com.agile4j.model.builder.scope.Scope.ScopeUtils.currentScope
import com.agile4j.model.builder.scope.Scope.ScopeUtils.supplyWithExistScope
import com.agile4j.model.builder.scope.ScopeKey
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
interface ITargetDelegate<T>{
    operator fun getValue(thisRef: Any, property: KProperty<*>): T =
        if (isTargetClass(property)) buildTargetWithScope(thisRef, property)
        else buildAccompanyWithScope(thisRef, property)

    fun buildTargetWithScope(thisRef: Any, property: KProperty<*>): T =
        supplyWithExistScope(copyScope(currentScope())) {
            modelBuilderScopeKey.set(copyBy(thisRef.buildInModelBuilder))
            return@supplyWithExistScope  buildTarget(thisRef, property)
        }

    fun buildAccompanyWithScope(thisRef: Any, property: KProperty<*>): T =
        supplyWithExistScope(copyScope(currentScope())) {
            modelBuilderScopeKey.set(copyBy(thisRef.buildInModelBuilder))
            return@supplyWithExistScope  buildAccompany(thisRef, property)
        }
    /*{
        val oldScope = Scope.scopeThreadLocal.get()
        val newScope = copyScope(oldScope)
        Scope.scopeThreadLocal.set(newScope)
        try {
            modelBuilderScopeKey.set(copyBy(thisRef.buildInModelBuilder))
            val result =  buildTarget(thisRef, property)

            return result
        } finally {
            if (oldScope != null) {
                Scope.scopeThreadLocal.set(oldScope)
            } else {
                Scope.scopeThreadLocal.remove()
            }
        }
    }*/

    fun buildTarget(thisRef: Any, property: KProperty<*>): T

    fun buildAccompany(thisRef: Any, property: KProperty<*>): T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        throw UnsupportedOperationException("model builder delegate field not support set")
    }

    object ScopeKeys{
        val modelBuilderScopeKey: ScopeKey<ModelBuilder> = ScopeKey.withDefaultValue(null)
    }
}