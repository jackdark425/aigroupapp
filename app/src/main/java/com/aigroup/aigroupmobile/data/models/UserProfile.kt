package com.aigroup.aigroupmobile.data.models

import com.aigroup.aigroupmobile.data.extensions.AppPreferencesDefaults
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PersistedName
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class UserProfile: RealmObject {
    @PrimaryKey
    @PersistedName("_id")
    var id = ObjectId()
    var username = ""
    var avatar: ImageMediaItem? = null
    var email: String? = null

    var favoriteModels: RealmList<String> = realmListOf<String>().apply {
        addAll(AppPreferencesDefaults.defaultFavoriteModels)
    }
}

