package com.billbharo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The main [Application] class for the Bill Bharo app.
 *
 * This class is annotated with [@HiltAndroidApp] to enable Hilt for dependency injection
 * throughout the application.
 */
@HiltAndroidApp
class BillBharoApplication : Application()
