package io.github.cdsap.talaiot.assertions

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

private fun <T : Any> containExactlyTypesOfInAnyOrder(ts: Collection<KClass<*>>): Matcher<Collection<T>> =
    object : Matcher<Collection<T>> {
        override fun test(value: Collection<T>) = MatcherResult(
            ts.size == value.size && value.all { ts.contains(it::class) },
            "Collection ${value.joinToString(", ")} should contain all of ${ts.joinToString(", ")}",
            "Collection ${value.joinToString(", ")} should not contain all of ${ts.joinToString(", ")}"
        )
    }

fun <T : Any> Collection<T>.shouldContainExactlyTypesOfInAnyOrder(ts: Collection<KClass<*>>) {
    this shouldBe containExactlyTypesOfInAnyOrder<T>(ts)
}
