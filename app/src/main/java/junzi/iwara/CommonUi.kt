package junzi.iwara

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import junzi.iwara.model.CommentItem
import junzi.iwara.model.ImageSummary
import junzi.iwara.model.IwaraUser
import junzi.iwara.model.VideoSummary
import junzi.iwara.ui.AsyncRemoteImage

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
fun AvatarImage(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
) {
    if (url != null) {
        AsyncRemoteImage(
            url = url,
            contentDescription = name,
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VideoRow(
    video: VideoSummary,
    onOpen: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val ratingLabel = when (video.rating.lowercase()) {
        "general" -> stringResource(R.string.rating_general)
        "ecchi" -> stringResource(R.string.rating_ecchi)
        else -> video.rating
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncRemoteImage(
            url = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier
                .size(width = 148.dp, height = 100.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${video.authorUsername}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable(onClick = onOpenProfile),
            )
            Text(
                text = stringResource(R.string.label_views_likes, video.views, video.likes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onOpen, label = { Text(ratingLabel) })
                video.durationSeconds?.let {
                    AssistChip(onClick = onOpen, label = { Text(formatDuration(it)) })
                }
            }
        }
    }
}

@Composable
fun UserRow(
    user: IwaraUser,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(user.avatarUrl, user.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.SemiBold)
            Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ImageRow(image: ImageSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncRemoteImage(
            url = image.thumbnailUrl,
            contentDescription = image.title,
            modifier = Modifier
                .size(width = 112.dp, height = 112.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(image.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("@${image.authorUsername}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.label_views_likes, image.views, image.likes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CommentRow(
    comment: CommentItem,
    onOpenProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarImage(comment.authorAvatarUrl, comment.authorName)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${comment.authorName} @${comment.authorUsername}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onOpenProfile),
            )
            Text(
                text = comment.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = comment.body)
            if (comment.numReplies > 0) {
                Text(
                    text = stringResource(R.string.label_replies, comment.numReplies),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CommentComposer(
    label: String,
    submitting: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var text by rememberSaveable(label) { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val payload = text.trim()
                    if (payload.isNotBlank()) {
                        onSubmit(payload)
                        text = ""
                    }
                },
                enabled = !submitting && text.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.action_send_comment),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (submitting) stringResource(R.string.label_posting) else stringResource(R.string.label_post))
            }
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

