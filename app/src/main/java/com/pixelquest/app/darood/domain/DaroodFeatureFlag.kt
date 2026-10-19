package com.pixelquest.app.darood.domain

/**
 * Darood Party's feature flag: whether the feature is offered at all. It's kept with the app's
 * settings (SettingsRepository.daroodAvailable). Off, the side menu has no Darood Party entry and
 * nobody is asked for a username; nothing is deleted, so turning it back on finds everything.
 */
object DaroodFeatureFlag {
    /** On unless turned off. */
    const val DEFAULT_ON = true
}
