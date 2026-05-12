package com.example.sentycare.data

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val rol: String = "",
    val especialidad: String = "",
    val nivel: String = "",
    val activo: Boolean = true,
    val creadoEn: Long = 0L
)

enum class Rol(val displayName: String, val descripcion: String) {
    ADMIN("Administrador", "Gestiona usuarios y configuración"),
    JEFE_UNIDAD("Jefe de Unidad", "Supervisa toda la UCI"),
    MEDICO_ESPECIALISTA("Médico Especialista", "Médico con especialidad en UCI pediátrica"),
    MEDICO_RESIDENTE("Médico Residente", "Médico en formación"),
    ENFERMERA("Enfermera/o", "Personal de enfermería UCI");

    companion object {
        fun fromString(value: String): Rol =
            entries.firstOrNull { it.name == value } ?: ENFERMERA
    }
}
