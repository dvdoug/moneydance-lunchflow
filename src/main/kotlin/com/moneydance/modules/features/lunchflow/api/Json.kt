package com.moneydance.modules.features.lunchflow.api

internal sealed class JsonVal {
    object Null : JsonVal()
    data class Bool(val value: Boolean) : JsonVal()
    data class Num(val value: Double) : JsonVal()
    data class Str(val value: String) : JsonVal()
    data class Arr(val value: List<JsonVal>) : JsonVal()
    data class Obj(val value: Map<String, JsonVal>) : JsonVal()

    fun obj(): Map<String, JsonVal> = (this as? Obj)?.value ?: emptyMap()
    fun arr(): List<JsonVal> = (this as? Arr)?.value ?: emptyList()
    fun str(): String? = (this as? Str)?.value
    fun long(): Long? = when (this) {
        is Num -> value.toLong()
        is Str -> value.toLongOrNull()
        else -> null
    }
    fun double(): Double? = when (this) {
        is Num -> value
        is Str -> value.toDoubleOrNull()
        else -> null
    }
    fun bool(): Boolean? = when (this) {
        is Bool -> value
        else -> null
    }
}

internal fun Map<String, JsonVal>.str(key: String): String? = this[key]?.str()
internal fun Map<String, JsonVal>.long(key: String): Long? = this[key]?.long()
internal fun Map<String, JsonVal>.double(key: String): Double? = this[key]?.double()
internal fun Map<String, JsonVal>.bool(key: String): Boolean? = this[key]?.bool()

internal fun JsonVal.requireObj(what: String): Map<String, JsonVal> =
    (this as? JsonVal.Obj)?.value ?: throw LunchFlowException.Parse("expected $what object")

internal fun Map<String, JsonVal>.requireArr(key: String): List<JsonVal> {
    val v = this[key] ?: throw LunchFlowException.Parse("missing $key")
    return (v as? JsonVal.Arr)?.value ?: throw LunchFlowException.Parse("$key is not an array")
}

internal fun parseJson(text: String): JsonVal = JsonReader(text).parseValue()

private class JsonReader(private val s: String) {
    private var i = 0

    fun parseValue(): JsonVal {
        skipWs()
        if (i >= s.length) throw LunchFlowException.Parse("Empty JSON")
        return when (s[i]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonVal.Str(parseString())
            't' -> { expect("true"); JsonVal.Bool(true) }
            'f' -> { expect("false"); JsonVal.Bool(false) }
            'n' -> { expect("null"); JsonVal.Null }
            '-', in '0'..'9' -> parseNumber()
            else -> throw LunchFlowException.Parse("Unexpected '${s[i]}' at $i")
        }
    }

    private fun parseObject(): JsonVal.Obj {
        expect("{")
        skipWs()
        val map = linkedMapOf<String, JsonVal>()
        if (peek() == '}') {
            i++
            return JsonVal.Obj(map)
        }
        while (true) {
            skipWs()
            val key = parseString()
            skipWs()
            expect(":")
            map[key] = parseValue()
            skipWs()
            when (peek()) {
                ',' -> i++
                '}' -> { i++; return JsonVal.Obj(map) }
                else -> throw LunchFlowException.Parse("Expected , or } in object at $i")
            }
        }
    }

    private fun parseArray(): JsonVal.Arr {
        expect("[")
        skipWs()
        val list = mutableListOf<JsonVal>()
        if (peek() == ']') {
            i++
            return JsonVal.Arr(list)
        }
        while (true) {
            list.add(parseValue())
            skipWs()
            when (peek()) {
                ',' -> i++
                ']' -> { i++; return JsonVal.Arr(list) }
                else -> throw LunchFlowException.Parse("Expected , or ] in array at $i")
            }
        }
    }

    private fun parseString(): String {
        expect("\"")
        val out = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> {
                    if (i >= s.length) throw LunchFlowException.Parse("Unterminated escape")
                    when (val e = s[i++]) {
                        '"', '\\', '/' -> out.append(e)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000c')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            if (i + 4 > s.length) throw LunchFlowException.Parse("Bad unicode escape")
                            val hex = s.substring(i, i + 4)
                            out.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> throw LunchFlowException.Parse("Bad escape \\$e")
                    }
                }
                else -> out.append(c)
            }
        }
        throw LunchFlowException.Parse("Unterminated string")
    }

    private fun parseNumber(): JsonVal.Num {
        val start = i
        if (peek() == '-') i++
        while (peek() in '0'..'9') i++
        if (peek() == '.') {
            i++
            while (peek() in '0'..'9') i++
        }
        if (peek() == 'e' || peek() == 'E') {
            i++
            if (peek() == '+' || peek() == '-') i++
            while (peek() in '0'..'9') i++
        }
        val n = s.substring(start, i).toDoubleOrNull()
            ?: throw LunchFlowException.Parse("Bad number at $start")
        return JsonVal.Num(n)
    }

    private fun expect(lit: String) {
        skipWs()
        if (!s.startsWith(lit, i)) throw LunchFlowException.Parse("Expected '$lit' at $i")
        i += lit.length
    }

    private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

    private fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }
}
