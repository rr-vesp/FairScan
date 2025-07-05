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

import java.io.File

fun getAvailableFilename(desiredFile: File): File {
    var file = desiredFile
    val dir = desiredFile.parentFile
    val desiredName = desiredFile.name
    val nameWithoutExtension = desiredName.removeSuffix(".pdf")
    var counter = 1
    while (file.exists()) {
        file = File(dir, "${nameWithoutExtension}_$counter.pdf")
        counter++
    }
    return file
}
