package com.example.sentycare

data class Patient(
    val id: String = "",
    val activo: Boolean = true,

    val nombre: String = "",
    val apellido: String = "",
    val genero: String = "",
    val noDoc: String = "",
    val fechaNacimiento: String = "",
    val rh: String = "",
    val numeroCama: String = "",
    val diagnostico: String = "",

    val acudienteNombre: String = "",
    val acudienteCedula: String = "",
    val acudienteTelefono: String = "",
    val acudienteParentesco: String = "",

    val registradoPorId: String = "",
    val registradoPorNombre: String = "",
    val registradoPorEspecialidad: String = "",
    val registradoEn: Long = 0L
)