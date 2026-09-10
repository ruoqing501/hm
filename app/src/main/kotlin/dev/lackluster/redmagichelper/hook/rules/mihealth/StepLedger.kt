package dev.lackluster.redmagichelper.hook.rules.mihealth

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dev.lackluster.redmagichelper.utils.MiHealthChannel
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TreeMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Private write-ahead ledger in the health app sandbox, isolated by account and source.
 *
 * 存储位置:hook 运行在 com.mi.health 进程内,台账放在该应用自己的
 * `filesDir/redmagichelper/` 子目录(见 [MiHealthChannel]),本进程天然可写、
 * 其他应用不可读,不依赖 WRITE_SETTINGS 或 root;进程被杀后记录仍在。
 */
internal class StepLedger(context: Context) {
    class Record(val raw: String, val output: String, val token: String, val generated: Boolean)

    private val directory = File(context.filesDir, MiHealthChannel.MODULE_DIR_NAME)
    private val databaseFile = File(directory, "steps_v1.db")
    private val lockFile = File(directory, "steps.lock")

    @Volatile
    private var database: SQLiteDatabase? = null

    private fun db(): SQLiteDatabase {
        database?.let { if (it.isOpen) return it }
        synchronized(this) {
            database?.let { if (it.isOpen) return it }
            directory.mkdirs()
            val opened = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
            opened.enableWriteAheadLogging()
            opened.execSQL(
                "CREATE TABLE IF NOT EXISTS records(account TEXT NOT NULL,sid TEXT NOT NULL,time INTEGER NOT NULL,raw TEXT NOT NULL,output TEXT NOT NULL,token TEXT NOT NULL,generated INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(account,sid,time))"
            )
            opened.execSQL(
                "CREATE TABLE IF NOT EXISTS plans(account TEXT NOT NULL,id TEXT NOT NULL,day TEXT NOT NULL,spec TEXT NOT NULL,PRIMARY KEY(account,id,day))"
            )
            opened.execSQL(
                "CREATE TABLE IF NOT EXISTS events(account TEXT NOT NULL,id TEXT NOT NULL,day TEXT NOT NULL,time INTEGER NOT NULL,steps INTEGER NOT NULL,admitted INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(account,id,day,time))"
            )
            database = opened
            return opened
        }
    }

    /** Covers the ledger and the native DAO commit together, across health processes. */
    fun guard(): Guard = Guard()

    inner class Guard : AutoCloseable {
        private var file: RandomAccessFile? = null
        private var lock: FileLock? = null
        private var closed = false

        init {
            WRITER.lock()
            try {
                if (WRITER.holdCount == 1) {
                    directory.mkdirs()
                    val f = RandomAccessFile(lockFile, "rw")
                    file = f
                    lock = f.channel.lock()
                }
            } catch (e: Throwable) {
                try {
                    file?.close()
                } catch (ignored: Throwable) {
                }
                WRITER.unlock()
                throw e
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            try {
                try {
                    lock?.release()
                } catch (ignored: Throwable) {
                }
            } finally {
                try {
                    file?.close()
                } catch (ignored: Throwable) {
                }
                WRITER.unlock()
            }
        }
    }

    fun get(account: String, sid: String, time: Long): Record? {
        db().rawQuery(
            "SELECT raw,output,token,generated FROM records WHERE account=? AND sid=? AND time=?",
            arrayOf(account, sid, time.toString())
        ).use { c ->
            return if (c.moveToFirst()) Record(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) != 0)
            else null
        }
    }

    fun put(account: String, sid: String, time: Long, r: Record) {
        val v = ContentValues()
        v.put("account", account)
        v.put("sid", sid)
        v.put("time", time)
        v.put("raw", r.raw)
        v.put("output", r.output)
        v.put("token", r.token)
        v.put("generated", if (r.generated) 1 else 0)
        if (db().insertWithOnConflict("records", null, v, SQLiteDatabase.CONFLICT_REPLACE) < 0) {
            throw IllegalStateException("无法保存增步去重记录")
        }
    }

    fun admit(p: StepPlan, now: Long) {
        val d = db()
        d.beginTransaction()
        try {
            val today = Instant.ofEpochSecond(now).atZone(ZoneId.of(p.zone)).toLocalDate()
            // An old configuration cannot silently schedule years of missed work.
            val first = if (p.startDate.isBefore(today.minusDays(366))) today.minusDays(366) else p.startDate
            var day = first
            while (!day.isAfter(today)) {
                if (p.repeat || day == p.startDate) {
                    val plan = ContentValues()
                    plan.put("account", p.account)
                    plan.put("id", p.id)
                    plan.put("day", day.toString())
                    plan.put("spec", p.serialize())
                    if (d.insertWithOnConflict("plans", null, plan, SQLiteDatabase.CONFLICT_IGNORE) >= 0) {
                        for ((time, steps) in p.timetable(day)) {
                            val v = ContentValues()
                            v.put("account", p.account)
                            v.put("id", p.id)
                            v.put("day", day.toString())
                            v.put("time", time)
                            v.put("steps", steps)
                            d.insertOrThrow("events", null, v)
                        }
                    }
                }
                day = day.plusDays(1)
            }
            d.execSQL(
                "UPDATE events SET admitted=1 WHERE account=? AND id=? AND time<=?",
                arrayOf(p.account, p.id, now)
            )
            d.setTransactionSuccessful()
        } finally {
            d.endTransaction()
        }
    }

    fun admitted(account: String): Map<Long, Int> {
        val values = TreeMap<Long, Int>()
        db().rawQuery(
            "SELECT time,SUM(steps) FROM events WHERE account=? AND admitted=1 GROUP BY time",
            arrayOf(account)
        ).use { c ->
            while (c.moveToNext()) values[c.getLong(0)] = c.getInt(1)
        }
        return values
    }

    fun beginTransaction() = db().beginTransaction()
    fun setTransactionSuccessful() = db().setTransactionSuccessful()
    fun endTransaction() = db().endTransaction()

    companion object {
        private val WRITER = ReentrantLock(true)
    }
}
