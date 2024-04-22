// !LANGUAGE: +AssigningArraysToVarargsInNamedFormInAnnotations, +ProhibitAssigningSingleElementsToVarargsInNamedForm -AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun foo(vararg s: Int) {}

open class Cls(vararg p: Long)

fun test(i: IntArray) {
    foo(s = <!ARGUMENT_TYPE_MISMATCH!>1<!>)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>i<!>)
    foo(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>i<!>)
    foo(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>intArrayOf(1)<!>)
    foo(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>intArrayOf(1)<!>)
    foo(1)

    Cls(p = <!ARGUMENT_TYPE_MISMATCH!>1<!>)

    class Sub : Cls(p = <!ARGUMENT_TYPE_MISMATCH!>1<!>)

    val c = object : Cls(p = <!ARGUMENT_TYPE_MISMATCH!>1<!>) {}

    foo(s = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>intArrayOf(elements = <!ARGUMENT_TYPE_MISMATCH!>1<!>)<!>)
}


fun anyFoo(vararg a: Any) {}

fun testAny() {
    anyFoo(a = <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    anyFoo(a = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf("")<!>)
    anyFoo(a = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arrayOf("")<!>)
}

fun <T> genFoo(vararg t: T) {}

fun testGen() {
    genFoo<Int>(t = <!ARGUMENT_TYPE_MISMATCH!>1<!>)
    genFoo<Int?>(t = <!NULL_FOR_NONNULL_TYPE!>null<!>)
    genFoo<Array<Int>>(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf()<!>)
    genFoo<Array<Int>>(t = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arrayOf(arrayOf())<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>genFoo<!>(t = <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    genFoo(t = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>arrayOf("")<!>)
    genFoo(t = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arrayOf("")<!>)
}

fun manyFoo(vararg v: Int) {}
fun manyFoo(vararg s: String) {}

fun testMany(a: Any) {
    manyFoo(v = <!ARGUMENT_TYPE_MISMATCH!>1<!>)
    manyFoo(s = <!ARGUMENT_TYPE_MISMATCH!>""<!>)

    <!NONE_APPLICABLE!>manyFoo<!>(a)
    manyFoo(v = <!ARGUMENT_TYPE_MISMATCH!>a<!>)
    manyFoo(s = <!ARGUMENT_TYPE_MISMATCH!>a<!>)
    manyFoo(v = <!ARGUMENT_TYPE_MISMATCH!>a as Int<!>)
    manyFoo(s = <!ARGUMENT_TYPE_MISMATCH!>a <!CAST_NEVER_SUCCEEDS!>as<!> String<!>)
}
