package com.uesar.postapp.post.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uesar.postapp.post.domain.model.Post
import com.uesar.postapp.post.domain.repository.PostRepository
import com.uesar.postapp.post.presentation.detail.PostDetailViewModel
import com.uesar.postapp.post.presentation.list.PostListViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PostViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val original = Post(1, "Original title", "Original body")
    private val repository = FakePostRepository(listOf(original))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun list() = PostListViewModel(repository).also { store.put("list", it) }
    private fun detail(id: Int = 1) = PostDetailViewModel(
        repository, SavedStateHandle(mapOf("postId" to id))
    ).also { store.put("detail", it) }

    @Test fun `initial load observes cached posts and finishes loading`() = runTest(dispatcher) {
        val vm = list()
        assertTrue(vm.state.value.isLoading)
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf(false), repository.syncRequests)
        assertEquals("Original title", vm.state.value.posts.single().title)
        repository.posts.value = listOf(original.copy(title = "Changed elsewhere"))
        runCurrent()
        assertEquals("Changed elsewhere", vm.state.value.posts.single().title)
    }

    @Test fun `refresh only fetches after confirmation`() = runTest(dispatcher) {
        val vm = list()
        runCurrent()
        vm.showRefreshConfirmation()
        assertTrue(vm.state.value.isRefreshConfirmationVisible)
        vm.dismissRefreshConfirmation()
        assertFalse(vm.state.value.isRefreshConfirmationVisible)
        assertEquals(listOf(false), repository.syncRequests)
        vm.showRefreshConfirmation()
        vm.confirmRefresh()
        runCurrent()
        assertFalse(vm.state.value.isRefreshConfirmationVisible)
        assertEquals(listOf(false, true), repository.syncRequests)
    }

    @Test fun `failed initial load shows full screen error`() = runTest(dispatcher) {
        repository.posts.value = emptyList()
        repository.syncFailure = IOException("Offline")
        val vm = list()
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertEquals(R.string.error_load_posts, vm.state.value.errorMessage)
        assertNull(vm.state.value.snackbarMessage)
    }

    @Test fun `failed refresh preserves cached content and shows snackbar`() = runTest(dispatcher) {
        val vm = list()
        runCurrent()
        repository.syncFailure = IOException("Offline")
        vm.confirmRefresh()
        runCurrent()
        assertEquals(1, vm.state.value.posts.size)
        assertNull(vm.state.value.errorMessage)
        assertEquals(R.string.error_load_posts, vm.state.value.snackbarMessage?.message)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun `delete can be undone once and duplicate requests are ignored`() = runTest(dispatcher) {
        val vm = list()
        runCurrent()
        vm.deletePost(1)
        vm.deletePost(1)
        runCurrent()
        assertTrue(vm.state.value.posts.isEmpty())
        assertEquals(listOf(1), repository.deletions)
        assertEquals(1, vm.state.value.snackbarMessage?.deletedPostId)
        vm.undoDelete()
        runCurrent()
        vm.undoDelete()
        runCurrent()
        assertEquals(listOf(original), repository.restorations)
        assertEquals(1, vm.state.value.posts.single().id)
        assertNull(vm.state.value.snackbarMessage)
    }

    @Test fun `dismissed deletion cannot be undone and unknown deletion is ignored`() = runTest(dispatcher) {
        val vm = list()
        runCurrent()
        vm.deletePost(99)
        runCurrent()
        assertTrue(repository.deletions.isEmpty())
        vm.deletePost(1)
        runCurrent()
        vm.dismissSnackbar()
        vm.undoDelete()
        runCurrent()
        assertTrue(repository.restorations.isEmpty())
        assertTrue(vm.state.value.posts.isEmpty())
    }

    @Test fun `detail loads requested post and missing post shows error`() = runTest(dispatcher) {
        val vm = detail()
        runCurrent()
        assertEquals(original.title, vm.state.value.title)
        assertEquals(original.body, vm.state.value.body)
        assertFalse(vm.state.value.isLoading)
        assertFalse(vm.state.value.canSave)
        val missing = detail(99)
        runCurrent()
        assertNull(missing.state.value.post)
        assertEquals(R.string.error_post_not_found, missing.state.value.errorMessage)
        assertFalse(missing.state.value.isLoading)
    }

    @Test fun `back navigation prompts only while edits are unsaved`() = runTest(dispatcher) {
        val vm = detail()
        runCurrent()
        var backCalls = 0
        vm.requestBack { backCalls++ }
        assertEquals(1, backCalls)
        vm.updateTitle("Edited")
        vm.requestBack { backCalls++ }
        assertEquals(1, backCalls)
        assertTrue(vm.state.value.isDiscardConfirmationVisible)
        vm.dismissDiscardConfirmation()
        assertFalse(vm.state.value.isDiscardConfirmationVisible)
        vm.updateTitle(original.title)
        vm.requestBack { backCalls++ }
        assertEquals(2, backCalls)
    }

    @Test fun `blank or unchanged fields do not save`() = runTest(dispatcher) {
        val vm = detail()
        runCurrent()
        vm.savePost()
        vm.updateTitle("  ")
        assertFalse(vm.state.value.canSave)
        vm.savePost()
        vm.updateTitle("Edited")
        vm.updateBody("\n ")
        assertFalse(vm.state.value.canSave)
        vm.savePost()
        runCurrent()
        assertTrue(repository.updates.isEmpty())
    }

    @Test fun `save trims edits and resets dirty state after persistence`() = runTest(dispatcher) {
        val vm = detail()
        runCurrent()
        repository.saveGate = CompletableDeferred()
        vm.updateTitle("  Edited title  ")
        vm.updateBody("  Edited body  ")
        assertTrue(vm.state.value.canSave)
        vm.savePost()
        runCurrent()
        assertTrue(vm.state.value.isSaving)
        assertFalse(vm.state.value.canSave)
        vm.savePost()
        repository.saveGate!!.complete(Unit)
        runCurrent()
        assertEquals(listOf(Post(1, "Edited title", "Edited body")), repository.updates)
        assertFalse(vm.state.value.isSaving)
        assertFalse(vm.state.value.hasUnsavedChanges)
        assertEquals("Edited title", vm.state.value.title)
        assertEquals("Edited body", vm.state.value.body)
    }

    private class FakePostRepository(initial: List<Post>) : PostRepository {
        val posts = MutableStateFlow(initial)
        val syncRequests = mutableListOf<Boolean>()
        val deletions = mutableListOf<Int>()
        val restorations = mutableListOf<Post>()
        val updates = mutableListOf<Post>()
        var syncFailure: Exception? = null
        var saveGate: CompletableDeferred<Unit>? = null
        override fun observePosts() = posts
        override suspend fun syncPosts(forceRefresh: Boolean) {
            syncRequests += forceRefresh
            syncFailure?.let { throw it }
        }
        override suspend fun getPost(postId: Int) = posts.value.find { it.id == postId }
        override suspend fun updatePost(post: Post) {
            saveGate?.await()
            updates += post
            posts.value = posts.value.map { if (it.id == post.id) post else it }
        }
        override suspend fun deletePost(postId: Int) {
            deletions += postId
            posts.value = posts.value.filterNot { it.id == postId }
        }
        override suspend fun restorePost(post: Post) {
            restorations += post
            posts.value = posts.value.filterNot { it.id == post.id } + post
        }
    }
}
