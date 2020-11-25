# 高级用法

# 目录
   * [过滤器](#过滤器)
   * [剪枝器](#剪枝器)
   * [异常处理](#异常处理)
   * [在kts文件中声明Relation](#在kts文件中声明relation)
   * [复用modelBuilder的缓存](#复用modelbuilder的缓存)
   * [集成agile4j-feed-builder](#集成agile4j-feed-builder)


## 过滤器
* 过滤是指对target的过滤。在工具中内置过滤器的好处是：因为agile4j-model-builder[聚合批量构建](README_FEATURE.md#聚合批量构建)的特性，即使在外层对views集合进行了过滤，工具内部仍然会持有被过滤view的引用，对字段求值时的聚合过程，仍会带上已被过滤的view，因为工具对外部过滤逻辑无感知。
* 解决的方案是：在构建api中，支持传入过滤器参数，在返回结果之前，进行过滤，并把被过滤的view在工具内部的引用移除，以切实"滤掉"，不影响后续字段求值时的性能。
* 相关API例如：
```Kotlin
fun <T : Any, IXA: Any> buildMulti(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean): List<T>

fun <T : Any, IXA: Any> buildMapOfI(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T>

fun <T : Any, IXA: Any> buildMapOfA(clazz: KClass<T>, sources: Collection<IXA?>, filter: (T) -> Boolean) : LinkedHashMap<Any, T>
```

## 剪枝器
* 剪枝是指对target中通过agile4j-model-builder进行关联构建的字段，求值时的短路操作。
* 举一个场景：一个新上线的http接口，需要在XXXView中新增一个字段。但该字段除了在该接口中用到，其他接口都不会使用。如果不进行剪枝操作，随着此类业务的迭代，view对象的构建会越来越重，每次构建接口调用方的有效字段占比越来越少，造成性能的浪费。
* 解决的方案是：在[inJoin](README_API.md#injoin)、[exJoin](README_API.md#exjoin)接口中，支持传入剪枝器(类型：() → Boolean)。
* 用法例如：
```Kotlin
data class MovieView (val movie: Movie) {
    val videos: Collection<Video>? by exJoin(::getVideosByMovieIds) { isFetchVideos() }
}

object Scopes {
    private val fetchVideos: ScopeKey<Boolean> = ScopeKey.withDefaultValue(false)
    fun setFetchVideos(isFetchVideos: Boolean) = fetchVideos.set(isFetchVideos)
    fun isFetchVideos() = fetchVideos.get() ?: false
}
```
* 被剪枝器短路掉的字段，会直接返回空值：

|字段类型|空值|
|---|:---:|
|Set|emptySet()|
|List/Collection|emptyList()|
|其他|null|

## 异常处理
* agile4j-model-builder[增量lazy式构建](README_FEATURE.md#增量lazy式构建)的特性，决定了对字段的求值往往在一次请求处理的最后一个环节发生，这就导致求值时的异常可能发生在业务代码外部，例如SpringMVC中。
* 如果想在异常发生时，进行一些定制化处理（例如：打日志、上报、兜底赋值），需要工具内置异常处理机制。
* 解决的方案是：提供3个层面的异常处理器注册，在异常处理器选取时按优先级由高到底依次为：
    1. 注册在target上的异常处理器
    2. 注册在accompany上的异常处理器
    3. 注册在全局的异常处理器
* 异常处理器的注册方法为：通过`handleExceptionBy`API，支持声明Relation时，在target、accompany上进行异常处理器的注册；通过`registerGlobalExceptionHandler`API，支持全局异常处理器的注册。
* 用法例如：
```Kotlin
fun initModelRelation() {
    Movie::class {
        indexBy(Movie::id)
        buildBy(::getMovieByIds)
        targets(
            MovieDTO::class,
            MovieView::class { handleExceptionBy(movieViewHandler) }
        )
        handleExceptionBy(movieHandler)
    }
    registerGlobalExceptionHandler(globalHandler)
}
```
* 异常处理器的实现例如：
```Kotlin
import com.agile4j.model.builder.exception.BaseExceptionContext
import com.agile4j.model.builder.exception.ExceptionHandler
import com.agile4j.model.builder.exception.ModelBuildException

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
class MockExceptionHandler : ExceptionHandler {
    override fun <I : Any, A : Any, T : Any, JR : Any> handleException(
        exceptionContext: BaseExceptionContext<I, A, T, *, *>): JR? {
        println("exceptionContext:$exceptionContext")
        throw ModelBuildException("for test", exceptionContext.throwable)
    }

    companion object {
        val globalHandler = MockExceptionHandler()
        val movieHandler = MockExceptionHandler()
        val movieViewHandler = MockExceptionHandler()
    }
}
```

## 在kts文件中声明Relation
* 之前对Relation的声明，是将代码写在一个function中，并确保function会在世界开始之初，业务代码运行之前被执行。
* 有没有一种办法，可以将Relation的声明，写到"配置文件"中，与代码隔离？毕竟它确实是属于配置的性质，没有逻辑性。
* 答案是肯定的，agile4j-model-builder支持将Relation的声明，以kts脚本的方式，放在工程下的任意目录下（例如resources），并通过[agile4j-kts-loader](https://github.com/agile4j/agile4j-kts-loader)工具，在JVM生命周期内执行脚本代码。
* 将脚本代码放在指定位置指定文件名下，可形成约定，有利于将Relation的声明，统一委托给中间件执行，对业务开发同学屏蔽实现细节。
* 用法例如：
> resources/model-builder-relation.kts
```Kotlin
import com.agile4j.model.builder.mock.Movie
import com.agile4j.model.builder.mock.MovieDTO
import com.agile4j.model.builder.mock.MovieView
import com.agile4j.model.builder.mock.Source
import com.agile4j.model.builder.mock.User
import com.agile4j.model.builder.mock.UserView
import com.agile4j.model.builder.mock.Video
import com.agile4j.model.builder.mock.VideoDTO
import com.agile4j.model.builder.mock.getMovieByIds
import com.agile4j.model.builder.mock.getSourceByIds
import com.agile4j.model.builder.mock.getUserByIds
import com.agile4j.model.builder.mock.getVideoByIds
import com.agile4j.model.builder.relation.buildBy
import com.agile4j.model.builder.relation.indexBy
import com.agile4j.model.builder.relation.invoke
import com.agile4j.model.builder.relation.targets

Movie::class {
    indexBy(Movie::id)
    buildBy(::getMovieByIds)
    targets(MovieDTO::class, MovieView::class)
}

User::class {
    indexBy(User::id)
    buildBy(::getUserByIds)
    targets(UserView::class)
}

Video::class {
    indexBy(Video::id)
    buildBy(::getVideoByIds)
    targets(VideoDTO::class)
}

Source::class {
    indexBy(Source::id)
    buildBy(::getSourceByIds)
}
```

> 执行脚本
```Kotlin
import com.agile4j.kts.loader.eval

eval("src/test/resources/model-builder-relation.kts")
```

## 复用modelBuilder的缓存
* agile4j-model-builder[不会重复构建](README_FEATURE.md#不会重复构建)的特性，给缓存复用提供了可能。
* 如果一个业务场景，需要进行多次target构建，且每次构建的过程对象有交集，可通过复用缓存的形式，提高性能。
* 解决方案是：通过`extractModelBuilder`API，从已构建target中抽取出带有缓存数据的ModelBuilder实例。然后通过`buildXXXWithExistModelBuilder`API复用缓存进行构建。
* 用法例如：
```Kotlin
val target = movieId1 mapSingle MovieView::class
val mb = extractModelBuilder(target!!)
val target2 = buildSingleWithExistModelBuilder(mb!!, MovieView::class, movieId2)
```

## 集成agile4j-feed-builder
* [agile4j-feed-builder](https://github.com/agile4j/agile4j-feed-builder)是用Kotlin语言实现的feed流构建器。
* agile4j-feed-builder与agile4j-model-builder集成后有性能优势。因为一次feed构建过程中的多次资源构建，会共用agile4j-model-builder的缓存。
* 具体用法，详见[集成agile4j-model-builder](https://github.com/agile4j/agile4j-feed-builder#%E9%9B%86%E6%88%90agile4j-model-builder)。

