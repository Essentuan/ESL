package net.essentuan.esl.model.factories

import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.model.Descriptor
import net.essentuan.esl.model.Factory
import net.essentuan.esl.model.Model

class DefaultFactory<T: Model<JSON>, JSON: AnyJson>(
    val descriptor: Descriptor<T, JSON>,
    cls: Class<T>
): Factory<T, JSON> {
    private val constructor = cls.getConstructor()

    init {
        constructor.isAccessible = true
    }

    override fun invoke(json: JSON, flags: Set<Any>): T {
        val instance = constructor.newInstance()
        descriptor.load(instance, json, flags)

        return instance
    }
}