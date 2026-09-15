package dev.lackluster.redmagichelper.hook.rules.android.nubia

import dev.lackluster.redmagichelper.data.Pref
import dev.lackluster.redmagichelper.hook.compat.XposedEnv
import dev.lackluster.redmagichelper.hook.compat.entity.YukiBaseHooker
import dev.lackluster.redmagichelper.hook.compat.log.YLog
import dev.lackluster.redmagichelper.utils.Prefs
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

/**
 * 允许与已安装应用同包名但签名不同的 APK 覆盖安装。
 *
 * 正常校验流程会先执行，仅对 UPDATE_INCOMPATIBLE 这一特定签名错误放行。
 * sharedUser 与跨包签名权限检查仍保留在平台实现中。
 *
 * 与 [NubiaDisableSystemSignatureVerification] 的区别：后者在解析 APK 时降低
 * 签名校验强度（影响所有安装），本 Hook 只在"覆盖安装时新旧签名不一致"这一个
 * 失败点上放行，其余校验不动。
 */
object SignatureMismatchInstallHook : YukiBaseHooker() {
    private const val TAG = "SignatureMismatchInstall"

    private const val PACKAGE_MANAGER_UTILS = "com.android.server.pm.PackageManagerServiceUtils"
    private const val PACKAGE_SETTING = "com.android.server.pm.PackageSetting"
    private const val INSTALL_PACKAGE_HELPER = "com.android.server.pm.InstallPackageHelper"
    private const val INSTALL_REQUEST = "com.android.server.pm.InstallRequest"
    private const val PARSED_PACKAGE = "com.android.internal.pm.parsing.pkg.ParsedPackage"
    private const val SIGNING_DETAILS = "android.content.pm.SigningDetails"

    private const val INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7
    private const val CAPABILITY_PERMISSION = 1
    private const val CAPABILITY_ROLLBACK = 8
    private const val DIRECT_SIGNATURE_CHECKS = 2

    private val currentInstall = ThreadLocal<InstallAttempt>()

    private class InstallAttempt(var packageName: String?) {
        var hasSharedUser: Boolean = false
        var verifying: Boolean = false
        var directChecksRemaining: Int = 0
    }

    private fun newInstallAttempt(packageName: String?) = InstallAttempt(packageName).apply {
        directChecksRemaining = if (isEligible(packageName)) DIRECT_SIGNATURE_CHECKS else 0
    }

    override fun onHook() {
        runCatching { hookVerifySignatures() }
            .onFailure { YLog.error("$TAG: hook verifySignatures failed", it) }
        runCatching { hookDoesSignatureMatchForPermissions() }
            .onFailure { YLog.error("$TAG: hook doesSignatureMatchForPermissions failed", it) }
        runCatching { hookPreparePackage() }
            .onFailure { YLog.error("$TAG: hook preparePackage failed", it) }
        runCatching { hookCheckCapability() }
            .onFailure { YLog.error("$TAG: hook checkCapability failed", it) }
    }

    private fun isHookEnabled() =
        Prefs.getBoolean(Pref.Key.Android.ANDROID_ALLOW_SIGNATURE_MISMATCH_INSTALL, false)

    private fun hookVerifySignatures() {
        val verifier = findClass(PACKAGE_MANAGER_UTILS)
        for (method in verifier.declaredMethods) {
            if (method.name != "verifySignatures"
                || method.returnType != java.lang.Boolean.TYPE
                || method.parameterCount != 7
                || method.parameterTypes[0].name != PACKAGE_SETTING
            ) continue
            method.isAccessible = true
            XposedEnv.module.hook(method).intercept { chain -> interceptVerify(chain) }
            YLog.debug("$TAG: hooked ${method.toGenericString()}")
        }
    }

    private fun hookDoesSignatureMatchForPermissions() {
        val installer = findClass(INSTALL_PACKAGE_HELPER)
        for (method in installer.declaredMethods) {
            if (method.name != "doesSignatureMatchForPermissions"
                || method.returnType != java.lang.Boolean.TYPE
                || method.parameterCount != 3
                || method.parameterTypes[0] != String::class.java
                || method.parameterTypes[1].name != PARSED_PACKAGE
                || method.parameterTypes[2] != Integer.TYPE
            ) continue
            method.isAccessible = true
            XposedEnv.module.hook(method).intercept { chain -> interceptOwnPermissionDeclarations(chain) }
            YLog.debug("$TAG: hooked ${method.toGenericString()}")
        }
    }

    private fun hookPreparePackage() {
        val installer = findClass(INSTALL_PACKAGE_HELPER)
        for (method in installer.declaredMethods) {
            if (method.name != "preparePackage"
                || method.returnType != Void.TYPE
                || method.parameterCount != 1
                || method.parameterTypes[0].name != INSTALL_REQUEST
            ) continue
            method.isAccessible = true
            XposedEnv.module.hook(method).intercept { chain -> interceptPreparePackage(chain) }
            YLog.debug("$TAG: hooked ${method.toGenericString()}")
        }
    }

    private fun hookCheckCapability() {
        val signingDetails = findClass(SIGNING_DETAILS)
        for (method in signingDetails.declaredMethods) {
            if (method.name != "checkCapability"
                || method.returnType != java.lang.Boolean.TYPE
                || method.parameterCount != 2
                || method.parameterTypes[0] != signingDetails
                || method.parameterTypes[1] != Integer.TYPE
            ) continue
            method.isAccessible = true
            XposedEnv.module.hook(method).intercept { chain -> interceptCheckCapability(chain) }
            YLog.debug("$TAG: hooked ${method.toGenericString()}")
        }
    }

    private fun interceptVerify(chain: XposedInterface.Chain): Any? {
        if (!isHookEnabled()) return chain.proceed()
        val packageName = packageNameOf(chain.getArg(0))
        val attempt = currentInstall.get()
        val hasSharedUser = chain.getArg(1) != null || hasSharedUser(chain.getArg(0))
        if (attempt != null && packageName != null) {
            attempt.packageName = packageName
            attempt.hasSharedUser = hasSharedUser
            attempt.directChecksRemaining = DIRECT_SIGNATURE_CHECKS
            attempt.verifying = true
        }
        try {
            return chain.proceed()
        } catch (error: Throwable) {
            val errorCode = intField(error, "error", Int.MIN_VALUE)
            if (!shouldBypassUpdate(packageName, hasSharedUser, errorCode, error.message)) {
                throw error
            }
            if (attempt != null) {
                attempt.packageName = packageName
                attempt.hasSharedUser = hasSharedUser
                attempt.directChecksRemaining = DIRECT_SIGNATURE_CHECKS
            }
            YLog.info("$TAG: bypass update-incompatible signature for $packageName (verifySignatures)")
            // verifySignatures 的返回值表示是否恢复了旧的 key-set 数据；
            // 返回 false 表示跳过该清理、继续更新流程。
            return false
        } finally {
            attempt?.verifying = false
        }
    }

    /**
     * Android 16 在 verifySignatures() 之后，还会在 InstallPackageHelper.preparePackage()
     * 内联做第二次签名校验。把包名记录在本线程上，使 capability Hook 只影响这一次更新。
     */
    private fun interceptPreparePackage(chain: XposedInterface.Chain): Any? {
        if (!isHookEnabled()) return chain.proceed()
        val previous = currentInstall.get()
        currentInstall.set(newInstallAttempt(packageNameFromInstallRequest(chain.getArg(0))))
        try {
            return chain.proceed()
        } finally {
            if (previous == null) currentInstall.remove() else currentInstall.set(previous)
        }
    }

    /**
     * 只放行内联更新门里失败的那两次 capability 调用；
     * verifySignatures 自身发起的调用一律不动。
     */
    private fun interceptCheckCapability(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        if (!isHookEnabled() || result == true) return result

        val attempt = currentInstall.get() ?: return result
        if (attempt.verifying) return result

        val capability = chain.getArg(1) as? Int ?: -1
        if (attempt.directChecksRemaining <= 0
            || !isEligible(attempt.packageName)
            || attempt.hasSharedUser
            || (capability != CAPABILITY_PERMISSION && capability != CAPABILITY_ROLLBACK)
        ) return result

        attempt.directChecksRemaining--
        YLog.info("$TAG: bypass direct capability check for ${attempt.packageName} (capability=$capability)")
        return true
    }

    private fun interceptOwnPermissionDeclarations(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        if (!isHookEnabled() || result == true) return result

        val ownerPackage = chain.getArg(0) as? String
        val incomingPackage = packageNameOf(chain.getArg(1))
        if (!isEligible(incomingPackage) || incomingPackage != ownerPackage) return result

        YLog.info("$TAG: keep own permission declarations for $incomingPackage")
        return true
    }

    private fun shouldBypassUpdate(
        packageName: String?,
        hasSharedUser: Boolean,
        errorCode: Int,
        message: String?,
    ): Boolean {
        if (!isEligible(packageName) || hasSharedUser || errorCode != INSTALL_FAILED_UPDATE_INCOMPATIBLE) {
            return false
        }
        return message?.lowercase()?.contains("signature") == true
    }

    private fun isEligible(packageName: String?): Boolean =
        !packageName.isNullOrEmpty() && packageName != "android"

    private fun packageNameOf(owner: Any?): String? {
        var type: Class<*>? = owner?.javaClass ?: return null
        while (type != null) {
            try {
                val method: Method = type.getDeclaredMethod("getPackageName")
                method.isAccessible = true
                return method.invoke(owner) as? String
            } catch (_: NoSuchMethodException) {
                // 继续沿 PackageSetting / ParsedPackage 继承体系查找
                type = type.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun packageNameFromInstallRequest(request: Any?): String? {
        if (request == null) return null
        packageNameOf(fieldValue(request, "mParsedPackage"))?.let { return it }
        packageNameOf(fieldValue(request, "mPackageLite"))?.let { return it }
        packageNameOf(fieldValue(request, "mPkg"))?.let { return it }
        stringField(request, "mName")?.let { return it }
        return stringField(request, "mExistingPackageName")
    }

    private fun fieldValue(owner: Any?, name: String): Any? {
        var type: Class<*>? = owner?.javaClass ?: return null
        while (type != null) {
            try {
                val field = type.getDeclaredField(name)
                field.isAccessible = true
                return field.get(owner)
            } catch (_: NoSuchFieldException) {
                type = type.superclass
            } catch (_: Throwable) {
                return null
            }
        }
        return null
    }

    private fun stringField(owner: Any?, name: String): String? =
        (fieldValue(owner, name) as? String)?.takeIf { it.isNotEmpty() }

    private fun hasSharedUser(packageSetting: Any?): Boolean {
        if (packageSetting == null) return false
        return runCatching {
            val method = packageSetting.javaClass.getMethod("hasSharedUser")
            method.isAccessible = true
            method.invoke(packageSetting) == true
        }.getOrDefault(false)
    }

    private fun intField(owner: Any?, name: String, fallback: Int): Int {
        var type: Class<*>? = owner?.javaClass ?: return fallback
        while (type != null) {
            try {
                val field = type.getDeclaredField(name)
                field.isAccessible = true
                return field.getInt(owner)
            } catch (_: NoSuchFieldException) {
                // 继续沿 PackageManagerException 继承体系查找
                type = type.superclass
            } catch (_: Throwable) {
                return fallback
            }
        }
        return fallback
    }
}
