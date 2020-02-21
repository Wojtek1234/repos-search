package pl.wojtek.searchwithcoroutines

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.vh_repos_element.view.*
import pl.wojtek.searchwithcoroutines.data.Repository

/**
 *
 */

internal class ReposAdapter(view: View) : RecyclerView.ViewHolder(view)

internal class RepoAdapter(private val click: (Repository) -> Unit) :
    ListAdapter<Repository, ReposAdapter>(object : DiffUtil.ItemCallback<Repository>() {
        override fun areItemsTheSame(oldItem: Repository, newItem: Repository): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Repository, newItem: Repository): Boolean = oldItem == newItem
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReposAdapter =
        LayoutInflater.from(parent.context).inflate(R.layout.vh_repos_element, parent, false).let { ReposAdapter(it) }

    override fun onBindViewHolder(holder: ReposAdapter, position: Int) {
        with(getItem(position)) {

            holder.itemView.vhRepoTitleText.text = repositoryName
            holder.itemView.setOnClickListener {
                click(this)
            }
            holder.itemView.vhRepoOwnerName.text = ownerName
            holder.itemView.vhRepoStartText.text = score.toString()
            holder.itemView.vhRepoUrlText.text = urlToRepo
        }
    }
}