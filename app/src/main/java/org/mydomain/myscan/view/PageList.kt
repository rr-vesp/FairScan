/*
 * Copyright 2025 Pierre-Yves Nicolas
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.mydomain.myscan.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun CommonPageList(
    pageIds: List<String>,
    imageLoader: (String) -> Bitmap?,
    onPageClick: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    currentPageIndex: Int? = null,
) {
    if (pageIds.isEmpty()) return
    LazyRow (
        state = listState,
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(pageIds) { index, id ->
            // TODO Use small images rather than big ones
            val image = imageLoader(id)
            if (image != null) {
                val bitmap = image.asImageBitmap()
                val isSelected = index == currentPageIndex
                val borderColor =
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val modifier =
                    if (bitmap.height > bitmap.width)
                        Modifier.height(120.dp)
                    else
                        Modifier.width(120.dp)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = modifier
                        .padding(4.dp)
                        .border(2.dp, borderColor)
                        .clickable { onPageClick(index) }
                )
            }
        }
    }
}
