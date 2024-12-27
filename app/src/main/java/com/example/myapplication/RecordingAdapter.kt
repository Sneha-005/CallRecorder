package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecordingAdapter(private val recordings: MutableList<File>, private val onPlayClick: (Recording) -> Unit) :
    RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    class RecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val callerName: TextView = view.findViewById(R.id.tvCallerName)
        val callDetails: TextView = view.findViewById(R.id.tvCallDetails)
        val playButton: Button = view.findViewById(R.id.btnPlayRecording)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]
        //holder.playButton.setOnClickListener { onPlayClick(recording) }
    }

    override fun getItemCount(): Int = recordings.size
}