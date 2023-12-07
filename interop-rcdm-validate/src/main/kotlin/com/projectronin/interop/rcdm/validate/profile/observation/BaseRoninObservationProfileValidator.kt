package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.rcdm.validate.profile.ProfileValidator
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

/**
 * Base validator for all Ronin Observation profiles.
 */
abstract class BaseRoninObservationProfileValidator(protected val registryClient: NormalizationRegistryClient) :
    ProfileValidator<Observation>() {
    override val supportedResource: KClass<Observation> = Observation::class
    override val r4Validator: R4ProfileValidator<Observation> = R4ObservationValidator

    /**
     * Validates the details for the specific observation. This does not need to include any details that are validated generically for all Observations.
     */
    abstract fun validateSpecificObservation(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation
    )

    /**
     * The List of supported categories by this profile.
     */
    open fun getSupportedCategories(): List<Coding> = emptyList()

    /**
     * The List of supported codes by this profile.
     */
    open fun getSupportedValueSet(): ValueSetList =
        registryClient.getRequiredValueSet("Observation.code", profile.value)

    private val requiredSubjectError = RequiredFieldError(Observation::subject)

    private val requiredExtensionCodeError = FHIRError(
        code = "RONIN_OBS_004",
        description = "Tenant source observation code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::extension)
    )

    private val requiredExtensionValueError = FHIRError(
        code = "RONIN_OBS_005",
        description = "Tenant source observation value extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(Observation::extension)
    )
    private val requiredComponentExtensionCodeError = FHIRError(
        code = "RONIN_OBS_006",
        description = "Tenant source observation component code extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ObservationComponent::extension)
    )

    private val requiredComponentExtensionValueError = FHIRError(
        code = "RONIN_OBS_007",
        description = "Tenant source observation component value extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(ObservationComponent::extension)
    )

    override fun validate(resource: Observation, validation: Validation, context: LocationContext) {
        validation.apply {
            validateRoninNormalizedCodeableConcept(resource.code, Observation::code, context, this)

            checkNotNull(resource.subject, requiredSubjectError, context)

            val supportedCategories = getSupportedCategories()
            if (supportedCategories.isNotEmpty()) {
                checkTrue(
                    resource.category.qualifiesForValueSet(supportedCategories),
                    FHIRError(
                        code = "RONIN_OBS_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${supportedCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }}",
                        location = LocationContext(Observation::category)
                    ),
                    context
                )
            }

            resource.code?.let { code ->
                val supportedValueSet = getSupportedValueSet()
                val supportedCodes = supportedValueSet.codes
                if (supportedCodes.isNotEmpty()) {
                    checkTrue(
                        code.qualifiesForValueSet(supportedCodes),
                        FHIRError(
                            code = "RONIN_OBS_003",
                            severity = ValidationIssueSeverity.ERROR,
                            description = "Must match this system|code: ${supportedCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }}",
                            location = LocationContext(Observation::code),
                            metadata = supportedValueSet.metadata?.let { listOf(it) } ?: emptyList()
                        ),
                        context
                    )
                }
            }

            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredExtensionCodeError,
                context
            )
            if (resource.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                checkTrue(
                    resource.extension.any {
                        it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri &&
                            it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                    },
                    requiredExtensionValueError,
                    context
                )
            }

            resource.component.forEachIndexed { index, observationComponent ->
                val componentContext = context.append(LocationContext("", "component[$index]"))
                checkTrue(
                    observationComponent.extension.any {
                        it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri &&
                            it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                    },
                    requiredComponentExtensionCodeError,
                    componentContext
                )
                if (observationComponent.value?.type == DynamicValueType.CODEABLE_CONCEPT) {
                    checkTrue(
                        observationComponent.extension.any {
                            it.url == RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri &&
                                it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                        },
                        requiredComponentExtensionValueError,
                        componentContext
                    )
                }
            }

            validateReferenceType(
                resource.subject,
                listOf(ResourceType.Patient),
                LocationContext(Observation::subject),
                this
            )

            validateSpecificObservation(resource, context, this)
        }
    }
}
