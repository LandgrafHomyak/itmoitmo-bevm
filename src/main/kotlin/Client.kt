import kotlinx.html.dom.append
import kotlinx.browser.document
import kotlinx.html.*
import org.w3c.dom.HTMLElement

fun main() {}

lateinit var inputElement: HTMLElement

@JsExport
@JsName("input")
val inputExport: HTMLElement
    get() = inputElement

lateinit var outputElement: HTMLElement

@JsExport
@JsName("output")
val outputExport: HTMLElement
    get() = outputElement

@JsExport
@JsName("decompilerSetHooks")
fun decompilerSetHooks(input: HTMLElement, output: HTMLElement) {
    inputElement = input
    outputElement = output
    println(input)
    println(output)
}

@JsExport
@JsName("decompile")
fun decompile() {
}

@JsExport
@JsName("decompileString")
fun decompile(s: String) {
}