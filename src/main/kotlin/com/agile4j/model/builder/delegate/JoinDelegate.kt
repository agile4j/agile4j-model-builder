package com.agile4j.model.builder.delegate

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
/*
internal interface JoinDelegate<M>{

    operator fun getValue(thisT: Any, property: KProperty<*>): M? =
        if (isTargetRelatedProperty(property)) buildWithScope(thisT, property, this::buildTarget)
        else buildWithScope(thisT, property, this::buildAccompany)

    fun buildWithScope(
        thisT: Any,
        property: KProperty<*>,
        builder: (Any, KProperty<*>) -> M?
    ): M? = supplyWithExistScope(copyScope(currentScope())) {
        setModelBuilder(copyBy(thisT.buildInModelBuilder))
        return@supplyWithExistScope  builder(thisT, property)
    }

    fun buildTarget(thisT: Any, property: KProperty<*>): M?

    fun buildAccompany(thisT: Any, property: KProperty<*>): M?

    // TODO 把类移出去
    */
/*object Scopes{
        fun nullableModelBuilder() = modelBuilderScopeKey.get()
        fun modelBuilder() = nullableModelBuilder() ?: err("modelBuilderScopeKey not init")
        fun setModelBuilder(modelBuilder: ModelBuilder) = modelBuilderScopeKey.set(modelBuilder)
        private val modelBuilderScopeKey: ScopeKey<ModelBuilder> = ScopeKey.withDefaultValue(null)
    }*//*

}*/
