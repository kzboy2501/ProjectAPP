package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.DutyHeader
import com.example.ui.DutyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DutyNotificationHelper
import com.example.viewmodel.DutyViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: DutyViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DutyNotificationHelper.initChannel(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        DutyScreen(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Re-verify data and state on page/activity resume
    viewModel.loadData()
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  DutyHeader()
}

