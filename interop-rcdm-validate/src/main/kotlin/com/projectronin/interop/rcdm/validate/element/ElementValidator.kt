package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import kotlin.reflect.KClass

interface ElementValidator<E : Element<E>> {
    val supportedElement: KClass<E>

    /**
     * Validates the [element] when encountered for [profiles].
     */
    fun validate(
        element: E,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
    ): Validation
}
