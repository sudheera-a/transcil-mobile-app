# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Gson / @SerializedName models
-keepattributes EnclosingMethod
-keep class com.transcil.rider.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# EncryptedSharedPreferences / security-crypto
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Navigation safe args / fragment class names in XML
-keepnames class com.transcil.rider.**Fragment
-keepnames class com.transcil.rider.**Activity

# ViewBinding / DataBinding
-keep class com.transcil.rider.databinding.** { *; }

# Play Services SMS Retriever
-keep class com.google.android.gms.auth.api.phone.** { *; }
-dontwarn com.google.android.gms.**

# Custom Tabs
-keep class androidx.browser.** { *; }
