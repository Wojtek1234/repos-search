package pl.wojtek

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pl.wojtek.ask.OnRepoClick
import pl.wojtek.searchwithcoroutines.OnRepoClickFromCoroutine

class MainActivity : AppCompatActivity() {

    private val onRepoClick: OnRepoClick by inject()
    private val onRepoClickCoroutine: OnRepoClickFromCoroutine by inject()
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        disposable = onRepoClick.reposStream().subscribe {
            navHostFragment.findNavController()
                .navigate(R.id.action_searchForReposFragment_to_webViewFragment2, Bundle().apply { putString(getString(R.string.url_key), it.urlToRepo) }, null)
        }

        GlobalScope.launch(Dispatchers.Main) {
            onRepoClickCoroutine.reposStream().collect {
                navHostFragment.findNavController().navigate(
                    R.id.action_searchForReposFragmentCor_to_webViewFragment,
                    Bundle().apply { putString(getString(R.string.url_key), it.urlToRepo) },
                    null
                )
            }
        }
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }
}
