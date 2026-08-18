package com.medicocare.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toFrequencyType(value: String): FrequencyType = FrequencyType.valueOf(value)

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name

    @TypeConverter
    fun toIntakeStatus(value: String): IntakeStatus = IntakeStatus.valueOf(value)

    @TypeConverter
    fun fromIntakeStatus(value: IntakeStatus): String = value.name

    @TypeConverter
    fun toVitalType(value: String): VitalType = VitalType.valueOf(value)

    @TypeConverter
    fun fromVitalType(value: VitalType): String = value.name

    @TypeConverter
    fun toMedicationCategory(value: String): MedicationCategory = MedicationCategory.valueOf(value)

    @TypeConverter
    fun fromMedicationCategory(value: MedicationCategory): String = value.name

    @TypeConverter
    fun toEmergencyCategory(value: String?): EmergencyCategory? = value?.let { EmergencyCategory.valueOf(it) }

    @TypeConverter
    fun fromEmergencyCategory(value: EmergencyCategory?): String? = value?.name
}
