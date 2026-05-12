package com.example.sentycare.permissions

import com.example.sentycare.data.Rol

object Permisos {

    fun puedeRegistrarPaciente(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE
    )

    fun puedeDarDeAlta(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE
    )

    fun puedeRealizarEvaluacion(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE, Rol.ENFERMERA
    )

    fun puedeAgregarRecomendacionMedico(rol: Rol) = rol in listOf(
        Rol.JEFE_UNIDAD, Rol.MEDICO_ESPECIALISTA, Rol.MEDICO_RESIDENTE
    )

    fun puedeGestionarUsuarios(rol: Rol) = rol == Rol.ADMIN
}
