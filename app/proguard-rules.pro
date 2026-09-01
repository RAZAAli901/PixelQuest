# ProGuard & R8 Keep Rules for PixelQuest

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep class com.pixelquest.app.data.local.entity.** { *; }
-keep interface com.pixelquest.app.data.local.dao.** { *; }
-keep class com.pixelquest.app.data.local.Converters { *; }
-dontwarn androidx.room.paging.**

# Dagger Hilt keep rules
-keep class * extends javax.inject.Provider
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.ComponentEntryPoint

# Jetpack Compose keep rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# WorkManager keep rules
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Model classes used in JSON serialization / backup payload
-keep class com.pixelquest.app.data.backup.** { *; }
-keep class com.pixelquest.app.domain.model.** { *; }
