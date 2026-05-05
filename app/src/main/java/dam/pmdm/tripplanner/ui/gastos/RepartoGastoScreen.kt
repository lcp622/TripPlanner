package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.ui.theme.*

@Composable
fun RepartoGastoScreen(
    gasto: GastoEntity,
    repository: GastoRepository,
    viewModel: GastoViewModel
) {
    val repartos by repository.obtenerRepartos(gasto.idViaje, gasto.idGasto)
        .collectAsState(initial = emptyList())
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Reparto de \"${gasto.concepto}\"",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Total: €${String.format("%.2f", gasto.importe)}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (repartos.isEmpty()) {
            Text(
                text = "No hay reparto — gasto individual",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            repartos.forEach { reparto ->
                val idUsuario = reparto["idUsuario"]?.toString() ?: ""
                val nombre = reparto["nombreUsuario"]?.toString() ?: "Usuario"
                val importe = (reparto["importeAsignado"] as? Double) ?: 0.0
                val saldado = reparto["saldado"] as? Boolean ?: false
                val esMiReparto = idUsuario == currentUserId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saldado)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (saldado) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    else TripBlue.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (saldado) Icons.Default.Check else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (saldado) Color(0xFF4CAF50) else TripBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nombre,
                                fontWeight = FontWeight.Medium,
                                color = if (saldado)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (saldado) "Saldado ✓" else "Pendiente",
                                fontSize = 12.sp,
                                color = if (saldado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            text = "€${String.format("%.2f", importe)}",
                            fontWeight = FontWeight.Bold,
                            color = if (saldado) MaterialTheme.colorScheme.onSurfaceVariant else TripBlue
                        )

                        if (esMiReparto && !saldado) {
                            Button(
                                onClick = {
                                    viewModel.marcarComoSaldado(gasto.idViaje, gasto.idGasto, currentUserId)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Saldar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}