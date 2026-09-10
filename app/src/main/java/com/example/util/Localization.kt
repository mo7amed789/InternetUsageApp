package com.example.util

object Localization {

    enum class Lang {
        AR,
        EN
    }

    // Helper translation retriever
    fun tr(key: String, lang: Lang): String {
        return if (lang == Lang.AR) {
            arabicMap[key] ?: englishMap[key] ?: key
        } else {
            englishMap[key] ?: key
        }
    }

    private val arabicMap = mapOf(
        "app_title" to "DataPulse",
        "tab_dashboard" to "الرئيسية",
        "tab_apps" to "التطبيقات",
        "tab_saver" to "توفير البيانات",
        "tab_speedtest" to "اختبار السرعة",
        "tab_settings" to "الإعدادات",

        // Dashboard
        "live_speed" to "السرعة اللحظية",
        "download" to "تحميل",
        "upload" to "رفع",
        "today_usage" to "استهلاك اليوم",
        "mobile_data" to "بيانات الهاتف",
        "wifi_data" to "واي فاي",
        "quota_status" to "حالة الباقة الشهرية",
        "remaining" to "المتبقي",
        "of" to "من",
        "days_left" to "يوم متبقي في الدورة",
        "period_today" to "اليوم",
        "period_week" to "الأسبوع",
        "period_month" to "الشهر",
        "period_year" to "السنة",
        "period_lifetime" to "الكل",
        "weekly_overview" to "إحصائيات آخر 7 أيام",
        "daily_avg" to "المتوسط اليومي",
        "vs_last_month" to "مقارنة بالشهر الماضي",
        "increase" to "زيادة",
        "decrease" to "انخفاض",
        "quick_actions" to "إجراءات سريعة",
        "live_notification" to "إشعار السرعة الدائم",
        "floating_bubble" to "الفقاعة العائمة",
        "data_saver_mode" to "وضع توفير البيانات",
        "roaming_active" to "تنبيه: أنت في وضع التجوال (Roaming)",

        // Apps
        "search_apps" to "ابحث عن تطبيق...",
        "filter_all" to "الكل",
        "sort_highest" to "الأعلى استهلاكاً",
        "sort_mobile" to "الأعلى بيانات هاتف",
        "sort_wifi" to "الأعلى واي فاي",
        "foreground" to "في المقدمة",
        "background" to "في الخلفية",
        "block_mobile" to "منع الهاتف",
        "block_wifi" to "منع واي فاي",
        "whitelist" to "استثناء",
        "permission_needed" to "يلزم منح صلاحية وصول إحصائيات الاستخدام",
        "grant_permission" to "منح الصلاحية الآن",
        "permission_desc" to "لعرض استهلاك كل تطبيق بدقة (Foreground vs Background)، يحتاج التطبيق لصلاحية Usage Stats من إعدادات النظام.",

        // Data Saver
        "data_saver_title" to "جدار الحماية وتوفير البيانات",
        "saver_desc" to "تحكم كامل في التطبيقات التي تستهلك بياناتك في الخلفية بدون روت (Local VPN Firewall)",
        "master_switch" to "تفعيل جدار الحماية المحلي",
        "auto_saver" to "تفعيل تلقائي عند استخدام بيانات الهاتف",
        "auto_saver_quota" to "تفعيل تلقائي عند بلوغ 80% من الباقة",
        "system_data_saver" to "توفير بيانات النظام",
        "system_data_saver_desc" to "تقييد وصول البيانات في الخلفية على مستوى النظام",
        "open_system_settings" to "فتح إعدادات النظام",
        "whitelisted_apps" to "التطبيقات المستثناة دائماً",
        "blocked_apps" to "التطبيقات المحظورة",
        "no_blocked_apps" to "لا توجد تطبيقات محظورة حالياً",
        "no_whitelisted_apps" to "لا توجد تطبيقات في القائمة البيضاء",

        // Speed test
        "speedtest_title" to "اختبار سرعة الإنترنت",
        "speedtest_desc" to "قياس سرعة الاتصال الحقيقية وزمن الاستجابة (Ping)",
        "ping" to "زمن الاستجابة",
        "jitter" to "التذبذب",
        "start_test" to "بدء الاختبار",
        "testing" to "جاري الاختبار...",
        "ms" to "مللي ثانية",
        "mbps" to "ميغابت/ثانية",
        "connection" to "نوع الاتصال",

        // Settings
        "settings_title" to "إعدادات الباقة والمراقبة",
        "plan_config" to "تخصيص باقة الإنترنت",
        "monthly_limit" to "حجم الباقة الشهرية (جيجابايت)",
        "renewal_day" to "يوم تجديد الباقة من كل شهر",
        "warning_percent" to "نسبة التنبيه قبل انتهاء الباقة",
        "unlimited_wifi" to "اعتبار شبكات واي فاي غير محدودة",
        "unlimited_wifi_desc" to "استبعاد استهلاك واي فاي من حساب الباقة الشهرية",
        "notifications_title" to "الإشعارات ومؤشرات السرعة",
        "notification_service" to "عرض السرعة في شريط الإشعارات",
        "floating_bubble_setting" to "النافذة العائمة (Floating Speed Bubble)",
        "floating_permission_needed" to "يلزم صلاحية الرسم فوق التطبيقات",
        "battery_optimization_title" to "تحسين البطارية",
        "battery_info" to "تحديث متكيف، إيقاف القراءة تلقائياً عند غلق الشاشة، وعدم استخدام أي مستشعرات.",
        "export_report" to "تصدير تقرير الاستهلاك (CSV)",
        "export_success" to "تم حفظ التقرير بنجاح",
        "language" to "اللغة / Language",
        "save_changes" to "حفظ التغييرات"
    )

    private val englishMap = mapOf(
        "app_title" to "DataPulse",
        "tab_dashboard" to "Dashboard",
        "tab_apps" to "Apps",
        "tab_saver" to "Data Saver",
        "tab_speedtest" to "Speed Test",
        "tab_settings" to "Settings",

        // Dashboard
        "live_speed" to "Live Speed",
        "download" to "Download",
        "upload" to "Upload",
        "today_usage" to "Today's Usage",
        "mobile_data" to "Mobile Data",
        "wifi_data" to "Wi-Fi",
        "quota_status" to "Monthly Data Plan",
        "remaining" to "Remaining",
        "of" to "of",
        "days_left" to "days left in cycle",
        "period_today" to "Today",
        "period_week" to "Week",
        "period_month" to "Month",
        "period_year" to "Year",
        "period_lifetime" to "Lifetime",
        "weekly_overview" to "Last 7 Days Usage",
        "daily_avg" to "Daily Average",
        "vs_last_month" to "vs Last Month",
        "increase" to "increase",
        "decrease" to "decrease",
        "quick_actions" to "Quick Actions",
        "live_notification" to "Status Bar Speed",
        "floating_bubble" to "Floating Bubble",
        "data_saver_mode" to "Data Saver Mode",
        "roaming_active" to "Notice: You are currently Roaming",

        // Apps
        "search_apps" to "Search applications...",
        "filter_all" to "All",
        "sort_highest" to "Highest Total",
        "sort_mobile" to "Highest Mobile",
        "sort_wifi" to "Highest Wi-Fi",
        "foreground" to "Foreground",
        "background" to "Background",
        "block_mobile" to "Block Mobile",
        "block_wifi" to "Block Wi-Fi",
        "whitelist" to "Whitelist",
        "permission_needed" to "Usage Access Permission Required",
        "grant_permission" to "Grant Access Now",
        "permission_desc" to "To inspect foreground vs background data for individual apps, please allow Usage Access in system settings.",

        // Data Saver
        "data_saver_title" to "Data Saver & Firewall",
        "saver_desc" to "Protect your quota by restricting background access per-app with zero root (Local VPN Firewall)",
        "master_switch" to "Enable Local Firewall",
        "auto_saver" to "Auto-enable on Mobile Data",
        "auto_saver_quota" to "Auto-enable when reaching 80% quota",
        "system_data_saver" to "System Data Saver",
        "system_data_saver_desc" to "Restrict background data at system level",
        "open_system_settings" to "Open System Settings",
        "whitelisted_apps" to "Always Whitelisted Apps",
        "blocked_apps" to "Blocked Apps",
        "no_blocked_apps" to "No apps currently blocked",
        "no_whitelisted_apps" to "No whitelisted apps",

        // Speed test
        "speedtest_title" to "Internet Speed Test",
        "speedtest_desc" to "Measure real network latency and bandwidth throughput",
        "ping" to "Ping Latency",
        "jitter" to "Jitter",
        "start_test" to "Run Speed Test",
        "testing" to "Testing in progress...",
        "ms" to "ms",
        "mbps" to "Mbps",
        "connection" to "Connection",

        // Settings
        "settings_title" to "Plan & Preferences",
        "plan_config" to "Monthly Data Plan Setup",
        "monthly_limit" to "Monthly Plan Limit (GB)",
        "renewal_day" to "Plan Renewal Day of Month",
        "warning_percent" to "Warning Alert Threshold (%)",
        "unlimited_wifi" to "Treat Wi-Fi as Unlimited",
        "unlimited_wifi_desc" to "Exclude Wi-Fi data from monthly quota depletion",
        "notifications_title" to "Speed Indicators",
        "notification_service" to "Show Speed in Status Bar",
        "floating_bubble_setting" to "Floating Speed Bubble",
        "floating_permission_needed" to "Requires 'Display over other apps' permission",
        "battery_optimization_title" to "Battery Optimization",
        "battery_info" to "Adaptive intervals, automatic pause on screen off, zero GPS/sensor overhead.",
        "export_report" to "Export Usage Report (CSV)",
        "export_success" to "Report exported successfully",
        "language" to "Language / اللغة",
        "save_changes" to "Save Changes"
    )
}
