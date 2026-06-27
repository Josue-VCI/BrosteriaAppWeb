import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { API_BASE_URL } from '../../config';
import { ToastService } from '../../services/toast.service';

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

  constructor(private http: HttpClient, private router: Router, private toastService: ToastService) {}

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

  private formatLocalISO(date: Date): string {
    const pad = (num: number) => num.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  cargarResumen() {
    let fechaInicio = '';
    const ahora = new Date();
    
    if (this.filtroRango === 'semana') {
      const fecha = new Date();
      fecha.setDate(ahora.getDate() - 7);
      fechaInicio = this.formatLocalISO(fecha);
    } else if (this.filtroRango === 'mes') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 1);
      fechaInicio = this.formatLocalISO(fecha);
    } else if (this.filtroRango === 'trimestre') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 3);
      fechaInicio = this.formatLocalISO(fecha);
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
      error: (err) => console.error('Error al cargar datos del grafico', err)
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
    const data = Object.values(metodosPago) as number[];
    const total = data.reduce((sum, val) => sum + val, 0);
    
    const labelsWithPercent = labels.map((label, idx) => {
      const val = data[idx];
      const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
      return `${label} (${pct}%)`;
    });

    this.pagosChartRef = new Chart('pagosChart', {
      type: 'pie',
      data: {
        labels: labelsWithPercent,
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
      fechaInicio = this.formatLocalISO(fecha);
    } else if (this.filtroRango === 'mes') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 1);
      fechaInicio = this.formatLocalISO(fecha);
    } else if (this.filtroRango === 'trimestre') {
      const fecha = new Date();
      fecha.setMonth(ahora.getMonth() - 3);
      fechaInicio = this.formatLocalISO(fecha);
    }

    let url = `${this.apiBaseUrl}/descargar-pdf?`;
    if (fechaInicio) url += `fechaInicio=${fechaInicio}&`;
    if (this.diaSemana) url += `diaSemana=${this.diaSemana}&`;
    if (this.tipoPedido) url += `tipoPedido=${this.tipoPedido}&`;
    url += `formato=${this.formatoPdf}`;

    const filename = `Reporte_Ventas_Filtrado_${new Date().toISOString().slice(0,10)}.pdf`;
    this.descargarBlobComoArchivo(url, filename);
  }

  renderizarGraficoHoras(pedidosPorHora: number[]) {
    if (this.horasChartRef) {
      this.horasChartRef.destroy();
    }
    
    // Solo mostrar las horas operativas activas de 18:00 a 23:00 (6 PM a 11 PM)
    const labels = [];
    const data = [];
    for (let i = 18; i <= 23; i++) {
      labels.push(`${i.toString().padStart(2, '0')}:00`);
      data.push(pedidosPorHora[i] || 0);
    }

    this.horasChartRef = new Chart('horasChart', {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Pedidos por Hora',
          data: data,
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
    const ahora = new Date();
    const fecha = new Date();
    fecha.setMonth(ahora.getMonth() - 3);
    const url = `${this.apiBaseUrl}/descargar-csv?fechaInicio=${this.formatLocalISO(fecha)}`;

    const filename = `Respaldo_Ventas_Trimestral_${new Date().toISOString().slice(0,10)}.csv`;
    this.descargarBlobComoArchivo(url, filename, 'text/csv');
  }

  /** Descarga un blob como archivo, compatible con iOS Safari, Chrome Android y Desktop */
  private descargarBlobComoArchivo(url: string, filename: string, mimeType: string = 'application/pdf') {
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const blobConTipo = new Blob([blob], { type: mimeType });
        const urlBlob = window.URL.createObjectURL(blobConTipo);

        // Detectar iOS Safari (no soporta <a download> creado dinamicamente)
        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
        const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

        if (isIOS || isSafari) {
          // En iOS/Safari: abrir el PDF en una nueva pestaña y dejar que el usuario lo guarde
          window.open(urlBlob, '_blank');
        } else {
          // En Android/Chrome/Desktop: descargar directamente
          const a = document.createElement('a');
          a.href = urlBlob;
          a.download = filename;
          a.style.display = 'none';
          document.body.appendChild(a);
          a.click();
          // Pequeño delay antes de limpiar para dar tiempo a que el navegador inicie la descarga
          setTimeout(() => {
            document.body.removeChild(a);
            window.URL.revokeObjectURL(urlBlob);
          }, 250);
        }
      },
      error: (err) => {
        console.error('Error al descargar el reporte PDF', err);
        this.toastService.error('No se pudo descargar el reporte. Verifica tu conexion al servidor.');
      }
    });
  }

  limpiarHistorico() {
    if (!confirm('¿Esta seguro de eliminar de forma permanente todos los pedidos de la base de datos que tengan mas de 90 dias? Se conservara el total acumulado de cada cliente.')) {
      return;
    }

    this.http.post(`${this.apiBaseUrl}/limpiar-historico`, {}).subscribe({
      next: (res: any) => {
        this.toastService.success(res.mensaje || 'Historial de base de datos purgado con exito.');
        this.aplicarFiltros();
      },
      error: (err) => {
        console.error('Error al purgar base de datos', err);
        this.toastService.error('Ocurrio un error al ejecutar el mantenimiento.');
      }
    });
  }
}
