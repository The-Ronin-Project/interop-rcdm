package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.validate.error.RoninInvalidDynamicValueError
import com.projectronin.interop.rcdm.validate.util.isNullOrDayFormat
import org.springframework.stereotype.Component

@Component
class RoninLaboratoryResultValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_LABORATORY_RESULT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_29_0
    override val profileVersion: Int = 4

    override fun getSupportedCategories(): List<Coding> =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("laboratory")))

    private val acceptedEffectiveTypes = listOf(
        DynamicValueType.DATE_TIME,
        DynamicValueType.PERIOD
    )

    private val requiredCategoryError = RequiredFieldError(Observation::category)
    private val invalidQuantitySystemError = FHIRError(
        code = "USCORE_LABOBS_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "Quantity system must be UCUM",
        location = LocationContext("Observation", "valueQuantity.system")
    )
    private val noChildValueOrDataAbsentReasonError = FHIRError(
        code = "USCORE_LABOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "If there is no component or hasMember element then either a value[x] or a data absent reason must be present",
        location = LocationContext(Observation::class)
    )
    private val invalidDateTimeError = FHIRError(
        code = "USCORE_LABOBS_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Datetime must be at least to day",
        location = LocationContext(Observation::effective)
    )
    private val invalidCodeSystemError = FHIRError(
        code = "RONIN_LABOBS_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Code system must be LOINC",
        location = LocationContext(Observation::code)
    )

    private val invalidCodedValueSystemError = FHIRError(
        code = "RONIN_LABOBS_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Value code system must be SNOMED CT",
        location = LocationContext(Observation::value)
    )

    override fun validateSpecificObservation(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation
    ) {
        validation.apply {
            resource.code?.coding?.let { coding ->
                checkTrue(
                    coding.all { it.system == CodeSystem.LOINC.uri },
                    invalidCodeSystemError,
                    parentContext
                )
            }

            checkTrue(resource.category.isNotEmpty(), requiredCategoryError, parentContext)

            if (resource.value?.type == DynamicValueType.QUANTITY) {
                val quantity = resource.value!!.value as Quantity

                // The presence of a code requires a system, so we're bypassing the check here.
                ifNotNull(quantity.system) {
                    checkTrue(quantity.system == CodeSystem.UCUM.uri, invalidQuantitySystemError, parentContext)
                }
            }

            if (resource.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                val quantity = resource.value!!.value as CodeableConcept

                // The presence of a code requires a system, so we're bypassing the check here.
                ifNotNull(quantity.coding) {
                    checkTrue(
                        quantity.coding.none { it.system?.value != CodeSystem.SNOMED_CT.uri.value },
                        invalidCodedValueSystemError,
                        parentContext
                    )
                }
            }

            if (resource.component.isEmpty() && resource.hasMember.isEmpty()) {
                checkTrue(
                    (resource.value != null || resource.dataAbsentReason != null),
                    noChildValueOrDataAbsentReasonError,
                    parentContext
                )
            }

            resource.effective?.let { effective ->
                if (effective.type == DynamicValueType.DATE_TIME) {
                    val dateTime = effective.value as? DateTime
                    checkTrue(dateTime?.value.isNullOrDayFormat(), invalidDateTimeError, parentContext)
                }
                checkTrue(
                    acceptedEffectiveTypes.contains(effective.type),
                    RoninInvalidDynamicValueError(
                        Observation::effective,
                        acceptedEffectiveTypes,
                        profile.value
                    ),
                    parentContext
                )
            }
        }
    }
}
