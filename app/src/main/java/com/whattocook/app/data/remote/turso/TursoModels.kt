package com.whattocook.app.data.remote.turso

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class TursoPipelineRequest(
    val requests: List<TursoPipelineStep>,
)

@Serializable
data class TursoPipelineStep(
    val type: String, // "execute" or "close"
    val stmt: TursoStmt? = null,
)

@Serializable
data class TursoStmt(
    val sql: String,
    val args: List<TursoArg> = emptyList(),
)

@Serializable
data class TursoArg(
    val type: String, // "text", "integer", "float", "blob", "null"
    val value: String? = null,
)

@Serializable
data class TursoPipelineResponse(
    val results: List<TursoResultItem> = emptyList(),
)

@Serializable
data class TursoResultItem(
    val type: String, // "ok" or "error"
    val response: TursoExecuteResponse? = null,
    val error: TursoError? = null,
)

@Serializable
data class TursoError(
    val message: String? = null,
    val code: String? = null,
)

@Serializable
data class TursoExecuteResponse(
    val type: String = "execute",
    val result: TursoResultData? = null,
)

@Serializable
data class TursoResultData(
    val cols: List<TursoCol> = emptyList(),
    val rows: List<List<JsonElement>> = emptyList(),
    val affected_row_count: Int? = null,
)

@Serializable
data class TursoCol(
    val name: String,
)

data class TursoUser(
    val id: String,
    val email: String,
)

/** Helper to cleanly extract string value whether it's a tagged JSON object or raw primitive */
fun JsonElement.extractString(): String? {
    return when (this) {
        is JsonPrimitive -> if (this.isString) content else contentOrNull
        is JsonObject -> {
            val v = this["value"]
            if (v is JsonPrimitive) v.contentOrNull else null
        }
        else -> null
    }
}
