package com.github.h0tk3y.probabilistik

import kotlin.coroutines.experimental.Continuation

private val coroutineImplClass by lazy { Class.forName("kotlin.coroutines.experimental.jvm.internal.CoroutineImpl") }

private val labelField by lazy { coroutineImplClass.getDeclaredField("label").apply { isAccessible = true } }
private val completionField by lazy { coroutineImplClass.getDeclaredField("completion").apply { isAccessible = true } }

private var <T> Continuation<T>.completion: Continuation<*>?
    get() = completionField.get(this) as Continuation<*>
    set(value) = completionField.set(this@completion, value)

internal var <T> Continuation<T>.stateStack: List<Map<String, *>>
    get() {
        if (!coroutineImplClass.isInstance(this)) return emptyList()
        val resultForThis = (this.javaClass.declaredFields + labelField)
            .associate { it.isAccessible = true; it.name to it.get(this@stateStack) }
            .let(::listOf)
        val resultForCompletion = completion?.stateStack
        return resultForCompletion?.let { resultForThis + it } ?: resultForThis
    }
    set(value) {
        if (!coroutineImplClass.isInstance(this)) return
        val mapForThis = value.first()
        (this.javaClass.declaredFields + labelField).forEach {
            if (it.name in mapForThis) {
                it.isAccessible = true
                val fieldValue = mapForThis[it.name]
                it.set(this@stateStack, fieldValue)
            }
        }
        completion?.stateStack = value.subList(1, value.size)
    }