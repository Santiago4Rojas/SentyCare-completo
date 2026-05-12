package com.example.sentycare

import com.example.sentycare.data.Rol
import com.example.sentycare.data.SesionUsuario

object SesionState {
    var usuario: SesionUsuario = SesionUsuario()

    val rol: Rol get() = usuario.rol

    fun limpiar() {
        usuario = SesionUsuario()
    }
}
