package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAbsentReasonExtension
import com.projectronin.interop.rcdm.common.util.dataAuthorityIdentifier
import com.projectronin.interop.rcdm.common.util.hasDataAbsentReason
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninPatientTransformer : ProfileTransformer<Patient>() {
    override val supportedResource: KClass<Patient> = Patient::class
    override val profile: RoninProfile = RoninProfile.PATIENT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 3
    override val isDefault: Boolean = true

    override fun qualifies(resource: Patient): Boolean = true

    override fun transformInternal(original: Patient, tenant: Tenant): TransformResponse<Patient>? {
        val maritalStatus = original.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.NULL_FLAVOR.uri,
                    code = Code("NI"),
                    display = FHIRString("NoInformation")
                )
            )
        )
        val gender = original.gender.takeIf { !it.hasDataAbsentReason() } ?: Code(
            AdministrativeGender.UNKNOWN.code,
            original.gender!!.id,
            original.gender!!.extension
        )

        val originalIdentifiers = normalizeIdentifierSystems(original.identifier)

        val transformed = original.copy(
            gender = gender,
            identifier = originalIdentifiers + tenant.toFhirIdentifier() + dataAuthorityIdentifier,
            maritalStatus = maritalStatus
        )
        return TransformResponse(transformed)
    }

    private fun normalizeIdentifierSystems(identifiers: List<Identifier>): List<Identifier> {
        return identifiers.map {
            if (it.system?.value == null) {
                updateIdentifierWithDAR(it)
            } else {
                it
            }
        }
    }

    private fun updateIdentifierWithDAR(identifier: Identifier): Identifier {
        return identifier.copy(system = Uri(value = null, extension = dataAbsentReasonExtension))
    }
}
