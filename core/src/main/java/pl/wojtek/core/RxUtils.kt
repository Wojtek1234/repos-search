package com.poccofinance.core.rx


import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.dsl.module
import java.util.concurrent.Executors

/**
 *
 */

interface SchedulerUtils{
    val observScheduler: Scheduler
    val subscribeScheduler:Scheduler
}



val coreModule = module{

    single<SchedulerUtils> { object :SchedulerUtils{
        override val observScheduler: Scheduler =  AndroidSchedulers.mainThread()
        override val subscribeScheduler: Scheduler  =   Schedulers.from(Executors.newFixedThreadPool(4))

    } }

}
