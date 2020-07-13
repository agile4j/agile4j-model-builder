package com.agile4j.model.builder

/**
 * 0. 增加对 Movie::class outJoin VIDEOS by ::getVideo【Ids】ByMovieIds 的支持
 * 0. 增加对 Movie::class join User::class by Movie::【author】 的支持
 * ——DONE 0. 支持根据伴生对象build目标对象
 * ——DONE 0. 验证对循环依赖的支持——get没问题，但toJSON时死循环的问题
 * ——DONE 0. 修复内部构建target时，modelBuilder未重用的问题
 * ——DONE 1. ModelBuilderDelegate有内存泄露风险，解决方案有两个：
 *      1). 反射动态添加字段——不可行
 *      2). 弱引用避免阻碍垃圾回收——WeakIdentityHashMap——DONE
 * 2. 校验：BuildMultiPair<KClass<T>>.by
 *      1). targetClazz是否已通过accompanyBy注册到BuildContext
 *      2). accompanyClazz与targetClazz单参构造函数参数类型是否匹配
 *      3). accompanyClazz是否非待构建类型(例如View)，是否为可build类型(例如DbModel)
 *      4). "outJoin SHARED"，outJoinPoint只能有一个
 *      5). ...
 * 3. CacheAccessor,未查到记录也可以记录下来，避免缓存击穿
 * 4. 代理属性不支持set合理么
 * 5. ModelBuilder::accompanyMap有必要存在么，或者说，有必要是map么
 * 6. error("")，把错误信息描述一下
 * ——DONE 7. 循环依赖怎么办？判断是否易注值，注值则停止？或者提前检查禁止循环依赖？
 * ——DONE 8. delegate.Join.buildTarget时，构建出来的targets是cache了的么？有办法cache么
 * 9. buildByAccompanyIndex，如何做到主动校验，类型不匹配抛出异常
 * @author liurenpeng
 * Created on 2020-06-17
 */