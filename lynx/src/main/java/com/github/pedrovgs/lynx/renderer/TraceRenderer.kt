/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.lynx.renderer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.pedrovgs.lynx.LynxConfig
import com.github.pedrovgs.lynx.R
import com.github.pedrovgs.lynx.model.Trace
import com.github.pedrovgs.lynx.model.TraceLevel
import com.pedrogomez.renderers.Renderer

/**
 * Base Renderer<Trace> used to show Trace objects inside a ListView using TraceLevel and Trace
 * message as main information to show. This Renderer<Trace> is used as the base of other
 * Renderers<Trace> and to show verbose TraceLevel traces.
 *
 *
 * To learn more about Renderers library take a look to the repository:
 * https://github.com/pedrovgs/Renderers
 *
 * @author Pedro Vicente Gomez Sanchez.
</Trace></Trace></Trace> */
internal open class TraceRenderer(private val lynxConfig: LynxConfig) : Renderer<Trace?>() {
    private var tvTrace: TextView? = null
    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): View {
        return inflater.inflate(R.layout.trace_row, parent, false)
    }

    override fun setUpView(rootView: View) {
        tvTrace = rootView.findViewById<View>(R.id.tv_trace) as TextView
        tvTrace!!.typeface = Typeface.MONOSPACE
        if (lynxConfig.hasTextSizeInPx()) {
            val textSize = lynxConfig.getTextSizeInPx()
            tvTrace!!.textSize = textSize
        }
    }

    override fun hookListeners(rootView: View) {
        rootView.setOnLongClickListener {
            val clipboard =
                rootView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Trace", tvTrace!!.text.toString())
            clipboard.setPrimaryClip(clip)
            true
        }
    }

    override fun render() {
        val trace = content
        val traceMessage = trace!!.message
        val traceRepresentation = getTraceVisualRepresentation(trace.level, traceMessage)
        tvTrace!!.text = traceRepresentation
    }

    protected open val traceColor: Int
         get() = Color.GRAY

    private fun getTraceVisualRepresentation(level: TraceLevel, traceMessage: String): Spannable {
        val message: String = " " + level.value + "  " + traceMessage
        val traceRepresentation: Spannable = SpannableString(message)
        val traceColor = traceColor
        traceRepresentation.setSpan(
            BackgroundColorSpan(traceColor), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return traceRepresentation
    }
}