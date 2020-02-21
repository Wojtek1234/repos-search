package pl.wojtek.searchwithcoroutines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.search_for_repos_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.dsl.module
import pl.wojtek.network.getApi
import pl.wojtek.pagination.coroutine.CoroutinePaginModelFactory
import pl.wojtek.pagination.coroutinePaginationModule
import pl.wojtek.searchwithcoroutines.data.ReposDataMapper
import pl.wojtek.searchwithcoroutines.data.api.ReposDataSource


@ExperimentalCoroutinesApi
class SearchForReposFragment : Fragment() {


    private val viewModel: SearchForReposViewModel by viewModel()
    private val onRepoClickFromCoroutine: OnRepoClickFromCoroutine by inject()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_for_repos_fragment, container, false)
    }


    private val adapter by lazy {
        RepoAdapter {
            onRepoClickFromCoroutine.onRepoClick(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repositoryRecyclerView.adapter = adapter
        repositoryRecyclerView.itemAnimator = DefaultItemAnimator()

        viewModel.listOfRepos.observe(this) {

            adapter.submitList(it)
        }

        searchText.doOnTextChanged { text, _, _, _ ->
            viewModel.query(text?.toString() ?: "")
        }

        refreshLayout.isEnabled = false


        viewModel.loading.observe(this) {
            refreshLayout.isRefreshing = it
        }


        viewModel.errors.observe(this) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }


        repositoryRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.loadMore()
                }
            }
        })
    }



}


val searchFragmentCoroutineModule = module {

    factory { ReposDataSource(getApi()) }
    factory { ReposDataMapper() }
    single<OnRepoClickFromCoroutine> { OnRepoClickImp(get()) }
    viewModel {
        SearchForReposViewModel(
            get(),
            get<CoroutinePaginModelFactory>().createPaginModel(get<ReposDataSource>(), get<ReposDataMapper>())
        )
    }
}.plus(coroutinePaginationModule)
