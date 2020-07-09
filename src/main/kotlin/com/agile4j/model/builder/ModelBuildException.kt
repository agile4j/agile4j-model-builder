package com.agile4j.model.builder

/**
 * @author: liurenpeng
 * @date: Created in 2020-07-09
 */
class ModelBuildException(var desc: String) : Throwable() {
    override val message: String?
        get() = "desc=$desc"
}