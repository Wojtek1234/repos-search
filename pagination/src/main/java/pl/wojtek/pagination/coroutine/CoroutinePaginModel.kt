package pl.wojtek.pagination.coroutine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import pl.wojtek.pagination.*

/**
 *

 */

interface CoroutinePaginModel<Q, R, A> {
    fun setQuery(q: Q)
    suspend fun askForMore(): List<R>?
    fun loadingState(): Flow<Boolean>
    fun clear()
}


interface CoroutineDataSource<Q, A> {
    fun askForData(query: QueryParams<Q>): A
}


@ExperimentalCoroutinesApi
internal class CoroutinPaginModelImp<Q, R, A>(private val dataSource: CoroutineDataSource<Q, A>,
                                              private val mapper: DataMapper<A, R, Q>,
                                              private val dataHolder: DataHolder<Q, R> = DataHolderImp(),
                                              private val queryDataHolder: QueryDataHolder<Q> = QueryDataHolderImp()) : CoroutinePaginModel<Q, R, A> {
    private val loadingChannel: ConflatedBroadcastChannel<Boolean> = ConflatedBroadcastChannel(false)
    override fun setQuery(q: Q) {
        queryDataHolder.setQuery(q)
    }

    override suspend fun askForMore(): List<R>? = queryDataHolder.canAskForAnotherOne()
        .takeIf { it && !loadingChannel.value }
        ?.let {
            loadingChannel.send(true)
            queryDataHolder.provideQueryParams()
        }
        ?.let { dataSource.askForData(it) }
        ?.let { mapper.map(it, queryDataHolder.provideQueryParams()) }
        ?.let {
            queryDataHolder.turnToNextPage()
            loadingChannel.send(false)
            queryDataHolder.setMax(it.query, it.max)
            dataHolder.provideData(it.query, it.list)
        }


    override fun loadingState(): Flow<Boolean> = loadingChannel.asFlow()

    override fun clear() {
        loadingChannel.close()
    }


}