/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.kotlin.completion.util

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

fun SmartCastManager.getSmartCastVariantsWithLessSpecificExcluded(
    receiverToCast: ReceiverValue,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
    dataFlowInfo: DataFlowInfo,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): List<KotlinType> {
    val variants = getSmartCastVariants(
        receiverToCast,
        bindingContext,
        containingDeclarationOrModule,
        dataFlowInfo,
        languageVersionSettings,
        dataFlowValueFactory
    )
    return variants.filter { type ->
        variants.all { another -> another === type || chooseMoreSpecific(type, another).let { it == null || it === type } }
    }
}

private fun chooseMoreSpecific(type1: KotlinType, type2: KotlinType): KotlinType? {
    val type1IsSubtype = KotlinTypeChecker.DEFAULT.isSubtypeOf(type1, type2)
    val type2IsSubtype = KotlinTypeChecker.DEFAULT.isSubtypeOf(type2, type1)

    when {
        type1IsSubtype && !type2IsSubtype -> return type1

        type2IsSubtype && !type1IsSubtype -> return type2

        !type1IsSubtype && !type2IsSubtype -> return null

        else -> {
            val flexible1 = type1.unwrap() as? FlexibleType
            val flexible2 = type2.unwrap() as? FlexibleType
            return when {
                flexible1 != null && flexible2 == null -> type2
                flexible2 != null && flexible1 == null -> type1
                else -> null //TODO?
            }
        }
    }
}