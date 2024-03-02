import dev.icerock.moko.resources.ImageResource
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import kotlinx.browser.window

actual suspend fun createImageFromResource(
    resource: ImageResource,
    imageLoader: ImageLoader
): Image? = imageLoader.load(resource.fileUrl)

actual fun setLocalStorageItem(key: String, value: String) {
    window.localStorage.setItem(key, value)
}

actual fun getLocalStorageItem(key: String): String? = window.localStorage.getItem(key)