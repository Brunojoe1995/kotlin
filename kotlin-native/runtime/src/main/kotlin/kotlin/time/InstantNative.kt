/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual fun systemClockNow(): Instant = memScoped {
    val tm = alloc<timespec>()
    val error = clock_gettime(CLOCK_REALTIME.convert(), tm.ptr)
    check(error == 0) { "Error when reading the system clock: ${strerror(errno)?.toKString() ?: "Unknown error"}" }
    try {
        require(tm.tv_nsec in 0 until NANOS_PER_ONE)
        Instant(tm.tv_sec.convert(), tm.tv_nsec.convert())
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("The readings from the system clock (${tm.tv_sec} seconds, ${tm.tv_nsec} nanoseconds) are not representable as an Instant")
    }
}
