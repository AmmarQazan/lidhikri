package com.greendome.adhkar.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

sealed class PlayUpdateManualStatus {
    data object Checking : PlayUpdateManualStatus()
    data object UpToDate : PlayUpdateManualStatus()
    data class Available(val versionCode: Int) : PlayUpdateManualStatus()
    data object Downloaded : PlayUpdateManualStatus()
    data object Cancelled : PlayUpdateManualStatus()
    data object NotFromPlay : PlayUpdateManualStatus()
    data object Failed : PlayUpdateManualStatus()
}

class PlayAppUpdateController(
    private val activity: Activity,
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
    private val store: PlayAppUpdateStore = PlayAppUpdateStore(activity),
) {
    var showAvailableDialog by mutableStateOf(false)
        private set
    var showRestartDialog by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var lastManualStatus by mutableStateOf<PlayUpdateManualStatus?>(null)
        private set

    private var pendingInfo: AppUpdateInfo? = null
    private var pendingVersionCode: Int = 0
    private var lastPromptWasManual = false
    private var flowInFlight = false
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var listenerRegistered = false

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            activity.runOnUiThread {
                showAvailableDialog = false
                showRestartDialog = true
                lastManualStatus = PlayUpdateManualStatus.Downloaded
            }
        }
    }

    fun attach(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        this.launcher = launcher
        if (!listenerRegistered) {
            manager.registerListener(installListener)
            listenerRegistered = true
        }
    }

    fun detach() {
        launcher = null
        if (listenerRegistered) {
            manager.unregisterListener(installListener)
            listenerRegistered = false
        }
    }

    suspend fun autoPromptIfNeeded(): Boolean {
        if (!installedFromPlay()) return false
        val snapshot = checkPlay() ?: return false
        return when (snapshot.kind) {
            CheckKind.DOWNLOADED -> {
                showRestartDialog = true
                true
            }
            CheckKind.IN_PROGRESS -> startFlow(snapshot.info)
            CheckKind.AVAILABLE -> {
                if (
                    PlayAppUpdateEligibility.shouldAutoPrompt(
                        availableVersionCode = snapshot.versionCode,
                        snoozedVersionCode = store.snoozeVersionCode(),
                        snoozeUntilMs = store.snoozeUntilMs(),
                        nowMs = System.currentTimeMillis(),
                    )
                ) {
                    lastPromptWasManual = false
                    showAvailableDialog = true
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    suspend fun checkNowAndApply() {
        busy = true
        lastManualStatus = PlayUpdateManualStatus.Checking
        lastPromptWasManual = true
        try {
            if (!installedFromPlay()) {
                lastManualStatus = PlayUpdateManualStatus.NotFromPlay
                return
            }
            val snapshot = checkPlay()
            if (snapshot == null) {
                lastManualStatus = PlayUpdateManualStatus.Failed
                return
            }
            when (snapshot.kind) {
                CheckKind.UP_TO_DATE -> lastManualStatus = PlayUpdateManualStatus.UpToDate
                CheckKind.DOWNLOADED -> {
                    lastManualStatus = PlayUpdateManualStatus.Downloaded
                    showRestartDialog = true
                }
                CheckKind.IN_PROGRESS -> {
                    lastManualStatus = PlayUpdateManualStatus.Available(snapshot.versionCode)
                    startFlow(snapshot.info)
                }
                CheckKind.AVAILABLE -> {
                    lastManualStatus = PlayUpdateManualStatus.Available(snapshot.versionCode)
                    if (!startFlow(snapshot.info)) {
                        lastManualStatus = PlayUpdateManualStatus.Failed
                        openPlayStore(activity)
                    }
                }
            }
        } finally {
            busy = false
        }
    }

    suspend fun onResume() {
        val snapshot = checkPlay() ?: return
        when (snapshot.kind) {
            CheckKind.DOWNLOADED -> showRestartDialog = true
            CheckKind.IN_PROGRESS -> startFlow(snapshot.info)
            else -> Unit
        }
    }

    fun applyFromDialog() {
        val info = pendingInfo
        showAvailableDialog = false
        if (info == null || !startFlow(info)) {
            openPlayStore(activity)
        }
    }

    fun snoozeFromDialog() {
        if (pendingVersionCode > 0) store.snooze(pendingVersionCode)
        showAvailableDialog = false
    }

    fun restartToComplete() {
        showRestartDialog = false
        manager.completeUpdate()
    }

    fun onUpdateFlowResult(resultCode: Int) {
        flowInFlight = false
        if (resultCode == Activity.RESULT_OK) return
        if (lastPromptWasManual) {
            lastManualStatus = PlayUpdateManualStatus.Cancelled
        } else {
            snoozeFromDialog()
        }
    }

    private suspend fun checkPlay(): UpdateSnapshot? {
        return try {
            val info = manager.appUpdateInfo.await()
            val snapshot = UpdateSnapshot.from(info)
            pendingInfo = info
            pendingVersionCode = snapshot.versionCode
            snapshot
        } catch (e: Exception) {
            Log.w(TAG, "Play update check failed", e)
            pendingInfo = null
            pendingVersionCode = 0
            null
        }
    }

    private fun startFlow(info: AppUpdateInfo): Boolean {
        if (flowInFlight) return true
        val type = preferredType(info) ?: return false
        val updateLauncher = launcher ?: return false
        return try {
            val started = manager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(type).build(),
            )
            if (started) flowInFlight = true
            started
        } catch (e: Exception) {
            Log.w(TAG, "Play update flow failed to start", e)
            false
        }
    }

    private fun preferredType(info: AppUpdateInfo): Int? = when {
        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
        else -> null
    }

    private fun installedFromPlay(): Boolean =
        PlayAppUpdateEligibility.isPlayInstaller(installerPackage(activity))

    private enum class CheckKind { AVAILABLE, UP_TO_DATE, DOWNLOADED, IN_PROGRESS }

    private data class UpdateSnapshot(
        val kind: CheckKind,
        val info: AppUpdateInfo,
        val versionCode: Int,
    ) {
        companion object {
            fun from(info: AppUpdateInfo): UpdateSnapshot {
                val version = info.availableVersionCode()
                val kind = when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> CheckKind.DOWNLOADED
                    info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        CheckKind.IN_PROGRESS
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ||
                            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) ->
                        CheckKind.AVAILABLE
                    else -> CheckKind.UP_TO_DATE
                }
                return UpdateSnapshot(kind, info, version)
            }
        }
    }

    companion object {
        private const val TAG = "PlayAppUpdate"

        fun installerPackage(context: Context): String? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        fun openPlayStore(context: Context) {
            val packageName = context.packageName
            val market = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName"),
            ).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(market)
            } catch (_: ActivityNotFoundException) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
