package com.uesar.postapp.post.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.uesar.postapp.post.domain.model.Post
import com.uesar.postapp.post.domain.repository.PostRepository
import com.uesar.postapp.post.presentation.R
import com.uesar.postapp.post.presentation.model.toUiModel
import com.uesar.postapp.post.presentation.navigation.PostRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val postId = savedStateHandle.toRoute<PostRoute.PostDetail>().postId
    private val _state = MutableStateFlow(PostDetailUiState())
    val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

    init {
        loadPost()
    }

    fun loadPost() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val post = postRepository.getPost(postId)
            _state.update {
                it.copy(
                    isLoading = false,
                    post = post?.toUiModel(),
                    title = post?.title.orEmpty(),
                    body = post?.body.orEmpty(),
                    errorMessage = if (post == null) R.string.error_post_not_found else null
                )
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title, saveErrorMessage = null) }
    }

    fun updateBody(body: String) {
        _state.update { it.copy(body = body, saveErrorMessage = null) }
    }

    fun requestBack(onBack: () -> Unit) {
        if (_state.value.hasUnsavedChanges) {
            _state.update { it.copy(isDiscardConfirmationVisible = true) }
        } else {
            onBack()
        }
    }

    fun dismissDiscardConfirmation() {
        _state.update { it.copy(isDiscardConfirmationVisible = false) }
    }

    fun savePost() {
        val currentState = _state.value
        val currentPost = currentState.post ?: return
        if (!currentState.canSave) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveErrorMessage = null) }
            postRepository.updatePost(
                Post(id = currentPost.id, title = currentState.title.trim(), body = currentState.body.trim())
            )
            _state.update {
                it.copy(
                    isSaving = false,
                    title = currentState.title.trim(),
                    body = currentState.body.trim(),
                    isDiscardConfirmationVisible = false,
                    post = currentPost.copy(
                        title = currentState.title.trim(),
                        body = currentState.body.trim()
                    )
                )
            }
        }
    }
}
