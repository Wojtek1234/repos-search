package pl.wojtek.pagination

import org.koin.dsl.module

/**
 *
 */


val paginationModule=module{
    factory<PaginModelFactory>{PaginModelFactoryImp()}
}