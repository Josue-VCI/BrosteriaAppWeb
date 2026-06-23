import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Chart, registerables } from 'chart.js';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

Chart.register(...registerables);

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css']
})
export class ReportesComponent implements OnInit {
  resumen: any = { ventasTotales: 0, totalPedidos: 0, completados: 0, cancelados: 0 };
  filtroRango = 'trimestre'; // semana, mes, trimestre
  tipoPedido = ''; // vacio, delivery, pickup

  private ventasChartRef: any = null;
  private pagosChartRef: any = null;

  private apiBaseUrl = 'http://localhost:8081/api/v1/reportes';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.aplicarFiltros();
  }

  aplicarFiltros() {
    this.cargarResumen();
    this.cargarDatosGrafico();
  }

  cargarResumen() {
    let fechaInicio = '';
    const ahora = new Date();
    
    if (this.filtroRango === 'semana') {
      const fecha = new Date();
      fecha.setDate(ahora.getDate() - 7);
      fechaInicio = fecha.toISOString();
    } else if (this.filtroRango === 'mes') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 1);
      fechaInicio = fecha.toISOString();
    } else if (this.filtroRango === 'trimestre') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 3);
      fechaInicio = fecha.toISOString();
    }

    let url = `${this.apiBaseUrl}/resumen?`;
    if (fechaInicio) url += `fechaInicio=${fechaInicio}&`;
    if (this.tipoPedido) url += `tipoPedido=${this.tipoPedido}`;

    this.http.get(url).subscribe({
      next: (data) => this.resumen = data,
      error: (err) => console.error('Error al cargar resumen de KPIs', err)
    });
  }

  cargarDatosGrafico() {
    this.http.get<any>(`${this.apiBaseUrl}/datos-grafico?filtroRango=${this.filtroRango}`).subscribe({
      next: (data) => {
        this.renderizarGraficoVentas(data.fechas, data.montos);
        this.renderizarGraficoPagos(data.metodosPago);
      },
      error: (err) => console.error('Error al cargar datos del gráfico', err)
    });
  }

  renderizarGraficoVentas(labels: string[], data: number[]) {
    // Destruir instancia anterior si existe
    if (this.ventasChartRef) {
      this.ventasChartRef.destroy();
    }

    this.ventasChartRef = new Chart('ventasChart', {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ventas Diarias (S/.)',
          data: data,
          borderColor: '#FF6B00',
          backgroundColor: 'rgba(255, 107, 0, 0.12)',
          fill: true,
          tension: 0.35,
          borderWidth: 3,
          pointBackgroundColor: '#FFB703',
          pointBorderColor: '#fff',
          pointHoverRadius: 7
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#A0A0A8', font: { family: 'Quicksand' } }
          },
          x: {
            grid: { display: false },
            ticks: { color: '#A0A0A8', font: { family: 'Quicksand' } }
          }
        }
      }
    });
  }

  renderizarGraficoPagos(metodosPago: any) {
    if (this.pagosChartRef) {
      this.pagosChartRef.destroy();
    }

    const labels = Object.keys(metodosPago);
    const data = Object.values(metodosPago);

    this.pagosChartRef = new Chart('pagosChart', {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: ['#00C853', '#00B0FF', '#FFB703', '#FF2A6D'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: '#FAFAFA', font: { family: 'Quicksand', size: 12 } }
          }
        }
      }
    });
  }

  descargarReportePdf() {
    let fechaInicio = '';
    const ahora = new Date();
    
    if (this.filtroRango === 'semana') {
      const fecha = new Date();
      fecha.setDate(ahora.getDate() - 7);
      fechaInicio = fecha.toISOString();
    } else if (this.filtroRango === 'mes') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 1);
      fechaInicio = fecha.toISOString();
    } else if (this.filtroRango === 'trimestre') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 3);
      fechaInicio = fecha.toISOString();
    }

    let url = `${this.apiBaseUrl}/descargar-pdf?`;
    if (fechaInicio) url += `fechaInicio=${fechaInicio}`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const urlBlob = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = urlBlob;
        a.download = `Reporte_Ventas_Filtrado_${new Date().toISOString().slice(0,10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(urlBlob);
      },
      error: (err) => console.error('Error al descargar el reporte PDF', err)
    });
  }
}
