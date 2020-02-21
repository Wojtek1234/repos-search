package pl.wojtek.searchwithcoroutines.data.api


import pl.wojtek.pagination.QueryParams
import pl.wojtek.pagination.coroutine.CoroutineDataSource
import pl.wojtek.searchwithcoroutines.network.SearchResult

/**
 *
 */



class ReposDataSource(private val api: SearchReposApi):CoroutineDataSource<String, SearchResult>{


    override suspend fun askForData(query: QueryParams<String>): SearchResult = api.searchRepos(query.query?:"",query.page,query.pageSize)
}