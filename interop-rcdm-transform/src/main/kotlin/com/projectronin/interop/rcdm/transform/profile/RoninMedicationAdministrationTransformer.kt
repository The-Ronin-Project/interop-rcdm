package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.extractor.MedicationExtractor
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.rcdm.transform.util.populateExtensionWithReference
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninMedicationAdministrationTransformer(
    private val medicationExtractor: MedicationExtractor
) : ProfileTransformer<MedicationAdministration>() {
    override val supportedResource: KClass<MedicationAdministration> = MedicationAdministration::class
    override val profile: RoninProfile = RoninProfile.MEDICATION_ADMINISTRATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_31_0
    override val profileVersion: Int = 3
    override fun transformInternal(
        original: MedicationAdministration,
        tenant: Tenant
    ): TransformResponse<MedicationAdministration>? {
        val medicationExtraction =
            medicationExtractor.extractMedication(original.medication, original.contained, original)

        val medication = medicationExtraction?.updatedMedication ?: original.medication
        val contained = medicationExtraction?.updatedContained ?: original.contained
        val embeddedMedications = medicationExtraction?.extractedMedication?.let { listOf(it) } ?: emptyList()

        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant),
            medication = medication,
            contained = contained,
            extension = original.extension + populateExtensionWithReference(original.medication) // populate extension based on medication[x]
        )

        return TransformResponse(transformed, embeddedMedications)
    }
}
