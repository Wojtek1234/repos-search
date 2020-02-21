package pl.wojtek.searchwithcoroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import pl.wojtek.core.CoroutineUtils
import pl.wojtek.searchwithcoroutines.data.Repository

/**
 *
 */


interface OnRepoClickFromCoroutine {
    fun onRepoClick(repo: Repository)
    fun reposStream(): Flow<Repository>
}

@UseExperimental(ExperimentalCoroutinesApi::class)
class OnRepoClickImp(private val coroutinesUtils: CoroutineUtils) : OnRepoClickFromCoroutine {

    private val repoStream: ConflatedBroadcastChannel<Repository> = ConflatedBroadcastChannel()
    override fun onRepoClick(repo: Repository) {
        coroutinesUtils.globalScope.launch {
            repoStream.send(repo)
        }
    }

    override fun reposStream(): Flow<Repository> = repoStream.asFlow()
}