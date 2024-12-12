// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

// FILE: KotlinClass.kt
import kotlin.concurrent.AtomicInt

@OptIn(ExperimentalStdlibApi::class)
open class KotlinClass {
    open fun foo(a: AtomicInt) { }
    open val a: AtomicInt = AtomicInt(0)
}

// FILE: JavaClassWithFakeOverride.java

public class JavaClassWithFakeOverride extends KotlinClass {}

// FILE: test.kt
import JavaClassWithFakeOverride
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.AtomicInt

class KotlinChildWithFakeOverride: JavaClassWithFakeOverride()

@OptIn(ExperimentalStdlibApi::class)
class KotlinChildWithExplicitOverride: JavaClassWithFakeOverride() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(i: AtomicInteger) { }
    override val a: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>AtomicInteger<!>
        get() = AtomicInteger(1)
}

@OptIn(ExperimentalStdlibApi::class)
fun usage(a:KotlinChildWithFakeOverride) {
    a.foo(AtomicInt(0))
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>AtomicInteger(0)<!>)
    val t1: AtomicInt = a.a
    val t2: AtomicInteger = <!INITIALIZER_TYPE_MISMATCH!>a.a<!>
}
