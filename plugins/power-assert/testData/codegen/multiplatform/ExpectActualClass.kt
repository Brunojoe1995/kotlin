// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiPlatformProjects
// FUNCTION: Asserter.assert

// MODULE: lib
// TARGET_PLATFORM: Common

expect class Asserter constructor() {
    fun assert(condition: Boolean, msg: Any? = null)
}

fun test1() {
    val asserter = Asserter()
    val hello = "Hello"
    asserter.assert(hello == "World")
}

fun test2() {
    with(Asserter()) {
        val hello = "Hello"
        assert(hello == "World")
    }
}

// MODULE: main()()(lib)

actual class Asserter {
    actual fun assert(condition: Boolean, msg: Any?) {
        if (!condition) throw AssertionError(msg.toString())
    }

    override fun toString(): String = "Asserter"
}

fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
)
