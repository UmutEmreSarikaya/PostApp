package com.uesar.postapp.post.data

import retrofit2.http.GET

internal interface PostService {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>
}
