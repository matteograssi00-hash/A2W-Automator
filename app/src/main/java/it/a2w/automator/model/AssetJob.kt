package it.a2w.automator.model

data class AssetJob(
    val asset: String,
    val value: String,
    var status: String = "DA FARE",
    var note: String = ""
)