package com.uesar.postapp.post.presentation.list

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.uesar.postapp.post.presentation.R
import com.uesar.postapp.post.presentation.model.PostUiModel

@Composable
fun PostListScreenRoot(
    onPostClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: PostListViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    PostListScreen(
        state = state,
        onPostClick = onPostClick,
        onRefreshClick = viewModel::showRefreshConfirmation,
        onRefreshConfirm = viewModel::confirmRefresh,
        onRefreshDismiss = viewModel::dismissRefreshConfirmation,
        onDeletePost = viewModel::deletePost,
        onSnackbarDismiss = viewModel::dismissSnackbar,
        onUndoDelete = viewModel::undoDelete,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostListScreen(
    state: PostListUiState,
    onPostClick: (Int) -> Unit,
    onRefreshClick: () -> Unit,
    onRefreshConfirm: () -> Unit,
    onRefreshDismiss: () -> Unit,
    onDeletePost: (Int) -> Unit,
    onSnackbarDismiss: () -> Unit,
    onUndoDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isRefreshConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onRefreshDismiss,
            title = { Text(text = stringResource(R.string.refresh_confirmation_title)) },
            text = { Text(text = stringResource(R.string.refresh_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onRefreshConfirm) {
                    Text(text = stringResource(R.string.action_refresh))
                }
            },
            dismissButton = {
                TextButton(onClick = onRefreshDismiss) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            state.snackbarMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(12.dp).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    action = {
                        if (message.deletedPostId != null) {
                            TextButton(onClick = onUndoDelete) {
                                Text(stringResource(R.string.action_undo))
                            }
                        }
                    },
                    dismissAction = {
                        TextButton(onClick = onSnackbarDismiss) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(stringResource(message.message))
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name_title), color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF03A9F4)
                ),
                actions = {
                    TextButton(
                        onClick = onRefreshClick,
                        enabled = !state.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.action_refresh),
                            color = if (state.isLoading) Color.LightGray else Color.White
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) {
                    items(10) {
                        PostListItemSkeleton()
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )
                    }
                }
                return@Box
            }

            if (state.errorMessage != null) {
                Text(
                    text = stringResource(state.errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                return@Box
            }

            if (state.posts.isEmpty()) {
                Text(
                    text = stringResource(R.string.info_no_posts),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                return@Box
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.posts, key = { it.id }) { post ->
                    SwipeToDeletePostItem(
                        post = post,
                        onClick = { onPostClick(post.id) },
                        onDelete = { onDeletePost(post.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeletePostItem(
    post: PostUiModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    var dismissState by remember {
        mutableStateOf(SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold))
    }
    SwipeToDismissBox(
        state = dismissState,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                dismissState = SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold)
                onDelete()
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFCC4545))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = Color.White
                )
            }
        }
    ) {
        PostListItem(post = post, onClick = onClick)
    }
}

@Composable
private fun PostListItemSkeleton(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .background(Color.LightGray.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .background(Color.LightGray.copy(alpha = alpha))
            )
        }
    }
}

@Composable
internal fun PostListItem(
    post: PostUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}
