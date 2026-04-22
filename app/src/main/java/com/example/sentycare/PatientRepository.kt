package com.example.sentycare

object PatientRepository {
    private val _patients = mutableListOf<Patient>()
    val patients: List<Patient> get() = _patients

    fun addPatient(patient: Patient) {
        _patients.add(patient)
    }
}