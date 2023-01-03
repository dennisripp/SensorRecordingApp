package dev.ostfalia.iotcam.ui.fragments.videolistview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import dev.ostfalia.iotcam.R
import dev.ostfalia.iotcam.network.VideoListViewFiles


class VideoListAdapter(
    context: Context,
    private val videoListViewFiles: VideoListViewFiles
) : ArrayAdapter<String>(context, R.layout.custom_list, videoListViewFiles.paths) {

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val rowView = inflater.inflate(R.layout.custom_list, null, true)

        val hasElememt = videoListViewFiles.size() > position

        if(!hasElememt) return rowView

        val titleText = rowView.findViewById(R.id.title) as TextView
        val imageView = rowView.findViewById(R.id.icon) as ImageView
        val lengthText = rowView.findViewById(R.id.length_text) as TextView
        val subtitleText = rowView.findViewById(R.id.description) as TextView
        val tagText = rowView.findViewById(R.id.tag_text) as TextView
        val projectIDText = rowView.findViewById(R.id.project_id) as TextView

        titleText.text = videoListViewFiles.names.get(position).substringBeforeLast("_")
        tagText.text = "tag: ${videoListViewFiles.tag.get(position)}"
        projectIDText.text = "project: ${videoListViewFiles.project.get(position)}"
        subtitleText.text = "${videoListViewFiles.description.get(position)}"
        imageView.setImageBitmap(videoListViewFiles.thumbnails.get(position))

        lengthText.text = "length: ${videoListViewFiles.length.get(position).format(2)}s"

        return rowView
    }

    fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
