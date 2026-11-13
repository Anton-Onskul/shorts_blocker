package com.shortsblock.app;

import com.shortsblock.app.core.DumpLog;

/** Общий для службы и экрана журнал диагностики (живёт в памяти процесса, на диск не пишется). */
final class Diagnostics {
    static final long DURATION_MS = 2 * 60_000L;
    static final DumpLog LOG = new DumpLog(40, 2_000_000);

    private Diagnostics() { }
}
