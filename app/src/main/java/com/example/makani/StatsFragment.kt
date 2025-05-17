package com.example.makani

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
//works
class StatsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        pieChart = view.findViewById(R.id.pieChart)

        database = FirebaseDatabase.getInstance().getReference("tasks")
        loadTaskStats()

        return view
    }

    private fun loadTaskStats() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var done = 0
                var remaining = 0
                var overdue = 0

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = Calendar.getInstance().time

                for (taskSnap in snapshot.children) {
                    val task = taskSnap.getValue(Task::class.java)

                    val isDone = taskSnap.child("isDone").getValue(Boolean::class.java) ?: false

                    try {
                        val dueDate = sdf.parse(task?.dueDate ?: "")
                        if (dueDate != null) {
                            if (isDone) {
                                done++
                            } else if (dueDate.before(today)) {
                                overdue++
                            } else {
                                remaining++
                            }
                        } else {
                            remaining++
                        }
                    } catch (e: Exception) {
                        remaining++
                    }
                }


                val legend = pieChart.legend
                legend.isEnabled = true
                legend.form = Legend.LegendForm.CIRCLE
                legend.textSize = 14f
                legend.textColor = Color.BLACK
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM  // مكان الليجند عمودياً
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER  // مكان الليجند أفقياً
                legend.orientation = Legend.LegendOrientation.HORIZONTAL  // ترتيب العناصر (أفقي)
                legend.setDrawInside(false)  // رسم الليجند خارج الرسم

                val labels = listOf("Done", "Left", "overdue and not done")
                val entries = ArrayList<PieEntry>()
                entries.add(PieEntry(done.toFloat(), labels[0]))
                entries.add(PieEntry(remaining.toFloat(), labels[1]))
                entries.add(PieEntry(overdue.toFloat(), labels[2]))

                val dataSet = PieDataSet(entries, "")
                dataSet.colors = listOf(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#FFC107"),
                    Color.parseColor("#F44336")
                )

                val data = PieData(dataSet)
                data.setValueTextSize(14f)
                data.setValueTextColor(Color.WHITE)

                pieChart.data = data
                pieChart.setDrawEntryLabels(false)
                pieChart.description.isEnabled = false
                pieChart.centerText = "Tasks state"
                pieChart.animateY(1000)
                pieChart.invalidate()

            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Fail in loading stats", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
