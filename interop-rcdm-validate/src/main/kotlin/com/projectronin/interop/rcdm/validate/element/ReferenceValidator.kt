package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ReferenceValidator : ElementValidator<Reference> {
    override val supportedElement: KClass<Reference> = Reference::class

    private val requiredDataAuthorityExtensionIdentifier =
        FHIRError(
            code = "RONIN_DAUTH_EX_001",
            severity = ValidationIssueSeverity.ERROR,
            description = "Data Authority extension identifier is required for reference",
            location = LocationContext("", "type.extension"),
        )

    override fun validate(
        element: Reference,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
    ): Validation =
        Validation().apply {
            element.type?.let { type ->
                // This only matters if the reference is populated. If the reference is not populated, then it's not part of a Data Authority.
                element.reference?.let {
                    val dataAuthExtensionIdentifier = type.extension
                    checkTrue(
                        dataAuthExtensionIdentifier == dataAuthorityExtension,
                        requiredDataAuthorityExtensionIdentifier,
                        parentContext,
                    )
                }
            }
        }
}
