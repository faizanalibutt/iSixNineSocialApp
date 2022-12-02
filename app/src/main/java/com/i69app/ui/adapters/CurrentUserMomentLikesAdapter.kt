package com.i69app.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.i69app.GetSelfMomentLikesQuery
import com.i69app.GetUserMomentsQuery
import com.i69app.databinding.ItemUserMomentLikeBinding

class CurrentUserMomentLikesAdapter(
    private val ctx: Context,
    private val listener: CurrentUserLikesUsers,
    private var currentUserMomentLikesList: ArrayList<GetSelfMomentLikesQuery.SelfMomentLike>,
    var userId: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun getViewHolderByType(type: Int, viewBinding: ViewDataBinding) = UserMomentLikesHolder(viewBinding as ItemUserMomentLikeBinding)


    private fun getViewDataBinding(viewType: Int, parent: ViewGroup) = ItemUserMomentLikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = getViewHolderByType(viewType, getViewDataBinding(viewType, parent))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as UserMomentLikesHolder
        val item = currentUserMomentLikesList[position]
        holder.bindData(position, item)
    }


    override fun getItemCount(): Int = if (currentUserMomentLikesList.size == 0) 0 else currentUserMomentLikesList.size


    fun add(r: GetSelfMomentLikesQuery.SelfMomentLike) {
        currentUserMomentLikesList.add(r)
        notifyItemInserted(currentUserMomentLikesList.size - 1)
    }

    fun addAll(newdata: List<GetSelfMomentLikesQuery.SelfMomentLike?>?) {
        newdata?.indices?.forEach { i ->
             newdata[i]?.let { add(it) }
        }
    }

    inner class UserMomentLikesHolder(val binding: ItemUserMomentLikeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(pos: Int, likedUsers: GetSelfMomentLikesQuery.SelfMomentLike?) {
            binding.userLikeText.text = likedUsers?.user?.username
            binding.userLikeText.setOnClickListener {
                listener.openLikedUserProfile(likedUsers?.user?.id)
            }
        }
    }

    interface CurrentUserLikesUsers {
        fun openLikedUserProfile(id: String?)
    }
}