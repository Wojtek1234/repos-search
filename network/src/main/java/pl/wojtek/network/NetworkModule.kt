package pl.wojtek.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 *
 */
private const val TIMEOUT = 30L
val networkModule = module {


    single {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {

                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                this.addInterceptor(loggingInterceptor)
            }

        }
            .build()
    }

    single {
        GsonConverterFactory.create(
            GsonBuilder()
                .setLenient()
                .create()
        )
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.github.com")
            .client(get())
            .addConverterFactory(get<GsonConverterFactory>())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }
}

inline fun <reified T> Scope.getApi() = get<Retrofit>().create(T::class.java)