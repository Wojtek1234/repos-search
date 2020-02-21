package pl.wojtek.pagination

import org.koin.dsl.module
import pl.wojtek.pagination.coroutine.CoroutinePaginModelFactory
import pl.wojtek.pagination.coroutine.CoroutinePaginModelFactoryImp

/**
 *
 */


val paginationModule=module{
    factory<PaginModelFactory>{PaginModelFactoryImp()}
}
val coroutinePaginationModule = module {
    factory<CoroutinePaginModelFactory> { CoroutinePaginModelFactoryImp() }
}