package cc.drny.lanzou

import android.util.Log

class Test private constructor() {

    private lateinit var mFilter: MutableList<String>
    private lateinit var mSource: List<String>

    constructor(list: MutableList<String>): this() {
        this.mFilter = list
        this.mSource = list
    }

    fun test() {
        mFilter.clear()
        Log.d("jdy", mSource.toString())
    }

}