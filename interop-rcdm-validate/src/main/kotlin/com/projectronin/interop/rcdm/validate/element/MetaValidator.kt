package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class MetaValidator : ElementValidator<Meta> {
    override val supportedElement: KClass<Meta> = Meta::class

    private val requiredMetaProfile = FHIRError(
        code = "RONIN_META_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Missing expected profiles",
        location = LocationContext(Meta::profile)
    )
    private val requiredMetaSource = RequiredFieldError(Meta::source)

    override fun validate(element: Meta, profiles: List<RoninProfile>, parentContext: LocationContext): Validation =
        Validation().apply {
            checkTrue(profiles.all { element.profile.contains(it.canonical) }, requiredMetaProfile, parentContext) {
                "one or more expected profiles are missing. Expected: ${profiles.joinToString(", ") { it.value }}"
            }

            checkNotNull(element.source, requiredMetaSource, parentContext)
        }
}
