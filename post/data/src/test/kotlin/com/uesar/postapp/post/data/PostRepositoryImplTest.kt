package com.uesar.postapp.post.data

import com.uesar.postapp.core.db.PostDao
import com.uesar.postapp.core.db.PostEntity
import com.uesar.postapp.core.db.SyncStateEntity
import com.uesar.postapp.post.domain.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class PostRepositoryImplTest {
    private val dao = FakePostDao()

    @Test
    fun `initial sync fetches once and preserves subsequent local changes`() = runBlocking {
        dao.initialFetchComplete = false
        dao.posts = emptyList()
        var fetchCount = 0
        val repository = repository {
            fetchCount++
            listOf(PostDto(userId = 1, id = 2, title = "Remote", body = "Fetched post"))
        }

        repository.syncPosts()
        assertEquals(listOf(Post(2, "Remote", "Fetched post")), repository.observePosts().first())

        repository.deletePost(2)
        repository.syncPosts()

        assertEquals(1, fetchCount)
        assertEquals(emptyList<Post>(), repository.observePosts().first())
    }

    @Test
    fun `forced sync replaces cached posts and publishes through observer`() = runBlocking {
        val repository = repository {
            listOf(PostDto(userId = 1, id = 2, title = "Remote", body = "Fetched post"))
        }
        val emissions = Channel<List<Post>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observePosts().collect { emissions.send(it) }
        }
        try {
            withTimeout(5_000) {
                assertEquals(listOf(Post(1, "Saved", "Cached post")), emissions.receive())
                repository.syncPosts(forceRefresh = true)
                assertEquals(listOf(Post(2, "Remote", "Fetched post")), emissions.receive())
            }
        } finally {
            observer.cancel()
            emissions.close()
        }
    }

    @Test
    fun `observer receives edits deletions and undo without reloading`() = runBlocking {
        val repository = repository()
        val emissions = Channel<List<Post>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observePosts().collect { emissions.send(it) }
        }
        try {
            withTimeout(5_000) {
                assertEquals(listOf(Post(1, "Saved", "Cached post")), emissions.receive())

                val edited = Post(1, "Edited", "New body")
                repository.updatePost(edited)
                assertEquals(listOf(edited), emissions.receive())

                repository.deletePost(edited.id)
                assertEquals(emptyList<Post>(), emissions.receive())

                repository.restorePost(edited)
                assertEquals(listOf(edited), emissions.receive())
            }
        } finally {
            observer.cancel()
            emissions.close()
        }
    }

    @Test
    fun `failed refresh exposes connection error and keeps cached posts`() {
        val cause = IOException("Offline")
        val repository = repository { throw cause }

        val error = assertThrows(IOException::class.java) {
            runBlocking { repository.syncPosts(forceRefresh = true) }
        }

        assertSame(cause, error)
        assertEquals(listOf(cachedPost), dao.posts)
    }

    @Test
    fun `initial fetch propagates HTTP error`() {
        dao.initialFetchComplete = false
        val cause = HttpException(Response.error<List<PostDto>>(503, "Unavailable".toResponseBody()))
        val repository = repository { throw cause }

        val error = assertThrows(HttpException::class.java) {
            runBlocking { repository.syncPosts() }
        }

        assertSame(cause, error)
    }

    @Test
    fun `sync state read propagates storage error`() {
        val cause = IllegalStateException("Database unavailable")
        dao.readFailure = cause
        val repository = repository()

        val error = assertThrows(cause.javaClass) {
            runBlocking { repository.syncPosts() }
        }

        assertSame(cause, error)
    }

    @Test
    fun `refresh write propagates storage error`() {
        val cause = IOException("Storage unavailable")
        dao.writeFailure = cause
        val repository = repository()

        val error = assertThrows(cause.javaClass) {
            runBlocking { repository.syncPosts(forceRefresh = true) }
        }

        assertSame(cause, error)
    }

    @Test
    fun `storage cancellation propagates unchanged`() {
        val cause = CancellationException("Cancelled")
        dao.readFailure = cause
        val repository = repository()

        val error = assertThrows(CancellationException::class.java) {
            runBlocking { repository.syncPosts() }
        }

        assertSame(cause, error)
    }

    @Test
    fun `network cancellation propagates unchanged`() {
        val cause = CancellationException("Cancelled")
        val repository = repository { throw cause }

        val error = assertThrows(CancellationException::class.java) {
            runBlocking { repository.syncPosts(forceRefresh = true) }
        }

        assertSame(cause, error)
    }

    @Test
    fun `update post persists title and body in storage`() = runBlocking {
        val repository = repository()

        repository.updatePost(Post(id = 1, title = "Updated", body = "Updated body"))

        assertEquals(
            listOf(PostEntity(id = 1, title = "Updated", body = "Updated body")),
            dao.posts
        )
    }

    @Test
    fun `deleted post can be restored in storage`() = runBlocking {
        val repository = repository()

        repository.deletePost(cachedPost.id)
        assertEquals(emptyList<PostEntity>(), dao.posts)

        repository.restorePost(Post(cachedPost.id, cachedPost.title, cachedPost.body))
        assertEquals(listOf(cachedPost), dao.posts)
    }

    private fun repository(fetch: suspend () -> List<PostDto> = { emptyList() }) =
        PostRepositoryImpl(object : PostService {
            override suspend fun getPosts(): List<PostDto> = fetch()
        }, dao)

    private class FakePostDao : PostDao {
        private val postsFlow = MutableStateFlow(listOf(cachedPost))
        var posts: List<PostEntity>
            get() = postsFlow.value
            set(value) { postsFlow.value = value }

        override fun observePosts() = postsFlow
        var initialFetchComplete = true
        var readFailure: Exception? = null
        var writeFailure: Exception? = null

        override suspend fun getPost(postId: Int): PostEntity? = posts.find { it.id == postId }

        override suspend fun isInitialFetchComplete(): Boolean {
            readFailure?.let { throw it }
            return initialFetchComplete
        }

        override suspend fun upsertPosts(posts: List<PostEntity>) {
            this.posts = posts
        }

        override suspend fun updatePost(post: PostEntity): Int {
            writeFailure?.let { throw it }
            val index = posts.indexOfFirst { it.id == post.id }
            if (index == -1) return 0
            posts = posts.toMutableList().also { it[index] = post }
            return 1
        }

        override suspend fun upsertPost(post: PostEntity) {
            writeFailure?.let { throw it }
            posts = posts.filterNot { it.id == post.id } + post
        }

        override suspend fun deletePost(postId: Int) {
            writeFailure?.let { throw it }
            posts = posts.filterNot { it.id == postId }
        }

        override suspend fun deletePosts() {
            posts = emptyList()
        }

        override suspend fun insertSyncState(syncState: SyncStateEntity) {
            initialFetchComplete = true
        }

        override suspend fun replacePosts(posts: List<PostEntity>) {
            writeFailure?.let { throw it }
            this.posts = posts
        }
    }

    private companion object {
        val cachedPost = PostEntity(id = 1, title = "Saved", body = "Cached post")
    }
}
