package com.agile4j.model.builder.utils

/**
 * @author liurenpeng
 * Created on 2020-07-20
 */

fun <K, V> Map<K, V>.reverseKV(): Map<V, K> = this.map { (k, v) -> v to k }.toMap()