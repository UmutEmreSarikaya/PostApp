package com.uesar.postapp.post.data

internal data class PostDto(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)
