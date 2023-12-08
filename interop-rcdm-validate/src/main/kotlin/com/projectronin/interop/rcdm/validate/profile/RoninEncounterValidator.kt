package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninEncounterValidator : ProfileValidator<Encounter>() {
    override val supportedResource: KClass<Encounter> = Encounter::class
    override val r4Validator: R4ProfileValidator<Encounter> = R4EncounterValidator
    override val profile = RoninProfile.ENCOUNTER
    override val rcdmVersion = RCDMVersion.V3_20_0
    override val profileVersion = 4

    private val requiredSubjectError = RequiredFieldError(Encounter::subject)
    private val requiredTypeError = RequiredFieldError(Encounter::type)
    private val requiredIdentifierSystemError = RequiredFieldError(Identifier::system)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    private val requiredExtensionClassError =
        FHIRError(
            code = "RONIN_ENC_001",
            description = "Tenant source encounter class extension is missing or invalid",
            severity = ValidationIssueSeverity.ERROR,
            location = LocationContext(Encounter::extension),
        )

    override fun validate(
        resource: Encounter,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS.uri &&
                        it.value?.type == DynamicValueType.CODING
                },
                requiredExtensionClassError,
                context,
            )

            checkTrue(resource.type.isNotEmpty(), requiredTypeError, context)

            checkNotNull(resource.subject, requiredSubjectError, context)

            resource.identifier.forEachIndexed { index, identifier ->
                val identifierContext = context.append(LocationContext("Encounter", "identifier[$index]"))
                checkNotNull(identifier.system, requiredIdentifierSystemError, identifierContext)
                checkNotNull(identifier.value, requiredIdentifierValueError, identifierContext)
            }
        }
    }
}
