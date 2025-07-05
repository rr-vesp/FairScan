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

        assertThat(f.exists()).isFalse
        assertThat(f1.exists()).isFalse
        assertThat(getAvailableFilename(f)).isEqualTo(f)

        f.apply { writeText("dummy") }
        assertThat(f.exists()).isTrue
        assertThat(getAvailableFilename(f)).isEqualTo(f1)

        f1.apply { writeText("dummy") }
        assertThat(f1.exists()).isTrue
        assertThat(getAvailableFilename(f)).isEqualTo(f2)
    }

}
