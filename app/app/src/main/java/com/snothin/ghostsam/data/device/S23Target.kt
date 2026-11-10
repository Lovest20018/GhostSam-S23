package com.snothin.ghostsam.data.device

/** Device scope only. Firmware changes do not establish or rule out runtime compatibility. */
internal object S23Target {
    const val MODEL = "SM-S9110"
    const val KMI = "android13-5.15"
    const val MIN_SDK = 36

    fun matches(model: String, sdk: Int): Boolean =
        model == MODEL && sdk >= MIN_SDK
}
