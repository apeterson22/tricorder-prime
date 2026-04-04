package com.solomonprime.tricorder

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.validation.HostValidator
import androidx.car.app.Session
import androidx.car.app.Screen

class TricorderCarAppService : CarAppService() {
    override fun createHostValidator() = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession() = TricorderCarSession()
}

class TricorderCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = TricorderCarScreen(carContext)
}
