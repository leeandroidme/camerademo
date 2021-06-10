package com.newland.tiktok

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.Unbinder

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
abstract class BaseActivity:AppCompatActivity() {
    private lateinit var unBinder:Unbinder
    abstract fun getLayoutId():Int
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        unBinder=ButterKnife.bind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unBinder.unbind()
    }

}