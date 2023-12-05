package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

/**
 * Validator for the [Ronin Location](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Location.html) profile
 */
@Component
class RoninLocationValidator : ProfileValidator<Location>() {
    override val supportedResource: KClass<Location> = Location::class
    override val r4Validator: R4ProfileValidator<Location> = R4LocationValidator
    override val profile = RoninProfile.LOCATION
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun validate(resource: Location, validation: Validation, context: LocationContext) {
        // Nothing explicit to validate
    }
}
