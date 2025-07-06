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
package org.mydomain.myscan

sealed class Screen {
    object Camera : Screen()
    data class Document(val initialPage: Int = 0) : Screen()
    object About : Screen()
}

data class Navigation(
    val toCameraScreen: () -> Unit,
    val toDocumentScreen: () -> Unit,
    val toAboutScreen: () -> Unit,
    val back: () -> Unit,
)
