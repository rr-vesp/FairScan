package org.mydomain.myscan

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageRepositoryTest {

    @get:Rule
    var folder: TemporaryFolder = TemporaryFolder()

    private var _filesDir: File? = null

    fun getFilesDir(): File {
        if (_filesDir == null) {
            _filesDir = folder.newFolder("files_dir")
        }
        return _filesDir!!
    }

    fun repo(): ImageRepository {
        return ImageRepository(getFilesDir())
    }

    @Test
    fun add_image() {
        val repo = repo()
        assertThat(repo.imageIds()).isEmpty()
        val bytes = byteArrayOf(101, 102, 103)
        repo.add(bytes)
        assertThat(repo.imageIds()).hasSize(1)
        assertThat(repo.getContent(repo.imageIds()[0])).isEqualTo(bytes)
    }

    @Test
    fun `should find existing files at initialization`() {
        val bytes = byteArrayOf(101, 102, 103)
        val repo1 = repo()
        assertThat(repo1.imageIds()).isEmpty()
        repo1.add(bytes)
        val repo2 = repo()
        assertThat(repo2.imageIds()).hasSize(1)
        assertThat(repo2.getContent(repo2.imageIds()[0])).isEqualTo(bytes)
    }

    @Test
    fun `should throw on invalid id`() {
        val repo = repo()
        assertThat(repo.imageIds()).isEmpty()
        Assertions.assertThatThrownBy { repo.getContent("x") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}