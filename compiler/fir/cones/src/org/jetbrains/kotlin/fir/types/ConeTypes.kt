/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithNullability
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(), KotlinTypeMarker, TypeArgumentListMarker {
    final override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT

    abstract val typeArguments: Array<out ConeTypeProjection>

    final override val type: ConeKotlinType
        get() = this

    abstract val nullability: ConeNullability

    abstract val attributes: ConeAttributes

    final override fun toString(): String {
        return renderForDebugging()
    }

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

sealed class ConeSimpleKotlinType : ConeKotlinType(), SimpleTypeMarker

class ConeClassLikeErrorLookupTag(override val classId: ClassId) : ConeClassLikeLookupTag()

class ConeErrorType(
    val diagnostic: ConeDiagnostic,
    val isUninferredParameter: Boolean = false,
    val delegatedType: ConeKotlinType? = null,
    override val typeArguments: Array<out ConeTypeProjection> = EMPTY_ARRAY,
    override val attributes: ConeAttributes = ConeAttributes.Empty
) : ConeClassLikeType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = ConeClassLikeErrorLookupTag(ClassId.fromString("<error>"))

    override val nullability: ConeNullability
        get() = if (diagnostic is ConeDiagnosticWithNullability) {
            if (diagnostic.isNullable) ConeNullability.NULLABLE else ConeNullability.NOT_NULL
        } else ConeNullability.UNKNOWN

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

abstract class ConeLookupTagBasedType : ConeSimpleKotlinType() {
    abstract val lookupTag: ConeClassifierLookupTag
}

abstract class ConeClassLikeType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeClassLikeLookupTag
}

open class ConeFlexibleType(
    val lowerBound: ConeSimpleKotlinType,
    val upperBound: ConeSimpleKotlinType
) : ConeKotlinType(), FlexibleTypeMarker {

    final override val typeArguments: Array<out ConeTypeProjection>
        get() = lowerBound.typeArguments

    final override val nullability: ConeNullability
        get() = lowerBound.nullability.takeIf { it == upperBound.nullability } ?: ConeNullability.UNKNOWN

    final override val attributes: ConeAttributes
        get() = lowerBound.attributes

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // I suppose dynamic type (see below) and flexible type should use the same equals,
        // because ft<Any?, Nothing> should never be created
        if (other !is ConeFlexibleType) return false

        if (lowerBound != other.lowerBound) return false
        if (upperBound != other.upperBound) return false

        return true
    }

    final override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        return result
    }
}

@RequiresOptIn(message = "Please use ConeDynamicType.create instead")
annotation class DynamicTypeConstructor

class ConeDynamicType @DynamicTypeConstructor constructor(
    lowerBound: ConeSimpleKotlinType,
    upperBound: ConeSimpleKotlinType
) : ConeFlexibleType(lowerBound, upperBound), DynamicTypeMarker {
    companion object
}

fun ConeSimpleKotlinType.unwrapDefinitelyNotNull(): ConeSimpleKotlinType {
    return when (this) {
        is ConeDefinitelyNotNullType -> original
        else -> this
    }
}

fun ConeKotlinType.unwrapFlexibleAndDefinitelyNotNull(): ConeKotlinType {
    return lowerBoundIfFlexible().unwrapDefinitelyNotNull()
}

class ConeCapturedTypeConstructor(
    val projection: ConeTypeProjection,
    var supertypes: List<ConeKotlinType>? = null,
    val typeParameterMarker: TypeParameterMarker? = null,
    identity: ConeCapturedTypeConstructor?,
) : CapturedTypeConstructorMarker {
    // Unwrap transitive identity
    val identity: ConeCapturedTypeConstructor? = identity?.identity ?: identity

    init {
        require(this.identity?.identity == null) {
            "Captured type identity shouldn't have an external identity"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeCapturedTypeConstructor) return false

        // TODO Do we need to check identity === other?
        if (identity == null || identity !== other.identity) return false

        if (projection != other.projection) return false
        if (supertypes != other.supertypes) return false
        if (typeParameterMarker != other.typeParameterMarker) return false

        return true
    }

    override fun hashCode(): Int {
        var result = projection.hashCode()
        result = 31 * result + (typeParameterMarker?.hashCode() ?: 0)
        return result
    }
}

data class ConeCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: ConeKotlinType?,
    override val nullability: ConeNullability = ConeNullability.NOT_NULL,
    val constructor: ConeCapturedTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    val isProjectionNotNull: Boolean = false,
) : ConeSimpleKotlinType(), CapturedTypeMarker {
    constructor(
        captureStatus: CaptureStatus,
        lowerType: ConeKotlinType?,
        projection: ConeTypeProjection,
        typeParameterMarker: TypeParameterMarker
    ) : this(
        captureStatus,
        lowerType,
        constructor = ConeCapturedTypeConstructor(
            projection,
            typeParameterMarker = typeParameterMarker,
            identity = null,
        )
    )

    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeCapturedType

        if (lowerType != other.lowerType) return false
        if (constructor != other.constructor) return false
        if (captureStatus != other.captureStatus) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + (lowerType?.hashCode() ?: 0)
        result = 31 * result + constructor.hashCode()
        result = 31 * result + captureStatus.hashCode()
        result = 31 * result + nullability.hashCode()
        return result
    }
}


data class ConeDefinitelyNotNullType(val original: ConeSimpleKotlinType) : ConeSimpleKotlinType(), DefinitelyNotNullTypeMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = original.typeArguments

    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL

    override val attributes: ConeAttributes
        get() = original.attributes

    companion object
}

class ConeRawType private constructor(
    lowerBound: ConeSimpleKotlinType,
    upperBound: ConeSimpleKotlinType
) : ConeFlexibleType(lowerBound, upperBound) {
    companion object {
        fun create(
            lowerBound: ConeSimpleKotlinType,
            upperBound: ConeSimpleKotlinType,
        ): ConeRawType {
            require(lowerBound is ConeClassLikeType && upperBound is ConeClassLikeType) {
                "Raw bounds are expected to be class-like types, but $lowerBound and $upperBound were found"
            }

            val lowerBoundToUse = if (!lowerBound.attributes.contains(CompilerConeAttributes.RawType)) {
                ConeClassLikeTypeImpl(
                    lowerBound.lookupTag, lowerBound.typeArguments, lowerBound.isNullable,
                    lowerBound.attributes.add(CompilerConeAttributes.RawType)
                )
            } else {
                lowerBound
            }

            return ConeRawType(lowerBoundToUse, upperBound)
        }
    }
}

/**
 * This class represents so-called intersection type like T1&T2&T3 [intersectedTypes] = listOf(T1, T2, T3).
 *
 * Contract of the intersection type: it is flat. It means that an intersection type can not contain another intersection type inside it.
 * To comply with this contract, construct new intersection types only via [org.jetbrains.kotlin.fir.types.ConeTypeIntersector].
 *
 * Except for T&Any types, [org.jetbrains.kotlin.fir.types.ConeIntersectionType] is non-denotable.
 * Moreover, it does not have an IR counterpart.
 * This means that approximation is often required, and normally a common supertype of [intersectedTypes] is used for this purpose.
 * In a situation with constraints like A <: T, B <: T, T <: C, [org.jetbrains.kotlin.fir.types.ConeIntersectionType]
 * and commonSupertype(A, B) </: C, an intersection type A&B is created,
 * C is stored as [upperBoundForApproximation] and used when approximation is needed.
 * Without it, we can violate a constraint system while doing intersection type approximation.
 * See also [org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver.specialResultForIntersectionType]
 *
 * @param intersectedTypes collection of types to be intersected. None of them is allowed to be another intersection type.
 * @param upperBoundForApproximation a super-type (upper bound), if it's known, to be used as an approximation.
 */
class ConeIntersectionType(
    val intersectedTypes: Collection<ConeKotlinType>,
    val upperBoundForApproximation: ConeKotlinType? = null,
) : ConeSimpleKotlinType(), IntersectionTypeConstructorMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL

    val effectiveNullability: ConeNullability
        get() = intersectedTypes.maxOf { it.nullability }

    override val attributes: ConeAttributes = intersectedTypes.foldMap(
        { it.attributes },
        { a, b -> a.intersect(b) }
    )

    private var hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeIntersectionType

        if (intersectedTypes != other.intersectedTypes) return false

        return true
    }

    override fun hashCode(): Int {
        if (hashCode != 0) return hashCode
        return intersectedTypes.hashCode().also { hashCode = it }
    }
}
