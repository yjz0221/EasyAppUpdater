package com.github.mylibdemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.yjz.easyupdater.EasyUpdater


class CheckUpdaterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btnCheckUpdater).setOnClickListener{
            onCheckEvent()
        }
    }

    fun onCheckEvent(){
        EasyUpdater.Builder(this)
            .checkUrl("https://down.szcitycar.com/file/downfile/get?sn=KL011803238S0008&fileapp=23&langid=1")
            .jsonParser(MyParser(application))
            .build()
            .check(isManual = true)
    }
}