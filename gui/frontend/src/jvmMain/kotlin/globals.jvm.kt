import dev.icerock.moko.resources.ImageResource
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader

actual suspend fun createImageFromResource(resource: ImageResource, imageLoader: ImageLoader): Image? {
    return imageLoader.load(resource.filePath)
}

actual fun setLocalStorageItem(key: String, value: String) {
}

actual fun getLocalStorageItem(key: String): String? = null