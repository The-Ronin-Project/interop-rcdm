package com.projectronin.interop.rcdm.registry.model

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Access to common Ronin-specific code systems
 */
enum class RoninConceptMap(uriString: String) {
    CODE_SYSTEMS("http://projectronin.io/fhir/CodeSystem");

    val uri = Uri(uriString)

    /**
     * Create a [Coding] for input to a Concept Map Registry request,
     * when the incoming data provides only a Code, with a string value, and no Coding.
     * The method derives the correct Coding.system and assigns the Code to Coding.code.
     */
    fun toCoding(tenantMnemonic: String, fhirPath: String, value: String) =
        Coding(system = this.toUri(tenantMnemonic, fhirPath), code = Code(value = value))

    /**
     * Create a [Uri] for the Coding.system input to a Concept Map Registry request,
     * when the incoming data provides only a Code value with no system value.
     */
    fun toUri(tenantMnemonic: String, fhirPath: String) =
        Uri(this.toUriString(tenantMnemonic, fhirPath))

    /**
     * Compose the string value for the Coding.system input to a Concept Map Registry request,
     * when the incoming data provides only a Code value with no system.
     */
    fun toUriString(tenantMnemonic: String, fhirPath: String) =
        "${this.uri.value}/$tenantMnemonic/${fhirPath.toUriName()}"

    private val uriRegex = Regex("${Regex.escape(uri.value!!)}/(\\w+)/([A-Za-z]+)")

    /**
     * Determines if the supplied URI could be considered a valid URI for this concept map.
     */
    fun isMappedUri(uri: String): Boolean = uriRegex.matches(uri)

    /**
     * Determines the tenant-agnostic version of the supplied URI for this concept map. If the supplied URI is not valid
     * for this concept map, it will be returned as-is.
     */
    fun toTenantAgnosticUri(uri: String): String {
        val result = uriRegex.matchEntire(uri) ?: return uri
        val (_, path) = result.destructured
        return "${this.uri.value}/$path"
    }

    /**
     * Parse the concept map URL path suffix - such as "ContactPointUse" -
     * from a dot-separated FHIR field path - such as "ContactPoint.use"
     */
    private fun String.toUriName() =
        this.split(".").filter { it.length > 1 }.map { field ->
            "${field[0].uppercase()}${field.substring(1)}"
        }.joinToString("")
}
