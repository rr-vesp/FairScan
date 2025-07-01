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

class GeometryTest {

    @Test
    fun line() {
        assertThat(Line(Point(0, 0), Point(10, 0)).norm()).isEqualTo(10.0)
        assertThat(Line(Point(1, 2), Point(4, 6)).norm()).isEqualTo(5.0)
    }
}
