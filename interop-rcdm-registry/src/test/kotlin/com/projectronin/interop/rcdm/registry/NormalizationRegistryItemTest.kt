package com.projectronin.interop.rcdm.registry

import com.projectronin.interop.common.jackson.JacksonUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NormalizationRegistryItemTest {
    @Test
    fun `can create`() {
        val registryJson =
            """
            [
                {
                    "resource_type": "Observation",
                    "data_element": "Observation.code",
                    "profile_url": "http://projectronin.io/fhir/StructureDefinition/ronin-observationLaboratoryResult",
                    "tenant_id": null,
                    "registry_entry_type": "value_set",
                    "version": 1,
                    "filename": "LabResultValueSet.json",
                    "source_extension_url": "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-laboratoryResult",
                    "registry_uuid": "2",
                    "value_set_name": "Value Set",
                    "value_set_uuid": "something-new-and-different"
                },
                {
                    "resource_type": "Appointment",
                    "data_element": "Appointment.status",
                    "tenant_id": "tenant",
                    "registry_entry_type": "concept_map",
                    "concept_map_uuid": "4ffae118-778f-4df9-bd73-aece934b521b",
                    "version": 1,
                    "filename": "ConceptMap.json",
                    "source_extension_url": "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus",
                    "registry_uuid": "1",
                    "concept_map_name": "AppointmentStatusRonin"
                }
            ]
            """.trimIndent()
        val items = JacksonUtil.readJsonList(registryJson, NormalizationRegistryItem::class)
        assertEquals(2, items.size)
        assertEquals(
            NormalizationRegistryItem(
                registryUuid = "2",
                dataElement = "Observation.code",
                filename = "LabResultValueSet.json",
                version = "1",
                sourceExtensionUrl = "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-laboratoryResult",
                resourceType = "Observation",
                tenantId = null,
                profileUrl = "http://projectronin.io/fhir/StructureDefinition/ronin-observationLaboratoryResult",
                conceptMapName = null,
                conceptMapUuid = null,
                valueSetName = "Value Set",
                valueSetUuid = "something-new-and-different",
                registryEntryType = RegistryType.VALUE_SET,
            ),
            items[0],
        )
        assertEquals(
            NormalizationRegistryItem(
                registryUuid = "1",
                dataElement = "Appointment.status",
                filename = "ConceptMap.json",
                version = "1",
                sourceExtensionUrl = "http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus",
                resourceType = "Appointment",
                tenantId = "tenant",
                profileUrl = null,
                conceptMapName = "AppointmentStatusRonin",
                conceptMapUuid = "4ffae118-778f-4df9-bd73-aece934b521b",
                valueSetName = null,
                valueSetUuid = null,
                registryEntryType = RegistryType.CONCEPT_MAP,
            ),
            items[1],
        )
    }
}
