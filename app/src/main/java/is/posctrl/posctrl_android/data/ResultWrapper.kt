package `is`.posctrl.posctrl_android.data

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class ResultWrapper<out T> {

    data class Success<out T>(val data: T) : ResultWrapper<T>()
    data class Error(
            val message: String? = null,
            val code: Int = ErrorCode.DEFAULT.code
    ) : ResultWrapper<Nothing>()

    object Loading : ResultWrapper<Nothing>()

    object None : ResultWrapper<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[message=$message]"
            Loading -> "Loading"
            None -> "None"
        }
    }
}

enum class ErrorCode(val code: Int) {
    DEFAULT(-1),
    NO_DATA_CONNECTION(1)
}
