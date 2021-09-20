package com.example.livetrackingapp

import com.google.firebase.database.IgnoreExtraProperties
@IgnoreExtraProperties
data class LocationModel(
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0
)

