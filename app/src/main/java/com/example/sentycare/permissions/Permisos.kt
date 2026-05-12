package com.example.sentycare.permissions

import com.example.sentycare.data.Rol

object Permisos {

    // Admisión inicial: todos los roles clínicos (incluyendo ENFERMERA)
    fun puedeRegistrarPaciente(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE, Rol.ENFERMERA
    )

    // Dar de alta: criterio médico — solo MEDICO_ESPECIALISTA
    fun puedeDarDeAlta(rol: Rol) = rol == Rol.MEDICO_ESPECIALISTA

    // Realizar evaluaciones: todos los roles clínicos
    fun puedeRealizarEvaluacion(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE, Rol.ENFERMERA
    )

    // Recomendación médica: solo MED_ESP y MED_RES (no JEFE, no ENFERMERA)
    fun puedeAgregarRecomendacionMedico(rol: Rol) = rol in listOf(
        Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE
    )

    // Ver historial: todos los roles clínicos
    fun puedeVerHistorial(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE, Rol.ENFERMERA
    )

    // Gestionar usuarios y panel admin: solo ADMIN
    fun puedeGestionarUsuarios(rol: Rol) = rol == Rol.ADMIN
}
