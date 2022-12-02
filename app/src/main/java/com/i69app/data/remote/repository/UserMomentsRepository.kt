package com.i69app.data.remote.repository

import android.util.Log
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.i69app.GetAllUserMomentsQuery
import com.i69app.data.models.User
import com.i69app.data.remote.api.GraphqlApi
import com.i69app.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class UserMomentsRepository  @Inject constructor(
    private val api: GraphqlApi,
) {
    fun getUserMoments(viewModelScope: CoroutineScope, token: String,width: Int, size: Int, i: Int, endCursors: String, callback: (ArrayList<GetAllUserMomentsQuery.Edge>,endCursor: String,hasNextPage: Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = try {
                apolloClientForContore(coroutineContext, token).query(GetAllUserMomentsQuery(width = width, characterSize = size,first=i,endCursors,"")).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                return@launch
            }
            if(res.hasErrors())
            {
                Log.e("rr2rrr","-->"+res.errors!!.get(0).nonStandardFields!!.get("code").toString())
                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")
                    viewModelScope.launch(Dispatchers.Main) {
                    }
                }
            }

            val allmoments = res.data?.allUserMoments!!.edges
            var endCursor: String=""
            var hasNextPage: Boolean= false
            endCursor = res.data?.allUserMoments?.pageInfo?.endCursor ?: ""
            hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage
            if(allmoments.size!=0)
            {

                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()


                allmoments.indices.forEach { i ->
                    allUserMomentsFirst.add(allmoments[i]!!)
                }
               callback(allUserMomentsFirst,endCursor,hasNextPage,null)
            }
        }
    }
    private suspend fun getAllMoments(
        token: String,
        query: String,
        getMomentQuryName: String,
        callback: (List<User>, List<User>, List<User>, String?) -> Unit
    ) {
        try {
            val result = api.callApi(token = "Token $token", body = query.getGraphqlApiBody())
            val jsonObject = Gson().fromJson(result.body(), JsonObject::class.java)
            Timber.i("Query: $query")
            Timber.w("Result: ${result.body()}")

            when {
                result.isSuccessful -> {
                    val allMomentsList = ArrayList<User>()

                    val dataJsonObject = jsonObject["data"].asJsonObject
                    val allMometsArrays = dataJsonObject[getMomentQuryName].asJsonArray

                    allMometsArrays.forEach { jsonElement ->
                        val json = Gson().fromJson(jsonElement, User::class.java)
                        allMomentsList.add(json)
                    }


                    val error: String? = if (jsonObject.has("errors")) {
                        jsonObject["errors"].asJsonArray[0].asJsonObject["message"].asString
                    } else {
                        null
                    }

                    if (error.isNullOrEmpty())
                        callback.invoke(allMomentsList, emptyList(), emptyList(),null)
                    else
                        callback.invoke(emptyList(), emptyList(), emptyList(), error)

                }
                else -> callback.invoke(emptyList(), emptyList(), emptyList(), "Something went wrong! Please try again later!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback.invoke(emptyList(), emptyList(), emptyList(), "Something went wrong! Please try again later!")
        }
    }

}