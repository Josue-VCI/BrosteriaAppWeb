import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';

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
  diaSemana = ''; // vacio, MONDAY, TUESDAY, etc.
  formatoPdf = 'naranja'; // naranja, monocromo, compacto

  private ventasChartRef: any = null;
  private pagosChartRef: any = null;
  private horasChartRef: any = null;
  private distritosChartRef: any = null;
  topProductos: any[] = [];

  private apiBaseUrl = `${API_BASE_URL}/api/v1/reportes`;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    if (localStorage.getItem('brosteria_role') !== 'ADMIN') {
      this.router.navigate(['/dashboard']);
      return;
    }
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
    if (this.tipoPedido) url += `tipoPedido=${this.tipoPedido}&`;
    if (this.diaSemana) url += `diaSemana=${this.diaSemana}`;

    this.http.get(url).subscribe({
      next: (data) => this.resumen = data,
      error: (err) => console.error('Error al cargar resumen de KPIs', err)
    });
  }

  cargarDatosGrafico() {
    this.http.get<any>(`${this.apiBaseUrl}/datos-grafico?filtroRango=${this.filtroRango}&diaSemana=${this.diaSemana}&tipoPedido=${this.tipoPedido}`).subscribe({
      next: (data) => {
        this.renderizarGraficoVentas(data.fechas, data.montos);
        this.renderizarGraficoPagos(data.metodosPago);
        this.renderizarGraficoHoras(data.pedidosPorHora || []);
        this.renderizarGraficoDistritos(data.distritos || {});
        this.topProductos = data.topProductos || [];
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
    if (fechaInicio) url += `fechaInicio=${fechaInicio}&`;
    if (this.diaSemana) url += `diaSemana=${this.diaSemana}&`;
    if (this.tipoPedido) url += `tipoPedido=${this.tipoPedido}&`;
    url += `formato=${this.formatoPdf}`;

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

  renderizarGraficoHoras(pedidosPorHora: number[]) {
    if (this.horasChartRef) {
      this.horasChartRef.destroy();
    }
    const labels = Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`);
    this.horasChartRef = new Chart('horasChart', {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Pedidos por Hora',
          data: pedidosPorHora,
          backgroundColor: '#FFB703',
          borderRadius: 4
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
            ticks: { color: '#A0A0A8', stepSize: 1 }
          },
          x: {
            grid: { display: false },
            ticks: { color: '#A0A0A8', maxRotation: 45, minRotation: 45 }
          }
        }
      }
    });
  }

  renderizarGraficoDistritos(distritos: any) {
    if (this.distritosChartRef) {
      this.distritosChartRef.destroy();
    }
    const labels = Object.keys(distritos);
    const data = Object.values(distritos);
    this.distritosChartRef = new Chart('distritosChart', {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Pedidos',
          data: data,
          backgroundColor: '#00B0FF',
          borderRadius: 4
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#A0A0A8', stepSize: 1 }
          },
          y: {
            grid: { display: false },
            ticks: { color: '#A0A0A8' }
          }
        }
      }
    });
  }

  descargarReportePDF90Dias() {
    let url = `${this.apiBaseUrl}/descargar-pdf?`;
    const ahora = new Date();
    const fecha = new Date();
    fecha.setMonth(ahora.getMonth() - 3);
    url += `fechaInicio=${fecha.toISOString()}&formato=${this.formatoPdf}`;
    
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const urlBlob = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = urlBlob;
        a.download = `Respaldo_Ventas_Trimestral_${new Date().toISOString().slice(0,10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(urlBlob);
      },
      error: (err) => console.error('Error al descargar el reporte PDF', err)
    });
  }

  limpiarHistorico() {
    if (!confirm('¿Está seguro de eliminar de forma permanente todos los pedidos de la base de datos que tengan más de 90 días? Se conservará el total acumulado de cada cliente.')) {
      return;
    }

    this.http.post(`${this.apiBaseUrl}/limpiar-historico`, {}).subscribe({
      next: (res: any) => {
        alert(res.mensaje || 'Historial de base de datos purgado con éxito.');
        this.aplicarFiltros();
      },
      error: (err) => {
        console.error('Error al purgar base de datos', err);
        alert('Ocurrió un error al ejecutar el mantenimiento.');
      }
    });
  }
}
