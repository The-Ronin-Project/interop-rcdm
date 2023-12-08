package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ContactPointValidator : ElementValidator<ContactPoint> {
    override val supportedElement: KClass<ContactPoint> = ContactPoint::class

    private val requiredTelecomSystemError = RequiredFieldError(ContactPoint::system)
    private val requiredTelecomValueError = RequiredFieldError(ContactPoint::value)

    private val requiredTelecomSystemExtensionError =
        FHIRError(
            code = "RONIN_CNTCTPT_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "A single tenant source element system extension is required",
            location = LocationContext(ContactPoint::system),
        )
    private val requiredTelecomUseExtensionError =
        FHIRError(
            code = "RONIN_CNTCTPT_003",
            severity = ValidationIssueSeverity.ERROR,
            description = "A single tenant source element use extension is required",
            location = LocationContext(ContactPoint::use),
        )

    override fun validate(
        element: ContactPoint,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
    ): Validation =
        Validation().apply {
            checkNotNull(element.system, requiredTelecomSystemError, parentContext)
            element.system?.let { system ->
                val extension =
                    system.extension.singleOrNull { it.url?.value == RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value }
                checkNotNull(
                    extension,
                    requiredTelecomSystemExtensionError,
                    parentContext,
                )
            }

            checkNotNull(element.value, requiredTelecomValueError, parentContext)

            element.use?.let { use ->
                val extension =
                    use.extension.singleOrNull { it.url?.value == RoninExtension.TENANT_SOURCE_TELECOM_USE.value }
                checkNotNull(
                    extension,
                    requiredTelecomUseExtensionError,
                    parentContext,
                )
            }
        }
}
