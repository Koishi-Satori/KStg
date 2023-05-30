import kotlin.coroutines.Continuation
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Test {
    suspend fun st() {
        suspendCoroutine<Unit> {
            println(6)
        }
        suspendCoroutine<Unit> {
            println(7)
        }
        suspendCoroutine<Unit> {
            println(8)
        }
    }
}