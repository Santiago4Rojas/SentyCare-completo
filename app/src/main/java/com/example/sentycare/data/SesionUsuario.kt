package com.example.sentycare.data

data class SesionUsuario(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val rol: Rol = Rol.ENFERMERA,
    val especialidad: String = "",
    val nivel: String = "",
    val noDoc: String = "",
    val rh: String = ""
) {
    val nombreCompleto: String get() = "$nombre $apellido".trim()
}
