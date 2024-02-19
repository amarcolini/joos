import kotlinx.browser.window

actual object Storage {
    actual fun getItem(key: String): String? = window.localStorage.getItem(key)

    actual fun setItem(key: String, data: String) {
        window.localStorage.setItem(key, data)
    }
}