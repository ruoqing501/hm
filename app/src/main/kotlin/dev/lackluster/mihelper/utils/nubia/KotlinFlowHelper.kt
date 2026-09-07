package dev.lackluster.mihelper.utils.nubia

import dev.lackluster.mihelper.hook.compat.factory.constructor
import dev.lackluster.mihelper.hook.compat.factory.method
import dev.lackluster.mihelper.hook.compat.param.PackageParam

@Suppress("FunctionName")
object KotlinFlowHelper {
    private const val STATE_FLOW = "kotlinx.coroutines.flow.StateFlow"
    private const val STATE_FLOW_KT = "kotlinx.coroutines.flow.StateFlowKt"
    const val READONLY_STATE_FLOW = "kotlinx.coroutines.flow.ReadonlyStateFlow"

    fun PackageParam.MutableStateFlow(initValue: Any?): Any? {
        return STATE_FLOW_KT.toClass().method {
            name = "MutableStateFlow"
            paramCount = 1
            modifiers { isStatic }
        }.get().call(initValue)
    }

    fun PackageParam.ReadonlyStateFlow(initValue: Any?): Any? {
        return READONLY_STATE_FLOW.toClass().constructor {
            param(STATE_FLOW)
        }.get().call(MutableStateFlow(initValue))
    }
}

