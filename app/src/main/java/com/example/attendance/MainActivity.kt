package com.example.attendance

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttendanceApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceApp() {
    var checkInLogs by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Attendance Tracker") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    checkInLogs = listOf(timestamp) + checkInLogs
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Check In")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    scope.launch {
                        val result = exportToExcel(context, checkInLogs)
                        snackbarHostState.showSnackbar(result)
                    }
                }) {
                    Text("Excel")
                }
                Button(onClick = {
                    scope.launch {
                        val result = exportToPdf(context, checkInLogs)
                        snackbarHostState.showSnackbar(result)
                    }
                }) {
                    Text("PDF")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(checkInLogs) { log ->
                    ListItem(
                        headlineContent = { Text(log) },
                        supportingContent = { Text("Checked in") }
                    )
                    Divider()
                }
            }
        }
    }
}

suspend fun exportToExcel(context: Context, logs: List<String>): String = withContext(Dispatchers.IO) {
    if (logs.isEmpty()) return@withContext "No logs to export"
    
    try {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Attendance")
        
        // Header
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("No")
        header.createCell(1).setCellValue("Timestamp")
        
        // Data
        logs.forEachIndexed { index, log ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            row.createCell(1).setCellValue(log)
        }
        
        // Save to Downloads
        val fileName = "Attendance_${System.currentTimeMillis()}.xlsx"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            return@withContext "Excel saved to Downloads"
        } ?: return@withContext "Failed to create Excel file"
        
    } catch (e: Exception) {
        return@withContext "Error: ${e.message}"
    }
}

suspend fun exportToPdf(context: Context, logs: List<String>): String = withContext(Dispatchers.IO) {
    if (logs.isEmpty()) return@withContext "No logs to export"

    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        // Title
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Attendance Report", 50f, 50f, paint)
        
        // Date
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $currentDate", 50f, 80f, paint)
        
        // Logs
        var yPos = 120f
        paint.textSize = 12f
        logs.forEachIndexed { index, log ->
            if (yPos > 800) { // Simple pagination check (basic)
                // In a real app, you'd start a new page here.
                // For this prototype, we'll stop to keep it simple.
                return@forEachIndexed 
            }
            canvas.drawText("${index + 1}. $log", 50f, yPos, paint)
            yPos += 25f
        }
        
        pdfDocument.finishPage(page)
        
        // Save to Downloads
        val fileName = "Attendance_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            return@withContext "PDF saved to Downloads"
        } ?: return@withContext "Failed to create PDF file"

    } catch (e: Exception) {
        return@withContext "Error: ${e.message}"
    }
}
