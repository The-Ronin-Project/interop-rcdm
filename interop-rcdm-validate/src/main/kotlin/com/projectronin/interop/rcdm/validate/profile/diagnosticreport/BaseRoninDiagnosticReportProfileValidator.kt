package com.projectronin.interop.rcdm.validate.profile.diagnosticreport

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.validate.profile.ProfileValidator
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

abstract class BaseRoninDiagnosticReportProfileValidator : ProfileValidator<DiagnosticReport>() {
    override val supportedResource: KClass<DiagnosticReport> = DiagnosticReport::class
    override val r4Validator: R4ProfileValidator<DiagnosticReport> = R4DiagnosticReportValidator

    private val requiredSubjectFieldError = RequiredFieldError(DiagnosticReport::subject)
    private val requiredCategoryFieldError = RequiredFieldError(DiagnosticReport::category)

    abstract fun validateProfile(resource: DiagnosticReport, validation: Validation, context: LocationContext)

    override fun validate(resource: DiagnosticReport, validation: Validation, context: LocationContext) {
        validation.apply {
            checkTrue(resource.category.isNotEmpty(), requiredCategoryFieldError, context)
            checkNotNull(resource.subject, requiredSubjectFieldError, context)

            validateReferenceType(
                resource.subject,
                listOf(ResourceType.Patient),
                context.append(LocationContext(DiagnosticReport::subject)),
                this
            )
        }

        validateProfile(resource, validation, context)
    }
}
