package pl.wojtek.ask.data.api

import io.reactivex.Maybe
import pl.wojtek.ask.network.SearchResult
import pl.wojtek.pagination.DataSource
import pl.wojtek.pagination.QueryParams

/**
 *
 */



class ReposDataSource(private val api:SearchReposApi):DataSource<String,SearchResult>{
    override fun askForData(query: QueryParams<String>): Maybe<SearchResult> {
       return api.searchRepos(query = query.query?:"",page = query.page,pageSize = query.pageSize)
    }
}