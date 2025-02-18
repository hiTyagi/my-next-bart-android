package com.rst.mynextbart.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import com.squareup.moshi.Moshi

class MessageAdapter {
    private val moshi = Moshi.Builder().build()
    private val errorAdapter = moshi.adapter(Message.Error::class.java)

    @FromJson
    fun fromJson(reader: JsonReader): Message {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> Message.fromString(reader.nextString())
            JsonReader.Token.BEGIN_OBJECT -> {
                reader.beginObject()
                var warning: String? = null
                var error: Message.Error? = null
                var schedNum: String? = null
                
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "warning" -> warning = reader.nextString()
                        "error" -> {
                            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                error = errorAdapter.fromJson(reader)
                            } else {
                                reader.skipValue()
                            }
                        }
                        "sched_num" -> schedNum = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                Message(warning = warning, error = error, sched_num = schedNum)
            }
            else -> {
                reader.skipValue()
                Message()
            }
        }
    }

    @ToJson
    fun toJson(message: Message): String {
        return message.text 
            ?: message.warning 
            ?: message.error?.text 
            ?: message.error?.details 
            ?: ""
    }
} 