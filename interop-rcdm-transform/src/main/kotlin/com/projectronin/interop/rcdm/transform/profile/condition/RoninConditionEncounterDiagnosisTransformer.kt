package com.projectronin.interop.rcdm.transform.profile.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component

/**
 * Defines the transformer for the [Ronin Condition Encounter Diagnosis](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Condition-Encounter-Diagnosis.html)
 * profile.
 */
@Component
class RoninConditionEncounterDiagnosisTransformer : BaseRoninConditionProfileTransformer() {
    override val profile: RoninProfile = RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_38_0
    override val profileVersion: Int = 3

    override fun getQualifyingCategories(): List<Coding> =
        listOf(Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = Code("encounter-diagnosis")))
}
