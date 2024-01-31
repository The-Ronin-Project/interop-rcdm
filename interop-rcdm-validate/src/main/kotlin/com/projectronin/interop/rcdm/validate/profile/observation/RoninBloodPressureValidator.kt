package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
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
class RoninBloodPressureValidator(registryClient: NormalizationRegistryClient) :
    BaseRoninVitalSignProfileValidator(registryClient) {
    override val profile: RoninProfile = RoninProfile.OBSERVATION_BLOOD_PRESSURE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_36_1
    override val profileVersion: Int = 6

    private val validBloodPressureUnits = listOf("mm[Hg]")

    override val acceptedValueTypes: List<DynamicValueType> =
        listOf(DynamicValueType.QUANTITY, DynamicValueType.INTEGER, DynamicValueType.RATIO, DynamicValueType.STRING)

    private fun validSystolicValueSet(): ValueSetList =
        registryClient.getRequiredValueSet("Observation.component:systolic.code", profile.value)

    private fun validDiastolicValueSet(): ValueSetList =
        registryClient.getRequiredValueSet("Observation.component:diastolic.code", profile.value)

    override fun validateVitalSign(
        resource: Observation,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        if (resource.dataAbsentReason == null) {
            val validSystolicValueSet = validSystolicValueSet()
            val validSystolicCodes = validSystolicValueSet.codes
            val validDiastolicValueSet = validDiastolicValueSet()
            val validDiastolicCodes = validDiastolicValueSet.codes

            val components = resource.component
            val systolic =
                components.filter { comp ->
                    comp.code?.coding?.any { it.isInValueSet(validSystolicCodes) } ?: false
                }
            val diastolic =
                components.filter { comp ->
                    comp.code?.coding?.any { it.isInValueSet(validDiastolicCodes) } ?: false
                }

            if (systolic.size == 1) {
                validateVitalSignValue(
                    systolic.first().value,
                    validBloodPressureUnits,
                    validation,
                    LocationContext("Observation", "component:systolic.value"),
                )
            }
            if (diastolic.size == 1) {
                validateVitalSignValue(
                    diastolic.first().value,
                    validBloodPressureUnits,
                    validation,
                    LocationContext("Observation", "component:diastolic.value"),
                )
            }
            validation.apply {
                val componentSystolicCodeContext = LocationContext("Observation", "component:systolic.code")
                checkTrue(
                    systolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                            validSystolicCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentSystolicCodeContext,
                        metadata = validSystolicValueSet.metadata?.let { listOf(it) } ?: emptyList(),
                    ),
                    parentContext,
                )
                checkTrue(
                    systolic.size <= 1,
                    FHIRError(
                        code = "USCORE_BPOBS_004",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for systolic blood pressure",
                        location = componentSystolicCodeContext,
                        metadata = validSystolicValueSet.metadata?.let { listOf(it) } ?: emptyList(),
                    ),
                    parentContext,
                )

                val componentDiastolicCodeContext = LocationContext("Observation", "component:diastolic.code")
                checkTrue(
                    diastolic.isNotEmpty(),
                    FHIRError(
                        code = "USCORE_BPOBS_002",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Must match this system|code: ${
                            validDiastolicCodes.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                        }",
                        location = componentDiastolicCodeContext,
                        metadata = validDiastolicValueSet.metadata?.let { listOf(it) } ?: emptyList(),
                    ),
                    parentContext,
                )
                checkTrue(
                    diastolic.size <= 1,
                    FHIRError(
                        code = "USCORE_BPOBS_005",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Only 1 entry is allowed for diastolic blood pressure",
                        location = componentDiastolicCodeContext,
                        metadata = validDiastolicValueSet.metadata?.let { listOf(it) } ?: emptyList(),
                    ),
                    parentContext,
                )
                checkTrue(
                    (components - diastolic.toSet() - systolic.toSet()).isEmpty(),
                    FHIRError(
                        code = "RONIN_BPOBS_001",
                        severity = ValidationIssueSeverity.ERROR,
                        description = "Blood Pressure components must be either a Systolic or Diastolic",
                        location = LocationContext("Observation", "component"),
                        metadata = emptyList(),
                    ),
                    parentContext,
                )
            }
        }
    }
}
