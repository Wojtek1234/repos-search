package pl.wojtek.ask

import com.poccofinance.core.rx.SchedulerUtils
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import pl.wojtek.ask.data.Repository

/**
 *
 */


interface OnRepoClick {
    fun onRepoClick(repo: Repository)
    fun reposStream(): Flowable<Repository>
}


class OnRepoClickImp(private val schedulerUtils: SchedulerUtils) : OnRepoClick {
    private val repoStream: PublishProcessor<Repository> = PublishProcessor.create()
    override fun onRepoClick(repo: Repository) {
        repoStream.onNext(repo)
    }

    override fun reposStream(): Flowable<Repository> = repoStream
}