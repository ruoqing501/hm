package dev.lackluster.redmagichelper.hook.rules.mihealth

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.data.Scope
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.factory.method
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.hook.compat.type.java.BooleanType
import dev.lackluster.redmagichelper.hook.compat.type.java.ListClass
import dev.lackluster.redmagichelper.hook.compat.type.java.StringClass
import dev.lackluster.redmagichelper.utils.MiHealthChannel
import dev.lackluster.redmagichelper.utils.Prefs
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.Objects
import java.util.TreeMap
import java.util.UUID

/**
 * 通过小米运动健康自己的 DAO 与上传通道改写步数记录(移植自 LS_Augment 的 MiHealthHook)。
 *
 * 与 LS_Augment 一致的关键语义:
 * - fail-closed:先通过内存 Room 库兼容自检(checkAdapter)才会改写任何数据
 * - 台账(StepLedger)去重:同一记录反复入库不会重复加倍/重复加步
 * - 配置经 [Prefs] 运行时重读,开关/倍率/计划修改无需重启健康应用
 * - 配置未生效(未绑定账户/总开关关闭)时停止后续新增,已保存记录保留
 * - 同步走健康应用自己的 syncWithServer 入口;模块不持有网络权限
 *
 * 状态回传:无 FeatureSettings/Provider 可用,改为写 com.mi.health 私有目录下的
 * 状态文件(见 [MiHealthChannel]),模块 UI 经 root shell 读取。
 */
object MiHealthHook : YukiBaseHooker() {
    private const val TAG = "MiHealthHook"
    private const val BASE = "com.xiaomi.fit.fitness."

    private val LOCAL = ThreadLocal.withInitial { false }
    private val GENERATED = ThreadLocal.withInitial { false }
    private val PINNED_DB = ThreadLocal<Any?>()
    private val PINNED_ACCOUNT = ThreadLocal<String?>()

    /** recordDailyRecordToDB 重入时保存外层 LOCAL 值。 */
    private val LOCAL_PREVIOUS = ThreadLocal<Boolean>()

    /** insertDailyRecordToDb 的 before/after 之间传递的本次调用状态。 */
    private class Invocation(
        val guard: StepLedger.Guard?,
        val pin: Controller.AccountPin?,
        val controller: Controller,
    )
    private val ACTIVE = ThreadLocal<Invocation?>()

    @Volatile
    private var controller: Controller? = null

    override fun onHook() {
        val utils = runCatching {
            appClassLoader.loadClass(BASE + "persist.db.utils.DailyRecordDaoUtils")
        }.getOrNull() ?: run {
            YLog.warn(tag = TAG, msg = "未找到 DailyRecordDaoUtils,步数增强未安装")
            return
        }
        val companion = runCatching {
            appClassLoader.loadClass(BASE + "persist.db.FitnessDatabase\$Companion")
        }.getOrNull() ?: run {
            YLog.warn(tag = TAG, msg = "未找到 FitnessDatabase.Companion,步数增强未安装")
            return
        }
        val recordFinder = utils.method {
            name = "recordDailyRecordToDB"
            param(StringClass, StringClass, ListClass, BooleanType)
        }
        val insertFinder = utils.method {
            name = "insertDailyRecordToDb"
            param(StringClass, ListClass, BooleanType)
        }
        val instanceFinder = companion.method {
            name = "getInstance"
            emptyParam()
        }
        // fail-closed:三个入口任何一个解析失败都不安装 hook、不启动控制器
        if (recordFinder.give() == null || insertFinder.give() == null || instanceFinder.give() == null) {
            YLog.error(tag = TAG, msg = "DAO 入口解析失败,步数增强未安装(fail-closed)")
            return
        }

        instanceFinder.hook {
            after {
                PINNED_DB.get()?.let { result = it }
            }
        }
        recordFinder.hook {
            before {
                LOCAL_PREVIOUS.set(LOCAL.get())
                LOCAL.set(true)
            }
            after {
                LOCAL.set(LOCAL_PREVIOUS.get() ?: false)
                LOCAL_PREVIOUS.remove()
            }
        }
        insertFinder.hook {
            before {
                val entities = args[1] as? List<*>
                if (GENERATED.get() || args[0] != "steps" || entities == null) return@before
                val c = ensure() ?: return@before
                var guard: StepLedger.Guard? = null
                var pin: Controller.AccountPin? = null
                try {
                    guard = c.ledger.guard()
                } catch (e: Throwable) {
                    c.error("等待记录写入", e)
                    return@before
                }
                try {
                    pin = c.pinAccount()
                    c.transform(entities, LOCAL.get())
                } catch (e: Throwable) {
                    c.error("保存适配失败", e)
                }
                ACTIVE.set(Invocation(guard, pin, c))
            }
            after {
                val invocation = ACTIVE.get() ?: return@after
                ACTIVE.remove()
                try {
                    invocation.controller.request()
                } finally {
                    invocation.pin?.close()
                    invocation.guard?.close()
                }
            }
        }

        // LS_Augment 在模块进程有配置快照监听;这里没有,改为延迟重试建立控制器,
        // 之后每次入库后防抖触发 + 30s 周期 tick 收敛
        val main = Handler(Looper.getMainLooper())
        val retry = object : Runnable {
            override fun run() {
                if (ensure() == null) main.postDelayed(this, 1500)
            }
        }
        main.postDelayed(retry, 1500)
        YLog.info(tag = TAG, msg = "MI_HEALTH hooks installed")
    }

    @Synchronized
    private fun ensure(): Controller? {
        controller?.let { return it }
        val context = appContext ?: return null
        if (context.packageName != Scope.MI_HEALTH) return null
        val created = Controller(context, appClassLoader)
        controller = created
        return created
    }

    internal class Controller(
        val context: Context,
        private val loader: ClassLoader,
    ) {
        val ledger = StepLedger(context)
        private val worker: Handler

        @Volatile
        var compatible = false
        @Volatile
        var gap = 0L
        var account = ""
        private var lastError = ""
        private var lastSync = 0L
        private var pendingUploads = 0

        private val tick = object : Runnable {
            override fun run() {
                try {
                    refreshIdentity()
                    if (!compatible) checkAdapter()
                    reconcile()
                } catch (e: Throwable) {
                    error("等待健康应用初始化", e)
                }
                worker.postDelayed(this, 30_000)
            }
        }
        private val requested = Runnable {
            try {
                refreshIdentity()
                if (!compatible) checkAdapter()
                reconcile()
            } catch (e: Throwable) {
                error("步数处理失败", e)
            }
        }

        init {
            val thread = HandlerThread("RMH-health-records", Process.THREAD_PRIORITY_BACKGROUND)
            thread.start()
            worker = Handler(thread.looper)
            worker.post(tick)
        }

        fun request() {
            worker.removeCallbacks(requested)
            worker.postDelayed(requested, 1200)
        }

        private fun type(name: String): Class<*> = Class.forName(name, false, loader)

        private fun utils(): Any =
            TargetReflection.singleton(type(BASE + "persist.db.utils.DailyRecordDaoUtils"))

        private fun database(): Any? =
            TargetReflection.call(
                TargetReflection.singleton(type(BASE + "persist.db.FitnessDatabase")),
                "getInstance"
            )

        private fun currentAccount(): String =
            PINNED_ACCOUNT.get() ?: identity().id

        private class Identity(val id: String, val database: Any?)

        private fun identity(): Identity {
            val accountClass = type("com.xiaomi.fitness.account.manager.AccountManager")
            val manager = TargetReflection.call(
                type("com.xiaomi.fitness.account.extensions.AccountManagerExtKt"),
                "getInstance",
                TargetReflection.singleton(accountClass)
            )
            val id = TargetReflection.call(manager!!, "getUserId")
            if (id !is String || id.isEmpty()) return Identity("", null)
            val db = database()
            val helper = TargetReflection.call(db!!, "getOpenHelper")
            val path = TargetReflection.call(helper!!, "getDatabaseName").toString()
            if (id != TargetReflection.call(manager, "getUserId")) {
                throw IllegalStateException("账户正在切换")
            }
            return Identity(hash("${Process.myUid() / 100000}|$id|$path"), db)
        }

        inner class AccountPin : AutoCloseable {
            private val previousAccount: String? = PINNED_ACCOUNT.get()
            private val previousDb: Any? = PINNED_DB.get()

            init {
                if (previousAccount == null) {
                    val v = identity()
                    PINNED_ACCOUNT.set(v.id)
                    if (v.database != null) PINNED_DB.set(v.database)
                }
            }

            override fun close() {
                if (previousAccount == null) PINNED_ACCOUNT.remove() else PINNED_ACCOUNT.set(previousAccount)
                if (previousDb == null) PINNED_DB.remove() else PINNED_DB.set(previousDb)
            }
        }

        fun pinAccount(): AccountPin = AccountPin()

        private fun refreshIdentity() {
            account = currentAccount()
            MiHealthChannel.writeStatus(context) {
                put("account", account)
                put("heartbeat", System.currentTimeMillis())
                if (account.isEmpty()) {
                    put("runtime", "已接入，等待在小米运动健康选择地区并登录账户")
                }
            }
        }

        private fun enabled(id: String): Boolean =
            id.isNotEmpty() && id == (Prefs.getString(Pref.Key.MiHealth.ACCOUNT, "") ?: "") &&
                Prefs.getBoolean(Pref.Key.MiHealth.ENABLED, false)

        private fun phoneSources(): Array<String> {
            val manager = TargetReflection.call(
                type("com.xiaomi.fitness.device.manager.export.DeviceManagerExtKt"),
                "getInstance",
                TargetReflection.singleton(type("com.xiaomi.fitness.device.manager.export.WearableDeviceManager"))
            )
            val local = TargetReflection.call(manager!!, "getLocalPhoneSid") as? String
            val server = TargetReflection.call(manager, "getCurrentPhoneSid") as? String
            if (local.isNullOrEmpty()) throw IllegalStateException("手机记录来源尚未就绪")
            return arrayOf(local, if (server.isNullOrEmpty()) local else server)
        }

        private fun savedRecord(id: String, sid: String, at: Long, phone: Array<String>): StepLedger.Record? {
            var saved = ledger.get(id, sid, at)
            if (saved == null && (sid == phone[0] || sid == phone[1])) {
                saved = ledger.get(id, if (sid == phone[0]) phone[1] else phone[0], at)
                if (saved != null) ledger.put(id, sid, at, saved)
            }
            return saved
        }

        private fun item(at: Long, steps: Int, sid: String): Any {
            val i = type(BASE + "export.data.item.StepItem")
                .getConstructor(Long::class.java, Int::class.java, Int::class.java, Float::class.java)
                .newInstance(at, steps, 0, 0f)
            TargetReflection.call(i, "setSid", sid)
            return i
        }

        private fun aggregate(values: List<Any>, day: Long): Int {
            val biz = type("com.xiaomi.fitness.repo.step.StepBiz").getConstructor().newInstance()
            val map = HashMap<String, List<Any>>()
            map["steps"] = values
            val report = TargetReflection.call(biz, "splitDailyReport", null, "days", day, map)
            return TargetReflection.call(report!!, "getSteps") as Int
        }

        private fun checkAdapter() {
            gap = (TargetReflection.field(
                type("com.xiaomi.fitness.repo.RepositoryManager"), "GAP_X_MINUTE"
            ) as Number).toLong()
            check(gap in 60..3600 && 86400 % gap == 0L) { "未识别步数合并间隔" }
            val day = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            val before = mutableListOf(item(day + 60, 100, "phone"), item(day + 120, 200, "watch"))
            check(aggregate(before, day) == 200) { "原厂来源合并方式已变化" }
            before.add(item(day + 180, 223, "phone"))
            check(aggregate(before, day) == 323) { "新增步数合并验证未通过" }
            // Exercise the installed Room implementation in memory, with no account database,
            // sync callbacks, or outer recordDailyRecordToDB notifications involved.
            val builder = TargetReflection.call(
                type("androidx.room.Room"), "inMemoryDatabaseBuilder",
                context, type(BASE + "persist.db.FitnessDatabase")
            )
            val temporary = TargetReflection.call(builder!!, "build")!!
            try {
                val dao = TargetReflection.call(temporary, "stepRecordItemDao")!!
                val row = type(BASE + "persist.db.internal.StepRecordEntity")
                    .getConstructor(String::class.java, String::class.java, Long::class.java)
                    .newInstance("steps", "rmh-check", day + 60)
                val model = type(BASE + "DailyRecordItemModel")
                    .getConstructor(
                        Long::class.java, Int::class.java,
                        type(BASE + "export.data.item.DailyRecordItem")
                    )
                    .newInstance(
                        day + 60,
                        ZoneId.systemDefault().rules.getOffset(Instant.ofEpochSecond(day + 60)).totalSeconds,
                        item(day + 60, 10, "rmh-check")
                    )
                TargetReflection.call(row, "setFromLocalRecord", model)
                TargetReflection.call(dao, "insertReplaceAll", listOf(row))
                val value = JSONObject(TargetReflection.call(row, "getValue") as String)
                value.put("steps", 11)
                TargetReflection.call(row, "setRecordValue", value.toString())
                TargetReflection.call(dao, "insertReplaceAll", listOf(row))
                val query = type("androidx.sqlite.db.SimpleSQLiteQuery")
                    .getConstructor(String::class.java, Array<Any>::class.java)
                    .newInstance(
                        "SELECT * FROM step_record WHERE key=? AND sid=? AND time=?",
                        arrayOf("steps", "rmh-check", day + 60)
                    )
                @Suppress("UNCHECKED_CAST")
                val saved = TargetReflection.call(dao, "getDailyRecord", query) as List<*>
                val first = saved.firstOrNull()
                check(
                    saved.size == 1 &&
                        JSONObject(TargetReflection.call(first!!, "getValue") as String).getInt("steps") == 11 &&
                        TargetReflection.call(first, "isUpload") != true &&
                        TargetReflection.call(first, "isDeleted") != true
                ) { "原厂记录替换或待同步标记校验未通过" }
            } finally {
                TargetReflection.call(temporary, "close")
            }
            compatible = true
            lastError = ""
            MiHealthChannel.writeStatus(context) {
                put("compatibility", "通过：原厂临时数据库写入、去重、待同步标记与多来源合并校验")
            }
        }

        fun transform(entities: List<*>, local: Boolean) {
            val id = currentAccount()
            if (id.isEmpty()) return
            val phone = phoneSources()
            val percent = if (enabled(id) && Prefs.getBoolean(Pref.Key.MiHealth.MULTIPLY_ENABLED, false)) {
                Prefs.getInt(Pref.Key.MiHealth.MULTIPLIER, 1).coerceIn(1, 10) * 100
            } else 100
            if (!compatible) return
            val since = (Prefs.getString(Pref.Key.MiHealth.SINCE, "0") ?: "0").toLongOrNull() ?: Long.MAX_VALUE
            ledger.beginTransaction()
            val changes = ArrayList<Triple<Any, String, Boolean>>()
            try {
                for (entity in entities) {
                    entity ?: continue
                    if (TargetReflection.call(entity, "getKey") != "steps" ||
                        TargetReflection.call(entity, "isDeleted") == true
                    ) continue
                    val sid = TargetReflection.call(entity, "getSid") as String
                    val time = TargetReflection.call(entity, "getTime") as Long
                    val input = TargetReflection.call(entity, "getValue") as String
                    val value = JSONObject(input)
                    if (!value.has("steps") || value.getLong("steps") < 0 ||
                        value.getLong("steps") > Int.MAX_VALUE
                    ) continue
                    val old = savedRecord(id, sid, time, phone)
                    var output = input
                    if (old != null && ((old.generated && !local) ||
                            (!local && sameReading(input, old.output)) || old.token == value.optString("_lsAugment"))
                    ) {
                        output = old.output
                    } else if (old != null && !local && sameReading(input, old.raw)) {
                        output = old.output
                    } else {
                        val rate = if (time >= since) percent else 100
                        if (old == null && rate == 100) continue
                        val raw = JSONObject(input)
                        raw.remove("_lsAugment")
                        val token = old?.token ?: UUID.randomUUID().toString()
                        // Previously generated bonus rows are reconciled separately and never multiplied.
                        val steps = if (old != null && old.generated) raw.getInt("steps")
                        else StepMath.multiply(raw.getInt("steps"), rate)
                        value.put("steps", steps)
                        value.put("_lsAugment", token)
                        output = value.toString()
                        ledger.put(id, sid, time, StepLedger.Record(raw.toString(), output, token, old != null && old.generated))
                    }
                    if (input != output) {
                        changes.add(
                            Triple(
                                entity, output,
                                TargetReflection.call(entity, "isUpload") == true && !sameReading(input, output)
                            )
                        )
                    }
                }
                ledger.setTransactionSuccessful()
            } finally {
                ledger.endTransaction()
            }
            // The intent is durable before any entity is changed. Retries reuse the same output.
            for ((entity, output, resetUpload) in changes) {
                TargetReflection.call(entity, "setRecordValue", output)
                if (resetUpload) TargetReflection.call(entity, "setUpload", false)
            }
        }

        private fun reconcile() {
            ledger.guard().use { AccountPin().use { reconcileLocked() } }
        }

        private fun reconcileLocked() {
            val id = currentAccount()
            if (id.isEmpty() || !compatible) return
            val now = System.currentTimeMillis() / 1000
            val plan = StepPlan.parse(Prefs.getString(Pref.Key.MiHealth.PLAN, "") ?: "")
            if (enabled(id) && Prefs.getBoolean(Pref.Key.MiHealth.PLAN_ENABLED, false) &&
                plan != null && plan.account == id
            ) {
                ledger.admit(plan, now - 60)
            }
            val due = ledger.admitted(id)
            val buckets = TreeMap<Long, Int>()
            val zone = ZoneId.systemDefault()
            for ((time, steps) in due) {
                val day = Instant.ofEpochSecond(time).atZone(zone).toLocalDate()
                    .atStartOfDay(zone).toEpochSecond()
                val bucket = day + (time - day) / gap * gap
                buckets.merge(bucket, steps) { a, b -> Math.addExact(a, b) }
            }
            var changed = 0
            pendingUploads = 0
            for ((bucket, bonus) in buckets) {
                if (reconcileBucket(id, bucket, bonus, due)) changed++
            }
            if ((changed > 0 || pendingUploads > 0) && now - lastSync >= 60 && id == identity().id) {
                val syncer = TargetReflection.call(utils(), "getServerSyncer")!!
                val callback = type(BASE + "export.api.FitnessServerSyncCallback")
                val result = Proxy.newProxyInstance(loader, arrayOf(callback)) { proxy, method, args ->
                    if (method.declaringClass == Any::class.java) {
                        return@newProxyInstance when (method.name) {
                            "hashCode" -> System.identityHashCode(proxy)
                            "equals" -> proxy === args?.get(0)
                            else -> "RMH-health-sync"
                        }
                    }
                    if (method.name == "onSyncResult") {
                        MiHealthChannel.writeStatus(context) {
                            put(
                                "runtime",
                                (if (args?.get(0) == true) "原厂同步完成" else "原厂同步未完成，将按原厂流程重试") +
                                    "；ts=${System.currentTimeMillis()}"
                            )
                        }
                    }
                    null
                }
                lastSync = now
                TargetReflection.call(syncer, "syncWithServer", false, "manual", result)
            }
            lastError = ""
            val admitted = due.values.sum()
            MiHealthChannel.writeStatus(context) {
                put(
                    "runtime",
                    "已接入保存与同步；计划累计已到时 $admitted 步；本轮更新 $changed 个时间段；" +
                        "待同步 $pendingUploads 段；${if (enabled(id)) "配置生效" else "已停止新增"}；" +
                        "ts=${System.currentTimeMillis()}"
                )
            }
        }

        private fun reconcileBucket(id: String, bucket: Long, bonus: Int, due: Map<Long, Int>): Boolean {
            val db = database()!!
            val dao = TargetReflection.call(db, "stepRecordItemDao")!!
            val query = type("androidx.sqlite.db.SimpleSQLiteQuery")
                .getConstructor(String::class.java, Array<Any>::class.java)
                .newInstance(
                    "SELECT * FROM step_record WHERE key=? AND time>=? AND time<? AND isDeleted=0",
                    arrayOf("steps", bucket, bucket + gap)
                )
            @Suppress("UNCHECKED_CAST")
            val records = TargetReflection.call(dao, "getDailyRecord", query) as List<*>
            val phoneFamily = phoneSources()
            val phone = phoneFamily[1]
            val natural = LinkedHashMap<String, Long>()
            val generated = TreeMap<Long, Any>()
            val generatedLedgers = TreeMap<Long, StepLedger.Record>()
            val obsolete = ArrayList<Any>()
            val occupied = HashSet<Long>()
            for (record in records) {
                record ?: continue
                val sid = TargetReflection.call(record, "getSid") as String
                val at = TargetReflection.call(record, "getTime") as Long
                val json = TargetReflection.call(record, "getValue") as String
                val saved = savedRecord(id, sid, at, phoneFamily)
                var count = JSONObject(json).getInt("steps")
                if (sid == phone) occupied.add(at)
                if (saved != null && saved.generated && (sid == phoneFamily[0] || sid == phoneFamily[1])) {
                    if (TargetReflection.call(record, "isUpload") != true) pendingUploads++
                    val minute = at / 60 * 60
                    val previous = generated[minute]
                    if (previous == null || sid == phone) {
                        if (previous != null) obsolete.add(previous)
                        generated[minute] = record
                        generatedLedgers[minute] = saved
                    } else {
                        obsolete.add(record)
                    }
                    if (sid != phone && !obsolete.contains(record)) obsolete.add(record)
                    count = JSONObject(saved.raw).optInt("steps", 0)
                }
                natural.merge(sid, count.toLong()) { a, b -> Math.addExact(a, b) }
            }
            val compensation = StepMath.phoneAddition(natural, phone, bonus) - bonus
            val models = ArrayList<Any>()
            var first = true
            for ((minute, eventSteps) in due) {
                if (minute < bucket || minute >= bucket + gap) continue
                val existing = generated[minute]
                val saved = generatedLedgers[minute]
                var targetTime = if (existing == null) -1L else TargetReflection.call(existing, "getTime") as Long
                if (targetTime < 0) {
                    var t = minute + 59
                    while (t >= minute) {
                        if (!occupied.contains(t)) {
                            targetTime = t
                            break
                        }
                        t--
                    }
                }
                check(targetTime >= 0) { "目标分钟没有可用记录位置" }
                val raw = if (saved == null) {
                    JSONObject().put("time", targetTime).put("steps", 0).put("distance", 0).put("calories", 0)
                } else JSONObject(saved.raw)
                val desired = Math.addExact(
                    raw.getInt("steps"),
                    Math.addExact(eventSteps, if (first) compensation else 0)
                )
                first = false
                if (existing != null && phone == TargetReflection.call(existing, "getSid") &&
                    JSONObject(TargetReflection.call(existing, "getValue") as String).getInt("steps") == desired
                ) continue
                val token = saved?.token ?: UUID.randomUUID().toString()
                val output = JSONObject(raw.toString()).put("steps", desired).put("_lsAugment", token)
                ledger.put(id, phone, targetTime, StepLedger.Record(raw.toString(), output.toString(), token, true))
                val step = type(BASE + "export.data.item.StepItem")
                    .getConstructor(Long::class.java, Int::class.java, Int::class.java, Float::class.java)
                    .newInstance(targetTime, desired, raw.optInt("distance", 0), raw.optDouble("calories", 0.0).toFloat())
                TargetReflection.call(step, "setSid", phone)
                val offset = ZoneId.systemDefault().rules
                    .getOffset(Instant.ofEpochSecond(targetTime)).totalSeconds
                models.add(
                    type(BASE + "DailyRecordItemModel")
                        .getConstructor(
                            Long::class.java, Int::class.java,
                            type(BASE + "export.data.item.DailyRecordItem")
                        )
                        .newInstance(targetTime, offset, step)
                )
            }
            if (models.isEmpty() && obsolete.isEmpty()) return false
            if (id != identity().id) throw IllegalStateException("账户已切换，本轮计划暂停")
            val prior = GENERATED.get()
            GENERATED.set(true)
            val previousDb = PINNED_DB.get()
            PINNED_DB.set(db)
            try {
                if (models.isNotEmpty() &&
                    TargetReflection.call(utils(), "recordDailyRecordToDB", "steps", phone, models, true) != true
                ) {
                    throw IllegalStateException("健康应用拒绝保存本段记录")
                }
                // Xiaomi migrates local phone rows to its registered server SID after upload.
                // Remove only ledger-owned duplicate rows, after the canonical row is durable.
                if (obsolete.isNotEmpty()) {
                    TargetReflection.call(utils(), "hardDeleteDailyRecord", "steps", obsolete)
                }
            } finally {
                GENERATED.set(prior)
                if (previousDb == null) PINNED_DB.remove() else PINNED_DB.set(previousDb)
            }
            return true
        }

        fun error(stage: String, e: Throwable) {
            val cause = if (e is InvocationTargetException && e.cause != null) e.cause!! else e
            val message = stage + "：" + cause.javaClass.simpleName + " " +
                Objects.toString(cause.message, "")
            if (message != lastError) {
                lastError = message
                MiHealthChannel.writeStatus(context) { put("runtime", message) }
                YLog.error(tag = TAG, msg = "HEALTH $message", e = cause)
            }
        }
    }

    private fun sameReading(a: String, b: String): Boolean {
        return try {
            val x = JSONObject(a)
            val y = JSONObject(b)
            x.optLong("time") == y.optLong("time") && x.getLong("steps") == y.getLong("steps") &&
                x.optLong("distance") == y.optLong("distance") &&
                x.optDouble("calories", 0.0).compareTo(y.optDouble("calories", 0.0)) == 0
        } catch (ignored: Exception) {
            false
        }
    }

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        val out = StringBuilder()
        for (b in bytes) out.append(String.format(Locale.ROOT, "%02x", b.toInt() and 255))
        return out.toString()
    }
}
