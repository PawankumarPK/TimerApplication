package com.example.hp.timerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi

class TimerNotificationActionReceiver : BroadcastReceiver(){

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action){
            AppConstants.ACTION_STOP -> {
                MainActivity.removeAlarm(context)
                PrefUtil.setTimerState(MainActivity.TimerState.Stopped, context)
                NotificationUtils.hideTimerNotification(context)
            }
            AppConstants.ACTION_PAUSE -> {
                var secondsRemaining = PrefUtil.getSecondsRemaining(context)
                val alarmSetTime = PrefUtil.getAlarmSetTime(context)
                val nowSeconds = MainActivity.nowSeconds

                secondsRemaining -= nowSeconds - alarmSetTime
                PrefUtil.setSecondsRemaining(secondsRemaining, context)

                MainActivity.removeAlarm(context)
                PrefUtil.setTimerState(MainActivity.TimerState.Paused, context)
                NotificationUtils.showTimerPaused(context)
            }
            AppConstants.ACTION_RESUME -> {
                val secondsRemaining = PrefUtil.getSecondsRemaining(context)
                val wakeUpTime = MainActivity.setAlarm(context, MainActivity.nowSeconds, secondsRemaining)
                PrefUtil.setTimerState(MainActivity.TimerState.Running, context)
                NotificationUtils.showTimerRunning(context, wakeUpTime)
            }
            AppConstants.ACTION_START -> {
                val minutesRemaining = PrefUtil.getTimerLength(context)
                val secondsRemaining = minutesRemaining * 60L
                val wakeUpTime = MainActivity.setAlarm(context, MainActivity.nowSeconds, secondsRemaining)
                PrefUtil.setTimerState(MainActivity.TimerState.Running, context)
                PrefUtil.setSecondsRemaining(secondsRemaining, context)
                NotificationUtils.showTimerRunning(context, wakeUpTime)
            }
        }
    }
}
