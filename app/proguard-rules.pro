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

# Supabase DTOs & Kotlinx Serialization keep rules
-keep class com.pixelquest.app.data.remote.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**


# Namaz Tracker reminders (docs/namaz-design.md, Reminders). The receivers are named in the
# manifest, and AAPT keeps manifest components too; they're kept here explicitly because armed
# alarms and posted notifications hold PendingIntents to them by name across an app update.
-keep class com.pixelquest.app.namaz.NamazAlarmReceiver
-keep class com.pixelquest.app.namaz.NamazActionReceiver
-keep class com.pixelquest.app.scheduling.BootReceiver
# Names stored outside the code: prayers and statuses in the database, alarm kinds and prayers in
# intent extras, the sound in SharedPreferences. R8 mustn't rename or unbox these enums, or a
# stored name would stop matching after an update.
-keepclassmembers enum com.pixelquest.app.domain.namaz.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.pixelquest.app.namaz.NamazAction {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
