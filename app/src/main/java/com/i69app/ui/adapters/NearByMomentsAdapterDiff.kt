package com.i69app.ui.adapters

import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.i69app.GetAllUserMomentsQuery

class NearByMomentsAdapterDiff(
    private val oldList: List<GetAllUserMomentsQuery.Edge>,
    private val newList: List<GetAllUserMomentsQuery.Edge>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].node?.pk == newList[newItemPosition].node?.pk
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].node?.pk != newList[newItemPosition].node?.pk  -> false
                //oldList[oldItemPosition].node?.id != newList[newItemPosition].node?.id -> false
                else -> true
            }
        }
    }