package com.medicocare.app.data

/**
 * Osnovne kategorije hitnih brojeva (policija/hitna pomoć/vatrogasci/pomoć na putu).
 * Redosled enum vrednosti određuje i podrazumevani redosled prikaza.
 */
enum class EmergencyCategory {
    POLICE,
    AMBULANCE,
    FIRE,
    ROADSIDE
}
