package com.example.jadwalharian // Ganti dengan nama paket Anda

import java.io.Serializable

data class Task(
    val id: Long,
    val title: String,
    val timestamp: Long,
    val soundUri: String? = null
) : Serializable