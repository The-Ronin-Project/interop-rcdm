package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidValueSetError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.validate.error.RoninInvalidDynamicValueError

/**
 * Base validator for all Ronin Vital Sign profiles.
 */
abstract class BaseRoninVitalSignProfileValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninObservationProfileValidator(registryClient) {
    open val acceptedValueTypes: List<DynamicValueType> = listOf(DynamicValueType.QUANTITY)

    override fun getSupportedCategories(): List<Coding> =
        listOf(Coding(system = CodeSystem.OBSERVATION_CATEGORY.uri, code = Code("vital-signs")))

    /**
     * Validates the details for the specific vital sign. This does not need to include any details that are validated generically for all Vital Signs.
     */
    abstract fun validateVitalSign(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    )

    private val acceptedEffectiveTypes =
        listOf(
            DynamicValueType.DATE_TIME,
            DynamicValueType.PERIOD,
        )

    override fun validateSpecificObservation(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        validation.apply {
            resource.value?.let { value ->
                checkTrue(
                    acceptedValueTypes.contains(value.type),
                    RoninInvalidDynamicValueError(Observation::value, acceptedValueTypes, profile.value),
                    parentContext,
                )
            }

            resource.effective?.let { data ->
                checkTrue(
                    acceptedEffectiveTypes.contains(data.type),
                    RoninInvalidDynamicValueError(
                        Observation::effective,
                        acceptedEffectiveTypes,
                        profile.value,
                    ),
                    parentContext,
                )
            }

            validateVitalSign(resource, parentContext, this)
        }
    }

    private val requiredQuantityValueError = RequiredFieldError(LocationContext(Quantity::value))
    private val requiredQuantityUnitError = RequiredFieldError(LocationContext(Quantity::unit))
    private val requiredQuantityCodeError = RequiredFieldError(LocationContext(Quantity::code))

    private val invalidQuantitySystemError =
        FHIRError(
            code = "USCORE_VSOBS_002",
            severity = ValidationIssueSeverity.ERROR,
            description = "Quantity system must be UCUM",
            location = LocationContext(Quantity::system),
        )

    /**
     * Validates the dynamic [value] against Ronin rules for vital sign quantities.
     * Checks that the value includes a value and unit, and that the value system is UCUM.
     * Checks that the quantity's coded unit value is in the supplied [validUnitCodeList].
     */
    protected fun validateVitalSignValue(
        value: DynamicValue<Any>?,
        validUnitCodeList: List<String>,
        validation: Validation,
        parentContext: LocationContext = LocationContext(Observation::value),
    ) {
        validation.apply {
            value?.let {
                if (value.type == DynamicValueType.QUANTITY) {
                    val quantity = value.value as Quantity

                    val quantityContext = LocationContext(parentContext.element, "${parentContext.field}Quantity")
                    checkNotNull(quantity.value, requiredQuantityValueError, quantityContext)
                    checkNotNull(quantity.unit, requiredQuantityUnitError, quantityContext)

                    // The presence of a code requires a system, so we're bypassing the check here.
                    ifNotNull(quantity.system) {
                        checkTrue(
                            quantity.system == CodeSystem.UCUM.uri,
                            invalidQuantitySystemError,
                            quantityContext,
                        )
                    }

                    val quantityCode = quantity.code
                    checkNotNull(quantityCode, requiredQuantityCodeError, quantityContext)
                    ifNotNull(quantityCode) {
                        checkTrue(
                            validUnitCodeList.contains(quantityCode.value),
                            InvalidValueSetError(
                                LocationContext(Quantity::code),
                                quantityCode.value ?: "",
                            ),
                            quantityContext,
                        )
                    }
                }
            }
        }
    }
}
