package com.i69app.ui.viewModels


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.i69app.GetAllUserMomentsQuery
import com.i69app.data.remote.repository.UserMomentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserMomentsModelView @Inject constructor(private val userMomentsRepo: UserMomentsRepository): ViewModel() {
    val userMomentsList=ArrayList<GetAllUserMomentsQuery.Edge>()
    //val arrayListLiveData = MutableLiveData<ArrayList<GetAllUserMomentsQuery.Edge>>()
    var endCursorN: String=""
    var hasNextPageN: Boolean= false
    val errorMessage=MutableLiveData<String>()
    fun getAllMoments( token: String,width: Int, size: Int, i: Int, endCursors: String, callback: (String?) -> Unit) {
        userMomentsRepo.getUserMoments(viewModelScope, token = token, width = width, size = size,i=i, endCursors = endCursors) { allUserMoments,endCursor,hasNextPage, error ->
            //this.userMomentsList.clear()
            this.userMomentsList.addAll(allUserMoments)
            endCursorN=endCursor
            hasNextPageN=hasNextPage
            //this.arrayListLiveData.postValue(allUserMoments)
            callback.invoke(error)
        }
    }
}