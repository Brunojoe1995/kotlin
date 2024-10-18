// WITH_STDLIB
// ISSUE: KT-71751
// IGNORE_BACKEND_K2: ANY

fun launch(x: () -> Unit) {
    x()
}

fun box(): String {
    var result: String = "fail"
    val job = launch {
        "test".let {
            null
        } ?: run { // this is not called if it is the last thing in the block
            result = "OK"
        }
    }

    return result
}
