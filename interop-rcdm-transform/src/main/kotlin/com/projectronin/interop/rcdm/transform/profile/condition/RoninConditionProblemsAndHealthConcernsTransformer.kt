package com.projectronin.interop.rcdm.transform.profile.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component

/**
 * Defines the transformer for the [Ronin Condition Problems and Health Concerns](https://supreme-garbanzo-99254d0f.pages.github.io/ig/Ronin-Implementation-Guide-Home-List-Profiles-Ronin-Condition-Problems-and-Health-Concerns.html)
 * profile.
 */
@Component
class RoninConditionProblemsAndHealthConcernsTransformer : BaseRoninConditionProfileTransformer() {
    override val profile: RoninProfile = RoninProfile.CONDITION_PROBLEMS_CONCERNS
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_38_0
    override val profileVersion: Int = 5

    override fun getQualifyingCategories(): List<Coding> =
        listOf(
            Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = Code("problem-list-item")),
            Coding(system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri, code = Code("health-concern")),
        )
}
