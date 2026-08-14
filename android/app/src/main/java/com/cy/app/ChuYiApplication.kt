package com.cy.app

import android.app.Application
import com.cy.app.data.ConversationRepository
import com.cy.app.data.SettingsRepository

class ChuYiApplication : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var conversationRepository: ConversationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        conversationRepository = ConversationRepository(this)
    }
}