package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.hasDataAbsentReason
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

/**
 * Validator for the [Ronin Patient](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Patient.html) profile
 */
@Component
class RoninPatientValidator : ProfileValidator<Patient>() {
    override val supportedResource: KClass<Patient> = Patient::class
    override val r4Validator: R4ProfileValidator<Patient> = R4PatientValidator
    override val profile: RoninProfile = RoninProfile.PATIENT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 3

    private val requiredGenderError = RequiredFieldError(Patient::gender)
    private val requiredBirthDateError = RequiredFieldError(Patient::birthDate)
    private val requiredIdentifierValueError = RequiredFieldError(Identifier::value)

    private val requiredMrnIdentifierError = FHIRError(
        code = "RONIN_PAT_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier is required",
        location = LocationContext(Patient::identifier)
    )
    private val wrongMrnIdentifierTypeError = FHIRError(
        code = "RONIN_PAT_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "MRN identifier type defined without proper CodeableConcept",
        location = LocationContext(Patient::identifier)
    )
    private val invalidBirthDateError = FHIRError(
        code = "RONIN_PAT_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "Birth date is invalid",
        location = LocationContext(Patient::birthDate)
    )
    private val invalidOfficialNameError = FHIRError(
        code = "RONIN_PAT_005",
        severity = ValidationIssueSeverity.ERROR,
        description = "A name for official use must be present",
        location = LocationContext(Patient::name)
    )
    private val requiredIdentifierSystemValueError = FHIRError(
        code = "RONIN_PAT_006",
        severity = ValidationIssueSeverity.ERROR,
        description = "Identifier system or data absent reason is required",
        location = LocationContext(Identifier::system)
    )

    private val requiredNameError = FHIRError(
        code = "RONIN_PAT_007",
        severity = ValidationIssueSeverity.ERROR,
        description = "At least one name must be provided",
        location = LocationContext(Patient::name)
    )
    private val requiredFamilyOrGivenError = FHIRError(
        code = "RONIN_PAT_008",
        severity = ValidationIssueSeverity.ERROR,
        description = "Either Patient.name.given and/or Patient.name.family SHALL be present or a Data Absent Reason Extension SHALL be present",
        location = null
    )

    override fun validate(resource: Patient, validation: Validation, context: LocationContext) {
        validation.apply {
            val mrnIdentifier = resource.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
            checkNotNull(mrnIdentifier, requiredMrnIdentifierError, context)

            ifNotNull(mrnIdentifier) {
                checkTrue(mrnIdentifier.type == CodeableConcepts.RONIN_MRN, wrongMrnIdentifierTypeError, context)
            }

            resource.identifier.forEachIndexed { index, identifier ->
                val identifierContext = context.append(LocationContext("", "identifier[$index]"))
                checkTrue(
                    identifier.system?.value != null || identifier.system.hasDataAbsentReason(),
                    requiredIdentifierSystemValueError,
                    identifierContext
                )
                checkNotNull(identifier.value, requiredIdentifierValueError, identifierContext)
            }

            checkNotNull(resource.birthDate, requiredBirthDateError, context)
            resource.birthDate?.value?.let {
                checkTrue(it.length == 10, invalidBirthDateError, context)
            }

            checkTrue(resource.name.isNotEmpty(), requiredNameError, context)

            val nameList = resource.name.find { it.use?.value == NameUse.OFFICIAL.code }
            checkNotNull(nameList, invalidOfficialNameError, context)

            // Each human name should have a first or last name populated, otherwise a data absent reason.
            resource.name.forEachIndexed { index, humanName ->
                checkTrue(
                    ((humanName.family != null) or (humanName.given.isNotEmpty())) xor humanName.hasDataAbsentReason(),
                    requiredFamilyOrGivenError,
                    context.append(LocationContext("Patient", "name[$index]"))
                )
            }

            checkNotNull(resource.gender, requiredGenderError, context)
        }
    }
}
