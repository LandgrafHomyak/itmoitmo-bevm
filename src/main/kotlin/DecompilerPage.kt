@file:Suppress("UNUSED")
@file:OptIn(ExperimentalUnsignedTypes::class)

import kotlinx.html.br
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.span
import kotlinx.html.style
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

/**
 * Shortcut for parsing 16bit unsigned hexadecimal number
 * @return [UShort]
 */
private inline fun String.toUByte16(): UShort = this.trim().toUShort(16)

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
    val bytecode: ByteCodeMapping,
    val output: Output,
    private val decompiledHtmlElement: HTMLElement
) {
    companion object {
        /**
         * Entry point to [DecompilerPage] with checking and coloring input fields
         * @param addressHtmlElement input field with address of first operation
         * @param bytecodeHtmlElement input field with encoded operations
         * @param errorsHtmlElement output node for displaying errors
         * @param decompiledHtmlElement output &lt;tbody&gt; or &lt;table&gt; tag for showing decompiled code
         * @param firstAddress raw predefined value in [addressHtmlElement]
         * @param bytecode raw predefined value in [bytecodeHtmlElement]
         * @return [DecompilerPage] if parsing was successful otherwise null
         */
        operator fun invoke(
            addressHtmlElement: HTMLInputElement,
            bytecodeHtmlElement: HTMLElement,
            errorsHtmlElement: HTMLElement,
            decompiledHtmlElement: HTMLElement,
            firstAddress: String? = null,
            bytecode: String? = null,
        ): DecompilerPage? {
            val output = Output(errorsHtmlElement)

            val firstAddressS: String = firstAddress ?: addressHtmlElement.value
            addressHtmlElement.value = firstAddressS
            addressHtmlElement.style.background = "white"
            val firstAddressI = try {
                firstAddressS.toUByte16()
            } catch (_: NumberFormatException) {
                output.error("Invalid format of <a href='#address'>address</a>")
                addressHtmlElement.style.background = "coral"
                return null
            }

            val bytecodeR: String = bytecode ?: bytecodeHtmlElement.innerText
            val bytecodeP = prepareBytecode(
                bytecodeR.prepareUByte16().also { l ->
                    if (l.isEmpty()) {
                        output.error("No operations passed")
                        return@invoke null
                    }
                },
                bytecodeHtmlElement
            )
                .mapIndexed { i, (row, code) ->
                    if (i > UShort.MAX_VALUE.toInt()) {
                        output.error("Too many operations (max ${UShort.MAX_VALUE}")
                        return@mapIndexed null
                    }
                    if (code == null) {
                        output.error("Invalid format of operation code (<a href='#${row.id}'>${(i.toUShort() + firstAddressI).toString(16)}</a>)")
                    }
                    return@mapIndexed code
                }.let { r ->
                    val f = r.filterNotNull()
                    if (f.size != r.size) return@invoke null
                    return@let f
                }.let { f ->
                    return@let ByteCodeMapping(firstAddressI, f.toUShortArray())
                }
            output.ok("Parsed successful")
            return DecompilerPage(bytecodeP, output, decompiledHtmlElement)
        }

        /**
         * Helper function for setting per row anchors in [bytecodeHtmlElement] and parsing operations' codes
         * @param raw raw codes to parse
         * @param bytecodeHtmlElement input field for creating anchors
         * @return iterator with created [HTMLElement] and parsed value [UShort] if successful otherwise null for each element in [raw]
         */
        private fun prepareBytecode(raw: Iterable<String>, bytecodeHtmlElement: HTMLElement): Iterable<Pair<HTMLElement, UShort?>> = object : Iterable<Pair<HTMLElement, UShort?>> {
            override operator fun iterator() = iterator {
                bytecodeHtmlElement.innerHTML = ""


                raw.forEachIndexed { i, s ->
                    lateinit var element: HTMLElement
                    bytecodeHtmlElement.append {
                        element = span {
                            id = "bc-$i"
                            +s
                        }
                        br {}
                    }
                    try {
                        yield(element to s.toUByte16())
                    } catch (_: NumberFormatException) {
                        element.style.color = "red"
                        yield(element to null)
                    }
                }
            }
        }
    }


    /**
     * Collection to storing operation codes with its addresses
     * @param firstAddress address where first operation is stored
     * @param codes operations' codes
     */
    class ByteCodeMapping(
        private val firstAddress: UShort, private val codes: UShortArray
    ) {
        /**
         * Class for iterating operations with theirs addresses
         * @property address operation's address
         * @property code operation's code
         */
        data class Row(
            val address: UShort, val code: UShort
        )

        /**
         * @param firstAddress address where first operation is stored
         * @param codes operations' codes
         */
        constructor(firstAddress: UShort, codes: Array<UShort>) : this(firstAddress, codes.toUShortArray())

        /**
         * @param firstAddress address where first operation is stored
         * @param codes operations' codes
         */
        constructor(firstAddress: UShort, codes: Collection<UShort>) : this(firstAddress, codes.toUShortArray())

        /**
         * @param firstAddress address where first operation is stored
         * @param codes operations' codes
         */
        constructor(firstAddress: UShort, vararg codes: UShort) : this(firstAddress, codes)

        /**
         * @param firstAddress address where first operation is stored
         * @param raw string with raw operations' codes split by '\n'
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: UShort, raw: String) : this(firstAddress, raw.split('\n'))

        /**
         * @param firstAddress address where first operation is stored
         * @param raw collection of raw operations' codes
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: UShort, raw: Collection<String>) : this(firstAddress, raw.toTypedArray())

        /**
         * @param firstAddress address where first operation is stored
         * @param raw array of raw operations' codes
         * @throws NumberFormatException if operation's code is not hex number
         */
        constructor(firstAddress: UShort, raw: Array<String>) : this(
            firstAddress, raw.map { s -> s.toUByte16() }.toUShortArray()
        )

        /**
         * Iterates over all operations with theirs addresses
         * @see Row
         */
        operator fun iterator(): Iterator<Row> = iterator {
            this@ByteCodeMapping.codes.forEachIndexed { i, c ->
                yield(Row((i.toUShort() + this@ByteCodeMapping.firstAddress).toUShort(), c))
            }
        }

        /**
         * Gets operation's code by address
         * @param address operation's address
         * @return operation's code
         */
        operator fun get(address: UShort): UShort = this.codes[(address - this.firstAddress).toInt()]
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
         * Prints error message (red)
         * @param message HTML-formatted error message
         */
        fun error(message: String) {
            this.errorsHtmlElement.append {
                div {
                    style = "color: red"
                }.innerHTML += message
            }
        }

        /**
         * Prints warning message (yellow)
         * @param message HTML-formatted warning message
         */
        fun warning(message: String) {
            this.errorsHtmlElement.append {
                div {
                    style = "color: yellow"
                }.innerHTML += message
            }
        }

        /**
         * Prints info message (grey)
         * @param message HTML-formatted warning message
         */
        fun info(message: String) {
            this.errorsHtmlElement.append {
                div {
                    style = "color: grey"
                }.innerHTML += message
            }
        }
        /**
         * Prints ok message (green)
         * @param message HTML-formatted warning message
         */
        fun ok(message: String) {
            this.errorsHtmlElement.append {
                div {
                    style = "color: green"
                }.innerHTML += message
            }
        }
    }
}