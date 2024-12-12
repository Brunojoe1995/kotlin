// DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// LANGUAGE: +ContextParameters

context(x: Int)
fun d(): Unit = js("console.log(x)")

<!WASM_EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS!>context(x: Int)
@JsFun("console.log(x)")
external fun d2(): Unit<!>

<!WASM_EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS!>context(x: Int)
external fun d3(): Unit<!>

external class E {
    <!WASM_EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS!>context(x: Int)
    fun d4(): Unit<!>
}
