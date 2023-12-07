package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.isInValueSet
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import org.springframework.stereotype.Component

@Component
class RoninPulseOximetryValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_PULSE_OXIMETRY
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
    override val profileVersion: Int = 3

    private val validPulseOximetryUnits = listOf("%")
    private val validFlowRateUnits = listOf("L/min")
    private val validConcentrationUnits = listOf("%")

    private fun validFlowRateValueSet(): ValueSetList =
        registryClient.getRequiredValueSet("Observation.component:FlowRate.code", profile.value)

    private fun validConcentrationValueSet(): ValueSetList =
        registryClient.getRequiredValueSet("Observation.component:Concentration.code", profile.value)

    override fun validateVitalSign(resource: Observation, parentContext: LocationContext, validation: Validation) {
        validateVitalSignValue(resource.value, validPulseOximetryUnits, validation)

        if (resource.dataAbsentReason == null) {
            val validFlowRateValueSet = validFlowRateValueSet()
            val validFlowRateCodes = validFlowRateValueSet.codes
            val validConcentrationValueSet = validConcentrationValueSet()
            val validConcentrationCodes = validConcentrationValueSet.codes

            val components = resource.component
            val flowRate = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(validFlowRateCodes) } ?: false
            }
            val concentration = components.filter { comp ->
                comp.code?.coding?.any { it.isInValueSet(validConcentrationCodes) } ?: false
            }

            if (flowRate.size == 1) {
                validateVitalSignValue(
                    flowRate.first().value,
                    validFlowRateUnits,
                    validation,
                    LocationContext("Observation", "component:FlowRate.value")
                )
            }
            if (concentration.size == 1) {
                validateVitalSignValue(
                    concentration.first().value,
                    validConcentrationUnits,
                    validation,
                    LocationContext("Observation", "component:Concentration.value")
                )
            }
            validation.apply {
                val flowRateCodeContext = LocationContext("Observation", "component:FlowRate.code")
                checkTrue(
                    flowRate.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_004",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        validFlowRateCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = flowRateCodeContext,
                        metadata = validFlowRateValueSet.metadata?.let { listOf(it) } ?: emptyList()
                    ),
                    parentContext
                )
                checkTrue(
                    flowRate.size <= 1,
                    FHIRError(
                        code = "USCORE_PXOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for pulse oximetry flow rate",
                        location = flowRateCodeContext,
                        metadata = validFlowRateValueSet.metadata?.let { listOf(it) } ?: emptyList()
                    ),
                    parentContext
                )

                val concentrationCodeContext = LocationContext("Observation", "component:Concentration.code")
                checkTrue(
                    concentration.isNotEmpty(),
                    FHIRError(
                        code = "RONIN_PXOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                        validConcentrationCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = concentrationCodeContext,
                        metadata = validConcentrationValueSet.metadata?.let { listOf(it) } ?: emptyList()
                    ),
                    parentContext
                )
                checkTrue(
                    concentration.size <= 1,
                    FHIRError(
                        code = "USCORE_PXOBS_006",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for pulse oximetry oxygen concentration",
                        location = concentrationCodeContext,
                        metadata = validConcentrationValueSet.metadata?.let { listOf(it) } ?: emptyList()
                    ),
                    parentContext
                )
            }
        }
    }
}
