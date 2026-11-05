package ru.denpom.finance.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Avoid MoneyApp initializers: all tests use their own in-memory Room database. */
class IsolatedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
