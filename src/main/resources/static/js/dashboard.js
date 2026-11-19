const ctx = document.getElementById('financeChart');

Chart.defaults.color = getComputedStyle(document.body).getPropertyValue('--text').trim();
Chart.defaults.borderColor = getComputedStyle(document.body).getPropertyValue('--border').trim();

const axisTextColor = () => getComputedStyle(document.body).getPropertyValue('--text').trim();
const axisGridColor = () => getComputedStyle(document.body).getPropertyValue('--border').trim();

new Chart(ctx, {

    type: 'bar',

    data: {

        labels: monthlyLabels,

        datasets: [

            {
                label: 'Ingresos',
                data: monthlyIncome,
                backgroundColor: 'rgba(34, 197, 94, 0.28)',
                borderColor: '#22c55e',
                borderWidth: 2
            },

            {
                label: 'Gastos',
                data: monthlyExpense,
                backgroundColor: 'rgba(239, 68, 68, 0.28)',
                borderColor: '#ef4444',
                borderWidth: 2
            }

        ]
    },

    options: {

        responsive: true,

        maintainAspectRatio: false,

        scales: {

            x: {
                ticks: {
                    color: axisTextColor
                },
                grid: {
                    color: axisGridColor
                },
                border: {
                    color: axisGridColor
                }
            },

            y: {
                beginAtZero: true,
                ticks: {
                    color: axisTextColor
                },
                grid: {
                    color: axisGridColor
                },
                border: {
                    color: axisGridColor
                }
            }

        }

    }

});
