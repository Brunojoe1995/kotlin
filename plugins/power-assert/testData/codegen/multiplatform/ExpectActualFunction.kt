// LANGUAGE: +MultiPlatformProjects
// FUNCTION: assert

// MODULE: lib
// TARGET_PLATFORM: Common

expect fun assert(condition: Boolean, msg: Any? = null)

fun test1() {
    val hello = "Hello"
    assert(hello == "World")
}

// MODULE: main()()(lib)

actual fun assert(condition: Boolean, msg: Any?) {
    if (!condition) throw AssertionError(msg.toString())
}

fun box(): String = runAll(
    "test1" to { test1() },
)
