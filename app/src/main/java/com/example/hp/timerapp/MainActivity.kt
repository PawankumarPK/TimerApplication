package com.example.hp.timerapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.support.annotation.RequiresApi
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_timer.*
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context){
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000

    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped

    private var secondRemaining: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "     Timer"

        fab_start.setOnClickListener { v ->

            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }
        fab_pause.setOnClickListener { v ->

            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }
        fab_stop.setOnClickListener { v ->
            timer.cancel()
            onTimerFinished()
        }

    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped

        //set the length of the timer to be the one set in SettingsActivity
        //if the length was changed when the timer was running
        setNewTimerLength()

        progress_countdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }


    private fun startTimer() {
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength() {
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    override fun onResume() {
        super.onResume()

        initTimer()

        removeAlarm(this)
        NotificationUtils.hideTimerNotification(this)
        //TODO : remove background timer, hide notification
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds,secondRemaining)
            NotificationUtils.showTimerRunning(this,wakeUpTime)

            //TODO : start background timer and show notification
        } else if (timerState == TimerState.Paused) {

            NotificationUtils.showTimerPaused(this)

            //TODO : show notification

        }
        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }


    private fun initTimer() {
        timerState = PrefUtil.getTimerState(this)

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        secondRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        if (alarmSetTime > 0)
            secondRemaining -= nowSeconds - alarmSetTime

        //TODO: change secondsRemaining according to where the background timer stopped

        //resume where we left off
        if (secondRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateButtons()
        updateCountdownUI()
    }

    private fun setPreviousTimerLength() {
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        progress_countdown.max = timerLengthSeconds.toInt()
    }

    private fun updateCountdownUI() {
        val minutesUntilFinished = secondRemaining / 60
        val secondsInMinuteUntilFinished = secondRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_countdown.text = "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress_countdown.progress = (timerLengthSeconds - secondRemaining).toInt()
    }

    private fun updateButtons() {
        when (timerState) {
            TimerState.Running -> {
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_stop.isEnabled = true
            }
            TimerState.Stopped -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = false
            }
            TimerState.Paused -> {
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_stop.isEnabled = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_timer,menu)
        return true
    }


        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            return when (item.itemId) {
                R.id.action_settings -> {
                    val intent = Intent(this, Settings::class.java)
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

        }
    }

