package com.example.watchkatya

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.*
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer

class ChatActivity : WearableActivity() {
    private lateinit var wrc: WearableRecyclerView
    private val chatList = ArrayList<Chat>()
    private lateinit var app: App
    private lateinit var message: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        app = applicationContext as App

        wrc = findViewById(R.id.chatRecyclerView)



        wrc.isEdgeItemsCenteringEnabled = true


        wrc.layoutManager = object: LinearLayoutManager(this, VERTICAL, false){
            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                if (direction == View.FOCUS_DOWN) {
                    val pos = getPosition(focused)
                    if (pos == itemCount-1)
                        return focused
                }
                if (direction == View.FOCUS_UP) {
                    val pos = getPosition(focused)
                    if (pos == 0)
                        return focused
                }
                return super.onInterceptFocusSearch(focused, direction)
            }
        }



        wrc.adapter = Adapter(chatList, this)
        message = findViewById(R.id.message)



        timer(period = 5000L, startAt = Date()){
            updateChat()

        }
    }
    fun updateChat(){


        HTTP.requestGET(
            "http://s4a.kolei.ru/chat",


            mapOf(
                "Token" to app.token
            )
        ){result, error ->
            if(result!=null){
                try {



                    val jsonResp = JSONObject(result)


                    if(!jsonResp.has("notice"))
                        throw Exception("Не верный формат ответа, ожидался объект notice")


                    if(jsonResp.getJSONObject("notice").has("answer"))
                        throw Exception(jsonResp.getJSONObject("notice").getString("answer"))



                    if (jsonResp.getJSONObject("notice").has("messages")) {
                        chatList.clear()
                        val data = jsonResp.getJSONObject("notice").getJSONArray("messages")
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            chatList.add(
                                Chat(
                                    item.getString("user"),
                                    item.getString("message")
                                )
                            )
                        }
                        runOnUiThread {
                            wrc.adapter?.notifyDataSetChanged()

                        }
                    }
                    else
                        throw Exception("Не верный формат ответа, ожидался объект messages")
                }
                catch (e: Exception)
                {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Ошибка")
                            .setMessage(e.message)
                            .setPositiveButton("OK", null)
                            .create()
                            .show()
                    }
                }
            }
            else
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Ошибка http-запроса")
                        .setMessage(error)
                        .setPositiveButton("OK", null)
                        .create()
                        .show()
                }

        }
    }

    fun Message(view: View) {

        HTTP.requestPOST(
            "http://s4a.kolei.ru/chat",
            JSONObject().put("message", message.text.toString()),

            mapOf(
                "Content-Type" to "application/json",
                "Token" to app.token
            )
        ){result, error ->
            if(result!=null){
                try {
                    // анализируем ответ
                    val jsonResp = JSONObject(result)

                    // если нет объекта notice
                    if(!jsonResp.has("notice"))
                        throw Exception("Не верный формат ответа, ожидался объект notice")


                    if(jsonResp.getJSONObject("notice").has("answer"))
                        throw Exception(jsonResp.getJSONObject("notice").getString("answer"))


                }
                catch (e: Exception)
                {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Ошибка")
                            .setMessage(e.message)
                            .setPositiveButton("OK", null)
                            .create()
                            .show()
                    }
                }
            }
        }
    }



}