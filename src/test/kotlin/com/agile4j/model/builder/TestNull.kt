package com.agile4j.model.builder

import com.agile4j.model.builder.delegate.InJoinDelegate.Companion.inJoin
import com.agile4j.model.builder.relation.accompanyBy
import com.agile4j.model.builder.relation.indexBy
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Test

/**
 * @author liurenpeng
 * Created on 2020-10-28
 */
class TestNull {

    @Test
    fun test() {
        initMB()

        val outerVOs = outerList mapMulti OuterVO::class
        val mapper = ObjectMapper().registerKotlinModule()
        val currJsonStr = mapper.writeValueAsString(outerVOs)
        println("currJsonStr:$currJsonStr")

        val i1 = Inner(1)
        val i11 = Inner(1)
        println(i1.equals(i11))
        println(i1 == i11)
        println(i1 === i11)
        /*val outerVOList = outerVOs as List<OuterVO>
        println("equal:" + (outerVOList[0].getResources()?.get(0)?.inner
                === outerVOList[1].getResources()?.get(0)?.inner))

        val list = listOf(Outer(1), null, Inner(1)) as List<Any>
        println(list)*/
    }

    fun initMB() {
        Inner::class indexBy Inner::id
        Outer::class indexBy Outer::id

        InnerDTO::class accompanyBy Inner::class
        InnerVO::class accompanyBy Inner::class
        OuterDTO::class accompanyBy Outer::class
        OuterVO::class accompanyBy Outer::class
    }
}

val innerList = listOf(Inner(1), Inner(2), Inner(3))
val outerList = listOf(Outer(1), Outer(2))

class Inner(val id: Long)

open class InnerDTO(val inner: Inner)

class InnerVO(inner: Inner): InnerDTO(inner)

class Outer(val id: Long) {
    val inners = listOf(Inner(1), Inner(2), Inner(3))
}

open class OuterDTO(val outer: Outer) {
    @get:JsonIgnore
    protected val innerVOs: List<InnerVO>? by inJoin(Outer::inners)
}

class OuterVO(outer: Outer): OuterDTO(outer) {
    fun getResources():List<InnerVO>?  {
        return innerVOs
    }
}