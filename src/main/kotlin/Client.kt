@file:Suppress("UNUSED")

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

fun main() {}

object Hooks {
    lateinit var addressHtmlElement: HTMLInputElement
    lateinit var bytecodeHtmlElement: HTMLElement
    lateinit var outputHtmlElement: HTMLElement
    lateinit var decompiledHtmlElement: HTMLElement
}


@JsExport
@JsName("address")
val addressExport: HTMLInputElement
    get() = Hooks.addressHtmlElement

@JsExport
@JsName("bytecode")
val bytecodeExport: HTMLElement
    get() = Hooks.bytecodeHtmlElement

@JsExport
@JsName("output")
val outputExport: HTMLElement
    get() = Hooks.outputHtmlElement

@JsExport
@JsName("decompiled")
val decompiledExport: HTMLElement
    get() = Hooks.decompiledHtmlElement

@JsExport
@JsName("decompilerSetHooks")
fun decompilerSetHooks(
    addressHtmlElement: HTMLInputElement?,
    bytecodeHtmlElement: HTMLElement?,
    errorsHtmlElement: HTMLElement?,
    decompiledHtmlElement: HTMLElement?,
) {
    Hooks.addressHtmlElement = addressHtmlElement!!
    Hooks.bytecodeHtmlElement = bytecodeHtmlElement!!
    Hooks.outputHtmlElement = errorsHtmlElement!!
    Hooks.decompiledHtmlElement = decompiledHtmlElement!!
    println(
        "Inserted hooks:" +
                "\n  addressHtmlElement=$addressHtmlElement" +
                "\n  bytecodeHtmlElement=$bytecodeHtmlElement" +
                "\n  errorsHtmlElement=$errorsHtmlElement" +
                "\n  decompiledHtmlElement=$decompiledHtmlElement"
    )
}

@JsExport
@JsName("decompile")
fun decompile() {
    DecompilerPage(
        addressHtmlElement = Hooks.addressHtmlElement,
        bytecodeHtmlElement = Hooks.bytecodeHtmlElement,
        errorsHtmlElement = Hooks.outputHtmlElement,
        decompiledHtmlElement = Hooks.decompiledHtmlElement,
        window = window,
    )?.decompileMap { DecompilerPage.DecompiledRow("abc", GotoIcon.CONDITIONAL, Argument.POINTER_INC(123), "123") }
}

@JsExport
@JsName("decompilePreset")
fun decompile(firstAddress: String?, rawBytecode: String?) {
    DecompilerPage(
        addressHtmlElement = Hooks.addressHtmlElement,
        bytecodeHtmlElement = Hooks.bytecodeHtmlElement,
        errorsHtmlElement = Hooks.outputHtmlElement,
        decompiledHtmlElement = Hooks.decompiledHtmlElement,
        window = window,
        firstAddress = firstAddress ?: "",
        bytecode = rawBytecode ?: ""
    )?.let(::Decompiler)
}