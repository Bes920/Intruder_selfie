package com.example.gradecalculatorconsole

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GradeCalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun GradeCalculatorScreen() {
    val context = LocalContext.current
    var studentName by remember { mutableStateOf(TextFieldValue("")) }
    var currentScore by remember { mutableStateOf(TextFieldValue("")) }
    val scores = remember { mutableStateListOf<Double>() }

    // Launcher for selecting a CSV file to upload
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val importedScores = importFromCsv(context, it)
            if (importedScores != null) {
                scores.clear()
                scores.addAll(importedScores)
                Toast.makeText(context, "Imported ${importedScores.size} scores", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for creating a CSV file to download/save
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            val success = exportToCsv(context, it, studentName.text, scores)
            if (success) {
                Toast.makeText(context, "Results saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save results", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Grade Calculator", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Student Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentScore,
                onValueChange = { currentScore = it },
                label = { Text("Enter Score") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    currentScore.text.toDoubleOrNull()?.let {
                        if (it in 0.0..100.0) {
                            scores.add(it)
                            currentScore = TextFieldValue("")
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Score")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { importLauncher.launch("text/*") }) {
                Text("Upload CSV")
            }
            Button(
                onClick = { exportLauncher.launch("${studentName.text.ifEmpty { "results" }}.csv") },
                enabled = scores.isNotEmpty()
            ) {
                Text("Download CSV")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Scores added: ${scores.size}", fontSize = 16.sp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(scores) { score ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = score.toString())
                        IconButton(onClick = { scores.remove(score) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (scores.isNotEmpty()) {
            val average = scores.average()
            val grade = calculateGrade(average)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Results for ${studentName.text.ifEmpty { "Unknown" }}", fontSize = 18.sp)
                    Text(text = "Average: ${String.format(Locale.US, "%.2f", average)}", fontSize = 16.sp)
                    Text(text = "Grade: $grade", fontSize = 16.sp)
                    Text(text = "Status: ${if (grade == 'F') "Fail" else "Pass"}", fontSize = 16.sp)
                }
            }
        }

        Button(
            onClick = {
                studentName = TextFieldValue("")
                currentScore = TextFieldValue("")
                scores.clear()
            },
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Reset All")
        }
    }
}

fun calculateGrade(average: Double): Char = when {
    average >= 90.0 -> 'A'
    average >= 80.0 -> 'B'
    average >= 70.0 -> 'C'
    average >= 60.0 -> 'D'
    else -> 'F'
}

private fun importFromCsv(context: Context, uri: Uri): List<Double>? {
    return try {
        val scores = mutableListOf<Double>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                // Skip header if exists
                if (line != null && line.contains("score", ignoreCase = true)) {
                    line = reader.readLine()
                }
                while (line != null) {
                    line.split(",").firstOrNull()?.toDoubleOrNull()?.let {
                        scores.add(it)
                    }
                    line = reader.readLine()
                }
            }
        }
        scores
    } catch (e: Exception) {
        null
    }
}

private fun exportToCsv(context: Context, uri: Uri, name: String, scores: List<Double>): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write("Student Name:,$name\n")
                writer.write("Score\n")
                scores.forEach { score ->
                    writer.write("$score\n")
                }
                val avg = scores.average()
                writer.write("\nAverage:,${String.format(Locale.US, "%.2f", avg)}\n")
                writer.write("Grade:,${calculateGrade(avg)}\n")
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}
