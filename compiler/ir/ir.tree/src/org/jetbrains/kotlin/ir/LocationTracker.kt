/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

object LocationTracker {
    init {
        registerHook()
    }
    fun initialize() {}

    val parameterAddedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val parameterRemovedWhileThereAreAlreadyCalls = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForNonExistingParameter = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()
    val argumentAddedForParameterInsertedAfterCreation = ConcurrentHashMap.newKeySet<List<StackTraceElement>>()

    private fun registerHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            listOf(
                ::parameterAddedWhileThereAreAlreadyCalls,
                ::parameterRemovedWhileThereAreAlreadyCalls,
                ::argumentAddedForNonExistingParameter,
                ::argumentAddedForParameterInsertedAfterCreation
            ).forEach { prop ->
                val name = prop.name
                val set = prop.get()
                println()
                println("!!! $name")
                set.forEach { stack ->
                    stack.firstOrNull()?.let { println(it) }
                    println(stack.drop(1).joinToString("\n") { "  $it" })
                    println()
                }
                println()
            }
        })
    }

    fun recordStackTrace(registry: MutableSet<List<StackTraceElement>>, framesToSkip: Int = 0) {
        val stack = StackWalker.getInstance().walk<List<StackTraceElement>> { stack ->
            var countAbove = 0
            stack
                .skip((framesToSkip + 1).toLong())
                //.limit(20)
                .takeWhile { frame ->
                    countAbove == 0 && (
                            frame.className.startsWith("org.jetbrains.kotlin.ir")
                                    && !frame.className.startsWith("org.jetbrains.kotlin.ir.backend")
                            ) || countAbove++ < 1
                }
                .map { it.toStackTraceElement() }
                .distinct()
                .collect(Collectors.toList())
        }

        //println(stack.joinToString("\n") { "  $it"})
        registry += stack
    }
}