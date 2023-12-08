package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.resource.DomainResource
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.InvalidReferenceType
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.getIdentifiersOrNull
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

abstract class ProfileValidator<R : Resource<R>> {
    protected val logger = KotlinLogging.logger(this::class.java.name)

    abstract val supportedResource: KClass<R>
    abstract val r4Validator: R4ProfileValidator<R>
    abstract val profile: RoninProfile
    abstract val rcdmVersion: RCDMVersion
    abstract val profileVersion: Int

    protected abstract fun validate(
        resource: R,
        validation: Validation,
        context: LocationContext,
    )

    /**
     * Returns true if [resource] qualifies for this particular profile.
     */
    fun qualifies(resource: R): Boolean {
        return resource.meta?.profile?.contains(profile.canonical) == true
    }

    private val requiredId = RequiredFieldError(LocationContext("", "id"))
    private val requiredMeta = RequiredFieldError(LocationContext("", "meta"))

    private val containedResourcePresentWarning =
        FHIRError(
            code = "RONIN_CONTAINED_RESOURCE",
            severity = ValidationIssueSeverity.WARNING,
            description = "There is a Contained Resource present",
            location = LocationContext("", "contained"),
        )

    /**
     * Validates the [resource].
     */
    fun validate(
        resource: R,
        locationContext: LocationContext,
    ): Validation =
        Validation().apply {
            checkNotNull(resource.id, requiredId, locationContext)
            checkNotNull(resource.meta, requiredMeta, locationContext)

            validateIdentifiers(resource, locationContext, this)
            validateContained(resource, locationContext, this)

            validate(resource, this, locationContext)
        }

    protected fun validateRoninNormalizedCodeableConcept(
        codeableConcept: CodeableConcept?,
        // CodeableConcepts could appear as a single element or part of a List.
        propertyCodeableConcept: KProperty1<R, CodeableConcept?>? = null,
        propertyCodeableConceptList: KProperty1<R, List<CodeableConcept?>>? = null,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val property = propertyCodeableConcept ?: propertyCodeableConceptList
        validation.apply {
            codeableConcept?.let {
                val coding = codeableConcept.coding.singleOrNull()
                checkNotNull(
                    coding,
                    FHIRError(
                        code = "RONIN_NOV_CODING_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must contain exactly 1 coding",
                        location = LocationContext("", "${property?.name}.coding"),
                    ),
                    parentContext,
                )
                ifNotNull(coding) {
                    checkTrue(
                        !coding.system?.value.isNullOrBlank(),
                        FHIRError(
                            code = "RONIN_NOV_CODING_003",
                            severity = ValidationIssueSeverity.ERROR,
                            description = "Coding system cannot be null or blank",
                            location = LocationContext("", "${property?.name}.coding[0].system"),
                        ),
                        parentContext,
                    )
                    checkTrue(
                        !coding.code?.value.isNullOrBlank(),
                        FHIRError(
                            code = "RONIN_NOV_CODING_004",
                            severity = ValidationIssueSeverity.ERROR,
                            description = "Coding code cannot be null or blank",
                            location = LocationContext("", "${property?.name}.coding[0].code"),
                        ),
                        parentContext,
                    )
                    checkTrue(
                        !coding.display?.value.isNullOrBlank(),
                        FHIRError(
                            code = "RONIN_NOV_CODING_005",
                            severity = ValidationIssueSeverity.ERROR,
                            description = "Coding display cannot be null or blank",
                            location = LocationContext("", "${property?.name}.coding[0].display"),
                        ),
                        parentContext,
                    )
                }
            }
        }
    }

    protected fun validateReferenceType(
        reference: Reference?,
        resourceTypesList: List<ResourceType>,
        context: LocationContext,
        validation: Validation,
        containedResource: List<Resource<*>>? = listOf(),
    ) {
        val resourceTypesStringList = resourceTypesList.map { it.name }
        val requiredContainedResource =
            FHIRError(
                code = "RONIN_REQ_REF_1",
                severity = ValidationIssueSeverity.ERROR,
                description = "Contained resource is required if a local reference is provided",
                location = LocationContext(Reference::reference),
            )

        validation.apply {
            reference?.let {
                if (it.reference?.value.toString().startsWith("#")) {
                    val id = it.reference?.value.toString().substringAfter("#")
                    if (containedResource.isNullOrEmpty()) {
                        checkTrue(
                            false,
                            requiredContainedResource,
                            context,
                        )
                    } else {
                        checkTrue(
                            resourceTypesStringList.contains(containedResource.find { r -> r.id?.value == id }?.resourceType),
                            InvalidReferenceType(Reference::reference, resourceTypesList),
                            context,
                        )
                    }
                } else {
                    checkTrue(
                        reference.isInTypeList(resourceTypesStringList),
                        InvalidReferenceType(Reference::reference, resourceTypesList),
                        context,
                    )
                }
            }
        }
    }

    private fun Reference?.isInTypeList(resourceTypeList: List<String>): Boolean {
        this?.let { reference ->
            resourceTypeList.forEach { value ->
                if (reference.isForType(value)) {
                    return true
                }
            }
        }
        return false
    }

    private fun validateContained(
        resource: R,
        locationContext: LocationContext,
        validation: Validation,
    ) {
        if (resource is DomainResource<*>) {
            val contained = resource.contained

            validation.checkTrue(contained.isEmpty(), containedResourcePresentWarning, locationContext)
            if (contained.isNotEmpty()) {
                logger.warn { "contained resource found @ $locationContext" }
            }
        }
    }

    private fun validateIdentifiers(
        resource: R,
        locationContext: LocationContext,
        validation: Validation,
    ) {
        val identifiers = resource.getIdentifiersOrNull()
        identifiers?.let {
            validateTenantIdentifier(identifiers, locationContext, validation)
            validateFhirIdentifier(identifiers, locationContext, validation)
            validateDataAuthorityIdentifier(identifiers, locationContext, validation)
        }
    }

    private val requiredTenantIdentifierError =
        FHIRError(
            code = "RONIN_TNNT_ID_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "Tenant identifier is required",
            location = LocationContext("", "identifier"),
        )
    private val wrongTenantIdentifierTypeError =
        FHIRError(
            code = "RONIN_TNNT_ID_002",
            severity = ValidationIssueSeverity.ERROR,
            description = "Tenant identifier provided without proper CodeableConcept defined",
            location = LocationContext("", "identifier"),
        )
    private val requiredTenantIdentifierValueError =
        FHIRError(
            code = "RONIN_TNNT_ID_003",
            severity = ValidationIssueSeverity.ERROR,
            description = "Tenant identifier value is required",
            location = LocationContext("", "identifier"),
        )

    private fun validateTenantIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val tenantIdentifier = identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        validation.apply {
            checkNotNull(tenantIdentifier, requiredTenantIdentifierError, parentContext)
            ifNotNull(tenantIdentifier) {
                checkTrue(
                    tenantIdentifier.type == CodeableConcepts.RONIN_TENANT,
                    wrongTenantIdentifierTypeError,
                    parentContext,
                )
                checkNotNull(tenantIdentifier.value, requiredTenantIdentifierValueError, parentContext)
            }
        }
    }

    private val requiredFhirIdentifierError =
        FHIRError(
            code = "RONIN_FHIR_ID_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "FHIR identifier is required",
            location = LocationContext("", "identifier"),
        )
    private val wrongFhirIdentifierTypeError =
        FHIRError(
            code = "RONIN_FHIR_ID_002",
            severity = ValidationIssueSeverity.ERROR,
            description = "FHIR identifier provided without proper CodeableConcept defined",
            location = LocationContext("", "identifier"),
        )
    private val requiredFhirIdentifierValueError =
        FHIRError(
            code = "RONIN_FHIR_ID_003",
            severity = ValidationIssueSeverity.ERROR,
            description = "FHIR identifier value is required",
            location = LocationContext("", "identifier"),
        )

    private fun validateFhirIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val fhirIdentifier = identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }
        validation.apply {
            checkNotNull(fhirIdentifier, requiredFhirIdentifierError, parentContext)
            ifNotNull(fhirIdentifier) {
                // tenantIdentifier.use is constrained by the IdentifierUse enum type, so it needs no validation.
                checkTrue(
                    fhirIdentifier.type == CodeableConcepts.RONIN_FHIR_ID,
                    wrongFhirIdentifierTypeError,
                    parentContext,
                )
                checkNotNull(fhirIdentifier.value, requiredFhirIdentifierValueError, parentContext)
            }
        }
    }

    private val requiredDataAuthorityIdentifierError =
        FHIRError(
            code = "RONIN_DAUTH_ID_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "Data Authority identifier is required",
            location = LocationContext("", "identifier"),
        )
    private val wrongDataAuthorityIdentifierTypeError =
        FHIRError(
            code = "RONIN_DAUTH_ID_002",
            severity = ValidationIssueSeverity.ERROR,
            description = "Data Authority identifier provided without proper CodeableConcept defined",
            location = LocationContext("", "identifier"),
        )
    private val requiredDataAuthorityIdentifierValueError =
        FHIRError(
            code = "RONIN_DAUTH_ID_003",
            severity = ValidationIssueSeverity.ERROR,
            description = "Data Authority identifier value is required",
            location = LocationContext("", "identifier"),
        )

    private fun validateDataAuthorityIdentifier(
        identifier: List<Identifier>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val dataAuthorityIdentifier = identifier.find { it.system == CodeSystem.RONIN_DATA_AUTHORITY.uri }
        validation.apply {
            checkNotNull(dataAuthorityIdentifier, requiredDataAuthorityIdentifierError, parentContext)
            ifNotNull(dataAuthorityIdentifier) {
                checkTrue(
                    dataAuthorityIdentifier.type == CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    wrongDataAuthorityIdentifierTypeError,
                    parentContext,
                )

                checkNotNull(dataAuthorityIdentifier.value, requiredDataAuthorityIdentifierValueError, parentContext)
            }
        }
    }
}
