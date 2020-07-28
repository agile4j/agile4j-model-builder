package com.agile4j.model.builder.exception

/**
 * @author: liurenpeng
 * @date: Created in 2020-07-09
 */
class ModelBuildException(
    var desc: String,
    e: Throwable? = null) : Throwable(e) {
    override val message: String?
        get() = "desc=$desc"

    companion object{
        fun err(desc: String) : Nothing {
            throw ModelBuildException(desc)
        }
    }
}