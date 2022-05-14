package io.github.cdsap.talaiot.utils

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

class CICondition : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean = when {
        kclass.simpleName?.contains("E2E") == true -> false
        else -> true
    }
}
