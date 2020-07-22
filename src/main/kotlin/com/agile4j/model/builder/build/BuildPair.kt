package com.agile4j.model.builder.build

import com.agile4j.utils.open.OpenPair

/**
 * @param T target
 * @author liurenpeng
 * Created on 2020-07-09
 */

class BuildSinglePair<out T>(modelBuilder: ModelBuilder, value: T) : OpenPair<ModelBuilder, T>(modelBuilder, value)
val <T> BuildSinglePair<T>.modelBuilder get() = first
val <T> BuildSinglePair<T>.targetClazz get() = second

class BuildMultiPair<out T>(modelBuilder: ModelBuilder, value: T) : OpenPair<ModelBuilder, T>(modelBuilder, value)
val <T> BuildMultiPair<T>.modelBuilder get() = first
val <T> BuildMultiPair<T>.targetClazz get() = second