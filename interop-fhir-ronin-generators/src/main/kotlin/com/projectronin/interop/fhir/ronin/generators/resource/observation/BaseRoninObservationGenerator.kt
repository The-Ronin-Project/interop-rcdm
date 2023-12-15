package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.dateTime
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.ObservationGenerator
import com.projectronin.interop.fhir.generators.resources.observation
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.referenceData
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.includeExtensions
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile

/**
 * Base ronin observation profile, applies generic category and code for profile
 */
fun rcdmObservation(
    tenant: String,
    block: ObservationGenerator.() -> Unit,
): Observation {
    return rcdmBaseObservation(tenant) {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.OBSERVATION, tenant) {}
        category of
            listOf(
                codeableConcept {
                    coding of observationCategory
                },
            )
        code of generateCodeableConcept(code.generate(), observationCode.random())
        subject of generateReference(subject.generate(), subjectBaseReferenceOptions, tenant, "Patient")
    }
}

/**
 * id and identifiers for ronin observation profiles
 */
fun rcdmBaseObservation(
    tenant: String,
    block: ObservationGenerator.() -> Unit,
): Observation {
    return observation {
        block.invoke(this)
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)

            val extensionsToAdd =
                if (value.generate()?.type == DynamicValueType.CODEABLE_CONCEPT) {
                    listOf(tenantSourceObservationCodeExtension, tenantSourceObservationValueExtension)
                } else {
                    listOf(tenantSourceObservationCodeExtension)
                }
            extension of includeExtensions(extension.generate(), extensionsToAdd)

            val updatedComponents =
                component.generate().map { component ->
                    val componentExtensionsToAdd =
                        if (component.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                            listOf(
                                tenantSourceObservationComponentCodeExtension,
                                tenantSourceObservationComponentValueExtension,
                            )
                        } else {
                            listOf(tenantSourceObservationComponentCodeExtension)
                        }

                    component.copy(extension = includeExtensions(component.extension, componentExtensionsToAdd))
                }
            component of updatedComponents
        }
    }
}

val loinc = "http://loinc.org"

fun Patient.rcdmObservation(block: ObservationGenerator.() -> Unit): Observation {
    val data = this.referenceData()
    return rcdmObservation(data.tenantId) {
        block.invoke(this)
        subject of
            generateReference(
                subject.generate(),
                subjectBaseReferenceOptions,
                data.tenantId,
                "Patient",
                data.udpId,
            )
    }
}

private val observationCode =
    listOf(
        coding {
            system of CodeSystem.SNOMED_CT.uri
            code of Code("160695008")
            display of "Transport too expensive"
        },
    )

val vitalSignsCategory =
    listOf(
        coding {
            system of CodeSystem.OBSERVATION_CATEGORY.uri
            code of Code("vital-signs")
        },
    )

val observationCategory =
    listOf(
        coding {
            system of CodeSystem.OBSERVATION_CATEGORY.uri
            code of Code("social-history")
        },
    )

val subjectBaseReferenceOptions =
    listOf(
        "Patient",
    )

val subjectStagingReferenceOptions =
    listOf(
        "Patient",
    )

val subjectReferenceOptions =
    listOf(
        "Patient",
    )

val possibleDateTime = DynamicValue(DynamicValueType.DATE_TIME, dateTime { })

val tenantSourceObservationCodeExtension =
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = "tenant-source-extension".asFHIR(),
                    coding =
                        listOf(
                            Coding(
                                code = Code("tenant-source-code-extension"),
                            ),
                        ),
                ),
            ),
    )
val tenantSourceExtension = "tenant-source-value-extension"
val tenantSourceObservationValueExtension =
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = tenantSourceExtension.asFHIR(),
                    coding =
                        listOf(
                            Coding(
                                code = Code(tenantSourceExtension),
                            ),
                        ),
                ),
            ),
    )
val tenantSourceObservationComponentCodeExtension =
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = "tenant-source-extension".asFHIR(),
                    coding =
                        listOf(
                            Coding(
                                code = Code("tenant-source-component-code-extension"),
                            ),
                        ),
                ),
            ),
    )
val tenantSourceObservationComponentValueExtension =
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = tenantSourceExtension.asFHIR(),
                    coding =
                        listOf(
                            Coding(
                                code = Code("tenant-source-component-value-extension"),
                            ),
                        ),
                ),
            ),
    )
