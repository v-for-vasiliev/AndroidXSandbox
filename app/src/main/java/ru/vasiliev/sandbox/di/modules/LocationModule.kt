package ru.vasiliev.sandbox.di.modules

import android.location.LocationManager
import com.google.android.gms.location.LocationRequest
import dagger.Module
import dagger.Provides
import ru.vasiliev.sandbox.App
import ru.vasiliev.sandbox.BuildConfig
import ru.vasiliev.sandbox.di.scopes.AppScope
import ru.vasiliev.sandbox.location.LocationServices
import ru.vasiliev.sandbox.location.RxFusedLocationProvider
import ru.vasiliev.sandbox.location.RxLegacyLocationProvider
import ru.vasiliev.sandbox.location.RxLocationProvider
import javax.inject.Named

/**
 * Date: 04.08.2019
 *
 * @author Kirill Vasiliev
 */
@Module
class LocationModule {

    @Named("fused")
    @AppScope
    @Provides
    fun provideRxFusedLocationProvider(app: App): RxLocationProvider {
        return RxFusedLocationProvider.Builder(app)
            .updateIntervalMilliseconds(BuildConfig.LOCATION_UPDATE_INTERVAL_MS)
            .fastestIntervalMilliseconds(BuildConfig.LOCATION_FASTEST_UPDATE_INTERVAL_MS)
            .priority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).build()
    }

    @Named("legacy")
    @AppScope
    @Provides
    fun provideRxLegacyLocationProvider(app: App): RxLocationProvider {
        return RxLegacyLocationProvider.Builder(app)
            .provider(LocationManager.NETWORK_PROVIDER)
            .updateIntervalMilliseconds(BuildConfig.LOCATION_UPDATE_INTERVAL_MS)
            .fastestIntervalMilliseconds(BuildConfig.LOCATION_FASTEST_UPDATE_INTERVAL_MS)
            .priority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY).build()
    }

    @AppScope
    @Provides
    fun provideLocationServices(
        app: App, @Named(BuildConfig.LOCATION_PROVIDER_TYPE)
        locationProvider: RxLocationProvider
    ): LocationServices {
        return LocationServices(app, locationProvider)
    }
}
