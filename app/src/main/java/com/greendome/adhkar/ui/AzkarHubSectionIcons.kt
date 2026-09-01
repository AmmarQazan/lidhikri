package com.greendome.adhkar.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.HomeAzkar
import com.greendome.adhkar.data.JawamiAzkarSeed
import com.greendome.adhkar.data.RidingAzkar
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.util.AzkarHubOrder

fun azkarHubSectionIconKey(sectionId: String): String = when (sectionId) {
    AzkarHubOrder.MY_DHIKR_ID -> "edit"
    AzkarHubOrder.SHORT_TASBIH_ID -> "repeat"
    JawamiAzkarSeed.COLLECTION_ID -> "auto_awesome"
    AzkarFavorites.COLLECTION_ID -> "favorite"
    "morning" -> "wb_sunny"
    "evening" -> "nights_stay"
    PrayerRespectGate.AFTER_PRAYER_COLLECTION_ID -> "mosque"
    "sleep" -> "bedtime"
    "wake_up" -> "alarm"
    "adhan" -> "notifications_active"
    HomeAzkar.COLLECTION_ID -> "home"
    RidingAzkar.COLLECTION_ID -> "directions_car"
    FridayAzkar.COLLECTION_ID -> "calendar_month"
    BlessedDaysAzkar.COLLECTION_ID -> "star"
    else -> "menu_book"
}

fun azkarHubSectionIcon(sectionId: String): ImageVector = when (azkarHubSectionIconKey(sectionId)) {
    "edit" -> Icons.Default.Edit
    "repeat" -> Icons.Default.Repeat
    "auto_awesome" -> Icons.Default.AutoAwesome
    "favorite" -> Icons.Default.Favorite
    "wb_sunny" -> Icons.Default.WbSunny
    "nights_stay" -> Icons.Default.NightsStay
    "mosque" -> Icons.Default.Mosque
    "bedtime" -> Icons.Default.Bedtime
    "alarm" -> Icons.Default.Alarm
    "notifications_active" -> Icons.Default.NotificationsActive
    "home" -> Icons.Default.Home
    "directions_car" -> Icons.Default.DirectionsCar
    "calendar_month" -> Icons.Default.CalendarMonth
    "star" -> Icons.Default.Star
    else -> Icons.Default.MenuBook
}
