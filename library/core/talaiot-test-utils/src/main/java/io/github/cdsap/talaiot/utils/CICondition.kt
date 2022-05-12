package io.github.cdsap.talaiot.utils

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec

class CICondition :  EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean = when {
        kclass.simpleName?.contains("CI") == true -> IS_OS_LINUX
        else -> true // non Linux tests always run
    }
}
