package com.agile4j.model.builder.utils

import com.agile4j.utils.util.MapUtil

/**
 * @author liurenpeng
 * Created on 2020-07-20
 */

fun <K, V> Map<K, V>.reverseKV(): Map<V, K> = this.map { (k, v) -> v to k }.toMap()

fun <K, V> Map<K, V>.firstValue(): V? = if (MapUtil.isEmpty(this)) null else this.values.elementAt(0)

