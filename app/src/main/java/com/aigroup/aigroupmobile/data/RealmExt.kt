package com.aigroup.aigroupmobile.data

import io.realm.kotlin.ext.backlinks
import io.realm.kotlin.types.BacklinksDelegate
import io.realm.kotlin.types.EmbeddedBacklinksDelegate
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KProperty1

inline fun <reified T : TypedRealmObject> EmbeddedRealmObject.embedBackLink(
    sourceProperty: KProperty1<T, *>
): EmbeddedBacklinksDelegate<T> = this.backlinks(sourceProperty, T::class)
