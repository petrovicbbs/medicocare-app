package com.medicocare.app.data

/**
 * Tip učestalosti uzimanja leka.
 */
enum class FrequencyType {
    SVAKI_DAN,        // svakog dana, u zadatim satima (times)
    ODREDJENI_DANI,   // samo određeni dani u nedelji, u zadatim satima (times)
    NA_SVAKIH_X_SATI  // ponavlja se na interval od X sati počevši od startTime
}
