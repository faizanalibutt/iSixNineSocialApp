package com.i69app.data.models

data class BlockedUser(
    val id: String,
    val username: String,
    val avatarPhotos: List<com.i69app.data.models.Photo>? = emptyList(),
    val fullName:String
)
