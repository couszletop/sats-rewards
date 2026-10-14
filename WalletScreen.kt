package com.satsrewards.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satsrewards.app.MainViewModel
import com.satsrewards.app.toBtcString

@Composable
fun WalletScreen(vm: MainViewModel) {
    var invoice by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }

    val me = vm.me
    val minWithdraw = vm.config?.minWithdrawSats ?: 2000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Wallet", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Balance", style = MaterialTheme.typography.labelLarge)
                Text(
                    "${me?.balanceSats?.toBtcString() ?: "0.00000000"} BTC",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("${me?.balanceSats ?: 0} sats")
            }
        }

        Text("Withdraw via Lightning", fontWeight = FontWeight.SemiBold)
        Text(
            "Paste an invoice from your Lightning wallet (min $minWithdraw sats).",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = invoice,
            onValueChange = { invoice = it.trim() },
            label = { Text("Lightning invoice") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit) },
            label = { Text("Amount (sats)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Button(
            onClick = {
                val sats = amount.toLongOrNull() ?: 0L
                busy = true
                resultMsg = null
                vm.withdraw(sats, invoice) { ok, msg ->
                    busy = false
                    resultMsg = msg
                    if (ok) {
                        invoice = ""
                        amount = ""
                    }
                }
            },
            enabled = !busy && invoice.isNotBlank() && (amount.toLongOrNull() ?: 0L) >= minWithdraw,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Sending..." else "Withdraw")
        }
        resultMsg?.let { Text(it) }

        Text("History", fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(vm.history) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(entry.type)
                    Text(
                        (if (entry.amountSats >= 0) "+" else "") + "${entry.amountSats} sats",
                        color = if (entry.amountSats >= 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
                Divider()
            }
        }
    }
}
