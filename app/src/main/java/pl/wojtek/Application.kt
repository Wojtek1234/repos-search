package pl.wojtek

import android.app.Application
import com.poccofinance.core.rx.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pl.wojtek.ask.searchFragmentModule
import pl.wojtek.network.networkModule

/**
 *
 */



class Application: Application(){

    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@Application)
            modules(listOf(coreModule, networkModule)+ searchFragmentModule)
        }
    }
}