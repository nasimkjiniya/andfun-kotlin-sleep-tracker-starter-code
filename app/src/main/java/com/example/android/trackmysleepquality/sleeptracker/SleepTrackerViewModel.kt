/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application)
{

        private var tonight = MutableLiveData<SleepNight?>()
        private var nights = database.getAllNights()

        private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()
        val navigateToSleepQuality: LiveData<SleepNight?>
                get() = _navigateToSleepQuality

        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }

        val startButtonVisible = Transformations.map(tonight) {
                null == it
        }
        val stopButtonVisible = Transformations.map(tonight) {
                null != it
        }
        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        private var _showSnackbarEvent = MutableLiveData<Boolean>()
        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent

        init {
            initializeTonight()
        }

        private fun initializeTonight() {
                //this Job will automatically gets destroyed on viewmodel onClear() method
                viewModelScope.launch(Dispatchers.IO) {
                        tonight.postValue(getTonightFromDatabase())
                }
        }

        //this is an async function
        private suspend fun getTonightFromDatabase() : SleepNight? {
                var night = database.getToNight()
                if (night?.endTimeMilli != night?.startTimeMilli) {
                        night = null  //night already been completed
                }
                return night
        }

        fun onStartTracking() {
                viewModelScope.launch(Dispatchers.IO) {
                        val newNight=SleepNight()
                        insert(newNight)
                        tonight.postValue(getTonightFromDatabase())
                }
        }

        private suspend fun insert(night: SleepNight) {
                database.insert(night)
        }

        fun onStopTracking() {
                viewModelScope.launch(Dispatchers.IO) {
                        val oldNight = tonight.value ?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.postValue(oldNight)
                }
        }

        private suspend fun update(night : SleepNight)
        {
                database.updateNight(night)
        }

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        fun doneShowingSnackbar() {
                _showSnackbarEvent.value = false
        }

        fun onClear() {
                viewModelScope.launch(Dispatchers.IO) {
                        clear()
                        tonight.postValue(null)
                }
                _showSnackbarEvent.value = true
        }

        suspend fun clear() {
                database.clear()
        }

}

