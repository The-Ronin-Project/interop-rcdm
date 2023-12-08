package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninAppointmentTransformer : ProfileTransformer<Appointment>() {
    override val supportedResource: KClass<Appointment> = Appointment::class
    override val profile: RoninProfile = RoninProfile.APPOINTMENT
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 3

    override fun transformInternal(
        original: Appointment,
        tenant: Tenant,
    ): TransformResponse<Appointment>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
