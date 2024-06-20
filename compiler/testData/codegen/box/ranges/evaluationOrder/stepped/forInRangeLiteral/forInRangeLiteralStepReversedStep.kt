// WITH_STDLIB

import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in (logged("start;", 1)..logged("end;", 10) step logged("step2;", 2)).reversed() step logged("step3;", 3)) {
        sum = sum * 10 + i
    }

    assertEquals(963, sum)

    assertEquals("start;end;step2;step3;", log.toString())

    return "OK"
}