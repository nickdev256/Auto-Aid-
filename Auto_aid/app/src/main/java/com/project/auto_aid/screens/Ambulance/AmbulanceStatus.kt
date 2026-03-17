package com.project.auto_aid.screens.ambulance

enum class AmbulanceStatus {

    REQUEST_SENT,        // User has requested an ambulance

    DRIVER_ASSIGNED,     // Ambulance driver assigned to the request

    AMBULANCE_ON_THE_WAY,// Ambulance is driving to the patient

    ARRIVED,             // Ambulance has reached the patient

    PATIENT_PICKED,      // Patient picked and going to hospital

    AT_HOSPITAL,         // Patient delivered to hospital

    COMPLETED,           // Service completed successfully

    CANCELLED            // Request cancelled
}