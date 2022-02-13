@file:Suppress("UNUSED") @file:OptIn(ExperimentalUnsignedTypes::class)

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.span
import kotlinx.html.js.tr
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Window
import org.w3c.dom.url.URLSearchParams


/**
 * Shortcut for preparing array to parsing
 * @return [List]&lt;[String]&gt;
 */
private inline fun Iterable<String>.prepareUByte16(): List<String> = this.filter { s -> s.isNotBlank() }

/**
 * Shortcut for splitting string and preparing to parsing
 * @return [List]&lt;[String]&gt;
 */
private inline fun String.prepareUByte16(): List<String> = this.split('\n').prepareUByte16()

/**
 * Proxy class provides access to frontend
 * @param bytecode parsed bytecode
 * @param output proxy to errors output
 * @param decompiledHtmlElement HTML element access to which provides this class
 * @property bytecode bytecode to decompile
 */
class DecompilerPage private constructor(
    val bytecode: ByteCodeMapping, val output: Output, private val decompiledHtmlElement: HTMLElement
) {
    companion object {
        /**
         * Entry point to [DecompilerPage] with checking and coloring input fields
         * @param addressHtmlElement input field with address of first operation
         * @param bytecodeHtmlElement input field with encoded operations
         * @param errorsHtmlElement output node for displaying errors
         * @param decompiledHtmlElement output &lt;tbody&gt; or &lt;table&gt; tag for showing decompiled code
         * @param window browser window
         * @param firstAddress raw predefined value in [addressHtmlElement]
         * @param bytecode raw predefined value in [bytecodeHtmlElement]
         * @return [DecompilerPage] if parsing was successful otherwise null
         */
        operator fun invoke(
            addressHtmlElement: HTMLInputElement,
            bytecodeHtmlElement: HTMLElement,
            errorsHtmlElement: HTMLElement,
            decompiledHtmlElement: HTMLElement,
            window: Window,
            firstAddress: String? = null,
            bytecode: String? = null,
        ): DecompilerPage? {
            val output = Output(errorsHtmlElement)

            val firstAddressS: String = firstAddress ?: addressHtmlElement.value
            val bytecodeR: String = bytecode ?: bytecodeHtmlElement.innerText
            window.history.replaceState(null, "", "?" + URLSearchParams().apply {
                set("firstAddress", firstAddressS)
                set("bytecode", bytecodeR)
            }.toString())


            addressHtmlElement.value = firstAddressS
            addressHtmlElement.style.background = "white"
            val firstAddressI = try {
                firstAddressS.toAddress()
            } catch (_: NumberFormatException) {
                output.error(addressHtmlElement.id, "address", Unit, "Invalid format of address")
                addressHtmlElement.style.background = "coral"
                return null
            }

            bytecodeHtmlElement.innerHTML = ""
            val bytecodeP = bytecodeR.prepareUByte16().also { l ->
                if (l.isEmpty()) {
                    output.error("No operations passed")
                    return@invoke null
                }
            }.mapIndexed { i, s ->
                lateinit var element: HTMLElement
                bytecodeHtmlElement.append {
                    element = span {
                        id = (firstAddressI + i).toHtmlId()
                        +s
                    }
                    br {}
                }
                try {
                    return@mapIndexed element.id to s.toUByte16()
                } catch (_: NumberFormatException) {
                    element.style.color = "red"
                    return@mapIndexed element.id to null
                }
            }.mapIndexed { i, (id, code) ->
                if (i > Address.MAX_VALUE.toInt()) {
                    output.error("Too many operations (max ${Address.MAX_VALUE}")
                    return@mapIndexed null
                }
                if (code == null) {
                    output.error(id, (firstAddressI + i).toString(16), Unit, "Invalid format of operation code")
                }
                return@mapIndexed code
            }.let { r ->
                val f = r.filterNotNull()
                if (f.size != r.size) return@invoke null
                return@let f
            }.let { f ->
                return@let ByteCodeMapping(firstAddressI, f.toTypedArray())
            }
            output.ok("Parsed successful")
            return DecompilerPage(bytecodeP, output, decompiledHtmlElement)
        }
    }


    /**
     * Collection to storing operation codes with its addresses
     * @param firstAddress address where first operation is stored
     * @param codes operations' codes
     */
    class ByteCodeMapping(
        private val firstAddress: Address, private val codes: Array<OpCode>
    ) {
        /**
         * Class for iterating operations with theirs addresses
         * @property address operation's address
         * @property code operation's code
         */
        data class Row(
            val address: Address, val code: OpCode
        )


        /**
         * @param firstAddress address where first operation is stored
         * @param codes operations' codes
         */
        constructor(firstAddress: Address, codes: Collection<OpCode>) : this(firstAddress, codes.toTypedArray())

        /**
         * @param firstAddress address where first operation is stored
         * @param codes operations' codes
         */
        constructor(firstAddress: Address, vararg codes: OpCode) : this(firstAddress, codes)

        /**
         * @param firstAddress address where first operation is stored
         * @param raw string with raw operations' codes split by '\n'
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: Address, raw: String) : this(firstAddress, raw.prepareUByte16())

        /**
         * @param firstAddress address where first operation is stored
         * @param raw collection of raw operations' codes
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: Address, raw: Collection<String>) : this(firstAddress, raw.toTypedArray())

        /**
         * @param firstAddress address where first operation is stored
         * @param raw array of raw operations' codes
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: Address, raw: Array<String>) : this(
            firstAddress, raw.map { s -> s.toUByte16() }.toTypedArray()
        )

        /**
         * Iterates over all operations with theirs addresses
         * @see Row
         */
        operator fun iterator(): Iterator<Row> = iterator {
            this@ByteCodeMapping.codes.forEachIndexed { i, c ->
                yield(Row(this@ByteCodeMapping.firstAddress + i, c))
            }
        }

        /**
         * Gets operation's code by address
         * @param address operation's address
         * @return operation's code
         */
        operator fun get(address: Address): OpCode = this.codes[(address - this.firstAddress).toInt()]
        // operator fun get(address: Int): UShort = this[address.toUShort()]
    }

    /**
     * Proxy class provides access errors output
     * @param errorsHtmlElement wrapped HTML node
     */
    class Output(private val errorsHtmlElement: HTMLElement) {
        init {
            this.errorsHtmlElement.innerHTML = ""
        }

        /**
         * Message builder
         * @param color message color
         * @param where optional link to location
         * @param whereText link's text (must be not null if param [where] was passed)
         * @param lines message info split by '\n'
         */
        private fun print(color: String, where: HtmlId?, whereText: String?, vararg lines: String) {
            this.errorsHtmlElement.append {
                tr {
                    style = "color: $color"
                    td {
                        if (where != null) {
                            a {
                                href = "#$where"
                                +whereText!!
                            }
                        }
                    }
                    td {
                        +lines.joinToString("\n")
                    }
                }
            }
        }

        /**
         * Prints error message (red)
         * @param firstLine error message
         * @param lines optional additional lines
         */
        fun error(firstLine: String, vararg lines: String) = this.print("red", null, null, firstLine, *lines)

        /**
         * Prints error message (red)
         * @param where link to error location
         * @param whereText link's text
         * @param firstLine error message
         * @param lines optional additional lines
         */
        fun error(where: HtmlId, whereText: String, sentinel: Unit, firstLine: String, vararg lines: String) = this.print("red", where, whereText, firstLine, *lines)

        /**
         * Prints warning message (yellow)
         * @param firstLine warning message
         * @param lines optional additional lines
         */
        fun warning(firstLine: String, vararg lines: String) = this.print("#ffd400", null, null, firstLine, *lines)

        /**
         * Prints warning message (yellow)
         * @param where link to warning location
         * @param whereText link's text
         * @param firstLine warning message
         * @param lines optional additional lines
         */
        fun warning(where: HtmlId, whereText: String, sentinel: Unit, firstLine: String, vararg lines: String) = this.print("#ffd400", where, whereText, firstLine, *lines)

        /**
         * Prints info message (grey)
         * @param firstLine warning message
         * @param lines optional additional lines
         */
        fun info(firstLine: String, vararg lines: String) = this.print("grey", null, null, firstLine, *lines)

        /**
         * Prints info message (grey)
         * @param where link to info location
         * @param whereText link's text
         * @param firstLine info message
         * @param lines optional additional lines
         */
        fun info(where: HtmlId, whereText: String, sentinel: Unit, firstLine: String, vararg lines: String) = this.print("grey", where, whereText, firstLine, *lines)

        /**
         * Prints ok message (green)
         * @param firstLine ok message
         * @param lines optional additional lines
         */
        fun ok(firstLine: String, vararg lines: String) = this.print("green", null, null, firstLine, *lines)

        /**
         * Prints ok message (green)
         * @param where link to ok location
         * @param whereText link's text
         * @param firstLine ok message
         * @param lines optional additional lines
         */
        fun ok(where: HtmlId, whereText: String, sentinel: Unit, firstLine: String, vararg lines: String) = this.print("green", where, whereText, firstLine, *lines)
    }

    class DecompiledRow()

    private class Chunk()
}

enum class GotoIcon(val path: String) {
    CONDITIONAL("goto/conditional.svg"),
    STRONG("goto/strong.svg")
}

enum class ArgumentIcon(val path: String) {
    ABSOLUTE("arguments/absolute.svg"),
    OFFSET("arguments/offset.svg"),
    POINTER("arguments/pointer.svg"),
    POINTER_INC("arguments/pointer-inc.svg"),
    POINTER_DEC("arguments/pointer-dec.svg"),
    STACK("arguments/stack.svg"),
    CONST("arguments/const.svg"),
}
