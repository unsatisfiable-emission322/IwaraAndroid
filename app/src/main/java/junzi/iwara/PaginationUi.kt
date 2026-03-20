package junzi.iwara

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginationBar(
    currentPage: Int,
    totalCount: Int,
    pageSize: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageSize <= 0) return
    val totalPages = ((totalCount + pageSize - 1) / pageSize).coerceAtLeast(1)
    if (totalPages <= 1) return

    val visiblePages = remember(currentPage, totalPages) {
        buildList {
            add(0)
            for (page in max(0, currentPage - 2)..min(totalPages - 1, currentPage + 2)) {
                add(page)
            }
            add(totalPages - 1)
        }.distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.label_page_indicator, currentPage + 1, totalPages),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = { if (currentPage > 0) onPageSelected(currentPage - 1) },
                enabled = currentPage > 0,
                label = { Text(stringResource(R.string.action_previous_page)) },
            )
            var previousPage: Int? = null
            visiblePages.forEach { page ->
                if (previousPage != null && page - previousPage!! > 1) {
                    Text(
                        text = "?",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilterChip(
                    selected = page == currentPage,
                    onClick = { onPageSelected(page) },
                    label = { Text((page + 1).toString()) },
                )
                previousPage = page
            }
            AssistChip(
                onClick = { if (currentPage + 1 < totalPages) onPageSelected(currentPage + 1) },
                enabled = currentPage + 1 < totalPages,
                label = { Text(stringResource(R.string.action_next_page)) },
            )
        }
    }
}
