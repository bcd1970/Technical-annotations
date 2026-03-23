package com.bcd.technotes.core.serialization

import com.bcd.technotes.core.model.Project
import kotlinx.serialization.json.Json

val projectJson = Json {
    prettyPrint = true
    classDiscriminator = "type"
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun Project.toJson(): String = projectJson.encodeToString(Project.serializer(), this)

fun String.toProject(): Project = projectJson.decodeFromString(Project.serializer(), this)
