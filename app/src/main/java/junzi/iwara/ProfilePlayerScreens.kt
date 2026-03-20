package junzi.iwara

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import junzi.iwara.app.IwaraAppController
import junzi.iwara.model.AppUiState
import junzi.iwara.model.CommentItem
import junzi.iwara.model.ProfileDetail
import junzi.iwara.model.VideoDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: AppUiState,
    controller: IwaraAppController,
) {
    BackHandler { controller.loadFeed(state.feed.sort, state.feed.selectedTag) }
    val detail = state.profile.detail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.user?.username ?: state.profile.username.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { controller.loadFeed(state.feed.sort, state.feed.selectedTag) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = controller::openSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search))
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.profile.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.profile.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.profile.error, color = MaterialTheme.colorScheme.error)
                }
            }

            detail != null -> {
                ProfileBody(
                    detail = detail,
                    commentSubmitting = state.profile.commentSubmitting,
                    commentError = state.profile.commentError,
                    onOpenProfile = controller::openProfile,
                    onOpenVideo = controller::openVideo,
                    onSubmitComment = controller::submitProfileComment,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun ProfileBody(
    detail: ProfileDetail,
    commentSubmitting: Boolean,
    commentError: String?,
    onOpenProfile: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onSubmitComment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProfileHeader(detail)
        }
        item {
            UserStrip(stringResource(R.string.section_followers), detail.followers, onOpenProfile)
        }
        item {
            UserStrip(stringResource(R.string.section_following), detail.following, onOpenProfile)
        }
        item { SectionTitle(stringResource(R.string.section_videos)) }
        items(detail.videos, key = { it.id }) { video ->
            VideoRow(
                video = video,
                onOpen = { onOpenVideo(video.id) },
                onOpenProfile = { onOpenProfile(video.authorUsername) },
            )
        }
        item { SectionTitle(stringResource(R.string.section_images)) }
        items(detail.images, key = { it.id }) { image ->
            ImageRow(image)
        }
        item { SectionTitle(stringResource(R.string.section_profile_comments)) }
        items(detail.comments, key = { it.id }) { comment ->
            CommentRow(comment = comment, onOpenProfile = { onOpenProfile(comment.authorUsername) })
        }
        item {
            CommentComposer(
                label = stringResource(R.string.label_leave_profile_comment),
                submitting = commentSubmitting,
                error = commentError,
                onSubmit = onSubmitComment,
            )
        }
    }
}

@Composable
private fun ProfileHeader(detail: ProfileDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (detail.headerUrl != null) {
            junzi.iwara.ui.AsyncRemoteImage(
                url = detail.headerUrl,
                contentDescription = stringResource(R.string.label_profile_header),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(detail.user.avatarUrl, detail.user.name)
            Column {
                Text(detail.user.name, style = MaterialTheme.typography.headlineSmall)
                Text("@${detail.user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!detail.body.isNullOrBlank()) {
            Text(text = detail.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun UserStrip(
    title: String,
    users: List<junzi.iwara.model.IwaraUser>,
    onOpenProfile: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (users.isEmpty()) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.label_none)) })
            }
            users.forEach { user ->
                AssistChip(
                    onClick = { onOpenProfile(user.username) },
                    label = { Text(user.username) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: AppUiState,
    controller: IwaraAppController,
) {
    BackHandler(onBack = controller::closePlayer)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.player.detail?.title ?: stringResource(R.string.label_loading)) },
                navigationIcon = {
                    IconButton(onClick = controller::closePlayer) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.player.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.player.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.player.error, color = MaterialTheme.colorScheme.error)
                }
            }

            state.player.detail != null -> {
                PlayerDetailBody(
                    detail = state.player.detail,
                    comments = state.player.comments,
                    commentsLoading = state.player.commentsLoading,
                    commentSubmitting = state.player.commentSubmitting,
                    commentError = state.player.commentError,
                    onVariantSelected = controller::selectVariant,
                    onOpenProfile = controller::openProfile,
                    onOpenTag = controller::openTag,
                    onSubmitComment = controller::submitVideoComment,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun PlayerDetailBody(
    detail: VideoDetail,
    comments: List<CommentItem>,
    commentsLoading: Boolean,
    commentSubmitting: Boolean,
    commentError: String?,
    onVariantSelected: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    onSubmitComment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedVariant = remember(detail.selectedVariantName, detail.variants) {
        detail.variants.firstOrNull { it.name == detail.selectedVariantName } ?: detail.variants.firstOrNull()
    }
    val context = LocalContext.current
    val player = remember(selectedVariant?.viewUrl) {
        ExoPlayer.Builder(context).build().apply {
            selectedVariant?.viewUrl?.let { setMediaItem(MediaItem.fromUri(it)) }
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
            ) {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = true
                            this.player = player
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "@${detail.authorUsername} · ${stringResource(R.string.label_views_likes, detail.views, detail.likes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onOpenProfile(detail.authorUsername) },
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    detail.variants.filterNot { it.name.equals("preview", ignoreCase = true) }.forEach { variant ->
                        val variantLabel = when {
                            variant.name.equals("Source", ignoreCase = true) -> stringResource(R.string.quality_source)
                            else -> "${variant.name}p"
                        }
                        FilterChip(
                            selected = variant.name == detail.selectedVariantName,
                            onClick = { onVariantSelected(variant.name) },
                            label = { Text(variantLabel) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    detail.tags.forEach { tag ->
                        AssistChip(
                            onClick = { onOpenTag(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
                if (detail.description.isNotBlank()) {
                    Text(text = detail.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.section_comments)) }
        if (commentsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        items(comments, key = { it.id }) { comment ->
            CommentRow(comment = comment, onOpenProfile = { onOpenProfile(comment.authorUsername) })
        }
        item {
            CommentComposer(
                label = stringResource(R.string.label_add_comment),
                submitting = commentSubmitting,
                error = commentError,
                onSubmit = onSubmitComment,
            )
        }
    }
}

