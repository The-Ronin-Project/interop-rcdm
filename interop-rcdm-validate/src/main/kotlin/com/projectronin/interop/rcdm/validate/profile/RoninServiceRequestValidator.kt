package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4ServiceRequestValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninServiceRequestValidator : ProfileValidator<ServiceRequest>() {
    override val supportedResource: KClass<ServiceRequest> = ServiceRequest::class
    override val r4Validator: R4ProfileValidator<ServiceRequest> = R4ServiceRequestValidator
    override val profile = RoninProfile.SERVICE_REQUEST
    override val rcdmVersion = RCDMVersion.V3_27_0
    override val profileVersion = 1

    private val requiredSubjectError = RequiredFieldError(ServiceRequest::subject)
    private val minimumExtensionError =
        FHIRError(
            code = "RONIN_SERVREQ_001",
            description = "Service Request must have at least two extensions",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(ServiceRequest::extension),
        )
    private val invalidTenantSourceServiceRequestCategoryError =
        FHIRError(
            code = "RONIN_SERVREQ_002",
            description = "Service Request extension Tenant Source Service Request Category is invalid",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(ServiceRequest::extension),
        )
    private val invalidTenantSourceServiceRequestCodeError =
        FHIRError(
            code = "RONIN_SERVREQ_003",
            description = "Service Request extension Tenant Source Service Request Code is invalid",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(ServiceRequest::extension),
        )
    private val invalidCategorySizeError =
        FHIRError(
            code = "RONIN_SERVREQ_004",
            description = "Service Request requires exactly 1 Category element",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(ServiceRequest::category),
        )

    override fun validate(
        resource: ServiceRequest,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            // Must have at least 2 extensions
            checkTrue(resource.extension.size >= 2, minimumExtensionError, context)

            // Check for a valid category.
            checkTrue(resource.category.size == 1, invalidCategorySizeError, context)
            validateRoninNormalizedCodeableConcept(
                resource.category.firstOrNull(),
                null,
                ServiceRequest::category,
                context,
                this,
            )

            checkTrue(
                resource.extension.any {
                    it.url?.value == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri.value &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                invalidTenantSourceServiceRequestCategoryError,
                context,
            )

            // Check the code field (R4 Requires a code).
            checkTrue(
                resource.extension.count {
                    it.url?.value == RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri.value &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                } == 1,
                invalidTenantSourceServiceRequestCodeError,
                context,
            )

            checkNotNull(resource.subject, requiredSubjectError, context)
            validateReferenceType(
                resource.subject,
                listOf(ResourceType.Patient),
                LocationContext(ServiceRequest::subject),
                validation,
            )
        }
    }
}
