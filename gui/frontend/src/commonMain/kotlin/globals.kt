import dev.icerock.moko.resources.ImageResource
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader

expect fun setLocalStorageItem(key: String, value: String)

expect fun getLocalStorageItem(key: String): String?

expect suspend fun createImageFromResource(resource: ImageResource, imageLoader: ImageLoader): Image?