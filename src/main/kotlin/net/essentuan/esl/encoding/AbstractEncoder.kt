package net.essentuan.esl.encoding

import net.essentuan.esl.reflections.extensions.typeInformationOf

@Suppress("UNCHECKED_CAST")
abstract class AbstractEncoder<T: Any, OUT: Any> : Encoder<T, OUT> {
    final override val type: Class<T>
    final override val out: Class<OUT>

    constructor(type: Class<T>, out: Class<OUT>) {
        this.type = type
        this.out = out
    }

    constructor() {
        val types = this.typeInformationOf(AbstractEncoder::class)

        type = types["T"] as Class<T>
        out = types["OUT"] as Class<OUT>
    }
}