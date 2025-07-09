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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileUtilsTest {

    @Test
    fun getAvailableName() {
        val dir = createTempDirectory().toFile()
        val f = File(dir, "f.pdf")
        val f1 = File(dir, "f_1.pdf")
        val f2 = File(dir, "f_2.pdf")

        assertThat(f).doesNotExist()
        assertThat(f1).doesNotExist()
        assertThat(getAvailableFilename(f)).isEqualTo(f)

        f.apply { writeText("dummy") }
        assertThat(f).exists()
        assertThat(getAvailableFilename(f)).isEqualTo(f1)

        f1.apply { writeText("dummy") }
        assertThat(f1).exists()
        assertThat(getAvailableFilename(f)).isEqualTo(f2)
    }

    @Test
    fun cleanUpOldFiles() {
        val dir = createTempDirectory().toFile()
        val subDir = File(dir,"subDir")
        cleanUpOldFiles(subDir, 10)
        assertThat(subDir).doesNotExist()

        subDir.mkdirs()
        assertThat(subDir).exists()
        val file1 = File(subDir, "file1")
        file1.createNewFile()
        val file2 = File(subDir, "file2")
        file2.createNewFile()

        val now = System.currentTimeMillis()
        file1.setLastModified(now - 10_000)
        file2.setLastModified(now - 11_000)
        cleanUpOldFiles(subDir, 10_500)
        assertThat(file1).exists()
        assertThat(file2).doesNotExist()
    }
}
