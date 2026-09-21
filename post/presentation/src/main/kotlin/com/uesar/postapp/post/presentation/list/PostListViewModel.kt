package com.uesar.postapp.post.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uesar.postapp.post.domain.model.Post
import com.uesar.postapp.post.domain.repository.PostRepository
import com.uesar.postapp.post.presentation.R
import com.uesar.postapp.post.presentation.model.PostUiModel
import com.uesar.postapp.post.presentation.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PostListViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostListUiState())
    val state: StateFlow<PostListUiState> = _state.asStateFlow()
    private val pendingDeletes = mutableMapOf<Int, PostUiModel>()

    init {
        observePosts()
        loadPosts()
    }

    fun showRefreshConfirmation() {
        _state.update { it.copy(isRefreshConfirmationVisible = true) }
    }

    fun dismissRefreshConfirmation() {
        _state.update { it.copy(isRefreshConfirmationVisible = false) }
    }

    fun confirmRefresh() {
        _state.update { it.copy(isRefreshConfirmationVisible = false) }
        loadPosts(forceRefresh = true)
    }

    fun deletePost(postId: Int) {
        if (postId in pendingDeletes) return
        val post = _state.value.posts.firstOrNull { it.id == postId } ?: return
        pendingDeletes[postId] = post
        viewModelScope.launch {
            postRepository.deletePost(post.id)
            showSnackbar(PostListSnackbarMessage(R.string.post_deleted, post.id))
        }
    }

    fun dismissSnackbar() {
        val message = takeSnackbarMessage() ?: return
        message.deletedPostId?.let { pendingDeletes.remove(it) }
    }

    fun undoDelete() {
        val postId = _state.value.snackbarMessage?.deletedPostId ?: return
        takeSnackbarMessage()
        val post = pendingDeletes.remove(postId) ?: return
        viewModelScope.launch {
            postRepository.restorePost(Post(post.id, post.title, post.body))
        }
    }

    private fun showSnackbar(message: PostListSnackbarMessage) {
        dismissSnackbar()
        _state.update { it.copy(snackbarMessage = message) }
    }

    private fun takeSnackbarMessage(): PostListSnackbarMessage? {
        val message = _state.value.snackbarMessage ?: return null
        _state.update { it.copy(snackbarMessage = null) }
        return message
    }

    private fun observePosts() {
        viewModelScope.launch {
            try {
                postRepository.observePosts().collect { posts ->
                    _state.update { currentState ->
                        currentState.copy(posts = posts.map { post ->
                            post.toUiModel()
                        })
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = R.string.error_storage) }
            }
        }
    }

    private fun loadPosts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                postRepository.syncPosts(forceRefresh)
                _state.update { it.copy(
                    isLoading = false,
                    errorMessage = null
                ) }
            } catch (_: Exception) {
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = R.string.error_load_posts.takeIf {
                            currentState.posts.isEmpty()
                        }
                    )
                }
                if (_state.value.posts.isNotEmpty()) {
                    showSnackbar(PostListSnackbarMessage(R.string.error_load_posts))
                }
            }
        }
    }
}
