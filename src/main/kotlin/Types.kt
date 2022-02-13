@file:Suppress("UNUSED")

typealias UByte16 = UShort
typealias OpCode = UByte16
typealias Address = UByte16

/**
 * Shortcut for parsing 16bit unsigned hexadecimal number
 * @return [UByte16]
 */
inline fun String.toUByte16(): UByte16 = this.trim().toUShort(16)
inline fun String.toOpCode(): OpCode = this.toUByte16()
inline fun String.toAddress(): Address = this.toUByte16()

inline fun Int.toOpCode(): OpCode = this.toUShort()
inline fun UInt.toOpCode(): OpCode = this.toUShort()
inline fun Int.toAddress(): Address = this.toUShort()
inline fun UInt.toAddress(): Address = this.toUShort()

operator fun Address.plus(other: Int): Address = (this + other.toUShort()).toAddress()

typealias HtmlId = String

inline fun Address.toHtmlId(): HtmlId = "bc-$this"