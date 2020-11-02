package `is`.posctrl.posctrl_android.di

import `is`.posctrl.posctrl_android.data.PosCtrlRepository
import `is`.posctrl.posctrl_android.data.local.ILocalInformation
import `is`.posctrl.posctrl_android.data.local.LocalInformation
import `is`.posctrl.posctrl_android.data.local.PreferencesSource
import android.app.Application
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dagger.Module
import dagger.Provides


@Module
class AppModule(private val app: Application) {

    @ApplicationScope
    @Provides
    fun provideAppContext(): Application =
            app

    @ApplicationScope
    @Provides
    fun provideLocalInfo(preferencesSource: PreferencesSource): ILocalInformation =
            LocalInformation(preferencesSource)

    @ApplicationScope
    @Provides
    fun providePref(app: Application): PreferencesSource =
            PreferencesSource(app)

    @ApplicationScope
    @Provides
    fun provideRepository(): PosCtrlRepository = PosCtrlRepository()

    @ApplicationScope
    @Provides
    fun provideXmlMapper(): XmlMapper {
        return XmlMapper(
                JacksonXmlModule().apply { setDefaultUseWrapper(false) }
        ).apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

}
