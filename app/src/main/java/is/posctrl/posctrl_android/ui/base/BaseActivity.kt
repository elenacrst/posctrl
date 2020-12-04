package `is`.posctrl.posctrl_android.ui.base

import `is`.posctrl.posctrl_android.PosCtrlApplication
import `is`.posctrl.posctrl_android.R
import `is`.posctrl.posctrl_android.data.ErrorCode
import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.ResultWrapper
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import `is`.posctrl.posctrl_android.data.local.set
import `is`.posctrl.posctrl_android.data.model.FilteredInfoResponse
import `is`.posctrl.posctrl_android.di.ActivityComponent
import `is`.posctrl.posctrl_android.di.ActivityModule
import `is`.posctrl.posctrl_android.ui.BaseFragmentHandler
import `is`.posctrl.posctrl_android.util.Event
import `is`.posctrl.posctrl_android.util.extensions.getAppDirectory
import `is`.posctrl.posctrl_android.util.extensions.toast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber
import java.io.File
import javax.inject.Inject


abstract class BaseActivity : AppCompatActivity(), BaseFragmentHandler {
    private lateinit var activityComponent: ActivityComponent
    private var receiver = createLogoutReceiver()

    @Inject
    lateinit var preferences: PreferencesSource

    @Inject
    lateinit var globalViewModel: GlobalViewModel

    fun createDownloadObserver(): Observer<Event<ResultWrapper<*>>> {
        return createLoadingObserver(successListener = {
            toast("successfully downloaded apk")
            openAPK()

        }, errorListener = {
            toast("error downloading apk")
        })
    }

    private fun openAPK() {
        val apkFile = File(getAppDirectory(), PosCtrlRepository.APK_FILE_NAME)
        val intent = Intent(Intent.ACTION_VIEW)
            .apply {
                val uri = FileProvider.getUriForFile(
                    this@BaseActivity,
                    "${applicationContext.packageName}.provider",
                    apkFile
                )
                setDataAndType(uri, INTENT_TYPE_APK)
                val resInfoList: List<ResolveInfo> =
                    applicationContext.packageManager.queryIntentActivities(
                        this,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    applicationContext.grantUriPermission(
                        packageName,
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivityComponent()
        activityComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            receiver,
            IntentFilter(ACTION_LOGOUT)
        )
    }

    private fun createLogoutReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Timber.d("received logout broadcast")
                handleLogout()
            }
        }
    }

    private fun initializeActivityComponent() {
        activityComponent = (application as PosCtrlApplication).appComponent
            .activityComponent(ActivityModule(this))
    }

    private fun handleError(resultError: ResultWrapper.Error): Boolean {
        return when (resultError.code) {
            ErrorCode.NO_DATA_CONNECTION.code -> {
                toast(getString(R.string.no_data_connection))
                true
            }
            else -> false
        }
    }

    override fun onStart() {
        super.onStart()
        preferences.defaultPrefs()[getString(R.string.key_app_visible)] = true
    }

    override fun createLoadingObserver(
        successListener: (ResultWrapper<*>?) -> Unit,
        errorListener: () -> Unit
    ): Observer<Event<ResultWrapper<*>>> {
        return Observer { result ->
            when (val value = result.getContentIfNotHandled()) {
                is ResultWrapper.Success -> {
                    successListener(value)
                }
                is ResultWrapper.Loading -> {
                    showLoading()
                }
                is ResultWrapper.Error -> {
                    hideLoading()
                    val resultError =
                        result.peekContent() as ResultWrapper.Error
                    val resultHandled = handleError(resultError)
                    if (!resultHandled) {
                        toast(
                            message = (result.peekContent() as
                                    ResultWrapper.Error).message.toString()
                        )
                    }
                    errorListener()
                }
                else -> {
                    Timber.d("Nothing to do here")
                }
            }
        }
    }

    abstract override fun showLoading()

    abstract override fun hideLoading()

    override fun onDestroy() {
        super.onDestroy()
        hideLoading()
    }

    override fun handleFilter(result: FilteredInfoResponse?) {
    }

    abstract fun handleLogout()

    override fun onDoubleTap() {
    }

    override fun downloadApk() {
        globalViewModel.downloadApk()
    }

    companion object {
        const val ACTION_LOGOUT = "is.posctrl.posctrl_android.ACTION_LOGOUT"
        const val INTENT_TYPE_APK = "application/vnd.android.package-archive"
    }
}