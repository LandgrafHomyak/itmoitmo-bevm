@file:Suppress("UNUSED")

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

fun main() {}

object Hooks {
    lateinit var addressHtmlElement: HTMLInputElement
    lateinit var bytecodeHtmlElement: HTMLElement
    lateinit var errorsHtmlElement: HTMLElement
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
@JsName("errors")
val errorsExport: HTMLElement
    get() = Hooks.errorsHtmlElement

@JsExport
@JsName("decompiled")
val decompiledExport: HTMLElement
    get() = Hooks.decompiledHtmlElement

@JsExport
@JsName("decompilerSetHooks")
fun decompilerSetHooks(
    addressHtmlElement: HTMLInputElement,
    bytecodeHtmlElement: HTMLElement,
    errorsHtmlElement: HTMLElement,
    decompiledHtmlElement: HTMLElement,
) {
    Hooks.addressHtmlElement = addressHtmlElement
    Hooks.bytecodeHtmlElement = bytecodeHtmlElement
    Hooks.errorsHtmlElement = errorsHtmlElement
    Hooks.decompiledHtmlElement = decompiledHtmlElement
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
        errorsHtmlElement = Hooks.errorsHtmlElement,
        decompiledHtmlElement = Hooks.decompiledHtmlElement,
    )
}

@JsExport
@JsName("decompileString")
fun decompile(firstAddress: String, rawBytecode: String) {
    DecompilerPage(
        addressHtmlElement = Hooks.addressHtmlElement,
        bytecodeHtmlElement = Hooks.bytecodeHtmlElement,
        errorsHtmlElement = Hooks.errorsHtmlElement,
        decompiledHtmlElement = Hooks.decompiledHtmlElement,
        firstAddress = firstAddress,
        bytecode = rawBytecode
    )
}