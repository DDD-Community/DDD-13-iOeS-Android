package com.pickflow.android.core.services.impl

import android.content.Context
import androidx.core.content.edit
import com.pickflow.android.core.services.protocols.MySpotReleaseStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "my_spot_release"

@Singleton
class PrefsMySpotReleaseStore @Inject constructor(
    @ApplicationContext context: Context,
) : MySpotReleaseStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun released(spotId: Long): Boolean = prefs.getBoolean(spotId.toString(), true)

    override fun setReleased(spotId: Long, released: Boolean) {
        prefs.edit { putBoolean(spotId.toString(), released) }
    }
}
