package pl.wojtek.ask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.poccofinance.core.rx.SchedulerUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.search_for_repos_fragment.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.dsl.module
import pl.wojtek.ask.data.ReposDataMapper
import pl.wojtek.ask.data.api.ReposDataSource
import pl.wojtek.network.getApi
import pl.wojtek.pagination.PaginModelImp


class SearchForReposFragment : Fragment() {


    private val viewModel: SearchForReposViewModel by viewModel()
    private val onRepoClick:OnRepoClick by inject()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_for_repos_fragment, container, false)
    }


    private lateinit var compositeDisposable: CompositeDisposable
    private val adapter by lazy {
        RepoAdapter {
            onRepoClick.onRepoClick(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repositoryRecyclerView.adapter = adapter
        repositoryRecyclerView.itemAnimator = DefaultItemAnimator()
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(viewModel.listOfRepos().subscribe {
            adapter.submitList(it)
        })

        searchText.doOnTextChanged { text, _, _, _ ->
            viewModel.query(text?.toString()?:"")
        }

        refreshLayout.isEnabled = false
        compositeDisposable.add(
            viewModel.loading().subscribe {
                refreshLayout.isRefreshing = it
            }
        )
        compositeDisposable.add(
            viewModel.errors().subscribe {
                Toast.makeText(requireContext(),it,Toast.LENGTH_SHORT).show()
            }
        )

        repositoryRecyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                  viewModel.loadMore()
                }
            }
        })
    }

    override fun onDestroyView() {
        compositeDisposable.dispose()
        super.onDestroyView()

    }
}


val searchFragmentModule = module {

    factory { ReposDataSource(getApi()) }
    factory { ReposDataMapper() }
    single<OnRepoClick> {  OnRepoClickImp(get()) }
    viewModel { SearchForReposViewModel(get(), PaginModelImp(get<ReposDataSource>(), get<ReposDataMapper>()), get<SchedulerUtils>().subscribeScheduler) }
}
