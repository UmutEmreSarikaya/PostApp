package com.uesar.postapp.post.domain.repository

import com.uesar.postapp.post.domain.model.Post

interface PostRepository {
    suspend fun getPosts(): List<Post>
}
