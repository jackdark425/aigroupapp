package com.aigroup.aigroupmobile.data

import androidx.lifecycle.LiveData
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.BaseRealmObject

class RealmLiveData<T: BaseRealmObject>(): LiveData<RealmResults<T>>() {

  override fun onActive() {
    super.onActive()
  }

  override fun onInactive() {
    super.onInactive()
  }
}